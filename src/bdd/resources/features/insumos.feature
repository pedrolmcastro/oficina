# language: pt
@insumos
Funcionalidade: Gerenciar insumos
  O administrador deve gerenciar insumos com nome único e preço de venda.

  Cenário: Cadastrar insumo
    Quando cadastro um insumo com nome "Óleo de motor" e preço "19.90"
    Então a API deve retornar 201
    E a resposta deve conter o insumo:
      | nome          | precoVenda |
      | Óleo de motor | 19.90      |

  Cenário: Consultar insumo pelo ID
    Dado que existe um insumo com nome "Óleo de motor" e preço "19.90"
    Quando consulto o insumo cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o insumo:
      | nome          | precoVenda |
      | Óleo de motor | 19.90      |

  Cenário: Rejeitar nome já cadastrado
    Dado que existe um insumo com nome "Óleo" e preço "19.90"
    Quando cadastro um insumo com nome "Óleo" e preço "25.00"
    Então a API deve retornar 409
    E o insumo original deve permanecer inalterado

  Cenário: Atualizar nome e preço de venda
    Dado que existe um insumo com nome "Óleo" e preço "19.90"
    Quando atualizo o insumo cadastrado com:
      | nome           | precoVenda |
      | Filtro de óleo | 29.99      |
    Então a API deve retornar 200
    Quando consulto o insumo cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o insumo:
      | nome           | precoVenda |
      | Filtro de óleo | 29.99      |

  Cenário: Impedir atualização para nome de outro insumo
    Dado que existe um insumo com nome "Óleo" e preço "19.90"
    E que existe um insumo com nome "Filtro" e preço "29.99"
    Quando atualizo o insumo cadastrado com:
      | nome | precoVenda |
      | Óleo | 100.01     |
    Então a API deve retornar 409
    E o insumo original deve permanecer inalterado

  Cenário: Excluir insumo e reutilizar seu nome em novo cadastro
    Dado que existe um insumo com nome "Filtro de óleo" e preço "19.90"
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
    Quando cadastro um insumo com nome "Filtro de óleo" e preço "29.99"
    Então a API deve retornar 201
    E o novo insumo deve ter outro ID
    Quando consulto o insumo cadastrado pelo ID
    Então a API deve retornar 404

  Cenário: Buscar palavras em português independentemente de maiúsculas e flexão
    Dado que existe um insumo com nome "Filtro de óleo" e preço "19.90"
    E que existe um insumo com nome "Óleos sintéticos" e preço "29.99"
    E que existe um insumo com nome "Pneu" e preço "100.00"
    Quando consulto insumos pelo nome "ÓLEO"
    Então a API deve retornar 200
    E a lista de insumos deve conter os nomes:
      | Filtro de óleo   |
      | Óleos sintéticos |
    Quando consulto insumos pelo nome "óleo filtro"
    Então a API deve retornar 200
    E a lista de insumos deve conter os nomes:
      | Filtro de óleo |

  Cenário: Buscar por palavras completas sem correspondência por prefixo
    Dado que existe um insumo com nome "Óleos sintéticos" e preço "29.99"
    Quando consulto insumos pelo nome "sint"
    Então a API deve retornar 200
    E a lista de insumos deve estar vazia
