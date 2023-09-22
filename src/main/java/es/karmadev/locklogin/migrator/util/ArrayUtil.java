package es.karmadev.locklogin.migrator.util;

import java.util.Arrays;

public class ArrayUtil {

    @SafeVarargs
    public static <T> T[] mix(final T[] output, final T[]... arrays) {
        int totalLength = 0;
        for (T[] t : arrays) {
            totalLength += t.length;
        }

        T[] destination = output;
        if (output.length < totalLength) {
            destination = Arrays.copyOf(destination, totalLength);
        }

        int vIndex = 0;

        for (T[] array : arrays) {
            for (T element : array) {
                destination[vIndex++] = element;
            }
        }

        return destination;
    }
}
