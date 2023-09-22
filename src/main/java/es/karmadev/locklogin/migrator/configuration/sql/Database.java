package es.karmadev.locklogin.migrator.configuration.sql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a database
 */
@RequiredArgsConstructor
public class Database {

    private final Map<String, Table> tables = new HashMap<>();

    @Getter
    private final String name;
    @Getter
    private final String host;
    @Getter
    private final int port;
    @Getter
    private final String user;
    @Getter
    private final String password;
    @Getter
    private final String userTable;
    @Getter
    private final String accTable;

    /**
     * Get the table
     *
     * @param name the table name
     * @return the table
     */
    public Table getTable(final String name) {
        return tables.get(name);
    }

    /**
     * Add a table
     *
     * @param name the table name
     * @param table the table
     */
    public void addTable(final String name, final Table table) {
        tables.put(name, table);
    }
}
