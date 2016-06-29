package android.support.v17.leanback.widget;

import android.content.Context;
import android.util.AttributeSet;

class VerticalGridViewEx extends VerticalGridView {

    public int mSmoothScrollByCalled;

    public VerticalGridViewEx(Context context) {
        super(context);
    }

    public VerticalGridViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalGridViewEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void smoothScrollBy(int dx, int dy) {
        mSmoothScrollByCalled++;
        super.smoothScrollBy(dx, dy);
    }
}