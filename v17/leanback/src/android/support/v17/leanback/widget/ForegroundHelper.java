package android.support.v17.leanback.widget;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

final class ForegroundHelper {

    final static ForegroundHelper sInstance = new ForegroundHelper();
    ForegroundHelperVersionImpl mImpl;

    /**
     * Interface implemented by classes that support Shadow.
     */
    static interface ForegroundHelperVersionImpl {

        public void setForeground(View view, Drawable drawable);

        public Drawable getForeground(View view);
    }

    /**
     * Implementation used on api 23 (and above).
     */
    private static final class ForegroundHelperApi23Impl implements ForegroundHelperVersionImpl {
        @Override
        public void setForeground(View view, Drawable drawable) {
            ForegroundHelperApi23.setForeground(view, drawable);
        }

        @Override
        public Drawable getForeground(View view) {
            return ForegroundHelperApi23.getForeground(view);
        }
    }

    /**
     * Stub implementation
     */
    private static final class ForegroundHelperStubImpl implements ForegroundHelperVersionImpl {
        @Override
        public void setForeground(View view, Drawable drawable) {
        }

        @Override
        public Drawable getForeground(View view) {
            return null;
        }
    }

    private ForegroundHelper() {
        if (supportsForeground()) {
            mImpl = new ForegroundHelperApi23Impl();
        } else {
            mImpl = new ForegroundHelperStubImpl();
        }
    }

    public static ForegroundHelper getInstance() {
        return sInstance;
    }

    /**
     * Returns true if view.setForeground() is supported.
     */
    public static boolean supportsForeground() {
        return Build.VERSION.SDK_INT >= 23;
    }

    public Drawable getForeground(View view) {
        return mImpl.getForeground(view);
    }

    public void setForeground(View view, Drawable drawable) {
        mImpl.setForeground(view, drawable);
    }
}
