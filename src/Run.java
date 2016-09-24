import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Scanner;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.IntObjectScatterMap;
import com.carrotsearch.hppc.IntScatterSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.IntObjectCursor;


public class Run {
	DataEncoder dataEncoder;
	ArrayDeque<String[]> dataToUpdate;
	int WINDOW_SIZE;
	String REGEX = " ";
	int windowCount;
	double buildCetTime;
	double runningTime;
	int minSupport;
	String fileName;
	
	public Run(String fileName,int windowSize, int windowCount, double minSupportPercent){
		this.dataEncoder = new DataEncoder();
		this.dataToUpdate = new ArrayDeque<>();
		this.WINDOW_SIZE = windowSize;
		this.windowCount = windowCount;
		this.minSupport = (int) (minSupportPercent/100*windowSize);
		this.fileName = fileName;
		
	}
	
	public void printToFile(String outFileName,MomentAlgorithm m, int numOfNodes) {
		try {

			PrintWriter pw = new PrintWriter(new File(outFileName));
			pw.println("Support: " + this.minSupport+"("+((double)this.minSupport)/this.WINDOW_SIZE+")");
			pw.println("Running time: " + this.runningTime);
			pw.println("CET building time: "+this.buildCetTime);
			pw.println("Number of nodes in CET: "+numOfNodes);
			pw.println("Number of closed nodes: "+ m.hashTable.size());
			
			for (IntObjectCursor<Node> entry : m.hashTable) {
				pw.println(entry.key + " " + entry.value.toString());
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public IntObjectMap<IntSet> prepareData() {
		IntObjectMap<IntSet> data = new IntObjectScatterMap<>();
		int tid = 0;
		try (Scanner scanner = new Scanner(Paths.get(fileName))) {
			while (scanner.hasNextLine()) {
				tid++;
				String[] line = scanner.nextLine().split(REGEX);

				IntArrayList transaction = dataEncoder.encodeTransaction(line);
				
				if (tid <= WINDOW_SIZE) {
					for (IntCursor i : transaction) {
						
						if (data.containsKey(i.value)) {
							data.get(i.value).add(tid);

						} else {
							
							IntScatterSet list = new IntScatterSet();
							list.add(tid);
							data.put(i.value, list);
						}

					}
				} else {
					
					if (dataToUpdate.size() < windowCount)
						dataToUpdate.add(line);
					else
						return data;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	public void run() {
		long algStart = System.currentTimeMillis();
		MomentAlgorithm ma = new MomentAlgorithm(this.WINDOW_SIZE);
		int minsup =  this.minSupport;

		Node root = new Node();
		root.item = (-100);
		IntObjectMap<IntSet> data = prepareData();
		System.out.println("Number of distinct items in window :"+data.size());
		System.out.println("Number of disticnt items over dataset :"+dataEncoder.getEncodedAlphabet().keys().size());

		for (IntCursor x : data.keys()) {
			Node child = root.createChild(x.value);
			//System.out.println(x.value);
			int[] frequencyTidSumPair = ma.calculateFrequencyTidSum(data, child.getItemSet());
			child.frequency = frequencyTidSumPair[0];
			child.tidSum = frequencyTidSumPair[1];
		}
		
		long exploreStart = System.currentTimeMillis();
		root.children.values().forEach(x -> ma.explore(x, data, minsup));
		long exploreEnd = System.currentTimeMillis();
		this.buildCetTime = (exploreEnd - exploreStart)/1000.0;
		System.out.println("CET built: " + (this.buildCetTime)+"sec");
		
		int newTid = WINDOW_SIZE;
		int oldTid = 0;
		for (String[] t : dataToUpdate) {
			IntArrayList transaction = dataEncoder.encodeTransaction(t);
			newTid++;
			oldTid++;
			Transaction newTransaction = ma.addTransaction(data, transaction, newTid);
			ma.add(root, newTransaction, data, minsup);
			Transaction oldTransaction = ma.transactionToDelete(data, oldTid);
			ma.delete(root, oldTransaction, data, minsup);
			ma.deleteTransaction(data, oldTransaction);
		}

		long algEnd = System.currentTimeMillis();
		if (this.windowCount != 0)
			this.runningTime = (algEnd - algStart) / 1000.0/this.windowCount;
		
		System.out.println("Finished, avg run time over "+windowCount+" windows: "+ runningTime+"sec");
		System.out.println("Number of closed itemsets: "+ma.hashTable.size());
		printToFile(this.fileName.substring(0, fileName.length()-4)+minsup+"Out.txt", ma, ma.numOfNodes(root));

	}

	public static void main(String[] args) {
		if(args.length==0){
			System.out.println("Please provide args: dataFile, windowSize, windowCount, minsup(double)(e.g 1.0 - 1%)");
			System.exit(0);}
		String filename = args[0];
		int windowSize = Integer.parseInt(args[1]);
		int windowCount = Integer.parseInt(args[2]);
		double minSupPercent  = Double.parseDouble(args[3]);
		if(windowSize<=0){
			System.out.println("Window size should not be 0 or less!");
			System.exit(0);
		}
			
		if(filename==null){
			System.out.println("Default dataset will be run");
			filename = "data.txt";
		}
		if(windowCount==0)
			System.out.println("No windows count specified, only CET building");
		if(minSupPercent==0.0){
			System.out.println("No min support specified, will run with support 1%");
		}
		
		Run  run = new Run(filename, windowSize, windowCount,minSupPercent);
		run.run();

	}
}
