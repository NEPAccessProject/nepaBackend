package nepaBackend;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nepaBackend.model.Geojson;

@Repository
public interface GeojsonRepository extends JpaRepository<Geojson, Long> {

	List<Geojson> findAllByName(String name);

	Optional<Geojson> findByGeoId(Long geo_id);
	
//	boolean existsById(Long id);
	boolean existsByGeoId(Long geoId);
}
