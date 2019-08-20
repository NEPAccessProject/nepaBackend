package nepaBackend;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.EmailLog;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
	EmailLog findByEmail(String email);
}