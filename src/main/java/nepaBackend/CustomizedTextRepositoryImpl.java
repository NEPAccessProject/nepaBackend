package nepaBackend;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import nepaBackend.controller.MetadataWithContext;
import nepaBackend.controller.MetadataWithContext2;
import nepaBackend.controller.MetadataWithContext3;
import nepaBackend.enums.EntityType;
import nepaBackend.enums.SearchType;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.pojo.DocumentTextStrings;
import nepaBackend.pojo.HighlightedResult;
import nepaBackend.pojo.ReducedText;
import nepaBackend.pojo.ScoredResult;
import nepaBackend.pojo.SearchInputs;
import nepaBackend.pojo.Unhighlighted;
import nepaBackend.pojo.Unhighlighted2;
import nepaBackend.pojo.Unhighlighted2DTO;
import nepaBackend.pojo.UnhighlightedDTO;

public class CustomizedTextRepositoryImpl implements CustomizedTextRepository {
	@PersistenceContext
	private EntityManager em;

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Autowired
	StandardAnalyzer analyzer;
	
	@Autowired
	IndexReader textReader;
//	@Autowired
//	IndexReader metaReader;
//	@Autowired
//	MultiReader multiReader;
	
	@Autowired
	IndexSearcher indexSearcher;
	
//	private static MultiSearcher indexSearcher;

	private static int numberOfFragmentsMin = 3;
	private static int numberOfFragmentsMax = 3;
	private static int fragmentSize = 500;
	private static int bigFragmentSize = 500;
	private static SimpleHTMLFormatter globalFormatter = new SimpleHTMLFormatter("<span class=\"highlight\">","</span>");
	
//	private static int fuzzyLevel = 1;

	/** Return all records matching terms in "plaintext" field (no highlights/context) 
	 * (This function is basically unused since the card format changes in Oct. '20)
	 * @throws ParseException */
	@Override
	public List<EISDoc> search(String terms, int limit, int offset) throws ParseException {
		
		String newTerms = mutateTermModifiers(terms);
		
		SearchSession session = org.hibernate.search.mapper.orm.Search.session(em);
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em); // Create fulltext entity manager
			
		QueryParser qp = new QueryParser("plaintext", new StandardAnalyzer());
		qp.setDefaultOperator(Operator.AND);

		// this may throw a ParseException which the caller has to deal with
		Query luceneQuery = qp.parse(newTerms);
		
		// Note: QueryBuilder, for whatever reason, doesn't treat ? like a single wildcard character.  queryparser.classic.QueryParser does.
//		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
//				.buildQueryBuilder().forEntity(DocumentText.class).get();
		
		// Old code: Only good for single terms, even encapsulated in double quotes.  For multiple terms, it splits them by spaces and will basically OR them together.
//		Query luceneQuery = queryBuilder
//				.keyword()
//				.onField("plaintext")
//				.matching(terms)
//				.createQuery();
		
		// Old code: Tries to match on phrases
//		Query luceneQuery = queryBuilder
//				.phrase()
//				.onField("plaintext")
//				.sentence(terms)
//				.createQuery();
		
		// This is as loose of a search as we can build.
//		Query luceneQuery = queryBuilder
//				.keyword()
//				.fuzzy()
//				.withEditDistanceUpTo(fuzzyLevel) // max: 2; default: 2; aka maximum fuzziness
//				.onField("plaintext")
//				.matching(terms)
//				.createQuery();
		
		// Let's try an all-word search.
//		SrndQuery q = QueryParser.parse(terms);
		
//		Query luceneQuery = queryBuilder
//				.simpleQueryString()
//				.onField("plaintext")
//				.withAndAsDefaultOperator()
//				.matching(terms)
//				.createQuery();

//		String[] termsArray = org.apache.commons.lang3.StringUtils.normalizeSpace(terms).split(" ");
//		String allWordTerms = "";
//		for(int i = 0; i < termsArray.length; i++) {
//			allWordTerms += termsArray[i] + " AND ";
//		}
//		allWordTerms = allWordTerms.substring(0, allWordTerms.length()-4).strip();
//		Query luceneQuery = queryBuilder
//				.keyword()
//				.onField("plaintext")
//				.matching(allWordTerms)
//				.createQuery();
		
		
//		String defaultField = "plaintext";
//		Analyzer analyzer = new StandardAnalyzer();
//		QueryParser queryParser = new QueryParser()
//				.AndQuery()
//				.;
//		queryParser.setDefaultOperator(QueryParser.Operator.AND);
//		Query query = queryParser.parse(terms);

		// wrap Lucene query in org.hibernate.search.jpa.FullTextQuery (partially to make use of projections)
		org.hibernate.search.jpa.FullTextQuery jpaQuery =
			fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class);
		
		// project only IDs in order to reduce RAM usage (heap outgrows max memory if we pull the full DocumentText list in)
		// we can't directly pull the EISDoc field here with projection because it isn't indexed by Lucene
		jpaQuery.setProjection(ProjectionConstants.ID);

		jpaQuery.setMaxResults(limit);
		jpaQuery.setFirstResult(offset);
		
		ArrayList<Long> new_ids = new ArrayList<Long>();
		
		List<Object[]> ids = jpaQuery.getResultList();
		for(Object[] id : ids) {
			new_ids.add((Long) id[0]);
		}
		
		// use the foreign key list from Lucene to make a normal query to get all associated metadata tuples from DocumentText
		// Note: Need distinct here because multiple files inside of archives are associated with the same metadata tuples
		// TODO: Can get filenames also, display those on frontend and no longer need DISTINCT (would require a new POJO, different structure than List<EISDoc>
		// or just use metadatawithcontext with blank highlight strings as very mild overhead)
		// Alternative: Because title and therefore eisdoc ID is indexed by Lucene, we could simplify
		// the above section to projecting EISDoc IDs directly and save the second query
		javax.persistence.Query query = em.createQuery("SELECT DISTINCT doc.eisdoc FROM DocumentText doc WHERE doc.id IN :ids");
		query.setParameter("ids", new_ids);

		List<EISDoc> docs = query.getResultList();

		fullTextEntityManager.close(); // Because this is created on demand, close it when we're done
		
		return docs;
	}
	
	/** Return all records matching terms in "title" field via Lucene.  
	 * This is used by metadataSearch (title-only search) to join on filtered records that
	 * match the fulltext search here.
	 * @throws ParseException 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<EISDoc> searchTitles(String terms) throws ParseException {
		
		String newTerms = mutateTermModifiers(terms);
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em); // Create fulltext entity manager
			
		QueryParser qp = new QueryParser("title", new StandardAnalyzer());
		qp.setDefaultOperator(Operator.AND);

		// this may throw a ParseException which the caller has to deal with
		Query luceneQuery = qp.parse(newTerms);
		
		
		// wrap Lucene query in org.hibernate.search.jpa.FullTextQuery
		org.hibernate.search.jpa.FullTextQuery jpaQuery =
			fullTextEntityManager.createFullTextQuery(luceneQuery, EISDoc.class);
		
		jpaQuery.setMaxResults(1000000);
		jpaQuery.setFirstResult(0);
		
		List<EISDoc> docs = jpaQuery.getResultList();

		fullTextEntityManager.close(); // Because this is created on demand, close it when we're done
		
		return docs;
	}

	// Note: Probably unnecessary function
	/** Return all highlights with context for matching terms (term phrase?) */
//	@SuppressWarnings("unchecked")
//	@Override
//	public List<String> searchContext(String terms, int limit, int offset) {
//
//		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
//		
//		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
//				.buildQueryBuilder().forEntity(DocumentText.class).get();
//		Query luceneQuery = queryBuilder
//				.keyword()
//				.onFields("plaintext")
//				.matching(terms)
//				.createQuery();
//		
//		// wrap Lucene query in a javax.persistence.Query
//		javax.persistence.Query jpaQuery =
//				fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class);
//		
//		jpaQuery.setMaxResults(limit);
//		jpaQuery.setFirstResult(offset);
//		
//		// execute search
//		List<DocumentText> docList = jpaQuery.getResultList();
//		List<String> highlightList = new ArrayList<String>();
//		
//		// Use PhraseQuery or TermQuery to get results for matching records
//		for (DocumentText doc: docList) {
//			try {
//				String[] words = terms.split(" ");
//				if(words.length>1) {
//					highlightList.add(getHighlightPhrase(doc.getPlaintext(), words));
//				} else {
//					highlightList.add(getHighlightTerm(doc.getPlaintext(), terms));
//				}
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} finally {
//				fullTextEntityManager.close();
//			}
//		}
//		
//		return highlightList;
//	}
	
	/** Return all highlights with context and document ID for matching terms from "plaintext" field
	 * @throws ParseException */
//	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public List<MetadataWithContext> metaContext(String terms, int limit, int offset, SearchType searchType) throws ParseException {
		long startTime = System.currentTimeMillis();
		
		terms = mutateTermModifiers(terms);
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

//		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
//				.buildQueryBuilder().forEntity(DocumentText.class).get();

		QueryParser qp = new QueryParser("plaintext", new StandardAnalyzer());
		qp.setDefaultOperator(Operator.AND);
				
		// this may throw a ParseException which the caller has to deal with
		Query luceneQuery = qp.parse(terms);
		
//		boolean fuzzy = false;
//		if(fuzzy) {
//			luceneQuery = queryBuilder
//					.keyword()
//					.fuzzy()
//					.withEditDistanceUpTo(fuzzyLevel) // max: 2; default: 2; aka maximum fuzziness
//					.onField("plaintext")
//					.matching(terms)
//					.createQuery();
			
//		} else {
			// for phrases
//			luceneQuery = queryBuilder
//					.phrase()
//						.withSlop(0) // default: 0 (note: doesn't work as expected)
//					.onField("plaintext")
//					.sentence(terms)
//					.createQuery();
			
			// all-word (newest querybuilder logic)
//			luceneQuery = queryBuilder
//					.simpleQueryString()
//					.onField("plaintext")
//					.withAndAsDefaultOperator()
//					.matching(terms)
//					.createQuery();
			
//			String[] termsArray = org.apache.commons.lang3.StringUtils.normalizeSpace(terms).split(" ");
//			String allWordTerms = "";
//			for(int i = 0; i < termsArray.length; i++) {
//				allWordTerms += "+" + termsArray[i] + " ";
//			}
//			allWordTerms = allWordTerms.strip();
//			
//			luceneQuery = queryBuilder
//					.keyword()
//					.onField("plaintext")
//					.matching(allWordTerms)
//					.createQuery();
//		}
			
		// wrap Lucene query in a javax.persistence.Query
		// TODO: Test org.hibernate.search.jpa.FullTextQuery instead
//		org.hibernate.search.jpa.FullTextQuery jpaQuery =
		javax.persistence.Query jpaQuery =
		fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class);
		
		jpaQuery.setMaxResults(limit);
		jpaQuery.setFirstResult(offset);
		
		// execute search
		List<DocumentText> docList = jpaQuery.getResultList();
		List<MetadataWithContext> highlightList = new ArrayList<MetadataWithContext>();

		QueryScorer scorer = new QueryScorer(luceneQuery);

		// Logic for exact phrase vs. all-word query
//		String[] words = terms.split(" ");
//		if(searchType == SearchType.ALL) { // .equals uses == internally
//			if(fuzzy) {
				// Fuzzy search code
//				FuzzyLikeThisQuery fuzzyQuery = new FuzzyLikeThisQuery(32, new StandardAnalyzer());
//				fuzzyQuery.addTerms(terms, "f", fuzzyLevel, 0);
//				scorer = new QueryScorer(fuzzyQuery);
//			} else {
				// Old search code: any-word
//				List<Term> termWords = new ArrayList<Term>();
//				for (String word: words) {
//					termWords.add(new Term("f", word));
//				}
//				TermsQuery query = new TermsQuery(termWords);
//				scorer = new QueryScorer(query);
				
				// all-word
//				BooleanQuery bq = new BooleanQuery();
//				for (String word: words) {
//					bq.add(new TermQuery(new Term("f", word)), Occur.MUST);
//				}
//				scorer = new QueryScorer(bq);
				
				// all-word using exact same query logic
//				scorer = new QueryScorer(luceneQuery);
//			}
//		} else {
			// Oldest code (most precision required)
//			PhraseQuery query = new PhraseQuery("f", words);
//			scorer = new QueryScorer(query);
//		}

		Highlighter highlighter = new Highlighter(globalFormatter, scorer);
		Fragmenter fragmenter = new SimpleFragmenter(fragmentSize);
		highlighter.setTextFragmenter(fragmenter);
		highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
		
		
		for (DocumentText doc: docList) {
			try {
				if(Globals.TESTING) {
					String highlight = getCustomSizeHighlightString(doc.getPlaintext(), scorer, bigFragmentSize, numberOfFragmentsMin);
					if(highlight.length() > 0) { // Length 0 shouldn't be possible since we are working on matching results already
						highlightList.add(new MetadataWithContext(doc.getEisdoc(), highlight, doc.getFilename()));
					}
				} else {
					String highlight = getHighlightString(doc.getPlaintext(), highlighter);
					if(highlight.length() > 0) { // Length 0 shouldn't be possible since we are working on matching results already
						highlightList.add(new MetadataWithContext(doc.getEisdoc(), highlight, doc.getFilename()));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				fullTextEntityManager.close();
			}
		}
		
		if(Globals.TESTING) {
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println(elapsedTime);
		}
		
		return highlightList;
	}

		
	/** Given multi-word search term and document text, return highlights with context via getHighlightString() */
//	private static String getHighlightPhrase(String text, String[] keywords, Highlighter highlighter) throws IOException {
//	//		Builder queryBuilder = new PhraseQuery.Builder();
//	//		for (String word: words) {
//	//			queryBuilder.add(new Term("f",word));
//	//		}
//		PhraseQuery query = new PhraseQuery("f", keywords);
//		QueryScorer scorer = new QueryScorer(query);
//		
//		return getHighlightString(text, scorer);
//	}

	/** Given single-word search term and document text, return highlights with context via getHighlightString() */
//	private static String getHighlightTerm (String text, String keyword, Highlighter highlighter) throws IOException {
//		TermQuery query = new TermQuery(new Term("f", keyword));
//		QueryScorer scorer = new QueryScorer(query);
//
//
//		return getHighlightString(text, scorer);
//	}


	/** Given document text and QueryScorer, return highlights with context */
//	private static String getHighlightString (String text, QueryScorer scorer) throws IOException {
//		
//		SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class=\"highlight\">","</span>");
//		Highlighter highlighter = new Highlighter(formatter, scorer);
//		Fragmenter fragmenter = new SimpleFragmenter(fragmentSize);
//		highlighter.setTextFragmenter(fragmenter);
//		highlighter.setMaxDocCharsToAnalyze(text.length());
//		StandardAnalyzer stndrdAnalyzer = new StandardAnalyzer();
//		TokenStream tokenStream = stndrdAnalyzer.tokenStream("f", new StringReader(text));
//		String result = "";
//		
//		try {
//			// Add ellipses to denote that these are text fragments within the string
//			result = highlighter.getBestFragments(tokenStream, text, numberOfFragmentsMax, " ...</span><br /><span class=\"fragment\">... ");
////			System.out.println(result);
//			if(result.length()>0) {
//				result = "<span class=\"fragment\">... " + (result.replaceAll("\\n+", " ")).trim().concat(" ...</span>");
////				System.out.println(result);
//			}
//		} catch (InvalidTokenOffsetsException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {
//			stndrdAnalyzer.close();
//			tokenStream.close();
//			text = "";
//		}
//	
////			StringBuilder writer = new StringBuilder("");
////			writer.append("<html>");
////			writer.append("<style>\n" +
////				".highlight {\n" +
////				" background: yellow;\n" +
////				"}\n" +
////				"</style>");
////			writer.append("<body>");
////			writer.append("");
////			writer.append("</body></html>");
//	
////			return ( writer.toString() );
//		return result;
//	 }
	

	// Given text, fragment size, num fragments, queryscorer, return highlight(s)
	private static String getCustomSizeHighlightString (String text, QueryScorer scorer, int fragmentSize, int numberOfFragments) throws IOException {
		

		Highlighter highlighter = new Highlighter(globalFormatter, scorer);
		Fragmenter fragmenter = new SimpleFragmenter(fragmentSize);
		highlighter.setTextFragmenter(fragmenter);
		highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
		StandardAnalyzer stndrdAnalyzer = new StandardAnalyzer();
		TokenStream tokenStream = stndrdAnalyzer.tokenStream("plaintext", new StringReader(text));
		String result = "";
		
		try {
			// Add ellipses to denote that these are text fragments within the string
			result = highlighter.getBestFragments(tokenStream, text, numberOfFragments, " ...</span><br /><span class=\"fragment\">... ");
//			System.out.println(result);
			if(result.length()>0) {
				result = "<span class=\"fragment\">... " + org.apache.commons.lang3.StringUtils.normalizeSpace(result).strip().concat(" ...</span>");
//				System.out.println(result);
			}
		} catch (InvalidTokenOffsetsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			stndrdAnalyzer.close();
			tokenStream.close();
			text = "";
		}
		
		return result;
	 }
	
	// Given text and highlighter, return highlights (fragments) for text
	private static String getHighlightString (String text, Highlighter highlighter) throws IOException {
		
		StandardAnalyzer stndrdAnalyzer = new StandardAnalyzer();
		TokenStream tokenStream = stndrdAnalyzer.tokenStream("plaintext", new StringReader(text));
		String result = "";
		
		try {
			// Add ellipses to denote that these are text fragments within the string
			result = highlighter.getBestFragments(tokenStream, text, numberOfFragmentsMax, " ...</span><span class=\"fragment\">... ");
			
			if(result.length()>0) {
				result = "<span class=\"fragment\">... " + org.apache.commons.lang3.StringUtils.normalizeSpace(result).strip().concat(" ...</span>");
//				System.out.println(result);
			}
		} catch (InvalidTokenOffsetsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			stndrdAnalyzer.close();
			tokenStream.close();
			text = "";
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
		return result;
	 }
	
	
	@Override
	public boolean sync() {
		SearchSession searchSession = org.hibernate.search.mapper.orm.Search.session(em);
		
		MassIndexer indexer = searchSession.massIndexer( DocumentText.class, EISDoc.class ); 

		try {
			indexer.startAndWait();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		} 
//		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
//		try {
//			fullTextEntityManager.createIndexer().startAndWait();
//		} catch (InterruptedException e) {
//			// TODO log interruption?
//			e.printStackTrace();
//			return false;
//		}
		
		return true;
	}
	
// As long as we use the ORM to delete DocumentText records, Lucene will know about it and delete them from its index
//	public boolean delete(Long id) {
//		return true;
//	}
	
	// escapeSpecialCharacters is now useless as we analyze/parse search terms more intelligently.
	/** Escape what Lucene defines as special characters to prevent things like unintentionally excluding the word "green" 
	 * 	when searching for "Duwamish-Green".  At the same time, Lucene does not index characters like "-", so prevent
	 *  searching for "Duwamish-Green" at all and instead search for "duwamish green".  This could change if a different 
	 *  analyzer is used.  */
//	private String escapeSpecialCharacters(String inputString) {
//		
//		// Lucene supports case-sensitive inpput, but I'm indexing only lowercase words and no punctuation
//		inputString = inputString.toLowerCase();
//		//+ - && || ! ( ) { } [ ] ^ \" ~ * ? : \\ /
////		final String[] metaCharacters = {"+","-","&&","||","!","(",")","{","}","[","]","^","\"","~","*","?",":","/","  "};
//		// - allows searching for exclusions, " allows exact phrase search, * allows wildcard search...
//		final String[] metaCharacters = {"+","&&","||","!","(",")","{","}","[","]","^","~","?",":","/","  "};
//		
//		for (int i = 0 ; i < metaCharacters.length ; i++){
//			if(inputString.contains(metaCharacters[i])){
//				// Lucene can use special characters, but until we decide how to handle that power just remove them all
////				inputString = inputString.replace(metaCharacters[i],"\\"+metaCharacters[i]);
//				inputString = inputString.replace(metaCharacters[i]," ") // replace special characters with spaces
//						.trim(); // extra spaces may mean no results when looking for an exact phrase
//			}
//		}
//		return inputString;
//	}

	/** Returns search terms after enforcing two rules:  Proximity matching was limited to 1 billion, just under absolute upper limit 
	 * (when going beyond the limit, proximity matching stopped working at all).  
	 * Support for | is added by converting to ||. */
    private String mutateTermModifiers(String terms){
    	if(terms != null && terms.strip().length() > 0) {
    		// + and - must immediately precede the next term (no space), therefore don't add a space after those when replacing
    		return org.apache.commons.lang3.StringUtils.normalizeSpace(terms).replace(" | ",  " || ")
//    				.replace("and", "AND") // support for AND is implicit currently
//    				.replace("or", "OR") // Lowercase term modifiers could easily trip people up accidentally if they're pasting something in and they don't actually want to OR them
//    				.replace("not", "NOT")
//    				.replace("&", "AND")
    				.replace("!", "-")
//    				.replace("%", "-")
//    				.replace("/", "~") // westlaw? options, can also add confusion
    				.strip(); // QueryParser doesn't support |, does support ?, OR, NOT
//    				.replaceAll("(~\\d{10}\\d*)", "~999999999"); // this was necessary with QueryBuilder (broke after limit)
    	} else {
    		return "";
    	}
    }

	
	/**1. Triggered, verified Lucene indexing on Title (Added @Indexed for EISDoc and @Field for title)
	 * 2. Lucene-friendly Hibernate/JPA-wrapped query based on custom, dynamically created query
	 * */
    /** Title-only search */
	@Override
	public List<EISDoc> metadataSearch(SearchInputs searchInputs, int limit, int offset, SearchType searchType) {
		try {
			
			searchInputs.title = mutateTermModifiers(searchInputs.title);
			
			// Init parameter lists
			ArrayList<String> inputList = new ArrayList<String>();
			ArrayList<String> whereList = new ArrayList<String>();

//			ArrayList<Long> new_ids = new ArrayList<Long>();
			
//			boolean saneTitle = false;
			
			// TODO: if searchInputs isn't null but title is null or blank, we can return a simple query with no text searching
//			if(searchInputs != null && searchInputs.title != null && !searchInputs.title.isBlank()) {
//				String formattedTitle = org.apache.commons.lang3.StringUtils.normalizeSpace(searchInputs.title.strip());
//
//				FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
//
//				QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
//						.buildQueryBuilder().forEntity(EISDoc.class).get();
//				
//				String[] arrKeywords = formattedTitle.split(" ");
//				
//				List<Query> queryList = new LinkedList<Query>();
//		        Query query = null;
//
//		        for (String keyword : arrKeywords) {
//		            query = queryBuilder.keyword().onField("title").matching(keyword).createQuery();
//		            queryList.add(query);
//		        }
//
//		        BooleanQuery finalQuery = new BooleanQuery();
//		        for (Query q : queryList) {
//		            finalQuery.add(q, Occur.MUST);
//		        }
//
//				org.hibernate.search.jpa.FullTextQuery jpaQuery =
//						fullTextEntityManager.createFullTextQuery(finalQuery, EISDoc.class);
//		        
//				
////				Query luceneQuery = queryBuilder
////						.keyword()
////						.onField("title")
//////						.withAndAsDefaultOperator()
////						.matching(formattedTitle)
////						.createQuery();
//
//				// wrap Lucene query in org.hibernate.search.jpa.FullTextQuery (partially to make use of projections)
////				org.hibernate.search.jpa.FullTextQuery jpaQuery =
////						fullTextEntityManager.createFullTextQuery(luceneQuery, EISDoc.class);
//				
//				// project only IDs in order to reduce RAM usage (heap outgrows max memory if we pull the full DocumentText list in)
//				// we can't directly pull the EISDoc field here with projection because it isn't indexed by Lucene
//				jpaQuery.setProjection(ProjectionConstants.ID);
//
//				jpaQuery.setMaxResults(limit);
//				jpaQuery.setFirstResult(offset);
//				
//				
//				List<Object[]> ids = jpaQuery.getResultList();
//				for(Object[] id : ids) {
//					System.out.println(id[0].toString());
//					new_ids.add((Long) id[0]);
//				}
//				
//				saneTitle = true;
//				
//				// use the foreign key list from Lucene to make a normal query to get all associated metadata tuples from DocumentText
//				// Note: Need distinct here because multiple files inside of archives are associated with the same metadata tuples
//				// TODO: Can get filenames also, display those on frontend and no longer need DISTINCT (would require a new POJO, different structure than List<EISDoc>)
////				javax.persistence.Query query = em.createQuery("SELECT DISTINCT doc.eisdoc FROM DocumentText doc WHERE doc.id IN :ids");
////				query.setParameter("ids", new_ids);
//			}
			
//			Query luceneQuery = queryBuilder
//					.simpleQueryString()
//					.onField("plaintext")
//					.withAndAsDefaultOperator()
//					.matching(searchInputs.title)
//					.createQuery();

			// wrap Lucene query in a javax.persistence.Query
//			FullTextQuery jpaQuery =
//			javax.persistence.Query jpaQuery =
//			fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class);
//			
//			jpaQuery.setMaxResults(limit);
//			jpaQuery.setFirstResult(offset);
			
			// TODO: Convert this to JPA like so:??
//			Collection<Professor> c =  
//				    em.createQuery("SELECT e " +
//				                   "FROM Professor e " +
//				                   "WHERE e.startDate BETWEEN :start AND :end")
//				      .setParameter("start", new Date(), TemporalType.DATE)
//				      .setParameter("end", new Date(), TemporalType.DATE)
//				      .getResultList();
			// So basically, your whereList probably has to change its inputs to :namedParam from ?, and your inputList
			// probably has to become an int position/object value pair or a string name/object value pair:
//			jpaQuery.setParameter(position, value)
//			jpaQuery.setParameter(name, value)
			// or something; have to review logic and syntax
			// depending on which variables are in play, probably need to build the parameters at the end after knowing
			// which ones to build, after the .createQuery
			// Note: Might actually be more complicated.  Maybe setHint helps?
			// If we can somehow use a native query, then we can even use the String we've already built in the original logic,
			// and as a bonus it'll actually work
			// If we can just set the Criteria a la FullTextQuery.setCriteriaQuery(Criteria critera)
			// https://docs.jboss.org/hibernate/search/5.4/api/org/hibernate/search/jpa/FullTextQuery.html
			// then I think we can do it.  This requires then building Criteria instead of a query string below.
			// Do we need a Hibernate session for Criteria? I don't have that
			// CriteriaBuilder is the absolute worst thing, so we'll do our best to avoid that.
			// Next solution to investigate is using the lucene queryparser to build the query from custom params.
			// https://lucene.apache.org/core/4_8_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package_description
			// https://stackoverflow.com/questions/60205647/how-to-construct-a-lucene-search-query-with-multiple-parameters

//			List<Predicate> predicates = new ArrayList<Predicate>();
//			List<SimpleExpression> expressionList = new ArrayList<SimpleExpression>();
//			List<Criterion> expressionList = new ArrayList<Criterion>();

//			CriteriaBuilder cb = fullTextEntityManager.getCriteriaBuilder();
			
//			CriteriaQuery q = cb.createQuery(EISDoc.class);
//			Root<EISDoc> root = q.from(EISDoc.class);
//			q.select(root);
//			
//			ParameterExpression<Integer> p = cb.parameter(Integer.class);
//			ParameterExpression<Integer> a = cb.parameter(Integer.class);
//			q.where(
//			    cb.ge(root.get("population"), p),
//			    cb.le(root.get("area"), a)
//			);

//			StandardAnalyzer stndrdAnalyzer = new StandardAnalyzer();
//			QueryParser luceneQueryParser = new QueryParser("plaintext", stndrdAnalyzer);
			
			// Select tables, columns
			String sQuery = "SELECT * FROM eisdoc";
			
			// If we have a valid title then search on new_ids
			// If we don't have a valid title then ignore new_ids and therefore run on entire database
//			if(saneTitle) {
//				if(new_ids.isEmpty()) {
//					// if valid title and new_ids is empty we can just return an empty list immediately
//					return new ArrayList<EISDoc>();
//				}
//				StringBuilder query = new StringBuilder(" id IN (");
//				for (int i = 0; i < new_ids.size(); i++) {
//					if (i > 0) {
//						query.append(",");
//					}
//					query.append("?");
//				}
//				query.append(")");
//	
//				for (int i = 0; i < new_ids.size(); i++) {
//					inputList.add(new_ids.get(i).toString());
//				}
//				whereList.add(query.toString());
//			}
			
			// Populate lists
			if(Globals.saneInput(searchInputs.startPublish)) {
				// I think this is right?
//				criteria.add(Restrictions.ge("register_date", searchInputs.startPublish));
//				q.select(root).where(cb.ge(root.get("register_date"), searchInputs.startPublish));
//				predicates.add(cb.ge(root.get("register_date"), searchInputs.startPublish));
//				expressionList.add(Restrictions.ge("register_date", searchInputs.startPublish));
				inputList.add(searchInputs.startPublish);
				whereList.add(" ((register_date) >= ?)");
			}
			
			if(Globals.saneInput(searchInputs.endPublish)) {
//				criteria.add(Restrictions.le("register_date", searchInputs.endPublish));
				inputList.add(searchInputs.endPublish);
				whereList.add(" ((register_date) <= ?)");
			}
	
			if(Globals.saneInput(searchInputs.startComment)) {
				inputList.add(searchInputs.startComment);
				whereList.add(" ((comment_date) >= ?)");
			}
			
			if(Globals.saneInput(searchInputs.endComment)) {
				inputList.add(searchInputs.endComment);
				whereList.add(" ((comment_date) <= ?)");
			}
			
			if(Globals.saneInput(searchInputs.typeAll)) { 
				// do nothing
			} else {
				
				ArrayList<String> typesList = new ArrayList<>();
				StringBuilder query = new StringBuilder(" document_type IN (");
				if(Globals.saneInput(searchInputs.typeFinal)) {
					typesList.add("Final");
				}
	
				if(Globals.saneInput(searchInputs.typeDraft)) {
					typesList.add("Draft");
				}
				
				if(Globals.saneInput(searchInputs.typeOther)) {
					typesList.addAll(Globals.EIS_TYPES);
				}
				String[] docTypes = typesList.toArray(new String[0]);
				for (int i = 0; i < docTypes.length; i++) {
					if (i > 0) {
						query.append(",");
					}
					query.append("?");
				}
				query.append(")");
	
				for (int i = 0; i < docTypes.length; i++) {
					inputList.add(docTypes[i]);
				}
				
				if(docTypes.length>0) {
					whereList.add(query.toString());
				}
	
			}
	
			// TODO: Temporary logic, filenames should each have their own field in the database later 
			// and they may also be a different format
			// (this will eliminate the need for the _% LIKE logic also)
			// _ matches exactly one character and % matches zero to many, so _% matches at least one arbitrary character
			if(Globals.saneInput(searchInputs.needsComments)) {
	//			whereList.add(" (documents LIKE 'CommentLetters-_%' OR documents LIKE 'EisDocuments-_%;CommentLetters-_%')");
				whereList.add(" (comments_filename<>'')");
			}
	
			if(Globals.saneInput(searchInputs.needsDocument)) { // Don't need an input for this right now
	//			whereList.add(" (documents LIKE 'EisDocuments-_%' OR documents LIKE 'EisDocuments-_%;CommentLetters-_%')");
				whereList.add(" (filename<>'')");
			}
			
			if(Globals.saneInput(searchInputs.state)) {
				StringBuilder query = new StringBuilder(" state IN (");
				for (int i = 0; i < searchInputs.state.length; i++) {
					if (i > 0) {
						query.append(",");
					}
					query.append("?");
				}
				query.append(")");
	
				for (int i = 0; i < searchInputs.state.length; i++) {
					inputList.add(searchInputs.state[i]);
				}
				whereList.add(query.toString());
			}
	
			if(Globals.saneInput(searchInputs.agency)) {
				StringBuilder query = new StringBuilder(" agency IN (");
				for (int i = 0; i < searchInputs.agency.length; i++) {
					if (i > 0) {
						query.append(",");
					}
					query.append("?");
				}
				query.append(")");
	
				for (int i = 0; i < searchInputs.agency.length; i++) {
					inputList.add(searchInputs.agency[i]);
				}
				whereList.add(query.toString());
			}
			
			boolean addAnd = false;
			for (String i : whereList) {
				if(addAnd) { // Not first conditional, append AND
					sQuery += " AND";
				} else { // First conditional, append WHERE
					sQuery += " WHERE";
				}
				sQuery += i; // Append conditional
				
				addAnd = true; // Raise AND flag for future iterations
			}
			
			// Order by Lucene score, not title, also we need a way to order the title results first for one of the A|B tests
//			sQuery += " ORDER BY title";
			
			
			// This is unfortunately the only way to preserve Lucene's order
//			if(saneTitle) {
//				StringBuilder query = new StringBuilder(" ORDER BY FIELD(id, ");
//				for (int i = 0; i < new_ids.size(); i++) {
//					if (i > 0) {
//						query.append(",");
//					}
//					query.append("?");
//				}
//				query.append(")");
//	
//				for (int i = 0; i < new_ids.size(); i++) {
//					inputList.add(new_ids.get(i).toString());
//				}
//				whereList.add(query.toString());
//			}
			
			
			// Finalize query
			
			// No reason to limit metadata-only search
			int queryLimit = 1000000;
			
			sQuery += " LIMIT " + String.valueOf(queryLimit);

//			jpaQuery.setCriteriaQuery(criteria);

			// TODO: Is this usable?
//			org.apache.lucene.search.Query finalQuery = luceneQueryParser.parse(sQuery);
			
//			javax.persistence.Query query = em.createQuery("SELECT DISTINCT doc.eisdoc FROM DocumentText doc WHERE doc.id IN :ids");
			// Finalize query
//			javax.persistence.Query finalQuery = em.createQuery(sQuery);
//			finalQuery.setMaxResults(limit);
//			finalQuery.setFirstResult(offset);
			
			// execute search
//			List<EISDoc> docList = finalQuery.getResultList();
			
			
			
			// Run query
			List<EISDoc> records = jdbcTemplate.query
			(
				sQuery, 
				inputList.toArray(new Object[] {}),
				(rs, rowNum) -> new EISDoc(
					rs.getLong("id"), 
					rs.getString("title"), 
					rs.getString("document_type"),
					rs.getObject("comment_date", LocalDate.class), 
					rs.getObject("register_date", LocalDate.class), 
					rs.getString("agency"),
					rs.getString("department"),
					rs.getString("cooperating_agency"),
					rs.getString("summary_text"),
					rs.getString("state"), 
					rs.getString("filename"),
					rs.getString("comments_filename"),
					rs.getString("folder"),
					rs.getLong("size"),
					rs.getString("web_link"),
					rs.getString("notes"),
					rs.getObject("noi_date", LocalDate.class), 
					rs.getObject("draft_noa", LocalDate.class), 
					rs.getObject("final_noa", LocalDate.class), 
					rs.getObject("first_rod_date", LocalDate.class)
				)
			);
			
			// debugging
			if(Globals.TESTING) {
				System.out.println(sQuery); 
			}

			// If we have a title then take the JDBC results and run a Lucene query on just them
			// (this is the simplest way to return the results in the scored order from Lucene)
			// Unfortunately, this low-level garbage breaks things like ~proximity matching and "exact phrase" searches.
			// TODO: Therefore, we have to run the lucene query on everything and manually join the results instead,
			// excluding anything that doesn't appear in BOTH result sets
//			if(searchInputs != null && searchInputs.title != null && !searchInputs.title.isBlank()) {
//				String formattedTitle = org.apache.commons.lang3.StringUtils.normalizeSpace(searchInputs.title.strip());
//
//				FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
//
//				QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
//						.buildQueryBuilder().forEntity(EISDoc.class).get();
//				
//				String[] arrKeywords = formattedTitle.split(" ");
//				
//				List<Query> queryList = new LinkedList<Query>();
//		        Query query = null;
//	
//				// Add keyword queries for each word
//		        for (String keyword : arrKeywords) {
//		            query = queryBuilder.keyword().onField("title").matching(keyword).createQuery();
//		            queryList.add(query);
//		        }
//
//		        BooleanQuery.setMaxClauseCount(200000);
//		        BooleanQuery finalQuery = new BooleanQuery();
//		        for (Query q : queryList) {
//		            finalQuery.add(q, Occur.MUST);
//		        }
//				for(EISDoc record: records) {
//		            finalQuery.add(
//		            		new TermQuery(new Term("ID", record.getId().toString())), Occur.SHOULD);
//				}
//	
//				org.hibernate.search.jpa.FullTextQuery jpaQuery =
//						fullTextEntityManager.createFullTextQuery(finalQuery, EISDoc.class);
//				
//				jpaQuery.setMaxResults(limit);
//				jpaQuery.setFirstResult(offset);
//				
//				List<EISDoc> results = jpaQuery.getResultList();
//				
//				return results;
//			} else {
//				return records;
//			}
			
			// Run Lucene query on title if we have one, join with JDBC results, return final results
			if(!searchInputs.title.isBlank()) {

				List<EISDoc> results = searchTitles(searchInputs.title);
				
				HashSet<Long> justRecordIds = new HashSet<Long>();
				for(EISDoc record: records) {
					justRecordIds.add(record.getId());
				}
				
				// Build new result list in the same order but excluding records that don't appear in the first result set (records).
				List<EISDoc> finalResults = new ArrayList<EISDoc>();
				for(EISDoc result : results) {
					if(justRecordIds.contains(result.getId())) {
						finalResults.add(result);
					}
				}
				
				if(Globals.TESTING) {
					System.out.println("Records filtered " + records.size());
					System.out.println("Records by term " + results.size());
				}
				
				return finalResults;
			} else { // no title: simply return JDBC results
				return records;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	
	}
	
	/** Uses full parameters (not just a String for terms) to narrow down results */
	private List<EISDoc> getFilteredRecords(SearchInputs searchInputs) {
		searchInputs.title = mutateTermModifiers(searchInputs.title);
		
		ArrayList<String> inputList = new ArrayList<String>();
		ArrayList<String> whereList = new ArrayList<String>();

		// Select tables, columns
		String sQuery = "SELECT * FROM eisdoc";
		
		// Populate lists
		if(Globals.saneInput(searchInputs.startPublish)) {
			inputList.add(searchInputs.startPublish);
			whereList.add(" ((register_date) >= ?)");
		}
		
		if(Globals.saneInput(searchInputs.endPublish)) {
			inputList.add(searchInputs.endPublish);
			whereList.add(" ((register_date) <= ?)");
		}

		if(Globals.saneInput(searchInputs.startComment)) {
			inputList.add(searchInputs.startComment);
			whereList.add(" ((comment_date) >= ?)");
		}
		
		if(Globals.saneInput(searchInputs.endComment)) {
			inputList.add(searchInputs.endComment);
			whereList.add(" ((comment_date) <= ?)");
		}
		
		if(Globals.saneInput(searchInputs.typeAll)) { 
			// do nothing
		} else {
			
			ArrayList<String> typesList = new ArrayList<>();
			StringBuilder query = new StringBuilder(" document_type IN (");
			if(Globals.saneInput(searchInputs.typeFinal)) {
				typesList.add("Final");
			}

			if(Globals.saneInput(searchInputs.typeDraft)) {
				typesList.add("Draft");
			}
			
			if(Globals.saneInput(searchInputs.typeOther)) {
				typesList.addAll(Globals.EIS_TYPES);
			}
			String[] docTypes = typesList.toArray(new String[0]);
			for (int i = 0; i < docTypes.length; i++) {
				if (i > 0) {
					query.append(",");
				}
				query.append("?");
			}
			query.append(")");

			for (int i = 0; i < docTypes.length; i++) {
				inputList.add(docTypes[i]);
			}
			
			if(docTypes.length>0) {
				whereList.add(query.toString());
			}

		}

		if(Globals.saneInput(searchInputs.needsComments)) { // Don't need an input for this right now
			whereList.add(" (comments_filename<>'')");
		}

		if(Globals.saneInput(searchInputs.needsDocument)) { // Don't need an input for this right now
			whereList.add(" (filename<>'')");
		}
		
		if(Globals.saneInput(searchInputs.state)) {
			StringBuilder query = new StringBuilder(" state IN (");
			for (int i = 0; i < searchInputs.state.length; i++) {
				if (i > 0) {
					query.append(",");
				}
				query.append("?");
			}
			query.append(")");

			for (int i = 0; i < searchInputs.state.length; i++) {
				inputList.add(searchInputs.state[i]);
			}
			whereList.add(query.toString());
		}

		if(Globals.saneInput(searchInputs.agency)) {
			StringBuilder query = new StringBuilder(" agency IN (");
			for (int i = 0; i < searchInputs.agency.length; i++) {
				if (i > 0) {
					query.append(",");
				}
				query.append("?");
			}
			query.append(")");

			for (int i = 0; i < searchInputs.agency.length; i++) {
				inputList.add(searchInputs.agency[i]);
			}
			whereList.add(query.toString());
		}
		
		boolean addAnd = false;
		for (String i : whereList) {
			if(addAnd) { // Not first conditional, append AND
				sQuery += " AND";
			} else { // First conditional, append WHERE
				sQuery += " WHERE";
			}
			sQuery += i; // Append conditional
			
			addAnd = true; // Raise AND flag for future iterations
		}
		
		
		// Finalize query
		int queryLimit = 1000000;

		// Note: For the metadata results, query is very fast and since we use this dataset for a join/comparison later
		// we do not want to limit it (for now, 1 million is fine)
//		if(Globals.saneInput(searchInputs.limit)) {
//			if(searchInputs.limit <= 100000) {
//				queryLimit = searchInputs.limit;
//			}
//		}
		
		
		sQuery += " LIMIT " + String.valueOf(queryLimit);

		// Run query
		List<EISDoc> records = jdbcTemplate.query
		(
			sQuery, 
			inputList.toArray(new Object[] {}),
			(rs, rowNum) -> new EISDoc(
				rs.getLong("id"), 
				rs.getString("title"), 
				rs.getString("document_type"),
				rs.getObject("comment_date", LocalDate.class), 
				rs.getObject("register_date", LocalDate.class), 
				rs.getString("agency"),
				rs.getString("department"),
				rs.getString("cooperating_agency"),
				rs.getString("summary_text"),
				rs.getString("state"), 
				rs.getString("filename"),
				rs.getString("comments_filename"),
				rs.getString("folder"),
				rs.getLong("size"),
				rs.getString("web_link"),
				rs.getString("notes"),
				rs.getObject("noi_date", LocalDate.class), 
				rs.getObject("draft_noa", LocalDate.class), 
				rs.getObject("final_noa", LocalDate.class), 
				rs.getObject("first_rod_date", LocalDate.class)
			)
		);

		// debugging
		if(Globals.TESTING) {
//			if(searchInputs.endPublish != null) {
//				DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
//				DateValidator validator = new DateValidatorUsingLocalDate(dateFormatter);
//				System.out.println(validator.isValid(searchInputs.endPublish));
//				System.out.println(searchInputs.endPublish);
//			}
//			System.out.println(sQuery); 
//			System.out.println(searchInputs.title);
		}
		
		return records;
	}
	
	// objective: Search both fields at once and return quickly in combined scored order
	@Override
	public List<Object[]> getRaw(String terms) throws ParseException {
		long startTime = System.currentTimeMillis();
		
		// Normalize whitespace and support added term modifiers
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(mutateTermModifiers(terms).strip());

	    if(Globals.TESTING) {System.out.println("Search terms: " + formattedTerms);}
	    
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		// Lucene flattens (denormalizes) and so searching both tables at once is simple enough, 
		// but the results will contain both types mixed together
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
					new String[] {"title", "plaintext"},
					new StandardAnalyzer());
		mfqp.setDefaultOperator(Operator.AND);

		Query luceneQuery = mfqp.parse(formattedTerms);
		
		org.hibernate.search.jpa.FullTextQuery jpaQuery =
					fullTextEntityManager.createFullTextQuery(luceneQuery);

		// Ex: [[8383,"nepaBackend.model.EISDoc",0.8749341],[1412,"nepaBackend.model.DocumentText",0.20437382]]
		jpaQuery.setProjection(
					ProjectionConstants.ID
					,ProjectionConstants.OBJECT_CLASS
					,ProjectionConstants.SCORE
					);
		jpaQuery.setMaxResults(1000000);
		jpaQuery.setFirstResult(0);
		
		List<Object[]> results = jpaQuery.getResultList();

		if(Globals.TESTING) {
			System.out.println("Results #: " + results.size());
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Time elapsed: " + elapsedTime);
		}
		
		return results;
	}

	// objective: Search both fields at once and return quickly in combined scored order
	@Override
	public List<MetadataWithContext2> getScored(String terms) throws ParseException {
		long startTime = System.currentTimeMillis();

		// Normalize whitespace and support added term modifiers
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(mutateTermModifiers(terms).strip());

	    if(Globals.TESTING) {System.out.println("Search terms: " + formattedTerms);}
	    
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		// Lucene flattens (denormalizes) and so searching both tables at once is simple enough, 
		// but the results will contain both types mixed together
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
					new String[] {"title", "plaintext"},
					new StandardAnalyzer());
		mfqp.setDefaultOperator(Operator.AND);

		Query luceneQuery = mfqp.parse(formattedTerms);
		
		org.hibernate.search.jpa.FullTextQuery jpaQuery =
				fullTextEntityManager.createFullTextQuery(luceneQuery);

		// Ex: [[8383,"nepaBackend.model.EISDoc",0.8749341],[1412,"nepaBackend.model.DocumentText",0.20437382]]
		jpaQuery.setProjection(
				ProjectionConstants.ID
				,ProjectionConstants.OBJECT_CLASS
				,ProjectionConstants.SCORE
				);
		jpaQuery.setMaxResults(1000000);
		jpaQuery.setFirstResult(0);
		
		// Lazy fetching isn't so easy here with combined results, so the goal is to get the order
		// first and then get all of the results maintaining that order but without getting full
		// texts which is slow and also overflows the heap
		
		// Could potentially try to get ProjectionConstants.ID and ProjectionConstants.SCORE
		// for two separate searches, join and sort by score,
		// then get the metadata and filenames.  This would maintain the order.
		
		// Returns a list containing both EISDoc and DocumentText objects.
		List<Object[]> results = jpaQuery.getResultList();
		
		if(Globals.TESTING) {System.out.println("Initial results size: " + results.size());}
		
		List<ScoredResult> converted = new ArrayList<ScoredResult>();
		Set<Long> metaIds = new HashSet<Long>();
		Set<Long> textIds = new HashSet<Long>();
		
		int i = 0;
		
		for(Object[] result : results) {
			ScoredResult convert = new ScoredResult();
			convert.id = (Long) result[0];
			convert.className = (Class<?>) result[1];
			convert.score = (Float) result[2];
			convert.idx = i;
			if(convert.className.equals(EISDoc.class)) {
				metaIds.add(convert.id);
			} else {
				textIds.add(convert.id);
			}
			converted.add(convert);
			i++;
		}
		
		// [8383,"nepaBackend.model.EISDoc"]
		// ProjectionConstants.SCORE could also give score to sort by.
		
		// 1: Get EISDocs by IDs.
		
		List<EISDoc> docs = em.createQuery("SELECT d FROM EISDoc d WHERE d.id IN :ids")
			.setParameter("ids", metaIds).getResultList();

		if(Globals.TESTING){System.out.println("Docs results size: " + docs.size());}
		
		HashMap<Long, EISDoc> hashDocs = new HashMap<Long, EISDoc>();
		for(EISDoc doc : docs) {
			hashDocs.put(doc.getId(), doc);
		}

		// 2: Get DocumentTexts by IDs WITHOUT getting the entire texts.

		List<Object[]> textIdMetaAndFilenames = em.createQuery("SELECT d.id, d.eisdoc, d.filename FROM DocumentText d WHERE d.id IN :ids")
				.setParameter("ids", textIds).getResultList();

		if(Globals.TESTING){System.out.println("Texts results size: " + textIdMetaAndFilenames.size());}
		
		HashMap<Long, ReducedText> hashTexts = new HashMap<Long, ReducedText>();
		for(Object[] obj : textIdMetaAndFilenames) {
			hashTexts.put(
					(Long) obj[0], 
					new ReducedText(
						(Long) obj[0],
						(EISDoc) obj[1],
						(String) obj[2]
					));
		}
		
		List <MetadataWithContext2> combinedResults = new ArrayList<MetadataWithContext2>();

		// 3: Join (combine) results from the two tables
		// 3.1: Condense (add filenames to existing records rather than adding new records)
		// 3.2: keep original order
		
		HashMap<Long, Integer> added = new HashMap<Long, Integer>();
		int position = 0;
		
		for(ScoredResult ordered : converted) {
			if(ordered.className.equals(EISDoc.class)) {
				if(!added.containsKey(ordered.id)) {
					// Add EISDoc into logical position
					combinedResults.add(new MetadataWithContext2(
							hashDocs.get(ordered.id),
							new ArrayList<String>(),
							"",
							ordered.score));
					added.put(ordered.id, position);
					position++;
				}
				// If we already have one, do nothing - (title result: no filenames to add.)
			} else {
				EISDoc eisFromDoc = hashTexts.get(ordered.id).eisdoc;
				if(!added.containsKey(eisFromDoc.getId())) {
					// Add DocumentText into logical position
					combinedResults.add(new MetadataWithContext2(
							eisFromDoc,
							new ArrayList<String>(),
							hashTexts.get(ordered.id).filename,
							ordered.score));
					added.put(eisFromDoc.getId(), position);
					position++;
				} else {
					// Add this combinedResult's filename to filename list
					String currentFilename = combinedResults.get(added.get(eisFromDoc.getId()))
							.getFilenames();
					// > is not a valid directory/filename char, so should work as delimiter
					// If currentFilename is blank (title match came first), no need to concat.  Just set.
					if(currentFilename.isBlank()) {
						combinedResults.get(added.get(eisFromDoc.getId()))
						.setFilenames(
							hashTexts.get(ordered.id).filename
						);
					} else {
						combinedResults.get(added.get(eisFromDoc.getId()))
						.setFilenames(
							currentFilename.concat(">" + hashTexts.get(ordered.id).filename)
						);
					}
				}
			}
		}
		
		if(Globals.TESTING) {
			System.out.println("Results #: " + results.size());
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Time elapsed: " + elapsedTime);
		}
		
		return combinedResults;
	}
	


	/** Combination title/fulltext query including the metadata parameters like agency/state/...
			 * and this is currently the default search; returns metadata plus filename 
			 * using Lucene's internal default scoring algorithm
			 * @throws ParseException
			 * */
	@Override
	public List<MetadataWithContext2> CombinedSearchNoContext(SearchInputs searchInputs, SearchType searchType) {
		try {
			long startTime = System.currentTimeMillis();
			
			if(Globals.TESTING) {
				System.out.println("Offset: " + searchInputs.offset);
			}
			
			List<EISDoc> records = getFilteredRecords(searchInputs);
			
			// Run Lucene query on title if we have one, join with JDBC results, return final results
			if(!searchInputs.title.isBlank()) {
				String title = searchInputs.title;

				// Collect IDs filtered by params
				HashSet<Long> justRecordIds = new HashSet<Long>();
				for(EISDoc record: records) {
					justRecordIds.add(record.getId());
				}

				List<MetadataWithContext2> results = getScored(title);
				
				// Build new result list in the same order but excluding records that don't appear in the first result set (records).
				List<MetadataWithContext2> finalResults = new ArrayList<MetadataWithContext2>();
				for(int i = 0; i < results.size(); i++) {
					if(justRecordIds.contains(results.get(i).getDoc().getId())) {
						finalResults.add(results.get(i));
					}
				}
				
				if(Globals.TESTING) {
					System.out.println("Records 1 " + records.size());
					System.out.println("Records 2 " + results.size());
				}

				if(Globals.TESTING) {
					long stopTime = System.currentTimeMillis();
					long elapsedTime = stopTime - startTime;
					System.out.println("Lucene search time: " + elapsedTime);
				}
				return results;
			} else { // no title: simply return JDBC results...  however they have to be translated
				// TODO: If we care to avoid this, frontend has to know if it's sending a title or not, and ask for the appropriate
				// return type (either EISDoc or MetadataWithContext), and then we need two versions of the search on the backend
				List<MetadataWithContext2> finalResults = new ArrayList<MetadataWithContext2>();
				for(EISDoc record : records) {
					finalResults.add(new MetadataWithContext2(record, new ArrayList<String>(), "", 0));
				}
				return finalResults;
			}
			
//			return lucenePrioritySearch(searchInputs.title, limit, offset);
		} catch(Exception e) {
			e.printStackTrace();
			String problem = e.getLocalizedMessage();
			MetadataWithContext2 result = new MetadataWithContext2(null, new ArrayList<String>(), problem, 0);
			List<MetadataWithContext2> results = new ArrayList<MetadataWithContext2>();
			results.add(result);
			return results;
		}
	}

	/** Combination title/fulltext query including the metadata parameters like agency/state/...
	 * returns metadata plus filename 
	 * using Lucene's internal default scoring algorithm
	 * @throws ParseException
	 * */
//	@Override
//	public List<MetadataWithContext2> CombinedSearchNoContextOld(SearchInputs searchInputs, SearchType searchType) {
//		try {
//			long startTime = System.currentTimeMillis();
////			System.out.println("Offset: " + searchInputs.offset);
//			List<EISDoc> records = getFilteredRecords(searchInputs);
//			
//			// Run Lucene query on title if we have one, join with JDBC results, return final results
//			if(!searchInputs.title.isBlank()) {
//				String formattedTitle = mutateTermModifiers(searchInputs.title);
//
//				HashSet<Long> justRecordIds = new HashSet<Long>();
//				for(EISDoc record: records) {
//					justRecordIds.add(record.getId());
//				}
//
//				List<MetadataWithContext2> results = searchNoContext(formattedTitle, searchInputs.limit, searchInputs.offset, justRecordIds);
//				
//				// Build new result list in the same order but excluding records that don't appear in the first result set (records).
//				List<MetadataWithContext2> finalResults = new ArrayList<MetadataWithContext2>();
//				for(int i = 0; i < results.size(); i++) {
//					if(justRecordIds.contains(results.get(i).getDoc().getId())) {
//						finalResults.add(results.get(i));
//					}
//				}
//				
//				if(Globals.TESTING) {
//					System.out.println("Records 1 " + records.size());
//					System.out.println("Records 2 " + results.size());
//				}
//
//				if(Globals.TESTING) {
//					long stopTime = System.currentTimeMillis();
//					long elapsedTime = stopTime - startTime;
//					System.out.println("Lucene search time: " + elapsedTime);
//				}
//				return finalResults;
//			} else { // no title: simply return JDBC results...  however they have to be translated
//				// TODO: If we care to avoid this, frontend has to know if it's sending a title or not, and ask for the appropriate
//				// return type (either EISDoc or MetadataWithContext), and then we need two versions of the search on the backend
//				List<MetadataWithContext2> finalResults = new ArrayList<MetadataWithContext2>();
//				for(EISDoc record : records) {
//					finalResults.add(new MetadataWithContext2(record, new ArrayList<String>(), ""));
//				}
//				return finalResults;
//			}
//			
////			return lucenePrioritySearch(searchInputs.title, limit, offset);
//		} catch(Exception e) {
//			e.printStackTrace();
//			return new ArrayList<MetadataWithContext2>();
//		}
//	}
	
	/** "A/B testing" search functions: */

	/** Combination title/fulltext query including the metadata parameters like agency/state/...
	 * and this is currently the default search; returns metadata plus filename and highlights
	 * using Lucene's internal default scoring algorithm
	 * @throws ParseException
	 * */
	@Override
	public List<MetadataWithContext> CombinedSearchLucenePriority(SearchInputs searchInputs, SearchType searchType) {
		try {
			long startTime = System.currentTimeMillis();
			if(Globals.TESTING) {System.out.println("Offset: " + searchInputs.offset);}
			List<EISDoc> records = getFilteredRecords(searchInputs);
			
			// Run Lucene query on title if we have one, join with JDBC results, return final results
			if(!searchInputs.title.isBlank()) {
				String formattedTitle = mutateTermModifiers(searchInputs.title);

				HashSet<Long> justRecordIds = new HashSet<Long>();
				for(EISDoc record: records) {
					justRecordIds.add(record.getId());
				}

				List<MetadataWithContext> results = lucenePrioritySearch(formattedTitle, searchInputs.limit, searchInputs.offset, justRecordIds);
				
				// Build new result list in the same order but excluding records that don't appear in the first result set (records).
				List<MetadataWithContext> finalResults = new ArrayList<MetadataWithContext>();
				for(int i = 0; i < results.size(); i++) {
					if(justRecordIds.contains(results.get(i).getDoc().getId())) {
						finalResults.add(results.get(i));
					}
				}
				
				if(Globals.TESTING) {
					System.out.println("Records 1 " + records.size());
					System.out.println("Records 2 " + results.size());
				}

				if(Globals.TESTING) {
					long stopTime = System.currentTimeMillis();
					long elapsedTime = stopTime - startTime;
					System.out.println("Lucene search time: " + elapsedTime);
				}
				return finalResults;
			} else { // no title: simply return JDBC results...  however they have to be translated
				// TODO: If we care to avoid this, frontend has to know if it's sending a title or not, and ask for the appropriate
				// return type (either EISDoc or MetadataWithContext), and then we need two versions of the search on the backend
				List<MetadataWithContext> finalResults = new ArrayList<MetadataWithContext>();
				for(EISDoc record : records) {
					finalResults.add(new MetadataWithContext(record, "", ""));
				}
				return finalResults;
			}
			
//			return lucenePrioritySearch(searchInputs.title, limit, offset);
		} catch(Exception e) {
			e.printStackTrace();
			return new ArrayList<MetadataWithContext>();
		}
	}
	
	/** Title matches brought to top
	 * @throws ParseException*/
	@Override
	public List<MetadataWithContext> CombinedSearchTitlePriority(SearchInputs searchInputs, SearchType searchType) {
		try {
			long startTime = System.currentTimeMillis();
			List<EISDoc> records = getFilteredRecords(searchInputs);
			
			// Run Lucene query on title if we have one, join with JDBC results, return final results
			if(!searchInputs.title.isBlank()) {
				String formattedTitle = mutateTermModifiers(searchInputs.title);

				HashSet<Long> justRecordIds = new HashSet<Long>();
				for(EISDoc record: records) {
					justRecordIds.add(record.getId());
				}
				
				List<MetadataWithContext> results = titlePrioritySearch(formattedTitle, searchInputs.limit, searchInputs.offset, justRecordIds);

				// Build new result list in the same order but excluding records that don't appear in the first result set (records).
				List<MetadataWithContext> finalResults = new ArrayList<MetadataWithContext>();
				for(int i = 0; i < results.size(); i++) {
					if(justRecordIds.contains(results.get(i).getDoc().getId())) {
						finalResults.add(results.get(i));
					}
				}
				
				if(Globals.TESTING) {
					System.out.println("Records 1 " + records.size());
					System.out.println("Records 2 " + results.size());
				}

				if(Globals.TESTING) {
					long stopTime = System.currentTimeMillis();
					long elapsedTime = stopTime - startTime;
					System.out.println("Manual search time: " + elapsedTime);
				}
				return finalResults;
			} else { // no title: simply return JDBC results...  however they have to be translated
				// TODO: If we care to avoid this, frontend has to know if it's sending a title or not, and ask for the appropriate
				// return type (either EISDoc or MetadataWithContext), and then we need two versions of the search on the backend
				List<MetadataWithContext> finalResults = new ArrayList<MetadataWithContext>();
				for(EISDoc record : records) {
					finalResults.add(new MetadataWithContext(record, "", ""));
				}
				return finalResults;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	
	}
	
	private List<EISDoc> getFulltextMetaResults(String field, int limit, int offset) throws ParseException{

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		
		QueryParser qp = new QueryParser("title", new StandardAnalyzer());
		qp.setDefaultOperator(Operator.AND);

		// this may throw a ParseException which the caller has to deal with
		Query luceneQuery = qp.parse(field);

//		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
//				.buildQueryBuilder().forEntity(EISDoc.class).get();
//
//		Query luceneQuery = queryBuilder
//				.simpleQueryString()
//				.onField("title")
//				.withAndAsDefaultOperator()
//				.matching(field)
//				.createQuery();
//		
		org.hibernate.search.jpa.FullTextQuery jpaQuery =
				fullTextEntityManager.createFullTextQuery(luceneQuery, EISDoc.class);
		
		jpaQuery.setMaxResults(limit);
		jpaQuery.setFirstResult(offset);
		
		List<EISDoc> results = jpaQuery.getResultList();
		
		return results;
		
	}

	
	private List<DocumentText> getFulltextResults(String field, int limit, int offset) throws ParseException{
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryParser qp = new QueryParser("plaintext", new StandardAnalyzer());
		qp.setDefaultOperator(Operator.AND);

		// this may throw a ParseException which the caller has to deal with
		Query luceneQuery = qp.parse(field);
		
//		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
//				.buildQueryBuilder().forEntity(DocumentText.class).get();
//
//		Query luceneQuery = queryBuilder
//				.simpleQueryString()
//				.onField("plaintext")
//				.withAndAsDefaultOperator()
//				.matching(field)
//				.createQuery();
		
		org.hibernate.search.jpa.FullTextQuery jpaQuery =
				fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class);
		
		jpaQuery.setMaxResults(limit);
		jpaQuery.setFirstResult(offset);
		
		List<DocumentText> results = jpaQuery.getResultList();
		
		return results;
		
	}
	
	// (probably O(n)) list merge
	private List<MetadataWithContext> mergeResultsWithHighlights(String field, final List<EISDoc> metadataList, final List<DocumentText> textList, final HashSet<Long> justRecordIds) throws IOException, ParseException {
    	// metadatawithcontext results so we can have a text field with all combined text results
		// LinkedHashMap should retain the order of the Lucene-scored results while also using advantages of a hashmap
//	    Map<Long, MetadataWithContext> combinedMap = new LinkedHashMap<Long, MetadataWithContext>();

//	    for (final EISDoc metaDoc : metadataList) {
//	    	MetadataWithContext translatedDoc = new MetadataWithContext(metaDoc, "", "");
//	        combinedMap.put(metaDoc.getId(), translatedDoc);
//	    }
		
		List<MetadataWithContext> combinedResults = new ArrayList<MetadataWithContext>();
		
		for(EISDoc item: metadataList) {
			combinedResults.add(new MetadataWithContext(item, "", ""));
		}

		// build highlighter
		QueryParser qp = new QueryParser("plaintext", new StandardAnalyzer());
		qp.setDefaultOperator(Operator.AND);
		
		// this may throw a ParseException which the caller has to deal with
		Query luceneQuery = qp.parse(field);
		QueryScorer scorer = new QueryScorer(luceneQuery);
		Highlighter highlighter = new Highlighter(globalFormatter, scorer);
		Fragmenter fragmenter = new SimpleFragmenter(fragmentSize);
		highlighter.setTextFragmenter(fragmenter);
		highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

		// TODO: This is probably not what we want.  What we may want is to add filename, highlights to EISDoc if it has none already.
		// Else append to list.  Figure out how to either "boost" title results or "sort" or "order" by title.
		// Preferably we don't have to go back to the QueryBuilder to do this because then we have to figure out how to support "?" term modifier
	    for (final DocumentText docText : textList) {

	    	// justRecordIds is our filter, if this doesn't join then don't add it
	    	if(justRecordIds.contains(docText.getEisdoc().getId())) {
	    		final String highlights = getHighlightString(docText.getPlaintext(), highlighter);
		    	if(!highlights.isBlank()) {
		    		combinedResults.add(new MetadataWithContext(docText.getEisdoc(), highlights, docText.getFilename()));
		    	} else {
		    		// shouldn't be possible since we matched
		    		System.out.println("Blank highlight for " + docText.getFilename() + " for term " + field + " text length " + docText.getPlaintext().length());
		    	}
	    	} 
	    }

	    return new ArrayList<MetadataWithContext>(combinedResults);
	}
	
	public List<MetadataWithContext> titlePrioritySearch(String terms, int limit, int offset, HashSet<Long> justRecordIds) throws ParseException {
		if(terms.isBlank()) {
			return new ArrayList<MetadataWithContext>();
		}
		
		// 0: Normalize whitespace and support all term modifiers
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(mutateTermModifiers(terms).strip());
	    
		// 1: Search title; now have result list in scored order
		List<EISDoc> titleResults = getFulltextMetaResults(formattedTerms, limit, offset);
		// 2: Search file texts
		List<DocumentText> fileTextResults = getFulltextResults(formattedTerms, limit, offset);

		// 3: Add texts to existing objects in list if matching, otherwise append (like a right outer join with left results ordered first)
		try {
			List<MetadataWithContext> combinedResults = mergeResultsWithHighlights(formattedTerms, titleResults, fileTextResults, justRecordIds);

			if(Globals.TESTING) {
				System.out.println("Title results " + titleResults.size());
				System.out.println("Text results " + fileTextResults.size());
				System.out.println("Combined results " + combinedResults.size());
			}

			// 4: Return list
			return combinedResults;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	

	// objective: Search both fields at once, connect fragments and return
	public List<MetadataWithContext> lucenePrioritySearch(String terms, int limit, int offset, HashSet<Long> justRecordIds) throws ParseException {
		long startTime = System.currentTimeMillis();
		// Normalize whitespace and support added term modifiers
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(mutateTermModifiers(terms).strip());

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		// Lucene flattens (denormalizes) and so searching both tables at once is simple enough, 
		// but the results will contain both types mixed together
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
					new String[] {"title", "plaintext"},
					new StandardAnalyzer());
		mfqp.setDefaultOperator(Operator.AND);

		Query luceneQuery = mfqp.parse(formattedTerms);
		
		org.hibernate.search.jpa.FullTextQuery jpaQuery =
				fullTextEntityManager.createFullTextQuery(luceneQuery);
		
		jpaQuery.setMaxResults(limit);
		jpaQuery.setFirstResult(offset);

		if(Globals.TESTING) {System.out.println("Query using limit " + limit);}
		
		// Returns a list containing both EISDoc and DocumentText objects.
		List<Object> results = jpaQuery.getResultList();
		
		// init final result list
		List<MetadataWithContext> combinedResultsWithHighlights = new ArrayList<MetadataWithContext>();
		
		// build highlighter
		QueryParser qp = new QueryParser("plaintext", new StandardAnalyzer());
		qp.setDefaultOperator(Operator.AND);
		Query luceneTextOnlyQuery = qp.parse(formattedTerms);
		QueryScorer scorer = new QueryScorer(luceneTextOnlyQuery);
		Highlighter highlighter = new Highlighter(globalFormatter, scorer);
		Fragmenter fragmenter = new SimpleFragmenter(fragmentSize);
		highlighter.setTextFragmenter(fragmenter);
		highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
		
		// Condense results:
		// If we have companion results (same EISDoc.ID), combine

		// Quickly build a HashMap of EISDoc (AKA metadata) IDs; these are unique
		// (we'll use these to condense the results on pass 2)
		HashMap<Long, Integer> metaIds = new HashMap<Long, Integer>(results.size());
		int position = 0;
		for (Object result : results) {
			if(result.getClass().equals(EISDoc.class)) {
				metaIds.put(((EISDoc) result).getId(), position);
			}
			position++;
		}
		
		HashMap<Long, Boolean> skipThese = new HashMap<Long, Boolean>();
		
		position = 0;
		for (Object result : results) {

			if(result.getClass().equals(DocumentText.class) && justRecordIds.contains(((DocumentText) result).getEisdoc().getId())) {
				
				try {
					long key = ((DocumentText) result).getEisdoc().getId();

					// Get highlights
					MetadataWithContext combinedResult = new MetadataWithContext(
							((DocumentText) result).getEisdoc(),
							getHighlightString(((DocumentText) result).getPlaintext(), highlighter),
							((DocumentText) result).getFilename());

					// If we have a companion result:
					if(metaIds.containsKey(key)) {
						// If this Text result comes before the Meta result:
						if(metaIds.get(key) > position) {
							// Flag to skip over the Meta result later
							skipThese.put(key, true);
							// Add this combinedResult to List
							combinedResultsWithHighlights.add( combinedResult );
						} else {
							// We already have a companion meta result in the table
							// If existing result has no highlight:
							if(combinedResultsWithHighlights.get(metaIds.get(key)).getHighlight().isBlank()) {
								// "update" that instead of adding this result
								combinedResultsWithHighlights.set(metaIds.get(key), combinedResult);
							} else {
								// Add this combinedResult to List
								combinedResultsWithHighlights.add( combinedResult );
							}
						}
					} else {
						// Add this companionless combinedResult to List
						combinedResultsWithHighlights.add( combinedResult );
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if(result.getClass().equals(EISDoc.class)) {
				// Add metadata result unless it's flagged for skipping
				if(!skipThese.containsKey(((EISDoc) result).getId())) {
					combinedResultsWithHighlights.add(new MetadataWithContext(((EISDoc) result),"",""));
				}
			}
			position++;
		}
		
		if(Globals.TESTING) {
			System.out.println("Results #: " + results.size());
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Time elapsed: " + elapsedTime);
		}

		return combinedResultsWithHighlights;
	}
	
	@Deprecated
	@SuppressWarnings("unchecked")
	public List<MetadataWithContext2> searchNoContext(String terms, int limit, int offset, HashSet<Long> justRecordIds) throws ParseException {
		long startTime = System.currentTimeMillis();

		// Normalize whitespace and support added term modifiers
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(mutateTermModifiers(terms).strip());

		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		// Lucene flattens (denormalizes) and so searching both tables at once is simple enough, 
		// but the results will contain both types mixed together
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
					new String[] {"title", "plaintext"},
					new StandardAnalyzer());
		mfqp.setDefaultOperator(Operator.AND);

		Query luceneQuery = mfqp.parse(formattedTerms);
		
//			org.hibernate.search.jpa.FullTextQuery jpaQuery =
//					fullTextEntityManager.createFullTextQuery(luceneQuery, DocumentText.class); // filters only DocumentText results
		org.hibernate.search.jpa.FullTextQuery jpaQuery =
				fullTextEntityManager.createFullTextQuery(luceneQuery);

//		jpaQuery.setProjection(ProjectionConstants.ID);
//		jpaQuery.setProjection(ProjectionConstants.ID, ProjectionConstants.OBJECT_CLASS);
//		jpaQuery.setProjection(ProjectionConstants.ID, ProjectionConstants.SCORE, "filename");
		jpaQuery.setMaxResults(1000000);
		jpaQuery.setFirstResult(0);
		
		// Lazy fetching isn't so easy here with combined results, so the goal is to get the order
		// first and then get all of the results maintaining that order but without getting full
		// texts which is slow and also overflows the heap
		
		// Could potentially try to get ProjectionConstants.ID and ProjectionConstants.SCORE
		// for two separate searches, join and sort by score,
		// then get the metadata and filenames.  This would maintain the order.
		

		if(Globals.TESTING) {System.out.println("Query using limit " + limit);}
		
		// Returns a list containing both EISDoc and DocumentText objects.
		List<Object> results = jpaQuery.getResultList();
		
//		Class<?> clazz = results.get(0).getClass();
//		System.out.println(clazz);
//		System.out.println(clazz.getClass());
//		for(Field field : clazz.getDeclaredFields()) {
//			System.out.println(field.getName());
//		}
//		if(Globals.TESTING) {
//			System.out.println(results.get(0).getId().toString());
//			System.out.println(results.get(0).getFilename());
//		}
		
		// init final result list
		List<MetadataWithContext2> combinedResults = new ArrayList<MetadataWithContext2>();
		
		// Condense results:
		// If we have companion results (same EISDoc.ID), combine

		// Quickly build a HashMap of EISDoc (AKA metadata) IDs; these are unique
		// (we'll use these to condense the results on pass 2)
		HashMap<Long, Integer> metaIds = new HashMap<Long, Integer>(results.size());
		int position = 0;
		for (Object result : results) {
			if(result.getClass().equals(EISDoc.class)) {
				metaIds.put(((EISDoc) result).getId(), position);
			}
			position++;
		}
		
		HashMap<Long, Boolean> skipThese = new HashMap<Long, Boolean>();
		HashMap<Long, Integer> added = new HashMap<Long, Integer>();
		
		// Handle DocumentText results
		position = 0;
		for (Object result : results) {
			if(result.getClass().equals(DocumentText.class) && justRecordIds.contains(((DocumentText) result).getEisdoc().getId())) {
				
				try {
					long key = ((DocumentText) result).getEisdoc().getId();

					// Get filename
					MetadataWithContext2 combinedResult = new MetadataWithContext2(
							((DocumentText) result).getEisdoc(),
							new ArrayList<String>(),
							((DocumentText) result).getFilename(),0);

					// 1. If we have already have a title result set skip flag
					if(metaIds.containsKey(key) && (metaIds.get(key) > position)) { // If this Text result comes before the Meta result
							// Flag to skip over the Meta result later
							skipThese.put(key, true);
					} 
					// 2. If this is the first non-title (text content) result, add new.
					if(!added.containsKey(key)) {
						combinedResults.add( combinedResult );
						added.put( key, position );
					} else {
						// 3. If we already have this result with no filename, add new filename.
						if(combinedResults.get(added.get(key)).getFilenames().isBlank()) {
							// "update" that instead of adding this result
							combinedResults.set(added.get(key), combinedResult);
						} else {
							// 4. If we have this WITH filename, concat.
							if(Globals.TESTING) {
								System.out.println("Adding filename to existing record: " + combinedResult.getFilenames());
							}
							// Add this combinedResult's filename to filename list
							String currentFilename = combinedResults.get(added.get(key)).getFilenames();
							// > is not a valid directory/filename char, so should work as delimiter
							combinedResults.get(added.get(key))
								.setFilenames(
									currentFilename.concat(">" + combinedResult.getFilenames())
								);
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if(result.getClass().equals(EISDoc.class)) {
				// Add metadata result unless it's flagged for skipping
				if(!skipThese.containsKey(((EISDoc) result).getId())) {
					combinedResults.add(new MetadataWithContext2(((EISDoc) result),new ArrayList<String>(),"",0));
				}
			}
			position++;
		}
		
		if(Globals.TESTING) {
			System.out.println("Results #: " + results.size());
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Time elapsed: " + elapsedTime);
		}

		return combinedResults;
	}
	
	@Deprecated
	public ArrayList<ArrayList<String>> getHighlights(UnhighlightedDTO unhighlighted) throws ParseException, IOException {
		long startTime = System.currentTimeMillis();
		// Normalize whitespace and support added term modifiers
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(mutateTermModifiers(unhighlighted.getTerms()).strip());
		
		// build highlighter with StandardAnalyzer
		QueryParser qp = new QueryParser("plaintext", new StandardAnalyzer());
		qp.setDefaultOperator(Operator.AND);
		Query luceneTextOnlyQuery = qp.parse(formattedTerms);
		QueryScorer scorer = new QueryScorer(luceneTextOnlyQuery);

//		IndexReader reader = DirectoryReader.open(FSDirectory.open(Globals.getIndexPath()));
//		IndexSearcher searcher = new IndexSearcher(reader);
		StandardAnalyzer stndrdAnalyzer = new StandardAnalyzer();
		Highlighter highlighter = new Highlighter(globalFormatter, scorer);
//		UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher, stndrdAnalyzer);
		
		Fragmenter fragmenter = new SimpleFragmenter(bigFragmentSize);
//		highlighter.setFormatter(formatter);
//		highlighter.setScorer(scorer);
//		highlighter.setMaxLength(Integer.MAX_VALUE);
		highlighter.setTextFragmenter(fragmenter);
		highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
		
		
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
		
		for(Unhighlighted input : unhighlighted.getUnhighlighted()) {
			ArrayList<String> result = new ArrayList<String>();

			// Run query to get each text via eisdoc ID and filename?
			// Need to split filenames by >
			String[] filenames = input.getFilename().split(">");
			List<String> texts = new ArrayList<String>();
			for(String filename : filenames) {
				ArrayList<String> inputList = new ArrayList<String>();
				inputList.add(input.getId().toString());
				inputList.add(filename);
				List<String> records = jdbcTemplate.query
				(
					"SELECT plaintext FROM test.document_text WHERE document_id = (?) AND filename=(?)", 
					inputList.toArray(new Object[] {}),
					(rs, rowNum) -> new String(
						rs.getString("plaintext")
					)
				);
				if(records.size()>0) {
					String text = records.get(0);
					texts.add(text);

					if(Globals.TESTING){
						System.out.println("ID: " + input.getId().toString() + "; Filename: " + filename);
					}
				}
			}
			
			
//				Optional<EISDoc> doc = DocRepository.findById(input.getId());
//				String text = TextRepository.findByEisdocAndFilenameIn(doc.get(), filename).getText();
			for(String text : texts) {
				TokenStream tokenStream = stndrdAnalyzer.tokenStream("plaintext", new StringReader(text));

//		        TopDocs topDocs = searcher.search(qp.parse(formattedTerms), Integer.MAX_VALUE);
//		        ScoreDoc scoreDocs[] = topDocs.scoreDocs;
		 
//		        for (ScoreDoc scoreDoc : scoreDocs) {
//		            Document document = searcher.getDocument(scoreDoc.doc);
//		            String title = document.get("title");
//		            System.out.println(title);
//		        }
				try {
					// Add ellipses to denote that these are text fragments within the string
					String highlight = highlighter.getBestFragments(tokenStream, text, 1, " ...</span><span class=\"fragment\">... ");
//					String[] highlight = highlighter.highlight("plaintext", qp.parse(formattedTerms), topDocs, 1);
//					highlighter.highlightFields(fieldsIn, query, docidsIn, maxPassagesIn)

//					String highlight = highlighter.highlightWithoutSearcher("plaintext", qp.parse(formattedTerms), text, 1).toString();
					if(highlight.length() > 0) {
						result.add("<span class=\"fragment\">... " + org.apache.commons.lang3.StringUtils.normalizeSpace(highlight).strip().concat(" ...</span>"));
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidTokenOffsetsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						tokenStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			results.add(result);
		}
		stndrdAnalyzer.close();
		

		if(Globals.TESTING) {
			System.out.println("Results #: " + results.size());
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Time elapsed: " + elapsedTime);
		}
		
		return results;
	}
	
	
	
	
    public ScoreDoc[] searchIndex(String searchQuery) throws Exception {
    	System.out.println("Search terms: " + searchQuery);

        Analyzer analyzer = new StandardAnalyzer();
        QueryParser queryParser = new QueryParser("plaintext", analyzer);
        Query query = queryParser.parse(searchQuery);
 
        TopDocs topDocs = indexSearcher.search(query, Integer.MAX_VALUE);
        ScoreDoc scoreDocs[] = topDocs.scoreDocs;
        
        return scoreDocs;
    }
    
    // This route could be the future of highlight searches at the very least.  Just needs
    // Lucene document IDs for the FVH.
    public List<HighlightedResult> searchAndHighlight(String searchQuery) throws Exception {
    	try {
    		String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(
	    			mutateTermModifiers(searchQuery.strip()));
		
    	if(Globals.TESTING) {System.out.println("Formatted: " + formattedTerms);}
   	 
        // 1. Search; instantiate highlighter
    	
//    	indexSearcher = new MultiSearcher();

        Analyzer analyzer = new StandardAnalyzer();
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
					new String[] {"title", "plaintext"},
					analyzer);
		mfqp.setDefaultOperator(Operator.AND);
        Query query = mfqp.parse(formattedTerms);
        System.out.println("Query "+query.toString());
        
    	// This appears to only get results that match on plaintext, 
    	// or on both plaintext AND title?
    	// So if we want both, we might need to do two separate searches and 
    	// then merge them ordered by score?
//        TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
        TopDocs topDocs = indexSearcher.search(query, 1000000);
        ScoreDoc scoreDocs[] = topDocs.scoreDocs;

//        QueryParser qp = new QueryParser("title",analyzer);
//        QueryParser qp2 = new QueryParser("plaintext",analyzer);

//        TopDocs topDocs2 = searcher.search(qp.parse(formattedTerms), 1000000);
//        TopDocs topDocs3 = searcher.search(qp2.parse(formattedTerms), 1000000);

		SearchSession session = org.hibernate.search.mapper.orm.Search.session(em);
		// just one Long ID and one null, index 0 of each list is text id if found, 1 is meta id
		SearchResult<List<?>> hits = session.search( Arrays.asList(DocumentText.class, EISDoc.class) )
				.select( f -> f.composite(
						f.field("text_id"),
						f.field("document_id")
				) )
				.where( f -> f.match().fields("title","plaintext")
						.matching(formattedTerms))
				.loading( o -> o.cacheLookupStrategy(
						EntityLoadingCacheLookupStrategy.PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE
				) ) // default: skip, but if we expect most hits to be cached this is useful
				.fetchAll();

		// returns entire classes from database
//		SearchResult<Object> hits2 = session.search( Arrays.asList(DocumentText.class, EISDoc.class) )
//				.where( f -> f.match().fields("title","plaintext")
//						.matching(formattedTerms))
//				.fetchAll();

		// reference only has low-level concepts like type name and document identifier
//		SearchResult<EntityReference> hits3 = session.search( Arrays.asList(DocumentText.class, EISDoc.class) )
//				.select( f -> f.entityReference() )
//				.where( f -> f.match().fields("title","plaintext")
//						.matching(formattedTerms))
//				.fetchAll();
//		System.out.println(hits.total().hitCount() + " hits to string " + hits.hits().toString().substring(0,1000));
//        System.out.println("topDocs totalHits "+topDocs.totalHits.value);
//        System.out.println("topDocs2 totalHits "+topDocs2.totalHits.value);
//        System.out.println("topDocs3 totalHits "+topDocs3.totalHits.value);
//		System.out.println(hits2.total().hitCount() + " hits2 to string " + hits2.hits().toString());
//		System.out.println(hits3.total().hitCount() + " hits3 to string " + hits3.hits().toString());
		
//		System.out.println(hits3.hits().get(0).name());
//		System.out.println(hits3.hits().get(0).id());
        
        FastVectorHighlighter fvh = new FastVectorHighlighter();
 
        // 2. Instantiate indexReader on index on disk
        File indexFile = new File(Globals.getIndexString());
        Directory directory = FSDirectory.open(indexFile.toPath());
        IndexReader indexReader = DirectoryReader.open(directory);
 
        // 3. Build multi-object results
        List<HighlightedResult> resultList = new ArrayList<HighlightedResult>(scoreDocs.length);
        int textCount = 0;
        int metaCount = 0;
        for (ScoreDoc scoreDoc : scoreDocs) {
        	Document document = indexSearcher.doc(scoreDoc.doc);
        	
        	// Print all fields and values (except plaintext) for testing
//            for(IndexableField field : document.getFields()) {
//            	if(!field.name().contentEquals("plaintext")) { // Don't print entire text
//                	System.out.print("Field " + field.name() + "; " + field.stringValue());
//            	}
//            }
            
            if(document.get("text_id") == null) {
            	metaCount++;
            	// Meta result only, no highlight
            } else {
            	textCount++;
                HighlightedResult result = new HighlightedResult();
    			// Note: 1. This needs projectable plaintext; we don't need to query
    			// the database for plaintext, although we do need filenames and eisdocs,
                // and also we haven't searched on title here
    			String fragment = fvh.getBestFragment(
    					fvh.getFieldQuery(query), 
    					indexReader, 
    					scoreDoc.doc, 
    					"plaintext", 
    					250);
    			
//    			if(fragment != null) {
//    				System.out.print(" Frag OK; length "+fragment.length());
//    			} else {
//    				System.out.print("MISS: Title match only?");
//    			}
    			
    			ArrayList<String> inputList = new ArrayList<String>();
    			inputList.add(document.get("text_id"));
//    			System.out.println(" ID " + document.get("text_id"));

    			// TODO: Get eisdoc AND filename here and then condense filenames by eisdoc before highlighting,
    			// and then the only thing is we aren't doing a title search at all...  but the title definitely
    			// will be represented in the body of these texts anyway - they just won't be scored the same.
    			List<String> records = jdbcTemplate.query
    			(
    				"SELECT filename FROM test.document_text WHERE id = (?)", 
    				inputList.toArray(new Object[] {}),
    				(rs, rowNum) -> new String(
    					rs.getString("filename")
    				)
    			);
    			if(records.size()>0) {
    				String filename = records.get(0);
    				result.addFilename(filename);
    				result.addHighlight(fragment);
    				resultList.add(result); // TODO: Missing EISDoc entirely.
    			}
            }
            
        }
        
        indexReader.close();
        System.out.println("Highlight count: " + resultList.size());
        System.out.println(metaCount);
        System.out.println(textCount);
        return resultList;
    	} catch(Exception e) {
    		e.printStackTrace();
    		return null;
    	}
	    
    }
    
    // uses term vectors via token stream.  Relatively slow.
    @Deprecated
    public List<HighlightedResult> searchAndHighlightSlow(String searchQuery) throws Exception {
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(
	    			mutateTermModifiers(searchQuery.strip()));
		
    	if(Globals.TESTING) {System.out.println("Formatted terms: " + formattedTerms);}
   	 
        // 1. Search; instantiate highlighter

		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
					new String[] {"title", "plaintext"},
					new StandardAnalyzer());
		mfqp.setDefaultOperator(Operator.AND);
        Query query = mfqp.parse(formattedTerms);
        
        TopDocs topDocs = indexSearcher.search(query, Integer.MAX_VALUE);
        ScoreDoc scoreDocs[] = topDocs.scoreDocs;
        
        QueryScorer queryScorer = new QueryScorer(query, "plaintext");
        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
//        Fragmenter fragmenter = new SimpleFragmenter(250);
        Highlighter highlighter = new Highlighter(queryScorer); // Set the best scorer fragments
        highlighter.setTextFragmenter(fragmenter); // Set fragment to highlight
        highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
 
        // 2. Instantiate indexReader on index on disk
        File indexFile = new File(Globals.getIndexString());
        Directory directory = FSDirectory.open(indexFile.toPath());
        IndexReader indexReader = DirectoryReader.open(directory);
 
        // 3. Build multi-object results
        List<HighlightedResult> resultList = new ArrayList<HighlightedResult>(scoreDocs.length);
        for (ScoreDoc scoreDoc : scoreDocs) {
        	Document document = indexSearcher.doc(scoreDoc.doc);
//        	if(document.get("_hibernate_class").contentEquals("nepaBackend.model.DocumentText") ) {
//
//        	} else {
//        		
//        	}
            HighlightedResult result = new HighlightedResult();
			
			ArrayList<String> inputList = new ArrayList<String>();
			inputList.add(document.get("text_id"));
			List<DocumentTextStrings> records = jdbcTemplate.query
			(
				"SELECT plaintext,filename FROM test.document_text WHERE id = (?)", 
				inputList.toArray(new Object[] {}),
				(rs, rowNum) -> new DocumentTextStrings(
					rs.getString("plaintext"),
					rs.getString("filename")
				)
			);
			
			
			if(records.size()>0) {
	            Fields termVectors = indexReader.getTermVectors(scoreDoc.doc);

	            TokenStream tokenStream = TokenSources.getTermVectorTokenStreamOrNull(
	            		"plaintext", 
	            		termVectors, 
	            		-1);
	            
	            String fragment = highlighter.getBestFragments(tokenStream, 
	            		records.get(0).text, 
	            		1, 
	            		" ...</span><span class=\"fragment\">... ");
				
	            
				String filename = records.get(0).filename;
				result.addFilename(filename);
				result.addHighlight(fragment);
				resultList.add(result); // TODO: Missing EISDoc entirely.
	            
	            tokenStream.close();
			}
            
        }
//        HashMap<String, Integer> idList = new HashMap<String,Integer>(scoreDocs.length);
//        for(Entry<String, Integer> entry : idList.entrySet()){
////          System.out.println( entry.getKey() + " => " + ": " + entry.getValue() );
//          HighlightedResult result = new HighlightedResult();
//			
//			ArrayList<String> inputList = new ArrayList<String>();
//			inputList.add(entry.getKey());
//			List<DocumentTextStrings> records = jdbcTemplate.query
//			(
//				"SELECT plaintext,filename FROM test.document_text WHERE id = (?)", 
//				inputList.toArray(new Object[] {}),
//				(rs, rowNum) -> new DocumentTextStrings(
//					rs.getString("plaintext"),
//					rs.getString("filename")
//				)
//			);
//			
//			if(records.size()>0) {
//	            Fields termVectors = indexReader.getTermVectors(entry.getValue());
////	            if(termVectors == null) {
////	            	System.out.println("Term vectors is null");
////	            } else {
////	            	System.out.print(" OK ");
////	            }
//	            
//	            TokenStream tokenStream = TokenSources.getTermVectorTokenStreamOrNull(
//	            		"plaintext", 
//	            		termVectors, 
//	            		-1);
//	            
////	            String fragment = highlighter.getBestFragment(tokenStream, records.get(0).text);
//	            String fragment = highlighter.getBestFragments(tokenStream, 
//	            		records.get(0).text, 
//	            		1, 
//	            		" ...</span><span class=\"fragment\">... ");
////				
//	            
//	            // fast vector highlighter test. Gets null because it requires stored texts, 
//	            // but our text is in database.
////	            String frag = fvh.getBestFragment(
////	            		fvh.getFieldQuery(query), indexReader, scoreDoc.doc, "plaintext", 100);
////	            System.out.println("FVH fragment: " + frag);
//				String filename = records.get(0).filename;
//				result.addFilename(filename);
//				result.addHighlight(fragment);
//				resultList.add(result); // TODO: Missing EISDoc entirely.
//				
////	            System.out.println("Fragment length: " + fragment.length());
//	            
//	            tokenStream.close();
//			}
//          
//      }
        System.out.println("Highlight count: " + resultList.size());
        indexReader.close();
        return resultList;
    }
	
	/** Return all records matching terms in "plaintext" field (no highlights/context) 
	 * @throws ParseException */
	@Override
	public List<List<?>> searchHibernate6(String terms) throws ParseException {
		
		String newTerms = mutateTermModifiers(terms.strip());
		
		SearchSession session = org.hibernate.search.mapper.orm.Search.session(em);

		SearchResult<Long> results = session.search(DocumentText.class)
				.select( f -> f.field("text_id", Long.class))
				.where( f -> f.match().field("plaintext")
						.matching(newTerms))
				.fetchAll();
		System.out.println("Results count:"+results.total().hitCount());
		
		// 1. Get all relevant IDs and remember position (TODO: verify ordered by score)
		
		// hits.hits() is a list of 2-length lists containing null in one index and a Long in the other
		// depending on whether it matched on title (EISDoc) or plaintext (DocumentText)
		// AKA which class it matched.
		SearchResult<List<?>> hits = session.search( Arrays.asList(DocumentText.class, EISDoc.class) )
				.select( f -> f.composite(
						f.field("text_id", Long.class),
						f.field("document_id", Long.class)
				) )
				.where( f -> f.match().fields("title","plaintext")
						.matching(newTerms))
				.fetchAll();
		System.out.println("Hits count:"+hits.total().hitCount());
		System.out.println(hits.toString());
		System.out.println(hits.hits().toString());
		
		
		HashMap<Long,Integer> textIds = new HashMap<Long,Integer>();
		HashMap<Long,Integer> docIds = new HashMap<Long,Integer>();
		
		int index = 0;
		for(List<?> obj : hits.hits()) {
			if(obj.get(1) != null) {
				textIds.put((Long) obj.get(1), index);
			} else if(obj.get(0) != null) {
				docIds.put((Long) obj.get(0), index);
			}
			
			index = index + 1;
		}

		// 2. Build list of metadata and text IDs in order of whichever was found first, if either.
		
		// Except we need to get eisdocs from text records where title never matched.
		// This is reinventing my own wheel.
		// TODO: Reuse all the database and ordering logic I already wrote, except use the UnifiedHighlighter
		// and term vectors.  Finally, see how long each step takes.
		HashMap<Long,List<Long>> mapOfLists = new HashMap<Long,List<Long>>();
		
		for(int i = 0; i < Math.max(textIds.size(), docIds.size()); i++) {
			
		}
		
//		javax.persistence.Query query = em.createQuery("SELECT DISTINCT doc.eisdoc FROM DocumentText doc WHERE doc.id IN :ids");
//		query.setParameter("ids", textIds);
////	
//		List<EISDoc> docs = query.getResultList();
		
		return hits.hits();
	}

	/** Combination title/fulltext query including the metadata parameters like agency/state/...
				 * and this is currently the default search; returns metadata plus filename 
				 * using Lucene's internal default scoring algorithm
				 * @throws ParseException
				 * */
	@Override
	public List<MetadataWithContext3> CombinedSearchNoContextHibernate6(SearchInputs searchInputs, SearchType searchType) {
		try {
			

			// Testing and for reference: getting the actual index
//			SearchMapping mapping = org.hibernate.search.mapper.orm.Search.mapping(em.getEntityManagerFactory()); 
//			IndexManager indexManager = mapping.indexManager( "EISDoc" ); 
//			LuceneIndexManager luceneIndexManager = indexManager.unwrap( LuceneIndexManager.class ); 
//			Analyzer indexingAnalyzer = luceneIndexManager.indexingAnalyzer(); 
//			Analyzer searchAnalyzer = luceneIndexManager.searchAnalyzer(); 
//			
//			long size = luceneIndexManager.computeSizeInBytes(); 
//			luceneIndexManager.computeSizeInBytesAsync() 
//			        .thenAccept( sizeInBytes -> {
//			            System.out.println(sizeInBytes/1000000 + " MB");
//			        } );
//			
//			indexManager = mapping.indexManager( "DocumentText" ); 
//			luceneIndexManager = indexManager.unwrap( LuceneIndexManager.class ); 
//			
//			size = luceneIndexManager.computeSizeInBytes(); 
//			luceneIndexManager.computeSizeInBytesAsync() 
//			        .thenAccept( sizeInBytes -> {
//			            System.out.println(sizeInBytes/1000000 + " MB");
//			        } );
			
			long initTime = System.currentTimeMillis();

			long startTime = System.currentTimeMillis();
			List<EISDoc> records = getFilteredRecords(searchInputs);
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Filtered records time: " + elapsedTime + "ms");
			
			// Run Lucene query on title if we have one, join with JDBC results, return final results
			if(!searchInputs.title.isBlank()) {
				String title = searchInputs.title;

				// Collect IDs filtered by params
				HashSet<Long> justRecordIds = new HashSet<Long>();
				for(EISDoc record: records) {
					justRecordIds.add(record.getId());
				}

//				startTime = System.currentTimeMillis();
				List<MetadataWithContext3> results = getScoredWithLuceneId(title);
//				stopTime = System.currentTimeMillis();
//				elapsedTime = stopTime - startTime;
//				System.out.println("Score time: " + elapsedTime + "ms");
				
				// Build new result list in the same order but excluding records that don't appear in the first result set (records).
				List<MetadataWithContext3> finalResults = new ArrayList<MetadataWithContext3>();
				for(int i = 0; i < results.size(); i++) {
					if(justRecordIds.contains(results.get(i).getDoc().getId())) {
						finalResults.add(results.get(i));
					}
				}
				
				if(Globals.TESTING) {
					System.out.println("Meta record count after filtering: " + records.size());
					System.out.println("Records 2 (final result set of combined metadata and filenames) " + results.size());
					stopTime = System.currentTimeMillis();
					elapsedTime = stopTime - initTime;
					System.out.println("Total (Filtering, lucene search, results combining and ordering time): " + elapsedTime + "ms");
				}
				return results;
			} else { // no title: simply return JDBC results...  however they have to be translated
				List<MetadataWithContext3> finalResults = new ArrayList<MetadataWithContext3>();
				for(EISDoc record : records) {
					finalResults.add(new MetadataWithContext3(new ArrayList<Integer>(), record, new ArrayList<String>(), "", 0));
				}
//				stopTime = System.currentTimeMillis();
//				elapsedTime = stopTime - initTime;
//				System.out.println("Blank search + translation time: " + elapsedTime + "ms");
				return finalResults;
			}
		} catch(Exception e) {
			e.printStackTrace();
			String problem = e.getLocalizedMessage();
			MetadataWithContext3 result = new MetadataWithContext3(null, null, null, problem, 0);
			List<MetadataWithContext3> results = new ArrayList<MetadataWithContext3>();
			results.add(result);
			return results;
		}
	}
		
	// The only way to modify the fragment length with the unified highlighter is to do something with
	// java.text.BreakIterator, apparently.
	public ArrayList<ArrayList<String>> getUnifiedHighlights(UnhighlightedDTO unhighlighted) throws ParseException, IOException {
		long startTime = System.currentTimeMillis();
		long cumulativeTime = 0;
		long cumulativeTime2 = 0;
		
		// Normalize whitespace and support added term modifiers
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(mutateTermModifiers(unhighlighted.getTerms()).strip());
		
		// build highlighter with StandardAnalyzer
		QueryParser qp = new QueryParser("plaintext", new StandardAnalyzer());
		qp.setDefaultOperator(Operator.AND);
		Query luceneTextOnlyQuery = qp.parse(formattedTerms);
//			PassageScorer scorer = new PassageScorer();

//			IndexReader reader = DirectoryReader.open(FSDirectory.open(Globals.getIndexPath()));
		StandardAnalyzer stndrdAnalyzer = new StandardAnalyzer();
//			stndrdAnalyzer.getStopwordSet();
//			IndexSearcher searcher = new IndexSearcher(reader);
//			UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher, stndrdAnalyzer);
		// if we're not using a searcher (getting text ourselves):
		UnifiedHighlighter highlighter = new UnifiedHighlighter(null, stndrdAnalyzer);
		
//			highlighter.setFormatter(formatter);
//			highlighter.setScorer(scorer);
//			List<String> whew = jdbcTemplate.query
//			(
//				"SELECT MAX(LENGTH(plaintext)) FROM test.document_text", 
//				(rs, rowNum) -> new String(
//					rs.getString("MAX(LENGTH(plaintext))")
//				)
//			);
//			highlighter.setMaxLength(Integer.parseInt(whew.get(0)));
//			System.out.println("MaxLength " + highlighter.getMaxLength());
		
		DefaultPassageFormatter dfp = new DefaultPassageFormatter();
//			dfp.
		
//			highlighter.setFormatter(new DefaultPassageFormatter());
		
		
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
		
		for(Unhighlighted input : unhighlighted.getUnhighlighted()) {
			long queryStartTime = System.currentTimeMillis();
			ArrayList<String> result = new ArrayList<String>();

			// Run query to get each text via eisdoc ID and filename?
			// Need to split filenames by >
			String[] filenames = input.getFilename().split(">");
			List<String> texts = new ArrayList<String>();
			for(String filename : filenames) {
				ArrayList<String> inputList = new ArrayList<String>();
				inputList.add(input.getId().toString());
				inputList.add(filename);
				List<String> records = jdbcTemplate.query
				(
					"SELECT plaintext FROM test.document_text WHERE document_id = (?) AND filename=(?)", 
					inputList.toArray(new Object[] {}),
					(rs, rowNum) -> new String(
						rs.getString("plaintext")
					)
				);
				if(records.size()>0) {
					String text = records.get(0);
					texts.add(text);

					if(Globals.TESTING){
						System.out.println("ID: " + input.getId().toString() + "; Filename: " + filename);
					}
				}
			}
			long queryStopTime = System.currentTimeMillis();

			long elapsedTime = queryStopTime - queryStartTime;
			cumulativeTime += elapsedTime;
			
			
//				Optional<EISDoc> doc = DocRepository.findById(input.getId());
//				String text = TextRepository.findByEisdocAndFilenameIn(doc.get(), filename).getText();
			for(String text : texts) {
//					TokenStream tokenStream = stndrdAnalyzer.tokenStream("plaintext", new StringReader(text));

//			        TopDocs topDocs = searcher.search(luceneTextOnlyQuery, Integer.MAX_VALUE);
//			        ScoreDoc scoreDocs[] = topDocs.scoreDocs;
//			 
//			        for (ScoreDoc scoreDoc : scoreDocs) {
//			            Document document = searcher.doc(scoreDoc.doc);
//			            System.out.println("text ID " + document.get("text_id"));
//			        }
				try {
					// For this to work, we would need to store projectable plaintexts using Lucene.
					// Then we would not get them from MySQL.
					// Also, this would happen all at once and we would have to worry about
					// combining filenames with metadata and such after the fact.
//						String[] highlight = highlighter.highlight("plaintext", luceneTextOnlyQuery, topDocs, 1);
//						if(highlight[0] != null) {System.out.println("Highlight: " + highlight[0]);}
					// Also, we'd perhaps get the doc ID list ourselves.  Finally, either way,
					// we'd want the highlights back out in order.  So again, logic changes.
//						highlightFields(String[] fieldsIn, Query query, int[] docidsIn, int[] maxPassagesIn)
//						Highlights the top-N passages from multiple fields, for the provided int[] docids.


//			            Fields termVectors = reader.getTermVectors(scoreDoc.doc);

//			            TokenStream tokenStream = TokenSources.getTermVectorTokenStreamOrNull(
//			            		"plaintext", 
//			            		termVectors, 
//			            		-1);
					
					
					long highlightStart = System.currentTimeMillis();
					String highlight = highlighter.highlightWithoutSearcher(
							"plaintext", 
							luceneTextOnlyQuery, 
							text, 
							1)
							.toString();
					long highlightStop = System.currentTimeMillis();
					long elapsedTime2 = highlightStop - highlightStart;
					cumulativeTime2 += elapsedTime2;

					if(highlight.length() > 0) {
						result.add("<span class=\"fragment\">... " + org.apache.commons.lang3.StringUtils.normalizeSpace(highlight).strip().concat(" ...</span>"));
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
//						try {
//							tokenStream.close();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
				}
			}
			results.add(result);
		}
		stndrdAnalyzer.close();
		

		if(Globals.TESTING) {
			System.out.println("Results #: " + results.size());
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Time elapsed: " + elapsedTime);
			System.out.println("Query/text retrieval time: " + cumulativeTime);
			System.out.println("Highlight time: " + cumulativeTime2);
		}
		
		return results;
	}

	/** Search both fields at once with MultiSearcher and return in combined scored order with lucene IDs */
	private List<MetadataWithContext3> getScoredWithLuceneId(String terms) throws Exception {
		long startTime = System.currentTimeMillis();
		String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(
    			mutateTermModifiers(terms.strip()));
	
		if(Globals.TESTING) {System.out.println("Formatted terms: " + formattedTerms);}
		 
	    // 1. Search; instantiate highlighter

//		indexSearcher = new MultiSearcher();
		
//		StandardAnalyzer analyzer = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
	
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
					new String[] {"title", "plaintext"},
					analyzer);
		mfqp.setDefaultOperator(Operator.AND);
	    Query query = mfqp.parse(formattedTerms);

//		long searchStart = System.currentTimeMillis();

		ScoreDoc scoreDocs[] = indexSearcher.search(query, Integer.MAX_VALUE).scoreDocs;
//	    analyzer.close();
//		long searchEnd = System.currentTimeMillis();
//		
//		System.out.println("Search time " + (searchEnd - searchStart));
//		System.out.println("Search count " + scoreDocs.length);

		List<ScoredResult> converted = new ArrayList<ScoredResult>(scoreDocs.length);
		Set<Long> metaIds = new HashSet<Long>();
		Set<Long> textIds = new HashSet<Long>();
		int i = 0;

//		List<Long> times = new ArrayList<Long>(scoreDocs.length);
		
//		long convertStart = System.currentTimeMillis();
    	// To try to ensure no plaintext overhead, specifically just load the IDs.
		// Seems to cut the getDocument process time in half.
		HashSet<String> fieldsToLoad = new HashSet<String>();
		fieldsToLoad.add("text_id");
		fieldsToLoad.add("document_id");
        for (ScoreDoc scoreDoc : scoreDocs) {
//    		long docStart = System.currentTimeMillis();
        	Document document = indexSearcher.getIndexReader().document(scoreDoc.doc, fieldsToLoad);
//    		long docEnd = System.currentTimeMillis();
//    		times.add(docEnd - docStart);
			ScoredResult convert = new ScoredResult();
			convert.score = scoreDoc.score;
			convert.idx = i;
			convert.luceneId = scoreDoc.doc;
//			System.out.print(" Lucene ID " + convert.luceneId);
			String textId = document.get("text_id");
			String metaId = document.get("document_id");
        	if(textId == null) {
        		// Handle meta ID
        		Long id = Long.parseLong(metaId);
//        		System.out.print(" meta ID " + id);
        		metaIds.add(id);
    			convert.id = id;
    			convert.entityName = EntityType.META;
        	} else {
        		// Handle text ID
        		Long id = Long.parseLong(textId);
//        		System.out.print(" text ID " + id);
        		textIds.add(id);
    			convert.id = id;
    			convert.entityName = EntityType.TEXT;
        	}
			converted.add(convert);
			i++;
        }
        
//		long convertEnd = System.currentTimeMillis();
		// This is time consuming, but not sure how we could reduce it.
		// We need the IDs as well as the lucene IDs.  The search itself is very fast, but only gets
		// lucene IDs...  then gathering the IDs using those takes a relatively long time for thousands
		// of hits.
		// Basically we can't do this faster because we need to carry around the lucene IDs because
		// that's needed for term vector highlighting.  So the search is a little slower so that the
		// highlighting is much faster.  Unfortunately don't see an alternative to getting .doc for everything
//		System.out.println("Convert time: " + (convertEnd - convertStart) + "ms");
//		double average = 0.0d;
//		long total = 0l;
//		for(int j = 0; j < times.size(); j++) {
//			long l = times.get(j);
//			double tmp = l / (double) times.size();
//			average += tmp;
//			total += l;
//		}
//		System.out.println("Average document time: " + (average) + "ms");
//		System.out.println("Total document time: " + (total) + "ms");
		
		
//		System.out.println("Title matches "+metaIds.size());	
//		System.out.println("Text matches "+textIds.size());
		
		// 1: Get EISDocs by IDs.
		
		List<EISDoc> docs = em.createQuery("SELECT d FROM EISDoc d WHERE d.id IN :ids")
			.setParameter("ids", metaIds).getResultList();
	
		if(Globals.TESTING){System.out.println("Docs results size: " + docs.size());}
		
		HashMap<Long, EISDoc> hashDocs = new HashMap<Long, EISDoc>();
		for(EISDoc doc : docs) {
			hashDocs.put(doc.getId(), doc);
		}
	
		// 2: Get DocumentTexts by IDs WITHOUT getting the entire texts.
	
		List<Object[]> textIdMetaAndFilenames = em.createQuery("SELECT d.id, d.eisdoc, d.filename FROM DocumentText d WHERE d.id IN :ids")
				.setParameter("ids", textIds).getResultList();
	
		if(Globals.TESTING){System.out.println("Texts results size: " + textIdMetaAndFilenames.size());}
		
		HashMap<Long, ReducedText> hashTexts = new HashMap<Long, ReducedText>();
		for(Object[] obj : textIdMetaAndFilenames) {
			hashTexts.put(
					(Long) obj[0], 
					new ReducedText(
						(Long) obj[0],
						(EISDoc) obj[1],
						(String) obj[2]
					));
		}
		
		List <MetadataWithContext3> combinedResults = new ArrayList<MetadataWithContext3>();
	
		// 3: Join (combine) results from the two tables
		// 3.1: Condense (add filenames to existing records rather than adding new records)
		// 3.2: keep original order
		
		HashMap<Long, Integer> added = new HashMap<Long, Integer>();
		int position = 0;
		
		for(ScoredResult ordered : converted) {
			if(ordered.entityName.equals(EntityType.META)) {
				if(!added.containsKey(ordered.id)) {
					// Add EISDoc into logical position
					MetadataWithContext3 combinedResult = new MetadataWithContext3(
							new ArrayList<Integer>(),
							hashDocs.get(ordered.id),
							new ArrayList<String>(),
							"",
							ordered.score);
					
					combinedResults.add(combinedResult);
					
					added.put(ordered.id, position);
					position++;
				}
				// If we already have one, do nothing - (title result: no filenames to add.)
			} else {
				EISDoc eisFromDoc = hashTexts.get(ordered.id).eisdoc;
				if(!added.containsKey(eisFromDoc.getId())) {
					// Add DocumentText into logical position plus lucene ID, filename
					MetadataWithContext3 combinedResult = new MetadataWithContext3(
							new ArrayList<Integer>(),
							eisFromDoc,
							new ArrayList<String>(),
							hashTexts.get(ordered.id).filename,
							ordered.score);
					combinedResult.addId(ordered.luceneId);
					
					combinedResults.add(combinedResult);
					added.put(eisFromDoc.getId(), position);
					position++;
				} else {
					// Add this combinedResult's filename to filename list
					// Add the lucene ID to this combinedResult's ID list
					String currentFilename = combinedResults.get(added.get(eisFromDoc.getId()))
							.getFilenames();
					// > is not a valid directory/filename char, so should work as delimiter
					// If currentFilename is blank (title match came first), no need to concat.  Just set.
					if(currentFilename.isBlank()) {
						combinedResults.get(added.get(eisFromDoc.getId()))
						.setFilenames(
							hashTexts.get(ordered.id).filename
						);
						combinedResults.get(added.get(eisFromDoc.getId()))
						.addId(ordered.luceneId);
					} else {
						combinedResults.get(added.get(eisFromDoc.getId()))
						.setFilenames(
							currentFilename.concat(">" + hashTexts.get(ordered.id).filename)
						);
						combinedResults.get(added.get(eisFromDoc.getId()))
						.addId(ordered.luceneId);
					}
				}
			}
		}
		
		if(Globals.TESTING) {
			System.out.println("Results # (individual title and text record hits): " + converted.size());
			System.out.println("Results # Combined by metadata: " + combinedResults.size());
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Total score time: " + elapsedTime + "ms");
		}
		
		return combinedResults;
	}
	
	/** Fastest highlighting available, requires full term vectors indexed */
	@Override
	public ArrayList<ArrayList<String>> getHighlightsFVH(Unhighlighted2DTO unhighlighted) throws Exception {
		long startTime = System.currentTimeMillis();
		int fragmentSizeCustom = setFragmentSize(unhighlighted.getFragmentSizeValue());
		
		// Normalize whitespace and support added term modifiers
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(mutateTermModifiers(unhighlighted.getTerms()).strip());
		
		// build highlighter with StandardAnalyzer
//		StandardAnalyzer analyzer = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);

		QueryParser qp = new QueryParser("plaintext", analyzer);
		qp.setDefaultOperator(Operator.AND);
		Query luceneTextOnlyQuery = qp.parse(formattedTerms);

//        File indexFile = new File(Globals.getIndexString());
//        Directory directory = FSDirectory.open(indexFile.toPath());
//        IndexReader indexReader = DirectoryReader.open(directory);

        FastVectorHighlighter fvh = new FastVectorHighlighter();
		
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();

		// To try to slightly optimize if/when using UnifiedHighlighter
		HashSet<String> fieldsToLoad = new HashSet<String>();
		fieldsToLoad.add("plaintext");

		UnifiedHighlighter highlighter = new UnifiedHighlighter(null, analyzer);
		
		for(Unhighlighted2 input : unhighlighted.getUnhighlighted()) {
			ArrayList<String> result = new ArrayList<String>();

			// Run query to get each text via eisdoc ID and filename?
			// Need to split filenames by >
//			String[] filenames = input.getFilename().split(">");
			for(int i = 0; i < input.getLuceneIds().size(); i++) {
				int luceneId = input.getId(i).intValue();
//	        	Document document = searcher.getDocument(luceneId);
//				if(document != null) {
					// We can just get the highlight here, immediately.
	    			String fragment = fvh.getBestFragment(
	    					fvh.getFieldQuery(luceneTextOnlyQuery), 
	    					textReader, 
	    					luceneId, 
	    					"plaintext", 
	    					fragmentSizeCustom);

					// So apparently proximity search can return null fragments.
					// I think this may only be when the fragment size is too small.
					// For example, if two words are 100 words away from each other and the fragment
					// is only 250 characters...  that's going to be out of range.
					// I think the old highlighter used to put ellipses in between the individual terms
					// to avoid this.
					// Try UnifiedHighlighter as a backup?  It could be slower.  Also, it doesn't actually
	    			// necessarily show both terms.
					if(fragment != null) { 
						result.add(
//								"<span class=\"fragment\">... " + 
								org.apache.commons.lang3.StringUtils.normalizeSpace(fragment)
								.strip()
//								.concat(" ...</span>")
						);
//					} else if(false) {
//						result.add("<span class=\"fragment\">"
//							.concat("Sorry, this fragment was too large to return (term distance exceeded current maximum fragment value).")
//							.concat("</span>"));
					} else {
			        	Document document = indexSearcher.doc(luceneId,fieldsToLoad);
						String highlight = highlighter.highlightWithoutSearcher(
								"plaintext", 
								luceneTextOnlyQuery, 
								document.get("plaintext"), 
								1)
								.toString();
						
						if(highlight.length() > fragmentSizeCustom) { //c'mon
							int firstHitAt = highlight.indexOf("<b>");
							highlight = highlight.substring(Math.max(firstHitAt - (fragmentSizeCustom / 2), 0), Math.min(firstHitAt + (fragmentSizeCustom / 2), highlight.length()));
						}
						result.add(
//								"<span class=\"fragment\">... " + 
								org.apache.commons.lang3.StringUtils.normalizeSpace(highlight)
								.strip()
//								.concat(" ...</span>")
						);
					}
//				}
			}

			results.add(result);
		}
		
//		analyzer.close();
		

		if(Globals.TESTING) {
			System.out.println("Highlights #: " + results.size());
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Total highlight time: " + elapsedTime);
		}
		
		return results;
	}
	
	private int setFragmentSize(int fragmentSizeValue) {
		if(fragmentSizeValue == 0) {
			return 250;
		} else if(fragmentSizeValue == 1) {
			return 500;
		} else if(fragmentSizeValue == 2) {
			return 1000;
		} else {
			return 2000;
		}
	}

	/** Fastest highlighting available, requires full term vectors indexed, no markup */
	@Override
	public ArrayList<ArrayList<String>> getHighlightsFVHNoMarkup(Unhighlighted2DTO unhighlighted) throws Exception {
		long startTime = System.currentTimeMillis();
		// Normalize whitespace and support added term modifiers
	    String formattedTerms = org.apache.commons.lang3.StringUtils.normalizeSpace(mutateTermModifiers(unhighlighted.getTerms()).strip());
		
		// build highlighter with StandardAnalyzer
//		StandardAnalyzer analyzer = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);

		QueryParser qp = new QueryParser("plaintext", analyzer);
		qp.setDefaultOperator(Operator.AND);
		Query luceneTextOnlyQuery = qp.parse(formattedTerms);

//        File indexFile = new File(Globals.getIndexString());
//        Directory directory = FSDirectory.open(indexFile.toPath());
//        IndexReader indexReader = DirectoryReader.open(directory);

        FastVectorHighlighter fvh = new FastVectorHighlighter();
		
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();

		// To try to slightly optimize if/when using UnifiedHighlighter
		HashSet<String> fieldsToLoad = new HashSet<String>();
		fieldsToLoad.add("plaintext");

		UnifiedHighlighter highlighter = new UnifiedHighlighter(null, analyzer);

		int fragmentSizeCustom = setFragmentSize(unhighlighted.getFragmentSizeValue());
		
		for(Unhighlighted2 input : unhighlighted.getUnhighlighted()) {
			ArrayList<String> result = new ArrayList<String>();

			// Run query to get each text via eisdoc ID and filename?
			// Need to split filenames by >
//			String[] filenames = input.getFilename().split(">");
			for(int i = 0; i < input.getLuceneIds().size(); i++) {
				int luceneId = input.getId(i).intValue();
//	        	Document document = searcher.getDocument(luceneId);
//				if(document != null) {
					// We can just get the highlight here, immediately.
	    			String fragment = fvh.getBestFragment(
	    					fvh.getFieldQuery(luceneTextOnlyQuery), 
	    					textReader, 
	    					luceneId, 
	    					"plaintext", 
	    					fragmentSizeCustom);

					// So apparently proximity search can return null fragments.
					// I think this may only be when the fragment size is too small.
					// For example, if two words are 100 words away from each other and the fragment
					// is only 250 characters...  that's going to be out of range.
					// I think the old highlighter used to put ellipses in between the individual terms
					// to avoid this.
					// Try UnifiedHighlighter as a backup?  It could be slower.  Also, it doesn't actually
	    			// necessarily show both terms.
					if(fragment != null) { 
//						result.add("<span class=\"fragment\">... " 
//								+ org.apache.commons.lang3.StringUtils.normalizeSpace(fragment)
//								.strip()
//								.concat(" ...</span>")
//						);
						result.add(fragment);
//					} else if(false) {
//						result.add("<span class=\"fragment\">"
//							.concat("Sorry, this fragment was too large to return (term distance exceeded current maximum fragment value).")
//							.concat("</span>"));
					} else {
			        	Document document = indexSearcher.doc(luceneId,fieldsToLoad);
						result.add(highlighter.highlightWithoutSearcher(
								"plaintext", 
								luceneTextOnlyQuery, 
								document.get("plaintext"), 
								1)
								.toString()
						);
					}
//				}
			}

			results.add(result);
		}
		
//		analyzer.close();
		

		if(Globals.TESTING) {
			System.out.println("Highlights #: " + results.size());
			
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			System.out.println("Total highlight time: " + elapsedTime);
		}
		
		return results;
	}
    
}
