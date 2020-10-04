package testPackage;

public class Log<T> {
	public T data;
	public String operation;
	public boolean successful;
	public long timestamp;

	public Log (T x, String operation, boolean successful, long timestamp) {
		this.data = x;
		this.operation = operation;
		this.successful = successful;
		this.timestamp = timestamp;
	}
}
