package nepaBackend;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
 
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
 
public class MultiSearcher { 
 
	private IndexSearcher indexSearcher;
  
	public MultiSearcher() throws Exception {
		File indexFile = new File(Globals.getIndexString());
		Directory directory = FSDirectory.open(indexFile.toPath());
		IndexReader textReader = DirectoryReader.open(directory);

		File indexFile2 = new File(Globals.getMetaIndexString());
		Directory directory2 = FSDirectory.open(indexFile2.toPath());
		IndexReader metaReader = DirectoryReader.open(directory2);
		MultiReader multiIndexReader = new MultiReader(textReader, metaReader);
		indexSearcher = new IndexSearcher(multiIndexReader);
 	}
 
	public TopDocs search(Query query, int n) throws Exception {
		return indexSearcher.search(query, n); 
	}
 
	public Document getDocument(int docID) throws Exception {
		return indexSearcher.doc(docID); // Returns a document at the nth ID
	}

	public Document getDocument(int docID, HashSet<String> fieldsToLoad) throws IOException {
		return indexSearcher.doc(docID,fieldsToLoad); // Returns a document at the nth ID with fields loaded
	}
	
}