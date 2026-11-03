# ADR 0001: Usar PostgreSQL

## Contexto

É necessário desenvolver um primeiro MVP da API do novo sistema de uma oficina de médio porte, substituindo anotações
manuais e planilhas. O sistema deve preservar o histórico de clientes e veículos e manter a consistência entre ordens
de serviço, orçamentos e estoque, mesmo com acessos simultâneos. Apesar de ser um MVP, espera-se que esse sistema seja
usado em produção e evolua com o tempo. A velocidade inicial de desenvolvimento é importante, mas não deve prejudicar
a manutenibilidade futura do sistema.

## Decisão

Utilizar o PostgreSQL como banco de dados do sistema.

## Justificativa

- Alta familiaridade do time
- Modelo relacional adequado aos vínculos entre clientes, veículos, ordens de serviço, serviços e insumos
- Transações e controle de concorrência para evitar reservas duplicadas e baixas inconsistentes de estoque
- Ausência de custos de licenciamento, compatível com o investimento inicial da oficina

## Consequências

### Positivas

- Garantias ACID
- Facilidades para garantir consistência de dados pela estrutura relacional
- Suporte a colunas JSONB para dados variáveis, como detalhes dos eventos das ordens de serviço
- Uso de uma ferramenta amplamente utilizada e estabelecida no mercado
- Sem custos de licenciamento e com código aberto

### Negativas

- Necessidade de operar um servidor de banco, com custos de infraestrutura e manutenção
- Necessidade de backups, testes de restauração, monitoramento e atualizações
- Alterações na estrutura dos dados exigem migrações planejadas

## Alternativas rejeitadas

### 01: Usar outro banco relacional em servidor como MySQL, Oracle Database ou SQL Server

**Vantagens:**

- Modelo relacional e suporte a transações adequados ao domínio
- Ferramentas maduras e opções de suporte comercial

**Desvantagens:**

- Menor familiaridade do time com outros bancos
- Possíveis custos de licenciamento, conforme o produto e a edição

**Justificativa da rejeição:** \
Sem benefício específico que compense a menor familiaridade do time e possíveis custos

### 02: Usar um banco relacional embedded como SQLite

**Vantagens:**

- Implantação simples, sem servidor de banco separado
- Sem custos de licenciamento
- Suporte a transações ACID

**Desvantagens:**

- Apenas uma escrita por vez no banco, limitando operações concorrentes
- Compartilhamento do banco entre múltiplas instâncias da API exige cuidados adicionais

**Justificativa da rejeição:** \
Preferência por um banco em servidor para atender acessos simultâneos e permitir ampliar as instâncias da API

### 03: Usar um banco não relacional como MongoDB

**Vantagens:**

- Modelo documental flexível para dados pouco estruturados
- Facilidade para evoluir a estrutura dos documentos sem migrações complexas

**Desvantagens:**

- Integridade entre referências depende de validações na aplicação
- Apesar de oferecer transações ACID, exige mais cuidado na modelagem de operações entre documentos

**Justificativa da rejeição:** \
- O domínio é predominantemente relacional e a flexibilidade documental não compensa o esforço adicional de consistência
