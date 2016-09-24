import java.util.Collection;
import com.carrotsearch.hppc.IntArrayList;

/*represent single transaction as transaction id - integer array pair*/
public class Transaction {
	int tid;
	IntArrayList transaction;
	
	public Transaction(int tid, IntArrayList transaction) {
		this.tid = tid;
		this.transaction = transaction;
	}
	
	public String toString() {
		return this.tid+" "+this.transaction.toString();
	}
	
	public boolean containsAll(Collection<Integer> c){
		for (Integer e : c)
            if (!this.transaction.contains(e))
                return false;
        return true;
	}
	
}
