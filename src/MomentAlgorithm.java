import java.util.*;

import com.carrotsearch.hppc.*;
import com.carrotsearch.hppc.cursors.*;

public class MomentAlgorithm {
	IntObjectMap<Node> hashTable;// hashTable for storing closed nodes
	int WINDOW_SIZE;

	public MomentAlgorithm(int windowSize) {
		hashTable = new IntObjectScatterMap<>();
		WINDOW_SIZE = windowSize;
	}

	private int sum(IntCollection set) {
		int sum = 0;
		for (IntCursor i : set) {
			sum += i.value;
		}
		return sum;
	}

	public static boolean disjoint(IntCollection c1, IntCollection c2) {
		if ((c1 instanceof IntSet) && !(c2 instanceof IntSet) || (c1.size() > c2.size())) {
			IntCollection tmp = c1;
			c1 = c2;
			c2 = tmp;
		}

		for (IntCursor e : c1)
			if (c2.contains(e.value))
				return false;
		return true;
	}

	public int[] calculateFrequencyTidSum(IntObjectMap<IntSet> data, TreeSet<Integer> items) {

		if (items.size() == 1) {

			int[] tuple = { data.get(items.first()).size(), sum(data.get(items.first())) };
			return tuple;

		}

		IntArrayList list = new IntArrayList(data.get(items.first()));
		for (Integer i : items) {
			if (disjoint(data.get(i), list)) {
				int[] tuple = { 0, 0 };
				return tuple;
			} else {
				if (i.equals(items.first()))
					continue;
				else
					list.retainAll((IntLookupContainer) data.get(i));
			}
		}
		int[] tuple = { list.size(), sum(list) };
		return tuple;
	}

	// private boolean leftcheck(Node node) {
	// for (ObjectCursor<Node> n : hashTable.values()) {
	// if (n.value.contains(node) && (node.compareTo(n.value) > 0) &&
	// n.value.frequency == node.frequency) {
	// return true;
	// }
	// }
	// return false;
	// }

	private boolean leftcheck(Node node) {
		Node n = this.hashTable.get(hashedKey(node.frequency,node.tidSum));
		if (n != null) {
			if (n.getItemSet().containsAll(node.getItemSet())&& (node.compareTo(n) > 0))
				return true;
		}
		return false;
	}

	public void printClosed() {
		System.out.println("Closed itemsets: ");
		System.out.println(hashTable.size());
	}


	private void removeFromHashTable(Node node) {
		// System.out.println("Node to rm "+node.toString());
		String key = Integer.toString(node.frequency) + Integer.toString(node.tidSum);
		if (hashTable.values().contains(node))
			hashTable.remove(key.hashCode());
	}

	private void updateHashTableEntry(Node node, int newTid) {
		// System.out.println("Node to up1: "+node.toString());
		
//		String oldKey = Integer.toString(node.frequency - 1) + Integer.toString(-newTid + node.tidSum);
//		String newKey = Integer.toString(node.frequency) + Integer.toString(node.tidSum);
//		Integer oldHashedKey = oldKey.hashCode();
//		Integer newHashedKey = newKey.hashCode();
		Integer oldHashedKey = hashedKey(node.frequency - 1, -newTid + node.tidSum);
		Integer newHashedKey = hashedKey(node.frequency, node.tidSum);
		
		
		hashTable.remove(oldHashedKey);
		hashTable.put(newHashedKey, node);
	}
	
	private Integer hashedKey(int frequency, int tidSum){
		String key = Integer.toString(frequency) + Integer.toString(tidSum);
		Integer hashedKey = key.hashCode();
		return hashedKey;
		
	}

	private void updateHashTableEntry2(Node node, int oldTid) {
		// System.out.println("Node to up2: "+node.toString());

//		String oldKey = Integer.toString(node.frequency + 1) + Integer.toString(node.tidSum + oldTid);
//		String newKey = Integer.toString(node.frequency) + Integer.toString(node.tidSum);
//		Integer oldHashedKey = oldKey.hashCode();
//		Integer newHashedKey = newKey.hashCode();
		Integer oldHashedKey = hashedKey(node.frequency + 1, oldTid + node.tidSum);
		Integer newHashedKey = hashedKey(node.frequency, node.tidSum);
		hashTable.remove(oldHashedKey);
		hashTable.put(newHashedKey, node);
	}

	public void explore(Node currentNode, IntObjectMap<IntSet> data, int minsup) {
		if (!currentNode.isFrequent(minsup)) {
			currentNode.label = NodeLabel.infrequent_gateway_node;
			return;
		}

		else if (leftcheck(currentNode)) {
			currentNode.label = NodeLabel.unpromising_gateway_node;
			return;
		} else {

			// siblings that are lexicographically larger
			NavigableMap<Integer, Node> rightSiblings = currentNode.parent.children.tailMap(currentNode.item, false);

			rightSiblings.forEach((x, y) -> {
				if (y.isFrequent(minsup)) {
					// Problem - creating not valid child
					TreeSet<Integer> set = new TreeSet<>();
					set.addAll(currentNode.getItemSet());
					set.add(y.item);

					int[] tuple = calculateFrequencyTidSum(data, set);

					int freq = tuple[0];
					int tidsum = tuple[1];
					if (freq > 0) {
						Node child = currentNode.createChild(y.item);
						child.frequency = freq;

						child.tidSum = tidsum;
					}
				}
			});

			currentNode.children.values().forEach(x -> explore(x, data, minsup));
			/*
			 * check, if exist child with support equal or grater than
			 * currentNode support
			 */
			boolean hasChildWithHigherFrequency = false;
			for (Node child : currentNode.children.values()) {
				if (child.frequency >= currentNode.frequency) {
					hasChildWithHigherFrequency = true;
					break;
				}

			}
			/* exist - mark node as intermediate */
			if (hasChildWithHigherFrequency)
				currentNode.label = NodeLabel.intermediate_node;
			/* otherwise mark as closed, add to hash table */
			else {
				currentNode.label = NodeLabel.closed_node;
				hashTable.put(hashedKey(currentNode.frequency,currentNode.tidSum), currentNode);
			}
		}
	}

	/*
	 * creates root, children of root from alphabet, calls explore on each of
	 * children
	 */

	public void add(Node node, Transaction newTransaction, IntObjectMap<IntSet> data, int minsup) {
		int newTid = newTransaction.tid;
		if (!(newTransaction.containsAll(node.getItemSet())) && node.item > 0)
			return;
		HashSet<Node> newlyFrequentNodes = new HashSet<>();
		for (Node child : node.children.values()) {
			if (newTransaction.containsAll(child.getItemSet())) {
				int[] tuple = calculateFrequencyTidSum(data, child.getItemSet());
				child.frequency = tuple[0];
				child.tidSum = tuple[1];
				// child.updateFrequency(1);
				// child.updateTidSum(newTid);
				// IMPORTANT to re-calculate, not update by one because of nodes
				// that are newly created

			}
		}

		for (Node child : node.children.values()) {
			if (child.isNewlyFrequent(minsup)) {
				newlyFrequentNodes.add(child);
			}
		}

		for (Node child : node.children.values()) {
			if (!child.isFrequent(minsup))
				child.label = NodeLabel.infrequent_gateway_node;
			else if (leftcheck(child))
				child.label = NodeLabel.unpromising_gateway_node;
			else if (child.isNewlyFrequent(minsup) || child.isNewlyPromising(minsup))
				explore(child, data, minsup);
			else {
				for (Node n : newlyFrequentNodes) {
					TreeSet<Integer> newItemSet = new TreeSet<>();
					newItemSet.addAll(child.getItemSet());
					newItemSet.add(n.item);

					if (newTransaction.containsAll(newItemSet))
						child.createChild(n.item);
				}

				add(child, newTransaction, data, minsup);
				if (child.label.equals(NodeLabel.closed_node)) {
					if (newTransaction.containsAll(child.getItemSet())) {
						updateHashTableEntry(child, newTid);
					}
				} else {
					int counter = 0;
					for (Node n : child.children.values())
						if (n.frequency >= child.frequency)
							counter++;
					if (counter == 0) {
						child.label = NodeLabel.closed_node;
						hashTable.put(hashedKey(child.frequency,child.tidSum), child);
					}
				}
			}
		}
		return;
	}

	public void delete(Node node, Transaction oldTransaction, IntObjectMap<IntSet> data, int minsup) {

		HashSet<Node> newlyInfrequentNodes = new HashSet<>();
		int oldTid = oldTransaction.tid;
		// ... and not a root node
		if (!(oldTransaction.containsAll(node.getItemSet())) && node.item > 0) {
			return;
		}
		for (Node child : node.children.values()) {
			if (oldTransaction.containsAll(child.getItemSet())) {
				child.updateFrequency(-1);
				child.updateTidSum(-oldTid);
			}
		}

		for (Node child : node.children.values())

			if (child.isNewlyInfrequent(minsup)) {
				newlyInfrequentNodes.add(child);

			}
		for (Node child : node.children.values()) {
			if (!newlyInfrequentNodes.contains(child) && (child.label.equals(NodeLabel.infrequent_gateway_node)
					|| child.label.equals(NodeLabel.unpromising_gateway_node))) {
				continue;
			}

			else if (newlyInfrequentNodes.contains(child)) {
				child.label = NodeLabel.infrequent_gateway_node;
				child.children.clear();
				return;
			} else if (leftcheck(child)) {
				child.label = (NodeLabel.unpromising_gateway_node);
				child.children.clear();
				return;

			} else {
				for (Node newlyInfrequentNode : newlyInfrequentNodes) {
					Collection<Node> copy = new ArrayList<>(child.children.values());
					for (Node childOfChild : copy) {
						if (childOfChild.contains(newlyInfrequentNode)) {

							child.children.remove(childOfChild.item);
							removeFromHashTable(childOfChild);
						}
					}
				}
				delete(child, oldTransaction, data, minsup);
				if (child.label.equals(NodeLabel.closed_node)) {
					boolean hasChildWithGreaterFrequency = false;
					for (Node childOfChild : child.children.values()) {
						if (childOfChild.frequency >= child.frequency) {
							hasChildWithGreaterFrequency = true;
							break;
						}
					}
					if (hasChildWithGreaterFrequency) {
						child.label = NodeLabel.intermediate_node;
						removeFromHashTable(child);
					} else {

						updateHashTableEntry2(child, oldTransaction.tid);
					}
				}
			}
		}
	}

	public void printTree(Node n) {
		if (n.children != null) {
			System.out.println(n.toString());
			n.children.values().forEach(x -> printTree(x));
		}
	}

	public int numOfNodes(Node n) {
		if (n.children.size() == 0)
			return 1;
		int sum = 1;
		for (Node c : n.children.values())
			sum += numOfNodes(c);
		return sum;
	}

	public Transaction addTransaction(IntObjectMap<IntSet> data, IntArrayList transaction, int newTid) {

		for (IntCursor i : transaction) {
			if (data.containsKey(i.value)) {
				data.get(i.value).add(newTid);
			} else {
				IntSet set = new IntScatterSet();
				set.add(newTid);
				data.put(i.value, set);
			}
		}
		return new Transaction(newTid, transaction);
	}

	public Transaction transactionToDelete(IntObjectMap<IntSet> data, int oldTid) {
		IntArrayList transaction = new IntArrayList();
		for (IntCursor i : data.keys()) {
			if (data.get(i.value).contains(oldTid)) {
				transaction.add(i.value);
			}
		}

		return new Transaction(oldTid, transaction);
	}

	public void deleteTransaction(IntObjectMap<IntSet> data, Transaction oldTransaction) {
		for (IntCursor i : oldTransaction.transaction) {
			((IntScatterSet) data.get(i.value)).remove(oldTransaction.tid);
		}
	}

}
