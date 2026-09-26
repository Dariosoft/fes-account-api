# Google OAuth

Una persona crea el cliente OAuth en Google. El client id y el client secret no van en el código ni en el bundle de los frontends.

1. Crear un OAuth Client ID de tipo aplicación web.
2. Registrar la redirect URI `{origen de api}/accounts/login/google/callback`. En Minikube es `http://api.friendly-e-shop.test/accounts/login/google/callback`. En otro dominio, el mismo path sobre el host `api` de ese dominio.
3. Registrar los orígenes de `shop` y `panel` de ese mismo entorno.
4. Cargar `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` como secretos del entorno. En Minikube pueden vivir en el secreto de desarrollo. En Hostinger el secret va cifrado con SOPS y no se commitea en claro.
5. `SESSION_COOKIE_DOMAIN` y `BROWSER_ORIGINS` siguen el dominio de ese entorno. En local la cookie es `.friendly-e-shop.test`.
