# language: pt
@clientes
Funcionalidade: Gerenciar clientes
  O administrador deve gerenciar clientes identificados por CPF ou CNPJ.
  # Cadastro

  Esquema do Cenário: Cadastrar cliente
    Quando cadastro um cliente com nome "<nome>", email "<email>" e documento "<documento>"
    Então a API deve retornar 201
    E a resposta deve conter o cliente:
      | nome   | email   | documento   |
      | <nome> | <email> | <documento> |

    Exemplos:
      | nome        | email               | documento      |
      | Maria Silva | maria@example.com   |    52998224725 |
      | Oficina     | oficina@example.com | 11222333000181 |

  Esquema do Cenário: Rejeitar cadastro com documento já cadastrado
    Dado que existe um cliente com documento "<documento>"
    Quando cadastro um cliente com nome "Outro cliente", email "outro@example.com" e documento "<documento>"
    Então a API deve retornar 409
    E o cliente original deve permanecer inalterado

    Exemplos:
      | documento      |
      |    52998224725 |
      | 11222333000181 |

  Esquema do Cenário: Rejeitar cadastro com dados inválidos
    Quando cadastro um cliente com nome "<nome>", email "<email>" e documento "<documento>"
    Então a API deve retornar 400

    Exemplos:
      | nome        | email             | documento      |
      |             | maria@example.com |    52998224725 |
      | Maria Silva | email-invalido    |    52998224725 |
      | Maria Silva | maria@example.com |    52998224724 |
      | Maria Silva | maria@example.com | 11222333000180 |
  # Consulta

  Cenário: Consultar cliente existente pelo ID
    Dado que existe um cliente
    E que existe outro cliente
    Quando consulto o cliente cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o ID do cliente cadastrado

  Cenário: Consultar cliente inexistente pelo ID
    Quando consulto o cliente pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404

  Esquema do Cenário: Consultar cliente existente pelo documento
    Dado que existe um cliente com documento "11144477735"
    E que existe um cliente com documento "<documento>"
    Quando consulto clientes pelo documento "<documento>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista com o cliente:
      | documento   |
      | <documento> |

    Exemplos:
      | documento      |
      |    52998224725 |
      | 11222333000181 |

  Esquema do Cenário: Consultar cliente inexistente pelo documento
    Quando consulto clientes pelo documento "<documento>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista vazia de clientes

    Exemplos:
      | documento      |
      |    52998224725 |
      | 11222333000181 |

  Esquema do Cenário: Rejeitar consulta com documento inválido
    Quando consulto clientes pelo documento "<documento>"
    Então a API deve retornar 400

    Exemplos:
      | documento      |
      |                |
      |            123 |
      |    52998224724 |
      | 11222333000180 |
  # Atualização

  Esquema do Cenário: Atualizar completamente dados do cliente
    Dado que existe um cliente com documento "<original>"
    Quando atualizo o cliente cadastrado com:
      | nome      | email            | documento |
      | Novo nome | novo@example.com | <novo>    |
    Então a API deve retornar 200
    Quando consulto clientes pelo documento "<novo>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista com o cliente:
      | nome      | email            | documento |
      | Novo nome | novo@example.com | <novo>    |
    Quando consulto clientes pelo documento "<original>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista vazia de clientes

    Exemplos:
      | original    | novo           |
      | 52998224725 |    11144477735 |
      | 52998224725 | 11222333000181 |

  Esquema do Cenário: Atualizar parcialmente dados do cliente
    Dado que existe um cliente com os dados:
      | nome        | email             | documento   |
      | Maria Silva | maria@example.com | 52998224725 |
    Quando atualizo o cliente cadastrado com:
      | <campo> |
      | <valor> |
    Então a API deve retornar 200
    E a resposta deve conter o ID do cliente cadastrado
    E a resposta deve conter o cliente:
      | nome   | email   | documento   |
      | <nome> | <email> | <documento> |
    Quando consulto o cliente cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o cliente:
      | nome   | email   | documento   |
      | <nome> | <email> | <documento> |

    Exemplos:
      | campo     | valor            | nome        | email             | documento      |
      | nome      | Novo nome        | Novo nome   | maria@example.com |    52998224725 |
      | email     | novo@example.com | Maria Silva | novo@example.com  |    52998224725 |
      | documento |      11144477735 | Maria Silva | maria@example.com |    11144477735 |
      | documento |   11222333000181 | Maria Silva | maria@example.com | 11222333000181 |

  Esquema do Cenário: Rejeitar atualização com documento já cadastrado
    Dado que existe um cliente com documento "<ocupado>"
    E que existe um cliente com documento "<original>"
    Quando atualizo o cliente cadastrado com:
      | nome      | documento |
      | Novo nome | <ocupado> |
    Então a API deve retornar 409
    E o cliente original deve permanecer inalterado

    Exemplos:
      | ocupado     | original       |
      | 52998224725 |    11144477735 |
      | 52998224725 | 11222333000181 |

  Esquema do Cenário: Rejeitar atualização com dados inválidos
    Dado que existe um cliente
    Quando atualizo o cliente cadastrado com nome "<nome>", email "<email>" e documento "<documento>"
    Então a API deve retornar 400
    E o cliente original deve permanecer inalterado

    Exemplos:
      | nome      | email            | documento      |
      |           | novo@example.com |    11144477735 |
      | Novo nome | email-invalido   |    11144477735 |
      | Novo nome | novo@example.com |    52998224724 |
      | Novo nome | novo@example.com | 11222333000180 |

  Cenário: Rejeitar atualização sem campos definidos
    Dado que existe um cliente
    Quando atualizo o cliente cadastrado sem campos definidos
    Então a API deve retornar 400
    E o cliente original deve permanecer inalterado

  Cenário: Rejeitar atualização em cliente inexistente
    Quando atualizo o cliente pelo ID "00000000-0000-0000-0000-000000000001" com:
      | nome      |
      | Novo nome |
    Então a API deve retornar 404

  Esquema do Cenário: Reutilizar documento de cliente atualizado em um novo cadastro
    Dado que existe um cliente com documento "<original>"
    Quando atualizo o cliente cadastrado com:
      | documento |
      | <novo>    |
    Então a API deve retornar 200
    Quando cadastro um cliente com nome "Novo cliente", email "novo@example.com" e documento "<original>"
    Então a API deve retornar 201
    E o novo cadastro deve ter outro ID

    Exemplos:
      | original       | novo           |
      |    52998224725 |    11144477735 |
      | 11222333000181 | 04252011000110 |
  # Exclusão

  Esquema do Cenário: Exluir cadastro do cliente
    Dado que existe um cliente com documento "<documento>"
    Quando excluo o cliente cadastrado
    Então a API deve retornar 204
    Quando consulto o cliente cadastrado pelo ID
    Então a API deve retornar 404
    Quando consulto clientes pelo documento "<documento>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista vazia de clientes

    Exemplos:
      | documento      |
      |    52998224725 |
      | 11222333000181 |

  Esquema do Cenário: Reutilizar documento de cliente excluído em um novo cadastro
    Dado que existe um cliente com documento "<documento>"
    Quando excluo o cliente cadastrado
    Então a API deve retornar 204
    Quando cadastro um cliente com nome "Novo cliente", email "novo@example.com" e documento "<documento>"
    Então a API deve retornar 201
    E o novo cadastro deve ter outro ID

    Exemplos:
      | documento      |
      |    52998224725 |
      | 11222333000181 |

  Cenário: Rejeitar exclusão de cliente inexistente
    Quando excluo o cliente pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404

  Regra: O cliente só pode ser excluído quando todas as suas OS estiverem entregues

    Esquema do Cenário: Rejeitar exclusão de cliente com OS ativa
      Dado que o cliente possui uma OS em "<status>"
      Quando excluo o cliente cadastrado
      Então a API deve retornar 409
      E o cliente original deve permanecer inalterado
      E a OS do cliente deve permanecer em "<status>"

      Exemplos:
        | status                 |
        | aguardando-diagnostico |
        | em-diagnostico         |
        | aguardando-confirmacao |
        | aguardando-insumos     |
        | aguardando-execucao    |
        | em-execucao            |
        | aguardando-retirada    |

    Cenário: Rejeitar exclusão de cliente enquanto aguarda retirada após rejeitar o orçamento
      Dado que o cliente possui uma OS com orçamento rejeitado aguardando retirada
      Quando excluo o cliente cadastrado
      Então a API deve retornar 409
      E o cliente original deve permanecer inalterado
      E a OS do cliente deve permanecer em "aguardando-retirada"

    Cenário: Excluir cliente com OS entregue preservando a OS
      Dado que o cliente possui uma OS em "entregue"
      Quando excluo o cliente cadastrado
      Então a API deve retornar 204
      Quando consulto o cliente cadastrado pelo ID
      Então a API deve retornar 404
      E a OS do cliente deve permanecer em "entregue"

    Cenário: Excluir cliente sem OS mesmo existindo OS ativa de outro cliente
      Dado que o cliente possui uma OS em "em-diagnostico"
      E que existe um cliente com documento "11144477735"
      Quando excluo o cliente cadastrado
      Então a API deve retornar 204
      E a OS do cliente deve permanecer em "em-diagnostico"

    Cenário: Rejeitar exclusão quando o cliente possui OS entregue e outra ativa
      Dado que o cliente possui uma OS em "entregue"
      E que o cliente possui outra OS aguardando diagnóstico
      Quando excluo o cliente cadastrado
      Então a API deve retornar 409
      E o cliente original deve permanecer inalterado
      E a OS do cliente deve permanecer em "aguardando-diagnostico"

    Cenário: Manter proteção do cliente original após transferir o veículo
      Dado que o cliente possui uma OS em "em-diagnostico"
      Quando transfiro o veículo da OS para outro cliente
      Então a API deve retornar 200
      Quando excluo o cliente cadastrado
      Então a API deve retornar 409
      E o cliente original deve permanecer inalterado
      E a OS do cliente deve permanecer em "em-diagnostico"
