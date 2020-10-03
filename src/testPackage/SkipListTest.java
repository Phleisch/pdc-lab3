package testPackage;
import java.util.LinkedList;

import skiplistPackage.LockFreeSkipList;

public class SkipListTest {
	public static final int N = (int) 1e3;  // TODO: switch to 1e7

	public static void main(String[] args) {
		// Create normal distribution skip list.
		LockFreeSkipList<Integer> skipListNormal = new LockFreeSkipList<Integer>();
		SkipListPopulator.populate(skipListNormal, N, "normal");
		System.out.println("Finished populating normal list.");
		
		// Check normal distribution skip list mean and variance.
		LinkedList<Integer> listNormal = skipListNormal.toList();
	    int count = (int) listNormal.parallelStream().count();
	    long sum = listNormal.parallelStream().mapToLong(i -> i).sum();
	    double mean = sum/count;
	    double std = Math.sqrt(listNormal.parallelStream().mapToDouble(i -> (Math.pow(i - mean, 2.))).sum()/count);
	    System.out.println("Normal mean and variance are: " + mean + " : " + std);
	    
		// Create uniform distribution skip list.
		LockFreeSkipList<Integer> skipListUniform = new LockFreeSkipList<Integer>();
		SkipListPopulator.populate(skipListUniform, N, "uniform");
		System.out.println("Finished populating uniform list.");
		
		// Check uniform distribution skip list mean and variance.
		LinkedList<Integer> listUniform = skipListUniform.toList();
	    count = (int) listUniform.parallelStream().count();
	    sum = listUniform.parallelStream().mapToLong(i -> i).sum();
	    double meanUniform = sum/count;
	    std = Math.sqrt(listUniform.parallelStream().mapToDouble(i -> (Math.pow(i - meanUniform, 2.))).sum()/count);
	    System.out.println("Uniform mean and variance are: " + meanUniform + " : " + std);
	}
		
}
