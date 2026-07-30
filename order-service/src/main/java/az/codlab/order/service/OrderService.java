package az.codlab.order.service;

import az.codlab.common.enums.OrderSource;
import az.codlab.common.enums.OrderStatus;
import az.codlab.common.enums.PaymentMethod;
import az.codlab.common.enums.PaymentStatus;
import az.codlab.order.dto.AddItemsRequest;
import az.codlab.order.dto.CancelRequest;
import az.codlab.order.dto.OrderRequest;
import az.codlab.order.dto.OrderResponse;
import az.codlab.order.dto.PaymentRequest;
import az.codlab.order.dto.StatusRequest;
import az.codlab.order.dto.WaiterConfirmRequest;
import az.codlab.order.entity.Order;
import az.codlab.order.entity.OrderItem;
import az.codlab.order.error.OrderErrorCode;
import az.codlab.order.mapper.OrderMapper;
import az.codlab.order.repository.OrderItemRepository;
import az.codlab.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    // TODO: order-service hazir olanda real HTTP call-larla evez et

    private static final Set<String> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING.name(),
            OrderStatus.CONFIRMED.name(),
            OrderStatus.PREPARING.name(),
            OrderStatus.READY.name()
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
    }

    public List<OrderResponse> getOrders(UUID orgId, String status, UUID tableId, UUID waiterId) {
        List<Order> orders;
        if (status != null && tableId != null) {
            orders = orderRepository.findByOrgIdAndTableIdAndStatus(orgId, tableId, OrderStatus.valueOf(status.toUpperCase()));
        } else if (status != null) {
            orders = orderRepository.findByOrgIdAndStatus(orgId, OrderStatus.valueOf(status.toUpperCase()));
        } else if (tableId != null) {
            orders = orderRepository.findByOrgIdAndTableId(orgId, tableId);
        } else if (waiterId != null) {
            orders = orderRepository.findByOrgIdAndWaiterId(orgId, waiterId);
        } else {
            orders = orderRepository.findByOrgId(orgId);
        }
        return orders.stream()
                .map(order -> buildResponse(order))
                .collect(Collectors.toList());
    }

    public OrderResponse getOrder(UUID id) {
        var order = findOrder(id);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        var source = OrderSource.valueOf(request.getOrderSource().toUpperCase());
        // TODO: masanin movcudlugunu ve statusunu yoxla (table-service HTTP call)
        // TODO: org settings-den orderMode ve paymentTiming cek (setting-service HTTP call)
        // TODO: menu item-lerin movcudlugunu yoxla (menu-service HTTP call)

        boolean isWaiter = source == OrderSource.WAITER;
        OrderStatus initialStatus;
        boolean waiterConfirmed;

        if (isWaiter) {
            initialStatus = OrderStatus.CONFIRMED;
            waiterConfirmed = true;
        } else {
            // customer - hal-hazirda birbaşa CONFIRMED, gələcəkdə CUSTOMER_WAITER_CONFIRM mode-u
            // TODO: setting-den orderMode-e gore status təyin et
            initialStatus = OrderStatus.CONFIRMED;
            waiterConfirmed = true;
        }

        var order = Order.builder()
                .tableId(request.getTableId())
                .tableNumber(null) // TODO: table-service-den tableNumber cek
                .status(initialStatus)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .waiterId(request.getWaiterId())
                .waiterName(request.getWaiterName())
                .orderSource(source)
                .waiterConfirmed(waiterConfirmed)
                .customerPhoto(request.getCustomerPhoto())
                .paymentRequested(false)
                .orgId(request.getOrgId())
                .build();

        order = orderRepository.save(order);

        var items = createOrderItems(order.getId(), request.getItems(), request.getOrgId());
        var total = calculateTotal(items);

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        // TODO: masanin statusunu OCCUPIED et, currentOrderId set et (table-service HTTP call)

        log.info("Order created: {} for table {} (source: {})", order.getId(), request.getTableId(), source);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, StatusRequest request) {
        var order = findOrder(id);
        var newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        var oldStatus = order.getStatus();

        // TODO: status kecid validasiyasini duzgun tetbiq et
        // PENDING → CONFIRMED → PREPARING → READY → SERVED → COMPLETED
        // CANCELLED her yerden

        if (newStatus == OrderStatus.CANCELLED) {
            throw OrderErrorCode.ORDER_NOT_CANCELLABLE.badRequest();
        }

        validateStatusTransition(oldStatus, newStatus);
        order.setStatus(newStatus);
        order = orderRepository.save(order);
        log.info("Order {} status changed: {} → {}", id, oldStatus, newStatus);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse updateItemStatus(UUID orderId, UUID itemId, StatusRequest request) {
        var order = findOrder(orderId);
        var item = orderItemRepository.findById(itemId)
                .orElseThrow(OrderErrorCode.ITEM_NOT_FOUND::notFound);

        if (!item.getOrderId().equals(orderId)) {
            throw OrderErrorCode.ITEM_NOT_FOUND.notFound();
        }

        var newStatus = request.getStatus().toUpperCase();
        // TODO: item status kecid validasiyasi
        item.setStatus(newStatus);
        orderItemRepository.save(item);

        // TODO: eger butun itemler READY ise order status-u READY et
        // TODO: eger butun itemler SERVED ise order status-u SERVED et

        log.info("Order item {} status changed: {}", itemId, newStatus);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse addItems(UUID orderId, AddItemsRequest request) {
        var order = findOrder(orderId);

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw OrderErrorCode.ORDER_NOT_ACTIVE.badRequest();
        }

        var newItems = createOrderItems(orderId, request.getItems(), order.getOrgId());
        var allItems = new ArrayList<>(orderItemRepository.findByOrderId(orderId));
        allItems.addAll(newItems);
        var total = calculateTotal(allItems);
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        log.info("Items added to order {}", orderId);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse waiterConfirm(UUID orderId, WaiterConfirmRequest request) {
        var order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw OrderErrorCode.ORDER_NOT_PENDING.badRequest();
        }

        if (order.getOrderSource() != OrderSource.CUSTOMER) {
            // TODO: customer olmayan sifarislerde waiter confirm lazim deyil
        }

        order.setWaiterConfirmed(true);
        order.setConfirmedBy(request.getWaiterName());
        order.setWaiterId(request.getWaiterId());
        order.setWaiterName(request.getWaiterName());
        order.setStatus(OrderStatus.CONFIRMED);
        order = orderRepository.save(order);

        log.info("Order {} confirmed by waiter {}", orderId, request.getWaiterName());
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, CancelRequest request) {
        var order = findOrder(orderId);

        if (!CANCELLABLE_STATUSES.contains(order.getStatus().name())) {
            throw OrderErrorCode.ORDER_NOT_CANCELLABLE.badRequest();
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request != null ? request.getReason() : null);
        order = orderRepository.save(order);

        // TODO: masanin statusunu CLEANING et, currentOrderId temizle (table-service HTTP call)

        log.info("Order {} cancelled", orderId);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse requestPayment(UUID orderId, PaymentRequest request) {
        var order = findOrder(orderId);

        order.setPaymentRequested(true);
        order.setPaymentMethod(PaymentMethod.valueOf(request.getMethod().toUpperCase()));
        order = orderRepository.save(order);

        log.info("Payment requested for order {} (method: {})", orderId, request.getMethod());
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse completePayment(UUID orderId) {
        var order = findOrder(orderId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw OrderErrorCode.PAYMENT_ALREADY_COMPLETED.conflict();
        }

        // TODO: paymentStatus PAİD-dən əvvəl order status-u SERVED olmalidir?
        // TODO: əgər paymentTiming BEFORE idisə, payment artıq alınıb

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.COMPLETED);
        order = orderRepository.save(order);

        // TODO: masanin statusunu AVAILABLE et, currentOrderId temizle (table-service HTTP call)

        log.info("Payment completed for order {}", orderId);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse startPreparing(UUID orderId) {
        var order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PENDING) {
            throw OrderErrorCode.INVALID_STATUS_TRANSITION.badRequest();
        }

        var items = orderItemRepository.findByOrderId(orderId);
        items.forEach(item -> {
            if ("PENDING".equals(item.getStatus()) || "CONFIRMED".equals(item.getStatus())) {
                item.setStatus("PREPARING");
            }
        });
        orderItemRepository.saveAll(items);

        order.setStatus(OrderStatus.PREPARING);
        order = orderRepository.save(order);

        log.info("Order {} started preparing", orderId);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse markAllReady(UUID orderId) {
        var order = findOrder(orderId);

        var items = orderItemRepository.findByOrderId(orderId);
        items.forEach(item -> {
            if ("PREPARING".equals(item.getStatus())) {
                item.setStatus("READY");
            }
        });
        orderItemRepository.saveAll(items);

        order.setStatus(OrderStatus.READY);
        order = orderRepository.save(order);

        log.info("All items ready for order {}", orderId);
        return buildResponse(order);
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(OrderErrorCode.ORDER_NOT_FOUND::notFound);
    }

    private List<OrderItem> createOrderItems(UUID orderId, List<OrderRequest.OrderItemRequest> items, UUID orgId) {
        return items.stream()
                .map(req -> {
                    var item = OrderItem.builder()
                            .orderId(orderId)
                            .menuItemId(req.getMenuItemId())
                            .menuItemName(req.getMenuItemName())
                            .quantity(req.getQuantity())
                            .price(req.getPrice())
                            .notes(req.getNotes() != null ? req.getNotes() : "")
                            .status("PENDING")
                            .orgId(orgId)
                            .build();
                    return orderItemRepository.save(item);
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        var validNext = switch (current) {
            case PENDING -> Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
            case CONFIRMED -> Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED);
            case PREPARING -> Set.of(OrderStatus.READY, OrderStatus.CANCELLED);
            case READY -> Set.of(OrderStatus.SERVED, OrderStatus.CANCELLED);
            case SERVED -> Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED);
            case COMPLETED -> Set.<OrderStatus>of();
            case CANCELLED -> Set.<OrderStatus>of();
        };
        if (!validNext.contains(next)) {
            throw OrderErrorCode.INVALID_STATUS_TRANSITION.badRequest();
        }
    }

    private OrderResponse buildResponse(Order order) {
        var items = orderItemRepository.findByOrderId(order.getId());
        return OrderResponse.builder()
                .id(order.getId().toString())
                .tableId(order.getTableId())
                .tableNumber(order.getTableNumber())
                .items(items.stream()
                        .map(orderMapper::toItemDto)
                        .collect(Collectors.toList()))
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .totalAmount(order.getTotalAmount())
                .waiterId(order.getWaiterId())
                .waiterName(order.getWaiterName())
                .orderSource(order.getOrderSource().name())
                .waiterConfirmed(order.isWaiterConfirmed())
                .confirmedBy(order.getConfirmedBy())
                .customerPhoto(order.getCustomerPhoto())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentRequested(order.isPaymentRequested())
                .cancelReason(order.getCancelReason())
                .orgId(order.getOrgId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

}
