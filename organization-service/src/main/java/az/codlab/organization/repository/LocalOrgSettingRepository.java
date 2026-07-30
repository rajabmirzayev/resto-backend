package az.codlab.organization.repository;

import az.codlab.organization.entity.LocalOrgSetting;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalOrgSettingRepository extends JpaRepository<LocalOrgSetting, UUID> {

    Optional<LocalOrgSetting> findByOrgId(UUID orgId);

}
