package es.karmadev.locklogin.migrator.configuration.sql.column;

import es.karmadev.locklogin.migrator.configuration.sql.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class TableColumn {

    private final Table table;
    private final String name;
    private final String type;
    private TableRelation relation;
    private final boolean enabled;

    /**
     * Add a relationship with this
     * column and the other column
     *
     * @param other the other column
     */
    public void addRelationship(final TableColumn other) {
        relation = new TableRelation(this, other);
    }
}
