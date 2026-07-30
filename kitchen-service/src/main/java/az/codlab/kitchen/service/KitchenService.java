package az.codlab.kitchen.service;

import az.codlab.kitchen.dto.KitchenOrderResponse;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KitchenService {

    private static final Logger log = LoggerFactory.getLogger(KitchenService.class);
    // TODO: order-service-e HTTP call (RestClient/WebClient) ile melumatlari cek

    public KitchenOrderGroup getOrders(UUID orgId) {
        log.debug("Fetching kitchen orders for org: {}", orgId);
        // TODO: order-service-den orders cek, status-a gore qruplasdir
        // status filter: new = PENDING, CONFIRMED; preparing = PREPARING; ready = READY
        return new KitchenOrderGroup(List.of(), List.of(), List.of());
    }

    public record KitchenOrderGroup(
            List<KitchenOrderResponse> newOrders,
            List<KitchenOrderResponse> preparing,
            List<KitchenOrderResponse> ready
    ) {}

}
