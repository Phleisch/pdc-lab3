package testPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import skiplistPackage.LockFreeSkipList;
import skiplistPackage.LockFreeSkipListGlobalLog;
import skiplistPackage.LockFreeSkipListLog;

public class SkipListPopulator {
	public static final int INT_MIN = 0;
	public static final int INT_MAX = (int) 1e7;
	public static final int INT_MEAN = (int) 5e6;
	public static final int INT_STD = (int) 5e6 / 3;
	private static Random r =	new Random();
	
	public static LockFreeSkipList<Integer> populate(LockFreeSkipList<Integer> skipList, int n, String mode) {
		ExecutorService exec = Executors.newFixedThreadPool(24);
        List<Callable<Void>> tasks = new ArrayList<>();
		if(mode.equals("uniform") || mode.equals("normal")) {
			for (int i = 0; i < 24; i++) {
				PopulateTask task = new PopulateTask(skipList, (int) n/24, mode);
	        	tasks.add(task);
	        }
			try {
				exec.invokeAll(tasks);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}else {
			System.out.println("INVALID MODE SELECTED IN SkipListPopulator.populate!");
		}
        exec.shutdown();
        try {
			exec.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return skipList;
	}
	
	public static LockFreeSkipListLog<Integer> populate(LockFreeSkipListLog<Integer> skipList, int n, String mode) {
		ExecutorService exec = Executors.newFixedThreadPool(24);
        List<Callable<Void>> tasks = new ArrayList<>();
		if(mode.equals("uniform") || mode.equals("normal")) {
			for (int i = 0; i < 24; i++) {
				PopulateTask task = new PopulateTask(skipList, (int) n/24, mode);
	        	tasks.add(task);
	        }
			try {
				exec.invokeAll(tasks);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}else {
			System.out.println("INVALID MODE SELECTED IN SkipListPopulator.populate!");
		}
        exec.shutdown();
        try {
			exec.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return skipList;
	}
	
	public static LockFreeSkipListGlobalLog<Integer> populate(LockFreeSkipListGlobalLog<Integer> skipList, int n, String mode) {
		ExecutorService exec = Executors.newFixedThreadPool(24);
        List<Callable<Void>> tasks = new ArrayList<>();
		if(mode.equals("uniform") || mode.equals("normal")) {
			for (int i = 0; i < 24; i++) {
				PopulateTask task = new PopulateTask(skipList, (int) n/24, mode);
	        	tasks.add(task);
	        }
			try {
				exec.invokeAll(tasks);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}else {
			System.out.println("INVALID MODE SELECTED IN SkipListPopulator.populate!");
		}
        exec.shutdown();
        try {
			exec.awaitTermination(3, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return skipList;
	}
	
	static class PopulateTask implements Callable<Void>{
		private int nOps;
		private boolean mode;
		private String listMode;
		private LockFreeSkipList<Integer> skipList;
		private LockFreeSkipListLog<Integer> skipListLog;
		private LockFreeSkipListGlobalLog<Integer> skipListGlobalLog;
		
		public PopulateTask(LockFreeSkipList<Integer> skipList, int nOps, String mode) {
			this.nOps = nOps;
			this.mode = mode.equals("uniform");
			this.skipList = skipList;
			listMode = "LockFreeSkipList";
		}
		
		public PopulateTask(LockFreeSkipListLog<Integer> skipList, int nOps, String mode) {
			this.nOps = nOps;
			this.mode = mode.equals("uniform");
			this.skipListLog = skipList;
			listMode = "LockFreeSkipListLog";
		}
		
		public PopulateTask(LockFreeSkipListGlobalLog<Integer> skipList, int nOps, String mode) {
			this.nOps = nOps;
			this.mode = mode.equals("uniform");
			this.skipListGlobalLog = skipList;
			listMode = "LockFreeSkipListGlobalLog";
		}
				
		public Void call() {
			if(listMode.equals("LockFreeSkipList")) {
				if(mode) {  // Uniform add mode.
					for(int i = 0; i < nOps; i++) {
						skipList.add(r.nextInt(INT_MAX));
					}
				}else {
					for(int i = 0; i < nOps; ) {  // Intentionally don't increment the for loop.
						int next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
						if(INT_MIN <= next && next <= INT_MAX) {
							skipList.add(next);
							i++;
						}
					}
				}
			}
			else if(listMode.equals("LockFreeSkipListLog")) {
				if(mode) {  // Uniform add mode.
					for(int i = 0; i < nOps; i++) {
						skipListLog.add(r.nextInt(INT_MAX));
					}
				}else {
					for(int i = 0; i < nOps; ) {  // Intentionally don't increment the for loop.
						int next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
						if(INT_MIN <= next && next <= INT_MAX) {
							skipListLog.add(next);
							i++;
						}
					}
				}
			}
			else if(listMode.equals("LockFreeSkipListGlobalLog")) {
				if(mode) {  // Uniform add mode.
					for(int i = 0; i < nOps; i++) {
						skipListGlobalLog.add(r.nextInt(INT_MAX));
					}
				}else {
					for(int i = 0; i < nOps; ) {  // Intentionally don't increment the for loop.
						int next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
						if(INT_MIN <= next && next <= INT_MAX) {
							skipListGlobalLog.add(next);
							i++;
						}
					}
				}
			}
			else {
				System.out.println("ERROR IN POPULATE: WRONG INPUT TYPE.");
			}
			return null;
		}
	}


}
