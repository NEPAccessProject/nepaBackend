package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import nepaBackend.model.ApplicationUser;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {
    ApplicationUser findByUsername(String username);
    ApplicationUser findByEmail(String email);
    

	@Query(value = "SELECT id,username,active,email,verified,first_name,last_name,role FROM application_user a",
			nativeQuery = true)
    List<Object> findLimited();
}