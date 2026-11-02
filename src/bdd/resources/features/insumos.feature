# language: pt
@insumos
Funcionalidade: Gerenciar insumos
  O administrador deve gerenciar insumos com nome único, preço de venda e estoque.
  # Cadastro

  Esquema do Cenário: Cadastrar insumo
    Quando cadastro um insumo com nome "<nome>" e preço "<preco>"
    Então a API deve retornar 201
    E a resposta deve conter o insumo:
      | nome   | precoVenda |
      | <nome> | <preco>    |
    E o estoque do insumo deve ter 0 unidades físicas, 0 reservadas e 0 disponíveis

    Exemplos:
      | nome          | preco    |
      | Óleo de motor |    19.90 |
      | Brinde        |     0.00 |
      | Motor         | 10000.00 |

  Cenário: Rejeitar cadastro com nome já cadastrado
    Dado que existe um insumo com nome "Óleo"
    Quando cadastro um insumo com nome "Óleo"
    Então a API deve retornar 409
    E o insumo original deve permanecer inalterado

  Esquema do Cenário: Rejeitar cadastro com dados inválidos
    Quando cadastro um insumo com nome "<nome>" e preço "<preco>"
    Então a API deve retornar 400

    Exemplos:
      | nome | preco    |
      |      |    19.90 |
      | Óleo |    -0.01 |
      | Óleo | 10000.01 |
      | Óleo |   19.901 |
  # Consulta

  Cenário: Consultar insumo existente pelo ID
    Dado que existe um insumo
    E que existe outro insumo
    Quando consulto o insumo cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o ID do insumo cadastrado

  Cenário: Consultar insumo inexistente pelo ID
    Quando consulto o insumo pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404

  Cenário: Consultar insumos
    Dado que existem 2 insumos
    Quando listo os insumos com limite 1
    Então a API deve retornar 200
    E a página deve conter o insumo 1 em ordem de ID
    E a resposta deve indicar a próxima página de insumos
    Quando consulto a próxima página de insumos
    Então a API deve retornar 200
    E a página deve conter o insumo 2 em ordem de ID
    E a resposta deve indicar o fim da lista de insumos

  Cenário: Consultar insumos sem isumos cadastrados
    Quando listo os insumos
    Então a API deve retornar 200
    E a lista de insumos deve estar vazia

  Esquema do Cenário: Consultar insumos por nome
    Dado que existe um insumo com nome "Óleos sintéticos"
    E que existe outro insumo
    Quando consulto insumos pelo nome "<nome>"
    Então a API deve retornar 200
    E a lista de insumos deve conter os nomes:
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
    Quando listo os insumos com paginação:
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

  Cenário: Atualizar completamente dados do insumo
    Dado que existe um insumo com nome "Óleo"
    Quando atualizo o insumo cadastrado com:
      | nome   | precoVenda |
      | Filtro |      29.99 |
    Então a API deve retornar 200
    Quando consulto insumos pelo nome "filtro"
    Então a API deve retornar 200
    E a resposta deve conter uma lista com o insumo:
      | nome   | precoVenda |
      | Filtro |      29.99 |
    Quando consulto insumos pelo nome "óleo"
    Então a API deve retornar 200
    E a lista de insumos deve estar vazia

  Esquema do Cenário: Atualizar parcialmente dados do insumo
    Dado que existe um insumo com nome "Óleo" e preço "19.90"
    Quando atualizo o insumo cadastrado com:
      | <campo> |
      | <valor> |
    Então a API deve retornar 200
    E a resposta deve conter o ID do insumo cadastrado
    Quando consulto o insumo cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o insumo:
      | nome   | precoVenda |
      | <nome> | <preco>    |

    Exemplos:
      | campo      | valor    | nome   | preco    |
      | nome       | Filtro   | Filtro |    19.90 |
      | nome       | Óleo     | Óleo   |    19.90 |
      | precoVenda |    29.99 | Óleo   |    29.99 |
      | precoVenda |     0.00 | Óleo   |     0.00 |
      | precoVenda | 10000.00 | Óleo   | 10000.00 |

  Cenário: Rejeitar atualização com nome já cadastrado
    Dado que existe um insumo com nome "Óleo"
    E que existe um insumo com nome "Filtro"
    Quando atualizo o insumo cadastrado com:
      | nome |
      | Óleo |
    Então a API deve retornar 409
    E o insumo original deve permanecer inalterado

  Esquema do Cenário: Rejeitar atualização com dados inválidos
    Dado que existe um insumo
    Quando atualizo o insumo cadastrado com nome "<nome>" e preço "<preco>"
    Então a API deve retornar 400
    E o insumo original deve permanecer inalterado

    Exemplos:
      | nome   | preco    |
      |        |    29.99 |
      | Filtro |    -0.01 |
      | Filtro | 10000.01 |
      | Filtro |   29.999 |

  Cenário: Rejeitar atualização sem campos definidos
    Dado que existe um insumo
    Quando atualizo o insumo cadastrado sem campos definidos
    Então a API deve retornar 400
    E o insumo original deve permanecer inalterado

  Cenário: Rejeitar atualização em insumo inexistente
    Quando atualizo o insumo pelo ID "00000000-0000-0000-0000-000000000001" com:
      | nome   |
      | Filtro |
    Então a API deve retornar 404

  Cenário: Reutilizar nome de insumo atualizado em um novo cadastro
    Dado que existe um insumo com nome "Óleo"
    Quando atualizo o insumo cadastrado com:
      | nome   |
      | Filtro |
    Então a API deve retornar 200
    Quando cadastro um insumo com nome "Óleo"
    Então a API deve retornar 201
    E o novo insumo deve ter outro ID
  # Estoque

  Cenário: Adicionar insumos ao estoque
    Dado que existe um insumo
    Quando adiciono 5 unidades ao estoque do insumo cadastrado
    Então a API deve retornar 200
    E o estoque do insumo deve ter 5 unidades físicas, 0 reservadas e 5 disponíveis

  Esquema do Cenário: Rejeitar estoque com dados inválidos
    Dado que existe um insumo
    Quando adiciono <quantidade> unidades ao estoque do insumo cadastrado
    Então a API deve retornar 400
    E o insumo original deve permanecer inalterado

    Exemplos:
      | quantidade |
      |          0 |
      |         -1 |

  Cenário: Rejeitar entrada em insumo inexistente
    Quando adiciono 1 unidades ao estoque do insumo pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404
  # Exclusão

  Cenário: Excluir cadastro do insumo
    Dado que existe um insumo com nome "Filtro de óleo"
    Quando excluo o insumo cadastrado
    Então a API deve retornar 204
    E a resposta não deve conter conteúdo nem permitir cache
    Quando consulto o insumo cadastrado pelo ID
    Então a API deve retornar 404
    Quando listo os insumos
    Então a API deve retornar 200
    E a lista de insumos deve estar vazia
    Quando consulto insumos pelo nome "filtro"
    Então a API deve retornar 200
    E a lista de insumos deve estar vazia

  Cenário: Reutilizar nome de insumo excluído em novo cadastro
    Dado que existe um insumo com nome "Filtro de óleo"
    Quando excluo o insumo cadastrado
    Então a API deve retornar 204
    Quando cadastro um insumo com nome "Filtro de óleo"
    Então a API deve retornar 201
    E o novo insumo deve ter outro ID

  Cenário: Rejeitar exclusão de insumo inexistente
    Quando excluo o insumo pelo ID "00000000-0000-0000-0000-000000000001"
    Então a API deve retornar 404

  Cenário: Rejeitar exclusão de insumo obrigatório para serviço
    Dado que existe um insumo
    E que o insumo é obrigatório para um serviço ativo
    Quando excluo o insumo cadastrado
    Então a API deve retornar 409
    E o insumo original deve permanecer inalterado

  Esquema do Cenário: Rejeitar exclusão de insumo necessário para ordem de serviço em andamento
    Dado que existe um insumo
    E que o insumo é necessário para uma OS em "<status>"
    Quando excluo o insumo cadastrado
    Então a API deve retornar 409
    E o insumo original deve permanecer inalterado

    Exemplos:
      | status                 |
      | aguardando-diagnostico |
      | em-diagnostico         |
      | aguardando-confirmacao |
      | aguardando-insumos     |
      | aguardando-execucao    |
      | em-execucao            |
