package nepaBackend;

import java.util.List;
import java.util.Optional;

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
	@Query(value = "SELECT gtable.gid,COUNT(*) FROM"
			+ " (SELECT g.geojson_id gid,g.eisdoc_id FROM geojson_lookup g"
			+ " WHERE"
			+ " g.eisdoc_id IN :ids) gtable"
			+ " GROUP BY gtable.gid ORDER BY gtable.gid",
			nativeQuery=true)
	List<Object[]> findDistinctGeojsonByEisdocIdIn(@Param("ids") List<Long> ids);
	@Query(value = "SELECT gtable.gid,COUNT(*) FROM"
			+ " (SELECT g.geojson_id gid,g.eisdoc_id FROM geojson_lookup g"
			+ " WHERE"
			+ " g.eisdoc_id IN :docs) gtable"
			+ " GROUP BY gtable.gid ORDER BY gtable.gid",
			nativeQuery=true)
	List<Object[]> findDistinctGeojsonByEisdocIn(@Param("docs") List<EISDoc> docs);
	@Query(value = "SELECT gtable.gid,COUNT(*) FROM"
			+ " (SELECT g.geojson_id gid,g.eisdoc_id FROM geojson_lookup g"
			+ " WHERE"
			+ " g.eisdoc_id = :doc_id) gtable"
			+ " GROUP BY gtable.gid",
			nativeQuery=true)
	List<Object[]> findDistinctGeojsonByEisdocId(@Param("doc_id") Long doc_id);
	
	
	// TODO: Include logic to give back something like an Object[] with counts for each geojson
	// so the user knows how many record (or process, if we can figure it out) hits there are per polygon.

	boolean existsByEisdoc(EISDoc eisDoc);
	boolean existsByGeojsonAndEisdoc(Geojson geojson, EISDoc eisdoc);
	@Query(value = "SELECT IF( EXISTS("
			+ " SELECT gl.eisdoc_id,gl.geojson_id,g.id,g.geo_id FROM geojson_lookup gl" +
			"   INNER JOIN geojson g ON g.id = gl.geojson_id" + 
			"	WHERE gl.eisdoc_id = :eid AND g.geo_id = :gid)," + 
			"    'true','false' )AS result",
			nativeQuery=true)
	boolean existsByGeojsonAndEisdoc(@Param("gid") long geojson, @Param("eid") long eisdoc); // more efficient?


	List<GeojsonLookup> findAllByEisdoc(EISDoc doc);
	List<GeojsonLookup> findAllByEisdocIn(List<EISDoc> docList);
	
	Optional<GeojsonLookup> findByGeojsonId(long geojsonId);

	@Query(value = "SELECT eisdoc_id FROM geojson_lookup g"
			+ " WHERE g.geojson_id = :geoId",
			nativeQuery=true)
	List<Long> findAllEisdocIdByGeojsonId(@Param("geoId") Long geoId);

}
