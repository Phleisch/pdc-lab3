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

public class SkipListTest {
	
	public static final int N = (int) 1e7;  // TODO: switch to 1e7
	public static final int nOps = (int) 1e6;  // TODO: switch to 1e6
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
		
		// Check normal distribution skip list mean and variance.
		LinkedList<Integer> listNormal = skipListNormal.toList();
	    int count = (int) listNormal.parallelStream().count();
	    long sum = listNormal.parallelStream().mapToLong(i -> i).sum();
	    double mean = sum/count;
	    double std = Math.sqrt(listNormal.parallelStream().mapToDouble(i -> (Math.pow(i - mean, 2.))).sum()/count);
	    System.out.println("Normal mean and variance are: " + mean + " : " + std + "\n");
	    
		// Create uniform distribution skip list.
		LockFreeSkipList skipListUniform = new LockFreeSkipList(false);
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
		
		
	private static void completeTest(LockFreeSkipList skipListUniform, LockFreeSkipList skipListNormal) {
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
			    testOps(skipListNormal, "normal", 10);
			    testOps(skipListUniform, "uniform", 10);
			}
		}
	}
	
	
	private static List<LogWrapper> testOps(LockFreeSkipList skipList, String mode, int nTests) {
		exec = Executors.newFixedThreadPool(nThreads);
		long totalTime = 0;
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
				long t1 = System.nanoTime();
				exec.invokeAll(tasks);
				long t2 = System.nanoTime();
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
        System.out.println(mode + "|add " + fracAdd + "|remove " + fracRemove + "|contains " + fracContains + "|threads " + nThreads + "|avTime " + totalTime/(1e9*nTests) + "s");
        return logList;
	}
}

