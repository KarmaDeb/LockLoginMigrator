package es.karmadev.locklogin.migrator.configuration;

import com.google.gson.*;
import es.karmadev.locklogin.migrator.configuration.sql.Database;
import es.karmadev.locklogin.migrator.configuration.sql.Table;
import es.karmadev.locklogin.migrator.configuration.sql.column.TableColumn;
import es.karmadev.locklogin.migrator.configuration.sql.column.TableRelation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class Configuration {

    private final JsonObject object;

    public Configuration() throws IOException {
        Gson gson = new GsonBuilder().create();

        Path config = Paths.get("config.json");
        if (!Files.exists(config)) {
            try (InputStream stream = Configuration.class.getResourceAsStream("/config.json")) {
                if (stream == null) throw new RuntimeException("Cannot create configuration file");

                Files.createFile(config);
                Files.copy(stream, config, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        JsonElement element = gson.fromJson(Files.newBufferedReader(config), JsonElement.class);
        if (element == null || !element.isJsonObject()) throw new IOException("Cannot read configuration file");

        this.object = element.getAsJsonObject().get("settings").getAsJsonObject();
    }

    public int getType() {
        String type = object.get("type").getAsString();
        return (type.equalsIgnoreCase("mysql") ? 0 : 1);
    }

    public boolean useSSL() {
        return object.get("ssl").getAsBoolean();
    }

    public boolean verifyCertificates() {
        return object.get("certificates").getAsBoolean();
    }

    public boolean hexToBase() {
        return object.get("migration").getAsJsonObject().get("hex2base").getAsBoolean();
    }

    public boolean plainToBase() {
        return object.get("migration").getAsJsonObject().get("raw2base").getAsBoolean();
    }

    public boolean ignoreEmpty() {
        return object.get("migration").getAsJsonObject().get("ignoreEmpty").getAsBoolean();
    }

    public boolean removeGeyserPrefix() {
        return object.get("migration").getAsJsonObject().get("geyser").getAsJsonObject().get("removePrefix").getAsBoolean();
    }

    public String geyserPrefix() {
        return object.get("migration").getAsJsonObject().get("geyser").getAsJsonObject().get("prefix").getAsString();
    }

    public Database getDatabase() {
        JsonObject databaseObject = object.get("database").getAsJsonObject();

        String name = databaseObject.get("name").getAsString();
        String host = object.get("host").getAsString();
        int port = object.get("port").getAsInt();
        String user = object.get("user").getAsString();
        String pass = object.get("password").getAsString();

        JsonObject userTableObject = databaseObject.get("tables").getAsJsonObject().get("user_table").getAsJsonObject();
        JsonObject accTableObject = databaseObject.get("tables").getAsJsonObject().get("account_table").getAsJsonObject();

        String userTableName = userTableObject.get("name").getAsString();
        String accTableName = accTableObject.get("name").getAsString();

        Database database = new Database(name, host, port, user, pass, userTableName, accTableName);

        JsonArray userTableColumns = userTableObject.get("columns").getAsJsonArray();
        JsonArray accTableColumns = accTableObject.get("columns").getAsJsonArray();

        List<TableRelation> relationCache = new ArrayList<>();

        Table userTable = new Table(userTableName);
        Table accTable = new Table(accTableName);

        for (JsonElement element : userTableColumns) {
            if (!element.isJsonObject()) continue;
            JsonObject elementObject = element.getAsJsonObject();

            String columnName = elementObject.get("name").getAsString();
            String columnType = elementObject.get("type").getAsString();
            JsonElement relation = elementObject.get("relation");
            boolean enabled = elementObject.get("enabled").getAsBoolean();

            TableColumn column = new TableColumn(userTable, columnName, columnType, null, enabled);

            if (relation != null && !relation.isJsonNull()) {
                String targetColumn = relation.getAsJsonObject().get("column").getAsString();

                TableColumn vColumn = new TableColumn(accTable, targetColumn, "primary", null, false);
                /*
                Any user relation must be with an account table
                 */
                TableRelation vRelation = new TableRelation(column, vColumn);
                relationCache.add(vRelation);
            }

            userTable.addColumn(columnType, column);
        }
        for (JsonElement element : accTableColumns) {
            if (!element.isJsonObject()) continue;
            JsonObject elementObject = element.getAsJsonObject();

            String columnName = elementObject.get("name").getAsString();
            String columnType = elementObject.get("type").getAsString();
            JsonElement relation = elementObject.get("relation");
            boolean enabled = elementObject.get("enabled").getAsBoolean();

            TableColumn column = new TableColumn(accTable, columnName, columnType, null, enabled);

            if (relation != null && !relation.isJsonNull()) {
                String targetColumn = relation.getAsJsonObject().get("column").getAsString();

                TableColumn vColumn = new TableColumn(userTable, targetColumn, "primary", null, false);
                /*
                Any account relation must be with an account table
                 */
                TableRelation vRelation = new TableRelation(column, vColumn);
                relationCache.add(vRelation);
            }

            accTable.addColumn(columnType, column);
        }

        for (TableRelation relation : relationCache) {
            TableColumn rOrigin = relation.getOrigin();
            TableColumn vDestination = relation.getDestination();

            Table destTable = vDestination.getTable();
            String vName = vDestination.getName();

            TableColumn realColumn = destTable.getColumn(vName);
            if (realColumn != null) {
                rOrigin.addRelationship(realColumn);
            }
        }

        database.addTable("user:" + userTableName, userTable);
        database.addTable("account:" + accTableName, accTable);

        return database;
    }
}
