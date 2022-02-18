package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nepaBackend.model.EISDoc;
import nepaBackend.model.Geojson;
import nepaBackend.model.GeojsonLookup;

@Repository
public interface GeojsonLookupRepository extends JpaRepository<GeojsonLookup, Long> {
	
	// Group by used when we only need to represent each unique geojson object once
	@Query(value = "SELECT DISTINCT * FROM geojson_lookup g"
			+ " WHERE"
			+ " g.eisdoc_id IN :ids"
			+ " GROUP BY g.geojson_id",
			nativeQuery=true)
	List<GeojsonLookup> findDistinctGeojsonByEisdocIdIn(@Param("ids") List<Long> ids);
	@Query(value = "SELECT DISTINCT * FROM geojson_lookup g"
			+ " WHERE"
			+ " g.eisdoc_id IN :docs"
			+ " GROUP BY g.geojson_id",
			nativeQuery=true)
	List<GeojsonLookup> findDistinctGeojsonByEisdocIn(@Param("docs") List<EISDoc> docs);
	@Query(value = "SELECT DISTINCT * FROM geojson_lookup g"
			+ " WHERE"
			+ " g.eisdoc_id = :doc_id"
			+ " GROUP BY g.geojson_id",
			nativeQuery=true)
	List<GeojsonLookup> findDistinctGeojsonByEisdocId(@Param("doc_id") Long doc_id);
	
	
	// TODO: Include logic to give back something like an Object[] with counts for each geojson
	// so the user knows how many record (or process, if we can figure it out) hits there are per polygon.

	boolean existsByEisdoc(EISDoc eisDoc);
	boolean existsByGeojsonAndEisdoc(Geojson geojson, EISDoc eisdoc);


	List<GeojsonLookup> findAllByEisdoc(EISDoc doc);
	List<GeojsonLookup> findAllByEisdocIn(List<EISDoc> docList);

}
