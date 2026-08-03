package az.flowix.user.repository;

import az.flowix.user.entity.User;
import az.flowix.user.entity.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findAllByDeletedFalseOrderByCreatedAtDesc();

    Optional<User> findByIdAndDeletedFalse(UUID id);

    List<User> findAllByOrgIdAndDeletedFalse(UUID orgId);

    List<User> findAllByOrgIdAndRoleAndDeletedFalse(UUID orgId, UserRole role);

    List<User> findAllByRoleIdAndDeletedFalse(UUID roleId);

    boolean existsByUsernameAndDeletedFalse(String username);

}
