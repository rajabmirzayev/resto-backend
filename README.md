## Quick local start

```bash
docker compose -f script/local-compose.yml up -d
set -a && source script/.env && set +a && ./gradlew :db-migrations:build :db-migrations:bootRun

docker exec -it $(docker compose -f script/local-compose.yml ps -q keycloak) /bin/sh /tmp/script/bootstrap-kc.sh

set -a && source script/.env && set +a && ./gradlew :auth-gateway:build :auth-gateway:bootRun
set -a && source script/.env && set +a && ./gradlew :cloud-gateway:build :cloud-gateway:bootRun
```
