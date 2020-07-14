package nepaBackend;

import java.util.List;

import nepaBackend.controller.MetadataWithContext;
import nepaBackend.enums.SearchType;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;

public interface CustomizedTextRepository {
	List<EISDoc> search(String term, int limit, int offset);
//	List<String> searchContext(String term, int limit, int offset);
	List<MetadataWithContext> metaContext(String term, int limit, int offset, SearchType searchType);

	boolean sync();
}
