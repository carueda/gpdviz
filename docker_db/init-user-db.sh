#!/bin/bash
set -e

echo "gpdviz: creating user and database"
psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" <<-EOSQL
    CREATE USER ${GPDVIZ_DB_USERNAME} WITH PASSWORD '${GPDVIZ_DB_USERPASS}';
    CREATE DATABASE gpdviz owner=${GPDVIZ_DB_USERNAME};
    GRANT ALL PRIVILEGES ON DATABASE gpdviz TO ${GPDVIZ_DB_USERNAME};
    ALTER DATABASE gpdviz SET TIMEZONE='GMT';
    CREATE DATABASE gpdviz_test owner=${GPDVIZ_DB_USERNAME};
    GRANT ALL PRIVILEGES ON DATABASE gpdviz_test TO ${GPDVIZ_DB_USERNAME};
    ALTER DATABASE gpdviz_test SET TIMEZONE='GMT';
EOSQL