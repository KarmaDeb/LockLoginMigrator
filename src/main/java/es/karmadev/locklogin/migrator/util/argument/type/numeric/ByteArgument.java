package es.karmadev.locklogin.migrator.util.argument.type;

import es.karmadev.locklogin.migrator.util.argument.AbstractArgument;
import es.karmadev.locklogin.migrator.util.argument.ClassArgument;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Represents a string argument
 */
public class ByteArgument extends AbstractArgument<Boolean> {

    /**
     * Initialize the argument
     *
     * @param key         the argument key
     * @param description the argument description
     * @param help        the argument help message
     */
    ByteArgument(final String key, final String description, final String help) {
        super(key, description, help, Boolean.class);
    }

    /**
     * Converse from string value
     *
     * @param value the value
     * @return the typed value
     */
    @Override
    public Optional<Boolean> converse(final Object value) {
        Boolean v = null;
        try {
            v = (Boolean) value;
        } catch (ClassCastException ignored) {}

        return Optional.ofNullable(v);
    }

    /**
     * Map the argument into the instance object
     * fields
     *
     * @param instance the object instance in where
     *                 to put the argument
     * @param value    the value to map
     */
    @Override
    public void mapArgument(final Object instance, final Boolean value) {
        Class<?> instanceClass = instance.getClass();
        for (Field field : instanceClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                if (field.isAnnotationPresent(ClassArgument.class)) {
                    ClassArgument argument = field.getAnnotation(ClassArgument.class);
                    if (argument.name().equals(key)) {
                        try {
                            field.setAccessible(true);
                            field.set(instance, value);
                        } catch (IllegalAccessException ignored) {}
                    }
                }
            }
        }
    }

    /**
     * Create a new argument
     *
     * @param key the key
     * @return the argument
     */
    public static ByteArgument valueOf(final String key) {
        return valueOf(key, "", "");
    }

    /**
     * Create a new argument
     *
     * @param key the key
     * @param description the description
     * @return the argument
     */
    public static ByteArgument valueOf(final String key, final String description) {
        return valueOf(key, description, "");
    }

    /**
     * Create a new argument
     *
     * @param key the key
     * @param description the description
     * @param help the help message
     * @return the argument
     */
    public static ByteArgument valueOf(final String key, final String description, final String help) {
        return new ByteArgument(key, description, help);
    }
}
