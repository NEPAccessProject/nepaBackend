package nepaBackend.pojo;

public class FolderDelete {
	public String deleteName;
	public String id;
	public String newName;
	
	public FolderDelete() {
	}
	
	public FolderDelete(String deleteName, String id, String newName) {
		this.deleteName = deleteName;
		this.id = id;
		this.newName = newName;
	}
}
