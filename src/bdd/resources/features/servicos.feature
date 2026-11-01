# language: pt
@servicos
Funcionalidade: Gerenciar serviços
  O administrador deve gerenciar serviços com nome único, preço de venda e indicação de diagnóstico.

  Cenário: Cadastrar serviço
    Quando cadastro um serviço com nome "Óleo de motor" e preço "19.90"
    Então a API deve retornar 201
    E a resposta deve conter o serviço:
      | nome          | precoVenda |
      | Óleo de motor | 19.90      |

  Cenário: Consultar serviço pelo ID
    Dado que existe um serviço com nome "Óleo de motor" e preço "19.90"
    Quando consulto o serviço cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o serviço:
      | nome          | precoVenda |
      | Óleo de motor | 19.90      |

  Cenário: Rejeitar nome já cadastrado
    Dado que existe um serviço com nome "Óleo" e preço "19.90"
    Quando cadastro um serviço com nome "Óleo" e preço "25.00"
    Então a API deve retornar 409
    E o serviço original deve permanecer inalterado

  Cenário: Atualizar nome e preço de venda
    Dado que existe um serviço com nome "Óleo" e preço "19.90"
    Quando atualizo o serviço cadastrado com:
      | nome           | precoVenda |
      | Filtro de óleo | 29.99      |
    Então a API deve retornar 200
    Quando consulto o serviço cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o serviço:
      | nome           | precoVenda |
      | Filtro de óleo | 29.99      |

  Cenário: Impedir atualização para nome de outro serviço
    Dado que existe um serviço com nome "Óleo" e preço "19.90"
    E que existe um serviço com nome "Filtro" e preço "29.99"
    Quando atualizo o serviço cadastrado com:
      | nome | precoVenda |
      | Óleo | 100.01     |
    Então a API deve retornar 409
    E o serviço original deve permanecer inalterado

  Cenário: Excluir serviço e reutilizar seu nome em novo cadastro
    Dado que existe um serviço com nome "Filtro de óleo" e preço "19.90"
    Quando excluo o serviço cadastrado
    Então a API deve retornar 204
    E a resposta não deve conter conteúdo nem permitir cache
    Quando consulto o serviço cadastrado pelo ID
    Então a API deve retornar 404
    Quando listo os serviços
    Então a API deve retornar 200
    E a lista de serviços deve estar vazia
    Quando consulto serviços pelo nome "filtro"
    Então a API deve retornar 200
    E a lista de serviços deve estar vazia
    Quando cadastro um serviço com nome "Filtro de óleo" e preço "29.99"
    Então a API deve retornar 201
    E o novo serviço deve ter outro ID
    Quando consulto o serviço cadastrado pelo ID
    Então a API deve retornar 404

  Cenário: Buscar palavras em português independentemente de maiúsculas e flexão
    Dado que existe um serviço com nome "Filtro de óleo" e preço "19.90"
    E que existe um serviço com nome "Óleos sintéticos" e preço "29.99"
    E que existe um serviço com nome "Pneu" e preço "100.00"
    Quando consulto serviços pelo nome "ÓLEO"
    Então a API deve retornar 200
    E a lista de serviços deve conter os nomes:
      | Filtro de óleo   |
      | Óleos sintéticos |
    Quando consulto serviços pelo nome "óleo filtro"
    Então a API deve retornar 200
    E a lista de serviços deve conter os nomes:
      | Filtro de óleo |

  Cenário: Buscar por palavras completas sem correspondência por prefixo
    Dado que existe um serviço com nome "Óleos sintéticos" e preço "29.99"
    Quando consulto serviços pelo nome "sint"
    Então a API deve retornar 200
    E a lista de serviços deve estar vazia

  Cenário: Alterar a exigência de diagnóstico preservando os outros dados
    Dado que existe um serviço com nome "Troca de óleo" e preço "100.00"
    Quando atualizo o serviço cadastrado com:
      | requerDiagnostico |
      | false             |
    Então a API deve retornar 200
    Quando consulto o serviço cadastrado pelo ID
    Então a API deve retornar 200
    E a resposta deve conter o serviço:
      | nome          | precoVenda | requerDiagnostico |
      | Troca de óleo | 100.00     | false             |

  Cenário: Gerenciar os insumos obrigatórios de um serviço
    Dado que existe um serviço com nome "Troca de óleo" e preço "100.00"
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve estar sem insumos obrigatórios
    Dado que existe um insumo disponível para o serviço com nome "Óleo" e preço "19.90"
    Quando associo o insumo disponível como obrigatório ao serviço cadastrado
    Então a API deve retornar 201
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve ter o insumo disponível como obrigatório
    Quando removo o insumo obrigatório do serviço cadastrado
    Então a API deve retornar 204
    E a resposta não deve conter conteúdo nem permitir cache
    Quando listo os insumos obrigatórios do serviço cadastrado
    Então a API deve retornar 200
    E o serviço deve estar sem insumos obrigatórios
    E o insumo disponível deve continuar cadastrado
