package skiplistPackage;
import java.util.concurrent.atomic.AtomicMarkableReference;

public final class LockFreeSkipList<T> {
	static final int MAX_LEVEL = 5;
	final Node<T> head = new Node<T>(Integer.MIN_VALUE);
	final Node<T> tail = new Node<T>(Integer.MAX_VALUE);

	public LockFreeSkipList() {
		for (int i = 0; i < head.next.length; i++) {
			head.next[i] = new AtomicMarkableReference<LockFreeSkipList.Node<T>>(tail, false);
		}
	}

	public static final class Node<T> {
		final T value;
		final int key;
		final AtomicMarkableReference<Node<T>>[] next;
		private int topLevel;

		public Node(int key) {
			value = null;
			this.key = key;
			next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];
			for (int i = 0; i < next.length; i++) {
				next[i] = new AtomicMarkableReference<Node<T>>(null, false);
			}
			topLevel = MAX_LEVEL;
		}

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

	public static void main(String[] args) {
		System.out.println("Class functional");
	}
}
