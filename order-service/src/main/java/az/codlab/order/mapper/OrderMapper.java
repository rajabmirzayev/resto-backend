package az.codlab.order.mapper;

import az.codlab.order.dto.OrderResponse;
import az.codlab.order.entity.Order;
import az.codlab.order.entity.OrderItem;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "items", source = "items")
    OrderResponse toDto(Order entity, List<OrderItem> items);

    default OrderResponse map(Order entity, List<OrderItem> items) {
        return toDto(entity, items);
    }

    default OrderResponse.OrderItemResponse toItemDto(OrderItem item) {
        if (item == null) return null;
        return OrderResponse.OrderItemResponse.builder()
                .id(item.getId().toString())
                .menuItemId(item.getMenuItemId())
                .menuItemName(item.getMenuItemName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .notes(item.getNotes())
                .status(item.getStatus())
                .build();
    }

}
