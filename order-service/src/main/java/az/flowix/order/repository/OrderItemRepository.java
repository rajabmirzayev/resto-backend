package az.flowix.order.repository;

import az.flowix.order.entity.OrderItem;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    long countByOrderIdAndStatus(UUID orderId, String status);

}
