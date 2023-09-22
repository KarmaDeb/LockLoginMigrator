package es.karmadev.locklogin.migrator.configuration.sql.column;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a relation between two tables
 */
@AllArgsConstructor @Getter
public class TableRelation {

    private final TableColumn origin;
    private final TableColumn destination;
}
