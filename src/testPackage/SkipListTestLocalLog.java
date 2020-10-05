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

	public static void main(String[] args) {
		// Create normal distribution skip list.
		LockFreeSkipList skipListNormal = new LockFreeSkipList(false);
		SkipListPopulator.populate(skipListNormal, N, "normal");
	    
		// Create uniform distribution skip list.
		LockFreeSkipList skipListUniform = new LockFreeSkipList(false);
		SkipListPopulator.populate(skipListUniform, N, "uniform");
	    	    
	    // Mixed operation test.
	    System.out.println("Starting mixed operations on the list.\n");
	    completeTest(skipListUniform, skipListNormal);
	    
        System.out.println("Finished testing.");
	}
		
		
	private static void completeTest(LockFreeSkipList skipListUniform, LockFreeSkipList skipListNormal) {
		testOps(skipListUniform, "uniform", null, false, 1);
		testOps(skipListNormal, "normal", null, false, 1);
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

