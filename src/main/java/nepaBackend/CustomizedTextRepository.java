package nepaBackend;

import java.util.Collection;
import java.util.List;

import nepaBackend.controller.MetadataWithContext;
import nepaBackend.enums.SearchType;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;
import nepaBackend.pojo.SearchInputs;

public interface CustomizedTextRepository {
	List<EISDoc> search(String term, int limit, int offset);
//	List<String> searchContext(String term, int limit, int offset);
	List<MetadataWithContext> metaContext(String term, int limit, int offset, SearchType searchType);

	List<EISDoc> metadataSearch(SearchInputs searchInputs, int i, int j, SearchType all);

	boolean sync();
}
