package org.graded_classes.graded_attendance.components;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class AnswerCompositeUniqueMigration {

    /**
     * Different versions of the application may contain either:
     *
     * answer
     * answers
     */
    private static final List<String> POSSIBLE_TABLE_NAMES =
            List.of("answer", "answers");

    /**
     * If neither answer nor answers exists, this table will be created.
     */
    private static final String DEFAULT_TABLE_NAME = "answers";

    private AnswerCompositeUniqueMigration() {
    }

    public static void main(String[] args) {

        Path databaseFolder = Paths.get(
                System.getProperty("user.home"),
                "My Drive",
                "GradeEd_Exam_2026"
        );

        migrateAll(databaseFolder);
    }

    public static void migrateAll(Path databaseFolder) {

        if (!Files.isDirectory(databaseFolder)) {
            throw new IllegalArgumentException(
                    "Database folder not found: "
                            + databaseFolder.toAbsolutePath()
            );
        }

        List<Path> databaseFiles = findDatabaseFiles(databaseFolder);

        int databasesChanged = 0;
        int databasesUnchanged = 0;
        int databasesFailed = 0;

        int tablesCreated = 0;
        int tablesMigrated = 0;
        int tablesAlreadyCorrect = 0;
        int duplicatesRemoved = 0;

        for (Path databaseFile : databaseFiles) {

            try {
                DatabaseMigrationResult result =
                        migrateDatabase(databaseFile);

                tablesCreated += result.tablesCreated();
                tablesMigrated += result.tablesMigrated();
                tablesAlreadyCorrect += result.tablesAlreadyCorrect();
                duplicatesRemoved += result.duplicatesRemoved();

                if (result.tablesCreated() > 0
                        || result.tablesMigrated() > 0) {

                    databasesChanged++;

                } else {
                    databasesUnchanged++;
                }

            } catch (Exception exception) {
                databasesFailed++;

                System.err.printf(
                        "[FAILED] %-20s %s%n",
                        databaseFile.getFileName(),
                        exception.getMessage()
                );

                exception.printStackTrace(System.err);
            }
        }

        System.out.println();
        System.out.println("========== MIGRATION SUMMARY ==========");
        System.out.println(
                "Database files found:      "
                        + databaseFiles.size()
        );
        System.out.println(
                "Databases changed:         "
                        + databasesChanged
        );
        System.out.println(
                "Databases unchanged:       "
                        + databasesUnchanged
        );
        System.out.println(
                "Databases failed:          "
                        + databasesFailed
        );
        System.out.println(
                "Tables created:            "
                        + tablesCreated
        );
        System.out.println(
                "Tables migrated:           "
                        + tablesMigrated
        );
        System.out.println(
                "Tables already correct:    "
                        + tablesAlreadyCorrect
        );
        System.out.println(
                "Duplicate rows removed:    "
                        + duplicatesRemoved
        );
        System.out.println("=======================================");
    }

    private static List<Path> findDatabaseFiles(
            Path databaseFolder
    ) {

        try (Stream<Path> paths = Files.list(databaseFolder)) {

            return paths
                    .filter(Files::isRegularFile)
                    .filter(
                            AnswerCompositeUniqueMigration::isDatabaseFile
                    )
                    .sorted(
                            Comparator.comparing(
                                    path -> path.getFileName()
                                            .toString()
                                            .toLowerCase(Locale.ROOT)
                            )
                    )
                    .toList();

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Unable to read database folder: "
                            + databaseFolder.toAbsolutePath(),
                    exception
            );
        }
    }

    private static DatabaseMigrationResult migrateDatabase(
            Path databaseFile
    ) throws SQLException {

        String databaseUrl =
                "jdbc:sqlite:"
                        + databaseFile.toAbsolutePath().normalize();

        int tablesCreated = 0;
        int tablesMigrated = 0;
        int tablesAlreadyCorrect = 0;
        int duplicatesRemoved = 0;

        try (Connection connection =
                     DriverManager.getConnection(databaseUrl)) {

            configureConnection(connection);

            List<String> existingAnswerTables =
                    findExistingAnswerTables(connection);

            /*
             * If neither answer nor answers exists,
             * create the canonical answers table.
             */
            if (existingAnswerTables.isEmpty()) {

                createMissingTable(
                        connection,
                        databaseFile,
                        DEFAULT_TABLE_NAME
                );

                tablesCreated++;

            } else {

                /*
                 * If answer exists, migrate answer.
                 * If answers exists, migrate answers.
                 * If both exist, migrate both.
                 */
                for (String tableName : existingAnswerTables) {

                    TableMigrationResult result =
                            migrateExistingTable(
                                    connection,
                                    databaseFile,
                                    tableName
                            );

                    if (result.status()
                            == TableMigrationStatus.MIGRATED) {

                        tablesMigrated++;
                        duplicatesRemoved +=
                                result.duplicatesRemoved();

                    } else {
                        tablesAlreadyCorrect++;
                    }
                }
            }

            verifyIntegrity(connection, databaseFile);
        }

        return new DatabaseMigrationResult(
                tablesCreated,
                tablesMigrated,
                tablesAlreadyCorrect,
                duplicatesRemoved
        );
    }

    private static void configureConnection(
            Connection connection
    ) throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 15000");

            /*
             * Table rebuilding can otherwise fail when foreign keys
             * reference the answer table.
             *
             * Run this migration while the main application is closed.
             */
            statement.execute("PRAGMA foreign_keys = OFF");
        }
    }

    private static List<String> findExistingAnswerTables(
            Connection connection
    ) throws SQLException {

        List<String> existingTables = new ArrayList<>();

        for (String tableName : POSSIBLE_TABLE_NAMES) {
            if (tableExists(connection, tableName)) {
                existingTables.add(tableName);
            }
        }

        return existingTables;
    }

    private static void createMissingTable(
            Connection connection,
            Path databaseFile,
            String tableName
    ) throws SQLException {

        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            createConstrainedTable(connection, tableName);

            connection.commit();

            System.out.printf(
                    "[CREATED]  %-19s table=%s%n",
                    databaseFile.getFileName(),
                    tableName
            );

        } catch (SQLException exception) {
            rollbackQuietly(connection);

            throw new SQLException(
                    "Could not create table '"
                            + tableName
                            + "' in "
                            + databaseFile.getFileName()
                            + ": "
                            + exception.getMessage(),
                    exception
            );

        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private static TableMigrationResult migrateExistingTable(
            Connection connection,
            Path databaseFile,
            String tableName
    ) throws SQLException {

        verifyRequiredColumns(connection, tableName);

        if (requiredConstraintExists(connection, tableName)) {

            System.out.printf(
                    "[CORRECT]  %-19s table=%s%n",
                    databaseFile.getFileName(),
                    tableName
            );

            return new TableMigrationResult(
                    TableMigrationStatus.ALREADY_CORRECT,
                    0
            );
        }

        String temporaryTableName =
                tableName + "_migration_old";

        if (tableExists(connection, temporaryTableName)) {
            throw new SQLException(
                    "Temporary table already exists: "
                            + temporaryTableName
                            + ". Inspect "
                            + databaseFile.getFileName()
                            + " before running the migration again."
            );
        }

        int originalRowCount =
                countRows(connection, tableName);

        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            renameTable(
                    connection,
                    tableName,
                    temporaryTableName
            );

            createConstrainedTable(
                    connection,
                    tableName
            );

            int copiedRows = copyLatestUniqueRows(
                    connection,
                    tableName,
                    temporaryTableName
            );

            executeUpdate(
                    connection,
                    "DROP TABLE "
                            + quoteIdentifier(temporaryTableName)
            );

            connection.commit();

            int removedDuplicates =
                    originalRowCount - copiedRows;

            System.out.printf(
                    "[MIGRATED] %-18s table=%-7s "
                            + "rows copied=%d, duplicates removed=%d%n",
                    databaseFile.getFileName(),
                    tableName,
                    copiedRows,
                    removedDuplicates
            );

            return new TableMigrationResult(
                    TableMigrationStatus.MIGRATED,
                    removedDuplicates
            );

        } catch (SQLException exception) {
            rollbackQuietly(connection);

            throw new SQLException(
                    "Could not migrate table '"
                            + tableName
                            + "' in "
                            + databaseFile.getFileName()
                            + ": "
                            + exception.getMessage(),
                    exception
            );

        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private static void renameTable(
            Connection connection,
            String currentName,
            String newName
    ) throws SQLException {

        String sql =
                "ALTER TABLE "
                        + quoteIdentifier(currentName)
                        + " RENAME TO "
                        + quoteIdentifier(newName);

        executeUpdate(connection, sql);
    }

    private static void createConstrainedTable(
            Connection connection,
            String tableName
    ) throws SQLException {

        String safeTable =
                quoteIdentifier(tableName);

        String constraintName =
                quoteIdentifier(
                        "uq_"
                                + tableName
                                + "_exam_question_date"
                );

        String sql = """
            CREATE TABLE %s (
                id INTEGER PRIMARY KEY AUTOINCREMENT,

                exam_id INTEGER NOT NULL,

                question_id INTEGER NOT NULL,

                selected_option_id INTEGER NOT NULL,

                time_slot TEXT NOT NULL,

                start_time DATETIME NOT NULL,

                end_time DATETIME NOT NULL,

                created_at DATETIME NOT NULL
                    DEFAULT CURRENT_TIMESTAMP,

                created_date TEXT NOT NULL
                    DEFAULT (DATE('now', 'localtime')),

                CONSTRAINT %s
                    UNIQUE (
                        exam_id,
                        question_id,
                        created_date
                    )
            )
            """.formatted(
                safeTable,
                constraintName
        );

        executeUpdate(connection, sql);
    }

    /**
     * Copies one row for each unique combination of:
     *
     * exam_id + question_id + creation date.
     *
     * If duplicates exist, the row with the highest id is kept.
     */
    private static int copyLatestUniqueRows(
            Connection connection,
            String newTableName,
            String oldTableName
    ) throws SQLException {

        String newTable =
                quoteIdentifier(newTableName);

        String oldTable =
                quoteIdentifier(oldTableName);

        String sql = """
            INSERT INTO %s (
                id,
                exam_id,
                question_id,
                selected_option_id,
                time_slot,
                start_time,
                end_time,
                created_at,
                created_date
            )
            SELECT
                old.id,
                old.exam_id,
                old.question_id,
                old.selected_option_id,
                old.time_slot,
                old.start_time,
                old.end_time,

                COALESCE(
                    old.created_at,
                    CURRENT_TIMESTAMP
                ),

                COALESCE(
                    DATE(old.created_at, 'localtime'),
                    DATE('now', 'localtime')
                )

            FROM %s AS old

            WHERE old.id = (
                SELECT MAX(latest.id)
                FROM %s AS latest

                WHERE latest.exam_id = old.exam_id

                  AND latest.question_id =
                      old.question_id

                  AND COALESCE(
                      DATE(
                          latest.created_at,
                          'localtime'
                      ),
                      DATE('now', 'localtime')
                  ) = COALESCE(
                      DATE(
                          old.created_at,
                          'localtime'
                      ),
                      DATE('now', 'localtime')
                  )
            )
            """.formatted(
                newTable,
                oldTable,
                oldTable
        );

        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    private static boolean requiredConstraintExists(
            Connection connection,
            String tableName
    ) throws SQLException {

        /*
         * A genuine SQLite table-level UNIQUE constraint is internally
         * represented by an automatically created unique index.
         *
         * origin = 'u' means the index comes from a UNIQUE constraint.
         */
        String sql =
                "PRAGMA index_list("
                        + quoteIdentifier(tableName)
                        + ")";

        try (
                Statement statement = connection.createStatement();
                ResultSet indexes = statement.executeQuery(sql)
        ) {
            while (indexes.next()) {

                boolean unique =
                        indexes.getInt("unique") == 1;

                String origin =
                        indexes.getString("origin");

                if (!unique
                        || origin == null
                        || !origin.equalsIgnoreCase("u")) {
                    continue;
                }

                String indexName =
                        indexes.getString("name");

                if (indexMatchesRequiredColumns(
                        connection,
                        indexName
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean indexMatchesRequiredColumns(
            Connection connection,
            String indexName
    ) throws SQLException {

        List<String> expectedColumns = List.of(
                "exam_id",
                "question_id",
                "created_date"
        );

        List<String> actualColumns =
                new ArrayList<>();

        String sql =
                "PRAGMA index_info("
                        + quoteIdentifier(indexName)
                        + ")";

        try (
                Statement statement = connection.createStatement();
                ResultSet columns = statement.executeQuery(sql)
        ) {
            while (columns.next()) {
                actualColumns.add(
                        columns.getString("name")
                                .toLowerCase(Locale.ROOT)
                );
            }
        }

        return actualColumns.equals(expectedColumns);
    }

    private static void verifyRequiredColumns(
            Connection connection,
            String tableName
    ) throws SQLException {

        List<String> requiredColumns = List.of(
                "id",
                "exam_id",
                "question_id",
                "selected_option_id",
                "time_slot",
                "start_time",
                "end_time",
                "created_at"
        );

        for (String columnName : requiredColumns) {

            if (!columnExists(
                    connection,
                    tableName,
                    columnName
            )) {
                throw new SQLException(
                        "Table '"
                                + tableName
                                + "' is missing required column: "
                                + columnName
                );
            }
        }
    }

    private static void verifyIntegrity(
            Connection connection,
            Path databaseFile
    ) throws SQLException {

        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "PRAGMA integrity_check"
                        )
        ) {
            if (!resultSet.next()) {
                throw new SQLException(
                        "Integrity check returned no result for "
                                + databaseFile.getFileName()
                );
            }

            String result = resultSet.getString(1);

            if (!"ok".equalsIgnoreCase(result)) {
                throw new SQLException(
                        "Integrity check failed for "
                                + databaseFile.getFileName()
                                + ": "
                                + result
                );
            }
        }
    }

    private static int countRows(
            Connection connection,
            String tableName
    ) throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM "
                        + quoteIdentifier(tableName);

        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)
        ) {
            if (!resultSet.next()) {
                return 0;
            }

            return resultSet.getInt(1);
        }
    }

    private static boolean tableExists(
            Connection connection,
            String tableName
    ) throws SQLException {

        String sql = """
            SELECT 1
            FROM sqlite_master
            WHERE type = 'table'
              AND name = ? COLLATE NOCASE
            LIMIT 1
            """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, tableName);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next();
            }
        }
    }

    private static boolean columnExists(
            Connection connection,
            String tableName,
            String columnName
    ) throws SQLException {

        String sql =
                "PRAGMA table_info("
                        + quoteIdentifier(tableName)
                        + ")";

        try (
                Statement statement = connection.createStatement();
                ResultSet columns = statement.executeQuery(sql)
        ) {
            while (columns.next()) {

                String existingColumnName =
                        columns.getString("name");

                if (columnName.equalsIgnoreCase(
                        existingColumnName
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void executeUpdate(
            Connection connection,
            String sql
    ) throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void rollbackQuietly(
            Connection connection
    ) {

        try {
            connection.rollback();

        } catch (SQLException rollbackException) {
            System.err.println(
                    "Rollback failed: "
                            + rollbackException.getMessage()
            );
        }
    }

    private static boolean isDatabaseFile(Path path) {

        String fileName =
                path.getFileName()
                        .toString()
                        .toLowerCase(Locale.ROOT);

        /*
         * Ignore SQLite temporary and journaling files.
         */
        if (fileName.endsWith("-wal")
                || fileName.endsWith("-shm")
                || fileName.endsWith("-journal")) {

            return false;
        }

        /*
         * Supported names:
         *
         * ED01
         * ED01.db
         * ED01.sqlite
         * ED01.sqlite3
         */
        return fileName.matches("ed\\d+")
                || fileName.matches("ed\\d+\\.db")
                || fileName.matches("ed\\d+\\.sqlite")
                || fileName.matches("ed\\d+\\.sqlite3");
    }

    private static String quoteIdentifier(
            String identifier
    ) {

        return "\""
                + identifier.replace("\"", "\"\"")
                + "\"";
    }

    private enum TableMigrationStatus {
        MIGRATED,
        ALREADY_CORRECT
    }

    private record TableMigrationResult(
            TableMigrationStatus status,
            int duplicatesRemoved
    ) {
    }

    private record DatabaseMigrationResult(
            int tablesCreated,
            int tablesMigrated,
            int tablesAlreadyCorrect,
            int duplicatesRemoved
    ) {
    }
}