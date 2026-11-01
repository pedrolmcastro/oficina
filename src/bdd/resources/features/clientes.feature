# language: pt
@clientes
Funcionalidade: Gerenciar clientes
  O administrador deve gerenciar clientes identificados por CPF ou CNPJ.

  Esquema do Cenário: Cadastrar clientes com CPF ou CNPJ
    Quando cadastro um cliente com nome "<nome>", email "<email>" e documento "<documento>"
    Então a API deve retornar 201
    E a resposta deve conter o cliente:
      | nome   | email   | documento   | tipo   |
      | <nome> | <email> | <documento> | <tipo> |

    Exemplos:
      | nome        | email               | documento      | tipo |
      | Maria Silva | maria@example.com   | 52998224725    | CPF  |
      | Oficina     | oficina@example.com | 11222333000181 | CNPJ |

  Esquema do Cenário: Rejeitar documento já cadastrado
    Dado que existe um cliente com documento "<documento>"
    Quando cadastro um cliente com nome "Outro cliente", email "outro@example.com" e documento "<documento>"
    Então a API deve retornar 409
    E o cliente original deve permanecer inalterado

    Exemplos:
      | documento      |
      | 52998224725    |
      | 11222333000181 |

  Cenário: Consultar cliente existente pelo ID
    Dado que existe um cliente com documento "52998224725"
    Quando consulto o cliente cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o cliente:
      | nome        | email             | documento   | tipo |
      | Maria Silva | maria@example.com | 52998224725 | CPF  |

  Esquema do Cenário: Consultar cliente existente pelo documento
    Dado que existe um cliente com documento "<documento>"
    Quando consulto clientes pelo documento "<documento>"
    Então a API deve retornar 200
    E a resposta deve conter o cliente:
      | nome        | email             | documento   | tipo   |
      | Maria Silva | maria@example.com | <documento> | <tipo> |

    Exemplos:
      | documento      | tipo |
      | 52998224725    | CPF  |
      | 11222333000181 | CNPJ |

  Cenário: Informar documento não encontrado
    Quando consulto clientes pelo documento "52998224725"
    Então a API deve retornar 404

  Esquema do Cenário: Atualizar dados e documento do cliente
    Dado que existe um cliente com documento "<original>"
    Quando atualizo o cliente cadastrado com:
      | nome      | email            | documento |
      | Novo nome | novo@example.com | <novo>    |
    Então a API deve retornar 200
    Quando consulto clientes pelo documento "<novo>"
    Então a API deve retornar 200
    E a resposta deve conter o cliente:
      | nome      | email            | documento | tipo   |
      | Novo nome | novo@example.com | <novo>    | <tipo> |
    Quando consulto clientes pelo documento "<original>"
    Então a API deve retornar 404

    Exemplos:
      | original       | novo           | tipo |
      | 52998224725    | 11144477735    | CPF  |
      | 52998224725    | 11222333000181 | CNPJ |

  Esquema do Cenário: Impedir atualização para documento de outro cliente
    Dado que existe um cliente com documento "<ocupado>"
    E que existe um cliente com documento "<original>"
    Quando atualizo o cliente cadastrado com:
      | nome      | documento |
      | Novo nome | <ocupado> |
    Então a API deve retornar 409
    E o cliente original deve permanecer inalterado

    Exemplos:
      | ocupado        | original       |
      | 52998224725    | 11144477735    |
      | 52998224725    | 11222333000181 |

  Esquema do Cenário: Reutilizar documento de cliente excluído em um novo cadastro
    Dado que existe um cliente com documento "<documento>"
    Quando excluo o cliente cadastrado
    Então a API deve retornar 204
    Quando cadastro um cliente com nome "Novo cliente", email "novo@example.com" e documento "<documento>"
    Então a API deve retornar 201
    E o novo cadastro deve ter outro ID
    Quando consulto o cliente cadastrado pelo ID
    Então a API deve retornar 404

    Exemplos:
      | documento      |
      | 52998224725    |
      | 11222333000181 |
