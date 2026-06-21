package com.banking.moneytransfer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@Slf4j
public class SnowflakeService {

    @Autowired
    @Qualifier("snowflakeDataSource")
    private DataSource snowflakeDataSource;

    /**
     * Exports a CSV of transactions into the Snowflake analytics warehouse.
     * The required objects (database, schema, table, stage, file format) are
     * created on first run, so no manual Snowflake setup is needed.
     */
    public void uploadAndCopy(File file) throws Exception {
        log.info("Loading CSV file into Snowflake: {}", file.getAbsolutePath());

        try (Connection conn = snowflakeDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            ensureSchema(stmt);

            // Snowflake PUT needs a forward-slash file URI (works on Windows too).
            String fileUri = "file://" + file.getAbsolutePath().replace('\\', '/');

            stmt.execute("REMOVE @TRANSFER_STAGE");
            stmt.execute("PUT '" + fileUri + "' @TRANSFER_STAGE OVERWRITE = TRUE AUTO_COMPRESS = TRUE");
            stmt.execute("""
                COPY INTO FACT_TRANSACTIONS (ACCOUNT_FROM_KEY, ACCOUNT_TO_KEY, AMOUNT, STATUS, DATE_KEY)
                FROM @TRANSFER_STAGE
                FILE_FORMAT = (FORMAT_NAME = CSV_FORMAT)
                ON_ERROR = 'CONTINUE'""");

            log.info("Snowflake load complete");
        }
    }

    /**
     * Idempotently provisions the analytics objects this ETL relies on.
     */
    private void ensureSchema(Statement stmt) throws SQLException {
        stmt.execute("CREATE DATABASE IF NOT EXISTS MONEY_TRANSFER_DW");
        stmt.execute("CREATE SCHEMA IF NOT EXISTS MONEY_TRANSFER_DW.ANALYTICS");
        stmt.execute("USE SCHEMA MONEY_TRANSFER_DW.ANALYTICS");
        stmt.execute("USE WAREHOUSE COMPUTE_WH");
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS FACT_TRANSACTIONS (
                ACCOUNT_FROM_KEY VARCHAR,
                ACCOUNT_TO_KEY   VARCHAR,
                AMOUNT           NUMBER(18,2),
                STATUS           VARCHAR,
                DATE_KEY         NUMBER
            )""");
        stmt.execute("""
            CREATE FILE FORMAT IF NOT EXISTS CSV_FORMAT
                TYPE = CSV
                FIELD_DELIMITER = ','
                SKIP_HEADER = 1
                NULL_IF = ('NULL', 'null')
                EMPTY_FIELD_AS_NULL = TRUE""");
        stmt.execute("CREATE STAGE IF NOT EXISTS TRANSFER_STAGE FILE_FORMAT = CSV_FORMAT");
        log.info("Snowflake schema ensured (MONEY_TRANSFER_DW.ANALYTICS)");
    }
}
