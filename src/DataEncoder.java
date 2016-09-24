import com.carrotsearch.hppc.*;

/*maps string values in transaction to integers*/

public class DataEncoder {
	private IntObjectMap<String> encodedAlphabet;
	public ObjectIntMap<String> decodedAlphabet;
	private int alphabetSize;

	public DataEncoder() {
		this.encodedAlphabet = new IntObjectScatterMap<>();
		this.decodedAlphabet = new ObjectIntScatterMap<>();
		this.alphabetSize = 0;
	}

	public IntArrayList encodeTransaction(String[] line) {
		for (String str : line)
			if (!this.decodedAlphabet.containsKey(str)) {
				alphabetSize++;	
				
				encodedAlphabet.put(alphabetSize, str);
				decodedAlphabet.put(str, alphabetSize);
				
				
			}
		
		IntArrayList transaction = new IntArrayList();
		for (int i = 0; i < line.length; i++)
			transaction.add(decodedAlphabet.get(line[i]));
		//Arrays.sort(transaction.toArray());
		return transaction;
	}
	
	
	
	public IntObjectMap<String> getEncodedAlphabet() {
		return encodedAlphabet;
	}

}
