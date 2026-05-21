# Estapar Parking — Backend

Sistema de gerenciamento de estacionamento desenvolvido com **Kotlin 2.1**, **Spring Boot 3.3** e **MySQL**.

---

## Pré-requisitos

- Java 21+
- Docker e Docker Compose
- Gradle (ou use o wrapper `./gradlew`)

---

## Como rodar

### Opção 1 — Docker Compose (recomendado)

Sobe MySQL + simulador + aplicação com um único comando:

```bash
cp .env.example .env      # ajuste as variáveis se necessário
docker-compose up --build
```

### Opção 2 — Manual

```bash
# 1. MySQL
docker run -d \
  --name estapar-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=estapar_parking \
  -p 3306:3306 \
  mysql:8

# 2. Simulador
docker run -d --network="host" cfontes0estapar/garage-sim:1.0.0

# 3. Aplicação
DB_USER=root DB_PASS=root ./gradlew bootRun
```

A aplicação irá:
1. Rodar as migrations Flyway automaticamente
2. Buscar a configuração da garagem no simulador (`GET /garage`)
3. Começar a escutar eventos em `POST /webhook`

---

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_USER` | `root` | Usuário do MySQL |
| `DB_PASS` | `root` | Senha do MySQL |
| `GARAGE_SIMULATOR_URL` | `http://localhost:8282` | URL do simulador |

Copie `.env.example` para `.env` para configurar localmente. O arquivo `.env` está no `.gitignore` e nunca deve ser commitado.

---

## Endpoints

### Webhook (recebe eventos do simulador)

```
POST /webhook
Content-Type: application/json
```

| Evento | Campos obrigatórios |
|---|---|
| `ENTRY` | `license_plate`, `entry_time`, `event_type` |
| `PARKED` | `license_plate`, `lat`, `lng`, `event_type` |
| `EXIT` | `license_plate`, `exit_time`, `event_type` |

### Consulta de receita

```
GET /revenue
Content-Type: application/json

{ "date": "2025-01-01", "sector": "A" }
```

Resposta:
```json
{ "amount": 120.00, "currency": "BRL", "timestamp": "2025-01-01T18:00:00.000Z" }
```

---

## Regras de negócio

| Situação | Comportamento |
|---|---|
| Permanência ≤ 30 min | Grátis |
| Permanência > 30 min | Cobra por hora cheia (ceil), incluindo a primeira |
| Lotação < 25% | Desconto de 10% no preço/hora |
| Lotação < 50% | Preço normal |
| Lotação < 75% | Acréscimo de 10% |
| Lotação ≥ 75% | Acréscimo de 25% |
| Lotação 100% | HTTP 409 — bloqueia novas entradas até liberar vaga |

---

## Documentação interativa (Swagger UI)

Com a aplicação rodando, acesse:

- **Swagger UI:** http://localhost:3003/swagger-ui.html
- **OpenAPI JSON:** http://localhost:3003/api-docs

---

## Rodando os testes

```bash
./gradlew test
```

Os testes usam H2 em memória — **não precisam de MySQL rodando**.

---

## Estrutura do projeto

```
src/
├── main/kotlin/com/estapar/parking/
│   ├── config/        # WebhookConfig, OpenApiConfig
│   ├── controller/    # WebhookController, RevenueController
│   ├── exception/     # GlobalExceptionHandler, ErrorResponse
│   ├── model/         # Garage, Spot, ParkingRecord, VehicleEvent, Revenue
│   ├── repository/    # Interfaces JPA
│   └── service/       # ParkingService, GarageService, RevenueService
└── test/kotlin/com/estapar/parking/
    ├── controller/    # WebhookControllerIntegrationTest
    └── service/       # ParkingServiceTest, RevenueServiceTest
```
