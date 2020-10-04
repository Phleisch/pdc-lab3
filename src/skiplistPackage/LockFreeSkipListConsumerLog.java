package skiplistPackage;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicMarkableReference;


// Most of the implementation directly from the code of H&S 14.4
public final class LockFreeSkipListConsumerLog<T> {
	static final int MAX_LEVEL = 32;
	final Node<T> head = new Node<T>(Integer.MIN_VALUE);
	final Node<T> tail = new Node<T>(Integer.MAX_VALUE);

	public LockFreeSkipListConsumerLog() {
		for (int i = 0; i < head.next.length; i++) {
			head.next[i] = new AtomicMarkableReference<LockFreeSkipListConsumerLog.Node<T>>(tail, false);
		}
	}

	public static final class Node<T> {
		final T value;
		final int key;
		final AtomicMarkableReference<Node<T>>[] next;
		private int topLevel;

		@SuppressWarnings("unchecked")
		public Node(int key) {
			value = null;
			this.key = key;
			next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];
			for (int i = 0; i < next.length; i++) {
				next[i] = new AtomicMarkableReference<Node<T>>(null, false);
			}
			topLevel = MAX_LEVEL;
		}

		@SuppressWarnings("unchecked")
		public Node(T x, int height) {
			value = x;
			key = x.hashCode();
			next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[height + 1];
			for (int i = 0; i < next.length; i++) {
				next[i] = new AtomicMarkableReference<Node<T>>(null, false);
			}
			topLevel = height;
		}
	}
	
	@SuppressWarnings("unchecked")
	public boolean add(T x) {		
		int topLevel = randomLevel();
		int bottomLevel = 0;
		Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
		Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
		while (true) {
			StampedBool stampedBool = find(x, preds, succs);  // Linearization point failed.
			boolean found = stampedBool.success;
			double linTime = stampedBool.timestamp;
			if (found) {
				return false;
			} else {
				Node<T> newNode = new Node<T>(x, topLevel);
				for (int level = bottomLevel; level <= topLevel; level++) {
					Node<T> succ = succs[level];
					newNode.next[level].set(succ, false);
				}
				Node<T> pred = preds[bottomLevel];
				Node<T> succ = succs[bottomLevel];
				if(!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false)) {  // Linearization point success.
					continue;
				}
				linTime = System.nanoTime();
				for(int level = bottomLevel+1; level <= topLevel; level++) {
					while(true) {
						pred = preds[level];
						succ = succs[level];
						if(pred.next[level].compareAndSet(succ, newNode, false, false))
							break;
						find(x, preds, succs);
					}
				}
				return true;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public boolean remove(T x) {
		int bottomLevel = 0;
		Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
		Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];
		Node<T> succ;
		while(true) {
			StampedBool stampedBool = find(x, preds, succs);  // Linearization point failed.
			boolean found = stampedBool.success;
			double linTime = stampedBool.timestamp;
			if(!found) {
				return false;
			} else {
				Node<T> nodeToRemove = succs[bottomLevel];
				for(int level = nodeToRemove.topLevel; level >= bottomLevel+1; level--) {
					boolean[] marked = {false};
					succ = nodeToRemove.next[level].get(marked);
					while(!marked[0]) {
						nodeToRemove.next[level].compareAndSet(succ, succ, false, true);
						succ = nodeToRemove.next[level].get(marked);
					}
				}
				boolean[] marked = {false};
				succ = nodeToRemove.next[bottomLevel].get(marked);
				while(true) {
					boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ, false, true);  // Linearization point success.
					linTime = System.nanoTime();
					succ = succs[bottomLevel].next[bottomLevel].get(marked);
					if(iMarkedIt) {
						find(x, preds, succs);
						return true;
					}
					else if(marked[0]) return false;
				}
			}
		}
	}
	
	 StampedBool find(T x, Node<T>[] preds, Node<T>[] succs) {
		double linTime = 0;
		
		int bottomLevel = 0;
		int key = x.hashCode();
		boolean[] marked = {false};
		boolean snip;
		Node<T> pred = null, curr = null, succ = null;
		retry:
			while (true) {
				pred = head;
				for(int level = MAX_LEVEL; level >= bottomLevel; level--) {
					curr = pred.next[level].getReference();  // Linearization point if last.
					linTime = System.nanoTime();
					while (true) {
						succ = curr.next[level].get(marked);
						while(marked[0]) {
							snip = pred.next[level].compareAndSet(curr, succ, false, false);
							if(!snip) continue retry;
							curr = pred.next[level].getReference();  // Linearization point if last.
							linTime = System.nanoTime();
							succ = curr.next[level].get(marked);
						}
						if(curr.key < key){
							pred = curr; curr = succ;
						} else {
							break;
						}
					}
					preds[level] = pred;
					succs[level] = curr;
				}
				return new StampedBool(curr.key == key, linTime);
			}
		}
	
	public boolean contains(T x) {
		int bottomLevel = 0;
		int v = x.hashCode();
		boolean[] marked = {false};
		Node<T> pred = head, curr = null, succ = null;
		for(int level = MAX_LEVEL; level >= bottomLevel; level--) {
			curr = pred.next[level].getReference();  // Linearization point if last.
			double linTime = System.nanoTime();
			while(true) {
				succ = curr.next[level].get(marked);
				while(marked[0]) {
					curr = pred.next[level].getReference();  // Linearization point if last.
					linTime = System.nanoTime();
					succ = curr.next[level].get(marked);
				}
				if(curr.key < v){
					pred = curr;
					curr = succ;
				} else {
					break;
				}
			}
		}
		return (curr.key == v);
	}
	
	public LinkedList<Integer> toList() {
		LinkedList<Integer> list = new LinkedList<Integer>();
		Node<T> currNode = head.next[0].getReference();
		while(currNode != null && currNode.key != Integer.MAX_VALUE) {
			list.add(currNode.key);
			currNode = currNode.next[0].getReference();
		}
		return list;
	}

	// Code from https://stackoverflow.com/questions/12067045/random-level-function-in-skip-list
	private static int randomLevel() {
	    int lvl = (int)(Math.log(1.-Math.random())/Math.log(0.5));
	    return Math.min(lvl, MAX_LEVEL);
	}
	
	private class StampedBool{
		public boolean success;
		public double timestamp;
		
		public StampedBool(boolean success, double timestamp) {
			this.success = success;
			this.timestamp = timestamp;
		}
	}
}
