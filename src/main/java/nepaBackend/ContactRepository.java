package nepaBackend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nepaBackend.model.Contact;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

}
