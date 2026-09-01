# SpaceCrop – Checkpoint Containers em Nuvem (ACR/ACI)

> Reaproveita o projeto Java/Spring Boot `spacecrop-devops` do semestre
> anterior, migrando o deploy de VM + docker-compose para **Azure
> Container Registry (ACR)** + **Azure Container Instances (ACI)**.

Grupo: RM561413 (representante) e demais integrantes — ver README
principal do repositório.

---

## O que muda em relação ao projeto anterior

| Antes (semestre passado) | Agora (checkpoint) |
|---|---|
| 1 VM Ubuntu criada via `azure-setup.sh` | Sem VM — apenas ACR + ACI |
| `docker-compose.yml` na VM | 2 imagens publicadas no ACR e 2 ACIs |
| Volume Docker local (`oracle-data`) | Azure File Share montado no ACI do banco |
| Rede `bridge` do compose | Cada ACI com IP público próprio; app fala com o banco pelo FQDN do ACI de banco |

---

## Pré-requisitos

- Docker instalado e rodando localmente
- Azure CLI instalado e autenticado (`az login`)
- Uma assinatura Azure ativa

---

## Estrutura de arquivos entregues

```
docker/
  Dockerfile.app        # imagem da API (Java 21 / Spring Boot)
  Dockerfile.db          # imagem do Oracle Free com o DDL já embutido
  spacecrop_ddl.sql      # script de banco (tabelas, sequences, índices)
scripts/
  azure-deploy.sh         # provisiona TUDO via Azure CLI (RG, ACR, Storage, ACIs)
  azure-teardown.sh       # remove todos os recursos criados
tests-json/
  auth/                   # login.json, register.json
  usuario/                # put_update.json
  fazenda/                # post_create.json, put_update.json
  setor-plantio/          # post_create.json, put_update.json
  leitura/                # post_create.json
  alerta/                 # put_resolver.json
```

---

## Passo a passo (How To)

### 1) Configurar variáveis de ambiente sensíveis

Antes de rodar o script, exporte as senhas (nunca commitar em texto puro):

```bash
export ORACLE_PASSWORD="SenhaForte#2026"
export APP_USER_PASSWORD="SenhaAppForte#2026"
export JWT_SECRET="uma-chave-secreta-bem-grande-e-aleatoria"
```

### 2) Testar localmente antes de subir para a nuvem

```bash
# Build das imagens localmente
docker build -t rm561413-spacecrop-db  -f docker/Dockerfile.db docker/
docker build -t rm561413-spacecrop-app -f docker/Dockerfile.app .

# Subir localmente com docker-compose para validar
docker compose up --build -d
docker ps
docker logs api-rm561413
```

Acesse `http://localhost:8080/swagger` e valide o CRUD localmente
usando os JSONs em `tests-json/` antes de ir para a nuvem.

```bash
docker compose down
```

### 3) Provisionar tudo na Azure (ACR + Storage + ACIs)

```bash
chmod +x scripts/azure-deploy.sh
./scripts/azure-deploy.sh
```

O script `azure-deploy.sh` faz, nesta ordem, via **Azure CLI**:

1. Cria o Resource Group `rm561413-spacecrop-rg`
2. Cria o Azure Container Registry `rm561413acr`
3. Faz `docker build` das duas imagens e `docker push` para o ACR
4. Cria a Conta de Armazenamento + File Share `oracle-data` (persistência)
5. Cria o ACI **rm561413-db** (Oracle), montando o File Share em
   `/opt/oracle/oradata`
6. Cria o ACI **rm561413-app** (API), apontando `DATABASE_URL` para o
   FQDN do ACI de banco

Ao final, o script imprime as URLs públicas da API e do banco.

### 4) Validar em nuvem

```bash
az container list -g rm561413-spacecrop-rg -o table
az container logs -g rm561413-spacecrop-rg -n rm561413-app
```

Acesse `http://<APP_FQDN>:8080/swagger` e repita as chamadas de
CRUD usando os arquivos de `tests-json/` (primeiro `auth/login.json`
para obter o token JWT, depois as demais rotas com o header
`Authorization: Bearer <token>`).

### 5) Evidenciar o CRUD direto no banco (SELECT)

Conecte-se ao Oracle publicado (porta 1521) com qualquer cliente
(SQL Developer, DBeaver, `sqlplus`) usando o FQDN impresso pelo
script e rode, por exemplo:

```sql
SELECT * FROM TB_USUARIO;
SELECT * FROM TB_FAZENDA;
SELECT * FROM TB_SETOR_PLANTIO;
SELECT * FROM TB_LEITURA_SATELITE;
SELECT * FROM TB_ALERTA;
```

Grave essas telas no vídeo, uma consulta após cada operação do
CRUD (POST → SELECT, PUT → SELECT, DELETE → SELECT).

### 6) Encerrar / limpar os recursos (para não gerar custo)

```bash
./scripts/azure-teardown.sh
```

---

## Observações importantes

- **Sem root no container da API**: o `Dockerfile.app` cria o usuário
  `lga` e roda a aplicação com `USER lga` (sem privilégios administrativos).
- **Sem banco H2**: mantivemos o Oracle Database Free 23c, já usado no
  semestre anterior, agora containerizado com Dockerfile próprio.
- **Persistência**: os dados do Oracle ficam no Azure File Share
  `oracle-data`, montado no ACI do banco — sobrevivem a reinícios/recriações
  do container.
- **Segredos**: `ORACLE_PASSWORD`, `APP_USER_PASSWORD` e `JWT_SECRET`
  nunca são commitados — são passados por variável de ambiente no
  momento do deploy.

# (README ANTIGO) SpaceCrop DevOps

> Global Solution 2026/1 — FIAP  
> Infraestrutura em nuvem para monitoramento agrícola inteligente utilizando Azure, Docker e Oracle Database.

---

## Integrantes

| Nome | RM |
|--------|--------|
| Lucas Grillo Alcântara | 561413 |
| Pietro Ferreira Gomes Abrahamian | 561469 |
| Pedro Peres Benitez | 561792 |
| Lucca Ramos Mussumecci | 562027 |

---

## Descrição

O SpaceCrop é uma plataforma voltada ao monitoramento agrícola por meio de dados coletados por sensores orbitais. Nesta entrega, o foco está na implantação da solução utilizando práticas de DevOps, com provisionamento de infraestrutura em nuvem, containerização, persistência de dados e disponibilização da aplicação para acesso externo.

---

## Repositório

https://github.com/lgaxd/spacecrop-devops

---

## Vídeo de Apresentação

https://www.youtube.com/watch?v=cIKP2-FXKSE

---

## Arquitetura da Solução

![Arquitetura SpaceCrop](diagrama-completo-devops.png)

---

## Tecnologias Utilizadas

### Infraestrutura

- Microsoft Azure
- Azure CLI
- Ubuntu Server 24.04 LTS

### Containers

- Docker
- Docker Compose

### Banco de Dados

- Oracle Database Free 23c

### Aplicação

- Java 21
- Spring Boot
- Maven

### Documentação

- Swagger / OpenAPI

---

## Provisionamento da Infraestrutura

A infraestrutura é provisionada utilizando Azure CLI.

Recursos criados:

- Resource Group
- Máquina Virtual Ubuntu
- Public IP
- Network Security Group
- Docker Engine
- Docker Compose
- Java 21
- Maven

---

## Estrutura dos Containers

### spacecrop-api

Container responsável pela execução da API REST.

Porta exposta:

```text
8080
```

### oracle-db

Container responsável pela persistência dos dados.

Porta utilizada:

```text
1521
```

---

## Rede e Persistência

### Rede Docker

```text
spacecrop-network
```

Permite comunicação segura entre API e banco de dados.

### Volume Persistente

```text
oracle-data
```

Responsável pela persistência dos dados do Oracle Database mesmo após reinicializações.

---

## Variáveis de Ambiente

Exemplo:

```bash
export ORACLE_PASSWORD=******
export APP_USER=******
export APP_USER_PASSWORD=******

export DATABASE_URL=jdbc:oracle:thin:@oracle-db:1521/FREEPDB1
export DATABASE_USERNAME=******
export DATABASE_PASSWORD=******

export JWT_SECRET=******
export JWT_EXPIRATION=86400000
```

---

## Executando o Projeto

### Clonar o repositório

```bash
git clone https://github.com/lgaxd/spacecrop-devops.git
cd spacecrop-devops
```

### Subir os containers

```bash
docker compose up --build -d
```

### Verificar containers

```bash
docker ps
```

### Verificar logs

```bash
docker logs api-rm561413
docker logs oracle-rm561413
```

---

## Health Check

A inicialização da aplicação depende do estado saudável do banco Oracle.

Fluxo:

1. Inicialização do Oracle Database
2. Execução do Health Check
3. Inicialização da API Spring Boot
4. Disponibilização do Swagger e endpoints REST

---

## Acesso

### Swagger

```text
http://IP_DA_VM:8080/swagger
```

### API

```text
http://IP_DA_VM:8080
```

---

Desenvolvido como parte da Global Solution 2026/1 — FIAP.