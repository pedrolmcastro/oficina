# language: pt
@ordens-servico
Funcionalidade: Reservar e consumir estoque para ordens de serviço
  A aprovação reserva os insumos disponíveis. Reposições completam as reservas e a execução consome os insumos uma única vez.

  Cenário: Reservar e consumir o mesmo insumo obrigatório e avulso separadamente
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 2
    E uma entrada de 7 unidades do insumo
    E o diagnóstico foi concluído
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-execucao" com total "160.00"
    E os insumos necessários da OS devem ter as quantidades:
      | nome | origem      | quantidade | reservada | consumida | pendente |
      | Óleo | obrigatório |          4 |         4 |         0 |        0 |
      | Óleo | avulso      |          2 |         2 |         0 |        0 |
    E o estoque utilizado pela OS deve ter 7 unidades físicas, 6 reservadas e 1 disponíveis
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 200
    E os insumos necessários da OS devem ter as quantidades:
      | nome | origem      | quantidade | reservada | consumida | pendente |
      | Óleo | obrigatório |          4 |         0 |         4 |        0 |
      | Óleo | avulso      |          2 |         0 |         2 |        0 |
    E o estoque utilizado pela OS deve ter 1 unidades físicas, 0 reservadas e 1 disponíveis

  Esquema do Cenário: Reservar apenas o saldo disponível ao aprovar o orçamento
    Dado uma OS em "aguardando-confirmacao"
    E uma entrada de <estoque> unidades do insumo
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    E a OS deve estar em "<status>" com total "140.00"
    E a OS possui <reservada> unidades reservadas e <pendente> pendentes
    E o estoque utilizado pela OS deve ter <estoque> unidades físicas, <reservada> reservadas e <disponivel> disponíveis

    Exemplos:
      | estoque | reservada | pendente | disponivel | status              |
      |       2 |         2 |        2 |          0 | aguardando-insumos  |
      |       4 |         4 |        0 |          0 | aguardando-execucao |
      |       6 |         4 |        0 |          2 | aguardando-execucao |

  Cenário: Aprovar orçamento sem estoque
    Dado uma OS em "aguardando-confirmacao"
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-insumos" com total "140.00"
    E a OS possui 0 unidades reservadas e 4 pendentes
    E o estoque utilizado pela OS deve ter 0 unidades físicas, 0 reservadas e 0 disponíveis

  Cenário: Completar reservas parciais automaticamente após reposição
    Dado uma OS em "aguardando-insumos"
    Quando incremento o estoque do insumo em 2 unidades
    Então a API deve retornar 200
    E a OS possui 2 unidades reservadas e 2 pendentes
    E a OS deve estar em "aguardando-insumos" com total "140.00"
    E o estoque utilizado pela OS deve ter 2 unidades físicas, 2 reservadas e 0 disponíveis
    Quando incremento o estoque do insumo em 3 unidades
    Então a API deve retornar 200
    E a OS possui 4 unidades reservadas e 0 pendentes
    E a OS deve estar em "aguardando-execucao" com total "140.00"
    E o estoque utilizado pela OS deve ter 5 unidades físicas, 4 reservadas e 1 disponíveis

  Cenário: Rejeitar execução enquanto faltam insumos
    Dado uma OS em "aguardando-insumos"
    E uma entrada de 2 unidades do insumo
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 409
    E a OS possui 2 unidades reservadas e 2 pendentes
    E a OS deve estar em "aguardando-insumos" com total "140.00"
    E o estoque utilizado pela OS deve ter 2 unidades físicas, 2 reservadas e 0 disponíveis

  Cenário: Manter estoque livre durante o diagnóstico e após rejeitar o orçamento
    Dado uma OS em "em-diagnostico"
    E uma entrada de 6 unidades do insumo
    Quando executo a ação administrativa "fim-diagnostico" na OS
    Então a API deve retornar 200
    E o estoque utilizado pela OS deve ter 6 unidades físicas, 0 reservadas e 6 disponíveis
    Quando o cliente envia "rejeicao" pelo link
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-retirada" com total "140.00"
    E a OS possui 0 unidades reservadas e 4 pendentes
    E o estoque utilizado pela OS deve ter 6 unidades físicas, 0 reservadas e 6 disponíveis

  Cenário: Consumir reservas uma única vez no início da execução
    Dado uma OS em "aguardando-execucao"
    E uma entrada de 2 unidades do insumo
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 200
    E a OS deve estar em "em-execucao"
    E os insumos necessários da OS devem ter as quantidades:
      | nome | origem      | quantidade | reservada | consumida | pendente |
      | Óleo | obrigatório |          4 |         0 |         4 |        0 |
    E o estoque utilizado pela OS deve ter 2 unidades físicas, 0 reservadas e 2 disponíveis
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 409
    E o estoque utilizado pela OS deve ter 2 unidades físicas, 0 reservadas e 2 disponíveis
    Quando executo a ação administrativa "fim-execucao" na OS
    Então a API deve retornar 200
    Quando executo a ação administrativa "retirada-veiculo" na OS
    Então a API deve retornar 200
    E os insumos necessários da OS devem ter as quantidades:
      | nome | origem      | quantidade | reservada | consumida | pendente |
      | Óleo | obrigatório |          4 |         0 |         4 |        0 |
    E o estoque utilizado pela OS deve ter 2 unidades físicas, 0 reservadas e 2 disponíveis

  Cenário: Priorizar a OS mais antiga sem compartilhar suas reservas com outra OS
    Dado uma OS em "aguardando-insumos"
    E outra OS aprovada para o mesmo insumo
    Quando incremento o estoque do insumo em 6 unidades
    Então a API deve retornar 200
    E o estoque utilizado pela OS deve ter 6 unidades físicas, 6 reservadas e 0 disponíveis
    Quando consulto a OS cadastrada 1
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-execucao" com total "140.00"
    E a OS possui 4 unidades reservadas e 0 pendentes
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 200
    E o estoque utilizado pela OS deve ter 2 unidades físicas, 2 reservadas e 0 disponíveis
    Quando consulto a OS cadastrada 2
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-insumos" com total "140.00"
    E a OS possui 2 unidades reservadas e 2 pendentes
    Quando incremento o estoque do insumo em 2 unidades
    Então a API deve retornar 200
    E a OS possui 4 unidades reservadas e 0 pendentes
    E a OS deve estar em "aguardando-execucao" com total "140.00"

  Cenário: Aprovar serviço sem insumos necessários e executar sem movimentar estoque
    Dado uma OS sem insumos obrigatórios
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-execucao" com total "100.00"
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 200
    E a OS deve estar em "em-execucao" com total "100.00"
    E o estoque utilizado pela OS deve ter 0 unidades físicas, 0 reservadas e 0 disponíveis

  Cenário: Aguardar todos os insumos necessários antes de liberar a execução
    Dado uma OS em "em-diagnostico"
    E uma entrada de 4 unidades do insumo
    E outro insumo disponível para inclusão como avulso
    E um insumo avulso incluído com quantidade 2
    E o diagnóstico foi concluído
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-insumos" com total "180.00"
    E os insumos necessários da OS devem ter as quantidades:
      | nome   | origem      | quantidade | reservada | consumida | pendente |
      | Óleo   | obrigatório |          4 |         4 |         0 |        0 |
      | Filtro | avulso      |          2 |         0 |         0 |        2 |
    Quando incremento o estoque do insumo em 2 unidades
    Então a API deve retornar 200
    E a OS possui 6 unidades reservadas e 0 pendentes
    E a OS deve estar em "aguardando-execucao" com total "180.00"
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 200
    E os insumos necessários da OS devem ter as quantidades:
      | nome   | origem      | quantidade | reservada | consumida | pendente |
      | Óleo   | obrigatório |          4 |         0 |         4 |        0 |
      | Filtro | avulso      |          2 |         0 |         2 |        0 |
    E o estoque utilizado pela OS deve ter 0 unidades físicas, 0 reservadas e 0 disponíveis

  Cenário: Repetir aprovação sem reservar ou consumir novamente o estoque
    Dado uma OS em "aguardando-execucao"
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-execucao"
    E a OS possui 4 unidades reservadas e 0 pendentes
    E o estoque utilizado pela OS deve ter 4 unidades físicas, 4 reservadas e 0 disponíveis
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 200
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    E a OS deve estar em "em-execucao"
    E os insumos necessários da OS devem ter as quantidades:
      | nome | origem      | quantidade | reservada | consumida | pendente |
      | Óleo | obrigatório |          4 |         0 |         4 |        0 |
    E o estoque utilizado pela OS deve ter 0 unidades físicas, 0 reservadas e 0 disponíveis
