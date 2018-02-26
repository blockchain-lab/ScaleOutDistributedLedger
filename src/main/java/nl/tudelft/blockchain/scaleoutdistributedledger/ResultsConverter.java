package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ResultsConverter {
	public static final StandardOpenOption OVERRIDE = StandardOpenOption.CREATE_NEW;
	
	public static final int  N = 20;
	public static final int  G = 1;
	public static final Mode MODE = Mode.GROUPING;
	public static final int  RUN = 1;
	
	public static final String BASE = "n" + N + "_g" + G + "_m" + MODE.ordinal() + "_r" + RUN;
	public static final String BASEFOLDER = "nresults/" + BASE.substring(0, BASE.length() - 3) + "/";
	public static final File FOLDER = new File(BASEFOLDER);
	public static final String INPUT = BASEFOLDER + BASE + ".json";
	public static final String DATA = BASEFOLDER + BASE + "_data.csv";
	public static final String FIXDATA = BASEFOLDER + BASE + "_data_fixed.csv";
	public static final String SETCSIZES = BASEFOLDER + BASE + "_setc_sizes.csv";
	public static final String OUT1 = BASEFOLDER + BASE + "_converted.csv";
	public static final String OUT2 = BASEFOLDER + BASE + "_sorted.csv";
	public static final String OUT3 = BASEFOLDER + BASE + "_sorted_setc.csv";
	
	public static final String[] A_HEADERS;
	public static final String[] B_HEADERS;
	public static final String[] FIXDATA_HEADERS;
	
	static {
		A_HEADERS = new String[N + 10];
		A_HEADERS[0] = "id";
		A_HEADERS[1] = "from";
		A_HEADERS[2] = "to";
		A_HEADERS[3] = "amount";
		A_HEADERS[4] = "remainder";
		A_HEADERS[5] = "nrOfChains";
		A_HEADERS[6] = "nrOfBlocks";
		A_HEADERS[7] = "setC";
		A_HEADERS[8] = "setCBlocks";
		A_HEADERS[9] = "|setC|";
		for (int i = 0; i < N; i++) {
			A_HEADERS[i + 10] = "knowledge " + i;
		}
		
		//id, node (to), setCSize, setC, setCBlocks, setCDelta, from, amount, remainder, numberOfChains, numberOfBlocks,
		B_HEADERS = new String[N + 12];
		B_HEADERS[0] = "id";
		B_HEADERS[1] = "node";
		B_HEADERS[2] = "setCSize";
		B_HEADERS[3] = "setC";
		B_HEADERS[4] = "setCBlocks";
		B_HEADERS[5] = "setCDelta";
		B_HEADERS[6] = "deltaBlocks";
		B_HEADERS[7] = "from";
		B_HEADERS[8] = "amount";
		B_HEADERS[9] = "remainder";
		B_HEADERS[10] = "nrOfChains";
		B_HEADERS[11] = "nrOfBlocks";
		for (int i = 0; i < N; i++) {
			B_HEADERS[i + 12] = "knowledge " + i;
		}
		
		FIXDATA_HEADERS = new String[N * 2 + 4];
		FIXDATA_HEADERS[0] = "nrOfTransactions";
		FIXDATA_HEADERS[1] = "average NrOfBlocks";
		FIXDATA_HEADERS[2] = "average NrOfChains";
		FIXDATA_HEADERS[3] = "average |setC|";
		for (int i = 0; i < N; i++) {
			FIXDATA_HEADERS[i + 4] = "|setC| " + i;
		}
		for (int i = 0; i < N; i++) {
			FIXDATA_HEADERS[i + N + 4] = "setC " + i;
		}
	}
	
	public static void main(String[] args) throws Exception {
		renameInputs();
		removeDuplicates();
		fixData();
		getIndividualSetCSizes();
		initialConvert();
		sort();
		sortedToDelta();
	}
	
	/**
	 * Renames the input files to their correct names.
	 */
	public static void renameInputs() {
		new File(FOLDER, "transactionlist.json").renameTo(new File(FOLDER, BASE + ".json"));
		new File(FOLDER, "data.csv").renameTo(new File(FOLDER, BASE + ".csv"));
	}
	
	/**
	 * Removes duplicates from the data csv.
	 * @throws Exception .
	 */
	public static void removeDuplicates() throws Exception {
		ProcessBuilder pb = new ProcessBuilder(
				"C:\\Program Files\\Git\\git-bash.exe",
				"-c",
				"cat -n " + BASE + ".csv | sort -k2 -k1n  | uniq -f1 | sort -nk1,1 | cut -f2- > " + BASE + "_data.csv");
		pb.directory(new File(BASEFOLDER));
		Process process = pb.start();
		process.waitFor();
	}
	
	/**
	 * DATA + INPUT --> FIXDATA.
	 * Calculates individual setC sizes from the JSON.
	 * @throws IOException .
	 */
	public static void fixData() throws IOException {
		//Read from file.
		JSONObject json;
		try (BufferedReader in = Files.newBufferedReader(new File(INPUT).toPath())) {
			json = new JSONObject(new JSONTokener(in));
		}
		
		JSONArray transactions = json.getJSONArray("transactions2");
		
		List<CSVRecord> records;
		try (BufferedReader in = Files.newBufferedReader(new File(DATA).toPath());
			 CSVParser csvp = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in)) {
			records = csvp.getRecords();
		}
		
		//Print out setCs
		try (BufferedWriter out = Files.newBufferedWriter(new File(FIXDATA).toPath(), OVERRIDE);
			 CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(FIXDATA_HEADERS))) {
			
			for (CSVRecord record : records) {
				printer.print(record.get(0));
				printer.print(record.get(1));
				printer.print(record.get(2));
				
				int nr = Integer.parseInt(record.get("numberOfTransactions"));
				Map<Integer, List<String>> setCs = calculateSetCUpTill(transactions, nr);
				
				//Print avg
				printer.print(((double) setCs.values().stream().mapToInt(List::size).sum()) / setCs.size());
				
				//Print individual sizes
				for (int i = 0; i < N; i++) {
					printer.print(setCs.getOrDefault(i, Collections.EMPTY_LIST).size());
				}
				
				//Print complete sets
				for (int i = 0; i < N; i++) {
					printer.print(setCs.getOrDefault(i, Collections.EMPTY_LIST).stream().collect(Collectors.joining(",")));
				}
				
				printer.println();
			}
			printer.flush();
		}
	}
	
	/**
	 * INPUT --> SETCSIZES.
	 * Calculates individual setC sizes from the JSON.
	 * @throws IOException .
	 */
	public static void getIndividualSetCSizes() throws IOException {
		//Read from file.
		JSONObject json;
		try (BufferedReader in = Files.newBufferedReader(new File(INPUT).toPath())) {
			json = new JSONObject(new JSONTokener(in));
		}
		
		//Calculate setCs
		JSONArray transactions = json.getJSONArray("transactions2");
		Map<Integer, List<String>> setCs = calculateSetCUpTill(transactions, transactions.length());
		
		//Write setc sizes
		try (BufferedWriter out = Files.newBufferedWriter(new File(SETCSIZES).toPath(), OVERRIDE);
				 CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("node", "|setC|", "setC"))) {
			
			for (int i = 0; i < N; i++) {
				printer.print(i);
				printer.print(setCs.get(i).size());
				printer.print(setCs.get(i).stream().collect(Collectors.joining(",")));
				printer.println();
			}
			
			printer.flush();
		}
	}

	/**
	 * Calculates setC from knowledge as a list of node ids (strings).
	 * @param transaction - the transaction
	 * @return - setC of the to node of this transaction
	 */
	private static List<String> calculateSetC(JSONObject transaction) {
		int nodeTo = transaction.getInt("to");
		JSONObject knowledge = transaction.getJSONObject("knowledge");
		List<String> setCN = new ArrayList<>(knowledge.keySet());
		for (int k = nodeTo - G; k < nodeTo; k++) {
			if (k < 0) {
				setCN.remove("" + (k + N));
			} else {
				setCN.remove("" + k);
			}
		}
		
		setCN.sort((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)));
		return setCN;
	}
	
	/**
	 * Calculates the largest setC for each node within transaction 0 till max. 
	 * @param transactions - the transactions
	 * @param max - the index to stop at (exclusive)
	 * @return a map of nodeid to setC (strings)
	 */
	private static Map<Integer, List<String>> calculateSetCUpTill(JSONArray transactions, int max) {
		Map<Integer, List<String>> setCs = new HashMap<>();
		
		for (int i = 0; i < max; i++) {
			JSONObject transaction = transactions.getJSONObject(i);
			
			int nodeTo = transaction.getInt("to");
			
			//Fix set C, by calculating it from knowledge
			List<String> setCN = calculateSetC(transaction);
			
			List<String> oldList = setCs.get(nodeTo);
			List<String> newList = setCN;
			if (oldList != null && oldList.size() == newList.size()) {
				continue;
			} else {
				setCs.put(nodeTo, newList);
			}
		}
		return setCs;
	}
	
	/**
	 * INPUT --> OUT1.
	 * Converts json to csv.
	 * @throws IOException .
	 */
	public static void initialConvert() throws IOException {
		//Read from file.
		JSONObject json;
		try (BufferedReader in = Files.newBufferedReader(new File(INPUT).toPath())) {
			json = new JSONObject(new JSONTokener(in));
		}
		
		//Read and convert
		JSONArray transactions = json.getJSONArray("transactions2");
		
		try (BufferedWriter out = Files.newBufferedWriter(new File(OUT1).toPath(), OVERRIDE);
			 CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(A_HEADERS))) {
			//Write to out.
			for (int i = 0; i < transactions.length(); i++) {
				JSONObject transaction = transactions.getJSONObject(i);
				
				int nodeTo = transaction.getInt("to");
				printer.print(i);
				printer.print(transaction.getInt("from"));
				printer.print(nodeTo);
				printer.print(transaction.getLong("amount"));
				printer.print(transaction.getLong("remainder"));
				printer.print(transaction.getInt("numberOfChains"));
				printer.print(transaction.getInt("numberOfBlocks"));
				JSONObject knowledge = transaction.getJSONObject("knowledge");
//				printer.print(setC.join(","));
//				printer.print(setC.toList().stream().map(o -> o.toString() + ";" + knowledge.optInt(String.valueOf(o))).collect(Collectors.joining(",")));
//				printer.print(setC.length());
				
				List<String> setCN = calculateSetC(transaction);
				
				printer.print(setCN.stream().collect(Collectors.joining(",")));
				printer.print(setCN.stream().map(s -> s + ";" + knowledge.getInt(s)).collect(Collectors.joining(",")));
				printer.print(setCN.size());
				
				for (int j = 0; j < N; j++) {
					int nodeNBlock = knowledge.optInt(String.valueOf(j));
					printer.print(nodeNBlock);
				}
				printer.println();
			}
			
			printer.flush();
		}
	}
	
	/**
	 * OUT1 --> OUT2.
	 * Sorts a converted csv by to.
	 * @throws IOException .
	 */
	public static void sort() throws IOException {
		List<CSVRecord> records;
		try (BufferedReader in = Files.newBufferedReader(new File(OUT1).toPath());
			 CSVParser csvp = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in)) {
			records = csvp.getRecords();
			records.sort((a, b) -> {
				int aNode = Integer.parseInt(a.get("to"));
				int bNode = Integer.parseInt(b.get("to"));
				if (aNode < bNode) return -1;
				if (aNode > bNode) return 1;
				
				int aId = Integer.parseInt(a.get("id"));
				int bId = Integer.parseInt(b.get("id"));
				return Integer.compare(aId, bId);
			});
		}
		
		try (BufferedWriter out = Files.newBufferedWriter(new File(OUT2).toPath(), OVERRIDE);
			 CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(A_HEADERS))) {
			
			printer.printRecords(records);
			printer.flush();
		}
	}
	
	/**
	 * OUT2 --> OUT3.
	 * Converts from a sorted csv to setC with deltas.
	 * @throws IOException .
	 */
	public static void sortedToDelta() throws IOException {
		try (BufferedReader in = Files.newBufferedReader(new File(OUT2).toPath());
			 CSVParser csvp = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
			 BufferedWriter out = Files.newBufferedWriter(new File(OUT3).toPath(), OVERRIDE);
			 CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(B_HEADERS))) {
			
			convertToNext(csvp, printer);
			printer.flush();
		}
	}

	private static void convertToNext(CSVParser csvp, CSVPrinter writer) throws IOException {
		//id, node (to), setCSize, setC, setCBlocks, setCDelta, from, amount, remainder, numberOfChains, numberOfBlocks, 
		String prevTo = null;
		String prevSetC = null;
		Map<Integer, Integer> prevMap = null;
		for (CSVRecord record : csvp) {
			String to = record.get("to");
			String setC = record.get("setCBlocks");
			
			writer.print(record.get("id"));
			writer.print(to);
			
			writer.print(record.get("|setC|"));
			writer.print(record.get("setC"));
			if (!to.equals(prevTo)) {
				//New record
				prevTo = to;
				prevSetC = setC;
				prevMap = map(setC);
				
				writeMap(prevMap, writer);
				writeMap(Collections.EMPTY_MAP, writer);
				writer.print(0);
			} else {
				if (setC.equals(prevSetC)) {
					//No delta, no change
					writeMap(prevMap, writer);
					writeMap(Collections.EMPTY_MAP, writer);
					writer.print(0);
				} else {
					Map<Integer, Integer> curMap = map(setC);
					Map<Integer, Integer> delta = calculateDelta(prevMap, curMap);
					prevMap = curMap;
					prevSetC = setC;
					
					writeMap(curMap, writer);
					writeMap(delta, writer);
					writer.print(delta.values().stream().mapToInt(Integer::intValue).sum());
				}
			}
			
			writer.print(record.get("from"));
			writer.print(record.get("amount"));
			writer.print(record.get("remainder"));
			writer.print(record.get("nrOfChains"));
			writer.print(record.get("nrOfBlocks"));
			for (int i = 0; i < N; i++) {
				writer.print(record.get("knowledge " + i));
			}
			
			writer.println();
		}
		writer.flush();
	}
	
	private static Map<Integer, Integer> map(String str) {
		if (str.isEmpty()) return Collections.EMPTY_MAP;
		
		HashMap<Integer, Integer> map = new HashMap<>();
		String[] parts = str.split(",");
		for (String s : parts) {
			String[] parts2 = s.split(";");
			map.put(Integer.parseInt(parts2[0]), Integer.parseInt(parts2[1]));
		}
		return map;
	}
	
	private static Map<Integer, Integer> calculateDelta(Map<Integer, Integer> prev, Map<Integer, Integer> cur) {
		Map<Integer, Integer> delta = new HashMap<>();
		for (Entry<Integer, Integer> eNew : cur.entrySet()) {
			Integer old = prev.get(eNew.getKey());
			if (old == null) old = 0;
			int amount = eNew.getValue() - old;
			if (amount != 0) delta.put(eNew.getKey(), amount);
		}
		return delta;
	}
	
	private static void writeMap(Map<Integer, Integer> map, CSVPrinter printer) throws IOException {
		printer.print(map.entrySet().stream().map(e -> e.getKey() + ";" + e.getValue()).collect(Collectors.joining(",")));
	}
	
	private static enum Mode {
		/**
		 * Mode 0: no grouping.
		 */
		NO_GROUPING,
		/**
		 * Mode 1: (old) grouping.
		 */
		GROUPING,
		/**
		 * Mode 2: (new) grouping, prefering other groups over genesis money.
		 */
		GROUPING_BUT_NOT_GENESIS;
	}
}
