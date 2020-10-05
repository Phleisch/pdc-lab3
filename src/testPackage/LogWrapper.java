package testPackage;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LogWrapper {
	TreeMap<Long, Log> log;
	ConcurrentLinkedQueue<Log> logConcurrent;
	
	public LogWrapper(TreeMap<Long, Log> log) {
		this.log = log;
	}
	
	public LogWrapper(ConcurrentLinkedQueue<Log> logConcurrent) {
		this.logConcurrent = logConcurrent;
	}
	
	public void put(Log inputLog) {
		if(log == null) {
			logConcurrent.add(inputLog);
			return;
		}
		log.put(inputLog.timestamp, inputLog);
	}
	
	public TreeMap<Long, Log> toTreeMap(){
		return log;
	}
}
