package es.karmadev.locklogin.migrator.configuration.sql;

import es.karmadev.locklogin.migrator.configuration.sql.column.TableColumn;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a database table
 */
@RequiredArgsConstructor
public class Table {

    private final Map<String, TableColumn> columns = new HashMap<>();

    @Getter
    private final String name;

    /**
     * Get a column
     *
     * @param type the column type
     * @return the column
     */
    public TableColumn getColumn(final String type) {
        return columns.get(type);
    }

    /**
     * Add a column
     *
     * @param type the column type
     * @param column the column
     */
    public void addColumn(final String type, final TableColumn column) {
        columns.put(type, column);
    }
}
