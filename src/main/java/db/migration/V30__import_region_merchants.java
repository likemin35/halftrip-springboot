package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class V30__import_region_merchants extends BaseJavaMigration {

    private static final int BATCH_SIZE = 500;

    private static final List<SeedFile> SEED_FILES = List.of(
            new SeedFile(1L, "merchant-seeds/01-wando.csv"),
            new SeedFile(2L, "merchant-seeds/02-gangjin.csv"),
            new SeedFile(3L, "merchant-seeds/03-pyeongchang.csv"),
            new SeedFile(4L, "merchant-seeds/04-haenam.csv"),
            new SeedFile(5L, "merchant-seeds/05-yeonggwang.csv"),
            new SeedFile(6L, "merchant-seeds/06-hoengseong.csv"),
            new SeedFile(7L, "merchant-seeds/07-yeongwol.csv"),
            new SeedFile(8L, "merchant-seeds/08-jecheon.csv"),
            new SeedFile(9L, "merchant-seeds/09-geochang.csv"),
            new SeedFile(10L, "merchant-seeds/10-gochang.csv"),
            new SeedFile(11L, "merchant-seeds/11-yeongam.csv"),
            new SeedFile(12L, "merchant-seeds/12-hapcheon.csv"),
            new SeedFile(13L, "merchant-seeds/13-miryang.csv"),
            new SeedFile(14L, "merchant-seeds/14-hadong.csv"),
            new SeedFile(15L, "merchant-seeds/15-namhae.csv"),
            new SeedFile(16L, "merchant-seeds/16-goheung.csv")
    );

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        ensureMerchantCoordinateColumns(connection);

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM merchants");
            statement.executeUpdate("ALTER TABLE merchants AUTO_INCREMENT = 1");
        }

        String sql = """
                INSERT INTO merchants
                    (region_id, name, address, category, latitude, longitude, created_at, updated_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SeedFile seedFile : SEED_FILES) {
                importSeedFile(statement, seedFile);
            }
        }
    }

    private void ensureMerchantCoordinateColumns(Connection connection) throws SQLException {
        if (!hasColumn(connection, "merchants", "latitude")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE merchants ADD COLUMN latitude DOUBLE NULL");
            }
        }
        if (!hasColumn(connection, "merchants", "longitude")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE merchants ADD COLUMN longitude DOUBLE NULL");
            }
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private void importSeedFile(PreparedStatement statement, SeedFile seedFile) throws IOException, SQLException {
        try (InputStream inputStream = getResourceAsStream(seedFile.resourcePath());
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            reader.readLine(); // header
            int batched = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                List<String> columns = parseCsvLine(line);
                if (columns.size() < 5) {
                    continue;
                }

                String name = trimToNull(columns.get(0));
                String address = trimToNull(columns.get(1));
                if (name == null || address == null) {
                    continue;
                }

                statement.setLong(1, seedFile.regionId());
                statement.setString(2, name);
                statement.setString(3, address);

                String category = trimToNull(columns.get(2));
                if (category == null) {
                    statement.setNull(4, Types.VARCHAR);
                } else {
                    statement.setString(4, category);
                }

                setNullableDouble(statement, 5, columns.get(3));
                setNullableDouble(statement, 6, columns.get(4));
                statement.addBatch();
                batched++;

                if (batched >= BATCH_SIZE) {
                    statement.executeBatch();
                    batched = 0;
                }
            }

            if (batched > 0) {
                statement.executeBatch();
            }
        }
    }

    private InputStream getResourceAsStream(String path) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IOException("Merchant seed not found: " + path);
        }
        return inputStream;
    }

    private void setNullableDouble(PreparedStatement statement, int index, String rawValue) throws SQLException {
        String value = trimToNull(rawValue);
        if (value == null) {
            statement.setNull(index, Types.DOUBLE);
            return;
        }

        try {
            statement.setDouble(index, Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            statement.setNull(index, Types.DOUBLE);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!trimmed.isEmpty() && trimmed.charAt(0) == '\uFEFF') {
            trimmed = trimmed.substring(1).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        values.add(current.toString());
        return values;
    }

    private record SeedFile(Long regionId, String resourcePath) {
    }
}
