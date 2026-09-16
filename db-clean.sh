#!/usr/bin/env bash
set -euo pipefail

# Deletes spendings older than 30 days.
#
# Runs from cron, which inherits none of the shell's environment, so the .env
# beside this script is where the credentials come from -- the same file the
# Java side reads. Nothing here is committed: .env is gitignored.
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "$REPO/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$REPO/.env"
  set +a
fi

: "${CONTROL_GASTOS_DB_USER:?not set (see .env.example)}"
: "${CONTROL_GASTOS_DB_PASSWORD:?not set (see .env.example)}"
: "${CONTROL_GASTOS_DB_NAME:?not set (see .env.example)}"

# MYSQL_PWD rather than --password=: an argument is visible to anyone who can
# run ps, including on a machine where this is the only thing running.
printf 'use %s;\ndelete from spending where creation_date < now() - interval 30 DAY;\n' "$CONTROL_GASTOS_DB_NAME" \
  | MYSQL_PWD="$CONTROL_GASTOS_DB_PASSWORD" mysql -u "$CONTROL_GASTOS_DB_USER"
