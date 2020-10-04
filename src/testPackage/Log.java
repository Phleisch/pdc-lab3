package testPackage;

public class Log {
	public int data;
	public String operation;
	public boolean successful;
	public long timestamp;

	public Log (int x, String operation, boolean successful, long timestamp) {
		this.data = x;
		this.operation = operation;
		this.successful = successful;
		this.timestamp = timestamp;
	}
}
