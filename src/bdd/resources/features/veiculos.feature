# language: pt
@veiculos
Funcionalidade: Gerenciar veículos
  O administrador deve gerenciar veículos de clientes identificados por placa brasileira.
  # Cadastro

  Esquema do Cenário: Cadastrar veículo
    Quando cadastro um veículo com placa "<placa>", modelo "<modelo>", marca "<marca>" e ano <ano>
    Então a API deve retornar 201
    E a resposta deve conter o veículo:
      | placa   | modelo   | marca   | ano   |
      | <placa> | <modelo> | <marca> | <ano> |

    Exemplos:
      | placa   | modelo  | marca  | ano  |
      | ABC1234 | Corolla | Toyota | 2024 |
      | ABC1D23 | Civic   | Honda  | 2025 |

  Esquema do Cenário: Rejeitar cadastro com placa já cadastrada
    Dado que existe um veículo com placa "<placa>"
    Quando cadastro um veículo com placa "<placa>", modelo "Civic", marca "Honda" e ano 2025
    Então a API deve retornar 409
    E o veículo original deve permanecer inalterado

    Exemplos:
      | placa   |
      | ABC1234 |
      | ABC1D23 |

  Esquema do Cenário: Rejeitar cadastro com dados inválidos
    Quando cadastro um veículo com placa "<placa>", modelo "<modelo>", marca "<marca>" e ano <ano>
    Então a API deve retornar 400

    Exemplos:
      | placa    | modelo  | marca  | ano  |
      |          | Corolla | Toyota | 2024 |
      | ABC-1234 | Corolla | Toyota | 2024 |
      | abc1d23  | Corolla | Toyota | 2024 |
      | ABC1234  |         | Toyota | 2024 |
      | ABC1234  | Corolla |        | 2024 |
      | ABC1234  | Corolla | Toyota | 1885 |
      | ABC1234  | Corolla | Toyota | 9999 |

  Cenário: Rejeitar cadastro com cliente inexistente
    Quando cadastro um veículo com placa "ABC1234" para o cliente "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404
  # Consulta

  Cenário: Consultar veículo existente pelo ID
    Dado que existe um veículo
    E que existe outro veículo
    Quando consulto o veículo cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o ID do veículo cadastrado

  Cenário: Consultar veículo inexistente pelo ID
    Quando consulto o veículo pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404

  Esquema do Cenário: Consultar veículo existente pela placa
    Dado que existe um veículo com placa "<placa>"
    E que existe um veículo com placa "DEF1234"
    Quando consulto veículos pela placa "<placa>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista com o veículo:
      | placa   |
      | <placa> |

    Exemplos:
      | placa   |
      | ABC1234 |
      | ABC1D23 |

  Esquema do Cenário: Consultar veículo inexistente pela placa
    Quando consulto veículos pela placa "<placa>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista vazia de veículos

    Exemplos:
      | placa   |
      | ABC1234 |
      | ABC1D23 |

  Cenário: Rejeitar consulta sem filtro
    Quando consulto veículos sem cliente e sem placa
    Então a API deve retornar 400

  Esquema do Cenário: Rejeitar consulta com placa inválida
    Quando consulto veículos pela placa "<placa>"
    Então a API deve retornar 400

    Exemplos:
      | placa    |
      |          |
      |      123 |
      | ABC-1234 |
      | abc1d23  |

  Cenário: Consultar veículos por cliente
    Dado que o cliente possui 2 veículos
    E que outro cliente possui 1 veículo
    Quando consulto veículos pelo ID do cliente cadastrado com limite 1
    Então a API deve retornar 200
    E a página deve conter o veículo 1 do cliente em ordem de ID
    E a resposta deve indicar a próxima página de veículos
    Quando consulto a próxima página de veículos do cliente cadastrado
    Então a API deve retornar 200
    E a página deve conter o veículo 2 do cliente em ordem de ID
    E a resposta deve indicar o fim da lista de veículos

  Cenário: Consultar veículos por cliente sem veículos
    Quando consulto veículos pelo ID do cliente cadastrado
    Então a API deve retornar 200
    E a resposta deve conter uma lista vazia de veículos

  Cenário: Rejeitar consulta com cliente inexistente
    Quando consulto veículos pelo ID do cliente "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404

  Esquema do Cenário: Rejeitar consulta com paginação inválida
    Quando consulto veículos pelo ID do cliente cadastrado com paginação:
      | <campo> |
      | <valor> |
    Então a API deve retornar 400

    Exemplos:
      | campo | valor           |
      | after | cursor-invalido |
      | first |              -1 |
      | first |               0 |
      | first |              26 |
  # Atualização

  Esquema do Cenário: Atualizar completamente dados do veículo
    Dado que existe um veículo com placa "<original>"
    Quando atualizo o veículo cadastrado com:
      | modelo | marca | ano  | placa  |
      | Civic  | Honda | 2025 | <novo> |
    Então a API deve retornar 200
    Quando consulto veículos pela placa "<novo>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista com o veículo:
      | modelo | marca | ano  | placa  |
      | Civic  | Honda | 2025 | <novo> |
    Quando consulto veículos pela placa "<original>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista vazia de veículos

    Exemplos:
      | original | novo    |
      | ABC1234  | DEF1234 |
      | ABC1234  | ABC1D23 |
      | ABC1D23  | DEF1234 |

  Esquema do Cenário: Atualizar parcialmente dados do veículo
    Dado que existe um veículo com os dados:
      | placa   | modelo  | marca  | ano  |
      | ABC1234 | Corolla | Toyota | 2024 |
    Quando atualizo o veículo cadastrado com:
      | <campo> |
      | <valor> |
    Então a API deve retornar 200
    E a resposta deve conter o ID do veículo cadastrado
    E a resposta deve conter o veículo:
      | placa   | modelo   | marca   | ano   |
      | <placa> | <modelo> | <marca> | <ano> |
    Quando consulto o veículo cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o veículo:
      | placa   | modelo   | marca   | ano   |
      | <placa> | <modelo> | <marca> | <ano> |

    Exemplos:
      | campo  | valor   | placa   | modelo  | marca  | ano  |
      | modelo | Civic   | ABC1234 | Civic   | Toyota | 2024 |
      | marca  | Honda   | ABC1234 | Corolla | Honda  | 2024 |
      | ano    |    2025 | ABC1234 | Corolla | Toyota | 2025 |
      | placa  | DEF1234 | DEF1234 | Corolla | Toyota | 2024 |
      | placa  | ABC1D23 | ABC1D23 | Corolla | Toyota | 2024 |

  Cenário: Atualizar cliente associado ao veículo
    Dado que existe um veículo
    E que existe outro cliente para associar ao veículo
    Quando atualizo o veículo cadastrado para o outro cliente
    Então a API deve retornar 200
    E a resposta deve conter o veículo associado ao outro cliente com os demais dados preservados
    Quando consulto o veículo cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o veículo associado ao outro cliente com os demais dados preservados
    Quando consulto veículos pelo ID do cliente cadastrado
    Então a API deve retornar 200
    E a resposta deve conter uma lista vazia de veículos
    Quando consulto veículos pelo ID do outro cliente
    Então a API deve retornar 200
    E a lista deve conter somente o veículo associado ao outro cliente

  Cenário: Rejeitar atualização com cliente inexistente
    Dado que existe um veículo
    Quando atualizo o veículo cadastrado com:
      | modelo | clienteId                            |
      | Civic  | 00000000-0000-0000-0000-000000000001 |
    Então a API deve retornar 404
    E o veículo original deve permanecer inalterado

  Esquema do Cenário: Rejeitar atualização com placa já cadastrada
    Dado que existe um veículo com placa "<ocupado>"
    E que existe um veículo com placa "<original>"
    Quando atualizo o veículo cadastrado com:
      | modelo | placa     |
      | Civic  | <ocupado> |
    Então a API deve retornar 409
    E o veículo original deve permanecer inalterado

    Exemplos:
      | ocupado | original |
      | ABC1234 | DEF1234  |
      | ABC1D23 | ABC1234  |

  Esquema do Cenário: Rejeitar atualização com dados inválidos
    Dado que existe um veículo
    Quando atualizo o veículo cadastrado com placa "<placa>", modelo "<modelo>", marca "<marca>" e ano <ano>
    Então a API deve retornar 400
    E o veículo original deve permanecer inalterado

    Exemplos:
      | placa    | modelo | marca | ano  |
      |          | Civic  | Honda | 2025 |
      | ABC-1234 | Civic  | Honda | 2025 |
      | abc1d23  | Civic  | Honda | 2025 |
      | DEF1234  |        | Honda | 2025 |
      | DEF1234  | Civic  |       | 2025 |
      | DEF1234  | Civic  | Honda | 1885 |
      | DEF1234  | Civic  | Honda | 9999 |

  Cenário: Rejeitar atualização sem campos definidos
    Dado que existe um veículo
    Quando atualizo o veículo cadastrado sem campos definidos
    Então a API deve retornar 400
    E o veículo original deve permanecer inalterado

  Cenário: Rejeitar atualização em veículo inexistente
    Quando atualizo o veículo pelo ID "00000000-0000-0000-0000-000000000001" com:
      | modelo |
      | Civic  |
    Então a API deve retornar 404

  Esquema do Cenário: Reutilizar placa de veículo atualizado em um novo cadastro
    Dado que existe um veículo com placa "<original>"
    Quando atualizo o veículo cadastrado com:
      | placa  |
      | <novo> |
    Então a API deve retornar 200
    Quando cadastro um veículo com placa "<original>"
    Então a API deve retornar 201
    E o novo veículo deve ter outro ID

    Exemplos:
      | original | novo    |
      | ABC1234  | DEF1234 |
      | ABC1D23  | DEF1G23 |
  # Exclusão

  Esquema do Cenário: Excluir cadastro do veículo
    Dado que existe um veículo com placa "<placa>"
    Quando excluo o veículo cadastrado
    Então a API deve retornar 204
    Quando consulto o veículo cadastrado pelo ID
    Então a API deve retornar 404
    Quando consulto veículos pela placa "<placa>"
    Então a API deve retornar 200
    E a resposta deve conter uma lista vazia de veículos
    Quando consulto veículos pelo ID do cliente cadastrado
    Então a API deve retornar 200
    E a resposta deve conter uma lista vazia de veículos

    Exemplos:
      | placa   |
      | ABC1234 |
      | ABC1D23 |

  Esquema do Cenário: Reutilizar placa de veículo excluído em um novo cadastro
    Dado que existe um veículo com placa "<placa>"
    Quando excluo o veículo cadastrado
    Então a API deve retornar 204
    Quando cadastro um veículo com placa "<placa>"
    Então a API deve retornar 201
    E o novo veículo deve ter outro ID

    Exemplos:
      | placa   |
      | ABC1234 |
      | ABC1D23 |

  Cenário: Rejeitar exclusão de veículo inexistente
    Quando excluo o veículo pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404
