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
								TreeMap<long, Log> opLogs) {
		
		int erroneousOps = 0;
		LockFreeSkipList skipList = convertLLToSkipList(startList);

		for(Map.Entry<long, Log> mapEntry : opLogs.entrySet()) {
			Log opLog = mapEntry.getValue();
			erronousOps += isOperationValid(opLog, skipList) ? 1 : 0;
		}

		return erronousOps;
	}

	/**
	* Given an operation, the set the operation is being performed on, and
	* the expected successfulness of the operation, return whether the
	* operation is valid. If the successfulness of the operation found in the
	* given operation log does not match the actual operation, then the
	* operation is not valid.
	*
	* @param opLog	Log of an operation
	* @param list	LockFree list on which the operation represented by opLog
	*				will be performed
	* @return whether the operation represented by opLog is valid
	*/
	private static boolean isOperationValid(Log opLog, LockFreeSkipList list) {
		
		Log result;

		// In this case, the linearization point happened in a different thread
		// so want to ignore the log
		if (opLog.timestamp == 0) {
			return true;
		} else if (opLog.operation.equals("add")) {
			result = list.add(opLog.data);
		} else if (opLog.operation.equals("remove")) {
			result = list.remove(opLog.data);
		} else if (opLog.operation.equals("contains")) {
			result = list.contains(opLog.data);
		}

		return result.successful == opLog.successful;
	}

	/**
	* Convert a Linked List into a LockFreeSkipList.
	*
	* @param ll	LinkedList to convert
	* @return LockFreeSkipList with the elements from the passed LinkedList
	*/
	public static int convertLLToSkipList(LinkedList<Integer> ll) {
		LockFreeSkipList skipList = new LockFreeSkipList(false);

		for (Integer elem : ll) {
			skipList.add(elem);
		}

		return skipList;
	}

}
