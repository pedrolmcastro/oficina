# Oficina

Este projeto foi desenvolvido para o Tech Challenge da Fase 1 da pós graduação de Software Architecture da FIAP. O
trabalho consiste na criação de um MVP para a API REST de um sistema de gestão de uma oficina de médio porte. Para
mais detalhes sobre a implementação do projeto confira [/doc/design-approval-sheet.md](/doc/design-approval-sheet.md).

## Como executar

Para executar a aplicação localmente, basta ter [Docker](https://docs.docker.com/get-started/get-docker/) instalado com
suporte ao plugin [Docker Compose](https://docs.docker.com/compose/install/) e executar:

```bash
docker compose up --build -d
```

Com a aplicação rodando, é possível acessar:

- A documentação interativa do Swagger em [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Os dashboards do Grafana em [http://localhost:3000](http://localhost:3000)
- O sandbox de emails do Mailpit em [http://localhost:8025](http://localhost:8025)
- Recomenda-se o uso da Collection do Postman [postman.json](/postman.json) para testar a API

> **Nota:** Tanto o Grafana quanto a aplicação têm um usuário "admin" com senha "admin"

Para encerrar a aplicação, basta executar:

```bash
docker compose down -v
```

### Como executar as validações

Para executar os passos de validação do projeto, incluindo testes de formatação, regras de estilo, análise estática,
testes unitários, testes de integração e testes de aceitação BDD basta ter o [JDK 27](https://jdk.java.net/27/)
instalado e o Docker em execução e executar:

**No Linux ou macOS:**

```bash
./gradlew check
```

**No Windows:**

```powershell
.\gradlew.bat check
```

> **Nota:** A validação não é executada via container Docker devido a utilização de Testcontainers
