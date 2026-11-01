# language: pt
@veiculos
Funcionalidade: Gerenciar veículos
  O administrador deve gerenciar veículos de clientes identificados por placa brasileira.

  Esquema do Cenário: Cadastrar veículo com placa antiga ou Mercosul
    Quando cadastro um veículo com placa "<placa>"
    Então a API deve retornar 201
    E a resposta deve conter o veículo:
      | placa   | modelo  | marca  | ano  |
      | <placa> | Corolla | Toyota | 2024 |

    Exemplos:
      | placa   |
      | ABC1234 |
      | ABC1D23 |

  Esquema do Cenário: Rejeitar placa já cadastrada
    Dado que existe um veículo com placa "<placa>"
    Quando cadastro um veículo com placa "<placa>"
    Então a API deve retornar 409
    E o veículo original deve permanecer inalterado

    Exemplos:
      | placa   |
      | ABC1234 |
      | ABC1D23 |

  Cenário: Consultar veículo pelo ID
    Dado que existe um veículo com placa "ABC1234"
    Quando consulto o veículo cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o veículo:
      | placa   | modelo  | marca  | ano  |
      | ABC1234 | Corolla | Toyota | 2024 |

  Esquema do Cenário: Consultar veículo pela placa
    Dado que existe um veículo com placa "<placa>"
    Quando consulto veículos pela placa "<placa>"
    Então a API deve retornar 200
    E a lista deve conter o veículo:
      | placa   | modelo  | marca  | ano  |
      | <placa> | Corolla | Toyota | 2024 |

    Exemplos:
      | placa   |
      | ABC1234 |
      | ABC1D23 |

  Cenário: Informar placa não encontrada
    Quando consulto veículos pela placa "ABC1234"
    Então a API deve retornar 200
    E a lista de veículos deve estar vazia

  Esquema do Cenário: Atualizar dados e placa do veículo
    Dado que existe um veículo com placa "<original>"
    Quando atualizo o veículo cadastrado com:
      | modelo | marca | ano  | placa  |
      | Civic  | Honda | 2025 | <nova> |
    Então a API deve retornar 200
    Quando consulto veículos pela placa "<nova>"
    Então a API deve retornar 200
    E a lista deve conter o veículo:
      | placa  | modelo | marca | ano  |
      | <nova> | Civic  | Honda | 2025 |
    Quando consulto veículos pela placa "<original>"
    Então a API deve retornar 200
    E a lista de veículos deve estar vazia

    Exemplos:
      | original | nova    |
      | ABC1234  | DEF1G23 |
      | ABC1D23  | DEF1234 |

  Cenário: Impedir atualização para placa de outro veículo
    Dado que existe um veículo com placa "ABC1234"
    E que existe um veículo com placa "DEF1G23"
    Quando atualizo o veículo cadastrado com:
      | modelo | placa   |
      | Civic  | ABC1234 |
    Então a API deve retornar 409
    E o veículo original deve permanecer inalterado

  Esquema do Cenário: Reutilizar placa de veículo excluído em novo cadastro
    Dado que existe um veículo com placa "<placa>"
    Quando excluo o veículo cadastrado
    Então a API deve retornar 204
    Quando cadastro um veículo com placa "<placa>"
    Então a API deve retornar 201
    E o novo veículo deve ter outro ID
    Quando consulto o veículo cadastrado pelo ID
    Então a API deve retornar 404

    Exemplos:
      | placa   |
      | ABC1234 |
      | ABC1D23 |
