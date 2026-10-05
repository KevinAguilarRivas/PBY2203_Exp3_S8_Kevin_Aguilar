#!/usr/bin/env bash
# Script de pruebas end-to-end del stack Banco XYZ (Exp3 S8)
# Uso: bash evidencia/pruebas.sh   (con el stack levantado via docker compose up -d)
KC=http://localhost:8180/realms/bancoxyz/protocol/openid-connect/token
tok(){ curl -s -X POST $KC -d "grant_type=password&client_id=bancoxyz-app&client_secret=bancoxyz-secret-2026&username=$1&password=$2" \
       | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])"; }
claims(){ python -c "import sys,json,base64;p=sys.argv[1].split('.')[1];p+='='*(-len(p)%4);d=json.loads(base64.urlsafe_b64decode(p));print('  iss =',d['iss'],'| usuario =',d['preferred_username'],'| roles =',d['realm_access']['roles'])" "$1"; }
call(){ echo "\$ curl $*" | sed -E 's/Bearer [A-Za-z0-9._-]+/Bearer <JWT>/'; out=$(curl -s -w "\n  -> HTTP %{http_code}" "$@"); body=${out%$'\n'*}
  [ ${#body} -gt 700 ] && body="${body:0:700} ... (${#body} caracteres, recortado)"; echo "$body"; echo "${out##*$'\n'}"; echo; }

echo "=================== 1. Estado de contenedores ==================="
docker compose ps --format 'table {{.Name}}\t{{.Status}}\t{{.Ports}}'
echo; echo "=================== 2. Eureka: servicios registrados ==================="
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps | python -c "import sys,json;[print('  ',a['name'],'->',[i['instanceId']+' '+i['status'] for i in a['instance']]) for a in json.load(sys.stdin)['applications']['application']]"
echo; echo "=================== 3. Config Server: config servida a core-service ==================="
curl -s http://localhost:8888/core-service/default | python -c "import sys,json;d=json.load(sys.stdin);[print('  ',s['name']) for s in d['propertySources']]"

echo; echo "=================== 4. OAuth2 (Keycloak) ==================="
echo "--- Sin token -> debe ser 401"; call -o /dev/null http://localhost:8081/web/transacciones
echo "--- Token invalido -> debe ser 401"; call -o /dev/null -H "Authorization: Bearer abc.def.ghi" http://localhost:8081/web/cuentas/101
TW=$(tok usuario.web web123); TM=$(tok usuario.mobile mobile123); TC=$(tok cajero.atm001 cajero123)
echo "--- Tokens JWT obtenidos (grant_type=password):"; claims "$TW"; claims "$TM"; claims "$TC"
echo "--- Credenciales incorrectas -> invalid_grant"
curl -s -X POST $KC -d "grant_type=password&client_id=bancoxyz-app&client_secret=bancoxyz-secret-2026&username=usuario.web&password=malo"; echo
echo "--- Client Credentials (M2M):"
curl -s -X POST $KC -d "grant_type=client_credentials&client_id=bancoxyz-app&client_secret=bancoxyz-secret-2026" | python -c "import sys,json;d=json.load(sys.stdin);print('  token_type =',d['token_type'],'| expires_in =',d['expires_in'])"

echo; echo "=================== 5. BFF Web (8081) ==================="
call -H "Authorization: Bearer $TW" http://localhost:8081/web/cuentas/101
call -H "Authorization: Bearer $TW" http://localhost:8081/web/transacciones

echo "=================== 6. BFF Mobile (8082) ==================="
call -H "Authorization: Bearer $TM" http://localhost:8082/mobile/cuentas/101/resumen
call -H "Authorization: Bearer $TM" "http://localhost:8082/mobile/cuentas/101/movimientos?limite=3"
echo "--- Cuenta inexistente -> 404 (no abre el Circuit Breaker)"
call -H "Authorization: Bearer $TM" http://localhost:8082/mobile/cuentas/99999/resumen

echo "=================== 7. BFF Cajero (8083) + Kafka ==================="
call -H "Authorization: Bearer $TC" http://localhost:8083/cajero/cuentas/101/saldo
call -X POST -H "Authorization: Bearer $TC" -H "Content-Type: application/json" -d '{"monto": 50.00}' http://localhost:8083/cajero/cuentas/101/retiro
call -X POST -H "Authorization: Bearer $TC" -H "Content-Type: application/json" -d '{"monto": 99999999}' http://localhost:8083/cajero/cuentas/101/retiro
call -H "Authorization: Bearer $TC" http://localhost:8083/cajero/cuentas/101/saldo
sleep 3
echo "--- Mensajes en topic bancoxyz.retiros (kafka-console-consumer):"
docker exec kafka kafka-console-consumer --bootstrap-server kafka:9092 --topic bancoxyz.retiros --from-beginning --timeout-ms 6000 2>/dev/null
echo "--- Log del productor y consumidor en core-service:"
docker logs core-service 2>&1 | grep -E "Evento de retiro publicado|\[KAFKA\]" | tail -4

echo; echo "=================== 8. Resilience4j: Circuit Breaker ==================="
echo "--- Estado inicial:"; curl -s http://localhost:8082/actuator/circuitbreakers | python -m json.tool
echo "--- Se detiene core-service (simula caida)"; docker stop core-service >/dev/null
for i in 1 2 3 4 5 6; do printf "  llamada $i: "; curl -s -o /dev/null -w "HTTP %{http_code} (%{time_total}s)\n" -H "Authorization: Bearer $TM" http://localhost:8082/mobile/cuentas/101/resumen; done
echo "--- Estado con core caido (debe estar OPEN):"; curl -s http://localhost:8082/actuator/circuitbreakers | python -m json.tool
docker logs bff-mobile 2>&1 | grep -E "OPEN|CircuitBreaker" | tail -2
echo "--- Retry en BFF Cajero (3 intentos antes del fallback):"
curl -s -o /dev/null -w "  HTTP %{http_code} (%{time_total}s)\n" -H "Authorization: Bearer $TC" http://localhost:8083/cajero/cuentas/101/saldo
curl -s http://localhost:8083/actuator/retryevents/core | python -c "import sys,json;[print('  ',e['creationTime'][11:23],e['type'],'intento',e['numberOfAttempts'],'-',e['errorMessage'][:70]) for e in json.load(sys.stdin)['retryEvents'][-3:]]"
echo "--- Se levanta core-service nuevamente"; docker start core-service >/dev/null
for i in $(seq 1 40); do curl -s localhost:8080/actuator/health | grep -q UP && break; sleep 3; done
sleep 12
for i in 1 2 3; do printf "  llamada $i: "; curl -s -o /dev/null -w "HTTP %{http_code}\n" -H "Authorization: Bearer $TM" http://localhost:8082/mobile/cuentas/101/resumen; done
echo "--- Estado tras recuperacion (HALF_OPEN -> CLOSED):"; curl -s http://localhost:8082/actuator/circuitbreakers | python -m json.tool
