package testPackage;

import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import skiplistPackage.LockFreeSkipList;

public class OpsTask implements Callable<Void>{
	private int nOps;
	private double addInterval, removeInterval, containsInterval;
	private LockFreeSkipList skipList;
	private TreeMap<Long, Log> log;
	boolean doLog;
	private static Random r =	new Random();
	private int INT_MIN;
	private int INT_MAX;
	private int INT_MEAN;
	private int INT_STD;
	private boolean mode;
	
	public OpsTask(LockFreeSkipList skipList, int nOps, double fracAdd, double fracRemove, double fracContains, 
			int INT_MIN, int INT_MAX, int INT_MEAN, int INT_STD, String mode, TreeMap<Long, Log> log, boolean doLog) {
		this.nOps = nOps;
		this.addInterval = fracAdd;
		this.removeInterval = fracAdd + fracRemove;
		this.containsInterval = fracAdd + fracRemove + fracContains;
		this.skipList = skipList;
		this.log = log;
		this.doLog = doLog;
		this.INT_MIN = INT_MIN;
		this.INT_MAX = INT_MAX;
		this.INT_MEAN = INT_MEAN;
		this.INT_STD = INT_STD;
		this.mode = mode.equals("normal");
	}
			
	public Void call() {
		for(int i = 0; i < nOps; i++) {
			double rnd = Math.random()*containsInterval;
			int next;
			if(mode) {
				 next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
				while(INT_MIN <= next && next <= INT_MAX) {
					next = (int) (r.nextGaussian()*INT_STD + INT_MEAN);
				}
			}else {
				next = r.nextInt(INT_MAX);
			}
			Log tmpLog;
			if(rnd < addInterval) {
				tmpLog = skipList.add(next);

			}else if(rnd < (removeInterval)) {
				tmpLog = skipList.remove(next);

			}else {
				tmpLog = skipList.contains(next);
			}
			if(doLog)
				log.put(tmpLog.timestamp, tmpLog);
		}
		return null;
	}
}
