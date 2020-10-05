package testPackage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import skiplistPackage.LockFreeSkipList;

public class SkipListTestGlobalLog {
	
	public static final int N = (int) 1e5;  // TODO: switch to 1e7
	public static final int nOps = (int) 1e4;  // TODO: switch to 1e6
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
		LockFreeSkipList skipListNormal = new LockFreeSkipList(true);
		SkipListPopulator.populate(skipListNormal, N, "normal");
		
		// Check normal distribution skip list mean and variance.
		LinkedList<Integer> listNormal = skipListNormal.toList();
	    int count = (int) listNormal.parallelStream().count();
	    long sum = listNormal.parallelStream().mapToLong(i -> i).sum();
	    double mean = sum/count;
	    double std = Math.sqrt(listNormal.parallelStream().mapToDouble(i -> (Math.pow(i - mean, 2.))).sum()/count);
	    System.out.println("Normal mean and variance are: " + mean + " : " + std + "\n");
	    
		// Create uniform distribution skip list.
		LockFreeSkipList skipListUniform = new LockFreeSkipList(true);
		SkipListPopulator.populate(skipListUniform, N, "uniform");
		
		// Check uniform distribution skip list mean and variance.
		LinkedList<Integer> listUniform = skipListUniform.toList();
	    count = (int) listUniform.parallelStream().count();
	    sum = listUniform.parallelStream().mapToLong(i -> i).sum();
	    double meanUniform = sum/count;
	    std = Math.sqrt(listUniform.parallelStream().mapToDouble(i -> (Math.pow(i - meanUniform, 2.))).sum()/count);
	    System.out.println("Uniform mean and variance are: " + meanUniform + " : " + std + "\n");
	    	    
	    // Mixed operation test.
	    System.out.println("Starting comparison test.\n");
	    // comparisonTest(skipListUniform, skipListNormal);
	    System.out.println("\nStarting sequential check test.\n");
	    TreeMap<Long, Log> logUniform = new TreeMap<Long, Log>();
	    TreeMap<Long, Log> logNormal = new TreeMap<Long, Log>();
	    sequentialTest(skipListUniform, skipListNormal, logUniform, logNormal);
        System.out.println("Finished testing.");
	}
		
		
	private static void comparisonTest(LockFreeSkipList skipListUniform, LockFreeSkipList skipListNormal) {
		nThreads = 2; fracAdd = 0.25; fracRemove = 0.25; fracContains = 0.5;
		testNormalOps(skipListNormal, null, false, 10);
	    testUniformOps(skipListUniform, null, false, 10);
	    nThreads = 46;
		testNormalOps(skipListNormal, null, false, 10);
	    testUniformOps(skipListUniform, null, false, 10);
	}
	
	private static void sequentialTest(LockFreeSkipList skipListUniform, LockFreeSkipList skipListNormal, 
			TreeMap<Long, Log> logUniform, TreeMap<Long, Log> logNormal) {
		// Set test configuration.
		nThreads = 48; fracAdd = 0.25; fracRemove = 0.25; fracContains = 0.5;
		LinkedList<Integer> uniformList = skipListUniform.toList();
		LinkedList<Integer> normalList = skipListNormal.toList();
		
		// Perform operations on the lists.
		testNormalOps(skipListNormal, logNormal, true, 1);
		testUniformOps(skipListUniform, logUniform, true, 1);
		
		// Check logs for consistency.
		int errorCountUniform = LogChecker.checkLogs(uniformList, logUniform);
		int errorCountNormal = LogChecker.checkLogs(normalList, logNormal);
		System.out.println(errorCountNormal + " violations on the normal set!");
		System.out.println(errorCountUniform + " violations on the uniform set!");
	}
	
	private static void testNormalOps(LockFreeSkipList skipList, TreeMap<Long, Log> log, boolean doLog, int nTests) {
		exec = Executors.newFixedThreadPool(nThreads);
        double totalTime = 0;
		for(int i = 0; i < nTests; i++) {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int j = 0; j < nThreads; j++) {
	        	NormalOpsTask task = new NormalOpsTask(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains,
	        			log, doLog);
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
        System.out.println("Normal|add " + fracAdd + "|remove " + fracRemove + "|contains " + fracContains + "|threads " + nThreads + "|avTime " + totalTime/1e9*nTests + "s");
	}
	
	
	private static void testUniformOps(LockFreeSkipList skipList, TreeMap<Long, Log> log, boolean doLog, int nTests) {
		exec = Executors.newFixedThreadPool(nThreads);
        double totalTime = 0;
		for(int i = 0; i < nTests; i++) {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int j = 0; j < nThreads; j++) {
				UniformOpsTask task = new UniformOpsTask(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains, 
						log, doLog);
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
        System.out.println("Uniform|add " + fracAdd + "|remove " + fracRemove + "|contains " + fracContains + "|threads " + nThreads + "|avTime " + totalTime/1e9*nTests + "s");
	}

	
	static class NormalOpsTask implements Callable<Void>{
		private int nOps;
		private double addInterval, removeInterval, containsInterval;
		private LockFreeSkipList skipList;
		private TreeMap<Long, Log> log;
		boolean doLog;
		
		public NormalOpsTask(LockFreeSkipList skipList, int nOps, double fracAdd, 
				double fracRemove, double fracContains, TreeMap<Long, Log> log, boolean doLog) {
			this.nOps = nOps;
			this.addInterval = fracAdd;
			this.removeInterval = fracAdd + fracRemove;
			this.containsInterval = fracAdd + fracRemove + fracContains;
			this.skipList = skipList;
			this.log = log;
			this.doLog = doLog;
		}
				
		public Void call() {
			for(int i = 0; i < nOps; i++) {
				double rnd = Math.random()*containsInterval;
				int next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
				while(INT_MIN <= next && next <= INT_MAX) {
					next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
				}
				if(rnd < addInterval) {
					Log tmpLog = skipList.add(next);
					if(doLog)
						log.put(tmpLog.timestamp, tmpLog);
				}else if(rnd < (removeInterval)) {
					Log tmpLog = skipList.remove(next);
					if(doLog)
						log.put(tmpLog.timestamp, tmpLog);
				}else {
					Log tmpLog = skipList.contains(next);
					if(doLog)
						log.put(tmpLog.timestamp, tmpLog);
				}
			}
			return null;
		}
	}
	
	
	static class UniformOpsTask implements Callable<Void>{
		private int nOps;
		private double addInterval, removeInterval, containsInterval;
		private LockFreeSkipList skipList;
		private TreeMap<Long, Log> log;
		boolean doLog;
		
		public UniformOpsTask(LockFreeSkipList skipList, int nOps, double fracAdd, 
				double fracRemove, double fracContains, TreeMap<Long, Log> log, boolean doLog) {
			this.nOps = nOps;
			this.addInterval = fracAdd;
			this.removeInterval = fracAdd + fracRemove;
			this.containsInterval = fracAdd + fracRemove + fracContains;
			this.skipList = skipList;
			this.log = log;
			this.doLog = doLog;
		}
				
		public Void call() {
			for(int i = 0; i < nOps; i++) {
				double rnd = Math.random()*containsInterval;
				int next = r.nextInt(INT_MAX);
				if(rnd < addInterval) {
					Log tmpLog = skipList.add(next);
					if(doLog)
						log.put(tmpLog.timestamp, tmpLog);

				}else if(rnd < (removeInterval)) {
					Log tmpLog = skipList.remove(next);
					if(doLog)
						log.put(tmpLog.timestamp, tmpLog);

				}else {
					Log tmpLog = skipList.contains(next);
					if(doLog)
						log.put(tmpLog.timestamp, tmpLog);
				}
			}
			return null;
		}
	}
}

