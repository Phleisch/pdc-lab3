package testPackage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import skiplistPackage.LockFreeSkipList;

public class SkipListTestConsumerLog {
	
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
		TreeMap<Long, Log> completeUniformLog = testOps(skipListUniform, "uniform", 1);
		TreeMap<Long, Log> completeNormalLog = testOps(skipListNormal, "normal", 1);
		int errCnt;
		errCnt = LogChecker.checkLogs(uniformList, completeUniformLog);
		System.out.println("Concurrent log uniform error count: " + errCnt);
		errCnt = LogChecker.checkLogs(normalList, completeNormalLog);
		System.out.println("Concurrent log normal error count: " + errCnt);
	}
	
	private static TreeMap<Long, Log> testOps(LockFreeSkipList skipList, String mode, int nTests) {
		exec = Executors.newFixedThreadPool(nThreads-1);
		TreeMap<Long, Log> finalLog = new TreeMap<Long, Log>();
		ConcurrentLinkedQueue<Log> concurrentLog = new ConcurrentLinkedQueue<Log>();
		LogWrapper logWrapper = new LogWrapper(concurrentLog);
		for(int i = 0; i < nTests; i++) {
			List<Callable<Void>> tasks = new ArrayList<>();
			for (int j = 0; j < nThreads-1; j++) {
				OpsTask task = new OpsTask(skipList, (int) nOps/(nThreads-1), fracAdd, fracRemove, fracContains, 
						INT_MIN, INT_MAX, INT_MEAN, INT_STD, mode, logWrapper, true);
	        	tasks.add(task);
	        }
			
			LogConsumer logConsumer = new LogConsumer(concurrentLog, finalLog);
			Thread logConsumerThread = new Thread(logConsumer);
			logConsumerThread.start();
			try {
				exec.invokeAll(tasks);
				logConsumer.stopFlag = true;
		        logConsumerThread.join();
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
        return finalLog;
	}
	
	public static class LogConsumer implements Runnable {
		private ConcurrentLinkedQueue<Log> log;
		TreeMap<Long, Log> finalLog;
		public volatile boolean stopFlag = false;
		private int size = -1;
		
		public LogConsumer(ConcurrentLinkedQueue<Log> log, TreeMap<Long, Log> finalLog) {
			this.log = log;
			this.finalLog = finalLog;
		}

	    public void run() {
	        while(true) {
	        	if(size == -1 && stopFlag) {
	        		size = log.size();
	        	}
	        	if(stopFlag && size == 0) {
	        		break;
	        	}
	        	Log tmpLog = log.poll();
	        	if(tmpLog == null)
	        			continue;
	        	finalLog.put(tmpLog.timestamp, tmpLog);
	        	if(size > 0) {
	        		size --;
	        	}
	        }
	    }
	}

}