package android.support.v17.leanback.widget;

import android.content.Context;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/*
 * Used internally by ListRowPresenter to provide shadow and color dim overlay.
 */
final class ListRowCardWrapper extends FrameLayout {

    boolean mInitialized;
    View mShadowNormal;
    View mShadowFocused;
    View mColorDimOverlay;

    ListRowCardWrapper(Context context) {
        this(context, null, 0);
    }

    ListRowCardWrapper(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    ListRowCardWrapper(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void initialize(boolean hasShadow, boolean hasColorDimOverlay) {
        if (mInitialized) {
            throw new IllegalStateException();
        }
        mInitialized = true;
        if (hasShadow) {
            OpticalBoundsHelper.getInstance().setOpticalBounds(this);
            mShadowNormal = LayoutInflater.from(getContext())
                    .inflate(R.layout.lb_shadow_normal, this, false);
            addView(mShadowNormal);
            mShadowFocused = LayoutInflater.from(getContext())
                    .inflate(R.layout.lb_shadow_focused, this, false);
            addView(mShadowFocused);
        }
        if (hasColorDimOverlay) {
            mColorDimOverlay = LayoutInflater.from(getContext())
                    .inflate(R.layout.lb_card_color_overlay, this, false);
            addView(mColorDimOverlay);
        }
    }

    void wrap(View view) {
        if (!mInitialized) {
            throw new IllegalStateException();
        }
        if (mColorDimOverlay != null) {
            addView(view, indexOfChild(mColorDimOverlay));
        } else {
            addView(view);
        }
    }

}
