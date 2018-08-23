/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.car.R;
import androidx.core.view.MarginLayoutParamsCompat;

/**
 * A toolbar for building car applications.
 *
 * <p>CarToolbar provides a subset of features of {@link Toolbar} through a driving safe UI. From
 * start to end, a CarToolbar provides the following elements:
 * <ul>
 *     <li><em>A navigation button.</em> Similar to that in Toolbar, navigation button should always
 *     provide access to other navigational destinations. If navigation button is to be used as
 *     Up Button, its <code>OnClickListener</code> needs to explicitly invoke
 *     {@link AppCompatActivity#onSupportNavigateUp()}
 *     <li><em>A title.</em> A single line text that ellipsizes at end.
 * </ul>
 *
 * <p>One distinction between CarToolbar and Toolbar is that CarToolbar cannot be used as action bar
 * through {@link androidx.appcompat.app.AppCompatActivity#setSupportActionBar(Toolbar)}.
 *
 * <p>The CarToolbar has a fixed height of {@code R.dimen.car_app_bar_height}.
 */
public class CarToolbar extends ViewGroup {

    private static final String TAG = "Toolbar";

    private final ImageButton mNavButtonView;
    private final int mNavButtonIconSize;
    private final int mToolbarHeight;
    // There is no actual container for nav button. This value is used to calculate a horizontal
    // space on both ends of nav button (so it's centered).
    // We use dedicated attribute over horizontal margin so that the API for setting space before
    // title (i.e. @dimen/car_margin) is simpler.
    private int mNavButtonContainerWidth;

    private final TextView mTitleTextView;
    private CharSequence mTitleText;

    public CarToolbar(Context context) {
        this(context, /* attrs= */ null);
    }

    public CarToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.carToolbarStyle);
    }

    public CarToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, /* defStyleRes= */ 0);
    }

    public CarToolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        Resources res = context.getResources();
        mToolbarHeight = res.getDimensionPixelSize(R.dimen.car_app_bar_height);
        mNavButtonIconSize = res.getDimensionPixelSize(R.dimen.car_primary_icon_size);

        LayoutInflater.from(context).inflate(R.layout.car_toolbar, this);

        // Ensure min touch target size for nav button.
        mNavButtonView = findViewById(R.id.nav_button);
        int minTouchSize = getContext().getResources().getDimensionPixelSize(
                R.dimen.car_touch_target_size);
        MinTouchTargetHelper.ensureThat(mNavButtonView).hasMinTouchSize(minTouchSize);

        mTitleTextView = findViewById(R.id.title);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CarToolbar, defStyleAttr,
                /* defStyleRes= */ 0);
        try {
            CharSequence title = a.getText(R.styleable.CarToolbar_title);
            setTitle(title);

            setTitleTextAppearance(a.getResourceId(R.styleable.CarToolbar_titleTextAppearance,
                    R.style.TextAppearance_Car_Body1_Medium));

            setNavigationIcon(a.getResourceId(R.styleable.CarToolbar_navigationIcon,
                    R.drawable.ic_nav_arrow_back));

            mNavButtonContainerWidth = a.getDimensionPixelSize(
                    R.styleable.CarToolbar_navigationIconContainerWidth,
                    res.getDimensionPixelSize(R.dimen.car_margin));
        } finally {
            a.recycle();
        }
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        // Car Toolbar uses fixed height.
        return mToolbarHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Desired height should be the height constraint for all child views.
        int desiredHeight = getPaddingTop() + getSuggestedMinimumHeight() + getPaddingBottom();
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                desiredHeight, MeasureSpec.AT_MOST);

        int width = 0;
        if (mNavButtonView.getVisibility() != View.GONE) {
            // Size of nav button is fixed.
            int measureSpec = MeasureSpec.makeMeasureSpec(mNavButtonIconSize, MeasureSpec.EXACTLY);
            mNavButtonView.measure(measureSpec, measureSpec);

            // Nav button width includes its container.
            int navWidth = Math.max(mNavButtonContainerWidth, mNavButtonView.getMeasuredWidth());
            width += navWidth + getHorizontalMargins(mNavButtonView);
        }
        if (mTitleTextView.getVisibility() != View.GONE) {
            measureChild(mTitleTextView, widthMeasureSpec, width, childHeightMeasureSpec, 0);
            width += mTitleTextView.getMeasuredWidth() + getHorizontalMargins(mTitleTextView);
        }

        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int height = bottom - top;
        int layoutLeft = getPaddingLeft();

        if (mNavButtonView.getVisibility() != View.GONE) {
            // Nav button is centered in container.
            int navButtonWidth = mNavButtonView.getMeasuredWidth();
            int containerWidth = Math.max(mNavButtonContainerWidth, navButtonWidth);
            int navButtonLeft = (containerWidth - navButtonWidth) / 2;

            layoutViewVerticallyCentered(mNavButtonView, navButtonLeft, height);
            layoutLeft += containerWidth;
        }

        if (mTitleTextView.getVisibility() != View.GONE) {
            layoutViewVerticallyCentered(mTitleTextView, layoutLeft, height);
        }
    }

    /**
     * Set the icon to use for the toolbar's navigation button.
     *
     * <p>The navigation button appears at the start of the toolbar if present. Setting an icon
     * will make the navigation button visible.
     *
     * @param resId Resource ID of drawable to set.
     *
     * @attr ref R.styleable#CarToolbar_navigationIcon
     */
    public void setNavigationIcon(@DrawableRes int resId) {
        setNavigationIcon(Icon.createWithResource(getContext(), resId));
    }

    /**
     * Set the icon to use for the toolbar's navigation button.
     *
     * <p>The navigation button appears at the start of the toolbar if present. Setting an icon
     * will make the navigation button visible.
     *
     * @param icon Icon to set; {@code null} will hide the icon.
     *
     * @attr ref R.styleable#CarToolbar_navigationIcon
     */
    public void setNavigationIcon(@Nullable Icon icon) {
        if (icon == null) {
            mNavButtonView.setVisibility(View.GONE);
            mNavButtonView.setImageDrawable(null);
            return;
        }
        mNavButtonView.setVisibility(View.VISIBLE);
        mNavButtonView.setImageDrawable(icon.loadDrawable(getContext()));
    }

    /**
     * Set a listener to respond to navigation events.
     *
     * <p>This listener will be called whenever the user clicks the navigation button
     * at the start of the toolbar. An icon must be set for the navigation button to appear.
     *
     * @param listener Listener to set.
     * @see #setNavigationIcon(Icon)
     */
    public void setNavigationIconOnClickListener(@Nullable View.OnClickListener listener) {
        mNavButtonView.setOnClickListener(listener);
    }

    /**
     * Set the width of container for navigation icon.
     *
     * <p>Navigation icon will be horizontally centered in its container. If the width of container
     * is less than that of navigation icon, there will be no space on both ends of navigation icon.
     *
     * @param width Width of container in pixels.
     */
    public void setNavigationIconContainerWidth(@Px int width) {
        mNavButtonContainerWidth = width;
        requestLayout();
    }

    /**
     * Returns the title of this toolbar.
     *
     * @return The current title.
     */
    public CharSequence getTitle() {
        return mTitleText;
    }

    /**
     * Set the title of this toolbar.
     *
     * <p>A title should be used as the anchor for a section of content. It should
     * describe or name the content being viewed.
     *
     * @param resId Resource ID of a string to set as the title.
     */
    public void setTitle(@StringRes int resId) {
        setTitle(getContext().getText(resId));
    }

    /**
     * Set the title of this toolbar.
     *
     * <p>A title should be used as the anchor for a section of content. It should
     * describe or name the content being viewed.
     *
     * <p>{@code null} or empty string will hide the title.
     *
     * @param title Title to set.
     */
    public void setTitle(CharSequence title) {
        mTitleText = title;
        mTitleTextView.setText(title);
        mTitleTextView.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);
    }

    /**
     * Sets the text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     *
     * @param resId Resource id of TextAppearance.
     */
    public void setTitleTextAppearance(@StyleRes int resId) {
        mTitleTextView.setTextAppearance(resId);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    private void layoutViewVerticallyCentered(View view, int left, int height) {
        int viewHeight = view.getMeasuredHeight();
        int viewWidth = view.getMeasuredWidth();
        int viewTop = (height - viewHeight) / 2;
        view.layout(left, viewTop, left + viewWidth, viewTop + viewHeight);
    }

    private int getHorizontalMargins(View v) {
        MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        return MarginLayoutParamsCompat.getMarginStart(mlp)
                + MarginLayoutParamsCompat.getMarginEnd(mlp);
    }

    /**
     * Measure child view.
     *
     * @param child Child view to measure.
     * @param parentWidthSpec Parent width MeasureSpec.
     * @param widthUsed Width used so far by other child views; used as part of padding
     *                  for current child view in MeasureSpec calculation.
     * @param parentHeightSpec Parent height MeasureSpec.
     * @param heightUsed Height used so far by other child views; used as part of padding
     *                   for current child view in MeasureSpec calculation.
     */
    private void measureChild(View child, int parentWidthSpec, int widthUsed,
            int parentHeightSpec, int heightUsed) {
        // Calculate the padding and margin of current dimension, including
        // the width/height used by other child views.
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int childWidthSpec = getChildMeasureSpec(parentWidthSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed,
                lp.width);
        int childHeightSpec = getChildMeasureSpec(parentHeightSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed,
                lp.height);
        child.measure(childWidthSpec, childHeightSpec);
    }
}
