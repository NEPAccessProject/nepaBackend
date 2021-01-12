package nepaBackend.pojo;

public class Unhighlighted {
	private Long id;
	private String filename;

	public Unhighlighted() {
		
	}

	public Unhighlighted(Long id, String filename) {
		this.id = id;
		this.filename = filename;
	}

	public Long getId() {
		return id;
	}

	public String getFilename() {
		return filename;
	}
	
}
