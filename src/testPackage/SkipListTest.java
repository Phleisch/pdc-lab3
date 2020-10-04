package testPackage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import skiplistPackage.LockFreeSkipList;

public class SkipListTest {
	public static final int N = (int) 1e3;  // TODO: switch to 1e7
	public static final int nOps = (int) 1e2;  // TODO: switch to 1e6
	public static final double fracAdd = 0.1;
	public static final double fracRemove = 0.1;
	public static final double fracContains = 0.8;
	public static final int nThreads = 2;
	private static ExecutorService exec = Executors.newFixedThreadPool(nThreads);
	
	public static final int INT_MIN = 0;
	public static final int INT_MAX = (int) 1e7;
	public static final int INT_MEAN = (int) 5e6;
	public static final int INT_STD = (int) 5e6 / 3;
	private static Random r =	new Random();

	public static void main(String[] args) {
		// Create normal distribution skip list.
		LockFreeSkipList<Integer> skipListNormal = new LockFreeSkipList<Integer>();
		SkipListPopulator.populate(skipListNormal, N, "normal");
		
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
		
		// Check uniform distribution skip list mean and variance.
		LinkedList<Integer> listUniform = skipListUniform.toList();
	    count = (int) listUniform.parallelStream().count();
	    sum = listUniform.parallelStream().mapToLong(i -> i).sum();
	    double meanUniform = sum/count;
	    std = Math.sqrt(listUniform.parallelStream().mapToDouble(i -> (Math.pow(i - meanUniform, 2.))).sum()/count);
	    System.out.println("Uniform mean and variance are: " + meanUniform + " : " + std);
	    	    
	    // Mixed operation test.
	    System.out.println("Starting mixed operations on the list.");
	    testNormalOps(skipListNormal, fracAdd, fracRemove, fracContains, nOps);
	    testUniformOps(skipListUniform, fracAdd, fracRemove, fracContains, nOps);
	    
        exec.shutdown();
        try {
			exec.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        System.out.println("Shutdown.");

	}
		
	private static void testNormalOps(LockFreeSkipList<Integer> skipList, double fracAdd, double fracRemove, double fracContains, int nOps) {
        List<Callable<Void>> tasks = new ArrayList<>();
		for (int i = 0; i < nThreads; i++) {
        	NormalOpsTask task = new NormalOpsTask(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains);
        	tasks.add(task);
        }
		try {
			exec.invokeAll(tasks);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        System.out.println("Finished testing on normal list.");
	}
	
	private static void testUniformOps(LockFreeSkipList<Integer> skipList, double fracAdd, double fracRemove, double fracContains, int nOps) {
		List<Callable<Void>> tasks = new ArrayList<>();
		for (int i = 0; i < nThreads; i++) {
        	UniformOpsTask task = new UniformOpsTask(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains);
        	tasks.add(task);
        }
		try {
			exec.invokeAll(tasks);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        System.out.println("Finished testing on uniform list.");
	}

	static class NormalOpsTask implements Callable<Void>{
		private int nOps;
		private double addInterval, removeInterval, containsInterval;
		private LockFreeSkipList<Integer> skipList;
		
		public NormalOpsTask(LockFreeSkipList<Integer> skipList, int nOps, double fracAdd, double fracRemove, double fracContains) {
			this.nOps = nOps;
			this.addInterval = fracAdd;
			this.removeInterval = fracAdd + fracRemove;
			this.containsInterval = fracAdd + fracRemove + fracContains;
			this.skipList = skipList;
		}
				
		public Void call() {
			for(int i = 0; i < nOps; i++) {
				double rnd = Math.random()*containsInterval;
				int next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
				while(INT_MIN <= next && next <= INT_MAX) {
					next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
				}
				if(rnd < addInterval) {
					skipList.add(next);
				}else if(rnd < (removeInterval)) {
					skipList.remove(next);
				}else {
					skipList.contains(next);
				}
			}
			return null;
		}
	}
	
	static class UniformOpsTask implements Callable<Void>{
		private int nOps;
		private double addInterval, removeInterval, containsInterval;
		private LockFreeSkipList<Integer> skipList;
		
		public UniformOpsTask(LockFreeSkipList<Integer> skipList, int nOps, double fracAdd, double fracRemove, double fracContains) {
			this.nOps = nOps;
			this.addInterval = fracAdd;
			this.removeInterval = fracAdd + fracRemove;
			this.containsInterval = fracAdd + fracRemove + fracContains;
			this.skipList = skipList;
		}
				
		public Void call() {
			for(int i = 0; i < nOps; i++) {
				double rnd = Math.random()*containsInterval;
				int next = r.nextInt(INT_MAX);
				if(rnd < addInterval) {
					skipList.add(next);
				}else if(rnd < (removeInterval)) {
					skipList.remove(next);
				}else {
					skipList.contains(next);
				}
			}
			return null;
		}
	}
}

