package at.technikum_wien.rest_server.repository;

import at.technikum_wien.rest_server.model.ApiToken;
import at.technikum_wien.rest_server.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    @Query("select t from ApiToken t join fetch t.user where t.secretHash = :secretHash")
    Optional<ApiToken> findBySecretHashWithUser(String secretHash);

    List<ApiToken> findByUserOrderByCreatedAtDesc(AppUser user);

    Optional<ApiToken> findByIdAndUser(Long id, AppUser user);

    long countByUser(AppUser user);

    @Modifying
    @Query("delete from ApiToken t where t.user = :user")
    void deleteAllByUser(AppUser user);
}
