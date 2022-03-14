package nepaBackend;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import nepaBackend.model.Geojson;

@Repository
public interface GeojsonRepository extends JpaRepository<Geojson, Long> {

	List<Geojson> findAllByName(String name);

	Optional<Geojson> findByGeoId(Long geo_id);
	
//	boolean existsById(Long id);
	boolean existsByGeoId(Long geoId);

	/** Currently all non-state/county geojson has a geo_id of 5000000+, so this gets everything below that */
	@Query(value = "SELECT * FROM geojson WHERE geo_id < 5000000 ORDER BY geo_id",
			nativeQuery=true)
	List<Geojson> findAllStateCountyGeojson();
}
