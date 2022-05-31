package nepaBackend.pojo;

public class Suggestion {
	public String title;
	public Long id;
	public Boolean isProcess;
	
	public Suggestion() {
	}

	public Suggestion(String title, Long id, Boolean isProcess) {
		super();
		this.title = title;
		this.id = id;
		this.isProcess = isProcess;
	}
	
}
