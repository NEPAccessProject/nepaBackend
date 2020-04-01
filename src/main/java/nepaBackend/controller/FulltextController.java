package nepaBackend.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import nepaBackend.TextRepository;
import nepaBackend.SearchLogRepository;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;

@RestController
@RequestMapping("/test")
public class FulltextController {

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Autowired
	private TextRepository textRepository;
	private SearchLogRepository searchLogRepository;
	
	public FulltextController(TextRepository textRepository, 
								SearchLogRepository searchLogRepository) {
		this.textRepository = textRepository;
		this.searchLogRepository = searchLogRepository;
	}

	/** Test TODO: Finalize */
	@CrossOrigin
	@PostMapping(path = "/full")
	public List<DocumentText> fulltextSearch(@RequestParam("plaintext") String plaintext)
	{
		System.out.println(plaintext);
		return textRepository.search(plaintext, 1000, 0);
	}
	
	
	
	
	
	// TODO: Temporary thing for testing, directory will differ for live
	private static final String INDEX_DIRECTORY = "C:/tmp/lucene";
	private File f = new File(INDEX_DIRECTORY);
	
	// Test TODO: Finalize
	// Add a MySQL table to Lucene via denormalization
	@CrossOrigin
	@PostMapping(path = "/sync")
	public void sync( ) {

		try {
			StandardAnalyzer analyzer = new StandardAnalyzer();
		    IndexWriterConfig config = new IndexWriterConfig(analyzer);
		    IndexWriter writer = new IndexWriter(FSDirectory.open(f.toPath()), config);

		    String sQuery = "SELECT id, document_id, plaintext FROM document_text";
	
			// Run query
			List<DocumentText> records = jdbcTemplate.query
			(
				sQuery, 
				(rs, rowNum) -> new DocumentText(
					rs.getLong("id"), 
					rs.getLong("document_id"), 
					rs.getString("plaintext")
				)
			);
		    
		    for (DocumentText record : records) {
		        Document document = new Document();
		        document.add(new StringField("id", record.getId().toString(), Field.Store.YES));
		        document.add(new StringField("document_id", record.getId().toString(), Field.Store.YES));
		        document.add(new TextField("plaintext", record.getPlaintext(), Field.Store.NO));
				writer.updateDocument(new Term("id", record.getId().toString()), document);
		    }
	
		    writer.close();
	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
}