# Tabler API Specification

## Ümumi Qaydalar

| Qayda | Dəyər |
|---|---|
| Base URL (API Gateway) | `http://localhost:8001` |
| Auth path | `/api/auth-ms/v1/auth/{action}` (birbaşa DTO, `ApiResponse` wrapper-i yoxdur) |
| Digər servislər | `/api/{service-ms}/v1/{resource}` (`ApiResponse<T>` wrapper-i ilə) |
| Uğur formatı (auth xaric) | `{ success: true, message: "...", data: {...} }` |
| Error formatı (bütün servislər) | Spring `ProblemDetail` (RFC 9457) — `key`, `path`, `timestamp` property-ləri ilə |
| Validation error | 400 + `fieldErrors` array |

---

## Ümumi Error Formatları

> Bütün microservice-lər eyni error formatını istifadə edir. Validation xaric bütün error-lar `fieldErrors` property-sini qaytarmır.

### Validation Error (400)

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "trace:xxx",
  "key": "USER_MS_1000",
  "path": "/api/user-ms/v1/users",
  "timestamp": "2026-07-30T12:00:00.000Z",
  "fieldErrors": [
    { "field": "name", "message": "Name is required" },
    { "field": "username", "message": "Username is required" }
  ]
}
```

### Unauthorized (401)

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required",
  "instance": "trace:xxx",
  "key": "COMMON_4001",
  "path": "/api/organization-ms/v1/organizations",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

### Forbidden (403)

```json
{
  "type": "about:blank",
  "title": "Access Denied",
  "status": 403,
  "detail": "Access is denied",
  "instance": "trace:xxx",
  "key": "COMMON_4003",
  "path": "/api/role-ms/v1/roles/r2",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

### Not Found (404)

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Organization with id org99 not found",
  "instance": "trace:xxx",
  "key": "ORG_MS_3001",
  "path": "/api/organization-ms/v1/organizations/org99",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

### Conflict (409)

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Table has an active order and cannot be deleted",
  "instance": "trace:xxx",
  "key": "TABLE_MS_2001",
  "path": "/api/table-ms/v1/tables/t2",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

### Internal Server Error (500)

```json
{
  "type": "about:blank",
  "title": "Internal Error",
  "status": 500,
  "detail": "Unexpected internal error",
  "instance": "trace:xxx",
  "key": "MENU_MS_9999",
  "path": "/api/menu-ms/v1/items",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

## 1. Auth — `auth-gateway` (port 8002)

> API prefix: `/api/auth-ms/v1/auth/...`
> Response: **birbaşa DTO** (`ApiResponse` wrapper-i yoxdur)
> Error: Spring `ProblemDetail` (yuxarıdakı format)

### `POST /api/auth-ms/v1/auth/login`

**Giriş.** Backend Keycloak üzərindən autentifikasiya edir.

Request:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

Success (200):
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl...",
  "expiresIn": 300,
  "roles": ["SUPER_ADMIN"],
  "uiScope": "ADMIN_PANEL"
}
```

**`uiScope`:**

| Dəyər | Redirect | İstifadəçi |
|---|---|---|
| `ADMIN_PANEL` | `/super-admin` | Super admin (`SUPER_ADMIN` rol) |
| `USER_PANEL` | `/admin` | Org admin, ofisant, aşpaz |

> **Qeyd:** Hal-hazırda `SUPER_ADMIN` roluna `ADMIN_PANEL`, qalanlarına `USER_PANEL` təyin olunur. Gələcəkdə `org_admin`, `waiter`, `chef` üçün ayrıca redirect əlavə olunacaq.

Error (401):
```json
{
  "type": "about:blank",
  "title": "Authentication Failed",
  "status": 401,
  "detail": "Invalid username or password",
  "instance": "/api/auth-ms/v1/auth/login",
  "key": "AUTH_001",
  "path": "/api/auth-ms/v1/auth/login",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

Error (502 — Keycloak unavailable):
```json
{
  "type": "about:blank",
  "title": "Service Unavailable",
  "status": 502,
  "detail": "Authentication service is temporarily unavailable. Please try again later.",
  "instance": "/api/auth-ms/v1/auth/login",
  "key": "AUTH_005",
  "path": "/api/auth-ms/v1/auth/login",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `POST /api/auth-ms/v1/auth/refresh`

**Refresh token ilə yeni access token.**

Request:
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

Success (200):
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "bmV3IHJlZnJlc2ggdG9r...",
  "expiresIn": 300
}
```

Error (401):
```json
{
  "type": "about:blank",
  "title": "Token Expired",
  "status": 401,
  "detail": "The access token has expired. Please refresh or login again.",
  "instance": "/api/auth-ms/v1/auth/refresh",
  "key": "AUTH_002",
  "path": "/api/auth-ms/v1/auth/refresh",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `POST /api/auth-ms/v1/auth/logout`

**Refresh token-i invalid edir (Keycloak session).**

Request:
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

Success (200): *empty body*

Error (502):
```json
{
  "type": "about:blank",
  "title": "Logout Failed",
  "status": 502,
  "detail": "Failed to revoke the session. Please try again.",
  "instance": "/api/auth-ms/v1/auth/logout",
  "key": "AUTH_004",
  "path": "/api/auth-ms/v1/auth/logout",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

## 2. Organization — `organization-service` (port 8102)

> API prefix: `/api/organization-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`

### `GET /api/organization-ms/v1/organizations`

**Bütün təşkilatların siyahısı. Super admin üçün.**

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Nərimanov Restoranı",
      "slug": "nerimanov-restoran",
      "adminName": "Orxan Əliyev",
      "adminEmail": "orxan@nerimanov.az",
      "logoUrl": null,
      "phone": "+994501234567",
      "address": "Nərimanov, Bakı",
      "createdAt": "2026-07-23T12:00:00.000Z"
    }
  ]
}
```

Error (401): *yuxarıdakı ümumi 401 formatı*

---

### `POST /api/organization-ms/v1/organizations`

**Yeni təşkilat yarat. Arxada avtomatik:**
- `org_admin` user-i (Keycloak + local DB)
- Org-a aid rol (permissions: `dashboard.view`, `menu.*`, `tables.*`, `orders.*`, `kitchen.*`)
- Default `OrgSetting` (`orderMode=customer`, `paymentTiming=after`, `customerTheme=classic`)
- Default `Zal 1` section

Headers: `Authorization: Bearer {token}` (super admin)

Request:
```json
{
  "name": "Nərimanov Restoranı",
  "adminName": "Orxan Əliyev",
  "adminEmail": "orxan@nerimanov.az",
  "adminPassword": "orxan123"
}
```

Success (201):
```json
{
  "success": true,
  "message": "Organization created",
  "errorCode": null,
  "data": {
    "organization": {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Nərimanov Restoranı",
      "slug": "nerimanov-restoran",
      "adminName": "Orxan Əliyev",
      "adminEmail": "orxan@nerimanov.az",
      "logoUrl": null,
      "phone": null,
      "address": null,
      "createdAt": "2026-07-30T12:00:00.000Z"
    },
    "adminUser": {
      "id": "550e8400-e29b-41d4-a716-446655440005",
      "name": "Orxan Əliyev",
      "username": "orxan@nerimanov.az",
      "email": "orxan@nerimanov.az",
      "role": "ORG_ADMIN",
      "roleId": "550e8400-e29b-41d4-a716-4466554400a1",
      "orgId": "550e8400-e29b-41d4-a716-446655440001"
    },
    "adminRole": {
      "id": "550e8400-e29b-41d4-a716-4466554400a1",
      "name": "Nərimanov Restoranı Admin",
      "permissions": [
        "dashboard.view",
        "menu.view", "menu.create", "menu.edit", "menu.delete",
        "tables.view", "tables.manage", "tables.status",
        "orders.view", "orders.manage", "orders.cancel",
        "kitchen.view", "kitchen.manage"
      ],
      "isSystem": false,
      "orgId": "550e8400-e29b-41d4-a716-446655440001"
    }
  }
}
```

Error (400):
```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "trace:xxx",
  "key": "ORG_MS_1000",
  "path": "/api/organization-ms/v1/organizations",
  "timestamp": "2026-07-30T12:00:00.000Z",
  "fieldErrors": [
    { "field": "name", "message": "Name is required" },
    { "field": "adminEmail", "message": "Invalid email format" }
  ]
}
```

---

### `GET /api/organization-ms/v1/organizations/{orgId}`

**Tək təşkilat məlumatı.**

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "name": "Nərimanov Restoranı",
    "slug": "nerimanov-restoran",
    "adminName": "Orxan Əliyev",
    "adminEmail": "orxan@nerimanov.az",
    "logoUrl": null,
    "phone": "+994501234567",
    "address": "Nərimanov, Bakı",
    "createdAt": "2026-07-23T12:00:00.000Z"
  }
}
```

Error (404):
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Organization with id org99 not found",
  "instance": "trace:xxx",
  "key": "ORG_MS_3001",
  "path": "/api/organization-ms/v1/organizations/org99",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `GET /api/organization-ms/v1/organizations/{orgId}/qr-code`

**QR kod (müştəri menyusu üçün).**

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "qrCodeUrl": "https://api.qrserver.com/v1/create-qr-code/?size=512x512&data=https%3A%2F%2Ftabler.az%2Forg%2F550e8400-e29b-41d4-a716-446655440001%2Fmenu"
  }
}
```

> Frontend hazırda `api.qrserver.com` istifadə edir. Backend öz QR generatoru əlavə edə bilər.

---

## 3. User / Staff — `user-service` (port 8103)

> API prefix: `/api/user-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`

### `GET /api/user-ms/v1/users`

**Bütün istifadəçilər.**

Headers: `Authorization: Bearer {token}`

Query:
| Parameter | Tip | Məcburi | İzah |
|---|---|---|---|
| `orgId` | UUID | X | Org-a görə filtr |
| `role` | String | X | Role adına görə filtr (`waiter`, `chef`) |

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440010",
      "keycloakId": "k123-keycloak-uuid",
      "name": "Leyla Hüseynova",
      "username": "waiter1",
      "email": null,
      "phone": "+994501112233",
      "role": "WAITER",
      "roleId": "550e8400-e29b-41d4-a716-446655440030",
      "orgId": "550e8400-e29b-41d4-a716-446655440001",
      "avatar": "",
      "isActive": true,
      "createdAt": "2026-07-24T10:00:00.000Z"
    }
  ]
}
```

> `password` field-i heç vaxt response-da qayıtmır.

---

### `GET /api/user-ms/v1/users/{id}`

**Tək istifadəçi.**

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "name": "Leyla Hüseynova",
    "username": "waiter1",
    "email": null,
    "role": "WAITER",
    "roleId": "550e8400-e29b-41d4-a716-446655440030",
    "orgId": "550e8400-e29b-41d4-a716-446655440001",
    "avatar": "",
    "isActive": true,
    "createdAt": "2026-07-24T10:00:00.000Z"
  }
}
```

Error (404):
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "User with id u99 not found",
  "instance": "trace:xxx",
  "key": "USER_MS_3001",
  "path": "/api/user-ms/v1/users/u99",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `POST /api/user-ms/v1/users`

**Yeni istifadəçi yarat (personal əlavə et). Keycloak-da da user yaranır.**

Headers: `Authorization: Bearer {token}`

Request:
```json
{
  "name": "Leyla Hüseynova",
  "username": "waiter1",
  "password": "waiter123",
  "roleId": "550e8400-e29b-41d4-a716-446655440030",
  "orgId": "550e8400-e29b-41d4-a716-446655440001",
  "email": null,
  "phone": "+994501112233"
}
```

> `phone` **Qlobal telefon formatına** tabedir (bütün servislərdə ortaq `@ValidPhone`): yalnız `0-9`, `+ - ( ) .` və boşluq; 7–15 rəqəm olmalıdır; maks 30 simvol. Saxlanarkən yalnız rəqəmlərə normalizasiya olunur (`+994 50 123 45 67` → `994501234567`). Field optionaldır.

Success (201):
```json
{
  "success": true,
  "message": "User created",
  "errorCode": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "name": "Leyla Hüseynova",
    "username": "waiter1",
    "email": null,
    "role": "WAITER",
    "roleId": "550e8400-e29b-41d4-a716-446655440030",
    "orgId": "550e8400-e29b-41d4-a716-446655440001",
    "avatar": "",
    "isActive": true
  }
}
```

**Business rule — `role` field-i `roleId`-dən avtomatik təyin edilir:**

| Role xüsusiyyəti | Nəticə `role` |
|---|---|
| `isSystem=true` | `ADMIN` |
| `kitchen.view` + `kitchen.manage` icazələri var | `CHEF` |
| Yuxarıdakılar yoxdur | `WAITER` |

---

### `PUT /api/user-ms/v1/users/{id}`

**İstifadəçini redaktə et.**

Headers: `Authorization: Bearer {token}`

Request:
```json
{
  "name": "Leyla H.",
  "username": "waiter1",
  "password": "yeniParol123",
  "roleId": "550e8400-e29b-41d4-a716-446655440031",
  "phone": "+994501112233",
  "isActive": true
}
```

> `password` göndərilməsə, köhnə şifrə qalır. Bütün field-lar optionaldır (partial update). Keycloak-da da məlumatlar yenilənir.

Success (200):
```json
{
  "success": true,
  "message": "User updated",
  "errorCode": null,
  "data": { "...User..." }
}
```

---

### `DELETE /api/user-ms/v1/users/{id}`

**İstifadəçini sil. Keycloak-da da user deaktiv edilir.**

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "User deleted",
  "errorCode": null,
  "data": null
}
```

---

### `GET /api/user-ms/v1/users/staff-performance`

**Personal performans statistikası.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440010",
      "name": "Leyla Hüseynova",
      "role": "WAITER",
      "totalOrders": 25,
      "completedOrders": 20,
      "revenue": 450.00,
      "activeOrders": 3
    }
  ]
}
```

> `role` dəyəri enum formatındadır (`WAITER`, `CHEF`). Frontend öz `t('role.waiter')` tərcüməsini istifadə edir.

---

## 4. Role — `role-service` (port 8104)

> API prefix: `/api/role-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`

### `GET /api/role-ms/v1/roles`

**Bütün rollar.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440020",
      "name": "Süper Admin",
      "permissions": [
        "dashboard.view", "menu.view", "menu.create", "menu.edit", "menu.delete",
        "tables.view", "tables.manage", "tables.status",
        "orders.view", "orders.manage", "orders.cancel",
        "reports.view",
        "staff.view", "staff.create", "staff.edit", "staff.delete",
        "roles.view", "roles.create", "roles.edit", "roles.delete",
        "kitchen.view", "kitchen.manage",
        "settings.view", "settings.edit"
      ],
      "isSystem": true,
      "orgId": null
    }
  ]
}
```

---

### `GET /api/role-ms/v1/roles/{id}`

**Tək rol.**

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440020",
    "name": "Süper Admin",
    "permissions": [
      "dashboard.view", "menu.view", "menu.create", "menu.edit", "menu.delete",
      "tables.view", "tables.manage", "tables.status",
      "orders.view", "orders.manage", "orders.cancel",
      "reports.view",
      "staff.view", "staff.create", "staff.edit", "staff.delete",
      "roles.view", "roles.create", "roles.edit", "roles.delete",
      "kitchen.view", "kitchen.manage",
      "settings.view", "settings.edit"
    ],
    "isSystem": true,
    "orgId": null
  }
}
```

Error (404):
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Role not found",
  "instance": "trace:xxx",
  "key": "ROLE_MS_3001",
  "path": "/api/role-ms/v1/roles/r99",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `POST /api/role-ms/v1/roles`

**Yeni rol yarat.**

Headers: `Authorization: Bearer {token}`

Request:
```json
{
  "name": "Menecer",
  "permissions": [
    "dashboard.view", "menu.view", "menu.create", "menu.edit",
    "tables.view", "tables.manage",
    "orders.view", "orders.manage"
  ],
  "orgId": "550e8400-e29b-41d4-a716-446655440001"
}
```

> `isSystem` həmişə `false` olur. `orgId` məcburidir.

Success (201):
```json
{
  "success": true,
  "message": "Role created",
  "errorCode": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440032",
    "name": "Menecer",
    "permissions": ["dashboard.view", "menu.view", "menu.create", "menu.edit", "tables.view", "tables.manage", "orders.view", "orders.manage"],
    "isSystem": false,
    "orgId": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

---

### `PUT /api/role-ms/v1/roles/{id}`

**Rol redaktə et.**

Headers: `Authorization: Bearer {token}`

Request:
```json
{
  "name": "Menecer+",
  "permissions": [
    "dashboard.view", "menu.view", "menu.create", "menu.edit", "menu.delete",
    "tables.view", "tables.manage", "tables.status",
    "orders.view", "orders.manage"
  ]
}
```

> `isSystem=true` olan rollar redaktə edilə bilməz.

Success (200):
```json
{
  "success": true,
  "message": "Role updated",
  "errorCode": null,
  "data": { "...Role..." }
}
```

Error (403):
```json
{
  "type": "about:blank",
  "title": "Access Denied",
  "status": 403,
  "detail": "System role cannot be modified",
  "instance": "trace:xxx",
  "key": "ROLE_MS_4003",
  "path": "/api/role-ms/v1/roles/r1",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `DELETE /api/role-ms/v1/roles/{id}`

**Rol sil.**

Headers: `Authorization: Bearer {token}`

**Business rules:**
- `isSystem=true` olan rollar silinə bilməz → 403
- Rola aid istifadəçilər varsa, onların `roleId`-si `null` olur

Success (200):
```json
{
  "success": true,
  "message": "Role deleted",
  "errorCode": null,
  "data": null
}
```

---

### `GET /api/role-ms/v1/roles/permissions`

**Bütün mövcud icazələrin siyahısı (frontend checkbox list üçün).**

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "groups": {
      "dashboard": ["dashboard.view"],
      "menu": ["menu.view", "menu.create", "menu.edit", "menu.delete"],
      "tables": ["tables.view", "tables.manage", "tables.status"],
      "orders": ["orders.view", "orders.manage", "orders.cancel"],
      "reports": ["reports.view"],
      "staff": ["staff.view", "staff.create", "staff.edit", "staff.delete"],
      "roles": ["roles.view", "roles.create", "roles.edit", "roles.delete"],
      "kitchen": ["kitchen.view", "kitchen.manage"],
      "settings": ["settings.view", "settings.edit"]
    }
  }
}
```

---

## 5. Menu — `menu-service` (port 8105)

> API prefix: `/api/menu-ms/v1/...`
> Gateway: `http://localhost:8001` → `/api/menu-ms/...` (bütün sorğular gateway-dən keçir)
> Auth: bütün endpoint-lər `Authorization: Bearer {token}` tələb edir (yalnız statik şəkil GET-i publicdir)
> Response: `ApiResponse<T>` wrapper → `{ success, message, errorCode, data }`
> Error: Spring `ProblemDetail` (RFC 9457) → `key`, `path`, `timestamp`, bəzi hallarda `fieldErrors`

### Tenant & Giriş Qaydaları

- Hər kateqoriya və menu maddəsi bir `orgId`-yə aiddir.
- Adi istifadəçi yalnız öz `organizationId`-nə aid data oxuya/yaza bilər:
  - **Read** (GET): sorğudakı `orgId` principal-in org-u ilə uyğun olmalıdır, əks halda `403 MENU_MS_3003`.
  - **Write** (POST/PUT/DELETE): hədəf entity-nin `orgId`-si principal-in org-u ilə uyğun olmalıdır.
  - Create zamanı `orgId` client tərəfindən "seçilə" bilməz — adi user üçün token-dəki org ilə uyğun gəlməzsə 403 qayıdır (servis həmişə token-dəki org-u əsas götürür).
- **SUPER_ADMIN** (platform admin) bütün org-lara tam girişə malikdir; create zamanı istədiyi `orgId`-ni verə bilər.
- Internal microservice çağrıları (`X-Internal-Auth` header-i ilə) bütün data-ya oxuya bilər.

### Lokalizasiya (`LocalizedString`)

`name` və `description` sahələri JSON obyektidir, 3 dil açarı var:
```json
{
  "az": "Pomidor Şorbası",
  "en": "Tomato Soup",
  "ru": "Томатный суп"
}
```

| Açar | Məcburi | İzah |
|---|---|---|
| `az` | Bəli (name üçün) | Boş ola bilməz |
| `en` | X | Opsional, boş ola bilər |
| `ru` | X | Opsional, boş ola bilər |

Ümumi qaydalar:
- Null character (`\u0000`) qəti qadağandır.
- Uzunluq limiti aşılırsa 400 + `fieldErrors` qayıdır.
- Dəyərlər servis tərəfində trim olunur.

### Menu-servis Error Kodları

| HTTP | `key` | Səbəb |
|---|---|---|
| 400 | `MENU_MS_1000` | Validation failed (DTO/field) |
| 400 | `MENU_MS_1001` | JSON parse error |
| 400 | `MENU_MS_3004` | Kateqoriya silərkən `moveItemsTo` özü ilə eyni id-dir |
| 401 | `COMMON_4001` | Token yoxdur / etibarsız |
| 403 | `COMMON_4003` | Security layer tərəfindən qadağan |
| 403 | `MENU_MS_3003` | Başqa org-un datasına giriş cəhdi / icazəsiz əməliyyat |
| 404 | `MENU_MS_3001` | Kateqoriya tapılmadı (silinib və ya mövcud deyil) |
| 404 | `MENU_MS_3002` | Menu maddəsi tapılmadı (silinib və ya mövcud deyil) |
| 500 | `MENU_MS_9999` | Daxili xəta |

> Qeyd: `MENU_MS_3002` kodu "menu item not found" üçündür. (Common `METHOD_NOT_ALLOWED` kodu da `3002`-dir, amma menu-servis 405 qaytarmır.)

> **Soft-delete:** silinən entity `deleted` bayrağı ilə işarələnir və GET-lərdə geri qayıtmır.

---

### Data Modelləri

**MenuItem**

| Field | Tip | Qeyd |
|---|---|---|
| `id` | UUID | |
| `name` | LocalizedString | |
| `description` | LocalizedString \| null | |
| `price` | BigDecimal | |
| `categoryId` | UUID | |
| `imageUrl` | String \| null | Şəklin tam URL-i (public) |
| `isAvailable` | Boolean | |
| `preparationTime` | Integer \| null | dəqiqə ilə |
| `orgId` | UUID | |
| `createdAt` | String (ISO-8601) | məs. `2026-07-25T12:00:00.000Z` |

**MenuCategory**

| Field | Tip | Qeyd |
|---|---|---|
| `id` | UUID | |
| `name` | LocalizedString | |
| `icon` | String \| null | Lucide React ikon adı (məs. `soup`, `beef`, `salad`, `pizza`, `hamburger`, `cup-soda`, `cake`, `cookie`) |
| `sortOrder` | Integer \| null | Menyuda sıralama |
| `orgId` | UUID | |

---

### `GET /api/menu-ms/v1/items`

**Menu maddələrinin siyahısı** (filterlərlə).

Headers: `Authorization: Bearer {token}`

Query:

| Parametr | Tip | Məcburi | İzah |
|---|---|---|---|
| `orgId` | UUID | Bəli (real user) | Org filter; verilməsə boş list qayıdır |
| `categoryId` | UUID | X | Kateqoriya filter |
| `available` | Boolean | X | `true` = yalnız aktiv, `false` = yalnız qeyri-aktiv |

> Real istifadəçi üçün `orgId` öz org-u ilə uyğun olmalıdır (əks halda 403). SUPER_ADMIN istənilən `orgId` verə bilər.

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440040",
      "name": { "az": "Pomidor Şorbası", "en": "Tomato Soup", "ru": "Томатный суп" },
      "description": { "az": "Klassik pomidor şorbası, krem ilə", "en": "Classic tomato soup with cream", "ru": "Классический томатный суп со сливками" },
      "price": 8.00,
      "categoryId": "550e8400-e29b-41d4-a716-446655440050",
      "imageUrl": "http://localhost:8001/api/menu-ms/v1/images/550e8400-e29b-41d4-a716-446655440040.jpg",
      "isAvailable": true,
      "preparationTime": 10,
      "orgId": "550e8400-e29b-41d4-a716-446655440001",
      "createdAt": "2026-07-25T12:00:00.000Z"
    }
  ]
}
```

Error (403) — başqa org-a giriş cəhdi:
```json
{
  "type": "about:blank",
  "title": "Access Denied",
  "status": 403,
  "detail": "Access is denied",
  "instance": "trace:xxx",
  "key": "MENU_MS_3003",
  "path": "/api/menu-ms/v1/items",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `GET /api/menu-ms/v1/items/{id}`

**Tək menu maddəsi.**

Headers: `Authorization: Bearer {token}`

Success (200): *yuxarıdakı kimi tək element*

Error (404):
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Menu item not found",
  "instance": "trace:xxx",
  "key": "MENU_MS_3002",
  "path": "/api/menu-ms/v1/items/550e8400-e29b-41d4-a716-446655440040",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

Error (403): item başqa org-a aiddirsə → `MENU_MS_3003`

---

### `POST /api/menu-ms/v1/items`

**Yeni menu maddəsi yarat.**

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "name": { "az": "Pomidor Şorbası", "en": "Tomato Soup", "ru": "Томатный суп" },
  "description": { "az": "Klassik pomidor şorbası, krem ilə", "en": "Classic tomato soup with cream", "ru": "Классический томатный суп со сливками" },
  "price": 8.00,
  "categoryId": "550e8400-e29b-41d4-a716-446655440050",
  "preparationTime": 10,
  "isAvailable": true,
  "imageUrl": "https://cdn.example.com/images/soup.jpg",
  "orgId": "550e8400-e29b-41d4-a716-446655440001"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `name` | ✅ | `LocalizedString`; `az` mütləq, hər dil maks 100 simvol; null char qadağan |
| `description` | ✗ | `LocalizedString`; hər dil maks 500 simvol |
| `price` | ✅ | `> 0`; maks 8 tam + 2 kəsr rəqəm (maks `99999999.99`) |
| `categoryId` | ✅ | UUID; kateqoriya **eyni org-da** olmalıdır (yoxdursa 404 `MENU_MS_3001`, başqa org-dadırsa 403 `MENU_MS_3003`) |
| `preparationTime` | ✗ | `0..10080` (dəqiqə) |
| `isAvailable` | ✗ | Boolean; default `true` |
| `imageUrl` | ✗ | Maks 512 simvol; **`http(s)://...` və ya `/` ilə başlayan relative path** olmalıdır; control char qadağan. **Base64 `data:` URL qəbul OLUNMUR.** Boş string `""` → `null` saxlanılır |
| `orgId` | ✅ | UUID; adi user üçün token-dəki org ilə uyğun olmalıdır (403); SUPER_ADMIN istədiyini verə bilər |

> **imageUrl qaydası:** şəkil ya bu modulun upload endpoint-i ilə yüklənir (cavabda tam URL), ya da istənilən xarici `http(s)` link verilir (Google Drive, CDN və s.). `data:` base64 URL-lər rədd edilir.

Success (201):
```json
{
  "success": true,
  "message": "Menu item created",
  "errorCode": null,
  "data": { "id": "...", "name": { ... }, "price": 8.00, "...": "..." }
}
```

Error (400) — validation:
```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "trace:xxx",
  "key": "MENU_MS_1000",
  "path": "/api/menu-ms/v1/items",
  "timestamp": "2026-07-30T12:00:00.000Z",
  "fieldErrors": [
    { "field": "name", "message": "Value must be provided for locale 'az'" },
    { "field": "price", "message": "must be greater than 0" },
    { "field": "imageUrl", "message": "imageUrl must be a valid http(s) URL or a relative path" }
  ]
}
```

---

### `PUT /api/menu-ms/v1/items/{id}`

**Menu maddəsini redaktə et (partial update).**

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "name": { "az": "Pomidor Şorbası", "en": "Tomato Soup", "ru": "Томатный суп" },
  "description": { "az": "...", "en": "...", "ru": "..." },
  "price": 9.00,
  "categoryId": "550e8400-e29b-41d4-a716-446655440051",
  "preparationTime": 12,
  "isAvailable": false,
  "imageUrl": "https://cdn.example.com/images/soup2.jpg"
}
```

- **Bütün field-lar optionaldır** — göndərilməyən (və ya `null`) field dəyişmir.
- `name`/`description` `null` ilə **silinə bilməz** (`null` = "dəyişmə").
- **Şəkli təmizləmək üçün** `imageUrl: ""` göndər → DB-də `null` olur.
- `categoryId` dəyişərsə, yeni kateqoriya eyni org-da olmalıdır.
- Validasiyalar `POST /items` ilə eynidir (yalnız göndərilən field-lar üçün).

Success (200):
```json
{
  "success": true,
  "message": "Menu item updated",
  "errorCode": null,
  "data": { "...MenuItem..." }
}
```

---

### `DELETE /api/menu-ms/v1/items/{id}`

**Menu maddəsini sil (soft delete).**

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "Menu item deleted",
  "errorCode": null,
  "data": null
}
```

---

### `POST /api/menu-ms/v1/items/{id}/image`

**Şəkil yüklə (multipart).** Köhnə şəkil avtomatik silinir, yenisi əvəz edir.

Headers: `Authorization: Bearer {token}`

Request: `Content-Type: multipart/form-data`, field adı: `file`

Validasiyalar:
- Maks fayl ölçüsü: **2MB**
- Yalnız **JPEG / PNG / WebP** — tip **faylın faktiki başlanğıc baytlarından (magic bytes)** təyin olunur, `Content-Type` header-i nəzərə alınmır. Başqa format (o cümlədən SVG) → 400.
- Fayl adı server tərəfindən `{itemId}.{ext}` kimi qurulur (user input yoxdur).

Success (200):
```json
{
  "success": true,
  "message": "Image uploaded",
  "errorCode": null,
  "data": {
    "imageUrl": "http://localhost:8001/api/menu-ms/v1/images/550e8400-e29b-41d4-a716-446655440040.jpg"
  }
}
```

> Qaytarılan `imageUrl` birbaşa `<img src>` kimi istifadə oluna bilər — **public GET, auth tələb etmir** (həm gateway, həm menu-service səviyyəsində). Şəkli silmək üçün `DELETE /items/{id}/image`, yalnız URL-i təmizləmək üçün isə `PUT /items/{id}` ilə `imageUrl: ""` göndərilir.

Error (400) — yanlış tip / boş fayl / 2MB-dan böyük:
```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "trace:xxx",
  "key": "MENU_MS_1000",
  "path": "/api/menu-ms/v1/items/550e8400-e29b-41d4-a716-446655440040/image",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `DELETE /api/menu-ms/v1/items/{id}/image`

**Item-ın şəklini sil.** Fayl diskdən silinir və `imageUrl` `null` olur.

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "Image deleted",
  "errorCode": null,
  "data": null
}
```

---

### `GET /api/menu-ms/v1/categories`

**Bütün kateqoriyalar (`sortOrder`-a görə artan sırada).**

Headers: `Authorization: Bearer {token}`

Query:

| Parametr | Tip | Məcburi | İzah |
|---|---|---|---|
| `orgId` | UUID | ✅ | Org filter; başqa org-a baxış → 403 `MENU_MS_3003` |

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440050",
      "name": { "az": "Şorbalar", "en": "Soups", "ru": "Супы" },
      "icon": "soup",
      "sortOrder": 1,
      "orgId": "550e8400-e29b-41d4-a716-446655440001"
    }
  ]
}
```

> `icon` field-ı Lucide React ikon adıdır (bu siyahı ilə məhdud deyil): `soup`, `beef`, `salad`, `pizza`, `hamburger`, `cup-soda`, `cake`, `cookie`.

---

### `GET /api/menu-ms/v1/categories/{id}`

**Tək kateqoriya.**

Headers: `Authorization: Bearer {token}`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440050",
    "name": { "az": "Şorbalar", "en": "Soups", "ru": "Супы" },
    "icon": "soup",
    "sortOrder": 1,
    "orgId": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

Error (404): `MENU_MS_3001` (yuxarıdakı format ilə, `title: "Not Found"`)
Error (403): başqa org-a aiddirsə → `MENU_MS_3003`

---

### `POST /api/menu-ms/v1/categories`

**Yeni kateqoriya yarat.**

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "name": { "az": "Şorbalar", "en": "Soups", "ru": "Супы" },
  "icon": "soup",
  "sortOrder": 1,
  "orgId": "550e8400-e29b-41d4-a716-446655440001"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `name` | ✅ | `LocalizedString`; `az` mütləq, maks 100 simvol |
| `icon` | ✗ | Maks 50 simvol; control char qadağan; boş string `""` → `null` |
| `sortOrder` | ✗ | `0..10000` |
| `orgId` | ✅ | UUID; adi user üçün token-dəki org ilə uyğun (403); SUPER_ADMIN istədiyini verə bilər |

Success (201):
```json
{
  "success": true,
  "message": "Category created",
  "errorCode": null,
  "data": { "...MenuCategory..." }
}
```

---

### `PUT /api/menu-ms/v1/categories/{id}`

**Kateqoriyanı redaktə et (partial update).**

Headers: `Authorization: Bearer {token}`

Request body (bütün field-lar optional):
```json
{
  "name": { "az": "Soyuq Şorbalar", "en": "Cold Soups", "ru": "Холодные супы" },
  "icon": "soup",
  "sortOrder": 2
}
```

Success (200):
```json
{
  "success": true,
  "message": "Category updated",
  "errorCode": null,
  "data": { "...MenuCategory..." }
}
```

---

### `DELETE /api/menu-ms/v1/categories/{id}`

**Kateqoriyanı sil.**

Headers: `Authorization: Bearer {token}`

Request body (opsional):
```json
{
  "moveItemsTo": "550e8400-e29b-41d4-a716-446655440051"
}
```

**Business rules:**
- `moveItemsTo` verilərsə → kateqoriyadakı **bütün maddələr** həmin kateqoriyaya köçürülür, sonra kateqoriya silinir.
  - `moveItemsTo == {id}` (özünə köçürmə) → **400 `MENU_MS_3004`**
  - Hədəf kateqoriya mövcud olmalıdır (404 `MENU_MS_3001`) və **eyni org-da** olmalıdır (403 `MENU_MS_3003`)
- `moveItemsTo` verilmirsə → kateqoriyadakı **bütün maddələr də silinir** (soft delete).

Success (200):
```json
{
  "success": true,
  "message": "Category deleted",
  "errorCode": null,
  "data": null
}
```

---

## 6. Table — `table-service` (port 8106)

> API prefix: `/api/table-ms/v1/...`
> Gateway: `http://localhost:8001` → `/api/table-ms/...` (bütün sorğular gateway-dən keçir)
> Auth: bütün endpoint-lər `Authorization: Bearer {token}` tələb edir (public endpoint yoxdur)
> Response: `ApiResponse<T>` wrapper → `{ success, message, errorCode, data }`
> Error: Spring `ProblemDetail` (RFC 9457) → `key`, `path`, `timestamp`, bəzi hallarda `fieldErrors`

### Tenant & Giriş Qaydaları

- Hər masa (`restaurant_tables`) və bölmə (`sections`) bir `orgId`-yə aiddir.
- Adi istifadəçi yalnız öz `organizationId`-nə aid data oxuya/yaza bilər:
  - **Read** (GET): sorğudakı `orgId` principal-in org-u ilə uyğun olmalıdır, əks halda `403 TABLE_MS_3003`.
  - **Write** (POST/PUT/DELETE): hədəf entity-nin `orgId`-si principal-in org-u ilə uyğun olmalıdır.
  - Create zamanı `orgId` client tərəfindən "seçilə" bilməz — adi user üçün token-dəki org ilə uyğun gəlməzsə 403 qayıdır (servis həmişə token-dəki org-u əsas götürür).
- **SUPER_ADMIN** (platform admin) bütün org-lara tam girişə malikdir; create zamanı istədiyi `orgId`-ni verə bilər.
- Internal microservice çağrıları (`X-Internal-Auth` header-i ilə) bütün data-ya oxuya bilər.
- `{id}` ilə işləyən endpoint-lər (GET/PUT/DELETE) entity-ni əvvəlcə tapır, sonra org-u yoxlayır. Başqa org-un entity `id`-si göndərsən → `404` yox, `403 TABLE_MS_3003` qayıdır (məlumatın mövcudluğu belə "sızdırılmır").

### Status Maşını

Masalar `AVAILABLE | OCCUPIED | RESERVED | CLEANING` statuslarından birindədir. Status keçidləri yalnız aşağıdakı qaydada mümkündür:

| Cari \\ Hədəf | `AVAILABLE` | `OCCUPIED` | `RESERVED` | `CLEANING` |
|---|---|---|---|---|
| `AVAILABLE` | ✓ | ✓ | ✗ | ✓ |
| `OCCUPIED` | ✓ | ✓ | ✗ | ✓ |
| `RESERVED` | ✗* | ✓ | ✗ | ✗ |
| `CLEANING` | ✓ | ✗ | ✗ | ✓ |

> `✗*` — status endpoint-i ilə qadağandır; `RESERVED` → `AVAILABLE` yalnız `DELETE /tables/{id}/reservation` vasitəsilə baş verir.

Qaydalar:
- **`RESERVED` statusu yalnız rezervasiya endpoint-ləri ilə idarə olunur** — `PUT /tables/{id}/reservation` (qoyur) və `DELETE /tables/{id}/reservation` (sıfırlayır). Status endpoint-i ilə `status: RESERVED` göndərmək → `409 TABLE_MS_2003`.
- **`OCCUPIED`-ə keçid üçün `currentOrderId` mütləqdir** — göndərilməzsə → `400 TABLE_MS_4003`. Masa artıq `OCCUPIED`-dirsə və yeni `currentOrderId` verilməyibsə, mövcud order qorunur.
- **`OCCUPIED`-dan çıxanda** (→ `AVAILABLE`/`CLEANING`) `currentOrderId` avtomatik `null` olur.
- **`RESERVED` → `OCCUPIED`** keçidində rezervasiya avtomatik təmizlənir (qonaq gəlib oturdu).
- `OCCUPIED` masaya rezervasiya qoymaq/rezervasiyanı silmək olmaz → `409 TABLE_MS_2004`.
- Yeni masa yaradılanda status həmişə `AVAILABLE` olur.

### Table-servis Error Kodları

| HTTP | `key` | Səbəb |
|---|---|---|
| 400 | `TABLE_MS_1000` | Validation failed (DTO/field) |
| 400 | `TABLE_MS_1001` | JSON parse error |
| 400 | `TABLE_MS_4001` | `status` query param-i yanlışdır (filter-də) |
| 400 | `TABLE_MS_4003` | `OCCUPIED`-ə keçiddə `currentOrderId` verilməyib |
| 401 | `COMMON_4001` | Token yoxdur / etibarsız |
| 403 | `COMMON_4003` | Security layer tərəfindən qadağan |
| 403 | `TABLE_MS_3003` | Başqa org-un datasına giriş cəhdi / icazəsiz əməliyyat |
| 404 | `TABLE_MS_3001` | Masa tapılmadı (silinib və ya mövcud deyil) |
| 404 | `TABLE_MS_3002` | Bölmə tapılmadı (silinib və ya mövcud deyil) |
| 409 | `TABLE_MS_2001` | Masada aktiv sifariş var → silmək olmaz |
| 409 | `TABLE_MS_2002` | Tək qalan bölmə → silmək olmaz |
| 409 | `TABLE_MS_2003` | Status keçidi qadağandır (RESERVED üçün rezerv endpoint-i istifadə edin) |
| 409 | `TABLE_MS_2004` | Masa OCCUPIED-dir → rezervasiya əməliyyatı qadağandır |
| 409 | `TABLE_MS_2005` | Masanın gələcəkdə rezervasiyası var → silmək olmaz |
| 409 | `TABLE_MS_3004` | Bu stol nömrəsi artıq org-da mövcuddur |
| 409 | `TABLE_MS_3005` | Bu bölmə adı artıq org-da mövcuddur |
| 409 | `TABLE_MS_4002` | Qonaq sayı masanın tutumundan çoxdur |
| 409 | `TABLE_MS_2006` | DB səviyyəsində başqa uyğunsuzluq (unikallıq indeksi və s.) |
| 500 | `TABLE_MS_9999` | Daxili xəta |

> **Soft-delete:** silinən entity `deleted` bayrağı ilə işarələnir və GET-lərdə geri qayıtmır. Silinmiş masanın stol nömrəsi və silinmiş bölmənin adı yenidən istifadə oluna bilər.

> **Unikallıq DB səviyyəsində də qorunur:** `(org_id, LOWER(name))` və `(org_id, table_number)` üçün partial unique indekslər mövcuddur. Eyni anda göndərilən iki eyni sorğudan biri də `409` alır.

---

### Data Modelləri

**RestaurantTable** (masa cavabı)

| Field | Tip | Qeyd |
|---|---|---|
| `id` | UUID | |
| `tableNumber` | Integer | Org daxilində unikal |
| `capacity` | Integer | Qonaq sayı |
| `status` | String | `AVAILABLE` \| `OCCUPIED` \| `RESERVED` \| `CLEANING` |
| `sectionId` | UUID \| null | Aid olduğu bölmə |
| `currentOrderId` | UUID \| null | Aktiv sifariş (yalnız `OCCUPIED`-də olur) |
| `reservation` | TableReservation \| null | Aktiv rezervasiya |
| `orgId` | UUID | |

**Section** (bölmə)

| Field | Tip | Qeyd |
|---|---|---|
| `id` | UUID | |
| `name` | String | Org daxilində unikal (case-insensitive) |
| `orgId` | UUID | |

**TableReservation** (`reservation` obyekti)

| Field | Tip | Qeyd |
|---|---|---|
| `guestName` | String | |
| `phone` | String | |
| `time` | String (ISO-8601) | məs. `2026-07-30T19:00:00.000Z` |
| `guestCount` | Integer | Masanın tutumundan böyük ola bilməz |
| `notes` | String \| null | |

---

### `GET /api/table-ms/v1/tables`

**Masaların siyahısı** (filterlərlə).

Headers: `Authorization: Bearer {token}`

Query:

| Parametr | Tip | Məcburi | İzah |
|---|---|---|---|
| `orgId` | UUID | ✅ | Org filter; başqa org-a baxış → 403 `TABLE_MS_3003` |
| `sectionId` | UUID | ✗ | Bölmə filter |
| `status` | String | ✗ | Status filter; dəyərlər case-insensitive-dir (`available` də olar): `AVAILABLE`, `OCCUPIED`, `RESERVED`, `CLEANING`. Yanlış dəyər → 400 `TABLE_MS_4001` |

> Filterlər birlikdə də işləyir (`orgId`+`sectionId`+`status`).

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440060",
      "tableNumber": 1,
      "capacity": 2,
      "status": "AVAILABLE",
      "sectionId": "550e8400-e29b-41d4-a716-446655440070",
      "currentOrderId": null,
      "reservation": null,
      "orgId": "550e8400-e29b-41d4-a716-446655440001"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440061",
      "tableNumber": 2,
      "capacity": 2,
      "status": "OCCUPIED",
      "sectionId": "550e8400-e29b-41d4-a716-446655440070",
      "currentOrderId": "550e8400-e29b-41d4-a716-446655440080",
      "reservation": null,
      "orgId": "550e8400-e29b-41d4-a716-446655440001"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440062",
      "tableNumber": 4,
      "capacity": 4,
      "status": "RESERVED",
      "sectionId": "550e8400-e29b-41d4-a716-446655440070",
      "currentOrderId": null,
      "reservation": {
        "guestName": "Əli Həsənov",
        "phone": "994501234567",
        "time": "2026-07-30T19:00:00.000Z",
        "guestCount": 4,
        "notes": "Ad gününə həsr olunub"
      },
      "orgId": "550e8400-e29b-41d4-a716-446655440001"
    }
  ]
}
```

Error (403) — başqa org-a giriş cəhdi:
```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "You do not have permission to access this resource",
  "instance": "trace:xxx",
  "key": "TABLE_MS_3003",
  "path": "/api/table-ms/v1/tables",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `GET /api/table-ms/v1/tables/{id}`

**Tək masa.**

Headers: `Authorization: Bearer {token}`

Success (200): *yuxarıdakı kimi tək element*

Error (404):
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Table not found",
  "instance": "trace:xxx",
  "key": "TABLE_MS_3001",
  "path": "/api/table-ms/v1/tables/550e8400-e29b-41d4-a716-446655440060",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

Error (403): masa başqa org-a aiddirsə → `TABLE_MS_3003`

---

### `POST /api/table-ms/v1/tables`

**Yeni masa yarat.**

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "tableNumber": 11,
  "capacity": 4,
  "sectionId": "550e8400-e29b-41d4-a716-446655440070",
  "orgId": "550e8400-e29b-41d4-a716-446655440001"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `tableNumber` | ✅ | `1..9999`; **org daxilində unikal** → təkrarda 409 `TABLE_MS_3004` |
| `capacity` | ✅ | `1..500` |
| `sectionId` | ✗ | UUID; bölmə **eyni org-da** olmalıdır (yoxdursa 404 `TABLE_MS_3002`, başqa org-dadırsa 403 `TABLE_MS_3003`) |
| `orgId` | ✅ | UUID; adi user üçün token-dəki org ilə uyğun olmalıdır (403); SUPER_ADMIN istədiyini verə bilər |

> `status` həmişə `AVAILABLE` olaraq yaradılır (request-dən qəbul edilmir).

Success (201):
```json
{
  "success": true,
  "message": "Table created",
  "errorCode": null,
  "data": { "...RestaurantTable..." }
}
```

Error (409) — stol nömrəsi təkrardır:
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "A table with this number already exists in this organization",
  "instance": "trace:xxx",
  "key": "TABLE_MS_3004",
  "path": "/api/table-ms/v1/tables",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `PUT /api/table-ms/v1/tables/{id}`

**Masanı redaktə et (partial update).**

Headers: `Authorization: Bearer {token}`

Request body (bütün field-lar optional):
```json
{
  "tableNumber": 11,
  "capacity": 6,
  "sectionId": "550e8400-e29b-41d4-a716-446655440071",
  "status": "AVAILABLE"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `tableNumber` | ✗ | `1..9999`; unikal → təkrarda 409 `TABLE_MS_3004` |
| `capacity` | ✗ | `1..500`; masanın **aktiv rezervasiyasının qonaq sayından aşağı** endirilə bilməz → 409 `TABLE_MS_4002` |
| `sectionId` | ✗ | UUID; yeni bölmə eyni org-da olmalıdır |
| `status` | ✗ | Status maşınına tabedir (yuxarıdakı cədvəl). `OCCUPIED` yalnız masanın artıq `currentOrderId`-si varsa mümkündür — yeni order ilə masanı tutmaq üçün `PUT /tables/{id}/status` istifadə edin (400 `TABLE_MS_4003` istisnası) |

Success (200):
```json
{
  "success": true,
  "message": "Table updated",
  "errorCode": null,
  "data": { "...RestaurantTable..." }
}
```

---

### `DELETE /api/table-ms/v1/tables/{id}`

**Masanı sil (soft delete).**

Headers: `Authorization: Bearer {token}`

**Business rules:**
- Aktiv sifarişi olan masa silinə bilməz → `409 TABLE_MS_2001`.
- **Gələcəkdə rezervasiyası olan masa silinə bilməz** → `409 TABLE_MS_2005` (rezervasiya vaxtı keçibsə silmək olar).
- Silinmiş masanın stol nömrəsi yenidən istifadə oluna bilər.

Error (409) — aktiv sifariş:
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Table has an active order and cannot be deleted",
  "instance": "trace:xxx",
  "key": "TABLE_MS_2001",
  "path": "/api/table-ms/v1/tables/550e8400-e29b-41d4-a716-446655440060",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

Error (409) — gələcəkdə rezervasiya:
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Table has an upcoming reservation and cannot be deleted",
  "instance": "trace:xxx",
  "key": "TABLE_MS_2005",
  "path": "/api/table-ms/v1/tables/550e8400-e29b-41d4-a716-446655440060",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

Success (200):
```json
{
  "success": true,
  "message": "Table deleted",
  "errorCode": null,
  "data": null
}
```

---

### `PUT /api/table-ms/v1/tables/{id}/status`

**Masasının statusunu dəyiş** (rezervasiya xaric).

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "status": "CLEANING",
  "currentOrderId": "550e8400-e29b-41d4-a716-446655440080"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `status` | ✅ | `AVAILABLE` \| `OCCUPIED` \| `RESERVED` \| `CLEANING` (case-insensitive). `RESERVED` bu endpoint ilə QƏBUL EDİLMİR → 409 `TABLE_MS_2003`. Yanlış dəyər → 400 `TABLE_MS_1000` |
| `currentOrderId` | ✗ | **`OCCUPIED`-ə keçid üçün mütləqdir** (yoxdursa və masanın mövcud order-i də yoxdursa → 400 `TABLE_MS_4003`). Masa artıq `OCCUPIED`-dirsə və verilməyibsə, mövcud order qorunur |

**Status maşını** yuxarıdakı cədvələ tabedir. Keçid qadağandırsa → `409 TABLE_MS_2003`.

Success (200):
```json
{
  "success": true,
  "message": "Table status updated",
  "errorCode": null,
  "data": { "...RestaurantTable..." }
}
```

Error (409) — qadağan keçid (masa AVAILABLE, hədəf RESERVED):
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Invalid table status transition. To manage reservations use the reservation endpoints",
  "instance": "trace:xxx",
  "key": "TABLE_MS_2003",
  "path": "/api/table-ms/v1/tables/550e8400-e29b-41d4-a716-446655440060/status",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `PUT /api/table-ms/v1/tables/{id}/reservation`

**Rezervasiya əlavə et / yenilə.** Status avtomatik `RESERVED` olur.

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "guestName": "Əli Həsənov",
  "phone": "+994501234567",
  "time": "2026-07-30T19:00:00.000Z",
  "guestCount": 4,
  "notes": "Ad gününə həsr olunub"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `guestName` | ✅ | Maks 100 simvol; control char (`\u0000` və s.) qadağan; trim olunur |
| `phone` | ✅ | **Qlobal telefon formatı** (bütün servislərdə ortaq `@ValidPhone`): yalnız `0-9`, `+ - ( ) .` və boşluq; 7–15 rəqəm olmalıdır; maks 30 simvol. Saxlanarkən yalnız rəqəmlərə normalizasiya olunur (`+994 50 123 45 67` → `994501234567`) |
| `time` | ✅ | ISO-8601; **gələcək zaman** olmalıdır (keçmiş → 400 `TABLE_MS_1000`) |
| `guestCount` | ✅ | `1..100`; masanın tutumundan böyük ola bilməz → 409 `TABLE_MS_4002` |
| `notes` | ✗ | Maks 500 simvol; control char qadağan; trim olunur |

**Business rules:**
- `OCCUPIED` masaya rezervasiya qoymaq olmaz → `409 TABLE_MS_2004`.
- `AVAILABLE`, `CLEANING` və ya artıq `RESERVED` masada rezervasiya qoyula/yenilənə bilər.

Success (200):
```json
{
  "success": true,
  "message": "Reservation updated",
  "errorCode": null,
  "data": { "...RestaurantTable (status: RESERVED)..." }
}
```

Error (409) — qonaq sayı tutumdan çox:
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Number of guests exceeds the table capacity",
  "instance": "trace:xxx",
  "key": "TABLE_MS_4002",
  "path": "/api/table-ms/v1/tables/550e8400-e29b-41d4-a716-446655440060/reservation",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `DELETE /api/table-ms/v1/tables/{id}/reservation`

**Rezervasiyanı sil.**

Headers: `Authorization: Bearer {token}`

**Business rules:**
- `OCCUPIED` masada rezervasiya silmək olmaz → `409 TABLE_MS_2004`.
- Rezervasiya `null` olur. Masa `RESERVED` idisə status `AVAILABLE` olur; başqa statusda (məs. `CLEANING`) status dəyişmir.

Success (200):
```json
{
  "success": true,
  "message": "Reservation cancelled",
  "errorCode": null,
  "data": { "...RestaurantTable (reservation: null)..." }
}
```

---

### `GET /api/table-ms/v1/sections`

**Bölmələrin siyahısı** (yaradılma tarixinə görə).

Headers: `Authorization: Bearer {token}`

Query:

| Parametr | Tip | Məcburi | İzah |
|---|---|---|---|
| `orgId` | UUID | ✅ | Org filter; başqa org-a baxış → 403 `TABLE_MS_3003` |

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440070",
      "name": "Zal 1",
      "orgId": "550e8400-e29b-41d4-a716-446655440001"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440071",
      "name": "Zal 2",
      "orgId": "550e8400-e29b-41d4-a716-446655440001"
    }
  ]
}
```

---

### `POST /api/table-ms/v1/sections`

**Yeni bölmə yarat.**

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "name": "Bağ evi",
  "orgId": "550e8400-e29b-41d4-a716-446655440001"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `name` | ✅ | Boş ola bilməz; maks 100 simvol; control char qadağan; trim olunur; **org daxilində unikal (case-insensitive)** → təkrarda 409 `TABLE_MS_3005` |
| `orgId` | ✅ | UUID; adi user üçün token-dəki org ilə uyğun olmalıdır (403); SUPER_ADMIN istədiyini verə bilər |

Success (201):
```json
{
  "success": true,
  "message": "Section created",
  "errorCode": null,
  "data": { "...Section..." }
}
```

Error (409) — eyni adlı bölmə:
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "A section with this name already exists in this organization",
  "instance": "trace:xxx",
  "key": "TABLE_MS_3005",
  "path": "/api/table-ms/v1/sections",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `PUT /api/table-ms/v1/sections/{id}`

**Bölmənin adını dəyiş.**

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "name": "Bağ Evi"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `name` | ✅ | Boş ola bilməz; maks 100 simvol; control char qadağan; trim olunur; **org daxilində unikal (case-insensitive)** → təkrarda 409 `TABLE_MS_3005` |

Success (200):
```json
{
  "success": true,
  "message": "Section renamed",
  "errorCode": null,
  "data": { "...Section..." }
}
```

---

### `DELETE /api/table-ms/v1/sections/{id}`

**Bölməni sil (soft delete).**

Headers: `Authorization: Bearer {token}`

**Business rules:**
- Bölmədəki bütün masalar avtomatik **qalan ilk bölməyə** köçürülür.
- Org-da **tək bölmə qalıbsa silmək olmaz** → `409 TABLE_MS_2002`.
- Silinmiş bölmənin adı yenidən istifadə oluna bilər.

Error (409) — son bölmə:
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Cannot delete the last section",
  "instance": "trace:xxx",
  "key": "TABLE_MS_2002",
  "path": "/api/table-ms/v1/sections/550e8400-e29b-41d4-a716-446655440070",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

Success (200):
```json
{
  "success": true,
  "message": "Section deleted",
  "errorCode": null,
  "data": null
}
```

---

## 7. Order — `order-service` (port 8107)

> API prefix: `/api/order-ms/v1/...`
> Gateway: `http://localhost:8001` → `/api/order-ms/...` (bütün sorğular gateway-dən keçir)
> Auth: bütün endpoint-lər `Authorization: Bearer {token}` tələb edir (public endpoint yoxdur)
> Response: `ApiResponse<T>` wrapper → `{ success, message, errorCode, data }`
> Error: Spring `ProblemDetail` (RFC 9457) → `key`, `path`, `timestamp`, bəzi hallarda `fieldErrors`

### Tenant & Giriş Qaydaları

- Hər sifariş və sifariş maddəsi bir `orgId`-yə aiddir.
- **Read** (GET): `orgId` query parametri məcburidir; bütün filterlər həmin org daxilində tətbiq olunur. `orgId` verilməzsə → 400 `ORDER_MS_1003`.
- **Write**: `orgId` request body-də məcburidir. Sifarişin maddələri yalnız həmin org-un menyusundan (menu-servis), masası isə yalnız həmin org-a aid masalardan (table-servis) seçilə bilər — hər iki yoxlama aidiyyatı servisin səviyyəsində aparılır.
- **SUPER_ADMIN** (platform admin) bütün org-lara tam girişə malikdir.
- Internal microservice çağrıları (`X-Internal-Auth` header-i ilə) bütün data-ya oxuya bilər.

> ⚠️ **Qeyd (tenant yoxlaması yarımçıqdır):** menu-ms və table-ms-dən fərqli olaraq order-ms hələ tam tenant yoxlaması tətbiq etmir — `GET /orders/{id}` kimi id-əsaslı endpoint-lərdə sifarişin `orgId`-si principal ilə yoxlanılmır, `orgId` əsasən filter kimi istifadə olunur. Servis table-ms/waiter-ms kimi bərkidilməli, `ORDER_MS_3003` kodu əlavə edilməlidir (yol xəritəsində).

### Sifariş Status Maşını (Lifecycle)

`/status` endpoint-i ilə yalnız irəli keçidlər mümkündür (geriyə keçid yoxdur):

```
PENDING → CONFIRMED → PREPARING → READY → SERVED → COMPLETED
```

Qaydalar:
- `PENDING` → `CONFIRMED`; `CONFIRMED` → `PREPARING`; `PREPARING` → `READY`; `READY` → `SERVED`; `SERVED` → `COMPLETED`.
- Eyni statusa keçid də qadağandır (məs. `CONFIRMED` → `CONFIRMED`) → 400 `ORDER_MS_4001`.
- `CANCELLED` `/status` endpoint-i ilə **QƏBUL EDİLMİR** → 400 `ORDER_MS_4009`. Ləğv üçün mütləq `POST /orders/{id}/cancel` istifadə olunur.
- **Ləğv edilə bilən statuslar:** `PENDING`, `CONFIRMED`, `PREPARING`, `READY`. `SERVED`-dən sonra ləğv mümkün deyil → 400 `ORDER_MS_4009`.
- `COMPLETED` və `CANCELLED` terminal statuslardır — heç bir keçid yoxdur.
- `COMPLETED` olmaq üçün sifariş `SERVED` olmalıdır (yoxdursa 400 `ORDER_MS_4001`); ödəniş `PAID` olanda sifariş avtomatik `COMPLETED` olur.

**Maddə (item) status maşını:**

| Cari \\ Hədəf | `PREPARING` | `READY` | `SERVED` | `CANCELLED` |
|---|---|---|---|---|
| `PENDING` | ✓ | ✗ | ✗ | ✓ |
| `CONFIRMED` | ✓ | ✗ | ✗ | ✓ |
| `PREPARING` | ✗ | ✓ | ✗ | ✓ |
| `READY` | ✗ | ✗ | ✓ | ✓ |
| `SERVED` | ✗ | ✗ | ✗ | ✗ |
| `CANCELLED` | ✗ | ✗ | ✗ | ✗ |

- Eyni status yenidən göndərilə bilər (idempotent); digər qadağan keçid → 400 `ORDER_MS_4007`.
- Maddə statusu dəyişəndə sifariş statusu avtomatik yenilənə bilər:
  - Bütün aktiv maddələr `READY`/`SERVED` və sifariş `PREPARING` → sifariş `READY`
  - Bütün aktiv maddələr `SERVED` və sifariş `READY` → sifariş `SERVED`

### Order-servis Error Kodları

| HTTP | `key` | Səbəb |
|---|---|---|
| 400 | `ORDER_MS_1000` | Validation failed (DTO/field) / yanlış enum dəyəri (`status`, `method` və s.) |
| 400 | `ORDER_MS_1001` | JSON parse error |
| 400 | `ORDER_MS_1003` | Parametr tipi yanlış (məs. UUID olmayan `{id}`) / məcburi parametr verilməyib |
| 401 | `COMMON_4001` | Token yoxdur / etibarsız |
| 403 | `COMMON_4003` | Security layer tərəfindən qadağan |
| 404 | `ORDER_MS_3001` | Sifariş tapılmadı (silinib və ya mövcud deyil) |
| 404 | `ORDER_MS_4006` | Sifariş maddəsi tapılmadı (bu sifarişə aid deyil) |
| 400 | `ORDER_MS_4001` | Sifariş status keçidi qadağandır |
| 400 | `ORDER_MS_4004` | Yalnız `PENDING` sifariş təsdiqlənə bilər (waiter-confirm) |
| 400 | `ORDER_MS_4005` | Tamamlanmış/ləğv olunmuş sifarişə maddə əlavə etmək olmaz |
| 400 | `ORDER_MS_4007` | Maddə status keçidi qadağandır |
| 409 | `ORDER_MS_4008` | Ödəniş artıq tamamlanıb (`PAID`) |
| 400 | `ORDER_MS_4009` | Sifariş cari statusda ləğv oluna bilməz |
| 400 | `ORDER_MS_4011` | Masa `AVAILABLE` deyil (boş deyil) |
| 400 | `ORDER_MS_4012` | Menu maddəsi org-un menyusunda yoxdur |
| 400 | `ORDER_MS_4013` | Menu maddəsi qeyri-aktivdir (`isAvailable=false`) |
| 500 | `ORDER_MS_9999` | Daxili xəta |

> `ORDER_MS_4002` (completed order ləğvi) və `ORDER_MS_4003` (paid order ləğvi) enum-da mövcuddur, amma hazırki kodda istifadə edilmir (ləğv qadağası `ORDER_MS_4009` ilə əhatə olunur).
> Masa mövcud deyilsə, table-servis öz xətasını (məs. `TABLE_MS_3001` 404) qaytarır və o, olduğu kimi ötürülür.

---

### Data Modelləri

**Order** (sifariş cavabı)

| Field | Tip | Qeyd |
|---|---|---|
| `id` | String (UUID) | |
| `tableId` | UUID | |
| `tableNumber` | Integer | Masa yaradılanda table-servisdən götürülür |
| `items` | List\<OrderItem\> | |
| `status` | String | `PENDING` \| `CONFIRMED` \| `PREPARING` \| `READY` \| `SERVED` \| `COMPLETED` \| `CANCELLED` |
| `paymentStatus` | String | `PENDING` \| `PAID` |
| `totalAmount` | BigDecimal | Σ (price × quantity) |
| `waiterId` | UUID \| null | |
| `waiterName` | String \| null | |
| `orderSource` | String | `WAITER` \| `CUSTOMER` |
| `waiterConfirmed` | Boolean | |
| `confirmedBy` | String \| null | Ofisant təsdiqi (`waiter-confirm` ilə) |
| `customerPhoto` | String \| null | |
| `paymentMethod` | String \| null | `CASH` \| `CARD` |
| `paymentRequested` | Boolean | |
| `cancelReason` | String \| null | |
| `orgId` | UUID | |
| `createdAt` | String (ISO-8601) | məs. `2026-07-30T14:30:00Z` |
| `updatedAt` | String (ISO-8601) | |

**OrderItem** (maddə)

| Field | Tip | Qeyd |
|---|---|---|
| `id` | String (UUID) | |
| `menuItemId` | UUID | |
| `menuItemName` | String | Yaradılma anında request-dən alınır |
| `quantity` | Integer | |
| `price` | BigDecimal | |
| `notes` | String \| null | |
| `status` | String | `PENDING` \| `CONFIRMED` \| `PREPARING` \| `READY` \| `SERVED` \| `CANCELLED` |

---

### `GET /api/order-ms/v1/orders`

**Sifariş siyahısı** (filterlərlə).

Headers: `Authorization: Bearer {token}`

Query:

| Parametr | Tip | Məcburi | İzah |
|---|---|---|---|
| `orgId` | UUID | ✅ | Org filter; verilməzsə → 400 `ORDER_MS_1003` |
| `status` | String | ✗ | Status filter (case-insensitive): `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `SERVED`, `COMPLETED`, `CANCELLED`. Yanlış dəyər → 400 `ORDER_MS_1000` |
| `tableId` | UUID | ✗ | Masa filter |
| `waiterId` | UUID | ✗ | Ofisant filter |

> **Filterlərin işləmə qaydası:** `status`+`tableId` birlikdə işləyir; `status`+`waiterId` birlikdə verilərsə `waiterId` nəzərə alınmır (status əsas götürülür). `waiterId` tək verilərsə həmin ofisantın sifarişləri qayıdır.

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440080",
      "tableId": "550e8400-e29b-41d4-a716-446655440061",
      "tableNumber": 2,
      "items": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440090",
          "menuItemId": "550e8400-e29b-41d4-a716-446655440040",
          "menuItemName": "Pomidor Şorbası",
          "quantity": 2,
          "price": 8.00,
          "notes": "",
          "status": "READY"
        }
      ],
      "status": "CONFIRMED",
      "paymentStatus": "PENDING",
      "totalAmount": 16.00,
      "waiterId": "550e8400-e29b-41d4-a716-446655440010",
      "waiterName": "Leyla Hüseynova",
      "orderSource": "WAITER",
      "waiterConfirmed": true,
      "confirmedBy": null,
      "customerPhoto": null,
      "paymentMethod": null,
      "paymentRequested": false,
      "cancelReason": null,
      "orgId": "550e8400-e29b-41d4-a716-446655440001",
      "createdAt": "2026-07-30T18:30:00.000Z",
      "updatedAt": "2026-07-30T18:32:00.000Z"
    }
  ]
}
```

Error (400) — yanlış `status` dəyəri:
```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "trace:xxx",
  "key": "ORDER_MS_1000",
  "path": "/api/order-ms/v1/orders",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `GET /api/order-ms/v1/orders/{id}`

**Tək sifariş.**

Headers: `Authorization: Bearer {token}`

Success (200): *yuxarıdakı kimi tək element*

Error (404):
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Order not found",
  "instance": "trace:xxx",
  "key": "ORDER_MS_3001",
  "path": "/api/order-ms/v1/orders/550e8400-e29b-41d4-a716-446655440080",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `POST /api/order-ms/v1/orders`

**Yeni sifariş yarat.** (Ofisant paneli və müştəri axını üçün; müştəri axını adətən customer-servis vasitəsilə internal çağrılır.)

Headers: `Authorization: Bearer {token}`

**Request (waiter):**
```json
{
  "orgId": "550e8400-e29b-41d4-a716-446655440001",
  "tableId": "550e8400-e29b-41d4-a716-446655440061",
  "waiterId": "550e8400-e29b-41d4-a716-446655440010",
  "waiterName": "Leyla Hüseynova",
  "orderSource": "WAITER",
  "items": [
    { "menuItemId": "550e8400-e29b-41d4-a716-446655440040", "menuItemName": "Pomidor Şorbası", "quantity": 2, "price": 8.00, "notes": "" },
    { "menuItemId": "550e8400-e29b-41d4-a716-446655440041", "menuItemName": "Lülə Kebab", "quantity": 1, "price": 28.00, "notes": "Az bişmiş" }
  ]
}
```

**Request (customer):**
```json
{
  "orgId": "550e8400-e29b-41d4-a716-446655440001",
  "tableId": "550e8400-e29b-41d4-a716-446655440063",
  "orderSource": "CUSTOMER",
  "items": [
    { "menuItemId": "550e8400-e29b-41d4-a716-446655440042", "menuItemName": "Margarita Pizza", "quantity": 1, "price": 18.00, "notes": "" }
  ],
  "customerPhoto": "data:image/jpeg;base64,...",
  "paymentMethod": "CASH"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `orgId` | ✅ | UUID |
| `tableId` | ✅ | UUID; masa **eyni org-da** və `AVAILABLE` olmalıdır → deyilsə 400 `ORDER_MS_4011` |
| `orderSource` | ✅ | `WAITER` \| `CUSTOMER` (case-insensitive) |
| `items` | ✅ | Ən azı 1 maddə; hər maddədə: `menuItemId` (UUID, org menyusunda olmalıdır → 400 `ORDER_MS_4012`, `isAvailable=true` olmalıdır → 400 `ORDER_MS_4013`), `menuItemName` (non-blank), `quantity` (required integer), `price` (required decimal), `notes` (opsional) |
| `waiterId` | ✗ | UUID; WAITER axını üçün |
| `waiterName` | ✗ | WAITER axını üçün |
| `customerPhoto` | ✗ | CUSTOMER axını üçün |
| `paymentMethod` | ✗ | `CASH` \| `CARD`; CUSTOMER axını üçün ilkin ödəniş metodu |

**Status assignment rules (backend):**

| orderSource | orderMode (org setting) | waiterConfirmed | status |
|---|---|---|---|
| `WAITER` | — | `true` | `CONFIRMED` |
| `CUSTOMER` | `CUSTOMER` | `true` | `CONFIRMED` |
| `CUSTOMER` | `CUSTOMER_WAITER_CONFIRM` | `false` | `PENDING` |

**PaymentStatus rules (create):**
- Org setting `paymentTiming=BEFORE` → `PAID`
- Org setting `paymentTiming=AFTER` → `PENDING`

**Business rules:**
- Masa `AVAILABLE` deyilsə → 400 `ORDER_MS_4011`; masa mövcud deyilsə table-servis 404-ü ötürülür (`TABLE_MS_3001`).
- Bütün maddələr org-un menyusunda olmalı və `isAvailable=true` olmalıdır.
- Yaradılanda masa `OCCUPIED` olur və masanın `currentOrderId`-si set edilir (table-servis çağrılır).
- Yeni maddələrin statusu `PENDING`, `paymentRequested=false` olur; `totalAmount` hesablanır.

Success (201):
```json
{
  "success": true,
  "message": "Order created",
  "errorCode": null,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440080",
    "tableId": "550e8400-e29b-41d4-a716-446655440061",
    "tableNumber": 2,
    "items": [
      { "id": "550e8400-e29b-41d4-a716-446655440081", "menuItemId": "550e8400-e29b-41d4-a716-446655440040", "menuItemName": "Pomidor Şorbası", "quantity": 2, "price": 8.00, "notes": "", "status": "PENDING" },
      { "id": "550e8400-e29b-41d4-a716-446655440082", "menuItemId": "550e8400-e29b-41d4-a716-446655440041", "menuItemName": "Lülə Kebab", "quantity": 1, "price": 28.00, "notes": "Az bişmiş", "status": "PENDING" }
    ],
    "status": "CONFIRMED",
    "paymentStatus": "PENDING",
    "totalAmount": 44.00,
    "waiterId": "550e8400-e29b-41d4-a716-446655440010",
    "waiterName": "Leyla Hüseynova",
    "orderSource": "WAITER",
    "waiterConfirmed": true,
    "confirmedBy": null,
    "customerPhoto": null,
    "paymentMethod": null,
    "paymentRequested": false,
    "cancelReason": null,
    "orgId": "550e8400-e29b-41d4-a716-446655440001",
    "createdAt": "2026-07-30T14:30:00.000Z",
    "updatedAt": "2026-07-30T14:30:00.000Z"
  }
}
```

> Müştəri axınında `orderMode=CUSTOMER_WAITER_CONFIRM` olan org-da status `PENDING`, `waiterConfirmed=false` qayıdır — təsdiq üçün `PUT /orders/{id}/waiter-confirm` istifadə olunur.

Error (400) — masa boş deyil:
```json
{
  "type": "about:blank",
  "title": "Table Not Available",
  "status": 400,
  "detail": "Table is not available",
  "instance": "trace:xxx",
  "key": "ORDER_MS_4011",
  "path": "/api/order-ms/v1/orders",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `PUT /api/order-ms/v1/orders/{id}/status`

**Sifariş statusunu dəyiş** (status maşınına tabedir).

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "status": "PREPARING"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `status` | ✅ | `PENDING` \| `CONFIRMED` \| `PREPARING` \| `READY` \| `SERVED` \| `COMPLETED` (case-insensitive). `CANCELLED` bu endpoint ilə **QƏBUL EDİLMİR** → 400 `ORDER_MS_4009` (bunun üçün `/cancel` var). Yanlış dəyər → 400 `ORDER_MS_1000` |

**Status maşını** yuxarıdakı cədvələ tabedir; qadağan keçid → 400 `ORDER_MS_4001`.

Success (200):
```json
{
  "success": true,
  "message": "Order status updated",
  "errorCode": null,
  "data": { "...Order (status: PREPARING)..." }
}
```

Error (400) — qadağan keçid (məs. `PENDING` → `READY`):
```json
{
  "type": "about:blank",
  "title": "Invalid Status",
  "status": 400,
  "detail": "Invalid order status transition",
  "instance": "trace:xxx",
  "key": "ORDER_MS_4001",
  "path": "/api/order-ms/v1/orders/550e8400-e29b-41d4-a716-446655440080/status",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `PUT /api/order-ms/v1/orders/{id}/items/{itemId}/status`

**Tək maddənin statusunu dəyiş** (maddə status maşınına tabedir).

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "status": "PREPARING"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `status` | ✅ | Maddə maşınındakı hədəflərdən biri: `PREPARING` \| `READY` \| `SERVED` \| `CANCELLED` (case-insensitive). Eyni status yenidən göndərilə bilər. Qadağan keçid → 400 `ORDER_MS_4007` |

**Business rules:**
- Maddə bu sifarişə aid olmalıdır (aid deyilsə → 404 `ORDER_MS_4006`).
- Yeniləmədən sonra sifariş statusu avtomatik hesablanır (bax: Maddə status maşını → avtomatik yenilənmə).

Success (200):
```json
{
  "success": true,
  "message": "Item status updated",
  "errorCode": null,
  "data": { "...Order (həmin maddənin statusu yenilənib)..." }
}
```

Error (404) — maddə tapılmadı / bu sifarişə aid deyil:
```json
{
  "type": "about:blank",
  "title": "Item Not Found",
  "status": 404,
  "detail": "Order item not found",
  "instance": "trace:xxx",
  "key": "ORDER_MS_4006",
  "path": "/api/order-ms/v1/orders/550e8400-e29b-41d4-a716-446655440080/items/550e8400-e29b-41d4-a716-446655440090/status",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `POST /api/order-ms/v1/orders/{id}/items`

**Mövcud sifarişə yeni maddələr əlavə et.**

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "items": [
    { "menuItemId": "550e8400-e29b-41d4-a716-446655440043", "menuItemName": "Cola", "quantity": 2, "price": 4.00, "notes": "" }
  ]
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `items` | ✅ | Ən azı 1 maddə; validasiyalar `POST /orders` ilə eynidir (`menuItemId`, `menuItemName`, `quantity`, `price`, `notes`) |

**Business rules:**
- Sifariş `COMPLETED` və ya `CANCELLED` statusundadırsa → 400 `ORDER_MS_4005`.
- Yeni maddələrin statusu `PENDING` olur; `totalAmount` yenidən hesablanır.

Success (200):
```json
{
  "success": true,
  "message": "Items added to order",
  "errorCode": null,
  "data": { "...Order (yeni maddələr əlavə olunub, totalAmount yenilənib)..." }
}
```

Error (400) — tamamlanmış sifarişə əlavə:
```json
{
  "type": "about:blank",
  "title": "Not Active",
  "status": 400,
  "detail": "Cannot modify a completed or cancelled order",
  "instance": "trace:xxx",
  "key": "ORDER_MS_4005",
  "path": "/api/order-ms/v1/orders/550e8400-e29b-41d4-a716-446655440080/items",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `PUT /api/order-ms/v1/orders/{id}/waiter-confirm`

**Ofisant müştəri sifarişini təsdiqləyir** (`orderMode=CUSTOMER_WAITER_CONFIRM` olan sifarişlər üçün).

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "waiterId": "550e8400-e29b-41d4-a716-446655440010",
  "waiterName": "Leyla Hüseynova"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `waiterId` | ✅ | UUID |
| `waiterName` | ✅ | Non-blank |

**Business rules:**
- Sifariş statusu `PENDING` olmalıdır → deyilsə 400 `ORDER_MS_4004`.
- Sifariş `orderSource=CUSTOMER` olmalıdır → deyilsə 400 `ORDER_MS_4001`.

Result: `waiterConfirmed=true`, `confirmedBy=waiterName`, `waiterId`/`waiterName` yenilənir, status `CONFIRMED` olur.

Success (200):
```json
{
  "success": true,
  "message": "Order confirmed",
  "errorCode": null,
  "data": { "...Order (status: CONFIRMED, waiterConfirmed: true)..." }
}
```

Error (400) — sifariş PENDING deyil:
```json
{
  "type": "about:blank",
  "title": "Not Pending",
  "status": 400,
  "detail": "Only pending orders can be confirmed",
  "instance": "trace:xxx",
  "key": "ORDER_MS_4004",
  "path": "/api/order-ms/v1/orders/550e8400-e29b-41d4-a716-446655440080/waiter-confirm",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `POST /api/order-ms/v1/orders/{id}/cancel`

**Sifarişi ləğv et.**

Headers: `Authorization: Bearer {token}`

Request body (opsional):
```json
{
  "reason": "Müştəri imtina etdi"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `reason` | ✗ | Opsional (uzunluq limiti servis tərəfindən tətbiq edilmir) |

**Business rules:**
- Yalnız `PENDING`, `CONFIRMED`, `PREPARING`, `READY` statuslarından ləğv oluna bilər → başqasından 400 `ORDER_MS_4009`.
- Ləğvdən sonra masa statusu `CLEANING` olur və masanın `currentOrderId`-si təmizlənir (table-servis çağrılır).

Success (200):
```json
{
  "success": true,
  "message": "Order cancelled",
  "errorCode": null,
  "data": { "...Order (status: CANCELLED, cancelReason set)..." }
}
```

Error (400) — cari statusdan ləğv mümkün deyil:
```json
{
  "type": "about:blank",
  "title": "Not Cancellable",
  "status": 400,
  "detail": "This order cannot be cancelled in its current state",
  "instance": "trace:xxx",
  "key": "ORDER_MS_4009",
  "path": "/api/order-ms/v1/orders/550e8400-e29b-41d4-a716-446655440080/cancel",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `POST /api/order-ms/v1/orders/{id}/request-payment`

**Ödəniş tələbi** (müştəri və ya ofisant). `paymentRequested=true` olur, `paymentMethod` set edilir.

Headers: `Authorization: Bearer {token}`

Request body:
```json
{
  "method": "CASH"
}
```

| Field | Məcburi | Validasiya |
|---|---|---|
| `method` | ✅ | `CASH` \| `CARD` (case-insensitive). Yanlış dəyər → 400 `ORDER_MS_1000` |

> `paymentStatus` bu əməliyyatda dəyişmir (`PENDING` qalır) — `PAID` yalnız `complete-payment` ilə olur.

Success (200):
```json
{
  "success": true,
  "message": "Payment requested",
  "errorCode": null,
  "data": { "...Order (paymentRequested: true, paymentMethod: CASH)..." }
}
```

---

### `POST /api/order-ms/v1/orders/{id}/complete-payment`

**Ödənişi qəbul et** (ofisant).

Headers: `Authorization: Bearer {token}`

Request body yoxdur.

**Business rules:**
- Sifariş statusu `SERVED` olmalıdır → deyilsə 400 `ORDER_MS_4001`.
- Sifariş artıq `PAID`-dırsa → 409 `ORDER_MS_4008`.

Result: `paymentStatus=PAID`, sifariş statusu `COMPLETED`, masa `AVAILABLE` olur (table-servis çağrılır).

Success (200):
```json
{
  "success": true,
  "message": "Payment completed",
  "errorCode": null,
  "data": { "...Order (paymentStatus: PAID, status: COMPLETED)..." }
}
```

Error (409) — ödəniş artıq tamamlanıb:
```json
{
  "type": "about:blank",
  "title": "Payment Completed",
  "status": 409,
  "detail": "Payment has already been completed for this order",
  "instance": "trace:xxx",
  "key": "ORDER_MS_4008",
  "path": "/api/order-ms/v1/orders/550e8400-e29b-41d4-a716-446655440080/complete-payment",
  "timestamp": "2026-07-30T12:00:00.000Z"
}
```

---

### `POST /api/order-ms/v1/orders/{id}/start-preparing`

**Bütün gözləyən maddələri `PREPARING` et.** Order status-u `PREPARING` olur.

Headers: `Authorization: Bearer {token}`

Request body yoxdur.

**Business rules:**
- Sifariş `CONFIRMED` və ya `PENDING` statusunda olmalıdır → deyilsə 400 `ORDER_MS_4001`.
- `PENDING`/`CONFIRMED` maddələr `PREPARING` olur; `READY`/`SERVED`/`CANCELLED` maddələr dəyişmir.

Success (200):
```json
{
  "success": true,
  "message": "Order is now being prepared",
  "errorCode": null,
  "data": { "...Order (status: PREPARING)..." }
}
```

---

### `POST /api/order-ms/v1/orders/{id}/mark-all-ready`

**Bütün maddələri `READY` et.** Order status-u `READY` olur.

Headers: `Authorization: Bearer {token}`

Request body yoxdur.

**Business rules:**
- `PREPARING` maddələr `READY` olur; digər maddələr dəyişmir.

Success (200):
```json
{
  "success": true,
  "message": "All items are ready",
  "errorCode": null,
  "data": { "...Order (status: READY)..." }
}
```

---

## 8. Kitchen — `kitchen-service` (port 8108)

> API prefix: `/api/kitchen-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> **Order əməliyyatları** (`start-preparing`, `mark-all-ready`) `order-service`-də yerləşir (yuxarıya bax)

### `GET /api/kitchen-ms/v1/orders`

**Mətbəx üçün sifarişlər, qruplaşdırılmış.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "new": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440080",
        "items": [
          { "id": "uuid", "menuItemId": "uuid", "menuItemName": "Pomidor Şorbası", "quantity": 2, "price": 8.00, "notes": "", "status": "PENDING" }
        ],
        "tableId": "uuid", "tableNumber": 2, "status": "PENDING",
        "paymentStatus": "PENDING", "totalAmount": 44.00,
        "waiterName": "Leyla Hüseynova", "orderSource": "WAITER",
        "createdAt": "2026-07-30T14:30:00Z"
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440083",
        "items": [
          { "id": "uuid", "menuItemId": "uuid", "menuItemName": "Qızardılmış Balıq", "quantity": 1, "price": 18.00, "notes": "", "status": "CONFIRMED" }
        ],
        "tableId": "uuid", "tableNumber": 5, "status": "CONFIRMED",
        "paymentStatus": "PENDING", "totalAmount": 18.00,
        "waiterName": "Murad Əliyev", "orderSource": "WAITER",
        "createdAt": "2026-07-30T14:35:00Z"
      }
    ],
    "preparing": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440084",
        "items": [
          { "id": "uuid", "menuItemId": "uuid", "menuItemName": "Margarita Pizza", "quantity": 2, "price": 18.00, "notes": "", "status": "PREPARING" }
        ],
        "tableId": "uuid", "tableNumber": 3, "status": "PREPARING",
        "paymentStatus": "PENDING", "totalAmount": 36.00,
        "waiterName": "Leyla Hüseynova", "orderSource": "CUSTOMER",
        "createdAt": "2026-07-30T14:20:00Z"
      }
    ],
    "ready": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440085",
        "items": [
          { "id": "uuid", "menuItemId": "uuid", "menuItemName": "Cola", "quantity": 3, "price": 4.00, "notes": "", "status": "READY" }
        ],
        "tableId": "uuid", "tableNumber": 1, "status": "READY",
        "paymentStatus": "PENDING", "totalAmount": 12.00,
        "waiterName": "Murad Əliyev", "orderSource": "WAITER",
        "createdAt": "2026-07-30T14:10:00Z"
      }
    ]
  }
}
```

**Filter:**
| Group | Status | Başlıq |
|---|---|---|
| `new` | `PENDING`, `CONFIRMED` | Yeni Sifarişlər |
| `preparing` | `PREPARING` | Hazırlanır |
| `ready` | `READY` | Hazırdır |

---

## 9. Waiter — `waiter-service` (port 8109)

> API prefix: `/api/waiter-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> **Not:** Aggregation service — öz DB-si yoxdur, digər servislərdən məlumatları birləşdirir.
> Yalnız oxuma (GET) endpoint-ləri var; yazma əməliyyatı yoxdur.

### Auth & Tenant qaydaları

- Bütün endpoint-lər `Authorization: Bearer {token}` və ya gateway-dən ötürülən identity header-ları (`X-User-Id`, `X-Org-Id`, `X-Roles`, `X-Platform-Admin`, `X-Internal-Auth`) ilə işləyir.
- Gateway müştəridən gələn bütün identity header-larını **silib** JWT-dən yenidən qoyur — kənar istifadəçi bu header-ları saxtalaşdıra bilməz; `X-Internal-Auth` secret-i yalnız gateway bilir.
- `orgId` **tenant yoxlaması**: tələb olunur və autentifikasiya olunmuş istifadəçinin `orgId`-si ilə eyni olmalıdır.
  - Uyğunsuzluq → **403** `WAITER_MS_3003` ACCESS_DENIED.
  - `SUPER_ADMIN` (platform admin) istənilən `orgId`-ni oxuya bilər.
  - Servislərarası (internal, `X-Internal-Auth`) çağrılar keçərlidir.
- Upstream servis xətaları **səssiz boş cavab kimi yutulmur**:
  - table-ms / order-ms əlçatmazdırsa (connect/timeout) → **503** `WAITER_MS_9001` UPSTREAM_UNAVAILABLE
  - cavab formatı keçərsizdirsə (`success=false` və ya null data) → **502** `WAITER_MS_9002` UPSTREAM_ERROR
  - table-ms / order-ms biznes xətası qaytararsa → onun öz error kodu olduğu kimi ötürülür (FeignClientException)
- Nəticələr `createdAt` azalan sıra ilə, ən son **200** qeydlə məhdudlaşdırılır (memory/DoS qoruması).

### Error kodları (WAITER_MS_*)

| Code | HTTP | Mənası |
|------|------|--------|
| WAITER_MS_3003 | 403 | Başqa təşkilatın məlumatına giriş qadağandır |
| WAITER_MS_9001 | 503 | Upstream servis müvəqqəti əlçatmazdır |
| WAITER_MS_9002 | 502 | Upstream servis keçərsiz cavab qaytardı |

### `GET /api/waiter-ms/v1/tables`

**Ofisant paneli üçün masa məlumatları (aktiv sifarişlərlə).**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Davranış:
- Masalar `tableNumber` artan sıra ilə qayıdır.
- `orderSummary` yalnız masanın **cari sifarişindən** (`currentOrderId`) hesablanır — keçmiş/historiya sifarişlər deyil.
- `section` section adıdır; tanınmayan/boş section üçün `""` qayıdır.

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "tables": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440061",
        "tableNumber": 2,
        "capacity": 2,
        "status": "OCCUPIED",
        "section": "Zal 1",
        "currentOrderId": "550e8400-e29b-41d4-a716-446655440080",
        "orderSummary": {
          "totalAmount": 44.00,
          "itemCount": 3,
          "status": "CONFIRMED"
        }
      },
      {
        "id": "550e8400-e29b-41d4-a716-446655440060",
        "tableNumber": 1,
        "capacity": 2,
        "status": "AVAILABLE",
        "section": "Zal 1",
        "currentOrderId": null,
        "orderSummary": null
      }
    ]
  }
}
```

Error: 401 (token yox/keçərsiz), 403 `WAITER_MS_3003`, 503 `WAITER_MS_9001`, 502 `WAITER_MS_9002`.

---

### `GET /api/waiter-ms/v1/orders/pending-confirm`

**Təsdiq gözləyən müştəri sifarişləri.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Filter: `waiterConfirmed=false`, `orderSource=CUSTOMER`, status `PENDING`. `createdAt` azalan sıra, maks 200.

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    { "id": "550e8400-e29b-41d4-a716-446655440080",
    "items": [
      { "id": "uuid", "menuItemId": "uuid", "menuItemName": "Pomidor Şorbası", "quantity": 2, "price": 8.00, "notes": "", "status": "PENDING" }
    ],
    "tableId": "uuid",
    "tableNumber": 2,
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "totalAmount": 44.00,
    "waiterId": null,
    "waiterName": null,
    "orderSource": "CUSTOMER",
    "waiterConfirmed": false,
    "confirmedBy": null,
    "customerPhoto": "https://...",
    "paymentMethod": null,
    "paymentRequested": false,
    "cancelReason": null,
    "orgId": "550e8400-e29b-41d4-a716-446655440001",
    "createdAt": "2026-07-30T14:30:00Z",
    "updatedAt": "2026-07-30T14:30:00Z" }
  ]
}
```

Error: 401, 403 `WAITER_MS_3003`, 503 `WAITER_MS_9001`, 502 `WAITER_MS_9002`.

---

### `GET /api/waiter-ms/v1/orders/payment-requests`

**Ödəniş tələbi gözləyən sifarişlər.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Filter: `paymentRequested=true`, `paymentStatus=PENDING`. `createdAt` azalan sıra, maks 200.

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    { "id": "550e8400-e29b-41d4-a716-446655440080",
    "items": [
      { "id": "uuid", "menuItemId": "uuid", "menuItemName": "Pomidor Şorbası", "quantity": 2, "price": 8.00, "notes": "", "status": "PENDING" }
    ],
    "tableId": "uuid",
    "tableNumber": 2,
    "status": "CONFIRMED",
    "paymentStatus": "PENDING",
    "totalAmount": 44.00,
    "waiterId": "uuid",
    "waiterName": "Leyla Hüseynova",
    "orderSource": "WAITER",
    "waiterConfirmed": true,
    "confirmedBy": "uuid",
    "customerPhoto": null,
    "paymentMethod": "CARD",
    "paymentRequested": true,
    "cancelReason": null,
    "orgId": "550e8400-e29b-41d4-a716-446655440001",
    "createdAt": "2026-07-30T14:30:00Z",
    "updatedAt": "2026-07-30T14:30:00Z" }
  ]
}
```

Error: 401, 403 `WAITER_MS_3003`, 503 `WAITER_MS_9001`, 502 `WAITER_MS_9002`.

---

## 10. Customer — `customer-service` (port 8110)

> API prefix: `/api/customer-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> **Not:** Bu endpoint-lər AUTH TƏLƏB ETMİR. İctimai API-lərdir.

### `GET /api/customer-ms/v1/{orgId}/menu`

**Müştəri menyusu — kateqoriyalar + maddələr (yalnız `isAvailable=true`).**

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "categories": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440050",
        "name": { "az": "Şorbalar", "en": "Soups", "ru": "Супы" },
        "icon": "soup"
      }
    ],
    "items": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440040",
        "name": { "az": "Pomidor Şorbası", "en": "Tomato Soup", "ru": "Томатный суп" },
        "description": { "az": "Klassik pomidor şorbası", "en": "Classic tomato soup", "ru": "Классический томатный суп" },
        "price": 8.00,
        "categoryId": "550e8400-e29b-41d4-a716-446655440050",
        "imageUrl": null,
        "isAvailable": true,
        "preparationTime": 10
      }
    ]
  }
}
```

---

### `GET /api/customer-ms/v1/{orgId}/tables`

**Müştəri üçün boş masalar (yalnız `status=AVAILABLE`).**

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440060",
      "tableNumber": 1,
      "capacity": 2,
      "sectionId": "550e8400-e29b-41d4-a716-446655440070"
    }
  ]
}
```

---

### `POST /api/customer-ms/v1/orders`

**Müştəri sifarişi yarat.**

Request:
```json
{
  "orgId": "550e8400-e29b-41d4-a716-446655440001",
  "tableId": "550e8400-e29b-41d4-a716-446655440063",
  "items": [
    {
      "menuItemId": "550e8400-e29b-41d4-a716-446655440042",
      "menuItemName": "Margarita Pizza",
      "quantity": 1,
      "price": 18.00,
      "notes": ""
    }
  ],
  "customerPhoto": "data:image/jpeg;base64,...",
  "paymentMethod": null
}
```

> `paymentMethod` `paymentTiming=BEFORE` olduqda göndərilir (`CASH` və ya `CARD`), `AFTER` olduqda `null`.

Success (201):
```json
{
  "success": true,
  "message": "Order placed",
  "errorCode": null,
  "data": { "id": "550e8400-e29b-41d4-a716-446655440080",
    "items": [
      { "id": "uuid", "menuItemId": "uuid", "menuItemName": "Pomidor Şorbası", "quantity": 2, "price": 8.00, "notes": "", "status": "PENDING" }
    ],
    "tableId": "uuid",
    "tableNumber": 2,
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "totalAmount": 44.00,
    "waiterId": "uuid",
    "waiterName": "Leyla Hüseynova",
    "orderSource": "WAITER",
    "waiterConfirmed": false,
    "confirmedBy": null,
    "customerPhoto": null,
    "paymentMethod": null,
    "paymentRequested": false,
    "cancelReason": null,
    "orgId": "550e8400-e29b-41d4-a716-446655440001",
    "createdAt": "2026-07-30T14:30:00Z",
    "updatedAt": "2026-07-30T14:30:00Z" }
}
```

---

### `GET /api/customer-ms/v1/orders/{orderId}`

**Sifariş izləmə.**

Headers: `Authorization: Bearer {token}` (opsional)

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": { "id": "550e8400-e29b-41d4-a716-446655440080",
    "items": [
      { "id": "uuid", "menuItemId": "uuid", "menuItemName": "Pomidor Şorbası", "quantity": 2, "price": 8.00, "notes": "", "status": "PENDING" }
    ],
    "tableId": "uuid",
    "tableNumber": 2,
    "status": "PENDING",
    "paymentStatus": "PENDING",
    "totalAmount": 44.00,
    "waiterId": "uuid",
    "waiterName": "Leyla Hüseynova",
    "orderSource": "WAITER",
    "waiterConfirmed": false,
    "confirmedBy": null,
    "customerPhoto": null,
    "paymentMethod": null,
    "paymentRequested": false,
    "cancelReason": null,
    "orgId": "550e8400-e29b-41d4-a716-446655440001",
    "createdAt": "2026-07-30T14:30:00Z",
    "updatedAt": "2026-07-30T14:30:00Z" }
}
```

---

### `POST /api/customer-ms/v1/orders/{orderId}/request-bill`

**Hesab tələbi.**

Request:
```json
{
  "method": "CASH"
}
```

Success (200):
```json
{
  "success": true,
  "message": "Bill requested",
  "errorCode": null,
  "data": null
}
```

---

## 11. Settings — `setting-service` (port 8111)

> API prefix: `/api/setting-ms/v1/...`
> Response: `ApiResponse<T>` wrapper

### `GET /api/setting-ms/v1/settings`

**Təşkilat parametrləri.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "orgId": "550e8400-e29b-41d4-a716-446655440001",
    "orderMode": "CUSTOMER",
    "customerPhotoRequired": false,
    "paymentTiming": "AFTER",
    "customerTheme": "CLASSIC"
  }
}
```

**Enum dəyərləri:**

| Field | Mümkün dəyərlər |
|---|---|
| `orderMode` | `WAITER`, `CUSTOMER`, `CUSTOMER_WAITER_CONFIRM`, `KITCHEN` |
| `paymentTiming` | `BEFORE`, `AFTER` |
| `customerTheme` | `CLASSIC`, `EMERALD`, `SUNSET`, `ROSE`, `VIOLET`, `AMBER` |

---

### `PUT /api/setting-ms/v1/settings`

**Parametrləri yenilə.**

Headers: `Authorization: Bearer {token}`

Request:
```json
{
  "orgId": "550e8400-e29b-41d4-a716-446655440001",
  "orderMode": "CUSTOMER",
  "customerPhotoRequired": true,
  "paymentTiming": "BEFORE",
  "customerTheme": "EMERALD"
}
```

> Bütün field-lar göndərilməlidir (tam yeniləmə).

Success (200):
```json
{
  "success": true,
  "message": "Settings updated",
  "errorCode": null,
  "data": { "...OrgSetting..." }
}
```

---

## 12. Dashboard — `dashboard-service` (port 8112)

> API prefix: `/api/dashboard-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> **Not:** Aggregation service — öz DB-si yoxdur

### `GET /api/dashboard-ms/v1/stats`

**Ümumi statistika.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "totalRevenue": 15240.00,
    "completedOrders": 128,
    "activeOrders": 7,
    "occupiedTables": 4
  }
}
```

---

### `GET /api/dashboard-ms/v1/top-items`

**Ən çox satılan 5 məhsul.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "menuItemId": "550e8400-e29b-41d4-a716-446655440040",
      "name": { "az": "Pomidor Şorbası", "en": "Tomato Soup", "ru": "Томатный суп" },
      "count": 45
    },
    {
      "menuItemId": "550e8400-e29b-41d4-a716-446655440041",
      "name": { "az": "Lülə Kebab", "en": "Lule Kebab", "ru": "Люля-кебаб" },
      "count": 38
    }
  ]
}
```

---

### `GET /api/dashboard-ms/v1/recent-orders`

**Son 6 sifariş.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440080",
      "tableNumber": 2,
      "waiterName": "Leyla Hüseynova",
      "totalAmount": 44.00,
      "status": "COMPLETED",
      "createdAt": "2026-07-30T18:30:00.000Z"
    }
  ]
}
```

---

### `GET /api/dashboard-ms/v1/staff-list`

**Aktiv personal (sifariş sayı ilə).**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440010",
      "name": "Leyla Hüseynova",
      "role": "WAITER",
      "activeOrders": 3
    }
  ]
}
```

---

## 13. Reports — `report-service` (port 8113)

> API prefix: `/api/report-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> **Not:** Aggregation service — öz DB-si yoxdur

### `GET /api/report-ms/v1/summary`

**Xülasə.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "totalRevenue": 15240.00,
    "completed": 128,
    "cancelled": 5,
    "avgOrderValue": 119.06
  }
}
```

---

### `GET /api/report-ms/v1/daily-revenue`

**Son 7 günlük gəlir.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    { "date": "2026-07-24", "revenue": 2150.00, "orderCount": 18 },
    { "date": "2026-07-25", "revenue": 1890.00, "orderCount": 15 },
    { "date": "2026-07-26", "revenue": 2450.00, "orderCount": 22 },
    { "date": "2026-07-27", "revenue": 3120.00, "orderCount": 28 },
    { "date": "2026-07-28", "revenue": 1780.00, "orderCount": 14 },
    { "date": "2026-07-29", "revenue": 1980.00, "orderCount": 16 },
    { "date": "2026-07-30", "revenue": 1870.00, "orderCount": 15 }
  ]
}
```

---

### `GET /api/report-ms/v1/hourly`

**Saatlıq sifariş paylanması (24 saat).**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "hourly": [0, 0, 0, 0, 0, 0, 0, 0, 5, 8, 12, 18, 22, 15, 10, 14, 20, 25, 18, 12, 8, 3, 0, 0]
  }
}
```

> Array indeksi saatı göstərir (0=00:00, 23=23:00).

---

### `GET /api/report-ms/v1/sales-by-category`

**Kateqoriyaya görə satış.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "categoryId": "550e8400-e29b-41d4-a716-446655440050",
      "name": { "az": "Şorbalar", "en": "Soups", "ru": "Супы" },
      "count": 85
    },
    {
      "categoryId": "550e8400-e29b-41d4-a716-446655440051",
      "name": { "az": "Əsas Yeməklər", "en": "Main Courses", "ru": "Основные блюда" },
      "count": 62
    }
  ]
}
```

> `count` = satılan maddələrin ümumi sayı (quantity cəmi).

---

### `GET /api/report-ms/v1/top-items`

**Ən çox satılan 8 məhsul (gəlirlə).**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "menuItemId": "550e8400-e29b-41d4-a716-446655440041",
      "name": { "az": "Lülə Kebab", "en": "Lule Kebab", "ru": "Люля-кебаб" },
      "count": 38,
      "revenue": 1064.00
    }
  ]
}
```

---

### `GET /api/report-ms/v1/staff-performance`

**Personal performansı.**

Headers: `Authorization: Bearer {token}`

Query: `?orgId=550e8400-e29b-41d4-a716-446655440001`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440010",
      "name": "Leyla Hüseynova",
      "role": "WAITER",
      "totalOrders": 45,
      "completedOrders": 40,
      "revenue": 4800.00
    }
  ]
}
```

---

## 14. Microservice Architecture & Port Plan

| Service | Module Name | Port | API Prefix | Purpose |
|---|---|---|---|---|
| **API Gateway** | `cloud-gateway` | 8001 | `/api/...` (route) | Gateway, auth filter, routing |
| **Auth** | `auth-gateway` | 8002 | `/api/auth-ms/v1/auth/` | Login, JWT, refresh (Keycloak proxy) |
| **Organization** | `organization-service` | 8102 | `/api/organization-ms/v1/` | Org CRUD |
| **User / Staff** | `user-service` | 8103 | `/api/user-ms/v1/` | User/staff CRUD, staff perf |
| **Role** | `role-service` | 8104 | `/api/role-ms/v1/` | Role/permission CRUD |
| **Menu** | `menu-service` | 8105 | `/api/menu-ms/v1/` | Menu items & categories |
| **Table** | `table-service` | 8106 | `/api/table-ms/v1/` | Table & section management |
| **Order** | `order-service` | 8107 | `/api/order-ms/v1/` | Order lifecycle, cart |
| **Kitchen** | `kitchen-service` | 8108 | `/api/kitchen-ms/v1/` | Kitchen order views |
| **Waiter** | `waiter-service` | 8109 | `/api/waiter-ms/v1/` | Waiter-specific aggregated data |
| **Customer** | `customer-service` | 8110 | `/api/customer-ms/v1/` | Public menu & order placement |
| **Settings** | `setting-service` | 8111 | `/api/setting-ms/v1/` | Org settings |
| **Dashboard** | `dashboard-service` | 8112 | `/api/dashboard-ms/v1/` | Dashboard aggregates |
| **Reports** | `report-service` | 8113 | `/api/report-ms/v1/` | Report aggregates |

---

## 15. Complete API Route Index (by module)

```
AUTH          (/api/auth-ms/v1/auth/)
  POST          /login
  POST          /refresh
  POST          /logout

ORGANIZATION  (/api/organization-ms/v1/)
  GET           /organizations
  POST          /organizations
  GET           /organizations/{id}
  GET           /organizations/{id}/qr-code

USER          (/api/user-ms/v1/)
  GET           /users
  GET           /users/{id}
  POST          /users
  PUT           /users/{id}
  DELETE        /users/{id}
  GET           /users/staff-performance
  PUT           /users/clear-role?roleId={roleId}

ROLE          (/api/role-ms/v1/)
  GET           /roles
  GET           /roles/{id}
  POST          /roles
  PUT           /roles/{id}
  DELETE        /roles/{id}
  GET           /roles/permissions

MENU          (/api/menu-ms/v1/)
  GET           /items
  GET           /items/{id}
  POST          /items
  PUT           /items/{id}
  DELETE        /items/{id}
  POST          /items/{id}/image
  DELETE        /items/{id}/image
  GET           /categories
  GET           /categories/{id}
  POST          /categories
  PUT           /categories/{id}
  DELETE        /categories/{id}

TABLE         (/api/table-ms/v1/)
  GET           /tables
  GET           /tables/{id}
  POST          /tables
  PUT           /tables/{id}
  DELETE        /tables/{id}
  PUT           /tables/{id}/status
  PUT           /tables/{id}/reservation
  DELETE        /tables/{id}/reservation
  GET           /sections
  POST          /sections
  PUT           /sections/{id}
  DELETE        /sections/{id}

ORDER         (/api/order-ms/v1/)
  GET           /orders
  GET           /orders/{id}
  POST          /orders
  PUT           /orders/{id}/status
  PUT           /orders/{id}/items/{itemId}/status
  POST          /orders/{id}/items
  PUT           /orders/{id}/waiter-confirm
  POST          /orders/{id}/cancel
  POST          /orders/{id}/request-payment
  POST          /orders/{id}/complete-payment
  POST          /orders/{id}/start-preparing
  POST          /orders/{id}/mark-all-ready

KITCHEN       (/api/kitchen-ms/v1/)
  GET           /orders

WAITER        (/api/waiter-ms/v1/)
  GET           /tables
  GET           /orders/pending-confirm
  GET           /orders/payment-requests

CUSTOMER      (/api/customer-ms/v1/)
  GET           /{orgId}/menu
  GET           /{orgId}/tables
  POST          /orders
  GET           /orders/{orderId}
  POST          /orders/{orderId}/request-bill

SETTINGS      (/api/setting-ms/v1/)
  GET           /settings
  PUT           /settings

DASHBOARD     (/api/dashboard-ms/v1/)
  GET           /stats
  GET           /top-items
  GET           /recent-orders
  GET           /staff-list

REPORTS       (/api/report-ms/v1/)
  GET           /summary
  GET           /daily-revenue
  GET           /hourly
  GET           /sales-by-category
  GET           /top-items
  GET           /staff-performance
```

---

## 16. Common Shared Types (for reference)

Hamısı `common-core` modulunda yerləşir:

```java
// === Enums ===
UserRole        { ADMIN, ORG_ADMIN, WAITER, CHEF, CUSTOMER }
TableStatus     { AVAILABLE, OCCUPIED, RESERVED, CLEANING }
OrderStatus     { PENDING, CONFIRMED, PREPARING, READY, SERVED, COMPLETED, CANCELLED }
PaymentStatus   { PENDING, PAID }
PaymentMethod   { CASH, CARD }
OrderMode       { WAITER, CUSTOMER, CUSTOMER_WAITER_CONFIRM, KITCHEN }
OrderSource     { WAITER, CUSTOMER }
CustomerTheme   { CLASSIC, EMERALD, SUNSET, ROSE, VIOLET, AMBER }
PaymentTiming   { BEFORE, AFTER }
UiScope         { ADMIN_PANEL, USER_PANEL }  // auth-gateway-specific

// === JSON Columns (PostgreSQL jsonb) ===
LocalizedString   { az, en, ru }
TableReservation  { guestName, phone, time, guestCount, notes }

// === Common fields (SoftDeletableCoreEntity) ===
id: UUID (PK)
createdAt: Instant
updatedAt: Instant
createdBy: UUID
updatedBy: UUID
isDeleted: boolean
deletedAt: Instant
deletedBy: UUID
orgId: UUID (nullable, multi-tenant)
```

**Permission constants:**
```
dashboard.view, menu.view, menu.create, menu.edit, menu.delete,
tables.view, tables.manage, tables.status,
orders.view, orders.manage, orders.cancel,
reports.view,
staff.view, staff.create, staff.edit, staff.delete,
roles.view, roles.create, roles.edit, roles.delete,
kitchen.view, kitchen.manage,
settings.view, settings.edit
```

---

## 17. Gradle Modules

Bütün modullar hazırdır (`settings.gradle`):

```gradle
include 'auth-gateway'
include 'cloud-gateway'
include 'common-core'
include 'common-jpa'
include 'common-exception-handling'
include 'common-security'
include 'db-migrations'
include 'organization-service'
include 'user-service'
include 'role-service'
include 'menu-service'
include 'table-service'
include 'order-service'
include 'kitchen-service'
include 'waiter-service'
include 'customer-service'
include 'setting-service'
include 'dashboard-service'
include 'report-service'
```
