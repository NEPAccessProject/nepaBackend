package uapBackend;

import org.springframework.data.jpa.repository.JpaRepository;

import uapBackend.model.ApplicationUser;

public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {
    ApplicationUser findByUsername(String username);
}