package android.support.v17.leanback.widget;

import android.content.Context;
import android.util.AttributeSet;

class HorizontalGridViewEx extends HorizontalGridView {

    public int mSmoothScrollByCalled;

    public HorizontalGridViewEx(Context context) {
        super(context);
    }

    public HorizontalGridViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalGridViewEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void smoothScrollBy(int dx, int dy) {
        mSmoothScrollByCalled++;
        super.smoothScrollBy(dx, dy);
    }
}