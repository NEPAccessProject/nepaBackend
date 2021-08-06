package nepaBackend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.auth0.jwt.JWT;

import nepaBackend.model.ApplicationUser;
import nepaBackend.security.SecurityConstants;

@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {
    ApplicationUser findByUsername(String username);
    ApplicationUser findByEmail(String email);
    

	@Query(value = "SELECT id,username,active,email,verified,first_name,last_name,role FROM application_user a",
			nativeQuery = true)
    List<Object> findLimited();
	
	@Modifying
	@Transactional
	@Query(value = "UPDATE `application_user` SET last_login=now() WHERE id=:id",
			nativeQuery = true)
	void updateLoginDate(@Param("id") Long id);
}