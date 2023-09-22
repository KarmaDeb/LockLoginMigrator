package es.karmadev.locklogin.migrator.util;

public final class NullObject {

    @SuppressWarnings("InstantiationOfUtilityClass")
    private final static NullObject instance = new NullObject();

    /**
     * Prevent null object creation
     */
    private NullObject() {}

    /**
     * Get the object instance
     *
     * @return the object instance
     */
    public static NullObject getInstance() {
        return instance;
    }
}
