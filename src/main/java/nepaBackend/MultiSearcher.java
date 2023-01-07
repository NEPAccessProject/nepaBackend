package nepaBackend;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
 
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
 
public class MultiSearcher { 
 
	private IndexSearcher indexSearcher;
	private IndexReader textReader;
	private IndexReader metaReader;
	private MultiReader multiIndexReader;
  
	public MultiSearcher() throws Exception {
		File indexFile = new File(Globals.getIndexString());
		Directory directory = FSDirectory.open(indexFile.toPath());
//		System.out.println("Text index? " + DirectoryReader.indexExists(directory));
		textReader = DirectoryReader.open(directory);
		

		File indexFile2 = new File(Globals.getMetaIndexString());
		Directory directory2 = FSDirectory.open(indexFile2.toPath());
//		System.out.println("Meta index? " + DirectoryReader.indexExists(directory2));
		metaReader = DirectoryReader.open(directory2);
		
		multiIndexReader = new MultiReader(textReader, metaReader);
		indexSearcher = new IndexSearcher(multiIndexReader);
		
 	}
	
	public MultiReader getIndexReader() {
		return multiIndexReader;
	}

	/** Returns textReader specifically; used for highlighting **/
	public IndexReader getTextReader() {
		return textReader;
	}
	
	public IndexReader getMetaReader() {
		return metaReader;
	}
 
	public TopDocs search(Query query, int n) throws Exception {
		return indexSearcher.search(query, n); 
	}
	public int searchHits(Query query, TotalHitCountCollector thcc) throws Exception {
		indexSearcher.search(query, thcc);
		return thcc.getTotalHits();
	}
 
	public Document getDocument(int docID) throws Exception {
		return indexSearcher.doc(docID); // Returns a document at the nth ID
	}

	public Document getDocument(int docID, HashSet<String> fieldsToLoad) throws IOException {
		return indexSearcher.doc(docID,fieldsToLoad); // Returns a document at the nth ID with fields loaded
	}

	public Document doc(int docID) throws IOException {
		return indexSearcher.doc(docID); // Returns a document at the nth ID
	}

	public Document doc(int docID, HashSet<String> fieldsToLoad) throws IOException {
		return indexSearcher.doc(docID,fieldsToLoad);
	}
	
}