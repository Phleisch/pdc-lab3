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

public class SkipListTestLocalLog {
	
	public static final int N = (int) 1e4;  // TODO: switch to 1e7
	public static final int nOps = (int) 1e2;  // TODO: switch to 1e6
	public static double fracAdd = 0.1;
	public static double fracRemove = 0.1;
	public static double fracContains = 0.8;
	public static int nThreads = 2;
	private static ExecutorService exec;
	
	public static final int INT_MIN = 0;
	public static final int INT_MAX = (int) 1e7;
	public static final int INT_MEAN = (int) 5e6;
	public static final int INT_STD = (int) 5e6 / 3;
	private static Random r =	new Random();

	public static void main(String[] args) {
		// Create normal distribution skip list.
		LockFreeSkipList<Integer> skipListNormal = new LockFreeSkipList<Integer>(false);
		SkipListPopulator.populate(skipListNormal, N, "normal");
		
		// Check normal distribution skip list mean and variance.
		LinkedList<Integer> listNormal = skipListNormal.toList();
	    int count = (int) listNormal.parallelStream().count();
	    long sum = listNormal.parallelStream().mapToLong(i -> i).sum();
	    double mean = sum/count;
	    double std = Math.sqrt(listNormal.parallelStream().mapToDouble(i -> (Math.pow(i - mean, 2.))).sum()/count);
	    System.out.println("Normal mean and variance are: " + mean + " : " + std + "\n");
	    
		// Create uniform distribution skip list.
		LockFreeSkipList<Integer> skipListUniform = new LockFreeSkipList<Integer>(false);
		SkipListPopulator.populate(skipListUniform, N, "uniform");
		
		// Check uniform distribution skip list mean and variance.
		LinkedList<Integer> listUniform = skipListUniform.toList();
	    count = (int) listUniform.parallelStream().count();
	    sum = listUniform.parallelStream().mapToLong(i -> i).sum();
	    double meanUniform = sum/count;
	    std = Math.sqrt(listUniform.parallelStream().mapToDouble(i -> (Math.pow(i - meanUniform, 2.))).sum()/count);
	    System.out.println("Uniform mean and variance are: " + meanUniform + " : " + std + "\n");
	    	    
	    // Mixed operation test.
	    System.out.println("Starting mixed operations on the list.\n");
	    completeTest(skipListUniform, skipListNormal);
	    
        System.out.println("Finished testing.");
	}
		
		
	private static void completeTest(LockFreeSkipList<Integer> skipListUniform, LockFreeSkipList<Integer> skipListNormal) {
		double[] fracAddRange = {0.1, 0.5, 0.25, 0.05};
		double[] fracRemoveRange = {0.1, 0.5, 0.25, 0.05};
		double[] fracContainsRange = {0.8, 0.0, 0.5, 0.9};
		int[] threadRange = {2, 12, 30, 48};
		for(int i = 0; i < fracAddRange.length; i++) {
			fracAdd = fracAddRange[i]; 
			fracRemove = fracRemoveRange[i]; 
			fracContains = fracContainsRange[i];
			for(int j = 0; j < threadRange.length; j++) {
				nThreads = threadRange[j];
			    testNormalOps(skipListNormal);
			    testUniformOps(skipListUniform);
			}
		}
	}
	
	
	private static void testNormalOps(LockFreeSkipList<Integer> skipList) {
		exec = Executors.newFixedThreadPool(nThreads);
        double totalTime = 0;
		for(int i = 0; i < 10; i++) {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int j = 0; j < nThreads; j++) {
	        	NormalOpsTask task = new NormalOpsTask(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains);
	        	tasks.add(task);
	        }
			try {
				double t1 = System.nanoTime();
				exec.invokeAll(tasks);
				double t2 = System.nanoTime();
				totalTime += (t2 - t1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
        exec.shutdown();
        try {
			exec.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        System.out.println("Normal|add " + fracAdd + "|remove " + fracRemove + "|contains " + fracContains + "|threads " + nThreads + "|avTime " + totalTime/1e10 + "s");
	}
	
	
	private static void testUniformOps(LockFreeSkipList<Integer> skipList) {
		exec = Executors.newFixedThreadPool(nThreads);
        double totalTime = 0;
		for(int i = 0; i < 10; i++) {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int j = 0; j < nThreads; j++) {
				UniformOpsTask task = new UniformOpsTask(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains);
	        	tasks.add(task);
	        }
			try {
				double t1 = System.nanoTime();
				exec.invokeAll(tasks);
				double t2 = System.nanoTime();
				totalTime += (t2 - t1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
        exec.shutdown();
        try {
			exec.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        System.out.println("Uniform|add " + fracAdd + "|remove " + fracRemove + "|contains " + fracContains + "|threads " + nThreads + "|avTime " + totalTime/1e10 + "s");
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

