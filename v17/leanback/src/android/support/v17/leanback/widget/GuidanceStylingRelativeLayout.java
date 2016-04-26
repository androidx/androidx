package android.support.v17.leanback.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Relative layout implementation that lays out child views based on provided keyline percent(
 * distance of TitleView baseline from the top).
 *
 * Repositioning child views in PreDraw callback in {@link GuidanceStylist} was interfering with
 * fragment transition. To avoid that, we do that in the onLayout pass.
 *
 * @hide
 */
class GuidanceStylingRelativeLayout extends RelativeLayout {
    private float mTitleKeylinePercent;

    public GuidanceStylingRelativeLayout(Context context) {
        this(context, null);
    }

    public GuidanceStylingRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GuidanceStylingRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        TypedArray ta = getContext().getTheme().obtainStyledAttributes(
                R.styleable.LeanbackGuidedStepTheme);
        mTitleKeylinePercent = ta.getFloat(R.styleable.LeanbackGuidedStepTheme_guidedStepKeyline,
                40);
        ta.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        TextView mTitleView = (TextView) getRootView().findViewById(R.id.guidance_title);
        TextView mBreadcrumbView = (TextView) getRootView().findViewById(R.id.guidance_breadcrumb);
        TextView mDescriptionView = (TextView) getRootView().findViewById(
                R.id.guidance_description);
        ImageView mIconView = (ImageView) getRootView().findViewById(R.id.guidance_icon);
        int mTitleKeylinePixels = (int) (getMeasuredHeight() * mTitleKeylinePercent / 100);

        if (mTitleView != null && mTitleView.getParent() == this) {
            Paint textPaint = mTitleView.getPaint();
            int titleViewTextHeight = -textPaint.getFontMetricsInt().top;
            int mBreadcrumbViewHeight = mBreadcrumbView.getMeasuredHeight();
            int guidanceTextContainerTop = mTitleKeylinePixels
                    - titleViewTextHeight - mBreadcrumbViewHeight - mTitleView.getPaddingTop();
            int offset = guidanceTextContainerTop - mBreadcrumbView.getTop();

            if (mBreadcrumbView != null && mBreadcrumbView.getParent() == this) {
                mBreadcrumbView.offsetTopAndBottom(offset);
            }

            mTitleView.offsetTopAndBottom(offset);

            if (mDescriptionView != null && mDescriptionView.getParent() == this) {
                mDescriptionView.offsetTopAndBottom(offset);
            }
        }

        if (mIconView != null && mIconView.getParent() == this) {
            Drawable drawable = mIconView.getDrawable();
            if (drawable != null) {
                mIconView.offsetTopAndBottom(
                        mTitleKeylinePixels - mIconView.getMeasuredHeight() / 2);
            }
        }
    }
}
