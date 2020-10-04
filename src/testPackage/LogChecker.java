public class LogChecker {
	
	/**
	* Take in a LinkedList - representing the originally populated set - and a
	* TreeMap of logs for the operations performed on the list. Check that the
	* logs represent a correct sequential execution, and if it doesn't, return
	* the number of logs that contain an operation with an invalid outcome.
	*
	* @param startSet	List representation of the set that was used in a test
	*
	* @param opLogs		TreeMap of logs for the operations run on the set
	*					provided as originalSet. The key is the timestamp for
	*					when the linearization point occurred.
	*
	* @return			the number of erronous operations in operationLogs
	*/
	public static int checkLogs(LinkedList<Integer> startSet,
								TreeMap<long, Log> opLogs) {
		LockFreeSkipList skipList = convertToSkipList(originalSet)
		
		int erronousOps = 0;

		for(log in operationLogs) {
			erronousOps += operationIsErronous ? 1 : 0;
		}

		return erronousOps;
	}

}
