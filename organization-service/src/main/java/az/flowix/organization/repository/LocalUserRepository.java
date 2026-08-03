package az.flowix.organization.repository;

import az.flowix.organization.entity.LocalUser;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalUserRepository extends JpaRepository<LocalUser, UUID> {

    Optional<LocalUser> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsernameAndDeletedFalse(String username);

}
