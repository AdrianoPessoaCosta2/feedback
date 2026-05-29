# Plataforma de Feedback

Plataforma de feedback para avaliação de aulas, com notificações automáticas para itens críticos e relatórios semanais.

## Arquitetura

```
                         ┌──────────────┐
                    ┌───►│ AWS Lambda   │  (envia email urgente via SES)
                    │    │ email-handler│
                    │    └──────────────┘
┌─────────┐   ┌────┴───┐
│ Quarkus │──►│  SNS   │
│   API   │   │ Topic  │
│ (EC2)   │   └────┬───┘
└─────────┘        │    ┌──────────────┐   ┌──────────┐   ┌──────────┐
                   └───►│   SQS Queue  │──►│ Spring   │──►│ DynamoDB │
                        └──────────────┘   │ Service  │   └──────────┘
                                           │ (EC2)    │
                                           └──────────┘
                                              ⏰ Relatório semanal
```

## Componentes

| Componente | Diretório | Descrição |
|---|---|---|
| **API Quarkus** | `api-quarkus/` | API REST — `POST /avaliacao` recebe feedbacks e publica no SNS |
| **Lambda Email** | `lambda-email/` | Função serverless — envia emails urgentes (nota ≤ 5) via SES |
| **Spring Service** | `service-spring/` | Consome SQS, persiste no DynamoDB, gera relatório semanal |
| **Infraestrutura** | `infra/` | Terraform — provisiona todos os recursos AWS |

## Endpoint

```
POST /avaliacao
Content-Type: application/json

{
  "descricao": "Aula muito boa sobre microserviços",
  "nota": 8
}
```

**Resposta (201 Created):**
```json
{
  "descricao": "Aula muito boa sobre microserviços",
  "nota": 8,
  "urgencia": "MEDIA",
  "dataEnvio": "2026-05-29T16:00:00Z",
  "messageId": "abc-123"
}
```

### Classificação de urgência

| Nota | Urgência |
|---|---|
| 0–3 | CRITICA |
| 4–5 | ALTA |
| 6–7 | MEDIA |
| 8–10 | BAIXA |

## Infraestrutura AWS (Terraform)

Recursos provisionados:

- **SNS Topic** — fan-out de feedbacks para Lambda e SQS
- **SQS Queue** — fila com DLQ para processamento resiliente
- **Lambda** (Java 21) — filtro por urgência CRITICA/ALTA, envia email via SES
- **DynamoDB** — tabela `feedback` (PAY_PER_REQUEST)
- **2x EC2** (Amazon Linux 2023) — Quarkus API + Spring Service
- **IAM** — roles com least-privilege para Lambda e EC2
- **Security Groups** — portas 8080, 8081, 22
- **CloudWatch Logs** — retenção de 14 dias para Lambda
- **S3 Bucket** — artefatos e state file

## Deploy

### Pré-requisitos

- AWS CLI configurado com credenciais
- Terraform >= 1.8
- JDK 21 + Maven 3.9+

### Build dos artefatos

```bash
# API Quarkus
cd api-quarkus && mvn package -DskipTests

# Lambda Email
cd lambda-email && mvn package

# Spring Service
cd service-spring && mvn package -DskipTests
```

### Deploy com Terraform

```bash
cd infra
terraform init \
  -backend-config="bucket=proj-us-east-2-terr-statefile" \
  -backend-config="key=feedback/dev/terraform.tfstate" \
  -backend-config="region=us-east-2" \
  -backend-config="dynamodb_table=proj-us-east-2-terr-lock"

terraform workspace select -or-create dev
terraform plan -var-file="./envs/dev/terraform.tfvars" -out=dev.plan
terraform apply dev.plan
```

### Upload do JAR da Lambda

```bash
aws s3 cp lambda-email/target/lambda-email-1.0.0-SNAPSHOT.jar \
  s3://<bucket>/lambda/dev/lambda-email.jar
aws lambda update-function-code \
  --function-name feedback-dev-email-handler \
  --s3-bucket <bucket> --s3-key lambda/dev/lambda-email.jar
```

## CI/CD

- **`build.yml`** — Build e testes em PRs (Quarkus, Spring, Lambda em paralelo + Terraform validate)
- **`develop.yml`** → **`terraform.yml`** — Deploy automático para DEV ao fazer push na branch `develop`
- **`main.yml`** → **`terraform.yml`** — Deploy automático para PRD ao fazer push na branch `main`

## Monitoramento

- **Quarkus**: `/health` (liveness/readiness), `/metrics` (Prometheus)
- **Spring**: `/actuator/health`, `/actuator/metrics`
- **Lambda**: CloudWatch Logs com retenção de 14 dias
- **DynamoDB**: métricas nativas do CloudWatch
