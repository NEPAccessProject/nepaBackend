package nepaBackend;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import nepaBackend.controller.MetadataWithContext3;
import nepaBackend.enums.EntityType;
import nepaBackend.enums.SearchType;
import nepaBackend.model.EISDoc;
import nepaBackend.pojo.ReducedText;
import nepaBackend.pojo.ScoredResult;
import nepaBackend.pojo.SearchInputs;
import nepaBackend.pojo.Unhighlighted;
import nepaBackend.pojo.UnhighlightedDTO;

public class CustomizedTextRepositoryImpl implements CustomizedTextRepository {
	@PersistenceContext
	private EntityManager em;

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	@Autowired
	Analyzer analyzer;
	
	private static final Logger logger = LoggerFactory.getLogger(CustomizedTextRepositoryImpl.class);
	
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
	
	
	@Override
	public boolean sync() {
		SearchSession searchSession = org.hibernate.search.mapper.orm.Search.session(em);
		
		try {
			searchSession.massIndexer().startAndWait();
		} catch (InterruptedException e1) {
			logger.error("Failed to /sync index");
			return false;
		} 
		
		return true;
	}
	
	public String testTerms(String terms) {
		System.out.println("Terms: " + terms);
		
		if(terms == null || terms.isBlank()) { // parser exceptions on blank query
			return "";
		} else {
			
			String formattedTerms = mutateTermModifiers(terms);
		
			System.out.println("Formatted terms: " + formattedTerms);
			
			try {
				MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
						new String[] {"title", "plaintext"},
						analyzer);
				mfqp.parse(formattedTerms);
				return terms;
			} catch(ParseException pe) {
				pe.printStackTrace();
				return MultiFieldQueryParser.escape(formattedTerms);
			}
		}
	}
	

	/** Returns search terms after enforcing two rules:  Proximity matching was limited to 1 billion, just under absolute upper limit 
	 * (when going beyond the limit, proximity matching stopped working at all).  
	 * Support for | is added by converting to ||. */
    private String mutateTermModifiers(String terms){
//	    ArrayList<String> remaining = new ArrayList<String>();
//
//		try {
//
//		    TokenStream tokenStream = analyzer.tokenStream("title", new StringReader(terms));
//		    CharTermAttribute term = tokenStream.addAttribute(CharTermAttribute.class);
//		    tokenStream.reset();
//
//		    while(tokenStream.incrementToken()) {
//		        System.out.print("[" + term.toString() + "] ");
//		        remaining.add(term.toString());
//		    }
//
//			terms = String.join(" ", remaining);
//			
//		    tokenStream.close();
//		} catch (IOException e) {
//		    e.printStackTrace();
//		}
		
    	
		// + and - must immediately precede the next term (no space), therefore don't add a space after those when replacing
		return Globals.normalizeSpace(terms).replace(" | ",  " || ")
//    				.replace("and", "AND") // support for AND is implicit currently
//    				.replace("or", "OR") // Lowercase term modifiers could easily trip people up accidentally if they're pasting something in and they don't actually want to OR them
//    				.replace("not", "NOT")
//    				.replace("&", "AND")
				.replace("!", "-")
				.replace(":",""); // better to not support this term modifier so either delete or escape it
//    				.replace("%", "-")
//    				.replace("/", "~") // westlaw? options, can also add confusion
				// QueryParser doesn't support |, does support ?, OR, NOT
//    				.replaceAll("(~\\d{10}\\d*)", "~999999999"); // this was necessary with QueryBuilder (broke after limit)
    }

	
	/**1. Triggered, verified Lucene indexing on Title (Added @Indexed for EISDoc and @Field for title)
	 * 2. Lucene-friendly Hibernate/JPA-wrapped query based on custom, dynamically created query
	 * */
    /** Title-only search */
	@Override
	public List<EISDoc> metadataSearch(
			SearchInputs searchInputs, 
			int limit, 
			int offset, 
			SearchType searchType) 
			throws ParseException {
		try {

			searchInputs.title = mutateTermModifiers(searchInputs.title);
			
			// get filtered records
			List<EISDoc> records = doQuery(searchInputs,1000000);

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
			
		} catch (ParseException pe) {
			throw pe;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	
	}
	
	/** Combination title/fulltext query including the metadata parameters like agency/state/...
				 * and this is currently the default search; returns metadata plus filename 
				 * using Lucene's internal default scoring algorithm
				 * @throws ParseException
				 * */
	@Override
	public List<MetadataWithContext3> CombinedSearchNoContextHibernate6(SearchInputs searchInputs, SearchType searchType, int limit) throws ParseException {
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

			searchInputs.title = mutateTermModifiers(searchInputs.title);
			
			// get filtered records
			List<EISDoc> records = doQuery(searchInputs, 1000000);
			
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
				List<MetadataWithContext3> results = getScoredWithLuceneId(title, limit);
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
				return finalResults;
			}
		} catch(ParseException pe) {
			throw pe;
//			String problem = pe.getLocalizedMessage();
//			MetadataWithContext3 result = new MetadataWithContext3(null, null, null, problem, 0);
//			List<MetadataWithContext3> results = new ArrayList<MetadataWithContext3>();
//			results.add(result);
//			return results;
		} catch(Exception e) {
			e.printStackTrace();
			String problem = e.getLocalizedMessage();
			MetadataWithContext3 result = new MetadataWithContext3(null, null, null, problem, 0);
			List<MetadataWithContext3> results = new ArrayList<MetadataWithContext3>();
			results.add(result);
			return results;
		}
	}
	

	/** Search both fields at once and return in combined scored order with lucene IDs */
	private List<MetadataWithContext3> getScoredWithLuceneId(String terms, int limit) throws Exception, ParseException {
		long startTime = System.currentTimeMillis();
		
		String formattedTerms = mutateTermModifiers(terms);
	
		System.out.println("Formatted terms: " + formattedTerms);
		 
	    // 1. Search; instantiate highlighter

		MultiSearcher indexSearcher = new MultiSearcher();
		
		// Issue: This malfunctions where terms and phrases are separated by stopwords.
		// Only solutions I've found:
		// - default OR (it's erroneously ORing the term before the stopword, but default OR would mean
		// that's not an error any more)
		// - only search on one field
		// - include stopwords by using a simpler/dumber Analyzer (this will lead to some massive results)
		// - rewrite everything to internally resolve the results from two separate queries on each field
		// - reinvent query parsing or at least manually remove any stopwords outside of "quotes"
		// since they do nothing.  Inside a "phrase query" they're parsed to "?" which is just
		// a placeholder for either any word, or any stopword in the sequence of words.  This obviously
		// gets complicated if the query has a lot of nested quotes.
		
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
				new String[] {"title", "plaintext"},
				analyzer);
//		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
//		new String[] {"title", "plaintext"},
//		new SimpleAnalyzer());
		mfqp.setDefaultOperator(Operator.AND);
		Query query = mfqp.parse(formattedTerms);
		// This results in requiring the terms be found in both title and plaintext fields if using .MUST,
		// otherwise it doesn't require all fields at all with .SHOULD.
//	    Query query = MultiFieldQueryParser.parse(
//	    		formattedTerms, 
//	    		new String[] {"title", "plaintext"}, 
//	    		new BooleanClause.Occur[] {BooleanClause.Occur.MUST,BooleanClause.Occur.MUST}, 
//	    		analyzer);
		
		// This is the best compromise I can come up with
//		QueryParser qpText = new QueryParser("plaintext",analyzer);
//		qpText.setDefaultOperator(Operator.AND);
//		QueryParser qpTitle = new QueryParser("title",analyzer);
//		qpTitle.setDefaultOperator(Operator.AND);
//		org.apache.lucene.search.BooleanQuery.Builder combined = 
//				new org.apache.lucene.search.BooleanQuery.Builder();
//		combined.add(qpText.parse(formattedTerms), BooleanClause.Occur.SHOULD);
//		combined.add(qpTitle.parse(formattedTerms), BooleanClause.Occur.SHOULD);
//		System.out.println(combined.build().toString());
//		Query query = combined.build();
		
		
	    System.out.println("Parsed query: " + query.toString());
	    
		ScoreDoc scoreDocs[] = indexSearcher.search(query, limit).scoreDocs;

		System.out.println("Search count " + scoreDocs.length);

		List<ScoredResult> converted = new ArrayList<ScoredResult>(scoreDocs.length);
		Set<Long> metaIds = new HashSet<Long>();
		Set<Long> textIds = new HashSet<Long>();
		int i = 0;

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
        
        List<EISDoc> docs = em
        		.createQuery("SELECT d FROM EISDoc d WHERE d.id IN :ids")
        		.setParameter("ids", metaIds)
        		.getResultList();
        
		
//		List<EISDoc> docs = em.createQuery("SELECT d FROM EISDoc d WHERE d.id IN :ids")
//			.setParameter("ids", metaIds).getResultList();
	
		if(Globals.TESTING){System.out.println("Docs results size: " + docs.size());}
		
		HashMap<Long, EISDoc> hashDocs = new HashMap<Long, EISDoc>();
		for(EISDoc doc : docs) {
			hashDocs.put(doc.getId(), doc);
		}
	
		// 2: Get DocumentTexts by IDs WITHOUT getting the entire texts.
	
		List<Object[]> textIdMetaAndFilenames = em
        		.createQuery("SELECT d.id, d.eisdoc, d.filename FROM DocumentText d WHERE d.id IN :ids")
				.setParameter("ids", textIds)
				.getResultList();
		
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
				ReducedText rd = hashTexts.get(ordered.id);
				EISDoc eisFromDoc = rd.eisdoc;
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
	public ArrayList<ArrayList<String>> getHighlightsFVH(UnhighlightedDTO unhighlighted) throws Exception, ParseException {
		long startTime = System.currentTimeMillis();
		int fragmentSizeCustom = setFragmentSize(unhighlighted.getFragmentSizeValue());

		MultiSearcher indexSearcher = new MultiSearcher();
		
		// Normalize whitespace and support added term modifiers
	    String formattedTerms = mutateTermModifiers(unhighlighted.getTerms());
		
		// build highlighter with StandardAnalyzer
//		StandardAnalyzer analyzer = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);

		QueryParser qp = new QueryParser("plaintext", analyzer);
		qp.setDefaultOperator(Operator.AND);
		Query luceneTextOnlyQuery = qp.parse(formattedTerms);

        File indexFile = new File(Globals.getIndexString());
        Directory directory = FSDirectory.open(indexFile.toPath());
        IndexReader indexReader = DirectoryReader.open(directory);

        FastVectorHighlighter fvh = new FastVectorHighlighter();
		
		ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();

		// To try to slightly optimize if/when using UnifiedHighlighter
		HashSet<String> fieldsToLoad = new HashSet<String>();
		fieldsToLoad.add("plaintext");

		UnifiedHighlighter highlighter = new UnifiedHighlighter(null, analyzer);
		
		for(Unhighlighted input : unhighlighted.getUnhighlighted()) {
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
	    					indexReader, 
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
								Globals.normalizeSpace(fragment)
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
								Globals.normalizeSpace(highlight)
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

	/** Fastest highlighting available, requires full term vectors indexed, no markup */
	@Override
	public ArrayList<ArrayList<String>> getHighlightsFVHNoMarkup(UnhighlightedDTO unhighlighted) throws Exception {
		long startTime = System.currentTimeMillis();
		// Normalize whitespace and support added term modifiers
	    String formattedTerms = mutateTermModifiers(unhighlighted.getTerms());
		
		// build highlighter with StandardAnalyzer
//		StandardAnalyzer analyzer = new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);

		QueryParser qp = new QueryParser("plaintext", analyzer);
		qp.setDefaultOperator(Operator.AND);
		Query luceneTextOnlyQuery = qp.parse(formattedTerms);

		MultiSearcher indexSearcher = new MultiSearcher();
		
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
		
		for(Unhighlighted input : unhighlighted.getUnhighlighted()) {
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
	    					indexSearcher.getTextReader(), 
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

	
	// this seems to be how to just get a total hit count
	@Override
	public int getTotalHits(String field) throws Exception {
		MultiSearcher indexSearcher = new MultiSearcher();
		
	    String formattedTerms = mutateTermModifiers(field);
	    
		MultiFieldQueryParser mfqp = new MultiFieldQueryParser(
					new String[] {"title", "plaintext"},
					new StandardAnalyzer());
		mfqp.setDefaultOperator(Operator.AND);

		Query luceneQuery = mfqp.parse(formattedTerms);
		
		org.apache.lucene.search.TotalHitCountCollector thcc = new TotalHitCountCollector();
		
		return indexSearcher.searchHits(luceneQuery, thcc);
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


	/** Uses full parameters (not just a String for terms) to narrow down results */
	private List<EISDoc> doQuery(SearchInputs searchInputs, int queryLimit) {
		
		// Init parameter lists
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

			// Wishlist: Redo the types logic if we ever put these search params back in
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

		if(Globals.saneInput(searchInputs.needsComments)) {
			whereList.add(" (comments_filename<>'')");
		}

		if(Globals.saneInput(searchInputs.needsDocument)) { 
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
		
		if(searchInputs.title == null || searchInputs.title.length() == 0) {
			sQuery += " ORDER BY REGISTER_DATE DESC";
		}
		
		sQuery += " LIMIT " + String.valueOf(queryLimit);
		

		// debugging
		if(Globals.TESTING) {
			System.out.println(sQuery); 
		}
		
		// This simply uses the EISDoc(...) constructor in EISDoc.java, so they should line up in the same order
		// or it could get confused.
		return jdbcTemplate.query
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
					rs.getString("notes"),
					rs.getString("web_link"),
					rs.getObject("noi_date", LocalDate.class), 
					rs.getObject("draft_noa", LocalDate.class), 
					rs.getObject("final_noa", LocalDate.class), 
					rs.getObject("first_rod_date", LocalDate.class),
					rs.getLong("process_id"),
					rs.getString("county"), 
					rs.getString("status"),
					rs.getString("subtype")
				)
			);
	}
	
}
