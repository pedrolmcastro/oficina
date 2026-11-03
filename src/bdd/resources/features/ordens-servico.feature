# language: pt
@ordens-servico
Funcionalidade: Conduzir uma ordem de serviço
  O administrador abre a OS e conduz o diagnóstico, a execução e a entrega do veículo.

  Esquema do Cenário: Abrir OS com os serviços contratados e insumos necessários
    Dado que existe um veículo e um serviço com diagnóstico "<diagnostico>" para abrir a OS
    Quando abro a OS para o veículo com o serviço cadastrado
    Então a API deve retornar 201
    E a OS criada deve pertencer ao veículo e ao cliente cadastrados
    E a OS deve estar em "<status>" com total "140.00"
    E os insumos necessários da OS devem ter as quantidades:
      | nome | origem      | quantidade | reservada | consumida | pendente |
      | Óleo | obrigatório |          4 |         0 |         0 |        4 |

    Exemplos:
      | diagnostico | status                 |
      | requer      | aguardando-diagnostico |
      | não requer  | aguardando-confirmacao |

  Esquema do Cenário: Rejeitar criação com dados inválidos
    Dado que existe um veículo e um serviço com diagnóstico "não requer" para abrir a OS
    Quando tento criar OS com referência "<referencia>"
    Então a API deve retornar <status>
    Quando consulto as OS
    Então a API deve retornar 200
    E a lista de OS deve estar vazia

    Exemplos:
      | referencia          | status |
      | veículo inexistente |    404 |
      | serviço inexistente |    404 |
      | serviço repetido    |    400 |
      | sem serviços        |    400 |

  Cenário: Manter o mesmo insumo obrigatório separado por serviço contratado
    Dado uma OS com dois serviços, dos quais apenas um requer diagnóstico
    Quando consulto a OS administrativamente
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-diagnostico" com total "250.00"
    E o insumo obrigatório mantém a quantidade de cada serviço contratado

  Cenário: Iniciar diagnóstico
    Dado uma OS em "aguardando-diagnostico"
    Quando executo a ação administrativa "inicio-diagnostico" na OS
    Então a API deve retornar 200
    E a OS deve estar em "em-diagnostico" com total "140.00"
    E o histórico da OS contém em ordem:
      | CRIADA               |
      | DIAGNOSTICO_INICIADO |

  Cenário: Concluir diagnóstico com insumos avulsos no orçamento
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 3
    Quando executo a ação administrativa "fim-diagnostico" na OS
    Então a API deve retornar 200
    E a OS deve estar em "aguardando-confirmacao" com total "170.00"
    E o histórico da OS contém em ordem:
      | CRIADA                   |
      | DIAGNOSTICO_INICIADO     |
      | INSUMO_AVULSO_ADICIONADO |
      | DIAGNOSTICO_CONCLUIDO    |

  Cenário: Concluir execução e liberar o veículo para retirada
    Dado uma OS em "em-execucao"
    Quando executo a ação administrativa "fim-execucao" na OS
    Então a API deve retornar 200
    E a resposta deve conter a OS:
      | status              | total  | resultado          |
      | aguardando-retirada | 140.00 | EXECUCAO_CONCLUIDA |
    E o cliente original recebe 1 email de acompanhamento, 1 email de decisão e 1 email de retirada

  Cenário: Entregar veículo após a execução
    Dado uma OS em "aguardando-retirada"
    Quando executo a ação administrativa "retirada-veiculo" na OS
    Então a API deve retornar 200
    E a OS deve estar em "entregue" com total "140.00"
    E o cliente original recebe 1 email de acompanhamento, 1 email de decisão e 1 email de retirada
    E o histórico da OS contém em ordem:
      | CRIADA                |
      | DIAGNOSTICO_INICIADO  |
      | DIAGNOSTICO_CONCLUIDO |
      | ORCAMENTO_APROVADO    |
      | INSUMO_RESERVADO      |
      | INSUMOS_RESERVADOS    |
      | EXECUCAO_INICIADA     |
      | EXECUCAO_CONCLUIDA    |
      | VEICULO_RETIRADO      |

  Cenário: Entregar veículo após a rejeição do orçamento
    Dado uma OS com um serviço que "não requer" diagnóstico
    E o orçamento já foi "rejeitado" pelo cliente
    Quando executo a ação administrativa "retirada-veiculo" na OS
    Então a resposta deve conter a OS:
      | status   | total  | resultado           |
      | entregue | 140.00 | ORCAMENTO_REJEITADO |
    E o histórico da OS contém em ordem:
      | CRIADA                 |
      | DIAGNOSTICO_DISPENSADO |
      | ORCAMENTO_REJEITADO    |
      | VEICULO_RETIRADO       |

  Esquema do Cenário: Rejeitar avanço da OS fora da etapa permitida
    Dado uma OS em "<status>"
    Quando executo a ação administrativa "<acao>" na OS
    Então a API deve retornar 409
    Quando consulto a OS administrativamente
    Então a API deve retornar 200
    E a OS deve estar em "<status>"

    Exemplos:
      | status                 | acao               |
      | aguardando-diagnostico | fim-diagnostico    |
      | aguardando-diagnostico | inicio-execucao    |
      | aguardando-diagnostico | fim-execucao       |
      | aguardando-diagnostico | retirada-veiculo   |
      | em-diagnostico         | inicio-diagnostico |
      | em-diagnostico         | inicio-execucao    |
      | aguardando-confirmacao | inicio-execucao    |
      | aguardando-execucao    | fim-execucao       |
      | em-execucao            | retirada-veiculo   |
      | aguardando-retirada    | fim-execucao       |

  Cenário: Preservar orçamento diante de alterações nos cadastros de serviços e insumos
    Dado uma OS com um serviço que "requer" diagnóstico
    Quando os preços e nomes dos serviços e insumos mudam nos cadastros
    E consulto a OS administrativamente
    Então a OS deve estar em "aguardando-diagnostico" com total "140.00"
    E os nomes contratados são preservados
    Quando o cliente consulta sua OS pelo link
    Então a API deve retornar 200
    E o orçamento do cliente deve conter os itens contratados

  Esquema do Cenário: Consultar OS por status
    Dado uma OS em "<statusAtual>"
    Quando consulto as OS com status "<filtro>"
    Então a API deve retornar 200
    E a lista de OS contém <quantidade> OS

    Exemplos:
      | statusAtual            | filtro                 | quantidade |
      | aguardando-diagnostico | aguardando-diagnostico |          1 |
      | aguardando-diagnostico | em-diagnostico         |          0 |
      | em-diagnostico         | em-diagnostico         |          1 |

  Cenário: Rejeitar filtro de status inexistente
    Quando consulto as OS com status "inexistente"
    Então a API deve retornar 400

  Cenário: Consultar OS inexistente
    Quando consulto a OS pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404

  Cenário: Consultar OS sem ordens cadastradas
    Quando consulto as OS
    Então a API deve retornar 200
    E a lista de OS deve estar vazia

  Cenário: Consultar OS com paginação mantendo o filtro de status
    Dado que existem 2 OS aguardando diagnóstico
    Quando consulto as OS aguardando diagnóstico com limite 1
    Então a API deve retornar 200
    E a página deve conter a OS 1 em ordem de ID
    E a resposta deve indicar a próxima página de OS
    Quando consulto a próxima página de OS aguardando diagnóstico
    Então a API deve retornar 200
    E a página deve conter a OS 2 em ordem de ID
    E a resposta deve indicar o fim da lista de OS

  Regra: A inclusão de avulsos exige diagnóstico iniciado

    Cenário: Rejeitar inclusão de insumo antes de iniciar diagnóstico
      Dado uma OS em "aguardando-diagnostico"
      Quando adiciono um insumo avulso com quantidade 1
      Então a API deve retornar 409

  Regra: Durante o diagnóstico, apenas insumos avulsos podem ser alterados

    Contexto:
      Dado uma OS em "em-diagnostico"

    Cenário: Adicionar insumo avulso
      Quando adiciono um insumo avulso com quantidade 3
      Então a API deve retornar 201
      E o item avulso tem quantidade 3

    Cenário: Rejeitar inclusão duplicada de insumo avulso
      Dado um insumo avulso incluído com quantidade 3
      Quando adiciono um insumo avulso com quantidade 2
      Então a API deve retornar 409

    Esquema do Cenário: Alterar quantidade de insumo avulso
      Dado um insumo avulso incluído com quantidade 3
      Quando altero o item "avulso" para quantidade <quantidade>
      Então a API deve retornar 200
      E o item avulso tem quantidade <quantidade>
      E o item avulso tem preço unitário "10.00" e subtotal "<subtotal>"
      Quando consulto a OS administrativamente
      Então a API deve retornar 200
      E a OS deve estar em "em-diagnostico" com total "<total>"
      Quando executo a ação administrativa "fim-diagnostico" na OS
      Então a API deve retornar 200
      Quando o cliente consulta o orçamento pelo link de decisão
      Então a API deve retornar 200
      E a OS deve estar em "aguardando-confirmacao" com total "<total>"
      E o orçamento do cliente contém o avulso com quantidade <quantidade> e subtotal "<subtotal>"

      Exemplos:
        | quantidade | subtotal | total  |
        |          5 | 50.00    | 190.00 |
        |          1 | 10.00    | 150.00 |
        |          3 | 30.00    | 170.00 |

    Cenário: Consultar insumos necessários
      Dado um insumo avulso incluído com quantidade 1
      Quando consulto os insumos necessários da OS
      Então a API deve retornar 200
      E existem 2 itens de insumos necessários com quantidade total 5

    Cenário: Rejeitar alteração de insumo obrigatório
      Quando altero o item "automático" para quantidade 2
      Então a API deve retornar 409

    Cenário: Rejeitar remoção de insumo obrigatório
      Quando removo o item "automático"
      Então a API deve retornar 409

    Cenário: Remover insumo avulso
      Dado um insumo avulso incluído com quantidade 3
      Quando removo o item "avulso"
      Então a API deve retornar 204
      E o histórico da OS contém em ordem:
        | CRIADA                   |
        | DIAGNOSTICO_INICIADO     |
        | INSUMO_AVULSO_ADICIONADO |
        | INSUMO_AVULSO_REMOVIDO   |
      Quando consulto os insumos necessários da OS
      Então a API deve retornar 200
      E existe 1 item de insumo necessário com quantidade total 4
      Quando consulto a OS administrativamente
      Então a API deve retornar 200
      E a OS deve estar em "em-diagnostico" com total "140.00"
      Quando executo a ação administrativa "fim-diagnostico" na OS
      Então a API deve retornar 200
      Quando o cliente consulta o orçamento pelo link de decisão
      Então a API deve retornar 200
      E a OS deve estar em "aguardando-confirmacao" com total "140.00"
      E o orçamento do cliente deve conter os itens contratados

    Cenário: Rejeitar remoção de insumo avulso inexistente
      Quando removo o insumo necessário pelo ID "00000000-0000-0000-0000-000000000001"
      Então a API deve retornar 404

    Esquema do Cenário: Rejeitar inclusão com quantidade não positiva
      Quando adiciono um insumo avulso com quantidade <quantidade>
      Então a API deve retornar 400

      Exemplos:
        | quantidade |
        |          0 |
        |         -1 |

    Esquema do Cenário: Rejeitar alteração com dados inválidos
      Dado um insumo avulso incluído com quantidade 1
      Quando altero o item "avulso" para quantidade <quantidade>
      Então a API deve retornar 400
      Quando consulto os insumos necessários da OS
      Então existem 2 itens de insumos necessários com quantidade total 5

      Exemplos:
        | quantidade |
        |          0 |
        |         -1 |

    Cenário: Manter quantidades independentes para insumos avulsos diferentes
      Dado um insumo avulso incluído com quantidade 1
      E outro insumo disponível para inclusão como avulso
      Quando adiciono um insumo avulso com quantidade 2
      Então a API deve retornar 201
      Quando consulto os insumos necessários da OS
      Então existem 3 itens de insumos necessários com quantidade total 7
      Quando consulto a OS administrativamente
      Então a API deve retornar 200
      E a OS deve estar em "em-diagnostico" com total "190.00"

  Regra: Após concluir o diagnóstico, os insumos do orçamento não podem ser alterados

    Contexto:
      Dado uma OS em "em-diagnostico"

    Cenário: Rejeitar inclusão de insumo após o diagnóstico
      Dado o diagnóstico foi concluído
      Quando adiciono um insumo avulso com quantidade 2
      Então a API deve retornar 409

    Cenário: Rejeitar alteração de insumo após o diagnóstico
      Dado um insumo avulso incluído com quantidade 1
      E o diagnóstico foi concluído
      Quando altero o item "avulso" para quantidade 3
      Então a API deve retornar 409

    Cenário: Rejeitar remoção de insumo após o diagnóstico
      Dado um insumo avulso incluído com quantidade 1
      E o diagnóstico foi concluído
      Quando removo o item "avulso"
      Então a API deve retornar 409
