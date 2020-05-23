package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.EmailLog;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
	EmailLog findByEmail(String email);

	List<EmailLog> findAll();
}