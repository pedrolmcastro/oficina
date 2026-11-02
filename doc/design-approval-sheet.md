# Design Approval Sheet

- **Projeto:** Oficina
- **Versão:** 1.0.0

## Requisitos

### Requisitos funcionais

Os requisitos funcionais do sistema foram descritos em [(/src/bdd/resources/features/](/src/bdd/resources/features/)
utilizando Gherkin e práticas de BDD com validação automática

### Requisitos não funcionais

#### Desempenho e capacidade

- Responder às operações usuais em até 2 segundos em 95% dos casos.
- Suportar o acesso simultâneo de funcionários e clientes sem prejudicar o atendimento.

#### Disponibilidade

- Garantir disponibilidade mensal mínima de 99,5% durante o expediente.
- Realizar manutenções programadas fora do horário de atendimento.

#### Segurança e privacidade

- Restringir o acesso aos dados conforme o perfil e o vínculo do usuário.
- Proteger dados pessoais e credenciais no armazenamento e na comunicação.

#### Confiabilidade e recuperação

- Manter a consistência dos dados em acessos simultâneos e falhas de operação

#### Rastreabilidade e monitoramento

- Registrar responsável, data e alterações relevantes para o negócio
- Monitorar falhas e indisponibilidade para agilizar a identificação de problemas

#### Manutenibilidade

- Validar fluxos com testes automatizados com cobertura mínima de 80% e ideal de 100%

## Modelo C4

### Context

[![C4: Context](./c4/context.jpg)](https://miro.com/app/board/uXjVETL6ylw=/?share_link_id=299706973475)

### Containers

[![C4: Containers](./c4/containers.jpg)](https://miro.com/app/board/uXjVETK2SB8=/?share_link_id=438686653458)

### Components

TODO

### Code

Esse projeto não contém um diagrama para o nível de Code do C4.

## Event Storming

[![Event Storming](./event-storming.jpg)](https://miro.com/app/board/uXjVETFbHdA=/?share_link_id=691304045105)
