# language: pt
@servicos
Funcionalidade: Gerenciar serviços
  O administrador deve gerenciar serviços com nome único, preço de venda e indicação de diagnóstico.
  # Cadastro

  Esquema do Cenário: Cadastrar serviço
    Quando cadastro um serviço com nome "<nome>", preço "<preco>" e requer diagnóstico "<diagnostico>"
    Então a API deve retornar 201
    E a resposta deve conter o serviço:
      | nome   | precoVenda | requerDiagnostico |
      | <nome> | <preco>    | <diagnostico>     |

    Exemplos:
      | nome          | preco    | diagnostico |
      | Troca de óleo |    19.90 | true        |
      | Inspeção      |     0.00 | false       |
      | Revisão       | 10000.00 | true        |

  Cenário: Rejeitar cadastro com nome já cadastrado
    Dado que existe um serviço com nome "Óleo"
    Quando cadastro um serviço com nome "Óleo"
    Então a API deve retornar 409
    E o serviço original deve permanecer inalterado

  Esquema do Cenário: Rejeitar cadastro com dados inválidos
    Quando cadastro um serviço com nome "<nome>" e preço "<preco>"
    Então a API deve retornar 400

    Exemplos:
      | nome | preco    |
      |      |    19.90 |
      | Óleo |    -0.01 |
      | Óleo | 10000.01 |
      | Óleo |   19.901 |
  # Consulta

  Cenário: Consultar serviço existente pelo ID
    Dado que existe um serviço
    E que existe outro serviço
    Quando consulto o serviço cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o ID do serviço cadastrado

  Cenário: Consultar serviço inexistente pelo ID
    Quando consulto o serviço pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404

  Cenário: Consultar serviços
    Dado que existem 2 serviços
    Quando listo os serviços com limite 1
    Então a API deve retornar 200
    E a página deve conter o serviço 1 em ordem de ID
    E a resposta deve indicar a próxima página de serviços
    Quando consulto a próxima página de serviços
    Então a API deve retornar 200
    E a página deve conter o serviço 2 em ordem de ID
    E a resposta deve indicar o fim da lista de serviços

  Cenário: Consultar serviços sem serviços cadastrados
    Quando listo os serviços
    Então a API deve retornar 200
    E a lista de serviços deve estar vazia

  Esquema do Cenário: Consultar serviços por nome
    Dado que existe um serviço com nome "Óleos sintéticos"
    E que existe outro serviço
    Quando consulto serviços pelo nome "<nome>"
    Então a API deve retornar 200
    E a lista de serviços deve conter os nomes:
      | <resultado> |

    Exemplos:
      | nome           | resultado        |
      | ÓLEO           | Óleos sintéticos |
      | óleos          | Óleos sintéticos |
      | óleo sintético | Óleos sintéticos |
      | óleo pneu      |                  |
      | sint           |                  |
      | filtro         |                  |
      |                |                  |

  Esquema do Cenário: Rejeitar consulta com paginação inválida
    Quando listo os serviços com paginação:
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

  Cenário: Atualizar completamente dados do serviço
    Dado que existe um serviço com nome "Óleo"
    Quando atualizo o serviço cadastrado com:
      | nome   | precoVenda | requerDiagnostico |
      | Filtro |      29.99 | false             |
    Então a API deve retornar 200
    Quando consulto serviços pelo nome "filtro"
    Então a API deve retornar 200
    E a resposta deve conter uma lista com o serviço:
      | nome   | precoVenda | requerDiagnostico |
      | Filtro |      29.99 | false             |
    Quando consulto serviços pelo nome "óleo"
    Então a API deve retornar 200
    E a lista de serviços deve estar vazia

  Esquema do Cenário: Atualizar parcialmente dados do serviço
    Dado que existe um serviço com nome "Óleo" e preço "19.90"
    Quando atualizo o serviço cadastrado com:
      | <campo> |
      | <valor> |
    Então a API deve retornar 200
    E a resposta deve conter o ID do serviço cadastrado
    Quando consulto o serviço cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o serviço:
      | nome   | precoVenda | requerDiagnostico |
      | <nome> | <preco>    | <diagnostico>     |

    Exemplos:
      | campo             | valor    | nome   | preco    | diagnostico |
      | nome              | Filtro   | Filtro |    19.90 | true        |
      | nome              | Óleo     | Óleo   |    19.90 | true        |
      | precoVenda        |    29.99 | Óleo   |    29.99 | true        |
      | precoVenda        |     0.00 | Óleo   |     0.00 | true        |
      | precoVenda        | 10000.00 | Óleo   | 10000.00 | true        |
      | requerDiagnostico | false    | Óleo   |    19.90 | false       |

  Cenário: Rejeitar atualização com nome já cadastrado
    Dado que existe um serviço com nome "Óleo"
    E que existe um serviço com nome "Filtro"
    Quando atualizo o serviço cadastrado com:
      | nome |
      | Óleo |
    Então a API deve retornar 409
    E o serviço original deve permanecer inalterado

  Esquema do Cenário: Rejeitar atualização com dados inválidos
    Dado que existe um serviço
    Quando atualizo o serviço cadastrado com nome "<nome>" e preço "<preco>"
    Então a API deve retornar 400
    E o serviço original deve permanecer inalterado

    Exemplos:
      | nome   | preco    |
      |        |    29.99 |
      | Filtro |    -0.01 |
      | Filtro | 10000.01 |
      | Filtro |   29.999 |

  Cenário: Rejeitar atualização sem campos definidos
    Dado que existe um serviço
    Quando atualizo o serviço cadastrado sem campos definidos
    Então a API deve retornar 400
    E o serviço original deve permanecer inalterado

  Cenário: Rejeitar atualização em serviço inexistente
    Quando atualizo o serviço pelo ID "00000000-0000-0000-0000-000000000001" com:
      | nome   |
      | Filtro |
    Então a API deve retornar 404

  Cenário: Reutilizar nome de serviço atualizado em um novo cadastro
    Dado que existe um serviço com nome "Óleo"
    Quando atualizo o serviço cadastrado com:
      | nome   |
      | Filtro |
    Então a API deve retornar 200
    Quando cadastro um serviço com nome "Óleo"
    Então a API deve retornar 201
    E o novo serviço deve ter outro ID
  # Exclusão

  Cenário: Excluir cadastro do serviço
    Dado que existe um serviço com nome "Filtro de óleo"
    Quando excluo o serviço cadastrado
    Então a API deve retornar 204
    Quando consulto o serviço cadastrado pelo ID
    Então a API deve retornar 404
    Quando listo os serviços
    Então a API deve retornar 200
    E a lista de serviços deve estar vazia
    Quando consulto serviços pelo nome "filtro"
    Então a API deve retornar 200
    E a lista de serviços deve estar vazia

  Cenário: Reutilizar nome de serviço excluído em novo cadastro
    Dado que existe um serviço com nome "Filtro de óleo"
    Quando excluo o serviço cadastrado
    Então a API deve retornar 204
    Quando cadastro um serviço com nome "Filtro de óleo"
    Então a API deve retornar 201
    E o novo serviço deve ter outro ID

  Cenário: Rejeitar exclusão de serviço inexistente
    Quando excluo o serviço pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404
  # Insumos obrigatórios

  Cenário: Consultar serviço sem insumos obrigatórios
    Dado que existe um serviço
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve estar sem insumos obrigatórios

  Cenário: Associar insumo obrigatório ao serviço
    Dado que existe um serviço
    E que existe um insumo disponível para o serviço
    Quando associo o insumo disponível como obrigatório ao serviço cadastrado com quantidade "4"
    Então a API deve retornar 201
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve ter o insumo disponível como obrigatório com quantidade "4"

  Cenário: Rejeitar associação de insumo já obrigatório
    Dado que existe um serviço
    E que existe um insumo disponível para o serviço
    E que o insumo disponível é obrigatório para o serviço cadastrado com quantidade "4"
    Quando associo o insumo disponível como obrigatório ao serviço cadastrado com quantidade "2"
    Então a API deve retornar 409
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve ter o insumo disponível como obrigatório com quantidade "4"

  Esquema do Cenário: Rejeitar associação de insumo obrigatório com dados inválidos
    Dado que existe um serviço
    E que existe um insumo disponível para o serviço
    Quando associo o insumo disponível como obrigatório ao serviço cadastrado com quantidade "<quantidade>"
    Então a API deve retornar 400
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve estar sem insumos obrigatórios

    Exemplos:
      | quantidade |
      |          0 |
      |         -1 |

  Cenário: Rejeitar associação de insumo inexistente
    Dado que existe um serviço
    Quando associo um insumo como obrigatório ao serviço cadastrado com:
      | insumoId                             | quantidade |
      | 00000000-0000-0000-0000-000000000001 |          4 |
    Então a API deve retornar 404
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve estar sem insumos obrigatórios

  Cenário: Atualizar associação de insumo obrigatório
    Dado que existe um serviço
    E que existe um insumo disponível para o serviço
    E que o insumo disponível é obrigatório para o serviço cadastrado
    Quando atualizo a associação do insumo obrigatório do serviço cadastrado com:
      | quantidade |
      |          2 |
    Então a API deve retornar 200
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve ter o insumo disponível como obrigatório com quantidade "2"

  Esquema do Cenário: Rejeitar atualização de associação de insumo obrigatório com dados inválidos
    Dado que existe um serviço
    E que existe um insumo disponível para o serviço
    E que o insumo disponível é obrigatório para o serviço cadastrado com quantidade "4"
    Quando atualizo a associação do insumo obrigatório do serviço cadastrado com:
      | quantidade   |
      | <quantidade> |
    Então a API deve retornar 400
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve ter o insumo disponível como obrigatório com quantidade "4"

    Exemplos:
      | quantidade |
      |          0 |
      |         -1 |

  Cenário: Rejeitar atualização de associação de insumo obrigatório inexistente
    Dado que existe um serviço
    Quando atualizo a associação do insumo obrigatório pelo ID "00000000-0000-0000-0000-000000000001" no serviço cadastrado com:
      | quantidade |
      |          2 |
    Então a API deve retornar 404
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve estar sem insumos obrigatórios

  Cenário: Excluir associação de insumo obrigatório
    Dado que existe um serviço
    E que existe um insumo disponível para o serviço
    E que o insumo disponível é obrigatório para o serviço cadastrado
    Quando excluo a associação do insumo obrigatório do serviço cadastrado
    Então a API deve retornar 204
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve estar sem insumos obrigatórios
    E o insumo disponível deve continuar cadastrado
