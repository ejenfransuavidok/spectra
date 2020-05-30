package exceptions;

public class InvalidData extends Exception {
	
	private String message;
	
	public InvalidData(String msg) {
		super();
		this.message = msg;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
