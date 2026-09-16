# Control de Gastos (gastosBot)

Este proyecto es una herramienta automatizada para el seguimiento de gastos, que obtiene información de "Visa Home" mediante Selenium, la procesa y envía notificaciones a través de Telegram. También incluye un bot handler para ejecutar el proceso bajo demanda.

## Estructura del Proyecto

El proyecto está dividido en dos módulos principales:

1.  **Core (`control-gastos`)**: La aplicación principal que realiza el scraping de Visa y gestiona la base de datos de gastos.
2.  **Bot Handler (`bot-handler`)**: Un servicio que escucha mensajes de Telegram y permite ejecutar el proceso principal mediante el comando `/run`.

## Requisitos Previos

- **Java 11** o superior.
- **MySQL**: Se requiere una base de datos; su nombre lo elegís vos y va en `CONTROL_GASTOS_DB_NAME`. La tabla está en `schema.sql`.
- **Chrome / ChromeDriver**: Necesario para la automatización con Selenium.
- **Tokens de Telegram**: Se requieren dos bots de Telegram (uno para notificaciones de gastos y otro para errores).

## Configuración

La aplicación se configura mediante variables de entorno (o un archivo `.env` en la raíz del proyecto, que se carga automáticamente al iniciar):

| Variable | Descripción |
| --- | --- |
| `CONTROL_GASTOS_DOCUMENT_NUMBER` | Número de documento para acceder a Visa Home / Mis Tarjetas. |
| `CONTROL_GASTOS_VISA_PASSWORD` | Contraseña de Visa Home. |
| `CONTROL_GASTOS_VISA_NICKNAME` | Nombre de usuario/apodo para Visa Home. |
| `CONTROL_GASTOS_TELEGRAM_GASTOS_TOKEN` | Token del bot de Telegram para notificaciones de gastos. |
| `CONTROL_GASTOS_TELEGRAM_ERRORES_TOKEN` | Token del bot de Telegram para reportes de errores. |
| `CONTROL_GASTOS_TELEGRAM_CHAT_ID` | Conversación a la que se publican los gastos. |
| `CONTROL_GASTOS_DB_URL` | JDBC de la base, con la forma `jdbc:mysql://<host>:<puerto>/<esquema>`. |
| `CONTROL_GASTOS_DB_USER` / `..._DB_PASSWORD` | Credenciales de MySQL. |
| `CONTROL_GASTOS_DB_NAME` | Nombre del esquema, sólo lo usa `db-clean.sh`. |
| `CONTROL_GASTOS_MONTHLY_SALARY_USD` | (Opcional) Sueldo en dólares contra el que se mide el acumulado. Sin él, el bot omite la línea `PORCENTAJE SUELDO` y reporta todo lo demás. |
| `CONTROL_GASTOS_GMAIL_CLIENT_ID` / `..._CLIENT_SECRET` / `..._REFRESH_TOKEN` | (Opcional) Credenciales OAuth de Gmail para leer el código OTP del login. |
| `CONTROL_GASTOS_GMAIL_QUERY` / `CONTROL_GASTOS_GMAIL_OTP_REGEX` | (Opcional) Query de búsqueda y regex para extraer el OTP. |

## Ejecución

### 1. Aplicación Principal (Scraper)

Para ejecutar el control de gastos una sola vez:

```bash
./gradlew :run
```

O utilizando el script proporcionado (hace `git pull`, rebuild si hay cambios, y ejecuta el jar):

```bash
./run.sh
```

### 2. Bot Handler (Servicio Continuo)

El bot handler se queda escuchando mensajes de Telegram. Al recibir el comando `/run`, ejecuta el proceso de control de gastos utilizando las variables de entorno `CONTROL_GASTOS_*`.

Para iniciar el bot:

```bash
./gradlew :bot-handler:run
```

*Nota: Si al iniciar faltan las variables `CONTROL_GASTOS_*`, el bot entra en un bucle de espera y reintenta cada una hora hasta que el entorno esté configurado.*

## Funcionalidades del Bot

- **/run**: Dispara manualmente la ejecución del scraper de gastos. El bot informará cuando el proceso comience y cuando termine exitosamente (o si ocurre algún error).
- **/start**: Muestra una lista de los comandos disponibles y su función.

## Base de Datos

MySQL/MariaDB. La tabla se crea con `schema.sql`:

```bash
mysql -u "$CONTROL_GASTOS_DB_USER" -p "$CONTROL_GASTOS_DB_NAME" < schema.sql
```

Nada de esto está en el repositorio, ni las credenciales ni la dirección.
`hibernate.cfg.xml` sólo declara el driver, el dialecto y el mapeo; la url, el
usuario y la contraseña las aplica `SessionFactoryProvider` desde
`CONTROL_GASTOS_DB_URL`, `CONTROL_GASTOS_DB_USER` y `CONTROL_GASTOS_DB_PASSWORD`,
y ninguna tiene valor por defecto: si falta una, el arranque falla nombrándola.
`db-clean.sh` lee el mismo `.env`.

Conviene que MySQL escuche sólo en `127.0.0.1`: el bot corre en la misma máquina
y no necesita aceptar conexiones de la red.

## Limpieza

`db-clean.sh` borra los gastos de más de 30 días. Está pensado para cron, así que
carga el `.env` que tiene al lado en vez de depender del entorno del shell.
