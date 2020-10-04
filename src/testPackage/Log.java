package testPackage;

public class Log {
	public int data;
	public String operation;
	public boolean successful;
	public long timestamp;

	public Log (int data, String operation, boolean successful, long timestamp) {
		this.data = data;
		this.operation = operation;
		this.successful = successful;
		this.timestamp = timestamp;
	}
}
