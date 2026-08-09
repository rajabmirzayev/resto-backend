package az.flowix.customer.service;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.customer.client.MenuServiceClient;
import az.flowix.customer.client.OrderServiceClient;
import az.flowix.customer.client.SettingServiceClient;
import az.flowix.customer.client.TableServiceClient;
import az.flowix.customer.client.dto.MenuServiceCategoryResponse;
import az.flowix.customer.client.dto.MenuServiceItemResponse;
import az.flowix.customer.client.dto.OrderServiceOrderRequest;
import az.flowix.customer.client.dto.OrderServiceOrderResponse;
import az.flowix.customer.client.dto.SettingServiceSettingResponse;
import az.flowix.customer.client.dto.TableServiceTableResponse;
import az.flowix.customer.dto.CustomerMenuResponse;
import az.flowix.customer.dto.CustomerOrderRequest;
import az.flowix.customer.dto.CustomerOrderResponse;
import az.flowix.customer.dto.CustomerTableResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final MenuServiceClient menuServiceClient;
    private final TableServiceClient tableServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final SettingServiceClient settingServiceClient;

    public CustomerService(MenuServiceClient menuServiceClient, TableServiceClient tableServiceClient,
                           OrderServiceClient orderServiceClient, SettingServiceClient settingServiceClient) {
        this.menuServiceClient = menuServiceClient;
        this.tableServiceClient = tableServiceClient;
        this.orderServiceClient = orderServiceClient;
        this.settingServiceClient = settingServiceClient;
    }

    public CustomerMenuResponse getMenu(UUID orgId) {
        log.debug("Fetching public menu for org: {}", orgId);
        var categories = unwrapList(menuServiceClient.getCategories(orgId)).stream()
                .map(c -> CustomerMenuResponse.CategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .icon(c.getIcon())
                        .build())
                .toList();
        var items = unwrapList(menuServiceClient.getItems(orgId)).stream()
                .filter(MenuServiceItemResponse::isAvailable)
                .map(i -> CustomerMenuResponse.ItemResponse.builder()
                        .id(i.getId())
                        .name(i.getName())
                        .description(i.getDescription())
                        .price(i.getPrice())
                        .categoryId(i.getCategoryId())
                        .imageUrl(i.getImageUrl())
                        .isAvailable(i.isAvailable())
                        .preparationTime(i.getPreparationTime())
                        .build())
                .toList();
        return new CustomerMenuResponse(categories, items);
    }

    public List<CustomerTableResponse> getAvailableTables(UUID orgId) {
        log.debug("Fetching available tables for org: {}", orgId);
        return unwrapList(tableServiceClient.getTables(orgId)).stream()
                .filter(t -> "AVAILABLE".equals(t.getStatus()))
                .map(t -> CustomerTableResponse.builder()
                        .id(t.getId())
                        .tableNumber(t.getTableNumber())
                        .capacity(t.getCapacity())
                        .sectionId(t.getSectionId())
                        .build())
                .toList();
    }

    public CustomerOrderResponse createOrder(CustomerOrderRequest request) {
        log.debug("Creating customer order for table: {}", request.getTableId());

        var settingsResponse = settingServiceClient.getSettings(request.getOrgId());
        var settings = settingsResponse != null && settingsResponse.isSuccess() ? settingsResponse.getData() : null;

        if (settings != null && settings.isCustomerPhotoRequired() && request.getCustomerPhoto() == null) {
            throw new IllegalArgumentException("Customer photo is required");
        }

        String paymentMethod = request.getPaymentMethod();
        if (settings != null && paymentMethod == null) {
            paymentMethod = switch (settings.getPaymentTiming()) {
                case "BEFORE" -> "CASH";
                default -> null;
            };
        }

        var orderRequest = OrderServiceOrderRequest.builder()
                .orgId(request.getOrgId())
                .tableId(request.getTableId())
                .orderSource("CUSTOMER")
                .customerPhoto(request.getCustomerPhoto())
                .paymentMethod(paymentMethod)
                .items(request.getItems().stream()
                        .map(i -> OrderServiceOrderRequest.OrderItemRequest.builder()
                                .menuItemId(i.getMenuItemId())
                                .menuItemName(i.getMenuItemName())
                                .quantity(i.getQuantity())
                                .price(i.getPrice())
                                .notes(i.getNotes())
                                .build())
                        .toList())
                .build();

        var createdResponse = orderServiceClient.createOrder(orderRequest);
        var created = createdResponse != null && createdResponse.isSuccess() ? createdResponse.getData() : null;
        if (created == null) {
            throw new RuntimeException("Failed to create order");
        }
        return toCustomerOrderResponse(created);
    }

    public CustomerOrderResponse getOrder(UUID orderId) {
        log.debug("Fetching order: {}", orderId);
        var response = orderServiceClient.getOrder(orderId);
        var order = response != null && response.isSuccess() ? response.getData() : null;
        if (order == null) {
            return null;
        }
        return toCustomerOrderResponse(order);
    }

    public void requestBill(UUID orderId, String method) {
        log.debug("Bill requested for order {} (method: {})", orderId, method);
        orderServiceClient.requestPayment(orderId, Map.of("method", method));
    }

    private CustomerOrderResponse toCustomerOrderResponse(OrderServiceOrderResponse o) {
        return CustomerOrderResponse.builder()
                .id(o.getId())
                .items(o.getItems().stream()
                        .map(i -> CustomerOrderResponse.ItemResponse.builder()
                                .id(i.getId())
                                .menuItemId(i.getMenuItemId())
                                .menuItemName(i.getMenuItemName())
                                .quantity(i.getQuantity())
                                .price(i.getPrice())
                                .notes(i.getNotes())
                                .status(i.getStatus())
                                .build())
                        .toList())
                .tableId(o.getTableId())
                .tableNumber(o.getTableNumber())
                .status(o.getStatus())
                .paymentStatus(o.getPaymentStatus())
                .totalAmount(o.getTotalAmount())
                .orderSource(o.getOrderSource())
                .waiterConfirmed(o.isWaiterConfirmed())
                .customerPhoto(o.getCustomerPhoto())
                .paymentMethod(o.getPaymentMethod())
                .paymentRequested(o.isPaymentRequested())
                .orgId(o.getOrgId())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private static <T> List<T> unwrapList(ApiResponse<List<T>> response) {
        return response != null && response.isSuccess() && response.getData() != null
                ? response.getData() : List.of();
    }

}
