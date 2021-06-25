package nepaBackend;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.NEPAProcess;

public interface ProcessRepository extends JpaRepository<NEPAProcess, Long> {

	Optional<NEPAProcess> findByProcessId(Long valueOf);
	
}
