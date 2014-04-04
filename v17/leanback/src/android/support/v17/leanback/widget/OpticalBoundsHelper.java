package android.support.v17.leanback.widget;

import android.content.Context;
import android.os.Build;
import android.view.ViewGroup;


/**
 * Helper for optical bounds.
 */
final class OpticalBoundsHelper {

    final static OpticalBoundsHelper sInstance = new OpticalBoundsHelper();
    OpticalBoundsHelperVersionImpl mImpl;

    /**
     * Gets whether the system supports OpticalBounds.
     *
     * @return True if OpticalBounds are supported.
     */
    public static boolean systemSupportsOpticalBounds() {
        if (Build.VERSION.SDK_INT >= 18) {
            // Supported on JBMR2 or later.
            return true;
        }
        return false;
    }

    /**
     * Interface implemented by classes that support OpticalBounds.
     */
    static interface OpticalBoundsHelperVersionImpl {

        public void setOpticalBounds(ViewGroup view);

    }

    /**
     * Interface used when we do not support OpticalBounds animations.
     */
    private static final class OpticalBoundsHelperStubImpl implements OpticalBoundsHelperVersionImpl {

        @Override
        public void setOpticalBounds(ViewGroup view) {
            // do nothing if not supported
        }

    }

    /**
     * Implementation used on JBMR2 (and above).
     */
    private static final class OpticalBoundsHelperJBMR2Impl implements OpticalBoundsHelperVersionImpl {
        private final OpticalBoundsHelperJbmr2 mOpticalBoundsHelper;

        OpticalBoundsHelperJBMR2Impl() {
            mOpticalBoundsHelper = new OpticalBoundsHelperJbmr2();
        }

        @Override
        public void setOpticalBounds(ViewGroup view) {
            mOpticalBoundsHelper.setOpticalBounds(view);
        }
    }

    /**
     * Returns the OpticalBoundsHelper.
     */
    private OpticalBoundsHelper() {
        if (systemSupportsOpticalBounds()) {
            mImpl = new OpticalBoundsHelperJBMR2Impl();
        } else {
            mImpl = new OpticalBoundsHelperStubImpl();
        }
    }

    public static OpticalBoundsHelper getInstance() {
        return sInstance;
    }

    public void setOpticalBounds(ViewGroup view) {
        mImpl.setOpticalBounds(view);
    }
}
