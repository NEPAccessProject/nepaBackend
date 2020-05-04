package nepaBackend;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.search.query.dsl.QueryBuilder;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;

import nepaBackend.controller.MetadataWithContext;
import nepaBackend.model.DocumentText;

public class CustomizedTextRepositoryImpl implements CustomizedTextRepository {
	@PersistenceContext
	private EntityManager em;

	/** Return all records matching terms */
	@SuppressWarnings("unchecked")
	@Override
	public List<DocumentText> search(String terms, int limit, int offset) {
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
			
		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
				.buildQueryBuilder().forEntity(DocumentText.class).get();
		Query luceneQuery = queryBuilder
				.keyword()
				.onFields("plaintext")
				.matching(terms)
				.createQuery();

		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery =
				fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class);

		jpaQuery.setMaxResults(limit);
		jpaQuery.setFirstResult(offset);

		// execute search
		return jpaQuery.getResultList();
	}

	// TODO: Return record IDs (located in docList) as well as text results
	/** Return all highlights with context for matching terms (term phrase?) */
	@SuppressWarnings("unchecked")
	@Override
	public List<String> searchContext(String terms, int limit, int offset) {

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		
		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
				.buildQueryBuilder().forEntity(DocumentText.class).get();
		Query luceneQuery = queryBuilder
				.keyword()
				.onFields("plaintext")
				.matching(terms)
				.createQuery();
		
		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery =
				fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class);
		
		jpaQuery.setMaxResults(limit);
		jpaQuery.setFirstResult(offset);
		
		// execute search
		List<DocumentText> docList = jpaQuery.getResultList();
		List<String> highlightList = new ArrayList<String>();
		
		// Use PhraseQuery or TermQuery to get results for matching records
		for (DocumentText doc: docList) {
			try {
				String[] words = terms.split(" ");
				if(words.length>1) {
					highlightList.add(getHighlightPhrase(doc.getPlaintext(), words));
				} else {
					highlightList.add(getHighlightTerm(doc.getPlaintext(), terms));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return highlightList;
	}
	
	/** Return all highlights with context and document ID for matching terms (term phrase?) */
	@SuppressWarnings("unchecked")
	@Override
	public List<MetadataWithContext> metaContext(String terms, int limit, int offset) {
		
		terms = escapeSpecialCharacters(terms);
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
				.buildQueryBuilder().forEntity(DocumentText.class).get();
		Query luceneQuery = queryBuilder
				.keyword()
				.onFields("plaintext")
				.matching(terms)
				.createQuery();
			
		// wrap Lucene query in a javax.persistence.Query
		javax.persistence.Query jpaQuery =
		fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class);
		
		jpaQuery.setMaxResults(limit);
		jpaQuery.setFirstResult(offset);
		
		// execute search
		List<DocumentText> docList = jpaQuery.getResultList();
		List<MetadataWithContext> highlightList = new ArrayList<MetadataWithContext>();
		
		// Use PhraseQuery or TermQuery to get results for matching records
		for (DocumentText doc: docList) {
			try {
			String[] words = terms.split(" ");
			String highlight = "";
			if(words.length>1) {
				highlight = getHighlightPhrase(doc.getPlaintext(), words);
			} else {
				highlight = getHighlightTerm(doc.getPlaintext(), terms);
			}
			if(highlight.length() > 0) {
				highlightList.add(new MetadataWithContext(doc.getEisdoc(), highlight));
			}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return highlightList;
	}

		
	/** Given multi-word search term and document text, return highlights with context via getHighlightString() */
	private static String getHighlightPhrase(String text, String[] keywords) throws IOException {
	//		Builder queryBuilder = new PhraseQuery.Builder();
	//		for (String word: words) {
	//			queryBuilder.add(new Term("f",word));
	//		}
		PhraseQuery query = new PhraseQuery("f", keywords);
		QueryScorer scorer = new QueryScorer(query);
		
		return getHighlightString(text, scorer);
	}

	/** Given single-word search term and document text, return highlights with context via getHighlightString() */
	private static String getHighlightTerm (String text, String keyword) throws IOException {
		TermQuery query = new TermQuery(new Term("f", keyword));
		QueryScorer scorer = new QueryScorer(query);
		
		return getHighlightString(text, scorer);
	}

	// TODO: Re-test
	/** Given document text and QueryScorer, return highlights with context */
	private static String getHighlightString (String text, QueryScorer scorer) throws IOException {
		
		SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class=\"highlight\">","</span>");
		Highlighter highlighter = new Highlighter(formatter, scorer);
		Fragmenter fragmenter = new SimpleFragmenter(50);
		highlighter.setTextFragmenter(fragmenter);
		highlighter.setMaxDocCharsToAnalyze(text.length());
		StandardAnalyzer sa = new StandardAnalyzer();
		TokenStream tokenStream = sa.tokenStream("f", new StringReader(text));
		String result = "";
		
		try {
			result = highlighter.getBestFragments(tokenStream, text, 5, " ... ");
		} catch (InvalidTokenOffsetsException e) {
			// TODO Auto-generated catch block
			sa.close();
			tokenStream.close();
			e.printStackTrace();
		}
	
//			StringBuilder writer = new StringBuilder("");
//			writer.append("<html>");
//			writer.append("<style>\n" +
//				".highlight {\n" +
//				" background: yellow;\n" +
//				"}\n" +
//				"</style>");
//			writer.append("<body>");
//			writer.append("");
//			writer.append("</body></html>");
	
//			return ( writer.toString() );
		sa.close();
		tokenStream.close();
		return result;
	 }
	
	
	@Override
	public boolean sync() {
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		try {
			fullTextEntityManager.createIndexer().startAndWait();
			return true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	/** Escape what Lucene defines as special characters to prevent things like unintentionally excluding the word "green" 
	 * 	when searching for "Duwamish-Green".  At the same time, Lucene does not index characters like "-", so prevent
	 *  searching for "Duwamish-Green" at all and instead search for "duwamish green".  This could change if a different 
	 *  analyzer is used.  */
	private String escapeSpecialCharacters(String inputString) {
		// Lucene supports case-sensitiev inpput, but I'm indexing only lowercase words and no punctuation
		inputString = inputString.toLowerCase();
		//+ - && || ! ( ) { } [ ] ^ \" ~ * ? : \\ /
		final String[] metaCharacters = {"+","-","&&","||","!","(",")","{","}","[","]","^","\"","~","*","?",":","/"};
		
		for (int i = 0 ; i < metaCharacters.length ; i++){
			if(inputString.contains(metaCharacters[i])){
				// Lucene can use special characters, but until we decide how to handle that power just remove them all
//				inputString = inputString.replace(metaCharacters[i],"\\"+metaCharacters[i]);
				inputString = inputString.replace(metaCharacters[i]," ");
			}
		}
		return inputString;
	}
}
