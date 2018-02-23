package nl.tudelft.blockchain.scaleoutdistributedledger.settings;

import java.io.File;
import java.util.logging.Level;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.NewTransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.amountselector.AmountSelector;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.nodeselector.NodeSelector;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Class for settings.
 */
public class Settings {
	public static Settings INSTANCE;
	
	// ------------------------------------ Number of nodes --------------------------------------------
	//number of local nodes to generate
	public int localNodesNumber = 20;
	//number of total nodes in the system
	public int totalNodesNumber = 20;
	//number from which our nodes are (e.g if we run nodes (2, 3, 4), then this should be 2
	public int nodesFromNumber;
	//Whether this main is the master coordinator of the simulation
	//Note that the master should always be started first
	public boolean isMaster = true;
	
	// ------------------------------------ Tracker ------------------------------------
	public String _____TRACKER_____ = "";
	//The URL of the tracker server
	public String trackerUrl = "http://localhost:3000";
	// The number of transactions that are registered in one batch.
	public int registerTransactionsEvery = 20;
	
	// ------------------------------------ Transaction Pattern ------------------------------------
	public String _____TRANSACTION_PATTERN_____ = "";
	public String seed = "";
	public String amountSelector = "fixed:10";
	public String nodeSelector = "uniform:0";
	
	//Commit every x transactions
	public int commitEvery = totalNodesNumber;
	
	//The initial amount of money each node has.
	public long initialMoney = 1000000;
	
	//If transactions should be grouped
	public boolean grouping;
	
	//------------------------------------ Transaction sender ------------------------------------
	public String _____TRANSACTION_SENDER_____ = "";
	//The number of blocks (with the same or higher block number) that need to be committed before we send a certain block.
	public int requiredCommits = 2;
	//Maximum number of blocks waiting to be sent (no new transaction will be created in the mean time)
	public int maxBlocksPending = 500;
	
	// ----------------------------- TIMINGS ---------------------------
	public String _____TIMINGS_____ = "";
	//The time to wait between making transactions
	public long roundTime = 100;
	
	
	//The duration of the simulation in seconds.
	public int simulationDuration = 6000;
	//The initial delay in milliseconds to wait before checking what blocks can be sent.
	public long initialSendingDelay = 2000;
	//The time in milliseconds between send checks.
	public long sendingWaitTime = 2000;
	//The time between checking to deliver sent messages.
	public long deliverRecheckTime = 200;
	
	// ----------------------------- TENDERMINT -----------------------------
	public String _____TENDERMINT_____ = "";
	//the directory to store the file (will create separate directories in it for each node)
	public String tendermintNodesFolder = "Z:\\tendermint-nodes";
	
	@JsonIgnore
	private NewTransactionPattern transactionPattern;
	
	static {
		INSTANCE = readSettings();
		if (INSTANCE == null) {
			Log.log(Level.WARNING, "Using default settings!");
			INSTANCE = new Settings();
		}
	}
	
	/**
	 * If settings don't exist, then writes default settings to file and returns this.
	 * Otherwise, this method returns the settings in the settings.json file.
	 * @return - the settings in the settings.json file
	 */
	public static Settings readSettings() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		
		File settingsFile = new File("settings.json");
		if (!settingsFile.exists()) {
			Log.log(Level.INFO, "Writing default settings");
			Settings settings = new Settings();
			try {
				mapper.writeValue(settingsFile, settings);
			} catch (Exception ex) {
				Log.log(Level.SEVERE, "Unable to write default settings!", ex);
			}
			return settings;
		} else {
			try {
				return mapper.readValue(new File("settings.json"), Settings.class);
			} catch (Exception ex) {
				Log.log(Level.SEVERE, "Unable to read settings!", ex);
				return null;
			}
		}
	}
	
	/**
	 * @return - the transaction pattern
	 */
	@JsonIgnore
	public NewTransactionPattern getTransactionPattern() {
		if (this.transactionPattern != null) return this.transactionPattern;
		NewTransactionPattern ntp = new NewTransactionPattern();
		ntp.setAmountSelector(AmountSelector.fromString(amountSelector));
		ntp.setNodeSelector(NodeSelector.fromString(nodeSelector));
		
		if (!seed.isEmpty()) ntp.setSeed(Long.valueOf(seed));
		
		this.transactionPattern = ntp;
		return ntp;
	}
}
