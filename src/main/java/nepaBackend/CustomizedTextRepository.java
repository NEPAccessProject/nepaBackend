package nepaBackend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;

import nepaBackend.controller.MetadataWithContext;
import nepaBackend.controller.MetadataWithContext2;
import nepaBackend.enums.SearchType;
import nepaBackend.model.EISDoc;
import nepaBackend.pojo.HighlightedResult;
import nepaBackend.pojo.SearchInputs;
import nepaBackend.pojo.UnhighlightedDTO;

public interface CustomizedTextRepository {
	List<EISDoc> search(String term, int limit, int offset) throws ParseException;
//	List<String> searchContext(String term, int limit, int offset);
	List<MetadataWithContext> metaContext(String term, int limit, int offset, SearchType searchType) throws ParseException;

	List<EISDoc> metadataSearch(SearchInputs searchInputs, int limit, int offset, SearchType searchType);

	boolean sync();
	
	List<MetadataWithContext> CombinedSearchTitlePriority(SearchInputs searchInputs, SearchType searchType);
	List<MetadataWithContext> CombinedSearchLucenePriority(SearchInputs searchInputs, SearchType searchType);
	List<MetadataWithContext2> CombinedSearchNoContext(SearchInputs searchInputs, SearchType searchType);
	ArrayList<ArrayList<String>> getHighlights(UnhighlightedDTO unhighlighted) throws ParseException, IOException;
	List<Object[]> getRaw(String title) throws ParseException;
	List<MetadataWithContext2> getScored(String title) throws ParseException;
	List<EISDoc> searchTitles(String terms) throws ParseException;
	
	List<HighlightedResult> searchAndHighlight(String terms) throws Exception;
	
}
