// Objeto de configuração inserido no inicializador padrão do Swagger UI.
({
  requestInterceptor: function (request) {
    const url = new URL(request.url, window.location.href);
    const login = new URL('../login', window.location.href);
    if (request.method === 'POST' && url.href === login.href) {
      // Permite entrar novamente mesmo quando a autorização anterior expirou.
      for (const header of Object.keys(request.headers)) {
        if (header.toLowerCase() === 'authorization') {
          delete request.headers[header];
        }
      }
    }
    return request;
  },
  responseInterceptor: function (response) {
    const url = new URL(response.url, window.location.href);
    const login = new URL('../login', window.location.href);
    if (url.href === login.href && response.status === 200) {
      const token = response.obj?.tokenAcesso ?? response.body?.tokenAcesso;
      if (typeof token === 'string' && token.length > 0) {
        window.ui.preauthorizeApiKey('bearerAuth', token);
        window.ui.authActions.persistAuthorizationIfNeeded();
      }
    }
    return response;
  },
})
