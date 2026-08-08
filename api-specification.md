# RestoFlow API Specification

> Bu sənəd **front-end** üçün yeganə mənbədir. Hər endpoint-ə görə: autentifikasiya tələbi, permission kodu (`@PreAuthorize`), request/response JSON nümunələri və error kodları verilib.
>
> Bütün dəyişikliklər `implementation-prompts/rbac-prompts.md` faylındakı promptlar (1–9) əsasında kodda tətbiq olunub. Bu fayl kodun hazırkı vəziyyəti ilə 100% uyğundur.

## Ümumi Qaydalar

| Qayda | Dəyər |
|---|---|
| Base URL (API Gateway) | `http://localhost:8001` |
| Auth path | `/api/auth-ms/v1/auth/{action}` — birbaşa DTO, `ApiResponse` wrapper-i yoxdur |
| Digər servislər | `/api/{service-ms}/v1/{resource}` — `ApiResponse<T>` wrapper-i ilə |
| Uğur formatı (auth xaric) | `{ success: true, message: "...", errorCode: null, data: {...} }` |
| Error formatı (bütün servislər) | Spring `ProblemDetail` (RFC 9457) — `key`, `path`, `timestamp`, bəzən `fieldErrors` |
| Validation error | 400 + `fieldErrors` array |
| Auth token | Keycloak JWT access token, header: `Authorization: Bearer {token}` |

### Autentifikasiya və Giriş Nəzarəti (RBAC)

- İstifadəçi **Keycloak** üzərindən `POST /api/auth-ms/v1/auth/login` ilə giriş edir. Cavabda JWT **access token**, **refresh token**, `uiScope` və istifadəçinin **permission** siyahısı gəlir.
- Hər istəkdə front `Authorization: Bearer {accessToken}` göndərir. **cloud-gateway (8001)** JWT-i doğrulayır, içindəki claim-ləri aşağıdakı header-lərə çevirib microservice-ə ötürür (token-ə front-ün ehtiyacı yoxdur — gateway bunu edir):

| Header | Mənbə (claim) | Nümunə |
|---|---|---|
| `X-User-Id` | `sub` | `bbbbbbbb-0000-4000-8000-000000000001` |
| `X-Org-Id` | `organizationId` | `01234567-89ab-cdef-0123-456789abcdef` |
| `X-Roles` | `roles` (CSV) | `SUPER_ADMIN` |
| `X-Permissions` | `permissions` (CSV) | `menu.view,menu.create,order.view,...` |
| `X-UI-Scope` | `uiScope` | `ADMIN_PANEL` |
| `X-Platform-Admin` | `roles` tərkibində `SUPER_ADMIN` olması | `true`/`false` |
| `X-Internal-Auth` | gateway secret (interne) | — |

- Microservice-lər bu header-lərdən `UserPrincipal` qurur; hər endpoint-də `@PreAuthorize("@perm.has('...')")` işləyir.
- **Platform admin bypass**: principal-ın `platformAdmin=true` olduğu halda (`SUPER_ADMIN` rol) bütün permission-lar avtomatik keçərlidir (403 atılmır).
- **Permission mənbəyi sırası**: JWT-də `permissions` claim-i varsa birbaşa ondan yoxlanır; boşdursa `roles` claim-i ilə DB-dəki `role_permissions` cədvəlindən həll edilir.

### Public (auth tələb olunmayan) marşrutlar

Gateway-də aşağıdakılar `permitAll`-dir (token olmadan):

- `POST /api/auth-ms/**` (login, refresh, logout)
- `GET /api/menu-ms/v1/images/**` (menu şəkilləri)
- `GET /api/access-ms/v1/permissions/my` (login zamanı permission oxumaq üçün)
- `/api/customer-ms/**` (customer menu/order — QR ssenarisi)
- `/actuator/**`, `/swagger-ui/**`, `/api/*/v3/api-docs/**`

Qalan hər şey **autentifikasiya tələb edir** (401) və endpoint-ə uyğun permission yoxlanılır (403).

### Search və Pagination (bütün list endpoint-ləri)

| Parametr | Tip | Açıqlama |
|---|---|---|
| `q` | `string` | Axtarış (ad/kod üzrə, case-insensitive) |
| `page` | `int` (default `0`) | Səhifə indeksi (0-dan başlayır) |
| `size` | `int` (default `20`, max `100`) | Səhifə ölçüsü |

Cavab həmişə `PageDto<T>`:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 47,
  "totalPages": 3,
  "first": true,
  "last": false,
  "empty": false
}
```

### Soft-delete davranışı

- Bütün `DELETE` endpoint-ləri **fiziki silmə yox, soft-delete** edir (`is_deleted=true`).
- Uğurlu silmə adətən **204 No Content** qaytarır (bəzi servislərdə `ApiResponse` wrapper ilə 200).
- Silinmiş entity-lər bütün sorğularda görünmür.

### Sistem rolları (SUPER_ADMIN, ORG_ADMIN)

- `SUPER_ADMIN` və `ORG_ADMIN` **`isSystem=true`** olan rollardır, heç bir org-a aid deyil (`org_id = null`).
- Sistem rolların **redaktəsi / silinməsi qadağandır** → **403 Forbidden** (`ACCESS_MS_4003` / `ROLE_IS_SYSTEM`).
- Digər rollar org-a aiddir (`org_id` dolu) və yalnız öz org tərəfindən idarə oluna bilər.

### Tenant izolyasiyası (org səviyyəsində)

- Non-platform-admin istifadəçi yalnız öz `organizationId`-si ilə işləyə bilər: fərqli org-a aid data sorğusu → **403** (`ACCESS_MS_4004` `USER_ORG_MISMATCH`, `ACCESS_MS_4003` `ROLE_ORG_MISMATCH`).
- Platform admin (SUPER_ADMIN) bütün org-ları görür.

---

## Ümumi Error Formatları

> Bütün microservice-lər eyni error formatını istifadə edir (Spring `ProblemDetail`). Validation xaric bütün error-lar `fieldErrors` property-sini qaytarmır. `key` formatı: `{SERVICE_KEY}_{CODE}`.

### Validation Error (400)

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "trace:xxx",
  "key": "ACCESS_MS_1000",
  "path": "/api/access-ms/v1/users",
  "timestamp": "2026-08-06T12:00:00.000Z",
  "fieldErrors": [
    { "field": "name", "message": "Name is required" },
    { "field": "username", "message": "Username is required" }
  ]
}
```

### Unauthorized (401) — token yoxdur / keçərsiz

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication is required",
  "instance": "trace:xxx",
  "key": "COMMON_4001",
  "path": "/api/organization-ms/v1/organizations",
  "timestamp": "2026-08-06T12:00:00.000Z"
}
```

### Forbidden (403) — permission yoxdur

```json
{
  "type": "about:blank",
  "title": "Access Denied",
  "status": 403,
  "detail": "Access is denied",
  "instance": "trace:xxx",
  "key": "COMMON_4003",
  "path": "/api/access-ms/v1/roles",
  "timestamp": "2026-08-06T12:00:00.000Z"
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
  "key": "ORG_3001",
  "path": "/api/organization-ms/v1/organizations/org99",
  "timestamp": "2026-08-06T12:00:00.000Z"
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
  "timestamp": "2026-08-06T12:00:00.000Z"
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
  "timestamp": "2026-08-06T12:00:00.000Z"
}
```

### Service key cədvəli

| Servis | `service-key` | Error key prefiksi |
|---|---|---|
| auth-gateway | `AUTH` | `AUTH_001`… |
| access-service | `ACCESS_MS` | `ACCESS_MS_3001`… |
| organization-service | `ORG` | `ORG_3001`… |
| menu-service | `MENU_MS` | `MENU_MS_3001`… |
| table-service | `TABLE_MS` | `TABLE_MS_3001`… |
| order-service | `ORDER_MS` | `ORDER_MS_3001`… |
| kitchen-service | `KITCHEN_MS` | (yoxdur — upstream `ORDER_MS_*` / ümumi) |
| waiter-service | `WAITER_MS` | `WAITER_MS_3003`, `WAITER_MS_9001/9002` |
| customer-service | `CUSTOMER_MS` | (yoxdur — ümumi + upstream `*_MS_*`) |
| setting-service | `SETTING_MS` | `SETTING_MS_3001`, `SETTING_MS_4001` |
| dashboard-service | `DASHBOARD_MS` | (yoxdur — upstream `ORDER_MS_*` / ümumi) |
| report-service | `REPORT_MS` | (yoxdur — upstream `ORDER_MS_*` / ümumi) |
| gateway (auth filter) | — | `COMMON_4001`, `COMMON_4003` |

---

## RBAC & Keycloak

### Rol–Permission modeli

- DB-də (`resto_access` schema): `modules`, `ui_groups`, `permissions`, `roles`, `role_permissions`, `users` cədvəlləri.
- **Rol** `ui_scope` daşıyır (hansı panelə aid olduğu) və istənilən sayda permission-a bağlanır (`role_permissions` many-to-many cədvəli).
- **Permission** kataloqu sabitdir (code-lar); kataloqdan kənar kod istifadə olunmur.
- İstifadəçi **bir rola** aiddir (`users.role_id`). Rol dəyişəndə/perm dəyişəndə **Keycloak** sinxronlaşır ki, növbəti token yeni claim-ləri gətirsin.

### JWT Claim Cədvəli

| Claim | Tip | Mənbə | Açıqlama |
|---|---|---|---|
| `sub` | string | Keycloak (user id) | `X-User-Id` header-inə gedir |
| `roles` | string[] | `resto-auth` client rolları | DB rolun `code`-u; `X-Roles` header-i. `SUPER_ADMIN` varsa → platform admin |
| `dbRoles` | string[] | `roles` user attribute | DB rol kodu (client rol ilə eynidir; sync üçün) |
| `permissions` | string[] | `permissions` user attribute | DB-dəki `role_permissions`-dan gələn kodlar; `X-Permissions` header-i |
| `uiScope` | string | `uiScope` user attribute | Panel: `SUPER_ADMIN_PANEL`/`ADMIN_PANEL`/`WAITER_PANEL`/`KITCHEN_PANEL` |
| `organizationId` | string | `organizationId` user attribute | Org UUID; `X-Org-Id` header-i |
| `email` | string | Keycloak | — |

### Realm Mappers (resto-realm.json, client `resto-auth`)

| Mapper adı | Protokol mapper | Claim | Qeyd |
|---|---|---|---|
| `sub` | `oidc-usermodel-property-mapper` | `sub` | id token + access token |
| `organizationId` | `oidc-usermodel-attribute-mapper` | `organizationId` | user attribute `organizationId` |
| `client roles` | `oidc-usermodel-client-role-mapper` | `roles` | `resto-auth` client rolları |
| `roles attribute` | `oidc-usermodel-attribute-mapper` | `dbRoles` | user attribute `roles`, multivalued |
| `permissions` | `oidc-usermodel-attribute-mapper` | `permissions` | user attribute `permissions`, multivalued |
| `uiScope` | `oidc-usermodel-attribute-mapper` | `uiScope` | user attribute `uiScope` |

> **Vacib:** `permissions`, `uiScope`, `dbRoles` Keycloak **user attributes** olaraq saxlanılır. Backend rol/perm dəyişikliklərində bu attribute-ları sinxronlayır (`KeycloakSyncService`).

### UiScope dəyərləri

| Dəyər | Panel | Tipik istifadəçi |
|---|---|---|
| `SUPER_ADMIN_PANEL` | Platform admin paneli | `SUPER_ADMIN` rol |
| `ADMIN_PANEL` | Restoran admin paneli | `ORG_ADMIN`, `ADMIN_DEFAULT` |
| `WAITER_PANEL` | Ofisiant paneli | `WAITER_DEFAULT` |
| `KITCHEN_PANEL` | Mətbəx paneli | `KITCHEN_DEFAULT` |

Login cavabındakı `uiScope`-ə görə front müvafiq panelə redirect etməlidir.

### Permission Kataloqu (bütün kodlar)

| Modul | Permission | Açıqlama |
|---|---|---|
| `dashboard` | `dashboard.view` | Dashboard səhifəsini görüntülə |
| `menu` | `menu.view`, `menu.create`, `menu.edit`, `menu.delete` | Menyu / kateqoriya CRUD + şəkil yükləmə |
| `table` | `table.view`, `table.create`, `table.edit`, `table.delete`, `table.status`, `table.reserve` | Masalar/seksiyalar + status + rezerv |
| `order` | `order.view`, `order.create`, `order.manage`, `order.cancel`, `order.payment` | Sifariş baxmaq/yaratmaq/idarə/ləğv/ödəniş |
| `kitchen` | `kitchen.view`, `kitchen.manage` | Mətbəx paneli |
| `waiter` | `waiter.view`, `waiter.manage` | Ofisiant paneli |
| `staff` | `staff.view`, `staff.create`, `staff.edit`, `staff.delete` | İşçilər |
| `roles` | `role.view`, `role.create`, `role.edit`, `role.delete`, `role.assign`, `permission.view`, `permission.manage` | Rollar + icazələr |
| `settings` | `settings.view`, `settings.edit` | Tənzimləmələr |
| `reports` | `report.view` | Hesabatlar |
| `organization` | `organization.view`, `organization.create`, `organization.edit`, `organization.delete` | Təşkilat |

> Kod `001-003`-cü migration (`003-insert-access-data.yml`) ilə `resto_access.permissions` cədvəlinə yazılıb. **Front yalnız bu kodları istifadə etməlidir.**

### Sistem rolları və default permissionları (seed)

| Rol | `ui_scope` | `isSystem` | Permission-lar |
|---|---|---|---|
| `SUPER_ADMIN` | `SUPER_ADMIN_PANEL` | true | bütün 38 perm |
| `ORG_ADMIN` | `ADMIN_PANEL` | true | bütün 38 perm |
| `ADMIN_DEFAULT` | `ADMIN_PANEL` | false | `organization.*` xaric bütün perm-lar |
| `WAITER_DEFAULT` | `WAITER_PANEL` | false | `dashboard.view`, `table.view`, `table.status`, `order.view`, `order.create`, `order.manage`, `waiter.view`, `waiter.manage` |
| `KITCHEN_DEFAULT` | `KITCHEN_PANEL` | false | `kitchen.view`, `kitchen.manage`, `order.view` |

> Bu seed-lər **demo org** (`01234567-89ab-cdef-0123-456789abcdef`) üçündür. Yeni org yaradılanda `ORG_ADMIN` rol yaradılan admin-ə avtomatik assign olunur (aşağıda Organization bölməsinə bax).

### Login axını (auth-gateway)

1. Front `POST /api/auth-ms/v1/auth/login` → `KeycloakClient.login()` Keycloak token endpoint-i çağırır.
2. `JwtTokenValidator.extractClaims()` access token-i decode edib claim-ləri oxuyur.
3. `uiScope` belə həll olunur: JWT-də `uiScope` claim-i (düzgün dəyər varsa) → yoxsa `roles` claim-ində platform admin rol varsa `SUPER_ADMIN_PANEL` → əks halda `ADMIN_PANEL`.
4. Cavabda `permissions` claim-i olduğu kimi verilir (boş ola bilər, boşdursa backend DB-dən yoxlayacaq).
5. Front token-i saxlayır, bütün istəklərdə `Authorization: Bearer` göndərir.

---

## 1. Auth — `auth-gateway` (port 8002)

> API prefix: `/api/auth-ms/v1/auth/...`
> Response: **birbaşa DTO** (`ApiResponse` wrapper-i yoxdur)
> Error: Spring `ProblemDetail`; key prefiksi `AUTH_`
> Context path: `/api/auth-ms`; gateway marşrutu: `/api/auth-ms/**` → `http://localhost:8002`

### `POST /api/auth-ms/v1/auth/login`

**Giriş.** Backend Keycloak üzərindən autentifikasiya edir və cavabda token-lər + `uiScope` + `permissions` qaytarır.

Request body:
```json
{
  "username": "demo.admin",
  "password": "demo12345"
}
```

Success (200):
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl...",
  "expiresIn": 300,
  "tokenType": "Bearer",
  "user": {
    "username": "demo.admin",
    "roles": ["ORG_ADMIN"]
  },
  "uiScope": "ADMIN_PANEL",
  "permissions": ["dashboard.view", "menu.view", "menu.create", "..."]
}
```

| Sahə | Tip | Açıqlama |
|---|---|---|
| `accessToken` | string | Keycloak JWT (~5 dəq etibarlıdır) |
| `refreshToken` | string | Refresh üçün |
| `expiresIn` | long | saniyə |
| `tokenType` | string | `Bearer` |
| `user.username` | string | giriş istifadəçisi |
| `user.roles` | string[] | DB rolları (client rolları) |
| `uiScope` | `SUPER_ADMIN_PANEL`/`ADMIN_PANEL`/`WAITER_PANEL`/`KITCHEN_PANEL` | Panel seçimi üçün |
| `permissions` | string[] | İstifadəçinin permission kodları |

> **Front:** `uiScope`-ə görə redirect; `permissions` array-i ilə menyu/hide elementləri. Token ~5 dəq etibarlı olduğundan front 401 alanda `/refresh` çağırmalıdır.

Error (401 — yanlış kredensial):
```json
{
  "type": "about:blank",
  "title": "Authentication Failed",
  "status": 401,
  "detail": "Invalid username or password",
  "instance": "/api/auth-ms/v1/auth/login",
  "key": "AUTH_001",
  "path": "/api/auth-ms/v1/auth/login",
  "timestamp": "2026-08-06T12:00:00.000Z"
}
```

Error (502 — Keycloak əlçatan deyil): `AUTH_005`.

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

Error (401 — token expired/invalid): `AUTH_002`, `AUTH_003`.

### `POST /api/auth-ms/v1/auth/logout`

**Refresh token-i invalid edir (Keycloak session).**

Request:
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJl..."
}
```

Success (200): *empty body*

Error (502): `AUTH_004`.

---

## 2. Access — `access-service` (port 8120)

> API prefix: `/api/access-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; key prefiksi `ACCESS_MS_`
> Context path: `/api/access-ms`; gateway marşrutu: `/api/access-ms/**` → `http://localhost:8120`
>
> Bu servis köhnə **user-service (8103)** və **role-service (8104)**-ü birləşdirir: İstifadəçilər (staff), rollar, permission kataloqu, modullar və ui-groups burada yaşayır.

### Tenant & Giriş Qaydaları

- Bütün endpoint-lər `Authorization: Bearer` tələb edir və `@PreAuthorize` permission kodu ilə qorunur.
- Non-platform-admin istifadəçi **yalnız öz org-unun** user/rol data-sını görür; başqa org-a aid ID sorğulasa → **403**.
- `GET /v1/permissions/my` istisnadır — yalnız autentifikasiya tələb edir (`isAuthenticated()`), heç bir perm kod yoxdur.
- User yaradılarkən həm **Keycloak**-da istifadəçi + rol + attribute-lar, həm DB-də istifadəçi yaradılır; Keycloak əlçatan olmasa → **502/`ACCESS_MS_3003`**.
- User/rol silmə soft-delete-dir.

### Access-servis Error Kodları

| Kod | HTTP | Açıqlama |
|---|---|---|
| `ACCESS_MS_1000` | 400 | Validation xətası (+ `fieldErrors`) |
| `ACCESS_MS_3001` | 404 | User/Rol tapılmadı |
| `ACCESS_MS_3002` | 409 | Username/rol code unikald deyil (duplicate) |
| `ACCESS_MS_3003` | 502 | Keycloak əlçatan deyil |
| `ACCESS_MS_3004` | 404 | Permission tapılmadı |
| `ACCESS_MS_4001` | 401 | Autentifikasiya tələb olunur |
| `ACCESS_MS_4003` | 403 | Permission yoxdur / sistem rol redaktə oluna bilməz (`ROLE_IS_SYSTEM`) / rol başqa org-a aiddir (`ROLE_ORG_MISMATCH`) |
| `ACCESS_MS_4004` | 403 | User başqa org-a aiddir (`USER_ORG_MISMATCH`) |
| `ACCESS_MS_9999` | 500 | Daxili xəta |

### Data Modelləri

**`UserDto`** (user cavabları):

| Sahə | Tip | Açıqlama |
|---|---|---|
| `id` | UUID | DB user id |
| `keycloakId` | string | Keycloak user id (null ola bilər) |
| `name` | string | Ad |
| `username` | string | Login |
| `email` | string | null ola bilər |
| `phone` | string | null ola bilər |
| `orgId` | UUID | Tenant |
| `role` | `RoleBriefDto` | Rol (null ola bilər — rol silinəndə) |
| `isActive` | boolean | Aktivlik |

**`RoleBriefDto`**: `id`, `code`, `name`, `uiScope`.

**`RoleResponse`**:

| Sahə | Tip |
|---|---|
| `id` | UUID |
| `code` | string |
| `name` | string |
| `uiScope` | UiScope |
| `isSystem` | boolean |
| `isActive` | boolean |
| `orgId` | UUID (sistem rollarda null) |
| `permissionIds` | UUID[] |
| `permissions` | `PermissionDto[]` |

**`PermissionDto`**:

| Sahə | Tip |
|---|---|
| `id` | UUID |
| `code` | string |
| `name` | string |
| `description` | string |
| `module` | `ModuleRefDto` (`id`,`code`,`name`) |
| `uiGroup` | `UiGroupRefDto` (`id`,`code`,`name`) |
| `sortOrder` | int |
| `isActive` | boolean |

**`ModuleDto`**: `id`, `code`, `name`, `sortOrder`, `uiGroups: UiGroupDto[]`.
**`UiGroupDto`**: `id`, `code`, `name`, `sortOrder`, `permissions: PermissionDto[]`.
**`ModuleTreeDto`**: `id`, `code`, `name`, `sortOrder`, `uiGroups: UiGroupDto[]`.

> Seed ID nümunələri (001-003 migration): modullar `50000000-0000-4000-8000-...001..00b`, ui-groups `60000000-...`, permissions `70000000-...001..026`, rollar `aaaaaaaa-0000-4000-8000-0000000000XX`, userlər `bbbbbbbb-0000-4000-8000-0000000000XX`. Front bunları hardcode etməməli, API-dan oxumalıdır.

---

### Users (staff)

#### `GET /api/access-ms/v1/users`

**İstifadəçi siyahısı (paginasiya ilə).**

- **Auth:** `Authorization: Bearer {token}`
- **Permission:** `staff.view`

Query parametrləri: `orgId` (optional), `roleId` (optional), `q` (optional), `page`, `size`.

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "content": [
      {
        "id": "bbbbbbbb-0000-4000-8000-000000000001",
        "keycloakId": "aa11bb22-...",
        "name": "Demo Admin",
        "username": "demo.admin",
        "email": "admin@flowix.az",
        "phone": "+994501234567",
        "orgId": "01234567-89ab-cdef-0123-456789abcdef",
        "role": {
          "id": "aaaaaaaa-0000-4000-8000-000000000002",
          "code": "ORG_ADMIN",
          "name": "Organizasiya Admini",
          "uiScope": "ADMIN_PANEL"
        },
        "isActive": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

Error (403): `ACCESS_MS_4003` — `staff.view` yoxdursa.

#### `GET /api/access-ms/v1/users/{id}`

**Bir istifadəçi.**

- **Auth:** Bearer
- **Permission:** `staff.view`

Success (200): yuxarıdakı `UserDto` (array deyil, obyekt).

#### `POST /api/access-ms/v1/users`

**Yeni istifadəçi.** DB + Keycloak-da yaradılır, Keycloak-a rol atributu yazılır.

- **Auth:** Bearer
- **Permission:** `staff.create`

Request:
```json
{
  "name": "Nigar Hüseynova",
  "username": "nigar",
  "password": "Nigar1234",
  "roleId": "aaaaaaaa-0000-4000-8000-000000000004",
  "orgId": "01234567-89ab-cdef-0123-456789abcdef",
  "email": "nigar@flowix.az",
  "phone": "+994 55 999 88 77"
}
```

Success (201): `UserDto`.

Error (409): `ACCESS_MS_3002` — username artıq mövcuddur.
Error (403): `ACCESS_MS_4004` — başqa org-a user yaratmaq.
Error (502): `ACCESS_MS_3003` — Keycloak əlçatan deyil (DB-də rollback olunur).

#### `PUT /api/access-ms/v1/users/{id}`

**İstifadəçini redaktə et** (name, username, email, password, phone, isActive). Bütün field-lər optional-dır — null/blank gələn field dəyişdirilmir. `username` yalnız lokal bazada yenilənir (Keycloak username immutable-dır). Rol ayrıca `POST /roles/{id}/users` ilə təyin olunur. Dəyişənlər Keycloak-a da yazılır.

- **Auth:** Bearer
- **Permission:** `staff.edit`

Request:
```json
{
  "name": "Nigar Hüseynova",
  "username": "nigar.huseynova",
  "email": "nigar@example.com",
  "password": "yeniSifre123",
  "phone": "+994 55 111 22 33",
  "isActive": false
}
```

Success (200): `UserDto`.
Error (403): `ACCESS_MS_4004`, Error (409): `ACCESS_MS_3002` (username duplicate), `ACCESS_MS_3005` (email duplicate), Error (502): `ACCESS_MS_3003`.

#### `DELETE /api/access-ms/v1/users/{id}`

**İstifadəçini sil (soft-delete).** Keycloak-da user deaktiv edilir.

- **Auth:** Bearer
- **Permission:** `staff.delete`

Success (204): *empty body*.
Error (403): `ACCESS_MS_4004`.

#### `DELETE /api/access-ms/v1/users/{id}/role`

**İstifadəçidən rolu geri al (unassign).** Rol `null` edilir, Keycloak-dan rol atributu silinir.

- **Auth:** Bearer
- **Permission:** `role.assign`

Success (204): *empty body*.

#### `GET /api/access-ms/v1/users/staff-performance`

**İşçi performans siyahısı.** Hal-hazırda order statistikası `0` dəyərləri ilə gəlir (placeholder).

- **Auth:** Bearer
- **Permission:** `staff.view`

Query: `orgId` (required), `roleId` (optional).

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "userId": "bbbbbbbb-0000-4000-8000-000000000001",
      "name": "Demo Admin",
      "role": "ORG_ADMIN",
      "totalOrders": 0,
      "completedOrders": 0,
      "revenue": 0,
      "activeOrders": 0
    }
  ]
}
```

---

### Roles

#### `GET /api/access-ms/v1/roles`

**Rol siyahısı (paginasiya).** Non-admin öz org-unun rollarını + sistem rolları görür.

- **Auth:** Bearer
- **Permission:** `role.view`

Query: `q`, `page`, `size`.

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "content": [
      {
        "id": "aaaaaaaa-0000-4000-8000-000000000002",
        "code": "ORG_ADMIN",
        "name": "Organizasiya Admini",
        "uiScope": "ADMIN_PANEL",
        "isSystem": true,
        "isActive": true,
        "orgId": null,
        "permissionIds": ["70000000-0000-4000-8000-000000000001", "..."],
        "permissions": [
          {
            "id": "70000000-0000-4000-8000-000000000001",
            "code": "dashboard.view",
            "name": "Dashboard görüntülə",
            "description": "Dashboard səhifəsini görüntüləmək",
            "module": { "id": "50000000-0000-4000-8000-000000000001", "code": "dashboard", "name": "Dashboard" },
            "uiGroup": { "id": "60000000-0000-4000-8000-000000000001", "code": "dashboard", "name": "Dashboard" },
            "sortOrder": 1,
            "isActive": true
          }
        ]
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

Error (403): `ACCESS_MS_4003`.

#### `GET /api/access-ms/v1/roles/{id}`

**Bir rol (permission-ları ilə).**

- **Auth:** Bearer
- **Permission:** `role.view`

Success (200): `RoleResponse` (yuxarıdakı kimi, array deyil).

#### `POST /api/access-ms/v1/roles`

**Yeni rol yarat.** `orgId` boş saxlanarsa non-admin üçün cari org götürülür; platform admin xaricdə başqa org seçə bilər.

- **Auth:** Bearer
- **Permission:** `role.create`

Request:
```json
{
  "code": "CASHIER",
  "name": "Kassir",
  "uiScope": "ADMIN_PANEL",
  "permissionIds": ["70000000-0000-4000-8000-00000000000c", "70000000-0000-4000-8000-00000000000d"]
}
```

Success (201): `RoleResponse`.
Error (409): `ACCESS_MS_3002` — code duplicate.
Error (403): `ACCESS_MS_4003` — başqa org üçün rol yaratmaq (non-admin).
Error (404): `ACCESS_MS_3004` — `permissionIds` içində tapılmayan ID.

#### `PUT /api/access-ms/v1/roles/{id}`

**Rolun adını/uiScope-nu dəyiş.** Sistem rol üzərində işləməz (403). Dəyişiklikdən sonra həmin roldakı userlərin Keycloak attribute-ları sinxronlaşır.

- **Auth:** Bearer
- **Permission:** `role.edit`

Request:
```json
{
  "name": "Baş Kassir",
  "uiScope": "ADMIN_PANEL"
}
```

Success (200): `RoleResponse`.
Error (403): `ACCESS_MS_4003` — `isSystem=true` olan rol (`ROLE_IS_SYSTEM`) və ya başqa org rolu.

#### `DELETE /api/access-ms/v1/roles/{id}`

**Rolu sil (soft-delete).** Roldakı userlərin rolu `null` olur, Keycloak sinxronlaşır.

- **Auth:** Bearer
- **Permission:** `role.delete`

Success (204): *empty body*.
Error (403): `ACCESS_MS_4003` — sistem rol silinə bilməz.

#### `POST /api/access-ms/v1/roles/{id}/permissions`

**Rola permission-ları əlavə et (add).**

- **Auth:** Bearer
- **Permission:** `role.edit`

Request:
```json
{
  "permissionIds": ["70000000-0000-4000-8000-000000000013"]
}
```

Success (200): `RoleResponse`.

#### `PUT /api/access-ms/v1/roles/{id}/permissions`

**Rolun permission-larını tam əvəz et (set).**

- **Auth:** Bearer
- **Permission:** `role.edit`

Request:
```json
{
  "permissionIds": ["70000000-0000-4000-8000-000000000002", "70000000-0000-4000-8000-000000000003"]
}
```

Success (200): `RoleResponse`.

#### `DELETE /api/access-ms/v1/roles/{id}/permissions/{permissionId}`

**Bir permission-u roldan çıxar.**

- **Auth:** Bearer
- **Permission:** `role.edit`

Success (204): *empty body*.

#### `POST /api/access-ms/v1/roles/{id}/users`

**İstifadəçilərə rol təyin et (assign).**

- **Auth:** Bearer
- **Permission:** `role.assign`

Request:
```json
{
  "userIds": ["bbbbbbbb-0000-4000-8000-000000000002"]
}
```

Success (201): *empty body*.
Error (404): `ACCESS_MS_3001` — user tapılmadı.
Error (403): `ACCESS_MS_4004` — başqa org user-inə rol təyin etmək.

#### `GET /api/access-ms/v1/roles/{id}/users`

**Roldakı istifadəçilər (paginasiya).**

- **Auth:** Bearer
- **Permission:** `role.view`

Query: `q`, `page`, `size`.

Success (200): `PageDto<UserDto>`.

#### `DELETE /api/access-ms/v1/roles/{id}/users/{userId}`

**Bir istifadəçidən rolu geri al.**

- **Auth:** Bearer
- **Permission:** `role.assign`

Success (204): *empty body*.

#### `GET /api/access-ms/v1/roles/system/{code}`

**Sistem rolu code ilə əldə et** (məsələn `ORG_ADMIN`). Yalnız `isSystem` rollar.

- **Auth:** Bearer
- **Permission:** `role.view`

Success (200): `RoleResponse`.
Error (404): `ACCESS_MS_3001`.

---

### Permissions (kataloq)

#### `GET /api/access-ms/v1/permissions/my`

**Cari istifadəçinin permission kodları.** Yalnız autentifikasiya tələb edir — login sonrası front menyu/hide üçün istifadə edə bilər. Gateway bu marşrutu public edib (`permitAll`), amma token varsa principal-dan oxunur.

- **Auth:** Bearer (tələb olunmur, amma varsa istifadə olunur)
- **Permission:** — (`isAuthenticated()`)

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    { "code": "dashboard.view" },
    { "code": "menu.view" },
    { "code": "menu.create" }
  ]
}
```

> **Qeyd:** cavab yalnız `code` ilə sadə `PermissionDto` obyektlərindən ibarətdir (digər sahələr null).

#### `GET /api/access-ms/v1/permissions`

**Permission kataloqu (paginasiya + filtrlər).**

- **Auth:** Bearer
- **Permission:** `permission.view`

Query: `q`, `module` (modul code, məs. `menu`), `uiGroup` (ui-group code, məs. `menu`), `page`, `size`.

Success (200): `PageDto<PermissionDto>` (modul/uiGroup ref-ləri dolu).

#### `GET /api/access-ms/v1/permissions/tree`

**Modul → ui-group → permission ağacı.** Rol formu üçün ən rahat endpoint.

- **Auth:** Bearer
- **Permission:** `permission.view`

Query: `q` (optional).

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "50000000-0000-4000-8000-000000000002",
      "code": "menu",
      "name": "Menyu",
      "sortOrder": 2,
      "uiGroups": [
        {
          "id": "60000000-0000-4000-8000-000000000002",
          "code": "menu",
          "name": "Menyu",
          "sortOrder": 2,
          "permissions": [
            {
              "id": "70000000-0000-4000-8000-000000000002",
              "code": "menu.view",
              "name": "Menyu görüntülə",
              "description": "Menyu və kateqoriyaları görüntüləmək",
              "module": { "id": "...", "code": "menu", "name": "Menyu" },
              "uiGroup": { "id": "...", "code": "menu", "name": "Menyu" },
              "sortOrder": 1,
              "isActive": true
            }
          ]
        }
      ]
    }
  ]
}
```

#### `GET /api/access-ms/v1/permissions/by-module`

**Modul code ilə permission siyahısı** (məs. `?module=order`).

- **Auth:** Bearer
- **Permission:** `permission.view`

Query: `module` (required), `q` (optional).

Success (200): `PermissionDto[]`.

#### `GET /api/access-ms/v1/permissions/by-ui-group`

**UI-group code ilə permission siyahısı** (məs. `?uiGroup=orders`).

- **Auth:** Bearer
- **Permission:** `permission.view`

Query: `uiGroup` (required), `q` (optional).

Success (200): `PermissionDto[]`.

---

### Modules

#### `GET /api/access-ms/v1/modules`

**Modul siyahısı (hər modulun ui-groups + permissions ilə).**

- **Auth:** Bearer
- **Permission:** `permission.view`

Query: `q` (optional).

Success (200): `ModuleDto[]` (`id`, `code`, `name`, `sortOrder`, `uiGroups[]`).

---

### UI Groups

#### `GET /api/access-ms/v1/ui-groups`

**UI-group siyahısı (permissions ilə).**

- **Auth:** Bearer
- **Permission:** `permission.view`

Query: `q` (optional), `module` (optional, modul code).

Success (200): `UiGroupDto[]`.

---

## 3. Organization — `organization-service` (port 8102)

> API prefix: `/api/organization-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; key prefiksi `ORG_`
> Context path: `/api/organization-ms`; gateway marşrutu: `/api/organization-ms/**` → `http://localhost:8102`

### Auth & Tenant Qaydaları

- `GET /v1/organizations` (list) və `POST /v1/organizations` (create) **yalnız platform admin** üçündür: `hasRole('SUPER_ADMIN') and @perm.has('organization.view'/'organization.create')`.
- `GET /v1/organizations/{orgId}` və qr-code: `@perm.has('organization.view')` — non-admin yalnız öz org-u ilə çağıra bilər (backend yoxlayır).
- **`ORG_ADMIN` rolunun avtomatik assign-ı:** `POST /v1/organizations` zamanı orchestrator yeni org üçün admin istifadəçi yaradır və ona sistem `ORG_ADMIN` rolunu təyin edir. Ayrıca "rol yarat" endpoint-i bu ssenaridə **yoxdur** (silməklə/əvəz edilib) — rol idarəsi `access-service` (bölmə 2) üzərindən aparılır.
- Org yaradılarkən access-service, setting-service, table-service, order-service müvafiq konfiqurasiyalarla sinxronlaşır.

### Organization-servis Error Kodları

> `OrganizationErrorCode` enum-ından; ümumi kodlar (`*_1000`, `*_4001`, `*_4003`, `*_9999`) bölmə 15-dədir.

| Kod | HTTP | Açıqlama |
|---|---|---|
| `ORG_3001` | 404 | Org tapılmadı (`ORGANIZATION_NOT_FOUND`) |
| `ORG_3002` | 409 | Org `slug` unikald deyil (`ORGANIZATION_SLUG_DUPLICATE`) |
| `ORG_3003` | 500 | Org yaradılmadı — Keycloak/admin/rol xətası (`ORGANIZATION_CREATION_FAILED`) |
| `ORG_3004` | 409 | Org silinə bilməz — aktiv sifarişlər var (`ORGANIZATION_HAS_ACTIVE_ORDERS`) |
| `ORG_3005` | 403 | Giriş qadağandır (`ORGANIZATION_ACCESS_DENIED`) |

### Data Modelləri

**`OrganizationDto`**: `id`, `name`, `slug`, `adminName`, `adminEmail`, `logoUrl`, `phone`, `address`, `createdAt`.

**`CreateOrganizationRequest`**:

| Sahə | Tip | Validasiya |
|---|---|---|
| `name` | string | 3–100 simvol |
| `adminName` | string | 2–100 simvol |
| `adminEmail` | string | email formatı, max 254 |
| `adminPassword` | string | 8–72 simvol, ən azı 1 hərf + 1 rəqəm |

**`CreateOrganizationResponse`**:
```json
{
  "organization": { ...OrganizationDto... },
  "adminUser": {
    "id": "...",
    "name": "...",
    "username": "...",
    "email": "...",
    "role": "ORG_ADMIN",
    "roleId": "aaaaaaaa-0000-4000-8000-000000000002",
    "orgId": "..."
  },
  "adminRole": {
    "id": "aaaaaaaa-0000-4000-8000-000000000002",
    "name": "Organizasiya Admini",
    "permissions": ["dashboard.view", "menu.view", "..."],
    "isSystem": true,
    "orgId": null
  }
}
```

**`QrCodeResponse`**: `qrCodeUrl` (string).

### `GET /api/organization-ms/v1/organizations`

**Bütün təşkilatların siyahısı. Yalnız platform admin.**

- **Auth:** Bearer
- **Permission:** `organization.view` + `hasRole('SUPER_ADMIN')`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "01234567-89ab-cdef-0123-456789abcdef",
      "name": "Demo Restoran",
      "slug": "demo-restoran",
      "adminName": "Demo Admin",
      "adminEmail": "admin@flowix.az",
      "logoUrl": null,
      "phone": "+994 50 123 45 67",
      "address": null,
      "createdAt": "2026-08-01T10:00:00Z"
    }
  ]
}
```

Error (403): `COMMON_4003` — platform admin deyilsə.

### `POST /api/organization-ms/v1/organizations`

**Yeni təşkilat yarat + admin + ORG_ADMIN rol assign et. Yalnız platform admin.**

- **Auth:** Bearer
- **Permission:** `organization.create` + `hasRole('SUPER_ADMIN')`

Request:
```json
{
  "name": "İstanbul Restoran",
  "adminName": "Əli Əliyev",
  "adminEmail": "ali@istanbul.az",
  "adminPassword": "Ali12345"
}
```

Success (201): `CreateOrganizationResponse` (yuxarıdakı kimi).

Error (409): email/username mövcuddur → Feign vasitəsilə `ACCESS_MS_3002`.
Error (502): Keycloak əlçatan deyil → `ACCESS_MS_3003`.

### `GET /api/organization-ms/v1/organizations/{orgId}`

**Bir təşkilatın məlumatı.**

- **Auth:** Bearer
- **Permission:** `organization.view`

Success (200): `OrganizationDto`.
Error (403): `ORG_3005` — başqa org sorğulanırsa (`ORGANIZATION_ACCESS_DENIED`).
Error (404): `ORG_3001`.

### `GET /api/organization-ms/v1/organizations/{orgId}/qr-code`

**Customer menyu üçün QR kod linki.**

- **Auth:** Bearer
- **Permission:** `organization.view`

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "qrCodeUrl": "https://qr.example.com/menu?org=01234567-89ab-cdef-0123-456789abcdef"
  }
}
```

Error (403): `ORG_3005` — başqa org.

---

## 4. Menu — `menu-service` (port 8105)

> API prefix: `/api/menu-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; key prefiksi `MENU_MS_`
> Context path: `/api/menu-ms`; gateway marşrutu: `/api/menu-ms/**` → `http://localhost:8105`

### Tenant & Giriş Qayraları

- Bütün endpoint-lər `Authorization: Bearer` tələb edir və `@PreAuthorize` permission kodu ilə qorunur.
- Non-admin istifadəçi yalnız öz org-unun menyusuna girişir; başqa org `orgId` → **403** (`MENU_MS_3003`).
- `GET /api/menu-ms/v1/images/**` — **public** (gateway permitAll), token tələb etmir. Şəkillərin base URL-i: `http://localhost:8001` (gateway).

### Lokalizasiya (`LocalizedString`)

Bütün ad/təsvir sahələri üç dildədir:
```json
"name": { "az": "Doner", "en": "Kebab", "ru": "Донер" }
```
Request-də ən azı `az` doldurulmalıdır; boş dillər `null` kimi saxlanılır.

### Menu-servis Error Kodları

| Kod | HTTP | Açıqlama |
|---|---|---|
| `MENU_MS_1000` | 400 | Validation (+`fieldErrors`) |
| `MENU_MS_3001` | 404 | Kateqoriya tapılmadı |
| `MENU_MS_3002` | 404 | Menyu elementi tapılmadı |
| `MENU_MS_3003` | 403 | Giriş qadağandır (başqa org) |
| `MENU_MS_3004` | 400 | `moveItemsTo` özü ilə üst-üstə düşür (`CATEGORY_SELF_MOVE`) |
| `MENU_MS_4001` | 401 | Token tələb olunur |
| `MENU_MS_4003` | 403 | Permission yoxdur |
| `MENU_MS_9999` | 500 | Daxili xəta |

### Data Modelləri

**`CategoryResponse`**: `id`, `name` (LocalizedString), `icon`, `sortOrder`, `orgId`.
**`MenuItemResponse`**: `id`, `name`, `description`, `price`, `categoryId`, `imageUrl`, `isAvailable`, `preparationTime`, `orgId`, `createdAt`.
**`ImageUploadResponse`**: `imageUrl`.

### Kateqoriyalar

#### `GET /api/menu-ms/v1/categories`

- **Auth:** Bearer
- **Permission:** `menu.view`
- Query: `orgId` (required)

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "aaaaaaaa-0000-4000-8000-000000000001",
      "name": { "az": "Pərciklər", "en": "Starters", "ru": "Закуски" },
      "icon": "🍢",
      "sortOrder": 1,
      "orgId": "01234567-89ab-cdef-0123-456789abcdef"
    }
  ]
}
```

#### `GET /api/menu-ms/v1/categories/{id}`

- **Auth:** Bearer
- **Permission:** `menu.view`

Success (200): `CategoryResponse`. Error (404): `MENU_MS_3001`.

#### `POST /api/menu-ms/v1/categories`

- **Auth:** Bearer
- **Permission:** `menu.create`

Request:
```json
{
  "name": { "az": "İçkilər", "en": "Drinks", "ru": "Напитки" },
  "icon": "🥤",
  "sortOrder": 2,
  "orgId": "01234567-89ab-cdef-0123-456789abcdef"
}
```

Success (201): `CategoryResponse`.

#### `PUT /api/menu-ms/v1/categories/{id}`

- **Auth:** Bearer
- **Permission:** `menu.edit`

Request:
```json
{
  "name": { "az": "Sərin İçkilər", "en": "Cold Drinks", "ru": "Холодные напитки" },
  "icon": "🧊",
  "sortOrder": 2
}
```

Success (200): `CategoryResponse`.

#### `DELETE /api/menu-ms/v1/categories/{id}`

Kateqoriyanı silir. İçindəki elementlər `moveItemsTo` kateqoriyasına köçürülə bilər (boş saxlanarsa elementlər silinir).

- **Auth:** Bearer
- **Permission:** `menu.delete`

Request body (optional):
```json
{ "moveItemsTo": "aaaaaaaa-0000-4000-8000-000000000001" }
```

Success (200): `{ "success": true, "message": "Category deleted", "data": null }`.
Error (400): `MENU_MS_3004` — `moveItemsTo` hədəf kateqoriya ilə eynidirsə.

### Menyu Elementləri

#### `GET /api/menu-ms/v1/items`

- **Auth:** Bearer
- **Permission:** `menu.view`
- Query: `orgId` (optional), `categoryId` (optional), `available` (optional boolean)

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "bbbbbbbb-0000-4000-8000-000000000101",
      "name": { "az": "Toyuq Doner", "en": "Chicken Kebab", "ru": "Куриный донер" },
      "description": { "az": "Lavaş, toyuq, tərəvəz", "en": "Wrap, chicken, veg", "ru": "Лаваш, курица, овощи" },
      "price": 9.50,
      "categoryId": "aaaaaaaa-0000-4000-8000-000000000001",
      "imageUrl": "http://localhost:8001/api/menu-ms/v1/images/items/bbbbbbbb-0000-4000-8000-000000000101.png",
      "isAvailable": true,
      "preparationTime": 15,
      "orgId": "01234567-89ab-cdef-0123-456789abcdef",
      "createdAt": "2026-08-01T10:00:00Z"
    }
  ]
}
```

#### `GET /api/menu-ms/v1/items/{id}`

- **Auth:** Bearer
- **Permission:** `menu.view`

Success (200): `MenuItemResponse`. Error (404): `MENU_MS_3002`.

#### `POST /api/menu-ms/v1/items`

- **Auth:** Bearer
- **Permission:** `menu.create`

Request:
```json
{
  "name": { "az": "Qarışıq Doner", "en": "Mixed Kebab", "ru": "Смешанный донер" },
  "description": { "az": "Mal + toyuq", "en": "Beef + chicken", "ru": "Говядина + курица" },
  "price": 11.00,
  "categoryId": "aaaaaaaa-0000-4000-8000-000000000001",
  "preparationTime": 20,
  "isAvailable": true,
  "orgId": "01234567-89ab-cdef-0123-456789abcdef"
}
```

Success (201): `MenuItemResponse`.

#### `PUT /api/menu-ms/v1/items/{id}`

- **Auth:** Bearer
- **Permission:** `menu.edit`

Request (yalnız dəyişən sahələr):
```json
{
  "price": 12.00,
  "isAvailable": false
}
```

Success (200): `MenuItemResponse`.

#### `DELETE /api/menu-ms/v1/items/{id}`

- **Auth:** Bearer
- **Permission:** `menu.delete`

Success (200): `{ "success": true, "message": "Menu item deleted", "data": null }`.

#### `POST /api/menu-ms/v1/items/{id}/image`

Şəkil yükləyir (multipart/form-data). Public URL qaytarılır.

- **Auth:** Bearer
- **Permission:** `menu.edit`
- Content-Type: `multipart/form-data`; field adı: `file` (max 2MB)

Success (200):
```json
{
  "success": true,
  "message": "Image uploaded",
  "errorCode": null,
  "data": {
    "imageUrl": "http://localhost:8001/api/menu-ms/v1/images/items/bbbbbbbb-0000-4000-8000-000000000101.png"
  }
}
```

#### `DELETE /api/menu-ms/v1/items/{id}/image`

- **Auth:** Bearer
- **Permission:** `menu.edit`

Success (200): `{ "success": true, "message": "Image deleted", "data": null }`.

---

## 5. Table — `table-service` (port 8106)

> API prefix: `/api/table-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; key prefiksi `TABLE_MS_`
> Context path: `/api/table-ms`; gateway marşrutu: `/api/table-ms/**` → `http://localhost:8106`

### Tenant & Giriş Qayraları

- Bütün endpoint-lər `Authorization: Bearer` tələb edir və `@PreAuthorize` permission kodu ilə qorunur.
- Non-admin istifadəçi yalnız öz org-unun masa/seksiyalarına girişir; başqa org → **403** (`TABLE_MS_3003`).

### Status Maşını

`AVAILABLE` → `OCCUPIED` → `CLEANING` → `AVAILABLE`; `RESERVED` ayrıca (rezerv vaxtı). Status dəyişmələri yalnız icazə verilən keçidlərdir (`TABLE_MS_2003`).

### Table-servis Error Kodları

| Kod | HTTP | Açıqlama |
|---|---|---|
| `TABLE_MS_3001` | 404 | Masa tapılmadı |
| `TABLE_MS_3002` | 404 | Seksiya tapılmadı |
| `TABLE_MS_3003` | 403 | Giriş qadağandır (başqa org) |
| `TABLE_MS_3004` | 409 | Masa nömrəsi artıq istifadə olunur |
| `TABLE_MS_3005` | 409 | Seksiya adı artıq istifadə olunur |
| `TABLE_MS_2001` | 409 | Masa aktiv sifarişlidir, silmək olmaz |
| `TABLE_MS_2002` | 409 | Son seksiya silinə bilməz |
| `TABLE_MS_2003` | 400 | Qeyri-keçərli status keçidi |
| `TABLE_MS_2004` | 409 | Masa doludur (OCCUPIED) |
| `TABLE_MS_2005` | 409 | Masada rezerv var |
| `TABLE_MS_2006` | 409 | Ümumi conflict |
| `TABLE_MS_4001` | 400 | `INVALID_STATUS` |
| `TABLE_MS_4002` | 400 | Rezerv qonaq sayı masanın tutumunu keçir |
| `TABLE_MS_4003` | 400 | OCCUPIED statusu üçün `currentOrderId` tələb olunur |

> `401` / `403` / `4000` (validation) ümumi error kodları bölmə 15-dədir.

### Data Modelləri

**`TableResponse`**: `id`, `tableNumber`, `capacity`, `status`, `sectionId`, `currentOrderId`, `reservation`, `orgId`.

**`TableReservation`** (nested): `guestName`, `phone`, `time` (ISO instant), `guestCount`, `notes`.

**`SectionResponse`**: `id`, `name`, `orgId`.

### Seksiyalar

#### `GET /api/table-ms/v1/sections`

- **Auth:** Bearer
- **Permission:** `table.view`
- Query: `orgId` (required)

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    { "id": "aaaaaaaa-0000-4000-8000-000000000001", "name": "Salon", "orgId": "01234567-89ab-cdef-0123-456789abcdef" }
  ]
}
```

#### `POST /api/table-ms/v1/sections`

- **Auth:** Bearer
- **Permission:** `table.create`

Request:
```json
{ "name": "Terras", "orgId": "01234567-89ab-cdef-0123-456789abcdef" }
```

Success (201): `SectionResponse`. Error (409): `TABLE_MS_3005`.

#### `PUT /api/table-ms/v1/sections/{id}`

- **Auth:** Bearer
- **Permission:** `table.edit`

Request: `{ "name": "Açıq Terras" }`

Success (200): `SectionResponse`.

#### `DELETE /api/table-ms/v1/sections/{id}`

- **Auth:** Bearer
- **Permission:** `table.delete`

Success (200): `{ "success": true, "message": "Section deleted", "data": null }`.
Error (409): `TABLE_MS_2002` (son seksiya).

### Masalar

#### `GET /api/table-ms/v1/tables`

- **Auth:** Bearer
- **Permission:** `table.view`
- Query: `orgId` (required), `sectionId` (optional), `status` (optional: `AVAILABLE`/`OCCUPIED`/`RESERVED`/`CLEANING`)

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": [
    {
      "id": "bbbbbbbb-0000-4000-8000-000000000201",
      "tableNumber": 1,
      "capacity": 4,
      "status": "AVAILABLE",
      "sectionId": "aaaaaaaa-0000-4000-8000-000000000001",
      "currentOrderId": null,
      "reservation": null,
      "orgId": "01234567-89ab-cdef-0123-456789abcdef"
    }
  ]
}
```

#### `GET /api/table-ms/v1/tables/{id}`

- **Auth:** Bearer
- **Permission:** `table.view`

Success (200): `TableResponse`. Error (404): `TABLE_MS_3001`.

#### `POST /api/table-ms/v1/tables`

- **Auth:** Bearer
- **Permission:** `table.create`

Request:
```json
{
  "tableNumber": 12,
  "capacity": 6,
  "sectionId": "aaaaaaaa-0000-4000-8000-000000000001",
  "orgId": "01234567-89ab-cdef-0123-456789abcdef"
}
```

Success (201): `TableResponse`. Error (409): `TABLE_MS_3004`.

#### `PUT /api/table-ms/v1/tables/{id}`

- **Auth:** Bearer
- **Permission:** `table.edit`

Request (yalnız dəyişənlər):
```json
{ "tableNumber": 13, "capacity": 8, "sectionId": "aaaaaaaa-0000-4000-8000-000000000001", "status": "AVAILABLE" }
```

Success (200): `TableResponse`.

#### `DELETE /api/table-ms/v1/tables/{id}`

- **Auth:** Bearer
- **Permission:** `table.delete`

Success (200): `{ "success": true, "message": "Table deleted", "data": null }`.
Error (409): `TABLE_MS_2001` — aktiv sifarişli masa.

#### `PUT /api/table-ms/v1/tables/{id}/status`

- **Auth:** Bearer
- **Permission:** `table.status`

Request:
```json
{ "status": "OCCUPIED", "currentOrderId": "order-000001" }
```

> `currentOrderId` `OCCUPIED` keçidində tələb olunur (`TABLE_MS_4003`).

Success (200): `TableResponse`. Error (400): `TABLE_MS_2003`.

#### `PUT /api/table-ms/v1/tables/{id}/reservation`

- **Auth:** Bearer
- **Permission:** `table.reserve`

Request:
```json
{
  "guestName": "Səbinə",
  "phone": "+994 55 222 33 44",
  "time": "2026-08-07T19:00:00Z",
  "guestCount": 4,
  "notes": "Pəncərə yanı"
}
```

Success (200): `TableResponse` (rezerv dolu). Error (400): `TABLE_MS_4002`, `TABLE_MS_2005`.

#### `DELETE /api/table-ms/v1/tables/{id}/reservation`

- **Auth:** Bearer
- **Permission:** `table.reserve`

Success (200): `TableResponse` (rezerv silinmiş).

---

## 6. Order — `order-service` (port 8107)

> API prefix: `/api/order-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; key prefiksi `ORDER_MS_`
> Context path: `/api/order-ms`; gateway marşrutu: `/api/order-ms/**` → `http://localhost:8107`

### Tenant & Giriş Qayraları

- `GET /orders`, `GET /orders?orgId=...` — hər kəs (orqanizasiya filtirləməsi `orgId` query param vasitəsilə). Xarici servis çağırışları (customer-service, kitchen-service, waiter-service) üçün `X-Internal-Auth` kifayətdir.
- `GET /orders/{id}` — `findOrder` daxilində `SecurityContextFacade` ilə cross-tenant yoxlanışı: platform admin istənilən sifarişi görür, qeyri-admin yalnız öz `orgId`-sinə aid sifarişi.
- Bütün digər yazma əməliyyatları `Authorization: Bearer` + `@PreAuthorize` permission kodu ilə qorunur.
- Servis daxilində Feign çağırışları (`table-service`, `menu-service`, `setting-service`) `X-Internal-Auth` ilə işləyir.

### Sifariş Status Maşını

**OrderStatus**: `PENDING` → `CONFIRMED` → `PREPARING` → `READY` → `SERVED` → `COMPLETED`
- `PENDING`/`CONFIRMED`/`PREPARING`/`READY`/`SERVED` vəziyyətindən `CANCELLED` mümkündür (yalnız `CANCELLED`-ə keçid `cancelOrder` endpoint-i ilə).
- `COMPLETED` statusu `completePayment` ilə set olunur (ödəniş tamamlananda).
- `startPreparing` yalnız `CONFIRMED` statusdan `PREPARING`-ə keçir (PENDING sifarişi birbaşa PREPARING edə bilməz — ofisiant təsdiqi lazımdır).

**OrderItemStatus**: `PENDING` → `PREPARING` → `READY` → `SERVED`; `CANCELLED` (PENDING/CONFIRMED/PREPARING/READY-dən)
- Bütün item-lər `SERVED` olduqda order avtomatik `SERVED` olur.
- Bütün item-lər `READY`/`SERVED` olduqda order `READY` olur (əgər `PREPARING`dədirsə).
- Bütün item-lər `CANCELLED` olarsa order avtomatik `CANCELLED` olur.

**PaymentStatus**: `PENDING` → `PAID`; **PaymentMethod**: `CASH`, `CARD`; **OrderSource**: `WAITER`, `CUSTOMER`.

> `request-payment` sifarişi `paymentRequested=true` edir + `paymentMethod` set olunur. `complete-payment` ilə `paymentStatus=PAID`, order `COMPLETED` olur, masa `AVAILABLE` edilir.

### Order-servis Error Kodları

| Kod | HTTP | Açıqlama | İstifadə Yeri |
|---|---|---|---|
| `ORDER_MS_3001` | 404 | Sifariş tapılmadı | `findOrder`, cross-tenant yoxlanışı |
| `ORDER_MS_4001` | 400 | Qeyri-keçərli status keçidi | `validateStatusTransition`, `updateStatus`, `completePayment`, `startPreparing` |
| `ORDER_MS_4004` | 400 | Sifariş PENDING deyil | `waiterConfirm` |
| `ORDER_MS_4005` | 400 | Sifariş aktiv deyil (COMPLETED/CANCELLED) | `addItems`, `completePayment` |
| `ORDER_MS_4006` | 404 | Sifarişdəki item tapılmadı | `updateItemStatus` |
| `ORDER_MS_4007` | 400 | Qeyri-keçərli item status keçidi | `validateItemStatusTransition` |
| `ORDER_MS_4008` | 409 | Ödəniş artıq tamamlanıb | `completePayment` |
| `ORDER_MS_4009` | 400 | Sifariş ləğv oluna bilməz | `cancelOrder` (COMPLETED/CANCELLED sifariş) |
| `ORDER_MS_4011` | 400 | Masa mövcud deyil (dolu/silinmiş) | `createOrder` |
| `ORDER_MS_4012` | 400 | Menyu elementi tapılmadı | `createOrder`, `addItems` |
| `ORDER_MS_4013` | 400 | Menyu elementi mövcud deyil (qeyri-aktiv) | `createOrder`, `addItems` |

### Data Modelləri

**`OrderResponse`**: `id`(String), `tableId`(UUID), `tableNumber`(Integer), `items[]`(OrderItemResponse), `status`(OrderStatus string), `paymentStatus`(PaymentStatus string), `totalAmount`(BigDecimal), `waiterId`(UUID, optional), `waiterName`(String, optional), `orderSource`(OrderSource string), `waiterConfirmed`(boolean), `confirmedBy`(String, optional), `customerPhoto`(String, optional), `paymentMethod`(PaymentMethod string, optional), `paymentRequested`(boolean), `cancelReason`(String, optional), `orgId`(UUID), `createdAt`(Instant), `updatedAt`(Instant).

**`OrderItemResponse`**: `id`(String), `menuItemId`(UUID), `menuItemName`(String), `quantity`(Integer), `price`(BigDecimal), `notes`(String), `status`(OrderItemStatus string).

**`OrderRequest`** — yeni sifariş yaratma:
```json
{
  "orgId": "01234567-89ab-cdef-0123-456789abcdef",
  "tableId": "bbbbbbbb-0000-4000-8000-000000000201",
  "waiterId": "bbbbbbbb-0000-4000-8000-000000000002",
  "waiterName": "Aysel Məmmədova",
  "orderSource": "WAITER",
  "items": [
    {
      "menuItemId": "bbbbbbbb-0000-4000-8000-000000000101",
      "menuItemName": "Toyuq Doner",
      "quantity": 2,
      "price": 9.50,
      "notes": "Çox ədviyyatlı olmasın"
    }
  ],
  "customerPhoto": null,
  "paymentMethod": "CASH"
}
```

Validation: `orgId` (`@NotNull`), `tableId` (`@NotNull`), `orderSource` (`@NotBlank` + `@ValidEnum(OrderSource.class)`), `items` (`@NotEmpty` + `@Valid`). Hər item: `menuItemId` (`@NotNull`), `menuItemName` (`@NotBlank`), `quantity` (`@NotNull` + `@Min(1)`), `price` (`@NotNull` + `@DecimalMin("0.01")`).

**`AddItemsRequest`**:
```json
{
  "items": [
    { "menuItemId": "...", "menuItemName": "Qarışıq Doner", "quantity": 1, "price": 11.00 }
  ]
}
```

**`StatusRequest`**: `{ "status": "CONFIRMED" }` — `@NotBlank`.

**`WaiterConfirmRequest`**: `{ "waiterId": "...", "waiterName": "Aysel Məmmədova" }` — `waiterId` (`@NotNull`), `waiterName` (`@NotBlank`).

**`PaymentRequest`**: `{ "method": "CARD" }` — `@NotBlank` + `@ValidEnum(PaymentMethod.class)`.

**`CancelRequest`** (optional body): `{ "reason": "Müştəri imtina etdi" }`.

### Endpoints

#### `GET /api/order-ms/v1/orders`

**Sifarişləri siyahıla** (filterlənə bilər).

- **Auth:** Bearer (xarici servis — `X-Internal-Auth`)
- Query: `orgId` (UUID, **required**), `status` (optional — `PENDING`/`CONFIRMED`/`PREPARING`/`READY`/`SERVED`/`COMPLETED`/`CANCELLED`), `tableId` (UUID, optional), `waiterId` (UUID, optional)
- Xidmət: `getOrders(orgId, status, tableId, waiterId)` → `orgId` ilə filterlənir (tenant isolation)

Success (200):
```json
{
  "data": [
    {
      "id": "ORDER-000123",
      "tableId": "bbbbbbbb-0000-4000-8000-000000000201",
      "tableNumber": 5,
      "items": [
        { "id": "...", "menuItemId": "...", "menuItemName": "Toyuq Doner", "quantity": 2, "price": 9.50, "notes": "", "status": "PENDING" }
      ],
      "status": "PENDING",
      "paymentStatus": "PENDING",
      "totalAmount": 19.00,
      "waiterId": "...",
      "waiterName": "Aysel",
      "orderSource": "WAITER",
      "waiterConfirmed": true,
      "confirmedBy": null,
      "customerPhoto": null,
      "paymentMethod": null,
      "paymentRequested": false,
      "cancelReason": null,
      "orgId": "01234567-89ab-cdef-0123-456789abcdef",
      "createdAt": "2026-08-01T10:00:00Z",
      "updatedAt": "2026-08-01T10:00:00Z"
    }
  ]
}
```

#### `GET /api/order-ms/v1/orders/{id}`

**Tək sifarişin detalları.**

- **Auth:** Bearer (xarici servis — `X-Internal-Auth`)
- Cross-tenant: platform admin istənilən sifarişi görür; qeyri-admin yalnız öz org-unun sifarişini

Success (200): `OrderResponse` (yuxarıdakı kimi).
Error (404): `ORDER_MS_3001` — sifariş tapılmadı və ya başqa org-a aiddir.

#### `POST /api/order-ms/v1/orders`

**Yeni sifariş yarat.** Masa `AVAILABLE` olmalıdır, menyu elementləri mövcud və aktiv olmalıdır.

- **Auth:** Bearer (xarici servis — `X-Internal-Auth`)
- Request: `OrderRequest` (`@Valid`)
- Business logic:
  1. Masa `tableServiceClient.getTable(tableId)` → status `AVAILABLE` deyilsə `ORDER_MS_4011`
  2. Menyu `menuServiceClient.getItems(orgId)` → hər item `menuItemMap`-da olmalıdır (`ORDER_MS_4012`), `isAvailable=true` olmalıdır (`ORDER_MS_4013`)
  3. `settingServiceClient.getSettings(orgId)` → `orderMode` (`CUSTOMER_WAITER_CONFIRM` → `PENDING`, əks halda `CONFIRMED`) + `paymentTiming` (`BEFORE` → `PAID`, əks halda `PENDING`)
  4. Masa `OCCUPIED` edilir (table-service `updateTableStatus`)
- Tenant: `orgId` request-dən götürülür

Success (201):
```json
{
  "data": {
    "id": "ORDER-000123",
    "status": "CONFIRMED",
    "paymentStatus": "PENDING",
    "totalAmount": 19.00,
    ...
  },
  "message": "Order created"
}
```
Error (400): `ORDER_MS_4011`, `ORDER_MS_4012`, `ORDER_MS_4013`.

#### `PUT /api/order-ms/v1/orders/{id}/status`

**Sifariş statusunu dəyiş.** Status keçidi validasiya olunur.

- **Auth:** Bearer
- **Permission:** `order.manage`
- Request: `{ "status": "CONFIRMED" }` — `@Valid` `StatusRequest` (`@NotBlank status`)
- `CANCELLED` statusu bu endpoint-dən keçmir (`ORDER_MS_4009`)

Success (200): `OrderResponse`.
Error (400): `ORDER_MS_4001`.

#### `PUT /api/order-ms/v1/orders/{id}/items/{itemId}/status`

**Tək sifariş maddəsinin statusunu dəyiş.** Status keçidi validasiya olunur, sonra order statusu avtomatik yenilənir (`updateOrderStatusFromItems`).

- **Auth:** Bearer
- **Permission:** `order.manage`
- Request: `{ "status": "READY" }` — `@Valid` `StatusRequest`
- Item status keçid qaydası: `PENDING→PREPARING`, `CONFIRMED→PREPARING`, `PREPARING→READY`, `READY→SERVED`; hər birindən `CANCELLED` mümkündür

Success (200): `OrderResponse`.
Error (404): `ORDER_MS_4006`. Error (400): `ORDER_MS_4007`.

#### `POST /api/order-ms/v1/orders/{id}/items`

**Mövcud sifarişə yeni maddələr əlavə et.** Menyu validasiyası tətbiq olunur.

- **Auth:** Bearer
- **Permission:** `order.manage`
- Request: `AddItemsRequest` (`@NotEmpty` + `@Valid` items)
- Hər əlavə olunan item üçün menyuda mövcudluq və aktivlik yoxlanır (`ORDER_MS_4012`, `ORDER_MS_4013`)
- `COMPLETED`/`CANCELLED` sifarişə əlavə etmək olmaz (`ORDER_MS_4005`)
- `totalAmount` yenidən hesablanır

Success (200): `OrderResponse`.
Error (400): `ORDER_MS_4005`, `ORDER_MS_4012`, `ORDER_MS_4013`.

#### `PUT /api/order-ms/v1/orders/{id}/waiter-confirm`

**Ofisiant sifarişi təsdiqləyir** (CUSTOMER rejimində PENDING → CONFIRMED).

- **Auth:** Bearer
- **Permission:** `order.manage`
- Request: `{ "waiterId": "...", "waiterName": "Aysel Məmmədova" }` — `@Valid`
- Yalnız `PENDING` statuslu + `orderSource=CUSTOMER` sifarişlər təsdiqlənə bilər

Success (200): `OrderResponse` (`waiterConfirmed=true`, `status=CONFIRMED`).
Error (400): `ORDER_MS_4004` (PENDING deyilsə), `ORDER_MS_4001` (source CUSTOMER deyilsə).

#### `POST /api/order-ms/v1/orders/{id}/cancel`

**Sifarişi ləğv et.** Yalnız aktiv sifarişlər ləğv oluna bilər.

- **Auth:** Bearer
- **Permission:** `order.cancel`
- Request (optional): `{ "reason": "Müştəri imtina etdi" }`
- `COMPLETED`/`CANCELLED` sifariş ləğv oluna bilməz (`ORDER_MS_4009`)
- Masada başqa aktiv sifariş yoxdursa masa `CLEANING` edilir (`tableServiceClient.updateTableStatus`)

Success (200): `OrderResponse` (`status=CANCELLED`, `cancelReason` dolu).
Error (400): `ORDER_MS_4009`.

#### `POST /api/order-ms/v1/orders/{id}/request-payment`

**Hesab istə** (müştəri və ya ofisiant tərəfindən). `paymentRequested=true` + `paymentMethod` set olunur.

- **Auth:** Bearer
- **Permission:** `order.payment`
- Request: `{ "method": "CARD" }` — `@Valid` `PaymentRequest` (`@NotBlank` + `@ValidEnum(PaymentMethod.class)`)

Success (200): `OrderResponse` (`paymentRequested=true`).

#### `POST /api/order-ms/v1/orders/{id}/complete-payment`

**Ödənişi tamamla.** `paymentStatus=PAID`, order `COMPLETED`, masa `AVAILABLE`.

- **Auth:** Bearer
- **Permission:** `order.payment`
- Request: *empty body*
- Artıq `PAID` sifarişə təkrar ödəniş olmaz (`ORDER_MS_4008`)
- `COMPLETED`/`CANCELLED` sifarişə ödəniş olmaz (`ORDER_MS_4005`)

Success (200): `OrderResponse` (`paymentStatus=PAID`, `status=COMPLETED`).
Error (400): `ORDER_MS_4005`. Error (409): `ORDER_MS_4008`.

#### `POST /api/order-ms/v1/orders/{id}/start-preparing`

**Sifarişi hazırlanmaya başla.** Yalnız `CONFIRMED` sifarişlər.

- **Auth:** Bearer
- **Permission:** `order.manage`
- Bütün `PENDING`/`CONFIRMED` item-lər `PREPARING` edilir, order `PREPARING` olur
- `PENDING` sifarişi birbaşa hazırlanmaya başlamaq olmaz — əvvəlcə ofisiant təsdiqi lazımdır

Success (200): `OrderResponse` (`status=PREPARING`).
Error (400): `ORDER_MS_4001`.

#### `POST /api/order-ms/v1/orders/{id}/mark-all-ready`

**Bütün hazırlanan item-ləri hazır et.** Yalnız `PREPARING` item-lər `READY` edilir.

- **Auth:** Bearer
- **Permission:** `order.manage`
- Yalnız `PREPARING` statuslu item-lər `READY` olur (artıq `READY`/`SERVED` olanlar dəyişmir)

Success (200): `OrderResponse` (`status=READY`).

### Servisdaxili Feign Əlaqələri

| Target | Metod | Məqsəd |
|---|---|---|
| `table-service` | `GET /tables/{id}` | Masa məlumatı (status, nömrə) |
| `table-service` | `PUT /tables/{id}/status` | Masa statusu yeniləmə (`OCCUPIED`, `CLEANING`, `AVAILABLE`) |
| `menu-service` | `GET /items?orgId=` | Menyu elementlərinin mövcudluq/aktivlik yoxlanışı |
| `setting-service` | `GET /settings?orgId=` | Org ayarları (`orderMode`, `paymentTiming`) |

Bütün Feign çağırışlarında `unwrap()` köməkçisi ilə response yoxlanılır (`success=true`, `data != null`). Xəta halında `RuntimeException` atılır.

### Dizayn Qərarları

- `updateOrderStatusFromItems`: bütün non-cancelled item-lər `SERVED` → order `SERVED`; hamısı `READY`/`SERVED` → order `READY`; hamısı `CANCELLED` → order `CANCELLED`.
- `startPreparing`: PENDING sifarişdən birbaşa PREPARING-ə keçid yoxdur — yalnız CONFIRMED-dən. Bu, CUSTOMER_WAITER_CONFIRM modunda ofisiant təsdiqini məcburi edir.
- `cancelOrder`: digər aktiv sifariş varsa masa `CLEANING` edilmir — çoxlu sifariş ssenarisi dəstəklənir.
- Cross-tenant: `findOrder` platform admin üçün skip, qeyri-admin üçün `SecurityContextFacade.getCurrentOrgId()` ilə yoxlanır.
- `OrderItem.status` — `OrderItemStatus` enum-u (PENDING, CONFIRMED, PREPARING, READY, SERVED, CANCELLED), DB-də `@Enumerated(EnumType.STRING)`.
- `Order.status` — `OrderStatus` enum-u, `Order.paymentStatus` — `PaymentStatus`, `Order.orderSource` — `OrderSource`.

---

## 7. Kitchen — `kitchen-service` (port 8108)

> API prefix: `/api/kitchen-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; servis-specific `KitchenErrorCode` yoxdur — bütün xətalar **upstream** (`order-ms`, `ORDER_MS_*`) və ya ümumi kodlardan (`*_1000`, `*_4001`, `*_4003`, `*_9999`) gəlir
> Context path: `/api/kitchen-ms`; gateway marşrutu: `/api/kitchen-ms/**` → `http://localhost:8108`

### Tenant & Giriş Qayraları

- Bütün endpoint-lər `Authorization: Bearer` tələb edir və `@PreAuthorize` permission kodu ilə qorunur.
- Non-admin istifadəçi yalnız öz org-unun sifarişlərini görür; başqa org → **403** (`*_4003`).

### Data Modelləri

**`KitchenOrderResponse`**: `id`(String), `items[]`, `tableId`, `tableNumber`, `status`, `paymentStatus`, `totalAmount`, `waiterName`, `orderSource`, `createdAt`(Instant).

**`KitchenItemResponse`**: `id`(String), `menuItemId`, `menuItemName`, `quantity`, `price`, `notes`, `status`.

**`KitchenOrderGroup`** (kitchen xüsusi): sifarişləri statusa görə qruplaşdırılmış wrapper.

### Endpoints

#### `GET /api/kitchen-ms/v1/orders`

- **Auth:** Bearer
- **Permission:** `kitchen.view`
- Query: `orgId` (required)

Məntiq: `order-ms`-dən **PREPARING / READY** sifarişlər götürülür, statusa görə qruplaşdırılır.

Success (200): `KitchenOrderGroup`:
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "preparing": [ /* KitchenOrderResponse[] */ ],
    "ready": [ /* KitchenOrderResponse[] */ ]
  }
}
```

> Qeyd: bu endpoint `KitchenService` vasitəsilə `order-ms`-ə **upstream** çağırış edir; `order-ms` əlçatmaz olarsa 502/503.

---

## 8. Waiter — `waiter-service` (port 8109)

> API prefix: `/api/waiter-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; key prefiksi `WAITER_MS_`
> Context path: `/api/waiter-ms`; gateway marşrutu: `/api/waiter-ms/**` → `http://localhost:8109`

### Tenant & Giriş Qayraları

- Bütün endpoint-lər `Authorization: Bearer` tələb edir və `@PreAuthorize` permission kodu ilə qorunur.
- Non-admin istifadəçi yalnız öz org-unun məlumatlarına girişir; başqa org → **403** (`WAITER_MS_3003`).
- Bu servis yalnız **oxuma** (read) əməliyyatları edir; yazma əməliyyatları `order-ms`/`table-ms` üzərindədir.

### Waiter-servis Error Kodları

| Kod | HTTP | Açıqlama |
|---|---|---|
| `WAITER_MS_3003` | 403 | Giriş qadağandır |
| `WAITER_MS_9001` | 503 | Upstream servis əlçatmaz |
| `WAITER_MS_9002` | 502 | Upstream servis xətası |

### Data Modelləri

**`WaiterTablesWrapper`**: masalar (status daxil) + tələb olunan köməkçi məlumat (məs. aktiv sifariş varlığı) ilə wrapper.

**`WaiterOrderResponse`**: `id`(String), `tableId`, `tableNumber`, `items[]`, `status`, `paymentStatus`, `totalAmount`, `customerPhoto`, `paymentMethod`, `waiterConfirmed`, `paymentRequested`, `cancelReason`, `orgId`, `createdAt`, `updatedAt`.

### Endpoints

#### `GET /api/waiter-ms/v1/tables`

- **Auth:** Bearer
- **Permission:** `waiter.view`
- Query: `orgId` (required)

Məntiq: `table-ms`-dən masalar götürülür, hər masada aktiv sifariş varmı işarələnir.

Success (200): `WaiterTablesWrapper`:
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "tables": [
      {
        "id": "bbbbbbbb-0000-4000-8000-000000000201",
        "tableNumber": 1,
        "capacity": 4,
        "status": "OCCUPIED",
        "sectionId": "aaaaaaaa-0000-4000-8000-000000000001",
        "activeOrder": true,
        "orgId": "01234567-89ab-cdef-0123-456789abcdef"
      }
    ]
  }
}
```

#### `GET /api/waiter-ms/v1/orders/pending-confirm`

- **Auth:** Bearer
- **Permission:** `waiter.view`
- Query: `orgId` (required)

Məntiq: `order-ms`-dən **PENDING + waiterConfirmed=false** sifarişlər (ofisiant təsdiqi gözləyənlər).

Success (200): `WaiterOrderResponse[]`.

#### `GET /api/waiter-ms/v1/orders/payment-requests`

- **Auth:** Bearer
- **Permission:** `waiter.view`
- Query: `orgId` (required)

Məntiq: `order-ms`-dən **paymentRequested=true + paymentStatus=PENDING** sifarişlər (müştəri hesab istəyib).

Success (200): `WaiterOrderResponse[]`.

---

## 9. Customer — `customer-service` (port 8110)

> API prefix: `/api/customer-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; servis-specific `CustomerErrorCode` yoxdur — xətalar ümumi kodlardan (`*_1000` validation, `*_3001` 404, `*_9999` 500) və ya **upstream** (`MENU_MS_*`, `TABLE_MS_*`, `ORDER_MS_*`) gəlir
> Context path: `/api/customer-ms`; gateway marşrutu: `/api/customer-ms/**` → `http://localhost:8110`

### Giriş Qayraları

- **Auth tələb olunmur** — bu bölmə müştəri (QR skan edərək) tərəfindən istifadə olunur. Gateway-də `/api/customer-ms/**` **permitAll**.
- Bütün əməliyyatlar `orgId` (path/query) üzərindən tenant-a bağlıdır.

### Data Modelləri

**`CustomerMenuResponse`**: `categories[]`, `items[]`.
- **`CategoryResponse`** (customer): `id`, `name`(`LocalizedString`), `icon`.
- **`ItemResponse`** (customer): `id`, `name`(`LocalizedString`), `description`(`LocalizedString`), `price`, `categoryId`, `imageUrl`, `@JsonProperty("isAvailable")`, `preparationTime`.

**`CustomerOrderRequest`**:
```json
{
  "orgId": "01234567-89ab-cdef-0123-456789abcdef",
  "tableId": "bbbbbbbb-0000-4000-8000-000000000201",
  "items": [
    { "menuItemId": "bbbbbbbb-0000-4000-8000-000000000101", "menuItemName": "Toyuq Doner", "quantity": 2, "price": 9.50, "notes": "" }
  ],
  "customerPhoto": null,
  "paymentMethod": "CARD"
}
```

**`CustomerOrderResponse`**: müştəriyə qaytarılan yığcam sifariş modeli (`id`, `status`, `totalAmount`, `items[]`, ...).

### Endpoints

#### `GET /api/customer-ms/v1/{orgId}/menu`

- **Auth:** public
- Path: `orgId`

Müştəri menyusu (yalnız **isAvailable=true** item-lər).

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "categories": [ { "id": "aaaaaaaa-0000-4000-8000-000000000001", "name": { "az": "Qrilla", "en": "Grill", "ru": "Гриль" }, "icon": "🔥" } ],
    "items": [ { "id": "bbbbbbbb-0000-4000-8000-000000000101", "name": { "az": "Toyuq Doner", "en": "Chicken Doner", "ru": "Куриный донер" }, "price": 9.50, "isAvailable": true, "preparationTime": 15 } ]
  }
}
```

#### `GET /api/customer-ms/v1/{orgId}/tables`

- **Auth:** public
- Path: `orgId`

QR-dakı masanın etibarlılığını yoxlamaq üçün. Success (200): `CustomerTableResponse[]` (yığcam masa modeli: `id`, `tableNumber`, `status`).

#### `POST /api/customer-ms/v1/orders`

- **Auth:** public

Müştəri sifarişi yaradır (`order-ms`-ə upstream, `orderSource=CUSTOMER`).

Request: `CustomerOrderRequest`. Success (201): `CustomerOrderResponse` (`status=PENDING`).

#### `GET /api/customer-ms/v1/orders/{orderId}`

- **Auth:** public

Müştəri sifarişinin statusunu izləyir. Success (200): `CustomerOrderResponse`.

#### `POST /api/customer-ms/v1/orders/{orderId}/request-bill`

- **Auth:** public

Request:
```json
{ "method": "CASH" }
```

Success (200): `CustomerOrderResponse` (`paymentRequested=true` — ofisiant təsdiqi gözləyir).

---

## 10. Settings — `setting-service` (port 8111)

> API prefix: `/api/setting-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; key prefiksi `SETTING_MS_`
> Context path: `/api/setting-ms`; gateway marşrutu: `/api/setting-ms/**` → `http://localhost:8111`

### Tenant & Giriş Qayraları

- Bütün endpoint-lər `Authorization: Bearer` tələb edir və `@PreAuthorize` permission kodu ilə qorunur.
- Non-admin istifadəçi yalnız öz org-unun parametrlərini görür/dəyişir; başqa org → **403** (`*_4003`).

### Setting-servis Error Kodları

> `SettingErrorCode` enum-ından.

| Kod | HTTP | Açıqlama |
|---|---|---|
| `SETTING_MS_3001` | 404 | Parametrlər tapılmadı (`SETTINGS_NOT_FOUND`) |
| `SETTING_MS_4001` | 400 | Org tapılmadı (`ORGANIZATION_NOT_FOUND`) |

### Data Modelləri

**`SettingRequest`**: `orgId` (required), parametrlər (org başına yeganə sənəd) — məs. `restaurantName`, `currency`, `taxRate`, `serviceChargeRate`, `workingHours`, `logoUrl`, `contactPhone` (bütün sahələr optional, ortaq `Setting` modeli).

**`SettingResponse`**: `orgId` + parametrlər (yuxarıdakı struktura uyğun).

### Endpoints

#### `GET /api/setting-ms/v1/settings`

- **Auth:** Bearer
- **Permission:** `settings.view`
- Query: `orgId` (required)

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "orgId": "01234567-89ab-cdef-0123-456789abcdef",
    "restaurantName": "RestoFlow Demo",
    "currency": "AZN",
    "taxRate": 0.18,
    "serviceChargeRate": 0.10,
    "logoUrl": "https://cdn.example.com/logo.png",
    "contactPhone": "+994 12 000 00 00"
  }
}
```

#### `PUT /api/setting-ms/v1/settings`

- **Auth:** Bearer
- **Permission:** `settings.edit`

Request: `SettingRequest` (dəyişdiriləcək sahələr). Success (200): `SettingResponse`.

---

## 11. Dashboard — `dashboard-service` (port 8112)

> API prefix: `/api/dashboard-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; servis-specific `DashboardErrorCode` yoxdur — bütün xətalar **upstream** (`ORDER_MS_*`, `ACCESS_MS_*`) və ya ümumi kodlardan (`*_4001`, `*_4003`, `*_9999`) gəlir
> Context path: `/api/dashboard-ms`; gateway marşrutu: `/api/dashboard-ms/**` → `http://localhost:8112`

### Tenant & Giriş Qayraları

- Bütün endpoint-lər `Authorization: Bearer` tələb edir və `@PreAuthorize` permission kodu ilə qorunur.
- Non-admin istifadəçi yalnız öz org-unun statistika məlumatlarına girişir; başqa org → **403** (`*_4003`).
- Bu servis **oxuma** üçündür; bütün məlumat `order-ms`/`staff` üzərindən **upstream** toplanır.

### Data Modelləri

**`DashboardStatsResponse`**: bugünkü ümumi statistikalar — məs. `totalOrders`, `totalRevenue`, `averageOrderValue`, `activeOrders`, `cancelledOrders`, `topCategory`.

**`RecentOrderResponse`**: yığcam sifariş sətri — `id`, `tableNumber`, `totalAmount`, `status`, `paymentStatus`, `createdAt`.

**`StaffListResponse`**: işçi xülasəsi — `userId`, `name`, `role`, `activeOrders`, `completedOrders`.

**`TopItemResponse`**: ən çox satılan — `menuItemId`, `menuItemName`, `quantity`, `revenue`.

### Endpoints

#### `GET /api/dashboard-ms/v1/stats`

- **Auth:** Bearer
- **Permission:** `dashboard.view`
- Query: `orgId` (required)

Success (200): `DashboardStatsResponse`:
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "totalOrders": 42,
    "totalRevenue": 785.50,
    "averageOrderValue": 18.70,
    "activeOrders": 6,
    "cancelledOrders": 2,
    "topCategory": "Qrilla"
  }
}
```

#### `GET /api/dashboard-ms/v1/top-items`

- **Auth:** Bearer
- **Permission:** `dashboard.view`
- Query: `orgId` (required), `limit` (optional, default 5)

Success (200): `TopItemResponse[]`.

#### `GET /api/dashboard-ms/v1/recent-orders`

- **Auth:** Bearer
- **Permission:** `dashboard.view`
- Query: `orgId` (required), `limit` (optional)

Success (200): `RecentOrderResponse[]`.

#### `GET /api/dashboard-ms/v1/staff-list`

- **Auth:** Bearer
- **Permission:** `dashboard.view`
- Query: `orgId` (required)

Success (200): `StaffListResponse[]`.

---

## 12. Reports — `report-service` (port 8113)

> API prefix: `/api/report-ms/v1/...`
> Response: `ApiResponse<T>` wrapper
> Error: Spring `ProblemDetail`; servis-specific `ReportErrorCode` yoxdur — bütün xətalar **upstream** (`ORDER_MS_*`, `ACCESS_MS_*`) və ya ümumi kodlardan (`*_4001`, `*_4003`, `*_9999`) gəlir
> Context path: `/api/report-ms`; gateway marşrutu: `/api/report-ms/**` → `http://localhost:8113`

### Tenant & Giriş Qayraları

- Bütün endpoint-lər `Authorization: Bearer` tələb edir və `@PreAuthorize` permission kodu ilə qorunur.
- Non-admin istifadəçi yalnız öz org-unun hesabatlarına girişir; başqa org → **403** (`*_4003`).
- Tarix aralığı query ilə: `startDate`, `endDate` (ISO `LocalDate`, optional — göstərilməsə **bugün**). Bu servis **oxuma** üçündür.

### Data Modelləri

**`SummaryResponse`**: `totalRevenue`, `totalOrders`, `averageOrderValue`, `totalItemsSold`, `completedOrders`, `cancelledOrders`.

**`DailyRevenueResponse`**: `date`, `revenue`, `orderCount`.

**`HourlyResponse`**: `hour` (0-23), `revenue`, `orderCount`.

**`SalesByCategoryResponse`**: `categoryName`, `quantity`, `revenue`.

**`TopItemResponse`** (report): `menuItemId`, `menuItemName`, `quantity`, `revenue`.

**`StaffPerformanceResponse`**: `userId`, `name`, `role`, `totalOrders`, `completedOrders`, `revenue`, `activeOrders`.

### Endpoints

#### `GET /api/report-ms/v1/summary`

- **Auth:** Bearer
- **Permission:** `report.view`
- Query: `orgId` (required), `startDate`, `endDate` (optional)

Success (200):
```json
{
  "success": true,
  "message": "Success",
  "errorCode": null,
  "data": {
    "totalRevenue": 4520.00,
    "totalOrders": 240,
    "averageOrderValue": 18.83,
    "totalItemsSold": 610,
    "completedOrders": 228,
    "cancelledOrders": 12
  }
}
```

#### `GET /api/report-ms/v1/daily-revenue`

- **Auth:** Bearer
- **Permission:** `report.view`
- Query: `orgId` (required), `startDate`, `endDate` (optional)

Success (200): `DailyRevenueResponse[]`:
```json
{ "data": [ { "date": "2026-08-06", "revenue": 1520.50, "orderCount": 80 } ] }
```

#### `GET /api/report-ms/v1/hourly`

- **Auth:** Bearer
- **Permission:** `report.view`
- Query: `orgId` (required), `startDate`, `endDate` (optional)

Success (200): `HourlyResponse[]`:
```json
{ "data": [ { "hour": 13, "revenue": 210.00, "orderCount": 12 } ] }
```

#### `GET /api/report-ms/v1/sales-by-category`

- **Auth:** Bearer
- **Permission:** `report.view`
- Query: `orgId` (required), `startDate`, `endDate` (optional)

Success (200): `SalesByCategoryResponse[]`:
```json
{ "data": [ { "categoryName": "Qrilla", "quantity": 180, "revenue": 1650.00 } ] }
```

#### `GET /api/report-ms/v1/top-items`

- **Auth:** Bearer
- **Permission:** `report.view`
- Query: `orgId` (required), `startDate`, `endDate` (optional), `limit` (optional, default 10)

Success (200): `TopItemResponse[]`.

#### `GET /api/report-ms/v1/staff-performance`

- **Auth:** Bearer
- **Permission:** `report.view`
- Query: `orgId` (required), `startDate`, `endDate` (optional)

Success (200): `StaffPerformanceResponse[]`.

---

## 13. Microservice Architecture & Port Plan

```
                         ┌──────────────────────────────┐
      Client (Web/QR)    │        cloud-gateway          │   :8001
      ──────────────────►│  (Spring Cloud Gateway)       │
                         └──────────────┬───────────────┘
                                        │ routes by context path
        ┌───────────┬───────────┬───────┴────────┬───────────┬───────────┐
        ▼           ▼           ▼                ▼           ▼           ▼
   :8002       :8102      :8105..:8113       :8120        Keycloak    Postgres/
  auth-ms    org-ms      order/table/menu/    access-ms    (:8443)    Mongo/Redis
 (gateway)               kitchen/waiter/                  (resto realm)
                         customer/setting/
                         dashboard/report
```

| Service | Module | Port | Context path | Service key |
|---|---|---|---|---|
| `cloud-gateway` | Gateway (Spring Cloud) | 8001 | `/` | `GATEWAY` |
| `auth-gateway` | Auth (Keycloak proxy) | 8002 | `/api/auth-ms` | `AUTH` |
| `access-service` | Users / Roles / Permissions | 8120 | `/api/access-ms` | `ACCESS_MS` |
| `organization-service` | Organization + Org admin bootstrap | 8102 | `/api/organization-ms` | `ORG` |
| `menu-service` | Menu categories & items | 8105 | `/api/menu-ms` | `MENU_MS` |
| `table-service` | Tables & sections | 8106 | `/api/table-ms` | `TABLE_MS` |
| `order-service` | Orders & payments | 8107 | `/api/order-ms` | `ORDER_MS` |
| `kitchen-service` | Kitchen panel (read) | 8108 | `/api/kitchen-ms` | `KITCHEN_MS` |
| `waiter-service` | Waiter panel (read) | 8109 | `/api/waiter-ms` | `WAITER_MS` |
| `customer-service` | Customer QR flow | 8110 | `/api/customer-ms` | `CUSTOMER_MS` |
| `setting-service` | Org settings | 8111 | `/api/setting-ms` | `SETTING_MS` |
| `dashboard-service` | Dashboard stats | 8112 | `/api/dashboard-ms` | `DASHBOARD_MS` |
| `report-service` | Reports | 8113 | `/api/report-ms` | `REPORT_MS` |

### Autentifikasiya axını

1. Client `POST /api/auth-ms/v1/auth/login` → auth-gateway `KeycloakClient.login()` → Keycloak token endpoint-i.
2. `JwtTokenValidator.extractClaims()` → claims `sub`, `organizationId`, `roles`, `permissions`, `uiScope` parse olunur.
3. auth-gateway cavabı: `accessToken`, `refreshToken`, `expiresIn`, `tokenType`, `user`, `uiScope`, `permissions`.
4. Sonrakı bütün sorğularda client `Authorization: Bearer <accessToken>` göndərir; cloud-gateway tokeni doğrulayır.
5. cloud-gateway-in `ClaimsForwardingFilter`-i claim-ləri header kimi downstream-ə ötürür: `X-User-Id`, `X-Org-Id`, `X-Roles`, `X-Permissions`, `X-UI-Scope`, `X-Platform-Admin`, `X-Internal-Auth`.
6. Hər microservice `HeaderAuthenticationFilter`-i bu header-lərdən `UserPrincipal` qurur; `@PreAuthorize("@perm.has('...')")` ilə icazə yoxlanır.

### Request axını (tenant + permission)

```
Client ──Bearer JWT──► Gateway ──► Service
                        │
                        ├─ X-Org-Id  → Service org-id ilə filterləyir (tenant izolyasiya)
                        ├─ X-Platform-Admin → true → bütün org-lara giriş
                        └─ X-Permissions → @perm.has() yoxlaması (DB fallback)
```

- **Platform admin** (`SUPER_ADMIN` rollu) bütün permission yoxlamalarını bypass edir və istənilən org üzərində işləyir.
- **ORG_ADMIN** yalnız öz org-u üzərində — bütün `organization.view/create/edit/delete`, `staff.*`, `role.*`, `permission.*`, `settings.*` (ümumilikdə 38 perm) icazəsinə sahibdir.

### Upstream (feign) əlaqələri

| Service | Çağırır | Port |
|---|---|---|
| `organization-service` | `access-service` (role assign), `setting-service`, `table-service`, `order-service` | 8120, 8111, 8106, 8107 |
| `kitchen-service` | `order-service` (PREPARING/READY sifarişlər) | 8107 |
| `waiter-service` | `table-service`, `order-service` | 8106, 8107 |
| `customer-service` | `menu-service`, `table-service`, `order-service` | 8105, 8106, 8107 |
| `dashboard-service` | `order-service`, `access-service` (staff) | 8107, 8120 |
| `report-service` | `order-service`, `access-service` | 8107, 8120 |

---

## 14. Complete API Route Index (by module)

> Bütün sorğular (public qeyd olunanlar istisna) `Authorization: Bearer` tələb edir. `Perm` sütunu tələb olunan `@PreAuthorize` kodudur.

### Auth — auth-gateway (:8002)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| POST | `/api/auth-ms/v1/auth/login` | public | – | Giriş → JWT + uiScope + permissions |
| POST | `/api/auth-ms/v1/auth/refresh` | public | – | Refresh token |
| POST | `/api/auth-ms/v1/auth/logout` | Bearer | – | Çıxış (refresh token) |

### Access — access-service (:8120)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/access-ms/v1/users` | Bearer | `staff.view` | İşçi siyahısı (orgId, roleId, q, page, size) |
| GET | `/api/access-ms/v1/users/{id}` | Bearer | `staff.view` | İşçi detalları |
| POST | `/api/access-ms/v1/users` | Bearer | `staff.create` | İşçi yarat (Keycloak + DB) |
| PUT | `/api/access-ms/v1/users/{id}` | Bearer | `staff.edit` | İşçi redaktə |
| DELETE | `/api/access-ms/v1/users/{id}` | Bearer | `staff.delete` | Soft-delete (Keycloak da silinir) |
| DELETE | `/api/access-ms/v1/users/{id}/role` | Bearer | `role.assign` | İşçidən rol götür |
| GET | `/api/access-ms/v1/users/staff-performance` | Bearer | `staff.view` | İşçi performans xülasəsi |
| GET | `/api/access-ms/v1/roles` | Bearer | `role.view` | Rol siyahısı (q, page, size) |
| GET | `/api/access-ms/v1/roles/{id}` | Bearer | `role.view` | Rol detalları (+permissions) |
| POST | `/api/access-ms/v1/roles` | Bearer | `role.create` | Rol yarat |
| PUT | `/api/access-ms/v1/roles/{id}` | Bearer | `role.edit` | Rol redaktə |
| DELETE | `/api/access-ms/v1/roles/{id}` | Bearer | `role.delete` | Soft-delete (sistem roluna 403) |
| GET | `/api/access-ms/v1/roles/system/{code}` | Bearer | `role.view` | Sistem rolunu koda görə al |
| POST | `/api/access-ms/v1/roles/{id}/permissions` | Bearer | `role.edit` | Rol-a permission təyin et |
| PUT | `/api/access-ms/v1/roles/{id}/permissions` | Bearer | `role.edit` | Rol permission-larını tam dəyiş |
| DELETE | `/api/access-ms/v1/roles/{id}/permissions/{permissionId}` | Bearer | `role.edit` | Bir permission-u sil |
| POST | `/api/access-ms/v1/roles/{id}/users` | Bearer | `role.assign` | Rol-a istifadəçilər təyin et |
| GET | `/api/access-ms/v1/roles/{id}/users` | Bearer | `role.view` | Rolun istifadəçiləri |
| DELETE | `/api/access-ms/v1/roles/{id}/users/{userId}` | Bearer | `role.assign` | İstifadəçini roldan çıxar |
| GET | `/api/access-ms/v1/permissions/my` | Bearer | (isAuthenticated) | Cari istifadəçinin permission-ları |
| GET | `/api/access-ms/v1/permissions` | Bearer | `permission.view` | Permission kataloqu (q, module, uiGroup, page, size) |
| GET | `/api/access-ms/v1/permissions/tree` | Bearer | `permission.view` | Module→UI Group→Permission ağacı |
| GET | `/api/access-ms/v1/permissions/by-module` | Bearer | `permission.view` | Modula görə permission-lar |
| GET | `/api/access-ms/v1/permissions/by-ui-group` | Bearer | `permission.view` | UI Group-a görə permission-lar |
| GET | `/api/access-ms/v1/modules` | Bearer | `permission.view` | Modul siyahısı |
| GET | `/api/access-ms/v1/ui-groups` | Bearer | `permission.view` | UI Group siyahısı |

### Organization — organization-service (:8102)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/organization-ms/v1/organizations` | Bearer | `organization.view` | Org siyahısı |
| POST | `/api/organization-ms/v1/organizations` | Bearer | `organization.create` | Org + ORG_ADMIN yarat |
| GET | `/api/organization-ms/v1/organizations/{orgId}` | Bearer | `organization.view` | Org detalları |
| GET | `/api/organization-ms/v1/organizations/{orgId}/qr-code` | Bearer | `organization.view` | QR URL (müştəri paneli) |

### Menu — menu-service (:8105)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/menu-ms/v1/categories` | Bearer | `menu.view` | Kateqoriyalar (orgId) |
| POST | `/api/menu-ms/v1/categories` | Bearer | `menu.create` | Kateqoriya yarat |
| PUT | `/api/menu-ms/v1/categories/{id}` | Bearer | `menu.edit` | Kateqoriya redaktə |
| DELETE | `/api/menu-ms/v1/categories/{id}` | Bearer | `menu.delete` | Kateqoriya sil (moveItemsTo) |
| GET | `/api/menu-ms/v1/items` | Bearer | `menu.view` | Menyu item-ləri (orgId, categoryId, q, page, size) |
| GET | `/api/menu-ms/v1/items/{id}` | Bearer | `menu.view` | Item detalları |
| POST | `/api/menu-ms/v1/items` | Bearer | `menu.create` | Item yarat |
| PUT | `/api/menu-ms/v1/items/{id}` | Bearer | `menu.edit` | Item redaktə |
| DELETE | `/api/menu-ms/v1/items/{id}` | Bearer | `menu.delete` | Item sil |
| GET | `/api/menu-ms/v1/images/**` | public | – | Şəkil (statik) |

### Table — table-service (:8106)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/table-ms/v1/tables` | Bearer | `table.view` | Masalar (orgId, sectionId, status) |
| GET | `/api/table-ms/v1/tables/{id}` | Bearer | `table.view` | Masa detalları |
| POST | `/api/table-ms/v1/tables` | Bearer | `table.create` | Masa yarat |
| PUT | `/api/table-ms/v1/tables/{id}` | Bearer | `table.edit` | Masa redaktə |
| DELETE | `/api/table-ms/v1/tables/{id}` | Bearer | `table.delete` | Masa sil |
| PUT | `/api/table-ms/v1/tables/{id}/status` | Bearer | `table.status` | Status keçidi |
| PUT | `/api/table-ms/v1/tables/{id}/reservation` | Bearer | `table.reserve` | Rezerv yarat |
| DELETE | `/api/table-ms/v1/tables/{id}/reservation` | Bearer | `table.reserve` | Rezerv sil |
| GET | `/api/table-ms/v1/sections` | Bearer | `table.view` | Seksiyalar (orgId) |
| POST | `/api/table-ms/v1/sections` | Bearer | `table.create` | Seksiya yarat |
| PUT | `/api/table-ms/v1/sections/{id}` | Bearer | `table.edit` | Seksiya redaktə |
| DELETE | `/api/table-ms/v1/sections/{id}` | Bearer | `table.delete` | Seksiya sil |

### Order — order-service (:8107)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/order-ms/v1/orders` | Bearer | `order.view` | Sifarişlər (orgId, status, tableId, waiterId) |
| GET | `/api/order-ms/v1/orders/{id}` | Bearer | `order.view` | Sifariş detalları |
| POST | `/api/order-ms/v1/orders` | Bearer | `order.create` | Sifariş yarat |
| PUT | `/api/order-ms/v1/orders/{id}/status` | Bearer | `order.manage` | Order status keçidi |
| PUT | `/api/order-ms/v1/orders/{id}/items/{itemId}/status` | Bearer | `order.manage` | Item status keçidi |
| POST | `/api/order-ms/v1/orders/{id}/items` | Bearer | `order.manage` | Item-lər əlavə et |
| PUT | `/api/order-ms/v1/orders/{id}/waiter-confirm` | Bearer | `order.manage` | Ofisiant təsdiqi |
| POST | `/api/order-ms/v1/orders/{id}/cancel` | Bearer | `order.cancel` | Ləğv et |
| POST | `/api/order-ms/v1/orders/{id}/request-payment` | Bearer | `order.payment` | Hesab istə (paylaşımlı) |
| POST | `/api/order-ms/v1/orders/{id}/complete-payment` | Bearer | `order.payment` | Ödənişi tamamla |
| POST | `/api/order-ms/v1/orders/{id}/start-preparing` | Bearer | `order.manage` | Hazırlanmaya başla |
| POST | `/api/order-ms/v1/orders/{id}/mark-all-ready` | Bearer | `order.manage` | Hamısı hazır |

### Kitchen — kitchen-service (:8108)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/kitchen-ms/v1/orders` | Bearer | `kitchen.view` | PREPARING/READY sifarişlər (qruplaşdırılmış) |

### Waiter — waiter-service (:8109)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/waiter-ms/v1/tables` | Bearer | `waiter.view` | Masalar + aktiv sifariş işarəsi |
| GET | `/api/waiter-ms/v1/orders/pending-confirm` | Bearer | `waiter.view` | Təsdiq gözləyən sifarişlər |
| GET | `/api/waiter-ms/v1/orders/payment-requests` | Bearer | `waiter.view` | Hesab istənmiş sifarişlər |

### Customer — customer-service (:8110)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/customer-ms/v1/{orgId}/menu` | public | – | Müştəri menyusu |
| GET | `/api/customer-ms/v1/{orgId}/tables` | public | – | Masa yoxlaması (QR) |
| POST | `/api/customer-ms/v1/orders` | public | – | Müştəri sifarişi |
| GET | `/api/customer-ms/v1/orders/{orderId}` | public | – | Sifariş statusu |
| POST | `/api/customer-ms/v1/orders/{orderId}/request-bill` | public | – | Hesab istə |

### Settings — setting-service (:8111)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/setting-ms/v1/settings` | Bearer | `settings.view` | Org parametrləri |
| PUT | `/api/setting-ms/v1/settings` | Bearer | `settings.edit` | Parametrləri yenilə |

### Dashboard — dashboard-service (:8112)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/dashboard-ms/v1/stats` | Bearer | `dashboard.view` | Ümumi statistika |
| GET | `/api/dashboard-ms/v1/top-items` | Bearer | `dashboard.view` | Ən çox satılanlar |
| GET | `/api/dashboard-ms/v1/recent-orders` | Bearer | `dashboard.view` | Son sifarişlər |
| GET | `/api/dashboard-ms/v1/staff-list` | Bearer | `dashboard.view` | İşçi xülasəsi |

### Reports — report-service (:8113)

| Method | Path | Auth | Perm | Açıqlama |
|---|---|---|---|---|
| GET | `/api/report-ms/v1/summary` | Bearer | `report.view` | Xülasə |
| GET | `/api/report-ms/v1/daily-revenue` | Bearer | `report.view` | Gündəlik gəlir |
| GET | `/api/report-ms/v1/hourly` | Bearer | `report.view` | Saatlıq gəlir |
| GET | `/api/report-ms/v1/sales-by-category` | Bearer | `report.view` | Kateqoriya üzrə satış |
| GET | `/api/report-ms/v1/top-items` | Bearer | `report.view` | Ən çox satılanlar |
| GET | `/api/report-ms/v1/staff-performance` | Bearer | `report.view` | İşçi performansı |

---

## 15. Common Shared Types (for reference)

> `common-*` modullarında tanımlanır; bütün servislərdə istifadə olunur.

### `ApiResponse<T>`

```json
{ "success": true, "message": "Success", "errorCode": null, "data": { } }
```

`errorCode` səhv halda `"PREFIX_XXXX"` (məs. `ORDER_MS_3001`), uğurda `null`.

### `PageDto<T>`

```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "empty": false
}
```

- Səhifələmə: `page` (0-dan), `size` (default 20, max 100).
- Filter: `q` (search) — ad/kod üzrə (case-insensitive).

### `LocalizedString`

```json
{ "az": "Toyuq Doner", "en": "Chicken Doner", "ru": "Куриный донер" }
```

`az` mütləq (required, max 100), `en`/`ru` optional. Yoxlamalar: boş string qadağan.

### `UiScope` (enum)

`SUPER_ADMIN_PANEL`, `ADMIN_PANEL`, `WAITER_PANEL`, `KITCHEN_PANEL`

- Front bu dəyərlə login cavabındakı `uiScope`-dən panel seçir.
- Fallback: platform admin → `SUPER_ADMIN_PANEL`, digər staff → `ADMIN_PANEL`.

### Ümumi enum-lar

| Enum | Dəyərlər |
|---|---|
| `OrderStatus` | `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `SERVED`, `COMPLETED`, `CANCELLED` |
| `PaymentStatus` | `PENDING`, `PAID` |
| `PaymentMethod` | `CASH`, `CARD` |
| `OrderSource` | `WAITER`, `CUSTOMER` |
| `TableStatus` | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `CLEANING` |

### Tenant header-ləri (gateway → service)

| Header | Məzmun | Mənbə (claim) |
|---|---|---|
| `X-User-Id` | İstifadəçi UUID | `sub` |
| `X-Org-Id` | Org UUID | `organizationId` |
| `X-Roles` | Rol kodları (CSV) | `roles` |
| `X-Permissions` | Permission kodları (CSV) | `permissions` |
| `X-UI-Scope` | `UiScope` dəyəri | `uiScope` |
| `X-Platform-Admin` | `true`/`false` | `roles`-də `SUPER_ADMIN` |
| `X-Internal-Auth` | Daxili servis imzası | Gateway tərəfindən əlavə edilir |

> `X-Internal-Auth` olmadan gələn sorğular `HeaderAuthenticationFilter` tərəfindən **401** alır (servislərarası qorunma).

### Validation Error forması (400)

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for one or more fields",
  "instance": "/api/menu-ms/v1/items",
  "key": "MENU_MS_1000",
  "path": "/api/menu-ms/v1/items",
  "timestamp": "2026-08-06T12:00:00.000Z",
  "fieldErrors": [
    { "field": "price", "message": "must be greater than 0" }
  ]
}
```

> `key` formatı `{SERVICE_KEY}_{CODE}`; `instance` trace header-i varsa `trace:<traceId>`, yoxsa request URI.
> BaseException error-larında property `errorCode` deyil, `key`-dir (bax: `AbstractGlobalExceptionHandler`).

### Ümumi error kodları (bütün servislərdə)

| Code | HTTP | Açıqlama |
|---|---|---|
| `*_1000` | 400 | Validation / tələb formatı səhvi |
| `*_3001` | 404 | Resurs tapılmadı |
| `*_3003` | 403 | Başqa org / permission yoxdur |
| `*_4001` | 401 | Token yoxdur / keçərsiz |
| `*_4003` | 403 | Permission yoxdur |
| `*_9001`/`*_9002` | 503/502 | Upstream servis əlçatmaz / xəta |
| `*_9999` | 500 | Gözlənilməz xəta |

---

## 16. Gradle Modules

> Çoxmodullu Gradle layihəsi; `common-*` modulları bütün servislərdə ortaq olaraq istifadə olunur.

| Modul | Rol |
|---|---|
| `common-core` | Ortaq tiplər: `ApiResponse`, `PageDto`, `UiScope`, `LocalizedString`, enum-lar |
| `common-exception-handling` | `ErrorCode`, `CommonErrorCode`, `BaseException`, `AbstractGlobalExceptionHandler`, `ErrorProperties` (service-key), `FeignClientException` |
| `common-security` | `HeaderAuthenticationFilter`, `JwtUserPrincipalConverter`, `UserPrincipal`, `PermissionEvaluator`, `SecurityConfig`, `KeycloakJwtDecoder` |
| `common-jpa` | Ortaq JPA konfiqurasiyası / audit |
| `cloud-gateway` | Spring Cloud Gateway; `JwtAuthenticationFilter`, `ClaimsForwardingFilter`, `GatewaySecurityConfig`, CORS |
| `auth-gateway` | Login/refresh/logout proxy (Keycloak); `KeycloakClient`, `JwtTokenValidator`, `KeycloakAuthService` |
| `access-service` | Users / Roles / Permissions / Modules / UI Groups (RBAC core) |
| `organization-service` | Organization CRUD + ORG_ADMIN bootstrap; `RoleServiceClient` (feign) |
| `menu-service` | Menu categories & items + lokalizasiya + şəkil |
| `table-service` | Tables & sections + status + rezerv |
| `order-service` | Orders, items, status maşını, ödənişlər |
| `kitchen-service` | Kitchen panel (read, order-ms-ə upstream) |
| `waiter-service` | Waiter panel (read, table-ms/order-ms-ə upstream) |
| `customer-service` | Customer QR flow (public, menu/table/order-ms-ə upstream) |
| `setting-service` | Org parametrləri |
| `dashboard-service` | Dashboard statistika (order/access-ms-ə upstream) |
| `report-service` | Hesabatlar (order/access-ms-ə upstream) |
| `db-migrations` | Liquibase changelog + seed məlumatları (`003-insert-access-data.yml` RBAC seed) |
| `script` | `resto-realm.json` — Keycloak realm exportu (mappers daxil) |

---














