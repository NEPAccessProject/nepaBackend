package nepaBackend;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.OptedOut;

public interface OptedOutRepository extends JpaRepository<OptedOut, Long> {
}