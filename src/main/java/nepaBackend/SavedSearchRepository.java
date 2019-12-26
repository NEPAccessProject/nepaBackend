package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.SavedSearch;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {
	// Get all saved searches for a user by their ID
	List<SavedSearch> findByUserId(Long userId);
}
