# Tabler Entity Specification

> Fayl yolu: `tabler-back/tabler-entities.md`

---

## Microservice → Entity Map

| # | Gradle Module | API Prefix | Entities | Package |
|---|---|---|---|---|
| 1 | `common-jpa` | — | `CoreEntity`, `SoftDeletableCoreEntity`, enums, types, DTOs | `az.codlab.common.*` |
| 2 | `auth-gateway` | `/api/auth-ms/auth/` | `RefreshToken` | `az.codlab.auth.entity` |
| 3 | `cloud-gateway` | `/api/...` (gateway) | — | — |
| 4 | `common-security` | — | — | — |
| 5 | `common-exception-handling` | — | — | — |
| 6 | `organization-service` | `/api/organization-ms/v1/` | `Organization` | `az.codlab.organization.entity` |
| 7 | `user-service` | `/api/user-ms/v1/` | `User` | `az.codlab.user.entity` |
| 8 | `role-service` | `/api/role-ms/v1/` | `Role` | `az.codlab.role.entity` |
| 9 | `menu-service` | `/api/menu-ms/v1/` | `MenuCategory`, `MenuItem` | `az.codlab.menu.entity` |
| 10 | `table-service` | `/api/table-ms/v1/` | `Section`, `RestaurantTable` | `az.codlab.table.entity` |
| 11 | `order-service` | `/api/order-ms/v1/` | `Order`, `OrderItem` | `az.codlab.order.entity` |
| 12 | `kitchen-service` | `/api/kitchen-ms/v1/` | _(reuses Order/OrderItem)_ | — |
| 13 | `waiter-service` | `/api/waiter-ms/v1/` | _(aggregation)_ | — |
| 14 | `customer-service` | `/api/customer-ms/v1/` | _(public API)_ | — |
| 15 | `setting-service` | `/api/setting-ms/v1/` | `OrgSetting` | `az.codlab.setting.entity` |
| 16 | `dashboard-service` | `/api/dashboard-ms/v1/` | _(aggregation)_ | — |
| 17 | `report-service` | `/api/report-ms/v1/` | _(aggregation)_ | — |

---

## 1. Common — `common-jpa`

**Package:** `az.codlab.common.entity`

### `CoreEntity`

```java
package az.codlab.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class CoreEntity extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;
}
```

### `SoftDeletableCoreEntity`

```java
package az.codlab.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class SoftDeletableCoreEntity extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;
}
```

---

**Package:** `az.codlab.common.enums`

### `UserRole`

```java
package az.codlab.common.enums;

public enum UserRole {
    ADMIN, ORG_ADMIN, WAITER, CHEF, CUSTOMER
}
```

### `TableStatus`

```java
package az.codlab.common.enums;

public enum TableStatus {
    AVAILABLE, OCCUPIED, RESERVED, CLEANING
}
```

### `OrderStatus`

```java
package az.codlab.common.enums;

public enum OrderStatus {
    PENDING, CONFIRMED, PREPARING, READY, SERVED, COMPLETED, CANCELLED
}
```

### `PaymentStatus`

```java
package az.codlab.common.enums;

public enum PaymentStatus {
    PENDING, PAID
}
```

### `PaymentMethod`

```java
package az.codlab.common.enums;

public enum PaymentMethod {
    CASH, CARD
}
```

### `OrderMode`

```java
package az.codlab.common.enums;

public enum OrderMode {
    WAITER, CUSTOMER, CUSTOMER_WAITER_CONFIRM, KITCHEN
}
```

### `OrderSource`

```java
package az.codlab.common.enums;

public enum OrderSource {
    WAITER, CUSTOMER
}
```

### `CustomerTheme`

```java
package az.codlab.common.enums;

public enum CustomerTheme {
    CLASSIC, EMERALD, SUNSET, ROSE, VIOLET, AMBER
}
```

### `PaymentTiming`

```java
package az.codlab.common.enums;

public enum PaymentTiming {
    BEFORE, AFTER
}
```

---

**Package:** `az.codlab.common.type`

### `LocalizedString`

> PostgreSQL `jsonb` sütununda saxlanılır.

```java
package az.codlab.common.type;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocalizedString {
    String az;
    String en;
    String ru;
}
```

### `TableReservation`

```java
package az.codlab.common.type;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TableReservation {
    String guestName;
    String phone;
    Instant time;
    Integer guestCount;
    String notes;
}
```

---

**Package:** `az.codlab.common.dto`

### `ApiResponse<T>`

```java
package az.codlab.common.exception.handling.dto;

import az.codlab.common.exception.handling.error.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static az.codlab.common.exception.handling.utils.Constant.DEFAULT_SUCCESS_MESSAGE;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    boolean success;
    String message;
    String errorCode;
    T data;

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .message(DEFAULT_SUCCESS_MESSAGE)
                .errorCode(null)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(DEFAULT_SUCCESS_MESSAGE)
                .errorCode(null)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .errorCode(null)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, CommonErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode.getCode())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, CommonErrorCode errorCode, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode.getCode())
                .data(data)
                .build();
    }
}
```

---

**Package:** `az.codlab.common.constant`

### `Permissions`

```java
package az.codlab.common.constant;

import java.util.List;
import java.util.Set;

public final class Permissions {

    private Permissions() {}

    public static final String DASHBOARD_VIEW   = "dashboard.view";
    public static final String MENU_VIEW        = "menu.view";
    public static final String MENU_CREATE      = "menu.create";
    public static final String MENU_EDIT        = "menu.edit";
    public static final String MENU_DELETE      = "menu.delete";
    public static final String TABLES_VIEW      = "tables.view";
    public static final String TABLES_MANAGE    = "tables.manage";
    public static final String TABLES_STATUS    = "tables.status";
    public static final String ORDERS_VIEW      = "orders.view";
    public static final String ORDERS_MANAGE    = "orders.manage";
    public static final String ORDERS_CANCEL    = "orders.cancel";
    public static final String REPORTS_VIEW     = "reports.view";
    public static final String STAFF_VIEW       = "staff.view";
    public static final String STAFF_CREATE     = "staff.create";
    public static final String STAFF_EDIT       = "staff.edit";
    public static final String STAFF_DELETE     = "staff.delete";
    public static final String ROLES_VIEW       = "roles.view";
    public static final String ROLES_CREATE     = "roles.create";
    public static final String ROLES_EDIT       = "roles.edit";
    public static final String ROLES_DELETE     = "roles.delete";
    public static final String KITCHEN_VIEW     = "kitchen.view";
    public static final String KITCHEN_MANAGE   = "kitchen.manage";
    public static final String SETTINGS_VIEW    = "settings.view";
    public static final String SETTINGS_EDIT    = "settings.edit";

    public static final Set<String> ALL = Set.of(
        DASHBOARD_VIEW, MENU_VIEW, MENU_CREATE, MENU_EDIT, MENU_DELETE,
        TABLES_VIEW, TABLES_MANAGE, TABLES_STATUS,
        ORDERS_VIEW, ORDERS_MANAGE, ORDERS_CANCEL,
        REPORTS_VIEW,
        STAFF_VIEW, STAFF_CREATE, STAFF_EDIT, STAFF_DELETE,
        ROLES_VIEW, ROLES_CREATE, ROLES_EDIT, ROLES_DELETE,
        KITCHEN_VIEW, KITCHEN_MANAGE,
        SETTINGS_VIEW, SETTINGS_EDIT
    );

    public static final List<String> SUPER_ADMIN_PERMISSIONS = List.copyOf(ALL);

    public static final List<String> ORG_ADMIN_PERMISSIONS = List.of(
        DASHBOARD_VIEW, MENU_VIEW, MENU_CREATE, MENU_EDIT, MENU_DELETE,
        TABLES_VIEW, TABLES_MANAGE, TABLES_STATUS,
        ORDERS_VIEW, ORDERS_MANAGE, ORDERS_CANCEL,
        KITCHEN_VIEW, KITCHEN_MANAGE
    );
}
```

---

## 2. Auth Gateway — `auth-gateway`

> API prefix: `/api/auth-ms/auth/`
> Package: `az.codlab.auth.entity`

### `RefreshToken`

```java
package az.codlab.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    String token;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Builder.Default
    @Column(name = "revoked", nullable = false)
    boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "keycloak_user_id")
    String keycloakUserId;
}
```

---

## 3. Organization Service — `organization-service`

> API prefix: `/api/organization-ms/v1/`
> Package: `az.codlab.organization.entity`

### `Organization`

```java
package az.codlab.organization.entity;

import az.codlab.common.entity.SoftDeletableCoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Organization extends SoftDeletableCoreEntity {

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "slug", nullable = false, unique = true)
    String slug;

    @Column(name = "admin_name", nullable = false)
    String adminName;

    @Column(name = "admin_email", nullable = false)
    String adminEmail;

    @Column(name = "logo_url")
    String logoUrl;

    @Column(name = "phone")
    String phone;

    @Column(name = "address")
    String address;
}
```

---

## 4. Role Service — `role-service`

> API prefix: `/api/role-ms/v1/`
> Package: `az.codlab.role.entity`

### `Role`

```java
package az.codlab.role.entity;

import az.codlab.common.entity.SoftDeletableCoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role extends SoftDeletableCoreEntity {

    @Column(name = "name", nullable = false)
    String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "jsonb", nullable = false)
    List<String> permissions;

    @Column(name = "is_system", nullable = false)
    boolean isSystem;

    @Column(name = "org_id")
    UUID orgId;
}
```

---

## 5. User Service — `user-service`

> API prefix: `/api/user-ms/v1/`
> Package: `az.codlab.user.entity`

### `User`

```java
package az.codlab.user.entity;

import az.codlab.common.entity.SoftDeletableCoreEntity;
import az.codlab.common.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends SoftDeletableCoreEntity {

    @Column(name = "keycloak_id", unique = true)
    String keycloakId;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "username", nullable = false)
    String username;

    @Column(name = "email")
    String email;

    @Column(name = "phone")
    String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    UserRole role;

    @Column(name = "role_id")
    UUID roleId;

    @Column(name = "org_id")
    UUID orgId;

    @Column(name = "avatar")
    String avatar;

    @Column(name = "is_active", nullable = false)
    boolean isActive;
}
```

---

## 6. Menu Service — `menu-service`

> API prefix: `/api/menu-ms/v1/`
> Package: `az.codlab.menu.entity`

### `MenuCategory`

```java
package az.codlab.menu.entity;

import az.codlab.common.entity.SoftDeletableCoreEntity;
import az.codlab.common.type.LocalizedString;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "menu_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MenuCategory extends SoftDeletableCoreEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name", columnDefinition = "jsonb", nullable = false)
    LocalizedString name;

    @Column(name = "icon", length = 50)
    String icon;

    @Column(name = "sort_order")
    Integer sortOrder;

    @Column(name = "org_id", nullable = false)
    UUID orgId;
}
```

### `MenuItem`

```java
package az.codlab.menu.entity;

import az.codlab.common.entity.SoftDeletableCoreEntity;
import az.codlab.common.type.LocalizedString;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MenuItem extends SoftDeletableCoreEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name", columnDefinition = "jsonb", nullable = false)
    LocalizedString name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description", columnDefinition = "jsonb")
    LocalizedString description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    @Column(name = "category_id", nullable = false)
    UUID categoryId;

    @Column(name = "image_url")
    String imageUrl;

    @Column(name = "is_available", nullable = false)
    boolean isAvailable;

    @Column(name = "preparation_time")
    Integer preparationTime;

    @Column(name = "org_id", nullable = false)
    UUID orgId;
}
```

---

## 7. Table Service — `table-service`

> API prefix: `/api/table-ms/v1/`
> Package: `az.codlab.table.entity`

### `Section`

```java
package az.codlab.table.entity;

import az.codlab.common.entity.SoftDeletableCoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Section extends SoftDeletableCoreEntity {

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "org_id", nullable = false)
    UUID orgId;
}
```

### `RestaurantTable`

```java
package az.codlab.table.entity;

import az.codlab.common.entity.SoftDeletableCoreEntity;
import az.codlab.common.enums.TableStatus;
import az.codlab.common.type.TableReservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "restaurant_tables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RestaurantTable extends SoftDeletableCoreEntity {

    @Column(name = "table_number", nullable = false)
    Integer tableNumber;

    @Column(name = "capacity", nullable = false)
    Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    TableStatus status;

    @Column(name = "section_id")
    UUID sectionId;

    @Column(name = "current_order_id")
    UUID currentOrderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reservation", columnDefinition = "jsonb")
    TableReservation reservation;

    @Column(name = "org_id", nullable = false)
    UUID orgId;
}
```

---

## 8. Order Service — `order-service`

> API prefix: `/api/order-ms/v1/`
> Package: `az.codlab.order.entity`

### `Order`

```java
package az.codlab.order.entity;

import az.codlab.common.entity.CoreEntity;
import az.codlab.common.enums.OrderSource;
import az.codlab.common.enums.OrderStatus;
import az.codlab.common.enums.PaymentMethod;
import az.codlab.common.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends CoreEntity {

    @Column(name = "table_id", nullable = false)
    UUID tableId;

    @Column(name = "table_number")
    Integer tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    PaymentStatus paymentStatus;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal totalAmount;

    @Column(name = "waiter_id")
    UUID waiterId;

    @Column(name = "waiter_name")
    String waiterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_source", nullable = false, length = 20)
    OrderSource orderSource;

    @Column(name = "waiter_confirmed", nullable = false)
    boolean waiterConfirmed;

    @Column(name = "confirmed_by")
    String confirmedBy;

    @Column(name = "customer_photo")
    String customerPhoto;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 10)
    PaymentMethod paymentMethod;

    @Column(name = "payment_requested", nullable = false)
    boolean paymentRequested;

    @Column(name = "cancel_reason")
    String cancelReason;

    @Column(name = "org_id", nullable = false)
    UUID orgId;
}
```

### `OrderItem`

```java
package az.codlab.order.entity;

import az.codlab.common.entity.CoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem extends CoreEntity {

    @Column(name = "order_id", nullable = false)
    UUID orderId;

    @Column(name = "menu_item_id", nullable = false)
    UUID menuItemId;

    @Column(name = "menu_item_name", nullable = false)
    String menuItemName;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    @Column(name = "notes")
    String notes;

    @Column(name = "status", nullable = false, length = 20)
    String status;

    @Column(name = "org_id", nullable = false)
    UUID orgId;
}
```

---

## 9. Settings Service — `setting-service`

> API prefix: `/api/setting-ms/v1/`
> Package: `az.codlab.setting.entity`

### `OrgSetting`

```java
package az.codlab.setting.entity;

import az.codlab.common.entity.CoreEntity;
import az.codlab.common.enums.CustomerTheme;
import az.codlab.common.enums.OrderMode;
import az.codlab.common.enums.PaymentTiming;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "org_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrgSetting extends CoreEntity {

    @Column(name = "org_id", nullable = false, unique = true)
    UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_mode", nullable = false, length = 30)
    OrderMode orderMode;

    @Column(name = "customer_photo_required", nullable = false)
    boolean customerPhotoRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_timing", nullable = false, length = 10)
    PaymentTiming paymentTiming;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_theme", nullable = false, length = 20)
    CustomerTheme customerTheme;
}
```

---

## 10. Services without dedicated entities

| Service | API Prefix | Note |
|---|---|---|
| `kitchen-service` | `/api/kitchen-ms/v1/` | Reuses `Order`/`OrderItem` from `order-service` |
| `waiter-service` | `/api/waiter-ms/v1/` | Aggregation — reads from `order-service`, `table-service` |
| `customer-service` | `/api/customer-ms/v1/` | Public API — reads from `menu-service`, `table-service`, writes to `order-service` |
| `dashboard-service` | `/api/dashboard-ms/v1/` | Aggregation — reads from `order-service`, `menu-service` |
| `report-service` | `/api/report-ms/v1/` | Aggregation — reads from `order-service` |

---

## 11. Cross-Service Reference Convention

Microservice-lər arasında **JPA `@ManyToOne` / `@OneToMany` istifadə edilmir**. Yalnız `UUID` ID ilə referens saxlanılır.

| Entity | Foreign Key | Referenced Entity | Service |
|---|---|---|---|
| `Role` | `orgId` | `Organization` | `organization-service` |
| `User` | `orgId`, `roleId` | `Organization`, `Role` | `organization-service`, `role-service` |
| `MenuCategory` | `orgId` | `Organization` | `organization-service` |
| `MenuItem` | `orgId`, `categoryId` | `Organization`, `MenuCategory` | `organization-service`, `menu-service` |
| `Section` | `orgId` | `Organization` | `organization-service` |
| `RestaurantTable` | `orgId`, `sectionId` | `Organization`, `Section` | `organization-service`, `table-service` |
| `Order` | `orgId`, `tableId`, `waiterId` | `Organization`, `RestaurantTable`, `User` | `organization-service`, `table-service`, `user-service` |
| `OrderItem` | `orgId`, `orderId`, `menuItemId` | `Organization`, `Order`, `MenuItem` | `organization-service`, `order-service`, `menu-service` |
| `OrgSetting` | `orgId` (unique) | `Organization` | `organization-service` |

---

## 12. DB Indexes (critical)

```sql
-- organization-service
CREATE INDEX idx_org_slug ON organizations(slug) WHERE is_deleted = false;
CREATE INDEX idx_org_created_at ON organizations(created_at);

-- role-service
CREATE INDEX idx_role_org ON roles(org_id) WHERE is_deleted = false;

-- user-service
CREATE UNIQUE INDEX idx_user_username ON users(username) WHERE is_deleted = false;
CREATE INDEX idx_user_org ON users(org_id) WHERE is_deleted = false;
CREATE INDEX idx_user_role ON users(role);

-- menu-service
CREATE INDEX idx_category_org ON menu_categories(org_id) WHERE is_deleted = false;
CREATE INDEX idx_item_org ON menu_items(org_id) WHERE is_deleted = false;
CREATE INDEX idx_item_category ON menu_items(category_id) WHERE is_deleted = false;
CREATE INDEX idx_item_available ON menu_items(org_id, is_available) WHERE is_deleted = false;

-- table-service
CREATE INDEX idx_section_org ON sections(org_id) WHERE is_deleted = false;
CREATE INDEX idx_table_org ON restaurant_tables(org_id) WHERE is_deleted = false;
CREATE INDEX idx_table_section ON restaurant_tables(section_id) WHERE is_deleted = false;
CREATE INDEX idx_table_status ON restaurant_tables(org_id, status) WHERE is_deleted = false;

-- order-service
CREATE INDEX idx_order_org ON orders(org_id);
CREATE INDEX idx_order_table ON orders(table_id);
CREATE INDEX idx_order_status ON orders(org_id, status);
CREATE INDEX idx_order_waiter ON orders(waiter_id);
CREATE INDEX idx_order_created ON orders(created_at DESC);
CREATE INDEX idx_order_payment ON orders(payment_requested, payment_status) WHERE payment_requested = true;
CREATE INDEX idx_order_item_order ON order_items(order_id);
CREATE INDEX idx_order_item_status ON order_items(order_id, status);
```

---

## 13. Summary: new modules to create

Mövcud modullar: `auth-gateway`, `cloud-gateway`, `common-jpa`, `common-exception-handling`, `common-security`, `db-migrations`.

Yeni yaradılmalı olan modullar:

| Module | API Prefix | Entities |
|---|---|---|
| `organization-service` | `/api/organization-ms/v1/` | `Organization` |
| `role-service` | `/api/role-ms/v1/` | `Role` |
| `user-service` | `/api/user-ms/v1/` | `User` |
| `menu-service` | `/api/menu-ms/v1/` | `MenuCategory`, `MenuItem` |
| `table-service` | `/api/table-ms/v1/` | `Section`, `RestaurantTable` |
| `order-service` | `/api/order-ms/v1/` | `Order`, `OrderItem` |
| `setting-service` | `/api/setting-ms/v1/` | `OrgSetting` |
| `kitchen-service` | `/api/kitchen-ms/v1/` | — |
| `waiter-service` | `/api/waiter-ms/v1/` | — |
| `customer-service` | `/api/customer-ms/v1/` | — |
| `dashboard-service` | `/api/dashboard-ms/v1/` | — |
| `report-service` | `/api/report-ms/v1/` | — |
