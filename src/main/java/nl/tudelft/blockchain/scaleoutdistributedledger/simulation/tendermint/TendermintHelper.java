package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.protobuf.ByteString;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import org.apache.commons.io.FileUtils;

/**
 * A class to help with using tendermint.
 */
public final class TendermintHelper {

	private static final String NODE_FOLDER_NAME_PREFIX = "node";
	//the directory to store the file (will create separate directories in it for each node)
	private static final String TENDERMINT_NODES_FOLDER = "tendermint-nodes";
	//the executable binary of tendermint
	private static final String TENDERMINT_BINARY = "./tendermint.exe";

	/* do not initialize this */
	private TendermintHelper() {}

	/**
	 * Generate the priv_validator.json file needed for Tendermint node to run.
	 * If public/private key pair are to be generated, use {@link TendermintHelper#generatePrivValidatorFile(int)}.
	 * If the file is already there, it is overridden.
	 * WARNING: providing custom keyPair does not work until jABCI gets updated to TM15
	 * @param keyPair - the public/private keypair that should be used, null if to be generated
	 * @return the public/private key pair generated if none provided, the same if provided, null if method failed.
	 */
	public static Ed25519Key generatePrivValidatorFile(Ed25519Key keyPair, int nodeNumber) {

		StringBuilder script = new StringBuilder();

		//add binary to execute
		script.append(TENDERMINT_BINARY).append(" gen_validator ");

		String nodeFilesLocation = getNodeFilesLocation(nodeNumber);
		//environment variable of TMROOT
		String[] envVariables = { "TMROOT=" + nodeFilesLocation };

		Runtime rt = Runtime.getRuntime();
		StringBuilder generatedJsonString = new StringBuilder();
		Log.log(Level.INFO, "Executed " + script.toString());
		try {
			final Process ps = rt.exec(script.toString(), envVariables);
			ps.waitFor();
			BufferedReader stdInput = new BufferedReader(new
					InputStreamReader(ps.getInputStream()));
			// read the output from the command
			String s;
			while ((s = stdInput.readLine()) != null) {
				generatedJsonString.append(s);
			}
			stdInput.close();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
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

		try (
				BufferedWriter writer = Files.newBufferedWriter(Paths.get(nodeFilesLocation, "priv_validator.json"))
		) {
			if (!ensureDirectoryExists(nodeFilesLocation)) {
				return null;
			}

			writer.write(privValidator.toString());
			writer.close();
		} catch (IOException e) {
			Log.log(Level.WARNING, "Could not generate priv_validator.json due to IO Exception", e);
			return null;
		}
		return keyPair;

	}

	private static String getNodeFilesLocation(int nodeNumber) {
		return new File(TENDERMINT_NODES_FOLDER, NODE_FOLDER_NAME_PREFIX + nodeNumber).toString();
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
		try (
				BufferedWriter writer = Files.newBufferedWriter(Paths.get(nodeFilesLocation, "genesis.json"))
		) {
			if (!ensureDirectoryExists(nodeFilesLocation)) {
				return false;
			}

			writer.write(genesis.toString());
			writer.close();
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
	 * @throws IOException - if an I/O error occurs
	 */
	public static void runTendermintNode(int nodeBasePort, List<String> peerAddresses, int nodeNumber) throws IOException {

		StringBuilder script = new StringBuilder(256);

		//add binary to execute
		script.append(TENDERMINT_BINARY).append(" node --consensus.create_empty_blocks=false ");

		//TODO: get this from config/properties file?
		//turn on for extra logging (on debug level)
		boolean logMore = false;
		if (logMore) {
			script.append("--log_level=debug ");
		}
		String nodeFilesLocation = getNodeFilesLocation(nodeNumber);
		//add arguments
		script.append("--home ").append(nodeFilesLocation).append(" ");
		script.append("--p2p.laddr=tcp://0.0.0.0:").append(nodeBasePort + 1).append(" ");
		script.append("--rpc.laddr=tcp://0.0.0.0:").append(nodeBasePort + 2).append(" ");
		script.append("--proxy_app=tcp://127.0.0.1:").append(nodeBasePort + 3).append(" ");

		//add other seeds
		if (peerAddresses != null && !peerAddresses.isEmpty()) {
			script.append("--p2p.seeds=").append(String.join(",", peerAddresses));
		}

		//environment variable of TMROOT
		String[] envVariables = { "TMROOT=" + nodeFilesLocation };

		//run the process (and destroy it on JVM stop)
		Runtime rt = Runtime.getRuntime();
		Log.log(Level.INFO, "Executed " + script.toString());
		final Process ps = rt.exec(script.toString(), envVariables);
		rt.addShutdownHook(new Thread(ps::destroy));

		//TODO: get this from some config/properties file?
		//whether to show stdin/stderr from tendermint; warning: may be costly (2 new threads)
		boolean enableLogsFromTendermint = true;
		if (enableLogsFromTendermint) {
			enableLogging(ps, Integer.toString(nodeBasePort));
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
			try (
					BufferedReader stdInput = new BufferedReader(new
							InputStreamReader(ps.getInputStream()))
			) {
				// read the output from the command
				String s;
				while ((s = stdInput.readLine()) != null) {
					Log.log(Level.FINE, "[TM STDIN " + logPrefix + " ] " + s);
				}
				stdInput.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		Thread stdErrThread = new Thread(() -> {
			try (
					BufferedReader stdError = new BufferedReader(new
							InputStreamReader(ps.getErrorStream()))
			) {
				// read any errors from the attempted command
				String s;
				while ((s = stdError.readLine()) != null) {
					Log.log(Level.FINE, "[TM STDERROR " + logPrefix + " ] " + s);
				}
				stdError.close();
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
		List<Transaction> initialTransactions = new LinkedList<>();
		for (Integer i : nodeList.keySet()) {
			Transaction t = new Transaction(i, null, nodeList.get(i), amount, 0, new HashSet<>(0));
			t.setBlockNumber(OptionalInt.of(0));
			initialTransactions.add(t);
		}
		return new Block(0, null, initialTransactions);
	}
	
	/**
	 * Remove given folder, used to delete tendermint folders.
	 */
	public static void cleanTendermintFiles() {
		try {
			FileUtils.deleteDirectory(new File(TENDERMINT_NODES_FOLDER));
		} catch (IOException ex) {
			Log.log(Level.WARNING, "Could not delete Tendermint folder");
		}
	}
	
}
