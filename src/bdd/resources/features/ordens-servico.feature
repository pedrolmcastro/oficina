# language: pt
@ordens-servico
Funcionalidade: Conduzir uma ordem de serviço

  # Ciclo da ordem de serviço

  Cenário: Criar OS que requer diagnóstico
    Dado uma OS com um serviço que "requer" diagnóstico
    Quando consulto a OS administrativamente
    Então a API deve retornar 200
    E a OS está em "aguardando-diagnostico" com total "140.00"

  Cenário: Aguardar confirmação quando o serviço não requer diagnóstico
    Dado uma OS com um serviço que "não requer" diagnóstico
    Quando consulto a OS administrativamente
    Então a OS está em "aguardando-confirmacao" com total "140.00"
    E o histórico da OS contém em ordem:
      | CRIADA                 |
      | DIAGNOSTICO_DISPENSADO |

  Cenário: Iniciar diagnóstico
    Dado uma OS em "aguardando-diagnostico"
    Quando executo a ação administrativa "inicio-diagnostico" na OS
    Então a API deve retornar 200
    E a OS está em "em-diagnostico" com total "140.00"
    E o histórico da OS contém em ordem:
      | CRIADA               |
      | DIAGNOSTICO_INICIADO |

  Cenário: Concluir diagnóstico com insumos avulsos no orçamento
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 3
    Quando executo a ação administrativa "fim-diagnostico" na OS
    Então a API deve retornar 200
    E a OS está em "aguardando-confirmacao" com total "170.00"
    E o histórico da OS contém em ordem:
      | CRIADA                   |
      | DIAGNOSTICO_INICIADO     |
      | INSUMO_AVULSO_ADICIONADO |
      | DIAGNOSTICO_CONCLUIDO    |

  Cenário: Iniciar execução de orçamento aprovado
    Dado uma OS em "aguardando-execucao"
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 200
    E a OS está em "em-execucao" com total "140.00"

  Cenário: Concluir execução e liberar o veículo para retirada
    Dado uma OS em "em-execucao"
    Quando executo a ação administrativa "fim-execucao" na OS
    Então a API deve retornar 200
    E a OS está em "aguardando-retirada" com total "140.00"
    E o resultado da OS é "EXECUCAO_CONCLUIDA"

  Cenário: Entregar veículo após a execução
    Dado uma OS em "aguardando-retirada"
    Quando executo a ação administrativa "retirada-veiculo" na OS
    Então a API deve retornar 200
    E a OS está em "entregue" com total "140.00"
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
    Então a OS está em "entregue" com total "140.00"
    E o resultado da OS é "ORCAMENTO_REJEITADO"
    E o histórico da OS contém em ordem:
      | CRIADA                 |
      | DIAGNOSTICO_DISPENSADO |
      | ORCAMENTO_REJEITADO    |
      | VEICULO_RETIRADO       |

  Esquema do Cenário: Impedir transição antes do início do diagnóstico
    Dado uma OS em "aguardando-diagnostico"
    Quando executo a ação administrativa "<acao>" na OS
    Então a API deve retornar 409
    E o histórico da OS contém em ordem:
      | CRIADA |
    E a OS está em "aguardando-diagnostico" com total "140.00"

    Exemplos:
      | acao             |
      | fim-diagnostico  |
      | inicio-execucao  |
      | fim-execucao     |
      | retirada-veiculo |

  Esquema do Cenário: Recusar criação com referências ou serviços inválidos
    Dado uma OS com um serviço que "não requer" diagnóstico
    Quando tento criar OS com referência "<referencia>"
    Então a API deve retornar <status>
    E a criação inválida não persiste outra OS nem eventos

    Exemplos:
      | referencia          | status |
      | veículo inexistente |    404 |
      | serviço inexistente |    404 |
      | serviço repetido    |    400 |
      | sem serviços        |    400 |

  Cenário: Impedir inclusão de insumo antes de iniciar diagnóstico
    Dado uma OS em "aguardando-diagnostico"
    Quando adiciono um insumo avulso com quantidade 1
    Então a API deve retornar 409

  Cenário: Manter o mesmo insumo obrigatório separado por serviço contratado
    Dado uma OS com dois serviços, dos quais apenas um requer diagnóstico
    Quando consulto a OS administrativamente
    Então a OS está em "aguardando-diagnostico" com total "250.00"
    E o insumo obrigatório mantém a quantidade de cada serviço contratado

  # Acompanhamento e decisão do cliente

  Cenário: Consultar orçamento pelo link de acompanhamento
    Dado uma OS em "aguardando-confirmacao"
    Quando o cliente consulta sua OS pelo link
    Então a API deve retornar 200
    E a OS está em "aguardando-confirmacao" com total "140.00"
    E o acompanhamento expõe somente os dados necessários ao cliente

  Cenário: Aprovar orçamento
    Dado uma OS em "aguardando-confirmacao"
    E uma entrada de 4 unidades do insumo
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    E a OS está em "aguardando-execucao" com total "140.00"
    E o acompanhamento expõe somente os dados necessários ao cliente

  Cenário: Rejeitar orçamento
    Dado uma OS em "aguardando-confirmacao"
    Quando o cliente envia "rejeicao" pelo link
    Então a API deve retornar 200
    E a OS está em "aguardando-retirada" com total "140.00"
    E o resultado da OS é "ORCAMENTO_REJEITADO"
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

  Esquema do Cenário: Impedir mudança de decisão do cliente
    Dado uma OS em "aguardando-confirmacao"
    E o orçamento já foi "<decisaoAnterior>" pelo cliente
    Quando o cliente envia "<novaDecisao>" pelo link
    Então a API deve retornar 409

    Exemplos:
      | decisaoAnterior | novaDecisao |
      | aprovado        | rejeicao    |
      | rejeitado       | aprovacao   |

  Cenário: Impedir execução de orçamento rejeitado
    Dado uma OS em "aguardando-confirmacao"
    E o orçamento já foi "rejeitado" pelo cliente
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 409

  Esquema do Cenário: Impedir decisão antes do diagnóstico
    Dado uma OS em "aguardando-diagnostico"
    Quando o cliente envia "<decisao>" pelo link
    Então a API deve retornar 404

    Exemplos:
      | decisao   |
      | aprovacao |
      | rejeicao  |

  Cenário: Acompanhar a entrega do veículo
    Dado uma OS em "aguardando-retirada"
    E o veículo já foi retirado
    Quando o cliente consulta sua OS pelo link
    Então a API deve retornar 200
    E a OS está em "entregue" com total "140.00"

  # Insumos durante o diagnóstico

  Cenário: Adicionar insumo avulso
    Dado uma OS em "em-diagnostico"
    Quando adiciono um insumo avulso com quantidade 3
    Então a API deve retornar 201
    E o item avulso tem quantidade 3

  Cenário: Impedir inclusão duplicada de insumo avulso
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 3
    Quando adiciono um insumo avulso com quantidade 2
    Então a API deve retornar 409

  Esquema do Cenário: Alterar quantidade de insumo avulso
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 3
    Quando altero o item "avulso" para quantidade <quantidade>
    Então a API deve retornar 200
    E o item avulso tem quantidade <quantidade>
    E o histórico da OS contém em ordem:
      | CRIADA                   |
      | DIAGNOSTICO_INICIADO     |
      | INSUMO_AVULSO_ADICIONADO |
      | INSUMO_AVULSO_ALTERADO   |

    Exemplos:
      | quantidade |
      | 5          |
      | 1          |

  Cenário: Manter a quantidade sem registrar uma alteração desnecessária
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 3
    Quando altero o item "avulso" para quantidade 3
    Então a API deve retornar 200
    E o item avulso tem quantidade 3
    E o histórico da OS contém em ordem:
      | CRIADA                   |
      | DIAGNOSTICO_INICIADO     |
      | INSUMO_AVULSO_ADICIONADO |

  Cenário: Listar insumos obrigatórios e avulsos
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 1
    Quando listo os insumos da OS
    Então a API deve retornar 200
    E existem 2 itens de insumos com quantidade total 5

  Cenário: Impedir alteração de insumo obrigatório
    Dado uma OS em "em-diagnostico"
    Quando altero o item "automático" para quantidade 2
    Então a API deve retornar 409

  Cenário: Impedir remoção de insumo obrigatório
    Dado uma OS em "em-diagnostico"
    Quando removo o item "automático"
    Então a API deve retornar 409

  Cenário: Remover insumo avulso
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 3
    Quando removo o item "avulso"
    Então a API deve retornar 204
    E a resposta não deve conter conteúdo nem permitir cache
    E o histórico da OS contém em ordem:
      | CRIADA                   |
      | DIAGNOSTICO_INICIADO     |
      | INSUMO_AVULSO_ADICIONADO |
      | INSUMO_AVULSO_REMOVIDO   |

  Cenário: Manter os insumos obrigatórios após remover o avulso
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 3
    E o insumo avulso já foi removido
    Quando listo os insumos da OS
    Então existe 1 item de insumo com quantidade total 4

  Cenário: Recusar remoção de insumo avulso já removido
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 3
    E o insumo avulso já foi removido
    Quando removo o item "avulso"
    Então a API deve retornar 404

  Esquema do Cenário: Recusar inclusão com quantidade não positiva
    Dado uma OS em "em-diagnostico"
    Quando adiciono um insumo avulso com quantidade <quantidade>
    Então a API deve retornar 400

    Exemplos:
      | quantidade |
      | 0          |
      | -1         |

  Esquema do Cenário: Recusar alteração para quantidade não positiva
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 1
    Quando altero o item "avulso" para quantidade <quantidade>
    Então a API deve retornar 400
    Quando listo os insumos da OS
    Então existem 2 itens de insumos com quantidade total 5

    Exemplos:
      | quantidade |
      | 0          |
      | -1         |

  Cenário: Impedir inclusão de insumo após o diagnóstico
    Dado uma OS em "em-diagnostico"
    E o diagnóstico foi concluído
    Quando adiciono um insumo avulso com quantidade 2
    Então a API deve retornar 409

  Cenário: Impedir alteração de insumo após o diagnóstico
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 1
    E o diagnóstico foi concluído
    Quando altero o item "avulso" para quantidade 3
    Então a API deve retornar 409

  Cenário: Impedir remoção de insumo após o diagnóstico
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 1
    E o diagnóstico foi concluído
    Quando removo o item "avulso"
    Então a API deve retornar 409

  Cenário: Manter quantidades independentes para insumos avulsos diferentes
    Dado uma OS em "em-diagnostico"
    E um insumo avulso incluído com quantidade 1
    E outro insumo disponível para inclusão como avulso
    Quando adiciono um insumo avulso com quantidade 2
    Então a API deve retornar 201
    Quando listo os insumos da OS
    Então existem 3 itens de insumos com quantidade total 7
    Quando consulto a OS administrativamente
    Então a OS está em "em-diagnostico" com total "190.00"

  # Consulta do orçamento e da fila

  Cenário: Preservar orçamento diante de alterações no catálogo
    Dado uma OS com um serviço que "requer" diagnóstico
    Quando os preços e nomes dos serviços e insumos mudam no catálogo
    E consulto a OS administrativamente
    Então a OS está em "aguardando-diagnostico" com total "140.00"
    E os nomes contratados são preservados

  Esquema do Cenário: Filtrar a fila por status da OS
    Dado uma OS em "<statusAtual>"
    Quando listo as OS com status "<filtro>"
    Então a API deve retornar 200
    E a listagem contém <quantidade> OS

    Exemplos:
      | statusAtual            | filtro                 | quantidade |
      | aguardando-diagnostico | aguardando-diagnostico | 1          |
      | aguardando-diagnostico | em-diagnostico         | 0          |
      | em-diagnostico         | em-diagnostico         | 1          |

  Cenário: Recusar filtro de status inexistente
    Quando listo as OS com status "inexistente"
    Então a API deve retornar 400

  # Controle de acesso

  Cenário: Persistir somente o hash do token de acompanhamento
    Dado uma OS com um serviço que "não requer" diagnóstico
    Então somente o hash do token está persistido

  Esquema do Cenário: Recusar token de acompanhamento inválido
    Dado uma OS com um serviço que "não requer" diagnóstico
    Quando o cliente consulta com o token "<token>"
    Então a API deve retornar 404

    Exemplos:
      | token        |
      | malformado   |
      | desconhecido |

  Cenário: Invalidar link anterior após reemissão
    Dado uma OS com um serviço que "não requer" diagnóstico
    E o link de acompanhamento foi reemitido
    Quando o cliente consulta com o token "anterior"
    Então a API deve retornar 404

  Cenário: Permitir consulta com o novo link após reemissão
    Dado uma OS com um serviço que "não requer" diagnóstico
    E o link de acompanhamento foi reemitido
    Quando o cliente consulta sua OS pelo link
    Então a API deve retornar 200

  Cenário: Recusar consulta com acesso expirado
    Dado uma OS com um serviço que "não requer" diagnóstico
    E o acesso do cliente está expirado
    Quando o cliente consulta sua OS pelo link
    Então a API deve retornar 404

  Cenário: Recusar aprovação com acesso expirado
    Dado uma OS com um serviço que "não requer" diagnóstico
    E o acesso do cliente está expirado
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 404

  Cenário: Aguardar insumos após aprovação sem estoque
    Dado uma OS em "aguardando-confirmacao"
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    E a OS está em "aguardando-insumos" com total "140.00"
    E a OS possui 0 unidades reservadas e 4 pendentes

  Cenário: Completar reservas parciais automaticamente após reposição
    Dado uma OS em "aguardando-insumos"
    Quando incremento o estoque do insumo em 2 unidades
    Então a API deve retornar 200
    E a OS possui 2 unidades reservadas e 2 pendentes
    E a OS está em "aguardando-insumos" com total "140.00"
    Quando incremento o estoque do insumo em 3 unidades
    Então a API deve retornar 200
    E a OS possui 4 unidades reservadas e 0 pendentes
    E a OS está em "aguardando-execucao" com total "140.00"

  Cenário: Impedir execução enquanto faltam insumos
    Dado uma OS em "aguardando-insumos"
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 409
