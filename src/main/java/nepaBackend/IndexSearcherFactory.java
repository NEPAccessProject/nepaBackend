package nepaBackend;
import java.io.File;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
 
public class IndexSearcherFactory { 
 
	public static IndexSearcher getIndexSearcher() {
		IndexSearcher indexSearcher;
		
		try {
			File indexFile = new File(Globals.getIndexString());
			Directory directory = FSDirectory.open(indexFile.toPath());
			IndexReader textReader = DirectoryReader.open(directory);

			File indexFile2 = new File(Globals.getMetaIndexString());
			Directory directory2 = FSDirectory.open(indexFile2.toPath());
			IndexReader metaReader = DirectoryReader.open(directory2);
			MultiReader multiIndexReader = new MultiReader(textReader, metaReader);
			indexSearcher = new IndexSearcher(multiIndexReader);
			
			return indexSearcher;
		} catch(Exception e) {
			return null;
		}
		
	}
}