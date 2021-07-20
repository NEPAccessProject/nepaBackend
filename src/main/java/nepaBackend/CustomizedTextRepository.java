package nepaBackend;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;

import nepaBackend.controller.MetadataWithContext3;
import nepaBackend.enums.SearchType;
import nepaBackend.model.EISDoc;
import nepaBackend.pojo.SearchInputs;
import nepaBackend.pojo.UnhighlightedDTO;

public interface CustomizedTextRepository {
	List<EISDoc> searchTitles(String terms) throws ParseException;
	List<EISDoc> metadataSearch(SearchInputs searchInputs, int limit, int offset, SearchType searchType) throws ParseException;

	boolean sync();
	
	List<MetadataWithContext3> CombinedSearchNoContextHibernate6(SearchInputs searchInputs, SearchType searchType) throws ParseException;
	
	ArrayList<ArrayList<String>> getHighlightsFVH(UnhighlightedDTO unhighlighted) throws Exception;
	ArrayList<ArrayList<String>> getHighlightsFVHNoMarkup(UnhighlightedDTO unhighlighted) throws Exception;
	
	int getTotalHits(String field) throws Exception;

	String testTerms(String terms);
}
