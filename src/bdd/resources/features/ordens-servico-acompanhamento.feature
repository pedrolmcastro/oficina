# language: pt
@ordens-servico
Funcionalidade: Acompanhar a OS e decidir sobre o orçamento
  O cliente acompanha a OS sem autenticação administrativa e aprova ou rejeita o orçamento com um link de decisão próprio.

  Regra: O cliente da OS recebe acompanhamento na abertura e decisão quando o orçamento está pronto

    Cenário: Enviar somente acompanhamento enquanto o diagnóstico está pendente
      Dado uma OS com um serviço que "requer" diagnóstico
      Então o cliente original recebe 1 email de acompanhamento e 0 emails de decisão
      Quando executo a ação administrativa "inicio-diagnostico" na OS
      Então a API deve retornar 200
      E o cliente original recebe 1 email de acompanhamento e 0 emails de decisão
      Quando executo a ação administrativa "fim-diagnostico" na OS
      Então a API deve retornar 200
      E o cliente original recebe 1 email de acompanhamento e 1 email de decisão
      Quando o cliente consulta sua OS pelo link
      Então a API deve retornar 200
      Quando o cliente consulta o orçamento pelo link de decisão
      Então a API deve retornar 200

    Cenário: Enviar acompanhamento e decisão na abertura quando o diagnóstico é dispensado
      Dado uma OS com um serviço que "não requer" diagnóstico
      Então o cliente original recebe 1 email de acompanhamento e 1 email de decisão

    Cenário: Aguardar diagnóstico quando apenas parte dos serviços o exige
      Dado uma OS com dois serviços, dos quais apenas um requer diagnóstico
      Então o cliente original recebe 1 email de acompanhamento e 0 emails de decisão
      Quando executo a ação administrativa "inicio-diagnostico" na OS
      Então a API deve retornar 200
      Quando executo a ação administrativa "fim-diagnostico" na OS
      Então a API deve retornar 200
      E o cliente original recebe 1 email de acompanhamento e 1 email de decisão

    Esquema do Cenário: Reemitir emails conforme a etapa da OS
      Dado uma OS em "<status>"
      E as notificações anteriores foram recebidas
      Quando reemito os links do cliente
      Então a API deve retornar 200
      E o cliente original recebe 1 email de acompanhamento e <decisao> emails de decisão

      Exemplos:
        | status                 | decisao |
        | aguardando-diagnostico |       0 |
        | em-diagnostico         |       0 |
        | aguardando-confirmacao |       1 |
        | aguardando-insumos     |       0 |
        | aguardando-execucao    |       0 |
        | em-execucao            |       0 |
        | aguardando-retirada    |       0 |

    Cenário: Manter o cliente original da OS após transferir o veículo
      Dado uma OS com um serviço que "requer" diagnóstico
      Quando transfiro o veículo da OS para outro cliente
      Então a API deve retornar 200
      Quando consulto a OS administrativamente
      Então a API deve retornar 200
      E a OS continua vinculada ao cliente original e ao mesmo veículo
      Quando executo a ação administrativa "inicio-diagnostico" na OS
      Então a API deve retornar 200
      Quando executo a ação administrativa "fim-diagnostico" na OS
      Então a API deve retornar 200
      E a OS continua vinculada ao cliente original e ao mesmo veículo
      E o cliente original recebe 1 email de acompanhamento e 1 email de decisão

  Regra: A decisão do cliente é definitiva e sua repetição não duplica efeitos

    Cenário: Rejeitar orçamento
      Dado uma OS em "aguardando-confirmacao"
      Quando o cliente envia "rejeicao" pelo link
      Então a API deve retornar 200
      E a resposta deve conter a OS:
        | status              | total  | resultado           |
        | aguardando-retirada | 140.00 | ORCAMENTO_REJEITADO |
      E o acompanhamento expõe somente os dados necessários ao cliente

    Esquema do Cenário: Aceitar repetição da mesma decisão sem duplicar eventos
      Dado uma OS com um serviço que "não requer" diagnóstico
      E o orçamento já foi "<decisaoAnterior>" pelo cliente
      Quando o cliente envia "<decisao>" pelo link
      Então a API deve retornar 200
      E o histórico da OS contém em ordem:
        | CRIADA                 |
        | DIAGNOSTICO_DISPENSADO |
        | <evento>               |

      Exemplos:
        | decisaoAnterior | decisao   | evento              |
        | aprovado        | aprovacao | ORCAMENTO_APROVADO  |
        | rejeitado       | rejeicao  | ORCAMENTO_REJEITADO |

    Esquema do Cenário: Rejeitar mudança de decisão do cliente
      Dado uma OS em "aguardando-confirmacao"
      E o orçamento já foi "<decisaoAnterior>" pelo cliente
      Quando o cliente envia "<novaDecisao>" pelo link
      Então a API deve retornar 409
      Quando consulto a OS administrativamente
      Então a API deve retornar 200
      E a OS deve estar em "<status>"

      Exemplos:
        | decisaoAnterior | novaDecisao | status              |
        | aprovado        | rejeicao    | aguardando-insumos  |
        | rejeitado       | aprovacao   | aguardando-retirada |

    Cenário: Rejeitar execução de orçamento rejeitado
      Dado uma OS em "aguardando-confirmacao"
      E o orçamento já foi "rejeitado" pelo cliente
      Quando executo a ação administrativa "inicio-execucao" na OS
      Então a API deve retornar 409

    Cenário: Acompanhar a entrega do veículo
      Dado uma OS em "aguardando-retirada"
      E o veículo já foi retirado
      Quando o cliente consulta sua OS pelo link
      Então a API deve retornar 200
      E a OS deve estar em "entregue" com total "140.00"

  Regra: Links de acompanhamento devem ser válidos, vigentes e substituídos na reemissão

    Contexto:
      Dado uma OS com um serviço que "não requer" diagnóstico

    Esquema do Cenário: Rejeitar token de acompanhamento inválido
      Quando o cliente consulta com o token "<token>"
      Então a API deve retornar 404

      Exemplos:
        | token        |
        | malformado   |
        | desconhecido |

    Cenário: Reemitir acompanhamento invalidando o link anterior e permitindo o novo
      Dado o link de acompanhamento foi reemitido
      Quando o cliente consulta com o token "anterior"
      Então a API deve retornar 404
      Quando o cliente consulta sua OS pelo link
      Então a API deve retornar 200

    Cenário: Rejeitar consulta com acesso expirado
      Dado o acesso do cliente está expirado
      Quando o cliente consulta sua OS pelo link
      Então a API deve retornar 404

    Cenário: Rejeitar aprovação com acesso expirado
      Dado o acesso do cliente está expirado
      Quando o cliente envia "aprovacao" pelo link
      Então a API deve retornar 404

  Regra: Acompanhamento e decisão expõem apenas os dados e as permissões necessários

    Esquema do Cenário: Acompanhar o andamento pelo link sem autenticação administrativa
      Dado uma OS em "<status>"
      Quando o cliente consulta sua OS pelo link
      Então a API deve retornar 200
      E a OS deve estar em "<status>" com total "140.00"
      E o acompanhamento expõe somente os dados necessários ao cliente
      E o orçamento do cliente deve conter os itens contratados

      Exemplos:
        | status                 |
        | aguardando-diagnostico |
        | em-diagnostico         |
        | aguardando-confirmacao |
        | aguardando-insumos     |
        | aguardando-execucao    |
        | em-execucao            |
        | aguardando-retirada    |

    Cenário: Consultar orçamento com o link de decisão enviado por email
      Dado uma OS em "aguardando-confirmacao"
      Quando o cliente consulta o orçamento pelo link de decisão
      Então a API deve retornar 200
      E a OS deve estar em "aguardando-confirmacao" com total "140.00"
      E o orçamento do cliente deve conter os itens contratados
      E o acompanhamento expõe somente os dados necessários ao cliente

    Esquema do Cenário: Rejeitar decisão com o link destinado somente ao acompanhamento
      Dado uma OS em "aguardando-confirmacao"
      Quando o cliente envia "<decisao>" com o link de acompanhamento
      Então a API deve retornar 404
      Quando consulto a OS administrativamente
      Então a API deve retornar 200
      E a OS deve estar em "aguardando-confirmacao" com total "140.00"

      Exemplos:
        | decisao   |
        | aprovacao |
        | rejeicao  |

    Cenário: Invalidar decisão anterior ao reemitir os links do cliente
      Dado uma OS em "aguardando-confirmacao"
      E o link de acompanhamento foi reemitido
      Quando o cliente envia "aprovacao" com o link de decisão anterior
      Então a API deve retornar 404
      Quando o cliente consulta o orçamento pelo link de decisão
      Então a API deve retornar 200
      Quando o cliente envia "aprovacao" pelo link
      Então a API deve retornar 200
      E a OS deve estar em "aguardando-insumos" com total "140.00"

    Cenário: Acompanhar a entrega após rejeitar o orçamento
      Dado uma OS com um serviço que "não requer" diagnóstico
      E o orçamento já foi "rejeitado" pelo cliente
      E o veículo já foi retirado
      Quando o cliente consulta sua OS pelo link
      Então a API deve retornar 200
      E a resposta deve conter a OS:
        | status   | resultado           |
        | entregue | ORCAMENTO_REJEITADO |
      E o acompanhamento expõe somente os dados necessários ao cliente
