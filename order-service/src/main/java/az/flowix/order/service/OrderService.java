package az.flowix.order.service;

import az.flowix.common.enums.OrderItemStatus;
import az.flowix.common.enums.OrderMode;
import az.flowix.common.enums.OrderSource;
import az.flowix.common.enums.OrderStatus;
import az.flowix.common.enums.PaymentMethod;
import az.flowix.common.enums.PaymentStatus;
import az.flowix.common.enums.PaymentTiming;
import az.flowix.common.enums.TableStatus;
import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.common.security.context.SecurityContextFacade;
import az.flowix.order.client.MenuServiceClient;
import az.flowix.order.client.SettingServiceClient;
import az.flowix.order.client.TableServiceClient;
import az.flowix.order.client.dto.ClientStatusUpdateRequest;
import az.flowix.order.dto.AddItemsRequest;
import az.flowix.order.dto.CancelRequest;
import az.flowix.order.dto.OrderRequest;
import az.flowix.order.dto.OrderResponse;
import az.flowix.order.dto.PaymentRequest;
import az.flowix.order.dto.StatusRequest;
import az.flowix.order.dto.WaiterConfirmRequest;
import az.flowix.order.entity.Order;
import az.flowix.order.entity.OrderItem;
import az.flowix.order.error.OrderErrorCode;
import az.flowix.order.mapper.OrderMapper;
import az.flowix.order.repository.OrderItemRepository;
import az.flowix.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final TableServiceClient tableServiceClient;
    private final MenuServiceClient menuServiceClient;
    private final SettingServiceClient settingServiceClient;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderMapper orderMapper,
                        TableServiceClient tableServiceClient,
                        MenuServiceClient menuServiceClient,
                        SettingServiceClient settingServiceClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.tableServiceClient = tableServiceClient;
        this.menuServiceClient = menuServiceClient;
        this.settingServiceClient = settingServiceClient;
    }

    public List<OrderResponse> getOrders(UUID orgId, String status, UUID tableId, UUID waiterId) {
        List<Order> orders;
        if (status != null && tableId != null) {
            orders = orderRepository.findByOrgIdAndTableIdAndStatus(orgId, tableId, parseOrderStatus(status));
        } else if (status != null) {
            orders = orderRepository.findByOrgIdAndStatus(orgId, parseOrderStatus(status));
        } else if (tableId != null) {
            orders = orderRepository.findByOrgIdAndTableId(orgId, tableId);
        } else if (waiterId != null) {
            orders = orderRepository.findByOrgIdAndWaiterId(orgId, waiterId);
        } else {
            orders = orderRepository.findByOrgId(orgId);
        }
        return orders.stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public OrderResponse getOrder(UUID id) {
        return buildResponse(findOrder(id));
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        var source = OrderSource.valueOf(request.getOrderSource().toUpperCase());

        var tableResponse = unwrap(tableServiceClient.getTable(request.getTableId()));
        if (!TableStatus.AVAILABLE.name().equals(tableResponse.getStatus())) {
            throw OrderErrorCode.TABLE_NOT_AVAILABLE.badRequest();
        }

        var menuItems = unwrap(menuServiceClient.getItems(request.getOrgId()));
        var menuItemMap = menuItems.stream()
                .collect(Collectors.toMap(mi -> mi.getId(), mi -> mi));
        for (var itemReq : request.getItems()) {
            var menuItem = menuItemMap.get(itemReq.getMenuItemId());
            if (menuItem == null) {
                throw OrderErrorCode.MENU_ITEM_NOT_FOUND.badRequest();
            }
            if (!menuItem.isAvailable()) {
                throw OrderErrorCode.MENU_ITEM_NOT_AVAILABLE.badRequest();
            }
        }

        var settings = unwrap(settingServiceClient.getSettings(request.getOrgId()));

        boolean isWaiter = source == OrderSource.WAITER;
        OrderStatus initialStatus;
        boolean waiterConfirmed;

        if (isWaiter) {
            initialStatus = OrderStatus.CONFIRMED;
            waiterConfirmed = true;
        } else {
            var orderMode = OrderMode.valueOf(settings.getOrderMode().toUpperCase());
            if (orderMode == OrderMode.CUSTOMER_WAITER_CONFIRM) {
                initialStatus = OrderStatus.PENDING;
                waiterConfirmed = false;
            } else {
                initialStatus = OrderStatus.CONFIRMED;
                waiterConfirmed = true;
            }
        }

        var paymentTiming = PaymentTiming.valueOf(settings.getPaymentTiming().toUpperCase());
        var initialPaymentStatus = paymentTiming == PaymentTiming.BEFORE
                ? PaymentStatus.PAID : PaymentStatus.PENDING;

        var order = Order.builder()
                .tableId(request.getTableId())
                .tableNumber(tableResponse.getTableNumber())
                .status(initialStatus)
                .paymentStatus(initialPaymentStatus)
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

        unwrap(tableServiceClient.updateTableStatus(request.getTableId(),
                ClientStatusUpdateRequest.builder().status(TableStatus.OCCUPIED.name()).currentOrderId(order.getId()).build()));

        log.info("Order created: {} for table {} (source: {})", order.getId(), request.getTableId(), source);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, StatusRequest request) {
        var order = findOrder(id);
        var newStatus = parseOrderStatus(request.getStatus());
        var oldStatus = order.getStatus();

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

        var newStatus = parseOrderItemStatus(request.getStatus());
        validateItemStatusTransition(item.getStatus(), newStatus);

        item.setStatus(newStatus);
        orderItemRepository.save(item);

        updateOrderStatusFromItems(order);

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
            throw OrderErrorCode.INVALID_STATUS_TRANSITION.badRequest();
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

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw OrderErrorCode.ORDER_NOT_CANCELLABLE.badRequest();
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(request != null ? request.getReason() : null);
        order = orderRepository.save(order);

        var items = orderItemRepository.findByOrderId(orderId);
        items.forEach(item -> {
            if (item.getStatus() != OrderItemStatus.SERVED && item.getStatus() != OrderItemStatus.CANCELLED) {
                item.setStatus(OrderItemStatus.CANCELLED);
            }
        });
        orderItemRepository.saveAll(items);

        boolean hasOtherActive = orderRepository.existsByTableIdAndOrgIdAndStatusNotIn(
                order.getTableId(), order.getOrgId(), List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));

        if (!hasOtherActive) {
            unwrap(tableServiceClient.updateTableStatus(order.getTableId(),
                    ClientStatusUpdateRequest.builder().status(TableStatus.CLEANING.name()).build()));
        }

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
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw OrderErrorCode.ORDER_NOT_ACTIVE.badRequest();
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.COMPLETED);
        order = orderRepository.save(order);

        unwrap(tableServiceClient.updateTableStatus(order.getTableId(),
                ClientStatusUpdateRequest.builder().status(TableStatus.AVAILABLE.name()).build()));

        log.info("Payment completed for order {}", orderId);
        return buildResponse(order);
    }

    @Transactional
    public OrderResponse startPreparing(UUID orderId) {
        var order = findOrder(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw OrderErrorCode.INVALID_STATUS_TRANSITION.badRequest();
        }

        var items = orderItemRepository.findByOrderId(orderId);
        items.forEach(item -> {
            if (item.getStatus() == OrderItemStatus.PENDING || item.getStatus() == OrderItemStatus.CONFIRMED) {
                item.setStatus(OrderItemStatus.PREPARING);
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
            if (item.getStatus() == OrderItemStatus.PREPARING) {
                item.setStatus(OrderItemStatus.READY);
            }
        });
        orderItemRepository.saveAll(items);

        order.setStatus(OrderStatus.READY);
        order = orderRepository.save(order);

        log.info("All items ready for order {}", orderId);
        return buildResponse(order);
    }

    private Order findOrder(UUID id) {
        var order = orderRepository.findById(id)
                .orElseThrow(OrderErrorCode.ORDER_NOT_FOUND::notFound);
        if (!SecurityContextFacade.isPlatformAdmin()) {
            String currentOrgId = SecurityContextFacade.getCurrentOrgId();
            if (currentOrgId != null && !currentOrgId.equals(order.getOrgId().toString())) {
                throw OrderErrorCode.ORDER_NOT_FOUND.notFound();
            }
        }
        return order;
    }

    private List<OrderItem> createOrderItems(UUID orderId, List<OrderRequest.OrderItemRequest> items, UUID orgId) {
        var menuItems = unwrap(menuServiceClient.getItems(orgId));
        var menuItemMap = menuItems.stream()
                .collect(toMap(mi -> mi.getId(), Function.identity()));
        return items.stream()
                .map(req -> {
                    var menuItem = menuItemMap.get(req.getMenuItemId());
                    if (menuItem == null) {
                        throw OrderErrorCode.MENU_ITEM_NOT_FOUND.badRequest();
                    }
                    var price = menuItem.getPrice();
                    if (req.getPrice() != null && req.getPrice().compareTo(price) != 0) {
                        throw OrderErrorCode.PRICE_MISMATCH.badRequest();
                    }
                    return orderItemRepository.save(OrderItem.builder()
                            .orderId(orderId)
                            .menuItemId(req.getMenuItemId())
                            .menuItemName(req.getMenuItemName())
                            .quantity(req.getQuantity())
                            .price(price)
                            .notes(req.getNotes() != null ? req.getNotes() : "")
                            .status(OrderItemStatus.PENDING)
                            .orgId(orgId)
                            .build());
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .filter(item -> item.getStatus() != OrderItemStatus.CANCELLED)
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
            case COMPLETED, CANCELLED -> Set.<OrderStatus>of();
        };
        if (!validNext.contains(next)) {
            throw OrderErrorCode.INVALID_STATUS_TRANSITION.badRequest();
        }
    }

    private void validateItemStatusTransition(OrderItemStatus current, OrderItemStatus next) {
        if (current == next) return;
        if (current == OrderItemStatus.CANCELLED || current == OrderItemStatus.SERVED) {
            throw OrderErrorCode.INVALID_ITEM_STATUS.badRequest();
        }
        var valid = switch (current) {
            case PENDING -> Set.of(OrderItemStatus.PREPARING, OrderItemStatus.CANCELLED);
            case CONFIRMED -> Set.of(OrderItemStatus.PREPARING, OrderItemStatus.CANCELLED);
            case PREPARING -> Set.of(OrderItemStatus.READY, OrderItemStatus.CANCELLED);
            case READY -> Set.of(OrderItemStatus.SERVED, OrderItemStatus.CANCELLED);
            default -> Set.<OrderItemStatus>of();
        };
        if (!valid.contains(next)) {
            throw OrderErrorCode.INVALID_ITEM_STATUS.badRequest();
        }
    }

    private void updateOrderStatusFromItems(Order order) {
        var allItems = orderItemRepository.findByOrderId(order.getId());
        if (allItems.isEmpty()) return;

        var nonCancelled = allItems.stream()
                .filter(i -> i.getStatus() != OrderItemStatus.CANCELLED)
                .collect(Collectors.toList());

        if (nonCancelled.isEmpty()) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            return;
        }

        boolean allReady = nonCancelled.stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.READY || i.getStatus() == OrderItemStatus.SERVED);
        boolean allServed = nonCancelled.stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.SERVED);

        if (allServed && order.getStatus() == OrderStatus.READY) {
            order.setStatus(OrderStatus.SERVED);
            orderRepository.save(order);
        } else if (allReady && order.getStatus() == OrderStatus.PREPARING) {
            order.setStatus(OrderStatus.READY);
            orderRepository.save(order);
        }
    }

    private OrderStatus parseOrderStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw OrderErrorCode.INVALID_STATUS.badRequest();
        }
    }

    private OrderItemStatus parseOrderItemStatus(String status) {
        try {
            return OrderItemStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw OrderErrorCode.INVALID_STATUS.badRequest();
        }
    }

    private static <T> T unwrap(ApiResponse<T> response) {
        if (response == null) {
            throw new RuntimeException("No response from downstream service");
        }
        if (!response.isSuccess() || response.getData() == null) {
            throw new RuntimeException(
                    response.getMessage() != null ? response.getMessage() : "External service returned unsuccessful response");
        }
        return response.getData();
    }

    private OrderResponse buildResponse(Order order) {
        var items = orderItemRepository.findByOrderId(order.getId());
        return OrderResponse.builder()
                .id(order.getId().toString())
                .tableId(order.getTableId())
                .tableNumber(order.getTableNumber())
                .items(items.stream().map(orderMapper::toItemDto).collect(Collectors.toList()))
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
