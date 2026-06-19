# ByteBank API Gateway

Ponto único de entrada do ecossistema ByteBank. Roteia requisições para os microsserviços corretos via Service Discovery (Eureka), agrega a documentação Swagger de todos os serviços e aplica filtros customizados para resolução de contexto de usuário.

---

## Funcionalidades

**Roteamento dinâmico**
Todas as requisições externas chegam na porta 8080 e são roteadas para o microsserviço correto via load balancer (`lb://`) integrado ao Eureka. Os serviços não precisam ser acessados diretamente.

**Agregação de documentação**
O Swagger UI centralizado (`/swagger-ui.html`) agrega a documentação de todos os microsserviços em um único lugar — sem precisar acessar cada serviço individualmente.

**Filtro de resolução de usuário (`ResolveUserGatewayFilterFactory`)**
Aplicado seletivamente nas rotas `/api/v1/operations/whatsapp` e `/api/v1/operations/audio` do `finance-ai-service`. Intercepta o número de telefone do header `X-Phone-Number`, consulta o `bytebank-customer` para resolver o `customerId` correspondente e substitui o header antes de encaminhar a requisição — garantindo que o `userId` nunca trafegue pelo n8n ou WAHA, sendo injetado internamente pelo Gateway.

**CORS configurado**
Em produção, aceita requisições de qualquer origem, método e header.

---

## Stack

| Camada | Tecnologia |
|--------|------------|
| Framework | Spring Boot 3.x, Spring Cloud Gateway |
| Service Discovery | Netflix Eureka Client |
| Documentação | SpringDoc / Swagger UI agregado |
| Observabilidade | Prometheus, Zipkin, Spring Boot Actuator |

---

## Rotas Configuradas

| Rota | Serviço destino | Predicado |
|------|-----------------|-----------|
| `api-gateway` | — | porta 8080 |
| `bytebank-accounts` | bytebank-accounts | `/api/v1/accounts/**` |
| `bytebank-customer` | bytebank-customer | `/api/v2/customers/**` |
| `bytebank-transactions` | bytebank-transactions | `/api/v1/transactions/**` |
| `bytebank-notification` | bytebank-notification | `/api/v1/notifications/**` |
| `finance-ai-audio` | finance-ai-service | `/api/v1/operations/audio`, `/api/v1/operations/whatsapp` + filtro `ResolveUser` |
| `finance-ai-service` | finance-ai-service | `/api/v1/operations/**` |

**Rotas de documentação** (reescrita de path):

| Path externo | Destino interno |
|---|---|
| `/bytebank-accounts/v3/api-docs` | `bytebank-accounts/v3/api-docs` |
| `/bytebank-customer/v3/api-docs` | `bytebank-customer/v3/api-docs` |
| `/bytebank-transactions/v3/api-docs` | `bytebank-transactions/v3/api-docs` |
| `/bytebank-notification/v3/api-docs` | `bytebank-notification/v3/api-docs` |

---

## Filtro ResolveUser

O `ResolveUserGatewayFilterFactory` é um `GatewayFilterFactory` customizado aplicado seletivamente apenas nas rotas do `finance-ai-service` que precisam de contexto de usuário.

```
Requisição chega com header X-Phone-Number: 5513999999999
        ↓
ResolveUserGatewayFilterFactory intercepta
        ↓
Consulta bytebank-customer: GET /api/v2/customers/phone/{number}
        ↓
Substitui header: X-Phone-Number → X-User-Id: <customerId>
        ↓
Encaminha para finance-ai-service
```

Isso garante que o número de telefone (dado do WAHA/n8n) nunca chega ao serviço de IA — apenas o ID interno do usuário é propagado.

---

## Swagger UI

Acessível em:
- **Produção:** [bytebank.thalesf.dev/swagger-ui.html](https://bytebank.thalesf.dev/swagger-ui.html)
- **Local:** http://localhost:8080/swagger-ui.html

Agrega a documentação de todos os microsserviços em abas separadas.

---

## Como Executar

### Pré-requisitos

- Docker e Docker Compose instalados
- Rede Docker `bytebank-net` criada
- Eureka Server rodando e acessível

### Variáveis de Ambiente

```env
EUREKA_DEFAULT_ZONE=http://eureka-server:8761/eureka/
ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans
```

### Subindo o serviço

```bash
docker compose -p api-gateway up -d --build
```

---

## Autor

**Thales Fernandes**

[![GitHub](https://img.shields.io/badge/GitHub-ThalesF93-181717?style=flat&logo=github)](https://github.com/ThalesF93)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Thales_Fernandes-0A66C2?style=flat&logo=linkedin)](https://www.linkedin.com/in/thales-fernandes-24418126a/)
