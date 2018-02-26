package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.protobuf.ByteString;
import com.moandjiezana.toml.TomlWriter;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

/**
 * A class to help with using tendermint.
 */
public final class TendermintHelper {

	private static final String NODE_FOLDER_NAME_PREFIX = "node";
	//the executable binary of tendermint
	private static final String TENDERMINT_BINARY = "./tendermint.exe";
	//The level at which tendermint logs (error, info, debug, none). E.g. "consensus:debug,*:error"
	private static final String TENDERMINT_LOG_LEVEL = "state:info,*:error"; //"state:info,*:error";
	private static final TendermintLogMethod TENDERMINT_LOG_METHOD = TendermintLogMethod.FILE_SPLIT;

	/* do not initialize this */
	private TendermintHelper() {}

	/**
	 * Generate the priv_validator.json file needed for Tendermint node to run.
	 * If public/private key pair are to be generated, use {@link TendermintHelper#generatePrivValidatorFile(int)}.
	 * If the file is already there, it is overridden.
	 * WARNING: providing custom keyPair does not work until jABCI gets updated to TM15
	 * @param keyPair - the public/private keypair that should be used, null if to be generated
	 * @param nodeNumber - the number of the node to generate a key for
	 * @return the public/private key pair generated if none provided, the same if provided, null if method failed.
	 */
	public static Ed25519Key generatePrivValidatorFile(Ed25519Key keyPair, int nodeNumber) {

		StringBuilder script = new StringBuilder();

		//add binary to execute
		script.append(TENDERMINT_BINARY).append(" gen_validator ");

		String nodeFilesLocation = getNodeFilesLocation(nodeNumber);

		StringBuilder generatedJsonString = new StringBuilder();
		Log.log(Level.INFO, "Executed " + script.toString());
		try {
			final Process ps = startProcess(script.toString(), nodeFilesLocation, true);
			ps.waitFor();
			
			try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(ps.getInputStream()))) {
				// read the output from the command
				String s;
				while ((s = stdInput.readLine()) != null) {
					generatedJsonString.append(s);
				}
			}
		} catch (InterruptedException e) {
			Log.log(Level.WARNING, "Interrupted on startup. No key pair generated.");
			return null;
		} catch (IOException e) {
			Log.log(Level.WARNING, "Could not generate priv_validator.json due to IO Exception", e);
			return null;
		}
		JSONObject privValidator = new JSONObject(generatedJsonString.toString());

		if (keyPair != null) { //keyPair provided
			//TODO: remove once jABCI gets updated to TM15
			Log.log(Level.WARNING, "You are replacing public/private key pair, which won't work until jABCI gets updated to TM15.");
			//Replace public key
			byte[] publicKey = keyPair.getPublicKey();
			JSONObject pubKey = new JSONObject();
			pubKey.put("type", "ed25519");
			pubKey.put("data", Utils.bytesToHexString(publicKey).toUpperCase());
			privValidator.put("pub_key", pubKey);

			//Replace private key
			byte[] privateKey = keyPair.getPrivateKey();
			JSONObject privKey = new JSONObject();
			privKey.put("type", "ed25519");
			privKey.put("data", Utils.bytesToHexString(privateKey).toUpperCase());
			privValidator.put("priv_key", privKey);
		} else { //keyPair not provided
			JSONObject pubKey = privValidator.getJSONObject("pub_key");
			byte[] publicKey = Utils.hexStringToBytes(pubKey.getString("data"));

			JSONObject privKey = privValidator.getJSONObject("priv_key");
			byte[] privateKey = Utils.hexStringToBytes(privKey.getString("data"));

			keyPair = new Ed25519Key(privateKey, publicKey);
		}

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(nodeFilesLocation, "priv_validator.json"))) {
			if (!ensureDirectoryExists(nodeFilesLocation)) {
				return null;
			}

			writer.write(privValidator.toString());
		} catch (IOException e) {
			Log.log(Level.WARNING, "Could not generate priv_validator.json due to IO Exception", e);
			return null;
		}
		return keyPair;

	}

	private static String getNodeFilesLocation(int nodeNumber) {
		return new File(Settings.INSTANCE.tendermintNodesFolder, NODE_FOLDER_NAME_PREFIX + nodeNumber).toString();
	}

	/**
	 * Generate the priv_validator.json file needed for Tendermint node to run,
	 * generating all necessary data if it doesn't exist.
	 * If the file is already there, it is overridden.
	 * @param nodeNumber - the number of the node to generate file for
	 * @return the public/private key pair generated, null if failed
	 */
	public static Ed25519Key generatePrivValidatorFile(int nodeNumber) {
		return generatePrivValidatorFile(null, nodeNumber);
	}

	/** Generate multiple priv_validator.json file, one for each node number given in the argument list.
	 * @param numbersToGenerate - a list of node numbers that you want to generate priv_validator.json for
	 * @return the map of id -> keyPair generated
	 */
	public static Map<Integer, Ed25519Key> generatePrivValidatorFiles(List<Integer> numbersToGenerate) {
		Map<Integer, Ed25519Key> nodeNumbersToKeys = new HashMap<>();
		for (Integer i : numbersToGenerate) {
			Ed25519Key generatedKey = generatePrivValidatorFile(i);
			nodeNumbersToKeys.put(i, generatedKey);
		}
		return nodeNumbersToKeys;
	}

	/** Generate multiple priv_validator.json file, one for each node number from start to end given in the argument list.
	 * @param start - the lowest number of node to generate a file for (inclusive)
	 * @param end - the highest number of node to generate a file for (inclusive)
	 * @return the map of id -> keyPair generated
	 */
	public static Map<Integer, Ed25519Key> generatePrivValidatorFiles(int start, int end) {
		List<Integer> range = IntStream.rangeClosed(start, end)
				.boxed().collect(Collectors.toList());
		return generatePrivValidatorFiles(range);
	}

	/**
	 * Method to generate a genesis.json file for tendermint. If the file is already in the given location, it is overridden.
	 * @param genesisTime - the time of genesis
	 * @param publicKeys - the public keys of validators (usually all the nodes)
	 * @param appHash - the hash of the application (ie. genesis block) at the beginning
	 * @param nodeNumber - the number of the node to generate file for
	 * @return true if succeeded; false otherwise
	 */
	public static boolean generateGenesisFile(Date genesisTime, Map<Integer, String> publicKeys, byte[] appHash, int nodeNumber) {
		JSONObject genesis = new JSONObject();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
		genesis.put("genesis_time", dateFormat.format(genesisTime));
		//this might not matter at all
		genesis.put("chain_id", "simulation-chain");
		genesis.put("app_hash", Utils.bytesToHexString(ByteString.copyFrom(appHash).toByteArray()));
		JSONArray validators = new JSONArray();
		for (Integer i : publicKeys.keySet()) {
			JSONObject validator = new JSONObject();
			JSONObject pubKey = new JSONObject();
			pubKey.put("data", publicKeys.get(i));
			pubKey.put("type", "ed25519");
			validator.put("pub_key", pubKey);
			validator.put("power", 1);
			validator.put("name", "" + i);
			validators.put(validator);
		}
		genesis.put("validators", validators);

		String nodeFilesLocation = getNodeFilesLocation(nodeNumber);
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(nodeFilesLocation, "genesis.json"))) {
			if (!ensureDirectoryExists(nodeFilesLocation)) {
				return false;
			}

			writer.write(genesis.toString());
		} catch (IOException e) {
			Log.log(Level.WARNING, "Could not generate genesis.json due to IO exception.", e);
			return false;
		}
		return true;
	}

	/**
	 * Generate multiple genesis.json files (one for each node). If the files is already in the given location, they are overridden.
	 * @param genesisTime - the time of genesis
	 * @param publicKeys - the public keys of validators (usually all the nodes)
	 * @param appHash - the hash of the application (ie. genesis block) at the beginning
	 * @param numbersOfNodes - the numbers of nodes to generate genesis.json for
	 * @return true if succeeded; false otherwise
	 */
	public static boolean generateGenesisFiles(Date genesisTime,
											   Map<Integer, String> publicKeys, byte[] appHash, List<Integer> numbersOfNodes) {
		for (Integer i : numbersOfNodes) {
			if (!generateGenesisFile(genesisTime, publicKeys, appHash, i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Generate multiple genesis.json files (one for each node). If the files is already in the given location, they are overridden.
	 * @param genesisTime - the time of genesis
	 * @param publicKeys - the public keys of validators (usually all the nodes)
	 * @param appHash - the hash of the application (ie. genesis block) at the beginning
	 * @param start - the lowest number of node to generate a file for (inclusive)
	 * @param end - the highest number of node to generate a file for (inclusive)
	 * @return true if succeeded; false otherwise
	 */
	public static boolean generateGenesisFiles(Date genesisTime,
											   Map<Integer, String> publicKeys, byte[] appHash, int start, int end) {
		List<Integer> range = IntStream.rangeClosed(start, end)
				.boxed().collect(Collectors.toList());
		return generateGenesisFiles(genesisTime, publicKeys, appHash, range);
	}

	/**
	 * Run the tendermint process for a single node (Application).
	 * Entire script will work like (and be similar to) this:
	 * TMROOT=[nodeFilesLocation] /path/to/tendermint node --consensus.create_empty_blocks=false
	 * 													   --p2p.laddr=tcp://0.0.0.0:[nodeBasePort+1]
	 * 												       --rpc.laddr=tcp://0.0.0.0:[nodeBasePort+2]
	 *												       --proxy_app=tcp://127.0.0.1:[nodeBasePort+2]
	 *												       --p2p.seeds=[peerAddresses]
	 * @param nodeBasePort - the base port for the node (ie the lowest assigned port for the node, the port+0)
	 * @param peerAddresses - a list of addresses (with ports, which should be basePort+1) of *other* nodes
	 * @param nodeNumber - the number of the node to run
	 * @return - the tendermint process
	 * @throws IOException - if an I/O error occurs
	 */
	public static Process runTendermintNode(int nodeBasePort, List<String> peerAddresses, int nodeNumber) throws IOException {
		//First write config file
		writeConfig(nodeBasePort, peerAddresses, nodeNumber);
		
		StringBuilder script = new StringBuilder(256);

		//add binary to execute
		script.append(TENDERMINT_BINARY).append(" node ");

		//TODO: get this from config/properties file?
		//turn on for extra logging (on debug level)
		boolean logMore = false;
		if (logMore) {
			script.append("--log_level=debug ");
		}
		String nodeFilesLocation = getNodeFilesLocation(nodeNumber);
		//add arguments
		script.append("--consensus.create_empty_blocks=false").append(' ');
		script.append("--home ").append(nodeFilesLocation).append(' ');
		script.append("--p2p.laddr=tcp://0.0.0.0:").append(nodeBasePort + 1).append(' ');
		script.append("--rpc.laddr=tcp://0.0.0.0:").append(nodeBasePort + 2).append(' ');
		script.append("--proxy_app=tcp://127.0.0.1:").append(nodeBasePort + 3).append(' ');
		script.append("--moniker=Node").append(nodeNumber).append(' ');

		//add other seeds
		if (peerAddresses != null && !peerAddresses.isEmpty()) {
			script.append("--p2p.seeds=").append(String.join(",", peerAddresses));
		}

		//run the process (and destroy it on JVM stop)
		Log.log(Level.INFO, "Executed " + script.toString());
		final Process ps = startProcess(script.toString(), nodeNumber);
		
		Runtime.getRuntime().addShutdownHook(new Thread(ps::destroy));
		return ps;
	}

	/**
	 * Starts a process running the given command.
	 * @param command        - the command to run
	 * @param tmHome         - the tendermint home location
	 * @param redirectErrors - if true, then the error stream is redirected to the output stream
	 * @return - the started process
	 * @throws IOException - If an exception occurs trying to start the process
	 */
	private static Process startProcess(String command, String tmHome, boolean redirectErrors) throws IOException {
		ProcessBuilder pb = new ProcessBuilder();
		StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            cmdarray[i] = st.nextToken();
        }
		pb.command(cmdarray);
		pb.environment().put("TMROOT", tmHome);
		pb.environment().put("TMHOME", tmHome);
		if (redirectErrors) pb.redirectErrorStream(true);
		return pb.start();
	}
	
	/**
	 * Starts a process running the given command.
	 * @param command      - the command to run
	 * @param nodeNumber   - the number of the node
	 * @return             - the started process
	 * @throws IOException - If an exception occurs trying to start the process.
	 */
	private static Process startProcess(String command, int nodeNumber) throws IOException {
		//Determine the home folder for tendermint
		File tmHomeFile = new File(Settings.INSTANCE.tendermintNodesFolder, NODE_FOLDER_NAME_PREFIX + nodeNumber);
		String tmHome = tmHomeFile.toString();
		
		ProcessBuilder pb = new ProcessBuilder();
		StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            cmdarray[i] = st.nextToken();
        }
		pb.command(cmdarray);
		
		//Set up the environment.
		//Tendermint changed the name of the variable in some version, so we just set both to be sure.
		pb.environment().put("TMROOT", tmHome);
		pb.environment().put("TMHOME", tmHome);
		
		//Redirect output to the correct location
		switch (TENDERMINT_LOG_METHOD) {
			case FILE_MERGED:
				pb.redirectErrorStream(true);
				pb.redirectOutput(new File(tmHomeFile, "output.log"));
				return pb.start();
			case FILE_SPLIT:
				pb.redirectError(new File(tmHomeFile, "errors.log"));
				pb.redirectOutput(new File(tmHomeFile, "output.log"));
				return pb.start();
			case CONSOLE:
				Process process = pb.start();
				enableLogging(process, Integer.toString(nodeNumber));
				return process;
		}
		
		throw new IllegalStateException("TENDERMINT LOG METHOD is not set to a valid value.");
	}
	
	/**
	 * Writes the config.toml file for the node with the given node number.
	 * @param nodeBasePort  - the base port of the node
	 * @param peerAddresses - the addresses of peer nodes
	 * @param nodeNumber    - the node to write the config for
	 */
	public static void writeConfig(int nodeBasePort, List<String> peerAddresses, int nodeNumber) {
		File nodeFolder = new File(Settings.INSTANCE.tendermintNodesFolder, NODE_FOLDER_NAME_PREFIX + nodeNumber);
		File configFile = new File(nodeFolder, "config.toml");
		Map<String, Object> toWrite = new HashMap<>();
		toWrite.put("proxy_app", "tcp://127.0.0.1:" + (nodeBasePort + 3));
		toWrite.put("moniker", "Node" + nodeNumber);
		toWrite.put("fast_sync", true);
		toWrite.put("db_backend", "memdb");
		toWrite.put("log_level", TENDERMINT_LOG_LEVEL);
		toWrite.put("addrbook_strict", false);
		
		Map<String, Object> rpcMap = new HashMap<>();
		rpcMap.put("laddr", "tcp://0.0.0.0:" + (nodeBasePort + 2));
		toWrite.put("rpc", rpcMap);
		
		Map<String, Object> p2pMap = new HashMap<>();
		p2pMap.put("laddr", "tcp://0.0.0.0:" + (nodeBasePort + 1));
		p2pMap.put("seeds", String.join(",", peerAddresses));
		toWrite.put("p2p", p2pMap);
		
		Map<String, Object> consensusMap = new HashMap<>();
		consensusMap.put("create_empty_blocks", false);
		toWrite.put("consensus", consensusMap);
		
		try {
			new TomlWriter().write(toWrite, configFile);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static boolean ensureDirectoryExists(String location) {
		File directory = new File(location);
		if (!directory.exists() && !directory.mkdirs()) {
			Log.log(Level.WARNING, "Directory " + location + " doesn't exist and could not create it.");
			return false;
		}
		return true;
	}

	/**
	 * For reading stdin and stderr of a spawned process.
	 * @param ps - the process from which to read the stdin and stderr
	 */
	private static void enableLogging(Process ps, String logPrefix) {
		Thread stdOutThread = new Thread(() -> {
			try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(ps.getInputStream()))) {
				// read the output from the command
				String s;
				while ((s = stdInput.readLine()) != null) {
					Log.log(Level.FINE, "[TM STDIN " + logPrefix + " ] " + s);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		Thread stdErrThread = new Thread(() -> {
			try (BufferedReader stdError = new BufferedReader(new InputStreamReader(ps.getErrorStream()))) {
				// read any errors from the attempted command
				String s;
				while ((s = stdError.readLine()) != null) {
					Log.log(Level.FINE, "[TM STDERROR " + logPrefix + " ] " + s);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		stdOutThread.start();
		stdErrThread.start();
	}

	/**
	 * Generate the genesis block for the application.
	 * @param amount - the amount to give each of the nodes
	 * @param nodeList - the list of all nodes with proper public keys, addresses and ports set
	 * @return the genesis block
	 */
	public static Block generateGenesisBlock(long amount, Map<Integer, Node> nodeList) {
		List<Transaction> initialTransactions = new ArrayList<>();
		for (int i = 0; i < nodeList.size(); i++) {
			Transaction t = new Transaction(i, null, nodeList.get(i), amount, 0, new TreeSet<>());
			initialTransactions.add(t);
		}
		
		Block block = new Block(0, null, initialTransactions);
		block.setNextCommittedBlock(block);
		return block;
	}
	
	/**
	 * Remove given folder, used to delete tendermint folders.
	 */
	public static void cleanTendermintFiles() {
		try {
			FileUtils.deleteDirectory(new File(Settings.INSTANCE.tendermintNodesFolder));
		} catch (IOException ex) {
			Log.log(Level.WARNING, "Could not delete Tendermint folder");
		}
	}
	
	private static enum TendermintLogMethod {
		/**
		 * Log tendermint output to 2 files, one for standard output and one for errors.
		 */
		FILE_SPLIT,
		/**
		 * Log tendermint output to a single file, where standard and error output are merged.
		 */
		FILE_MERGED,
		/**
		 * Log tendermint output to the console of the simulation.
		 */
		CONSOLE
	}
}
