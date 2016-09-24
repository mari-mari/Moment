import java.util.*;

import com.carrotsearch.hppc.IntCollection;

public class Node implements Comparable<Node> {

	Node parent;
	TreeMap<Integer, Node> children;
	NodeLabel label;
	int item;
	int frequency;
	int tidSum;
	TreeSet<Integer> itemSet;
	IntCollection records;

	public Node() {
		this.parent = null;
		this.children = new TreeMap<>();
		this.itemSet = null;

	}

	public Node createChild(int item) {
		Node child = new Node();
		child.item = item;
		this.children.put(item, child);
		child.parent = this;
		return child;
	}

	private TreeSet<Integer> traverse(Node node, Node parent) {
		TreeSet<Integer> itemSet = new TreeSet<>();

		while (node.parent != null) {
			if (parent != null) {
				itemSet.add(node.item);
			}
			node = node.parent;
		}
		return itemSet;
	}

	public TreeSet<Integer> getItemSet() {
		if (this.itemSet == null) {

			TreeSet<Integer> itemSet = traverse(this, this.parent);
			this.itemSet = itemSet;
		}
		return this.itemSet;
	}

	public boolean isFrequent(int minsup) {
		return this.frequency >= minsup;
	}

	public boolean isNewlyFrequent(int minsup) {
		return (this.frequency >= minsup)
				&& (this.label == null || (this.label.equals(NodeLabel.infrequent_gateway_node)));
	}

	public boolean isNewlyPromising(int minsup) {
		return (this.frequency >= minsup) && (this.label.equals(NodeLabel.unpromising_gateway_node));
	}

	public boolean isNewlyInfrequent(int minsup) {
		return (this.frequency < minsup)
				&& (this.label != null && !this.label.equals(NodeLabel.infrequent_gateway_node));
	}

	public String toString() {
		if (this.label == null)
			return this.getItemSet().toString() + " " + this.frequency;

		return this.label.toString() + " " + this.getItemSet().toString() + ":  frequency = " + this.frequency
				+ ": tidSum = " + this.tidSum;
	}

	public void updateTidSum(int update) {
		this.tidSum += update;
	}

	public void updateFrequency(int update) {
		this.frequency += update;
	}

	public boolean contains(Node node) {
		if(!node.getItemSet().isEmpty())
		return this.getItemSet().containsAll(node.getItemSet());
		else return false;
	}

	@Override
	public int compareTo(Node o) {

		return this.getItemSet().toString().substring(1, this.getItemSet().toString().length() - 1)
				.compareTo(o.getItemSet().toString().substring(1, o.getItemSet().toString().length() - 1));
	}

}
