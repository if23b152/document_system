package at.technikum_wien.rest_server.repository;

import at.technikum_wien.rest_server.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<AppUser> findByTemporaryTrue();
}
