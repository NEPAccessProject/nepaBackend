package nepaBackend.absurdity;

/** Can add:
 * 
 * Include drafts; include finals?
 * Comment date?
 * 
 *
 */
public class SearchInputs {
	public String title;
	public String startPublish;
	public String endPublish;
	public String startComment;
	public String endComment;
	public String[] state;
	public String[] agency;
	public boolean needsComments;
	
	public SearchInputs() {
	}
}
