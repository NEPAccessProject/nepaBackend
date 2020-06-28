package nepaBackend.pojo;

/** Can add:
 * 
 * Include drafts; include finals?
 * 
 *
 */
public class SearchInputs {
	public String searchMode;
	public String title;
	public String startPublish;
	public String endPublish;
	public String startComment;
	public String endComment;
	public String[] state;
	public String[] agency;
	public boolean typeAll;
	public boolean typeFinal;
	public boolean typeDraft;
	public boolean typeOther;
	public boolean needsComments;
	public boolean needsDocument;
	public int limit;
	
	public SearchInputs() {
	}
}
