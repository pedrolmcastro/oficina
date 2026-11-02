# language: pt
@ordens-servico
Funcionalidade: Conduzir uma ordem de serviço
  A oficina diagnostica e executa serviços mediante aprovação do cliente por um link secreto.
  Insumos obrigatórios são imutáveis e insumos avulsos só mudam durante o diagnóstico.

  Cenário: Diagnosticar, aprovar, executar e entregar o veículo
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Quando consulto a OS administrativamente
    Então a OS está em "aguardando-diagnostico" com total "250.00"
    Quando executo a ação administrativa "inicio-diagnostico" na OS
    Então a API deve retornar 200
    E a OS está em "em-diagnostico" com total "250.00"
    Quando adiciono um insumo avulso com quantidade 3
    Então a API deve retornar 201
    Quando executo a ação administrativa "fim-diagnostico" na OS
    Então a OS está em "aguardando-confirmacao" com total "280.00"
    Quando o cliente consulta sua OS pelo link
    Então a API deve retornar 200
    E o acompanhamento expõe somente os dados necessários ao cliente
    Quando o cliente envia "aprovacao" pelo link
    Então a OS está em "aguardando-execucao" com total "280.00"
    E o acompanhamento expõe somente os dados necessários ao cliente
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 200
    Quando o cliente envia "rejeicao" pelo link
    Então a API deve retornar 409
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a OS está em "em-execucao" com total "280.00"
    Quando executo a ação administrativa "fim-execucao" na OS
    Então a OS está em "aguardando-retirada" com total "280.00"
    E o resultado da OS é "SERVICO_CONCLUIDO"
    Quando executo a ação administrativa "retirada-veiculo" na OS
    Então a OS está em "entregue" com total "280.00"
    Quando o cliente consulta sua OS pelo link
    Então a OS está em "entregue" com total "280.00"
    E o histórico da OS contém em ordem:
      | OS_CRIADA             |
      | DIAGNOSTICO_INICIADO   |
      | INSUMO_ADICIONADO      |
      | DIAGNOSTICO_CONCLUIDO  |
      | ORCAMENTO_APROVADO     |
      | EXECUCAO_INICIADA      |
      | EXECUCAO_CONCLUIDA     |
      | VEICULO_RETIRADO       |

  Cenário: Dispensar diagnóstico e rejeitar orçamento sem executar serviços
    Dado uma OS com dois serviços que "dispensam" diagnóstico
    Quando o cliente consulta sua OS pelo link
    Então a OS está em "aguardando-confirmacao" com total "250.00"
    Quando o cliente envia "rejeicao" pelo link
    Então a OS está em "aguardando-retirada" com total "250.00"
    E o resultado da OS é "ORCAMENTO_REJEITADO"
    E o acompanhamento expõe somente os dados necessários ao cliente
    Quando o cliente envia "rejeicao" pelo link
    Então a API deve retornar 200
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 409
    Quando executo a ação administrativa "inicio-execucao" na OS
    Então a API deve retornar 409
    Quando executo a ação administrativa "retirada-veiculo" na OS
    Então a OS está em "entregue" com total "250.00"
    E o histórico da OS contém em ordem:
      | OS_CRIADA              |
      | DIAGNOSTICO_DISPENSADO  |
      | ORCAMENTO_REJEITADO     |
      | VEICULO_RETIRADO        |

  Cenário: Aumentar, diminuir e remover somente a quantidade de itens avulsos
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Quando executo a ação administrativa "inicio-diagnostico" na OS
    E adiciono um insumo avulso com quantidade 3
    Então a API deve retornar 201
    Quando adiciono um insumo avulso com quantidade 2
    Então a API deve retornar 409
    Quando altero o item "avulso" para quantidade 5
    Então a API deve retornar 200
    E o item avulso tem quantidade 5
    Quando altero o item "avulso" para quantidade 5
    Então o item avulso tem quantidade 5
    Quando altero o item "avulso" para quantidade 1
    Então o item avulso tem quantidade 1
    Quando listo os insumos da OS
    Então existem 3 itens de insumos com quantidade total 6
    Quando altero o item "automático" para quantidade 2
    Então a API deve retornar 409
    Quando removo o item "automático"
    Então a API deve retornar 409
    Quando removo o item "avulso"
    Então a API deve retornar 204
    E a resposta não deve conter conteúdo nem permitir cache
    Quando removo o item "avulso"
    Então a API deve retornar 404
    Quando listo os insumos da OS
    Então existem 2 itens de insumos com quantidade total 5

  Cenário: Congelar insumos ao concluir diagnóstico
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Quando adiciono um insumo avulso com quantidade 1
    Então a API deve retornar 409
    Quando executo a ação administrativa "inicio-diagnostico" na OS
    E adiciono um insumo avulso com quantidade 1
    E executo a ação administrativa "fim-diagnostico" na OS
    E adiciono um insumo avulso com quantidade 2
    Então a API deve retornar 409
    Quando altero o item "avulso" para quantidade 3
    Então a API deve retornar 409
    Quando removo o item "avulso"
    Então a API deve retornar 409
    Quando o cliente consulta sua OS pelo link
    Então a OS está em "aguardando-confirmacao" com total "260.00"

  Esquema do Cenário: Rejeitar quantidades não positivas
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Quando executo a ação administrativa "inicio-diagnostico" na OS
    E adiciono um insumo avulso com quantidade <quantidade>
    Então a API deve retornar 400
    Quando adiciono um insumo avulso com quantidade 1
    E altero o item "avulso" para quantidade <quantidade>
    Então a API deve retornar 400
    Quando listo os insumos da OS
    Então existem 3 itens de insumos com quantidade total 6
    Exemplos:
      | quantidade |
      | 0          |
      | -1         |

  Cenário: Preservar orçamento diante de alterações no catálogo
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Quando os preços e nomes dos serviços e insumos mudam no catálogo
    E consulto a OS administrativamente
    Então a OS está em "aguardando-diagnostico" com total "250.00"
    E os nomes contratados são preservados

  Cenário: Restringir acesso do cliente por token com validade e reemissão
    Dado uma OS com dois serviços que "dispensam" diagnóstico
    Então somente o hash do token está persistido
    Quando o cliente consulta com o token "malformado"
    Então a API deve retornar 404
    Quando o cliente consulta com o token "desconhecido"
    Então a API deve retornar 404
    Quando reemitimos o link de acompanhamento
    E o cliente consulta com o token "anterior"
    Então a API deve retornar 404
    Quando o cliente consulta sua OS pelo link
    Então a API deve retornar 200
    Quando o acesso do cliente expira
    E o cliente consulta sua OS pelo link
    Então a API deve retornar 404
    Quando o cliente envia "aprovacao" pelo link
    Então a API deve retornar 404

  Cenário: Listar a fila conforme as ações da oficina
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Quando listo as OS com status "aguardando-diagnostico"
    Então a listagem contém 1 OS
    Quando listo as OS com status "em-diagnostico"
    Então a listagem contém 0 OS
    Quando executo a ação administrativa "inicio-diagnostico" na OS
    E listo as OS com status "em-diagnostico"
    Então a listagem contém 1 OS
    Quando listo as OS com status "inexistente"
    Então a API deve retornar 400

  Esquema do Cenário: Impedir transições fora de ordem
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Quando executo a ação administrativa "<acao>" na OS
    Então a API deve retornar 409
    Quando consulto a OS administrativamente
    Então a OS está em "aguardando-diagnostico" com total "250.00"
    E o histórico da OS contém em ordem:
      | OS_CRIADA |
    Exemplos:
      | acao             |
      | fim-diagnostico  |
      | inicio-execucao  |
      | fim-execucao     |
      | retirada-veiculo |

  Esquema do Cenário: Impedir decisão antes do diagnóstico
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Quando o cliente envia "<decisao>" pelo link
    Então a API deve retornar 409
    Exemplos:
      | decisao   |
      | aprovacao |
      | rejeicao  |

  Esquema do Cenário: Validar referências e serviços sem persistência parcial
    Dado uma OS com dois serviços que "dispensam" diagnóstico
    Quando tento criar OS com referência "<referencia>"
    Então a API deve retornar <status>
    E a criação inválida não persiste outra OS nem eventos
    Exemplos:
      | referencia              | status |
      | cliente inexistente     | 404    |
      | veículo inexistente     | 404    |
      | serviço inexistente     | 404    |
      | veículo de outro cliente| 400    |
      | serviço repetido        | 400    |
      | sem serviços            | 400    |

  Cenário: Restringir administração à oficina
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Então as operações administrativas exigem JWT com perfil ADMIN

  Cenário: Manter quantidades independentes para insumos avulsos diferentes
    Dado uma OS com dois serviços que "exigem" diagnóstico
    Quando executo a ação administrativa "inicio-diagnostico" na OS
    E adiciono um insumo avulso com quantidade 1
    Dado outro insumo disponível para inclusão como avulso
    Quando adiciono um insumo avulso com quantidade 2
    Então a API deve retornar 201
    Quando listo os insumos da OS
    Então existem 4 itens de insumos com quantidade total 8
    Quando consulto a OS administrativamente
    Então a OS está em "em-diagnostico" com total "300.00"
