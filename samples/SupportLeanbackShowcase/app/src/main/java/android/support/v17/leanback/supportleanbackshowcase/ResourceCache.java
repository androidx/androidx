package android.support.v17.leanback.supportleanbackshowcase;

import android.util.SparseArray;
import android.view.View;

/**
 * ResourceCache allows retrieving children from a given view and caches the resulting views in
 * order to prevent future lookups.
 */
public class ResourceCache {

    private final SparseArray<View> mCachedViews = new SparseArray<View>();

    public <ViewType extends View> ViewType getViewById(View view, int resId) {
        View child = mCachedViews.get(resId, null);
        if (child == null) {
            child = view.findViewById(resId);
            mCachedViews.put(resId, child);
        }
        return (ViewType) child;
    }
}
