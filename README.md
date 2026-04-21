# Backend TechStore

Backend REST de TechStore (Spring Boot + Spring Security + JPA).

## Variables de entorno recomendadas

Para no exponer secretos en repositorios publicos, configura estas variables:

- `DB_URL` (ejemplo: `jdbc:postgresql://localhost:5432/dbTechStore`)
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `IP_FRONTEND` (ejemplo: `https://tu-frontend.onrender.com`)
- `DDL_AUTO` (recomendado en cloud: `update`)
- `SQL_INIT_MODE` (recomendado en cloud: `never`)
- `PORT` (Render lo asigna automaticamente)

Si no defines variables, el proyecto usa valores de desarrollo locales.

## Configuracion recomendada para Render

Usa estos valores en el servicio backend de Render:

- `DB_URL`: JDBC de tu Postgres en Render
- `DB_USERNAME`: usuario de la BD
- `DB_PASSWORD`: password de la BD
- `JWT_SECRET`: secreto largo y unico
- `IP_FRONTEND`: URL publica de tu frontend
- `DDL_AUTO`: `update`
- `SQL_INIT_MODE`: `never`

Notas:

- En local puedes seguir trabajando sin definir estas variables (se usan defaults).
- En produccion evita `create-drop` y evita ejecutar `import.sql` en cada arranque.

## Datos semilla (desarrollo)

El archivo `src/main/resources/import.sql` carga usuarios y productos de ejemplo.

Credenciales demo:

- Admin: `admin.demo@techstore.local` / `Admin#123`
- User: `user1.demo@techstore.local` / `User#123`
- User: `user2.demo@techstore.local` / `User#123`

> Importante: no usar estas credenciales en produccion.

## Ejecutar proyecto

```powershell
.\mvnw.cmd spring-boot:run
```

## Build rapido

```powershell
.\mvnw.cmd -q -DskipTests compile
```

