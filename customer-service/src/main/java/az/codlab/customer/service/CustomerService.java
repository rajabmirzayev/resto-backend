package az.codlab.customer.service;

import az.codlab.customer.dto.CustomerMenuResponse;
import az.codlab.customer.dto.CustomerOrderRequest;
import az.codlab.customer.dto.CustomerOrderResponse;
import az.codlab.customer.dto.CustomerTableResponse;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);
    // TODO: menu-service, table-service ve order-service-e HTTP call-lar

    public CustomerMenuResponse getMenu(UUID orgId) {
        log.debug("Fetching public menu for org: {}", orgId);
        // TODO: menu-service-den kateqoriyalari ve available itemleri cek
        return new CustomerMenuResponse(List.of(), List.of());
    }

    public List<CustomerTableResponse> getAvailableTables(UUID orgId) {
        log.debug("Fetching available tables for org: {}", orgId);
        // TODO: table-service-den AVAILABLE statuslu masalari cek
        return List.of();
    }

    public CustomerOrderResponse createOrder(CustomerOrderRequest request) {
        log.debug("Creating customer order for table: {}", request.getTableId());
        // TODO: order-service-e POST /orders call, orderSource=CUSTOMER
        // TODO: org setting-den paymentTiming ve customerPhotoRequired yoxla
        return null;
    }

    public CustomerOrderResponse getOrder(UUID orderId) {
        log.debug("Fetching order: {}", orderId);
        // TODO: order-service-den sifarisi cek
        return null;
    }

    public void requestBill(UUID orderId, String method) {
        log.debug("Bill requested for order {} (method: {})", orderId, method);
        // TODO: order-service-e POST /orders/{id}/request-payment call
    }

}
