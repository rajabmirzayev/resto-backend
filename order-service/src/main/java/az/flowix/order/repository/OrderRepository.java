package az.flowix.order.repository;

import az.flowix.common.enums.OrderStatus;
import az.flowix.order.entity.Order;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByOrgId(UUID orgId);

    List<Order> findByOrgIdAndStatus(UUID orgId, OrderStatus status);

    List<Order> findByOrgIdAndTableId(UUID orgId, UUID tableId);

    List<Order> findByOrgIdAndWaiterId(UUID orgId, UUID waiterId);

    List<Order> findByOrgIdAndTableIdAndStatus(UUID orgId, UUID tableId, OrderStatus status);

}
