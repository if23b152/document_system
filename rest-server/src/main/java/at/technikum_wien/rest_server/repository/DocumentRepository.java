package at.technikum_wien.rest_server.repository;

import at.technikum_wien.rest_server.model.Document;
import at.technikum_wien.rest_server.model.AppUser;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    interface DocumentCleanupProjection {
        Long getId();
        String getMinioObjectKey();
    }

    List<Document> findAllByOrderByUploadTimestampDesc();

    List<Document> findByOwnerUsernameOrderByUploadTimestampDesc(String username);

    List<Document> findByOwnerId(Long ownerId);

    @Query("select d.id as id, d.minioObjectKey as minioObjectKey from Document d where d.owner.id = :ownerId")
    List<DocumentCleanupProjection> findCleanupDataByOwnerId(Long ownerId);

    Optional<Document> findByIdAndOwnerUsername(Long id, String username);
    @Modifying
    @Transactional
    @Query("update Document d set d.owner = :owner where d.owner is null")
    int assignOwnerToUnownedDocuments(AppUser owner);
}
