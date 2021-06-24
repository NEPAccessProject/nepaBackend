package nepaBackend;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;

import nepaBackend.controller.MetadataWithContext3;
import nepaBackend.enums.SearchType;
import nepaBackend.model.EISDoc;
import nepaBackend.pojo.SearchInputs;
import nepaBackend.pojo.Unhighlighted2DTO;

public interface CustomizedTextRepository {
	List<EISDoc> searchTitles(String terms) throws ParseException;
	List<EISDoc> metadataSearch(SearchInputs searchInputs, int limit, int offset, SearchType searchType);

	boolean sync();
	
	List<MetadataWithContext3> CombinedSearchNoContextHibernate6(SearchInputs searchInputs, SearchType searchType);
	
	ArrayList<ArrayList<String>> getHighlightsFVH(Unhighlighted2DTO unhighlighted) throws Exception;
	ArrayList<ArrayList<String>> getHighlightsFVHNoMarkup(Unhighlighted2DTO unhighlighted) throws Exception;
	
	int getTotalHits(String field) throws Exception;
}
