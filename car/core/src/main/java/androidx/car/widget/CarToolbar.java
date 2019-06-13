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
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.car.R;
import androidx.car.app.CarListDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.MarginLayoutParamsCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * A toolbar for building car applications.
 *
 * <p>CarToolbar provides a subset of features of {@link Toolbar} through a driving safe UI. From
 * start to end, a CarToolbar provides the following elements:
 * <ul>
 * <li><em>A navigation button.</em> Similar to that in Toolbar, navigation button should
 * always provide access to other navigational destinations. If navigation button is to
 * be used as Up Button, its {@code OnClickListener} needs to explicitly invoke
 * {@link AppCompatActivity#onSupportNavigateUp()}
 * <li><em>A title icon.</em> A @{@code Drawable} shown before the title.
 * <li><em>A title.</em> A single line primary text that ellipsizes at the end.
 * <li><em>A subtitle.</em> A single line secondary text that ellipsizes at the end.
 * <li><em>An overflow button.</em> A button that opens the overflow menu.
 * </ul>
 *
 * <p>{@link CarMenuItem} in overflow menu will be shown as a {@link CarListDialog}. Overflow menu
 * items with icons are not supported yet, i.e. only texts from {@link CarMenuItem#getTitle()} will
 * be displayed.
 *
 * <p>One distinction between CarToolbar and Toolbar is that CarToolbar cannot be used as action bar
 * through {@link androidx.appcompat.app.AppCompatActivity#setSupportActionBar(Toolbar)}.
 *
 * <p>The CarToolbar has a fixed height of {@code R.dimen.car_app_bar_height}.
 */
public class CarToolbar extends ViewGroup {

    // Max number of Action items displayed on the toolbar, only applies to IF_ROOM items.
    @VisibleForTesting
    static final int ACTION_ITEM_COUNT_LIMIT = 4;

    private static final String TAG = "CarToolbar";

    private final ImageButton mNavButtonView;
    private final int mEdgeButtonIconSize;
    private final ImageView mTitleIconView;
    private final ImageButton mOverflowButtonView;
    private final int mToolbarHeight;
    private final int mTextVerticalPadding;
    private final int mActionButtonPadding;
    private final int mActionButtonIconBound;
    private final int mActionButtonHeight;
    private int mTitleIconSize;
    // There is no actual container for edge buttons (Navigation / Overflow). This value is used
    // to calculate a horizontal margin on both ends of the edge buttons so that they're centered.
    // We use dedicated attribute over horizontal margin so that the API for setting space before
    // title (i.e. @dimen/car_margin) is simpler.
    private int mEdgeButtonContainerWidth;

    private final TextView mTitleTextView;
    private CharSequence mTitleText;

    private final TextView mSubtitleTextView;
    private CharSequence mSubtitleText;

    @Nullable
    private List<CarMenuItem> mMenuItems;
    @Nullable
    private CarListDialog mOverflowDialog;
    private final List<InflatedMenuItem> mAllMenuItems = new ArrayList<>();
    private final List<CarMenuItem> mOverflowMenuItems = new ArrayList<>();
    private final List<View> mToolbarItems = new ArrayList<>();
    // Number of items that are always displayed on the toolbar.
    private int mAlwaysItemCount;

    // Minimum width required to show a menu item as action item on the toolbar.
    // This var is used as threshold for showing if_room items, therefore its value is subject
    // to change - DO NOT depend on it.
    private final int mMinActionButtonWidth;

    /**
     * OnClickListener that handles the overflow dialog clicks by calling the appropriate
     * {@link CarMenuItem.OnClickListener} of the overflow {@link CarMenuItem}s.
     */
    private DialogInterface.OnClickListener mOverflowDialogClickListener = (dialog, which) -> {
        CarMenuItem item = mOverflowMenuItems.get(which);
        if (item.getOnClickListener() != null) {
            item.getOnClickListener().onClick(item);
        }
    };


    public CarToolbar(Context context) {
        this(context, /* attrs= */ null);
    }

    public CarToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.carToolbarStyle);
    }

    public CarToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_Car_CarToolbar);
    }

    public CarToolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        Resources res = context.getResources();
        mToolbarHeight = res.getDimensionPixelSize(R.dimen.car_app_bar_height);
        mEdgeButtonIconSize = res.getDimensionPixelSize(R.dimen.car_primary_icon_size);
        mTextVerticalPadding = res.getDimensionPixelSize(R.dimen.car_padding_1);
        mActionButtonPadding = res.getDimensionPixelSize(R.dimen.car_padding_2);
        mActionButtonIconBound = res.getDimensionPixelSize(R.dimen.car_app_bar_action_icon_bound);
        mActionButtonHeight = res.getDimensionPixelSize(R.dimen.car_button_height);
        LayoutInflater.from(context).inflate(R.layout.car_toolbar, this);

        // Ensure min touch target size for nav button.
        mNavButtonView = findViewById(R.id.nav_button);
        int minTouchSize = getContext().getResources().getDimensionPixelSize(
                R.dimen.car_touch_target_size);
        MinTouchTargetHelper.ensureThat(mNavButtonView).hasMinTouchSize(minTouchSize);

        mTitleTextView = findViewById(R.id.title);
        mTitleIconView = findViewById(R.id.title_icon);
        mSubtitleTextView = findViewById(R.id.subtitle);
        mOverflowButtonView = findViewById(R.id.overflow_menu);

        mMinActionButtonWidth = res.getDimensionPixelOffset(R.dimen.car_button_min_width);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CarToolbar, defStyleAttr,
                /* defStyleRes= */ 0);
        try {
            CharSequence title = a.getText(R.styleable.CarToolbar_title);
            setTitle(title);

            setTitleTextAppearance(a.getResourceId(R.styleable.CarToolbar_titleTextAppearance,
                    R.style.TextAppearance_Car_Body1_Medium_Light));

            setNavigationIcon(a.getResourceId(R.styleable.CarToolbar_navigationIcon,
                    R.drawable.ic_nav_arrow_back));

            int navigationIconTintResId =
                    a.getResourceId(R.styleable.CarToolbar_navigationIconTint, -1);
            if (navigationIconTintResId != -1) {
                setNavigationIconTint(ContextCompat.getColor(context, navigationIconTintResId));
            }

            int titleIconResId = a.getResourceId(R.styleable.CarToolbar_titleIcon, -1);
            setTitleIcon(titleIconResId != -1
                    ? context.getDrawable(titleIconResId)
                    : null);

            setTitleIconStartMargin(
                    a.getDimensionPixelSize(R.styleable.CarToolbar_titleIconStartMargin, 0));

            setTitleIconEndMargin(
                    a.getDimensionPixelSize(R.styleable.CarToolbar_titleIconEndMargin, 0));

            setTitleIconSize(a.getDimensionPixelSize(R.styleable.CarToolbar_titleIconSize,
                    res.getDimensionPixelSize(R.dimen.car_application_icon_size)));

            CharSequence subtitle = a.getText(R.styleable.CarToolbar_subtitle);
            setSubtitle(subtitle);

            setSubtitleTextAppearance(a.getResourceId(R.styleable.CarToolbar_subtitleTextAppearance,
                    R.style.TextAppearance_Car_Body2_Light));

            setOverflowIcon(a.getResourceId(R.styleable.CarToolbar_overflowIcon,
                    R.drawable.ic_more_vert));

            mOverflowButtonView.setOnClickListener(v -> {
                populateOverflowMenu();
                mOverflowDialog.show();
            });

            mEdgeButtonContainerWidth = a.getDimensionPixelSize(
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
        // Measure the two edge buttons first because they have a higher
        // display priority than the title, subtitle, or the titleIcon.
        if (mNavButtonView.getVisibility() != GONE) {
            // Size of nav button is fixed.
            int measureSpec = MeasureSpec.makeMeasureSpec(mEdgeButtonIconSize, MeasureSpec.EXACTLY);
            mNavButtonView.measure(measureSpec, measureSpec);

            // Nav button width includes its container.
            int navWidth = Math.max(mEdgeButtonContainerWidth, mNavButtonView.getMeasuredWidth());
            width += navWidth + getHorizontalMargins(mNavButtonView);
        }

        mToolbarItems.forEach(this::removeView);
        mToolbarItems.clear();
        mOverflowMenuItems.clear();

        // Measure items that will always be shown on the toolbar.
        int alwaysItemsWidth = measureAlwaysItems(width, widthMeasureSpec, childHeightMeasureSpec);
        width += alwaysItemsWidth;

        // Decide how many IF_ROOM items to show, and measure them.
        width += measureIfRoomItems(width, alwaysItemsWidth, widthMeasureSpec,
                childHeightMeasureSpec);

        // Split the items appropriately based on the display location.
        // Done post measurement to ensure the order in the two lists matches the passed-in items.
        for (InflatedMenuItem inflatedItem : mAllMenuItems) {
            if (inflatedItem.mIsDisplayedOnToolbar) {
                mToolbarItems.add(inflatedItem.mView);
            } else {
                mOverflowMenuItems.add(inflatedItem.mItem);
            }
        }

        // Show the overflow menu button if there are any overflow menu items.
        mOverflowButtonView.setVisibility(mOverflowMenuItems.isEmpty() ? GONE : VISIBLE);

        if (mOverflowButtonView.getVisibility() != GONE) {
            int measureSpec = MeasureSpec.makeMeasureSpec(mEdgeButtonIconSize, MeasureSpec.EXACTLY);
            mOverflowButtonView.measure(measureSpec, measureSpec);
            width += Math.max(mEdgeButtonContainerWidth, mOverflowButtonView.getMeasuredWidth())
                    + getHorizontalMargins(mOverflowButtonView);
        }

        if (mTitleIconView.getVisibility() != GONE) {
            int measureSpec = MeasureSpec.makeMeasureSpec(mTitleIconSize, MeasureSpec.EXACTLY);
            mTitleIconView.measure(measureSpec, measureSpec);
            width += mTitleIconView.getMeasuredWidth() + getHorizontalMargins(mTitleIconView);
        }

        int titleLength = 0;
        int subtitleLength = 0;
        if (mTitleTextView.getVisibility() != GONE) {
            measureChild(mTitleTextView, widthMeasureSpec, width, childHeightMeasureSpec, 0);
            titleLength = mTitleTextView.getMeasuredWidth() + getHorizontalMargins(mTitleTextView);
        }
        if (mSubtitleTextView.getVisibility() != GONE) {
            measureChild(mSubtitleTextView, widthMeasureSpec, width, childHeightMeasureSpec, 0);
            subtitleLength = mSubtitleTextView.getMeasuredWidth()
                    + getHorizontalMargins(mSubtitleTextView);
        }
        width += Math.max(titleLength, subtitleLength);

        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    /**
     * Measures ALWAYS menu items and adds them to the layout.
     *
     * @param widthUsed         Total width used by other child views so far.
     * @param widthMeasureSpec  Parent width measure spec.
     * @param heightMeasureSpec Parent height measure spec.
     * @return Total width occupied by ALWAYS items.
     */
    private int measureAlwaysItems(int widthUsed, int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        for (InflatedMenuItem inflatedItem : mAllMenuItems) {
            if (inflatedItem.mItem.getDisplayBehavior() == CarMenuItem.DisplayBehavior.ALWAYS) {
                View action = inflatedItem.mView;
                measureChild(action, widthMeasureSpec, widthUsed + width, heightMeasureSpec, 0);
                inflatedItem.mIsDisplayedOnToolbar = true;
                addView(action);

                width += action.getMeasuredWidth() + getHorizontalMargins(action);
            }
        }
        return width;
    }

    /**
     * Measures IF_ROOM menu items and adds them to the layout. Items past
     * {@link #ACTION_ITEM_COUNT_LIMIT} or half the toolbar width will not be measured/added.
     *
     * @param widthUsed         Total width used by other child views so far.
     * @param alwaysItemsWidth  Total width used by ALWAYS item views.
     * @param widthMeasureSpec  Parent width measure spec.
     * @param heightMeasureSpec Parent height measure spec.
     * @return Total width occupied by IF_ROOM items.
     */
    private int measureIfRoomItems(int widthUsed, int alwaysItemsWidth,
            int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;

        // If mode is unspecified, all IF_ROOM items should be measured.
        boolean isWidthUnspecified =
                MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED;
        // If specified, available width for IF_ROOM items. (50% of toolbar)
        int widthCapacity = (MeasureSpec.getSize(widthMeasureSpec) / 2) - alwaysItemsWidth;
        // Count of IF_ROOM items to be displayed on the toolbar.
        int allowedCount = ACTION_ITEM_COUNT_LIMIT - mAlwaysItemCount;

        for (InflatedMenuItem inflatedItem : mAllMenuItems) {
            if (allowedCount < 1
                    || (!isWidthUnspecified && widthCapacity <= mMinActionButtonWidth)) {
                // Cannot show more IF_ROOM items because either max count is reached, or remaining
                // width is not sufficient.
                break;
            }
            if (inflatedItem.mItem.getDisplayBehavior() == CarMenuItem.DisplayBehavior.IF_ROOM) {
                View action = inflatedItem.mView;
                measureChild(action, widthMeasureSpec, widthUsed + width, heightMeasureSpec, 0);
                int viewWidth = action.getMeasuredWidth() + getHorizontalMargins(action);
                if (isWidthUnspecified || widthCapacity - viewWidth > 0) {
                    allowedCount--;
                    widthCapacity -= viewWidth;
                    width += viewWidth;
                    inflatedItem.mIsDisplayedOnToolbar = true;
                    addView(action);
                }
            }
        }
        return width;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int height = bottom - top;
        int layoutLeft = getPaddingLeft();
        int layoutRight = getPaddingRight();

        if (mNavButtonView.getVisibility() != GONE) {
            // Nav button is centered in container.
            int navButtonWidth = mNavButtonView.getMeasuredWidth();
            int containerWidth = Math.max(mEdgeButtonContainerWidth, navButtonWidth);
            int navButtonLeft = (containerWidth - navButtonWidth) / 2;

            layoutViewFromLeftVerticallyCentered(mNavButtonView, navButtonLeft, height);
            layoutLeft += containerWidth;
        }

        if (mOverflowButtonView.getVisibility() != GONE) {
            int horizontalMargin = (mEdgeButtonContainerWidth
                    - mOverflowButtonView.getMeasuredWidth()) / 2;
            layoutViewFromRightVerticallyCentered(mOverflowButtonView,
                    right - horizontalMargin, height);
            layoutRight += Math.max(mEdgeButtonContainerWidth,
                    mOverflowButtonView.getMeasuredWidth());
        }

        for (View view : mToolbarItems) {
            layoutViewFromRightVerticallyCentered(view, right - layoutRight, height);
            layoutRight += view.getMeasuredWidth();
        }

        if (mTitleIconView.getVisibility() != GONE) {
            MarginLayoutParams marginParams = (MarginLayoutParams) mTitleIconView.getLayoutParams();
            layoutLeft += marginParams.getMarginStart();
            layoutViewFromLeftVerticallyCentered(mTitleIconView, layoutLeft, height);
            layoutLeft += mTitleIconView.getMeasuredWidth() + marginParams.getMarginEnd();
        }

        if (mTitleTextView.getVisibility() != GONE && mSubtitleTextView.getVisibility() != GONE) {
            layoutTextViewsVerticallyCentered(mTitleTextView, mSubtitleTextView, layoutLeft,
                    height);
        } else if (mTitleTextView.getVisibility() != GONE) {
            layoutViewFromLeftVerticallyCentered(mTitleTextView, layoutLeft, height);
        } else if (mSubtitleTextView.getVisibility() != GONE) {
            layoutViewFromLeftVerticallyCentered(mSubtitleTextView, layoutLeft, height);
        }
    }

    /**
     * Set the icon to use for the toolbar's navigation button.
     *
     * <p>The navigation button appears at the start of the toolbar if present. Setting an icon
     * will make the navigation button visible.
     *
     * @param iconResId The resource id of the icon to set on the navigatino button.
     *
     * {@link R.attr#navigationIcon}
     */
    public void setNavigationIcon(@DrawableRes int iconResId) {
        setNavigationIcon(getContext().getDrawable(iconResId));
    }

    /**
     * Set the icon to use for the toolbar's navigation button.
     *
     * <p>The navigation button appears at the start of the toolbar if present. Setting an icon
     * will make the navigation button visible.
     *
     * @param icon Icon to set; {@code null} will hide the icon.
     *
     * {@link R.attr#navigationIcon}
     */
    public void setNavigationIcon(@Nullable Drawable icon) {
        if (icon == null) {
            mNavButtonView.setVisibility(GONE);
            mNavButtonView.setImageDrawable(null);
            return;
        }
        mNavButtonView.setVisibility(VISIBLE);
        mNavButtonView.setImageDrawable(icon);
    }

    /**
     * Sets the tint color for the navigation icon.
     *
     * @param tint Color tint to apply.
     *
     * {@link R.attr#navigationIconTint}
     */
    public void setNavigationIconTint(@ColorInt int tint) {
        mNavButtonView.setColorFilter(tint);
    }

    /**
     * Sets the given {@link ColorFilter} as the tint for the navigation icon. A {@code null}
     * {@code ColorFilter} will clear any set color filters.
     *
     * @param colorFilter Color filter to apply for the tint.
     *
     * {@link R.attr#navigationIconTint}
     */
    public void setNavigationIconTint(@Nullable ColorFilter colorFilter) {
        mNavButtonView.setColorFilter(colorFilter);
    }

    /**
     * Sets a listener to respond to navigation events.
     *
     * <p>This listener will be called whenever the user clicks the navigation button
     * at the start of the toolbar. An icon must be set for the navigation button to appear.
     *
     * @param listener Listener to set.
     * @see #setNavigationIcon(Drawable)
     */
    public void setNavigationIconOnClickListener(@Nullable View.OnClickListener listener) {
        mNavButtonView.setOnClickListener(listener);
    }

    /**
     * Sets the width of container for navigation icon.
     *
     * <p>Navigation icon will be horizontally centered in its container. If the width of container
     * is less than that of navigation icon, there will be no space on both ends of navigation icon.
     *
     * @param width Width of container in pixels.
     */
    public void setNavigationIconContainerWidth(@Px int width) {
        mEdgeButtonContainerWidth = width;
        requestLayout();
    }

    /**
     * Sets the title icon to use in the toolbar.
     *
     * <p>The title icon is positioned between the navigation button and the title.
     *
     * @param iconResId Resource id of the drawable to use as the title icon.
     * {@link R.attr#titleIcon}
     */
    public void setTitleIcon(@DrawableRes int iconResId) {
        setTitleIcon(getContext().getDrawable(iconResId));
    }

    /**
     * Sets the title icon to use in the toolbar.
     *
     * <p>The title icon is positioned between the navigation button and the title.
     *
     * @param icon Icon to set; {@code null} will hide the icon.
     * {@link R.attr#titleIcon}
     */
    public void setTitleIcon(@Nullable Drawable icon) {
        if (icon == null) {
            mTitleIconView.setVisibility(GONE);
            mTitleIconView.setImageDrawable(null);
            return;
        }
        mTitleIconView.setVisibility(VISIBLE);
        mTitleIconView.setImageDrawable(icon);
    }

    /**
     * Sets the start margin of the title icon.
     *
     * @param margin Start margin of the title icon in pixels.
     * @attr ref R.styleable#CarToolbar_titleIconStartMargin
     */
    public void setTitleIconStartMargin(@Px int margin) {
        MarginLayoutParams marginParams = (MarginLayoutParams) mTitleIconView.getLayoutParams();
        marginParams.setMarginStart(margin);
        requestLayout();
    }

    /**
     * Sets the end margin of the title icon.
     *
     * @param margin End margin of the title icon in pixels.
     * @attr ref R.styleable#CarToolbar_titleIconEndMargin
     */
    public void setTitleIconEndMargin(@Px int margin) {
        MarginLayoutParams marginParams = (MarginLayoutParams) mTitleIconView.getLayoutParams();
        marginParams.setMarginEnd(margin);
        requestLayout();
    }

    /**
     * Sets a new size for the title icon.
     *
     * @param size Size of the title icon dimensions in pixels.
     * {@link R.attr#titleIconSize}
     */
    public void setTitleIconSize(@Px int size) {
        mTitleIconSize = size;
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
     * Sets the title of this toolbar.
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
     * Sets the title of this toolbar.
     *
     * <p>A title should be used as the anchor for a section of content. It should
     * describe or name the content being viewed.
     *
     * <p>{@code null} or empty string will hide the title.
     *
     * @param title Title to set.
     */
    public void setTitle(@Nullable CharSequence title) {
        mTitleText = title;
        mTitleTextView.setText(title);
        mTitleTextView.setVisibility(TextUtils.isEmpty(title) ? GONE : VISIBLE);
    }

    /**
     * Returns the subtitle of this toolbar.
     *
     * @return The current subtitle, or {@code null} if none has been set.
     */
    @Nullable
    public CharSequence getSubtitle() {
        return mSubtitleText;
    }

    /**
     * Sets the subtitle of this toolbar.
     *
     * <p>Subtitles should express extended information about the current content.
     * Subtitle will appear underneath the title if the title exists.
     *
     * @param resId Resource ID of a string to set as the subtitle.
     */
    public void setSubtitle(@StringRes int resId) {
        setSubtitle(getContext().getText(resId));
    }

    /**
     * Sets the subtitle of this toolbar.
     *
     * <p>Subtitle should express extended information about the current content.
     * Subtitle will appear underneath the title if the title exists.
     *
     * @param subtitle Subtitle to set. {@code null} or empty string will hide the subtitle.
     */
    public void setSubtitle(@Nullable CharSequence subtitle) {
        mSubtitleText = subtitle;
        mSubtitleTextView.setText(subtitle);
        mSubtitleTextView.setVisibility(TextUtils.isEmpty(subtitle) ? GONE : VISIBLE);
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

    /**
     * Sets the text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     *
     * @param resId Resource id of TextAppearance.
     */
    public void setSubtitleTextAppearance(@StyleRes int resId) {
        mSubtitleTextView.setTextAppearance(resId);
    }

    /**
     * Sets the list of {@link CarMenuItem}s that will be displayed on this {@code CarToolbar}.
     *
     * @param items List of {@link CarMenuItem}s to display, {@code null} to remove all items.
     */
    public void setMenuItems(@Nullable List<CarMenuItem> items) {
        mMenuItems = items;

        mAllMenuItems.clear();
        mAlwaysItemCount = 0;

        if (mMenuItems == null) {
            requestLayout();
            return;
        }

        // Create Views for all ALWAYS and IF_ROOM items.
        for (CarMenuItem item : mMenuItems) {
            View action;
            switch (item.getDisplayBehavior()) {
                case ALWAYS:
                    mAlwaysItemCount++;
                    // Fall-through
                case IF_ROOM:
                    action = item.isCheckable() ? createCheckableAction(item) : createAction(item);
                    break;
                case NEVER:
                    action = null;
                    break;
                default:
                    throw new IllegalStateException(
                            "Unknown display behavior: " + item.getDisplayBehavior());
            }
            mAllMenuItems.add(new InflatedMenuItem(item, action));
        }
        requestLayout();
    }

    /**
     * Returns a list of this {@code CarToolbar}'s {@link CarMenuItem}s, or
     * {@code null} if none were set.
     */
    @Nullable
    public List<CarMenuItem> getMenuItems() {
        return mMenuItems;
    }

    /**
     * Creates an Action {@link Button} item configured for the given {@link CarMenuItem}.
     *
     * @param item The {@link CarMenuItem} used to create the {@link Button}.
     * @return A configured {@link Button} view.
     */
    private Button createAction(CarMenuItem item) {
        Context context = getContext();
        Button button = new Button(context, null, 0, item.getStyleResId());
        button.setLayoutParams(
                new MarginLayoutParams(LayoutParams.WRAP_CONTENT, mActionButtonHeight));
        CharSequence title = item.getTitle();
        button.setText(title);

        if (item.getIcon() != null) {
            Drawable icon = item.getIcon();
            icon.setBounds(0, 0, mActionButtonIconBound, mActionButtonIconBound);
            // Set the Drawable on the left side.
            button.setCompoundDrawables(icon, null, null, null);
            if (!TextUtils.isEmpty(title)) {
                // Add padding after the icon only if there's a title.
                button.setCompoundDrawablePadding(mActionButtonPadding);
            }
        }

        button.setEnabled(item.isEnabled());
        button.setOnClickListener(v -> {
            CarMenuItem.OnClickListener onClickListener = item.getOnClickListener();
            if (onClickListener != null) {
                onClickListener.onClick(item);
            }
        });
        return button;
    }

    /**
     * Creates an Action {@link Switch} item configured for the given {@link CarMenuItem}.
     *
     * @param item The checkable {@link CarMenuItem} used to create the {@link Switch}.
     * @return A configured {@link Switch} view.
     */
    private View createCheckableAction(CarMenuItem item) {
        Context context = getContext();
        ViewGroup checkableAction = (ViewGroup) LayoutInflater.from(context)
                .inflate(R.layout.checkable_action_item, this, false);
        Switch switchWidget = checkableAction.findViewById(R.id.switch_widget);
        switchWidget.setEnabled(item.isEnabled());
        switchWidget.setChecked(item.isChecked());

        if (item.isEnabled()) {
            checkableAction.setOnClickListener(v -> {
                switchWidget.toggle();
                item.setChecked(switchWidget.isChecked());
                CarMenuItem.OnClickListener itemOnClickListener = item.getOnClickListener();
                if (itemOnClickListener != null) {
                    itemOnClickListener.onClick(item);
                }
            });
        } else {
            checkableAction.setClickable(false);
        }

        CharSequence title = item.getTitle();
        if (!TextUtils.isEmpty(title)) {
            Button button = new Button(context, null, 0, item.getStyleResId());
            // The button is added programmatically so that we can apply a custom style.
            button.setText(title);

            checkableAction.addView(button);
        }
        return checkableAction;
    }

    /**
     * Adds the overflow items to the overflow menu dialog.
     */
    private void populateOverflowMenu() {
        if (mOverflowMenuItems.isEmpty()) {
            mOverflowDialog = null;
            return;
        }

        CharSequence[] titles = mOverflowMenuItems.stream()
                .map(CarMenuItem::getTitle)
                .toArray(CharSequence[]::new);

        mOverflowDialog = new CarListDialog.Builder(getContext())
                .setItems(titles, mOverflowDialogClickListener)
                .create();
    }

    /**
     * Sets the icon of the overflow menu button.
     *
     * @param iconResId Resource id of the drawable to use for the overflow menu button.
     * @attr ref R.styleable#CarToolbar_overflowIcon
     */
    public void setOverflowIcon(@DrawableRes int iconResId) {
        mOverflowButtonView.setImageDrawable(getContext().getDrawable(iconResId));
    }

    /**
     * Sets the icon of the overflow menu button.
     *
     * @param icon Icon to set.
     * @attr ref R.styleable#CarToolbar_overflowIcon
     */
    public void setOverflowIcon(@NonNull Drawable icon) {
        if (icon == null) {
            throw new IllegalArgumentException("Provided overflow icon cannot be null.");
        }
        mOverflowButtonView.setImageDrawable(icon);
    }

    /**
     * Returns {@code true} if the overflow menu is showing.
     */
    public boolean isOverflowMenuShowing() {
        return mOverflowDialog != null && mOverflowDialog.isShowing();
    }

    /**
     * Sets whether the overflow menu is shown.
     *
     * @param show {code true} to show the overflow menu or {@code false} to hide it.
     */
    public void setOverflowMenuShown(boolean show) {
        if (show) {
            populateOverflowMenu();
            if (mOverflowDialog != null) {
                mOverflowDialog.show();
            }
        } else if (mOverflowDialog != null) {
            mOverflowDialog.dismiss();
        }
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

    /**
     * Lays out a view on the left side so that it's vertically centered in its parent.
     *
     * @param view         The view to layout.
     * @param left         Position from the left.
     * @param parentHeight Height of the parent view.
     */
    private void layoutViewFromLeftVerticallyCentered(View view, int left, int parentHeight) {
        int height = view.getMeasuredHeight();
        int top = (parentHeight - height) / 2;

        view.layout(left, top, left + view.getMeasuredWidth(), top + height);
    }

    /**
     * Lays out a view on the right side so that it's vertically centered in its parent.
     *
     * @param view         The view to layout.
     * @param right        Position from the right.
     * @param parentHeight Height of the parent view.
     */
    private void layoutViewFromRightVerticallyCentered(View view, int right, int parentHeight) {
        int height = view.getMeasuredHeight();
        int top = (parentHeight - height) / 2;

        view.layout(right - view.getMeasuredWidth(), top, right, top + height);
    }

    private void layoutTextViewsVerticallyCentered(View title, View subtitle, int left,
            int height) {
        int titleHeight = title.getMeasuredHeight();
        int titleWidth = title.getMeasuredWidth();

        int subtitleHeight = subtitle.getMeasuredHeight();
        int subtitleWidth = subtitle.getMeasuredWidth();

        int titleTop = (height - titleHeight - subtitleHeight - mTextVerticalPadding) / 2;
        title.layout(left, titleTop, left + titleWidth, titleTop + titleHeight);

        int subtitleTop = title.getBottom() + mTextVerticalPadding;
        subtitle.layout(left, subtitleTop, left + subtitleWidth, subtitleTop + subtitleHeight);
    }

    private int getHorizontalMargins(View v) {
        MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        return MarginLayoutParamsCompat.getMarginStart(mlp)
                + MarginLayoutParamsCompat.getMarginEnd(mlp);
    }

    /**
     * Measure child view.
     *
     * @param child            Child view to measure.
     * @param parentWidthSpec  Parent width MeasureSpec.
     * @param widthUsed        Width used so far by other child views; used as part of padding
     *                         for current
     *                         child view in MeasureSpec calculation.
     * @param parentHeightSpec Parent height MeasureSpec.
     * @param heightUsed       Height used so far by other child views; used as part of padding for
     *                         current child view in MeasureSpec calculation.
     */
    private void measureChild(View child, int parentWidthSpec, int widthUsed,
            int parentHeightSpec, int heightUsed) {
        // Calculate the padding and margin of current dimension,
        // including the width/height used by other child views.
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int childWidthSpec = getChildMeasureSpec(parentWidthSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed,
                lp.width);
        int childHeightSpec = getChildMeasureSpec(parentHeightSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed,
                lp.height);
        child.measure(childWidthSpec, childHeightSpec);
    }

    /**
     * Class that keeps track of a {@link CarMenuItem} and its inflated {@link View}.
     */
    private static class InflatedMenuItem {
        final CarMenuItem mItem;
        @Nullable
        final View mView;
        boolean mIsDisplayedOnToolbar;

        // |view| is Nullable since overflow items do not have a View associated with them.
        InflatedMenuItem(@NonNull CarMenuItem item, @Nullable View view) {
            mItem = item;
            mView = view;
        }
    }
}
