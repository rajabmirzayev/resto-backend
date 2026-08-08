package az.flowix.order.mapper;

import az.flowix.order.dto.OrderResponse;
import az.flowix.order.entity.OrderItem;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    default OrderResponse.OrderItemResponse toItemDto(OrderItem item) {
        if (item == null) return null;
        return OrderResponse.OrderItemResponse.builder()
                .id(item.getId().toString())
                .menuItemId(item.getMenuItemId())
                .menuItemName(item.getMenuItemName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .notes(item.getNotes())
                .status(item.getStatus().name())
                .build();
    }

}
