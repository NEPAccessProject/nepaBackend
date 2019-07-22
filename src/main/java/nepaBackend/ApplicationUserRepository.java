package nepaBackend;

import org.springframework.data.jpa.repository.JpaRepository;

import nepaBackend.model.ApplicationUser;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {
    ApplicationUser findByUsername(String username);
    ApplicationUser findByEmail(String email);
}