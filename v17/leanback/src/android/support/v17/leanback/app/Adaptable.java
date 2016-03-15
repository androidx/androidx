package android.support.v17.leanback.app;

/**
 * Generic interface to implement the adapter pattern.
 * {@link RowsFragment} implements this interface to return adapter class used in
 * {@link BrowseFragment}.
 */
public interface Adaptable {
    /**
     * Return the adapter for a given adaptable class. Return null if the adapter class is
     * not supported.
     *
     * @param clazz The class type for the adapter.
     * @return The adapter implementation.
     */
    Object getAdapter(Class clazz);
}
