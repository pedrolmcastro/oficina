# Documentação da API

Com a aplicação em execução, acesse:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>

A documentação é gerada pelo springdoc a partir dos controladores, das anotações
OpenAPI e das restrições de validação. Ao alterar o contrato de um endpoint,
atualize suas descrições, exemplos e respostas junto com a implementação.

## Autenticação

O `POST /login` é público e recebe JSON com `nomeUsuario` e `senha`, ambos
obrigatórios e não vazios. O botão **Try it out** permite testar com as
credenciais configuradas no ambiente. Não inclua senhas reais nos exemplos.

Uma autenticação válida retorna `200` com `tokenAcesso`, `tipoToken` (`Bearer`)
e `expiraEm` (validade em segundos). O token deve ser enviado no cabeçalho
`Authorization: Bearer <token>` nas requisições que exigem autenticação.
JSON inválido ou campos ausentes/em branco retornam `400`; credenciais inválidas
retornam `401`, sem corpo. As respostas de autenticação `200` e `401` incluem
`Cache-Control: no-store`.

Somente as rotas de documentação são liberadas para leitura sem autenticação,
além do login público. As demais rotas continuam exigindo o perfil `ADMIN`.

## Verificação

Execute `./gradlew check`. Os testes de integração verificam a geração do
documento OpenAPI e o acesso público à documentação e aos recursos da interface.
