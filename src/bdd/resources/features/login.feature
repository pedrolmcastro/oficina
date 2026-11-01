# language: pt
Funcionalidade: Login
  O usuario deve obter um token para acessar a API com suas credenciais.

  Cenário: Deve autenticar o administrador com credenciais válidas
    Dado que existe um administrador cadastrado
    Quando envio suas credenciais válidas
    Então devo receber um token válido de administrador
    E devo acessar uma rota protegida com o token

  Esquema do Cenário: Deve rejeitar credenciais incorretas
    Quando envio as credenciais "<usuario>" e "<senha>"
    Então a API deve retornar 401
    E a resposta não deve conter conteúdo nem permitir cache

    Exemplos:
      | usuario     | senha  |
      | admin       | errada |
      | inexistente | admin  |

  Cenário: Não deve permitir acesso sem token
    Quando acesso uma rota protegida sem token
    Então a API deve retornar 401

  Cenário: Não deve autenticar por cookie
    Quando acesso uma rota protegida com token válido apenas no cookie
    Então a API deve retornar 401

  Cenário: Não deve permitir acesso com token inválido
    Quando acesso uma rota protegida com token inválido
    Então a API deve retornar 401

  Cenário: Não deve permitir acesso sem perfil de administrador
    Quando acesso uma rota protegida com token sem perfil administrador
    Então a API deve retornar 403
