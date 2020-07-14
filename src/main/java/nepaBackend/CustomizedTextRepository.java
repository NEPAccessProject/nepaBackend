package nepaBackend;

import java.util.List;

import nepaBackend.controller.MetadataWithContext;
import nepaBackend.model.DocumentText;
import nepaBackend.model.EISDoc;

public interface CustomizedTextRepository {
	List<EISDoc> search(String term, int limit, int offset);
//	List<String> searchContext(String term, int limit, int offset);
	List<MetadataWithContext> metaContext(String term, int limit, int offset);

	boolean sync();
}
