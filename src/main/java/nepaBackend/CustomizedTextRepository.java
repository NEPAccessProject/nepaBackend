package nepaBackend;

import java.util.Collection;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;

import nepaBackend.controller.MetadataWithContext;
import nepaBackend.enums.SearchType;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.pojo.SearchInputs;

public interface CustomizedTextRepository {
	List<EISDoc> search(String term, int limit, int offset) throws ParseException;
//	List<String> searchContext(String term, int limit, int offset);
	List<MetadataWithContext> metaContext(String term, int limit, int offset, SearchType searchType) throws ParseException;

	List<EISDoc> metadataSearch(SearchInputs searchInputs, int i, int j, SearchType all);

	boolean sync();
	
	List<MetadataWithContext> CombinedSearchTitlePriority(SearchInputs searchInputs, int limit, int offset,
			SearchType searchType);
	List<MetadataWithContext> CombinedSearchLucenePriority(SearchInputs searchInputs, int limit, int offset, SearchType searchType);
}
