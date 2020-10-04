package skiplistPackage;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.locks.ReentrantLock;

import testPackage.Log;


// Most of the implementation directly from the code of H&S 14.4
public final class LockFreeSkipList<Integer> {
	static final int MAX_LEVEL = 32;
	final Node<Integer> head = new Node<Integer>(Integer.MIN_VALUE);
	final Node<Integer> tail = new Node<Integer>(Integer.MAX_VALUE);
	final ReentrantLock linearizationLock = new ReentrantLock();
	private boolean useLinearizationLock;

	public LockFreeSkipList(boolean useLinearizationLock) {
		for (int i = 0; i < head.next.length; i++) {
			head.next[i] = new AtomicMarkableReference<LockFreeSkipList.Node<Integer>>(tail, false);
		}

		this.useLinearizationLock = useLinearizationLock;
	}

	public static final class Node<Integer> {
		final T value;
		final int key;
		final AtomicMarkableReference<Node<Integer>>[] next;
		private int topLevel;

		@SuppressWarnings("unchecked")
		public Node(int key) {
			value = null;
			this.key = key;
			next = (AtomicMarkableReference<Node<Integer>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];
			for (int i = 0; i < next.length; i++) {
				next[i] = new AtomicMarkableReference<Node<Integer>>(null, false);
			}
			topLevel = MAX_LEVEL;
		}

		@SuppressWarnings("unchecked")
		public Node(T x, int height) {
			value = x;
			key = x.hashCode();
			next = (AtomicMarkableReference<Node<Integer>>[]) new AtomicMarkableReference[height + 1];
			for (int i = 0; i < next.length; i++) {
				next[i] = new AtomicMarkableReference<Node<Integer>>(null, false);
			}
			topLevel = height;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Log<Integer> add(T x) {		
		int topLevel = randomLevel();
		int bottomLevel = 0;
		Node<Integer>[] preds = (Node<Integer>[]) new Node[MAX_LEVEL + 1];
		Node<Integer>[] succs = (Node<Integer>[]) new Node[MAX_LEVEL + 1];
		while (true) {
			

			StampedBool stampedBool = find(x, preds, succs);  // Linearization point failed.
			boolean found = stampedBool.success;
			long linTime = stampedBool.timestamp;
			if (found) {
				return new Log<Integer>((Integer) x, "add", false, linTime);
			} else {
				Node<Integer> newNode = new Node<Integer>(x, topLevel);
				for (int level = bottomLevel; level <= topLevel; level++) {
					Node<Integer> succ = succs[level];
					newNode.next[level].set(succ, false);
				}
				Node<Integer> pred = preds[bottomLevel];
				Node<Integer> succ = succs[bottomLevel];
				
				if (useLinearizationLock)
					linearizationLock.lock();
				if(!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false)) {  // Linearization point success.
					if (useLinearizationLock)
						linearizationLock.unlock();
					continue;
				}
				linTime = System.nanoTime();
				if (useLinearizationLock)
					linearizationLock.unlock();

				for(int level = bottomLevel+1; level <= topLevel; level++) {
					while(true) {
						pred = preds[level];
						succ = succs[level];
						if(pred.next[level].compareAndSet(succ, newNode, false, false))
							break;
						find(x, preds, succs);
					}
				}
				return new Log<Integer>((Integer) x, "add", true, linTime);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public Log<Integer> remove(T x) {
		int bottomLevel = 0;
		Node<Integer>[] preds = (Node<Integer>[]) new Node[MAX_LEVEL + 1];
		Node<Integer>[] succs = (Node<Integer>[]) new Node[MAX_LEVEL + 1];
		Node<Integer> succ;
		while(true) {
			StampedBool stampedBool = find(x, preds, succs);  // Linearization point failed.
			boolean found = stampedBool.success;
			long linTime = stampedBool.timestamp;
			if(!found) {
				return new Log<Integer>((Integer) x, "remove", false, linTime);
			} else {
				Node<Integer> nodeToRemove = succs[bottomLevel];
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
					
					if (useLinearizationLock)
						linearizationLock.lock();
					boolean iMarkedIt = nodeToRemove.next[bottomLevel].compareAndSet(succ, succ, false, true);  // Linearization point success.
					linTime = System.nanoTime();
					if (useLinearizationLock)
						linearizationLock.unlock();

					succ = succs[bottomLevel].next[bottomLevel].get(marked);
					if(iMarkedIt) {
						find(x, preds, succs);
						return new Log<Integer>((Integer) x, "remove", true, linTime);
					}
					else if(marked[0]) {
						return new Log<Integer>((Integer) x, "remove", false, 0);  // Linearization point outside function, omit log.
					}
				}
			}
		}
	}
	
	 StampedBool find(T x, Node<Integer>[] preds, Node<Integer>[] succs) {
		long linTime = 0;
		
		int bottomLevel = 0;
		int key = x.hashCode();
		boolean[] marked = {false};
		boolean snip;
		Node<Integer> pred = null, curr = null, succ = null;
		retry:
			while (true) {
				pred = head;
				for(int level = MAX_LEVEL; level >= bottomLevel; level--) {
					if (useLinearizationLock)
						linearizationLock.lock();
					curr = pred.next[level].getReference();  // Linearization point if last.
					linTime = System.nanoTime();
					if (useLinearizationLock)
						linearizationLock.unlock();
					while (true) {
						succ = curr.next[level].get(marked);
						while(marked[0]) {
							snip = pred.next[level].compareAndSet(curr, succ, false, false);
							if(!snip) continue retry;
							
							if (useLinearizationLock)
								linearizationLock.lock();
							curr = pred.next[level].getReference();  // Linearization point if last.
							linTime = System.nanoTime();
							
							if (useLinearizationLock)
								linearizationLock.unlock();

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
	
	public Log<Integer> contains(T x) {
		long linTime = 0;
		
		int bottomLevel = 0;
		int v = x.hashCode();
		boolean[] marked = {false};
		Node<Integer> pred = head, curr = null, succ = null;
		for(int level = MAX_LEVEL; level >= bottomLevel; level--) {
			
			if (useLinearizationLock)
				linearizationLock.lock();
			curr = pred.next[level].getReference();  // Linearization point if last.
			linTime = System.nanoTime();
			if (useLinearizationLock)
				linearizationLock.unlock();
			while(true) {
				succ = curr.next[level].get(marked);
				while(marked[0]) {
					
					if (useLinearizationLock)
						linearizationLock.lock();
					curr = pred.next[level].getReference();  // Linearization point if last.
					linTime = System.nanoTime();
					if (useLinearizationLock)
						linearizationLock.unlock();

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
		return new Log<Integer>((Integer) x, "contains", (curr.key == v), linTime);
	}
	
	public LinkedList<Integer> toList() {
		LinkedList<Integer> list = new LinkedList<Integer>();
		Node<Integer> currNode = head.next[0].getReference();
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
		public long timestamp;
		
		public StampedBool(boolean success, long timestamp) {
			this.success = success;
			this.timestamp = timestamp;
		}
	}
}
