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
	
	public static final int N = (int) 1e7;  // TODO: switch to 1e7
	public static final int nOps = (int) 1e6;  // TODO: switch to 1e6
	public static double fracAdd = 0.1;
	public static double fracRemove = 0.1;
	public static double fracContains = 0.8;
	public static int nThreads = 48;
	private static ExecutorService exec;
	
	public static int INT_MIN = 0;
	public static int INT_MAX = (int) 1e7;  // 1e7
	public static int INT_MEAN = (int) 5e6;  // 5e6
	public static int INT_STD = (int) 5e6 / 3;  // 5e6 / 3

	public static void main(String[] args) {
		// Create normal distribution skip list.
		LockFreeSkipList skipListNormal = new LockFreeSkipList(false);
		SkipListPopulator.populate(skipListNormal, N, "normal");
	    
		// Create uniform distribution skip list.
		LockFreeSkipList skipListUniform = new LockFreeSkipList(false);
		SkipListPopulator.populate(skipListUniform, N, "uniform");
	    	    
	    // Mixed operation test.
	    System.out.println("\nStarting consistency test.\n");
	    consistencyTest(skipListUniform, skipListNormal);
	    INT_MAX = (int) 1e6; INT_MEAN = (int) 5e5; INT_STD = (int) 5e5 / 3;
	    consistencyTest(skipListUniform, skipListNormal);
	    INT_MAX = (int) 1e5; INT_MEAN = (int) 5e4; INT_STD = (int) 5e4 / 3;
	    consistencyTest(skipListUniform, skipListNormal);
        System.out.println("Finished testing.");
	}
		
		
	private static void consistencyTest(LockFreeSkipList skipListUniform, LockFreeSkipList skipListNormal) {
		LinkedList<Integer> uniformList = skipListUniform.toList();
		LinkedList<Integer> normalList = skipListNormal.toList();
		List<LogWrapper> logListUniform = testOps(skipListUniform, "uniform", 1);
		List<LogWrapper> logListNormal = testOps(skipListNormal, "normal", 1);
		TreeMap<Long, Log> completeUniformLog = new TreeMap<Long, Log>();
		TreeMap<Long, Log> completeNormalLog = new TreeMap<Long, Log>();
		for(LogWrapper log : logListUniform) {
			completeUniformLog.putAll(log.toTreeMap());
		}
		for(LogWrapper log : logListNormal) {
			completeNormalLog.putAll(log.toTreeMap());
		}
		int errCnt;
		errCnt = LogChecker.checkLogs(uniformList, completeUniformLog);
		System.out.println("Local log uniform error count: " + errCnt);
		errCnt = LogChecker.checkLogs(normalList, completeNormalLog);
		System.out.println("Local log normal error count: " + errCnt);
	}
	
	private static List<LogWrapper> testOps(LockFreeSkipList skipList, String mode, int nTests) {
		exec = Executors.newFixedThreadPool(nThreads);
		List<LogWrapper> logList = new ArrayList<>();
		for(int i = 0; i < nTests; i++) {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int j = 0; j < nThreads; j++) {
				TreeMap<Long, Log> log = new TreeMap<Long, Log>();
				LogWrapper logWrapper = new LogWrapper(log);
				logList.add(logWrapper);
				OpsTask task = new OpsTask(skipList, (int) nOps/nThreads, fracAdd, fracRemove, fracContains, 
						INT_MIN, INT_MAX, INT_MEAN, INT_STD, mode, logWrapper, true);
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

