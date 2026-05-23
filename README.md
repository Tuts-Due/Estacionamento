# Estapar Backend Developer Test

Sistema backend para gerenciamento de estacionamento desenvolvido com **Kotlin + Spring Boot + MySQL**, integrado ao simulador fornecido no desafio técnico da Estapar.

---

# Tecnologias utilizadas

* Kotlin 2.1.x
* Java 21
* Spring Boot
* Spring Data JPA
* MySQL 8
* Flyway
* Docker / Docker Compose
* Swagger / OpenAPI
* JUnit 5
* Mockito
* H2 Database
* Gradle Kotlin DSL

---

# Funcionalidades

* Consumo automático da configuração da garagem via `GET /garage`
* Armazenamento de setores e vagas
* Recebimento de eventos via webhook:

  * ENTRY
  * PARKED
  * EXIT
* Controle de ocupação das vagas
* Controle de lotação por setor
* Regra de preço dinâmico por ocupação
* Cálculo de permanência
* Cálculo de receita por setor e data
* Persistência em MySQL
* Controle transacional com lock pessimista
* Migração de banco com Flyway
* Documentação Swagger/OpenAPI

---

# Regras de negócio implementadas

## Entrada de veículos

Ao receber um evento `ENTRY`:

* Uma vaga livre é reservada
* A vaga é marcada como ocupada
* O preço por hora é calculado no momento da entrada
* O preço fica congelado durante toda a permanência

---

## Evento PARKED

O simulador envia latitude e longitude da vaga real.

O sistema:

* Identifica a vaga correta pelas coordenadas
* Corrige o setor do veículo caso necessário
* Atualiza o `ParkingRecord`

---

## Saída de veículos

Ao receber um evento `EXIT`:

* A vaga é liberada
* O valor final é calculado
* O faturamento é registrado

---

# Regras de cobrança

## Até 30 minutos

* Gratuito

## Após 30 minutos

Cobrança por hora com arredondamento para cima.

Exemplo:

* 61 minutos = 2 horas cobradas

---

# Preço dinâmico por ocupação

| Ocupação     | Regra        |
| ------------ | ------------ |
| < 25%        | -10%         |
| 25% até 50%  | preço normal |
| 50% até 75%  | +10%         |
| 75% até 100% | +25%         |

---

# Controle de concorrência

O sistema utiliza:

* `@Transactional`
* `@Lock(PESSIMISTIC_WRITE)`

para evitar que múltiplos veículos ocupem a mesma vaga simultaneamente.

---

# Estrutura do projeto

```text
src/main/kotlin/com/arthur/estapar/parking

├── config
├── controller
├── exception
├── model
├── repository
└── service
```

---

# Ambiente de desenvolvimento

Projeto desenvolvido e testado utilizando:

* Windows 11
* WSL2 Ubuntu
* Docker Desktop
* Java 21
* Kotlin 2.1.x

---

# Estrutura Docker

O ambiente sobe 3 containers:

* `estacionamento-app`
* `estacionamento-mysql`
* `estacionamento-simulator`

Todos conectados através da rede bridge compartilhada:

```text
estapar-net
```

---

# Como executar via WSL

## 1. Abrir WSL

```bash
wsl
```

---

## 2. Entrar na pasta do projeto

```bash
cd /mnt/d/Workspace/Estacionamento
```

---

## 3. Subir containers

```bash
docker compose up -d
```

---

## 4. Verificar containers

```bash
docker ps
```

Containers esperados:

* estacionamento-app
* estacionamento-mysql
* estacionamento-simulator

---

# Logs da aplicação

Visualizar logs completos:

```bash
docker compose logs -f
```

Logs apenas da aplicação:

```bash
docker logs -f estacionamento-app
```

---

# Acesso ao MySQL

Entrar no container:

```bash
docker exec -it estacionamento-mysql mysql -u root -p
```

Selecionar database:

```sql
USE estacionamento;
```

Ver tabelas:

```sql
SHOW TABLES;
```

Consultar registros:

```sql
SELECT * FROM parking_record ORDER BY id DESC LIMIT 10;
```

---

# Acesso via MySQL Workbench

Host:

```text
localhost
```

Porta:

```text
3307
```

Usuário:

```text
root
```

Senha:

```text
root
```

Database:

```text
estacionamento
```

---

# Endpoints

## Webhook

```http
POST /webhook
```

Recebe eventos do simulador:

* ENTRY
* PARKED
* EXIT

---

## Receita

```http
GET /revenue?date=2026-05-23&sector=A
```

### Exemplo de resposta

```json
{
  "amount": 120.0,
  "currency": "BRL",
  "timestamp": "2026-05-23T18:00:00Z"
}
```

---

# Swagger

Disponível em:

```text
http://localhost:3003/swagger-ui.html
```

---

# Banco de dados

Tabelas principais:

* garage
* spot
* parking_record
* vehicle_event

---

# Flyway

As migrations ficam em:

```text
src/main/resources/db/migration
```

---

# Testes

Executar testes:

```bash
./gradlew test
```

Os testes utilizam:

* H2 Database
* Mockito
* JUnit 5

---

# Fluxo esperado da aplicação

1. Aplicação inicia
2. `GarageService` executa `@PostConstruct`
3. Sistema consome `GET /garage`
4. Setores e vagas são persistidos
5. Simulador começa a enviar eventos para `/webhook`
6. Sistema processa:

  * ENTRY
  * PARKED
  * EXIT
7. Receita fica disponível em:

  * `GET /revenue`

---

# Observações técnicas

## Sobre o simulador

O simulador retorna `basePrice = 0.0` no endpoint `/garage`, porém internamente utiliza outro valor para cálculo da receita exibida nos logs do simulador.

A aplicação utiliza exatamente os dados fornecidos pelo simulador conforme especificação do desafio.

---

## Sobre Docker no Windows

O desafio original utilizava:

```bash
docker run --network="host"
```

Essa abordagem funciona nativamente apenas em Linux.

Para compatibilidade com Docker Desktop no Windows/Mac, foi utilizada uma rede bridge compartilhada entre os containers.

Também foi utilizado:

```yaml
extra_hosts:
  - "localhost:host-gateway"
```

para permitir que o simulador acessasse corretamente a aplicação Spring Boot.

---

# Autor

Arthur Dué

* Kotlin / Java Backend Developer
* Spring Boot
* Microsserviços
* APIs REST
* Docker
* SQL / NoSQL

Portfólio:

[Portfolio Arthur Dué](https://portfolio-arthur-due.vercel.app/?utm_source=chatgpt.com)
