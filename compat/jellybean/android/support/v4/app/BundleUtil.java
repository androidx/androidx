package android.support.v4.app;

import android.os.Bundle;
import android.os.Parcelable;

import java.util.Arrays;

/**
 * @hide
 */
class BundleUtil {
    /**
     * Get an array of Bundle objects from a parcelable array field in a bundle.
     * Update the bundle to have a typed array so fetches in the future don't need
     * to do an array copy.
     */
    public static Bundle[] getBundleArrayFromBundle(Bundle bundle, String key) {
        Parcelable[] array = bundle.getParcelableArray(key);
        if (array instanceof Bundle[] || array == null) {
            return (Bundle[]) array;
        }
        Bundle[] typedArray = Arrays.copyOf(array, array.length,
                Bundle[].class);
        bundle.putParcelableArray(key, typedArray);
        return typedArray;
    }
}
