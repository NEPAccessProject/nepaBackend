package nepaBackend.pojo;

import nepaBackend.enums.EntityType;

public class ScoredResult {
	public Long id;
	public Class<?> className;
	public Float score;
	public Integer idx;
	public int luceneId;
	public EntityType entityName;
	
	public ScoredResult() {
	}
}
