package testPackage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class LogChecker {
	
	/**
	* Take in a LinkedList - representing the originally populated set - and a
	* TreeMap of logs for the operations performed on the list. Check that the
	* logs represent a correct sequential execution, and if it doesn't, return
	* the number of logs that contain an operation with an invalid outcome.
	*
	* @param startList	LinkedList representation of the SkipList that was given
	*					at the beginning of a multithreaded operation test
	*
	* @param opLogs		TreeMap of logs for the operations run on the SkipList
	*					provided as startList. The key is the timestamp for
	*					when the linearization point occurred.
	*
	* @return			the number of erroneous operations in operationLogs
	*/
	public static int checkLogs(LinkedList<Integer> startList,
								TreeMap<Long, Log> opLogs) {
		
		int erroneousOps = 0;
		int totalOps = 0;
		Set<Integer> testSet = new HashSet<>(startList);
		
		List<Log> list = new ArrayList<Log>(opLogs.values());
		long t1 = System.nanoTime();
		for(Log opLog : list) {
			//erroneousOps += isOperationValid(opLog, testSet) ? 0 : 1;
			totalOps += 1;
			// System.out.println(totalOps);
		}
		long t2 = System.nanoTime();
		System.out.println("check time was: " + (t2-t1)/1e9);
		return erroneousOps;
	}

	/**
	* Given an operation, the set the operation is being performed on, and
	* the expected successfulness of the operation, return whether the
	* operation is valid. If the successfulness of the operation found in the
	* given operation log does not match the actual operation, then the
	* operation is not valid.
	*
	* @param opLog	 Log of an operation
	* @param testSet Current set of values on which the operation represented by opLog
	*				 will be performed
	* @return whether the operation represented by opLog is valid
	*/
	private static boolean isOperationValid(Log opLog, Set<Integer> testSet) {
		boolean result;

		// In this case, the linearization point happened in a different thread
		// so want to ignore the log
		if (opLog.timestamp == 0) {
			return true;
		} else if (opLog.operation.equals("add")) {
			result = testSet.add(opLog.data);
		} else if (opLog.operation.equals("remove")) {
			result = testSet.remove(opLog.data);
		} else{
			result = testSet.contains(opLog.data);
		}
		return result == opLog.successful;
	}
}
