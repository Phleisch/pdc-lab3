package testPackage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import skiplistPackage.LockFreeSkipList;

public class SkipListTestGlobalLog {
	
	public static final int N = (int) 1e5;  // TODO: switch to 1e7
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

	public static void main(String[] args) {
		// Create normal distribution skip list.
		LockFreeSkipList skipListNormal = new LockFreeSkipList(true);
		SkipListPopulator.populate(skipListNormal, N, "normal");
	    
		// Create uniform distribution skip list.
		LockFreeSkipList skipListUniform = new LockFreeSkipList(true);
		SkipListPopulator.populate(skipListUniform, N, "uniform");
	    	    
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
		testOps(skipListNormal, "normal", null, false, 10);
		testOps(skipListUniform, "uniform", null, false, 10);
	    nThreads = 46;
		testOps(skipListNormal, "normal", null, false, 10);
		testOps(skipListUniform, "uniform", null, false, 10);
	}
	
	private static void sequentialTest(LockFreeSkipList skipListUniform, LockFreeSkipList skipListNormal, 
			TreeMap<Long, Log> logUniform, TreeMap<Long, Log> logNormal) {
		// Set test configuration.
		nThreads = 48; fracAdd = 0.25; fracRemove = 0.25; fracContains = 0.5;
		LinkedList<Integer> uniformList = skipListUniform.toList();
		LinkedList<Integer> normalList = skipListNormal.toList();
		
		// Perform operations on the lists.
		testOps(skipListNormal, "normal", logNormal, true, 1);
		testOps(skipListUniform, "uniform", logUniform, true, 1);
		
		// Check logs for consistency.
		int errorCountUniform = LogChecker.checkLogs(uniformList, logUniform);
		int errorCountNormal = LogChecker.checkLogs(normalList, logNormal);
		System.out.println(errorCountNormal + " violations on the normal set!");
		System.out.println(errorCountUniform + " violations on the uniform set!");
	}

	
	private static void testOps(LockFreeSkipList skipList, String mode, TreeMap<Long, Log> log, boolean doLog, int nTests) {
		exec = Executors.newFixedThreadPool(nThreads);
        double totalTime = 0;
		for(int i = 0; i < nTests; i++) {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int j = 0; j < nThreads; j++) {
				OpsTask task = new OpsTask(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains, 
						INT_MIN, INT_MAX, INT_MEAN, INT_STD, mode, log, doLog);
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
        System.out.println(mode + "|add " + fracAdd + "|remove " + fracRemove + "|contains " + fracContains + "|threads " + nThreads + "|avTime " + totalTime/1e9*nTests + "s");
	}
}