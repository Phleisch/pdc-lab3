package testPackage;

public class Log<T> {
	public T data;
	public String operation;
	public boolean successful;
	public long timestamp;

	public Log (T data, String operation, boolean successful, long timestamp) {
		this.data = data;
		this.operation = operation;
		this.successful = successful;
		this.timestamp = timestamp;
	}
}
