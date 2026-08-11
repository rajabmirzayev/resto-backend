#!/bin/sh

set -e

log_success() {
  echo "[SUCCESS] $1"
}

log_fail() {
  echo "[FAILED] $1"
  echo "[REASON] $2"
  exit 1
}

: "${KC_BOOTSTRAP_ADMIN_USERNAME:?KC_BOOTSTRAP_ADMIN_USERNAME is required}"
: "${KC_BOOTSTRAP_ADMIN_PASSWORD:?KC_BOOTSTRAP_ADMIN_PASSWORD is required}"
: "${AUTH_KEYCLOAK_CLIENT_SECRET:?AUTH_KEYCLOAK_CLIENT_SECRET is required}"
: "${PLATFORM_BOOTSTRAP_PASSWORD:?PLATFORM_BOOTSTRAP_PASSWORD is required}"

KC_BASE_URL="${KC_BASE_URL:-http://localhost:8080}"
PLATFORM_BOOTSTRAP_EMAIL="${PLATFORM_BOOTSTRAP_EMAIL:-admin@flowix.az}"
PLATFORM_BOOTSTRAP_ORG_ID="${PLATFORM_BOOTSTRAP_ORG_ID:-a55faced-dead-4bed-babe-feeddeadbeef}"
KC_REALM="${KC_REALM:-resto}"
KC_CLIENT_ID="${KC_CLIENT_ID:-resto-auth}"

# All permission codes (superset used by the platform SUPER_ADMIN role).
ALL_PERMISSIONS="dashboard.view
menu.view menu.create menu.edit menu.delete
table.view table.create table.edit table.delete table.status table.reserve
order.view order.create order.manage order.cancel order.payment
kitchen.view kitchen.manage
waiter.view waiter.manage
staff.view staff.create staff.edit staff.delete
role.view role.create role.edit role.delete role.assign permission.view permission.manage
settings.view settings.edit
report.view
organization.view organization.create organization.edit organization.delete"

echo "[STEP 1] Configuring credentials..."
output=$(/opt/keycloak/bin/kcadm.sh config credentials --server "$KC_BASE_URL" --realm master --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" 2>&1) \
  && log_success "Credentials configured" \
  || log_fail "Failed to configure credentials" "$output"

echo "[STEP 2] Updating client secret..."
CLIENT_ID=$(/opt/keycloak/bin/kcadm.sh get clients -r "$KC_REALM" --query clientId="$KC_CLIENT_ID" --fields id --format csv --noquotes 2>&1)
[ -z "$CLIENT_ID" ] && log_fail "Failed to get client ID" "Empty result"
output=$(/opt/keycloak/bin/kcadm.sh update clients/$CLIENT_ID -r "$KC_REALM" -s "secret=$AUTH_KEYCLOAK_CLIENT_SECRET" -s fullScopeAllowed=true 2>&1) \
  && log_success "Client secret updated (ID: $CLIENT_ID)" \
  || log_fail "Failed to update client secret" "$output"

echo "[STEP 3] Configuring user profile (optional lastName, allows roles/permissions/uiScope)..."
PROFILE_COMP=$(/opt/keycloak/bin/kcadm.sh get components -r "$KC_REALM" --fields id,providerId 2>/dev/null | grep -B1 'declarative-user-profile' | grep -oE '[0-9a-f-]{36}' | head -1)
if [ -z "$PROFILE_COMP" ]; then
  log_fail "Failed to locate user profile component" "Empty result"
fi
cat > /tmp/profile.json <<'PAYLOAD'
{
  "name": "declarative-user-profile",
  "providerId": "declarative-user-profile",
  "providerType": "org.keycloak.userprofile.UserProfileProvider",
  "config": {
    "kc.user.profile.config": [
      "{\"attributes\":[{\"name\":\"username\",\"displayName\":\"${username}\",\"validations\":{\"length\":{\"min\":3,\"max\":255},\"username-prohibited-characters\":{},\"up-username-not-idn-homograph\":{}},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\",\"user\"]},\"multivalued\":false},{\"name\":\"firstName\",\"displayName\":\"${firstName}\",\"validations\":{\"length\":{\"max\":255},\"person-name-prohibited-characters\":{}},\"required\":{\"roles\":[\"user\"]},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\",\"user\"]},\"multivalued\":false},{\"name\":\"lastName\",\"displayName\":\"${lastName}\",\"validations\":{\"length\":{\"max\":255},\"person-name-prohibited-characters\":{}},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\",\"user\"]},\"multivalued\":false},{\"name\":\"email\",\"displayName\":\"${email}\",\"validations\":{\"email\":{},\"length\":{\"max\":255}},\"required\":{\"roles\":[\"user\"]},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\",\"user\"]},\"multivalued\":false},{\"name\":\"organizationId\",\"displayName\":\"${organizationId}\",\"validations\":{},\"annotations\":{},\"required\":{\"roles\":[\"admin\",\"user\"]},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\"]},\"multivalued\":false},{\"name\":\"roles\",\"displayName\":\"${roles}\",\"validations\":{},\"annotations\":{},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\"]},\"multivalued\":true},{\"name\":\"permissions\",\"displayName\":\"${permissions}\",\"validations\":{},\"annotations\":{},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\"]},\"multivalued\":true},{\"name\":\"uiScope\",\"displayName\":\"${uiScope}\",\"validations\":{},\"annotations\":{},\"permissions\":{\"view\":[\"admin\",\"user\"],\"edit\":[\"admin\"]},\"multivalued\":false}],\"groups\":[{\"name\":\"user-metadata\",\"displayHeader\":\"User metadata\",\"displayDescription\":\"Attributes, which refer to user metadata\"}]}"
    ]
  }
}
PAYLOAD
output=$(/opt/keycloak/bin/kcadm.sh update "components/$PROFILE_COMP" -r "$KC_REALM" -f /tmp/profile.json 2>&1) \
  && log_success "User profile updated" \
  || log_fail "Failed to update user profile" "$output"

echo "[STEP 4] Checking/Creating user..."
USER_ID=$(/opt/keycloak/bin/kcadm.sh get users -r "$KC_REALM" -q "username=$PLATFORM_BOOTSTRAP_EMAIL" --fields id --format csv --noquotes 2>/dev/null || echo "")

if [ -z "$USER_ID" ]; then
  output=$(/opt/keycloak/bin/kcadm.sh create users -r "$KC_REALM" -s "username=$PLATFORM_BOOTSTRAP_EMAIL" -s "email=$PLATFORM_BOOTSTRAP_EMAIL" -s firstName=plat -s lastName=form -s enabled=true -s emailVerified=true -s "attributes.organizationId=$PLATFORM_BOOTSTRAP_ORG_ID" 2>&1) \
    && log_success "User created" \
    || log_fail "Failed to create user" "$output"
  USER_ID=$(/opt/keycloak/bin/kcadm.sh get users -r "$KC_REALM" -q "username=$PLATFORM_BOOTSTRAP_EMAIL" --fields id --format csv --noquotes 2>&1)
else
  echo "[SUCCESS] User already exists, skipping creation."
fi

echo "[STEP 5] Ensuring platform user profile and attributes (roles, permissions, uiScope)..."
PERM_JSON="["
PERM_FIRST=1
for perm in $ALL_PERMISSIONS; do
  if [ "$PERM_FIRST" = 1 ]; then
    PERM_JSON="$PERM_JSON\"$perm\""
    PERM_FIRST=0
  else
    PERM_JSON="$PERM_JSON,\"$perm\""
  fi
done
PERM_JSON="$PERM_JSON]"
output=$(/opt/keycloak/bin/kcadm.sh update "users/$USER_ID" -r "$KC_REALM" -s "email=$PLATFORM_BOOTSTRAP_EMAIL" -s firstName=plat -s lastName=form -s enabled=true -s emailVerified=true -s "attributes.organizationId=$PLATFORM_BOOTSTRAP_ORG_ID" -s attributes.roles=SUPER_ADMIN -s attributes.uiScope=SUPER_ADMIN_PANEL -s "attributes.permissions=$PERM_JSON" 2>&1) \
  && log_success "Platform user profile and attributes set" \
  || log_fail "Failed to set platform user profile and attributes" "$output"

echo "[STEP 6] Setting password..."
output=$(/opt/keycloak/bin/kcadm.sh set-password -r "$KC_REALM" --username "$PLATFORM_BOOTSTRAP_EMAIL" --new-password "$PLATFORM_BOOTSTRAP_PASSWORD" 2>&1) \
  && log_success "Password set" \
  || log_fail "Failed to set password" "$output"

echo "[STEP 7] Adding roles..."
output=$(/opt/keycloak/bin/kcadm.sh add-roles -r "$KC_REALM" --uusername "$PLATFORM_BOOTSTRAP_EMAIL" --rolename SUPER_ADMIN --cclientid "$KC_CLIENT_ID" 2>&1) \
  && log_success "Role assigned" \
  || log_fail "Failed to assign role" "$output"

echo "[STEP 8] Granting user-management to the service account..."
output=$(/opt/keycloak/bin/kcadm.sh add-roles -r "$KC_REALM" --uusername "service-account-$KC_CLIENT_ID" --cclientid realm-management --rolename view-users --rolename manage-users --rolename view-clients --rolename manage-clients 2>&1) \
  && log_success "Service account roles assigned" \
  || log_fail "Failed to assign service account roles" "$output"

echo ""
echo "✓ Bootstrap completed successfully"
