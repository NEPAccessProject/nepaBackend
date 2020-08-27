package nepaBackend;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import nepaBackend.controller.MetadataWithContext;
import nepaBackend.enums.SearchType;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.pojo.SearchInputs;

// TODO: Probably want a way to search for many/expanded highlights/context from one archive only
public class CustomizedTextRepositoryImpl implements CustomizedTextRepository {
	@PersistenceContext
	private EntityManager em;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private static int numberOfFragmentsMax = 5;
	private static int fragmentSize = 250;
	
//	private static int fuzzyLevel = 1;

	/** Return all records matching terms (no highlights/context) */
	@SuppressWarnings("unchecked")
	@Override
	public List<EISDoc> search(String terms, int limit, int offset) {
		
		terms = mutateTermModifiers(terms);
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em); // Create fulltext entity manager
			
		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
				.buildQueryBuilder().forEntity(DocumentText.class).get();
		
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
		
		Query luceneQuery = queryBuilder
				.simpleQueryString()
				.onField("plaintext")
				.withAndAsDefaultOperator()
				.matching(terms)
				.createQuery();

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
		// TODO: Can get filenames also, display those on frontend and no longer need DISTINCT (would require a new POJO, different structure than List<EISDoc>)
		javax.persistence.Query query = em.createQuery("SELECT DISTINCT doc.eisdoc FROM DocumentText doc WHERE doc.id IN :ids");
		query.setParameter("ids", new_ids);

		List<EISDoc> docs = query.getResultList();

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
	
	/** Return all highlights with context and document ID for matching terms (term phrase?) */
//	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public List<MetadataWithContext> metaContext(String terms, int limit, int offset, SearchType searchType) {
		long startTime = System.currentTimeMillis();
		
		terms = mutateTermModifiers(terms);
		
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

		QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
				.buildQueryBuilder().forEntity(DocumentText.class).get();
		Query luceneQuery = null;
		
		boolean fuzzy = false;
		if(fuzzy) {
//			luceneQuery = queryBuilder
//					.keyword()
//					.fuzzy()
//					.withEditDistanceUpTo(fuzzyLevel) // max: 2; default: 2; aka maximum fuzziness
//					.onField("plaintext")
//					.matching(terms)
//					.createQuery();
			
		} else {
			// for phrases
//			luceneQuery = queryBuilder
//					.phrase()
//						.withSlop(0) // default: 0 (note: doesn't work as expected)
//					.onField("plaintext")
//					.sentence(terms)
//					.createQuery();
			
			// all-word
			luceneQuery = queryBuilder
					.simpleQueryString()
					.onField("plaintext")
					.withAndAsDefaultOperator()
					.matching(terms)
					.createQuery();
			
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
		}
			
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
		

		
		
		SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class=\"highlight\">","</span>");

		// Logic for exact phrase vs. all-word query
		QueryScorer scorer = null;
		String[] words = terms.split(" ");
		if(searchType == SearchType.ALL) { // .equals uses == internally
			if(fuzzy) {
				// Fuzzy search code
//				FuzzyLikeThisQuery fuzzyQuery = new FuzzyLikeThisQuery(32, new StandardAnalyzer());
//				fuzzyQuery.addTerms(terms, "f", fuzzyLevel, 0);
//				scorer = new QueryScorer(fuzzyQuery);
			} else {
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
				scorer = new QueryScorer(luceneQuery);
			}
		} else {
			// Oldest code (most precision required)
			PhraseQuery query = new PhraseQuery("f", words);
			scorer = new QueryScorer(query);
		}

		Highlighter highlighter = new Highlighter(formatter, scorer);
		Fragmenter fragmenter = new SimpleFragmenter(fragmentSize);
		highlighter.setTextFragmenter(fragmenter);
		highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
		
		
		for (DocumentText doc: docList) {
			try {
				String highlight = getHighlightString(doc.getPlaintext(), highlighter);
				if(highlight.length() > 0) { // Length 0 shouldn't be possible since we are working on matching results already
					highlightList.add(new MetadataWithContext(doc.getEisdoc(), highlight, doc.getFilename()));
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
	
	// Given text and highlighter, return highlights (fragments) for text
	private static String getHighlightString (String text, Highlighter highlighter) throws IOException {
		
		
		StandardAnalyzer stndrdAnalyzer = new StandardAnalyzer();
		TokenStream tokenStream = stndrdAnalyzer.tokenStream("plaintext", new StringReader(text));
		String result = "";
		
		try {
			// Add ellipses to denote that these are text fragments within the string
			result = highlighter.getBestFragments(tokenStream, text, numberOfFragmentsMax, " ...</span><br /><span class=\"fragment\">... ");
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
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
		try {
			fullTextEntityManager.createIndexer().startAndWait();
			return true;
		} catch (InterruptedException e) {
			// TODO log interruption?
			e.printStackTrace();
			return false;
		}
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

    private String mutateTermModifiers(String terms){
    	if(terms != null && terms.strip().length() > 0) {
    		// + and - must immediately precede the next term (no space), therefore match the space also.
    		return terms.replace("OR", "|").replace("AND ", "+").replace("NOT ", "-");
    	} else {
    		return "";
    	}
    }

	
	/** TODO: Complete; test
	 * 1. Verify/trigger Lucene indexing on Title (Added @Indexed for EISDoc and @Field for title, need to make sure it's indexed)
	 * 2. Lucene-friendly Hibernate/JPA-wrapped query based on my custom, dynamically created query 
	 * 3. Ultimately, goal is to then create a combination title/fulltext query including the metadata parameters like agency/state/...
	 * and make that the default search
	 * */
	@Override
	public List<EISDoc> metadataSearch(SearchInputs searchInputs, int limit, int offset, SearchType searchType) {
		try {
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
			int queryLimit = 100000;
			if(Globals.saneInput(searchInputs.limit)) {
				if(searchInputs.limit <= 100000) {
					queryLimit = searchInputs.limit;
				}
			}
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
					rs.getString("state"), 
					rs.getString("filename"),
					rs.getString("comments_filename"),
					rs.getString("folder"),
					rs.getString("web_link"),
					rs.getString("notes")
				)
			);
			
			// debugging
			if(Globals.TESTING && searchInputs.endPublish != null) {
//				DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
//				DateValidator validator = new DateValidatorUsingLocalDate(dateFormatter);
//				System.out.println(validator.isValid(searchInputs.endPublish));
				System.out.println(sQuery); 
//				System.out.println(searchInputs.endPublish);
				System.out.println(searchInputs.title);
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
			if(searchInputs != null && searchInputs.title != null && !searchInputs.title.isBlank()) {
				String formattedTitle = org.apache.commons.lang3.StringUtils.normalizeSpace(searchInputs.title.strip());

				FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);

				QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
						.buildQueryBuilder().forEntity(EISDoc.class).get();
				

				Query luceneQuery = queryBuilder
						.simpleQueryString()
						.onField("title")
						.withAndAsDefaultOperator()
						.matching(formattedTitle)
						.createQuery();
	
				org.hibernate.search.jpa.FullTextQuery jpaQuery =
						fullTextEntityManager.createFullTextQuery(luceneQuery, EISDoc.class);
				
				jpaQuery.setMaxResults(limit);
				jpaQuery.setFirstResult(offset);
				
				List<EISDoc> results = jpaQuery.getResultList();
				
				List<Long> justRecordIds = new ArrayList<Long>();
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
					System.out.println("Records 1 " + records.size());
					System.out.println("Records 2 " + results.size());
				}
				
				// TODO: Final step is now to stop excluding special characters on the frontend.
				
				return finalResults;
			} else { // no title: simply return JDBC results
				return records;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	
	}

}
