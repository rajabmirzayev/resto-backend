package az.codlab.setting.repository;

import az.codlab.setting.entity.OrgSetting;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgSettingRepository extends JpaRepository<OrgSetting, UUID> {

    Optional<OrgSetting> findByOrgId(UUID orgId);

}
