#!/usr/bin/env bash

function run_psql() {
  local sql_input="$1"
  PGPASSWORD=secret psql -U myuser -h localhost mydatabase <<EOF
$sql_input
EOF
}

run_psql "`cat dogs.sql`"
run_psql "select id, name from dog"
