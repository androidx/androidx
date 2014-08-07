package android.support.v7.internal.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

class ActionBarBackgroundDrawable extends Drawable {

    private final ActionBarContainer mContainer;

    public ActionBarBackgroundDrawable(ActionBarContainer container) {
        mContainer = container;
    }

    @Override
    public void draw(Canvas canvas) {
        final Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.draw(canvas);
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    protected final Drawable getDrawable() {
        if (mContainer.mIsSplit) {
            if (mContainer.mSplitBackground != null) {
                return mContainer.mSplitBackground;
            }
        } else {
            if (mContainer.mBackground != null) {
                return mContainer.mBackground;
            }
            if (mContainer.mStackedBackground != null && mContainer.mIsStacked) {
                return mContainer.mStackedBackground;
            }
        }
        return null;
    }
}
