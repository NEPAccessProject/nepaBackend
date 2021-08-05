package nepaBackend;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.DeleteRequest;

public interface DeleteRequestRepository extends JpaRepository<DeleteRequest, Long> {
	
}
