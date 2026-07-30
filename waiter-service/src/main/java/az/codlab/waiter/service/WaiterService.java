package az.codlab.waiter.service;

import az.codlab.waiter.dto.WaiterOrderResponse;
import az.codlab.waiter.dto.WaiterTablesWrapper;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WaiterService {

    private static final Logger log = LoggerFactory.getLogger(WaiterService.class);
    // TODO: table-service ve order-service-e HTTP call ile melumatlari birlesdir

    public WaiterTablesWrapper getTables(UUID orgId) {
        log.debug("Fetching waiter tables for org: {}", orgId);
        // TODO: table-service-den masalari, order-service-den sifaris melumatlarini cek
        return new WaiterTablesWrapper(List.of());
    }

    public List<WaiterOrderResponse> getPendingConfirmOrders(UUID orgId) {
        log.debug("Fetching pending confirm orders for org: {}", orgId);
        // TODO: order-service-den waiterConfirmed=false, orderSource=CUSTOMER, status=PENDING filter
        return List.of();
    }

    public List<WaiterOrderResponse> getPaymentRequests(UUID orgId) {
        log.debug("Fetching payment requests for org: {}", orgId);
        // TODO: order-service-den paymentRequested=true, paymentStatus=PENDING filter
        return List.of();
    }

}
