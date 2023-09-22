package es.karmadev.locklogin.migrator.configuration.database;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a database
 */
public class Database {

    private final Map<String, Table> tables = new HashMap<>();
    private final String host;

    /**
     * Get the table
     *
     * @param name the table name
     * @return the table
     */
    public Table getTable(final String name) {
        return tables.get(name);
    }
}
