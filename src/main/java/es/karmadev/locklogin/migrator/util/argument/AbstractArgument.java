package es.karmadev.locklogin.migrator.argument;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Represents an argument
 */
public abstract class AbstractArgument<T> {

    /**
     * The argument key
     */
    private final String key;
    /**
     * The argument type, used later by
     * {@link #converse(String)}
     */
    private final Class<T> type;

    /**
     * Initialize the argument
     *
     * @param key the argument key
     * @param type the argument type
     */
    private AbstractArgument(final String key, final Class<T> type) {
        this.key = key;
        this.type = type;
    }

    /**
     * Get the key
     *
     * @return the key
     */
    public final String getKey() {
        return key;
    }

    /**
     * Get the type
     *
     * @return the type
     */
    public final Type getType() {
        return type;
    }

    /**
     * Converse from string value
     *
     * @param value the value
     * @return the typed value
     */
    public abstract Optional<T> converse(final String value);
}
