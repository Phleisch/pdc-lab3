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

public class SkipListTestConsumerLog {
	
	public static final int N = (int) 1e5;  // TODO: switch to 1e7
	public static final int nOps = (int) 1e2;  // TODO: switch to 1e6
	public static double fracAdd = 0.1;
	public static double fracRemove = 0.1;
	public static double fracContains = 0.8;
	public static int nThreads = 2;
	private static ExecutorService exec;
	
	public static final int INT_MIN = 0;
	public static final int INT_MAX = (int) 1e7;  // 1e7
	public static final int INT_MEAN = (int) 5e6;  // 5e6
	public static final int INT_STD = (int) 5e6 / 3;  // 5e6 / 3

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
		LinkedList<Integer> uniformList = skipListUniform.toList();
		LinkedList<Integer> normalList = skipListNormal.toList();
		List<TreeMap<Long, Log>> logListUniform = testOps(skipListUniform, "uniform", 1);
		List<TreeMap<Long, Log>> logListNormal = testOps(skipListNormal, "normal", 1);
		TreeMap<Long, Log> completeUniformLog = new TreeMap<Long, Log>();
		TreeMap<Long, Log> completeNormalLog = new TreeMap<Long, Log>();
		for(TreeMap<Long, Log> log : logListUniform) {
			completeUniformLog.putAll(log);
		}
		for(TreeMap<Long, Log> log : logListNormal) {
			completeNormalLog.putAll(log);
		}
		int errCnt;
		errCnt = LogChecker.checkLogs(uniformList, completeUniformLog);
		System.out.println("Local log uniform error count: " + errCnt);
		errCnt = LogChecker.checkLogs(normalList, completeNormalLog);
		System.out.println("Local log normal error count: " + errCnt);
	}
	
	private static List<TreeMap<Long, Log>> testOps(LockFreeSkipList skipList, String mode, int nTests) {
		exec = Executors.newFixedThreadPool(nThreads);
		List<TreeMap<Long, Log>> logList = new ArrayList<>();
		for(int i = 0; i < nTests; i++) {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int j = 0; j < nThreads; j++) {
				TreeMap<Long, Log> log = new TreeMap<Long, Log>();
				logList.add(log);
				OpsTask task = new OpsTask(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains, 
						INT_MIN, INT_MAX, INT_MEAN, INT_STD, mode, log, true);
	        	tasks.add(task);
	        }
			try {
				exec.invokeAll(tasks);
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
        return logList;
	}
}