package nepaBackend.model;

public class EISDocMatchJoin {
	Long id;
    String title;
  
    String documentType;
    String commentDate;
    String registerDate;
  
    String agency;
  
    String state;
  
    String filename;

    String commentsFilename;
    
    Long match_id;
    
    int document1;
    
	int document2;

	int match_percent;
    
	public EISDocMatchJoin () {}
    public EISDocMatchJoin(Long id, String title, String documentType, String commentDate, String registerDate,
			String agency, String state, String filename, String commentsFilename, Long match_id, int document1,
			int document2, int match_percent) {
		super();
		this.id = id;
		this.title = title;
		this.documentType = documentType;
		this.commentDate = commentDate;
		this.registerDate = registerDate;
		this.agency = agency;
		this.state = state;
		this.filename = filename;
		this.commentsFilename = commentsFilename;
		this.match_id = match_id;
		this.document1 = document1;
		this.document2 = document2;
		this.match_percent = match_percent;
	}
}
