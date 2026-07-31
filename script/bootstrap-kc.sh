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
PLATFORM_BOOTSTRAP_EMAIL="${PLATFORM_BOOTSTRAP_EMAIL:-platform@codlab.az}"
PLATFORM_BOOTSTRAP_ORG_ID="${PLATFORM_BOOTSTRAP_ORG_ID:-a55faced-dead-4bed-babe-feeddeadbeef}"
KC_REALM="${KC_REALM:-tabler}"
KC_CLIENT_ID="${KC_CLIENT_ID:-tabler-auth}"

echo "[STEP 1] Configuring credentials..."
output=$(/opt/keycloak/bin/kcadm.sh config credentials --server "$KC_BASE_URL" --realm master --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" 2>&1) \
  && log_success "Credentials configured" \
  || log_fail "Failed to configure credentials" "$output"

echo "[STEP 2] Updating client secret..."
CLIENT_ID=$(/opt/keycloak/bin/kcadm.sh get clients -r "$KC_REALM" --query clientId="$KC_CLIENT_ID" --fields id --format csv --noquotes 2>&1)
[ -z "$CLIENT_ID" ] && log_fail "Failed to get client ID" "Empty result"
output=$(/opt/keycloak/bin/kcadm.sh update clients/$CLIENT_ID -r "$KC_REALM" -s "secret=$AUTH_KEYCLOAK_CLIENT_SECRET" 2>&1) \
  && log_success "Client secret updated (ID: $CLIENT_ID)" \
  || log_fail "Failed to update client secret" "$output"

echo "[STEP 3] Checking/Creating user..."
USER_ID=$(/opt/keycloak/bin/kcadm.sh get users -r "$KC_REALM" -q "username=$PLATFORM_BOOTSTRAP_EMAIL" --fields id --format csv --noquotes 2>/dev/null || echo "")

if [ -z "$USER_ID" ]; then
  output=$(/opt/keycloak/bin/kcadm.sh create users -r "$KC_REALM" -s "username=$PLATFORM_BOOTSTRAP_EMAIL" -s "email=$PLATFORM_BOOTSTRAP_EMAIL" -s firstName=plat -s lastName=form -s enabled=true -s emailVerified=true -s "attributes.organizationId=$PLATFORM_BOOTSTRAP_ORG_ID" 2>&1) \
    && log_success "User created" \
    || log_fail "Failed to create user" "$output"
else
  echo "[SUCCESS] User already exists, skipping creation."
fi

echo "[STEP 4] Setting password..."
output=$(/opt/keycloak/bin/kcadm.sh set-password -r "$KC_REALM" --username "$PLATFORM_BOOTSTRAP_EMAIL" --new-password "$PLATFORM_BOOTSTRAP_PASSWORD" 2>&1) \
  && log_success "Password set" \
  || log_fail "Failed to set password" "$output"

echo "[STEP 5] Adding roles..."
output=$(/opt/keycloak/bin/kcadm.sh add-roles -r "$KC_REALM" --uusername "$PLATFORM_BOOTSTRAP_EMAIL" --rolename SUPER_ADMIN --cclientid "$KC_CLIENT_ID" 2>&1) \
  && log_success "Role assigned" \
  || log_fail "Failed to assign role" "$output"

echo ""
echo "✓ Bootstrap completed successfully"
