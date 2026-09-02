# SpaceCrop — 1º Checkpoint | Containers em Nuvem (ACR/ACI)

Projeto desenvolvido para o checkpoint de **DevOps Tools & Cloud Computing** da FIAP.

O objetivo desta entrega é demonstrar a conteinerização de uma aplicação Java/Spring Boot e de um banco relacional, o build local das imagens, o registro no Azure Container Registry (ACR), o deploy em dois Azure Container Instances (ACI), a persistência do banco em Azure Storage e a validação do CRUD diretamente no banco por meio de `SELECT`.

## Integrantes

| Nome | RM |
|---|---:|
| Lucas Grillo Alcântara | 561413 |
| Pietro Ferreira Gomes Abrahamian | 561469 |
| Pedro Peres Benitez | 561792 |
| Lucca Ramos Mussumecci | 562027 |

## Stack

- Java 21
- Spring Boot
- Maven
- Oracle Database Free 23c
- Docker
- Docker Compose
- Azure CLI
- Azure Container Registry (ACR)
- Azure Container Instances (ACI)
- Azure Storage Account / Azure File Share
- Swagger / OpenAPI

## Arquitetura da entrega

O fluxo da solução é:

```text
Código-fonte
    │
    ├── docker/Dockerfile.app ──> imagem da API
    │
    └── docker/Dockerfile.db  ──> imagem Oracle + DDL
              │
              ▼
      Build local / teste local
              │
              │ docker push
              ▼
        Azure Container Registry
              │
       ┌──────┴──────┐
       ▼             ▼
    ACI App        ACI DB
    :8080          :1521
       │             │
       │ DATABASE_URL│
       └──────► FQDN público
                     │
                     ▼
              Oracle FREEPDB1
                     │
                     ▼
              Azure File Share
                 oracle-data
```

No ambiente local, o `docker-compose.yml` cria uma rede Docker para que a API utilize `oracle-db` como hostname. No Azure, são criados **dois ACIs separados**, conforme o checkpoint, e a aplicação recebe o FQDN público do ACI do banco.

## Estrutura principal

```text
docker-compose.yml
.env.example
.dockerignore
pom.xml

Docker/
  Dockerfile.app
  Dockerfile.db
  spacecrop_ddl.sql

scripts/
  azure-deploy.sh
  azure-teardown.sh

src/
  main/
    java/br/com/fiap/spacecrop/
    resources/application.properties
  test/
```

O DDL existe em uma única localização: `docker/spacecrop_ddl.sql`. O banco Docker o copia para `/container-entrypoint-initdb.d/` durante a construção da imagem.

## 1. Pré-requisitos

Instale e autentique:

- Docker Desktop ou Docker Engine com Docker Compose
- Azure CLI
- JDK 21 (opcional para execução fora do container)
- Maven (opcional; o projeto possui Maven Wrapper)

Login no Azure:

```bash
az login
az account show
```

## 2. Configuração local

Copie o arquivo de exemplo:

```bash
cp .env.example .env
```

No Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Edite `.env` e defina valores reais:

```dotenv
ORACLE_PASSWORD=senha-do-oracle
APP_USER=spacecrop
APP_USER_PASSWORD=senha-do-app
JWT_SECRET=uma-chave-secreta-longa
JWT_EXPIRATION=86400000
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
```

O arquivo `.env` está no `.gitignore` e não deve ser versionado.

## 3. Build local das imagens

O checkpoint exige que as imagens sejam construídas localmente antes do envio ao ACR.

```bash
docker build -t rm561413-spacecrop-db:latest -f docker/Dockerfile.db docker/

docker build -t rm561413-spacecrop-app:latest -f docker/Dockerfile.app .
```

Verifique:

```bash
docker images | grep rm561413-spacecrop
```

## 4. Teste local com Docker Compose

Suba o banco e a API:

```bash
docker compose up --build -d
```

Verifique o estado:

```bash
docker compose ps
```

O Oracle precisa aparecer como `healthy` antes da API ser iniciada.

Acompanhe os logs:

```bash
docker compose logs -f oracle-db
```

Em outro terminal:

```bash
docker compose logs -f spacecrop-api
```

API:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger
```

Para testar novamente o banco do zero, remova também o volume:

```bash
docker compose down -v
```

Para apenas parar os containers sem apagar os dados:

```bash
docker compose down
```

## 5. Publicação no ACR e deploy no ACI

Depois de validar localmente, execute:

```bash
chmod +x scripts/azure-deploy.sh
./scripts/azure-deploy.sh
```

O script cria, via Azure CLI:

1. Resource Group `rm561413-spacecrop-rg`
2. ACR `rm561413acr`
3. Build local das imagens
4. Push das duas imagens para o ACR
5. Storage Account `rm561413storage`
6. Azure File Share `oracle-data`
7. ACI do banco `rm561413-db`
8. ACI da aplicação `rm561413-app`

As imagens possuem o RM do representante no nome:

```text
rm561413-spacecrop-db:latest
rm561413-spacecrop-app:latest
```

## 6. Persistência do banco

O ACI do Oracle monta o Azure File Share:

```text
Azure Storage Account
        │
        ▼
     oracle-data
        │
        ▼
/opt/oracle/oradata
        │
        ▼
Oracle Database
```

Isso atende ao requisito de persistir os dados do banco em uma Conta de Armazenamento.

Importante: recriar apenas o ACI não significa apagar os dados persistidos. Para um reset completo do ambiente de checkpoint, utilize o teardown abaixo, que remove o Resource Group e todos os recursos dentro dele.

## 7. Testes em nuvem

Liste os ACIs:

```bash
az container list \
  --resource-group rm561413-spacecrop-rg \
  --output table
```

Logs da API:

```bash
az container logs \
  --resource-group rm561413-spacecrop-rg \
  --name rm561413-app
```

Logs do banco:

```bash
az container logs \
  --resource-group rm561413-spacecrop-rg \
  --name rm561413-db
```

Obtenha o endereço público da API:

```bash
az container show \
  --resource-group rm561413-spacecrop-rg \
  --name rm561413-app \
  --query ipAddress.fqdn \
  --output tsv
```

Swagger:

```text
http://<APP_FQDN>:8080/swagger
```

## 8. Evidência do CRUD no banco

Após executar operações pela API, conecte-se ao Oracle usando o FQDN do ACI do banco e consulte diretamente as tabelas.

Exemplos:

```sql
SELECT * FROM TB_USUARIO;
SELECT * FROM TB_FAZENDA;
SELECT * FROM TB_SETOR_PLANTIO;
SELECT * FROM TB_LEITURA_SATELITE;
SELECT * FROM TB_ALERTA;
```

Para a demonstração em vídeo, a evidência deve mostrar a alteração refletida no banco, por exemplo:

```text
POST / recurso
       │
       ▼
SELECT * FROM tabela;

PUT / recurso/{id}
       │
       ▼
SELECT * FROM tabela;

DELETE / recurso/{id}
       │
       ▼
SELECT * FROM tabela;
```

## 9. Limpeza do ambiente Azure

```bash
./scripts/azure-teardown.sh
```

O script aguarda a conclusão da exclusão do Resource Group. Não utiliza `--no-wait`, evitando iniciar outro deploy enquanto a infraestrutura anterior ainda está sendo removida.

## 10. Conformidade com o checkpoint

| Requisito | Implementação |
|---|---|
| Aplicação Java ou .NET em container | Java 21 + Spring Boot |
| Banco relacional em container | Oracle Database Free 23c |
| Banco em container na nuvem | ACI `rm561413-db` |
| App em container na nuvem | ACI `rm561413-app` |
| Dockerfile do App | `docker/Dockerfile.app` |
| Dockerfile do Banco | `docker/Dockerfile.db` |
| Build local | `docker build` |
| Teste local | `docker-compose.yml` |
| Registro das imagens | Azure Container Registry |
| RM no nome das imagens | `rm561413-spacecrop-*` |
| Dois ACIs | App + Banco |
| Persistência | Azure Storage Account + Azure File Share |
| Testes em nuvem | Swagger/API + logs Azure CLI |
| Evidência via SELECT | SQL direto no Oracle |
| Recursos criados via Azure CLI | `scripts/azure-deploy.sh` |
| App sem root/admin | `USER lga` no Dockerfile da API |
| H2 | Não utilizado |
| Comandos build/push documentados | Este README |
