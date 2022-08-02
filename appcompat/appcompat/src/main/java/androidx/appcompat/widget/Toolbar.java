/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.annotation.RestrictTo.Scope.TESTS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.ColorInt;
import androidx.annotation.DoNotInline;
import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.R;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.CollapsibleActionView;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.view.menu.SubMenuBuilder;
import androidx.core.view.GravityCompat;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuHostHelper;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.resourceinspection.annotation.Attribute;

import java.util.ArrayList;
import java.util.List;

/**
 * A standard toolbar for use within application content.
 *
 * <p>A Toolbar is a generalization of {@link ActionBar action bars} for use
 * within application layouts. While an action bar is traditionally part of an
 * {@link android.app.Activity Activity's} opaque window decor controlled by the framework,
 * a Toolbar may be placed at any arbitrary level of nesting within a view hierarchy.
 * An application may choose to designate a Toolbar as the action bar for an Activity
 * using the {@link androidx.appcompat.app.AppCompatActivity#setSupportActionBar(Toolbar)
 * setSupportActionBar()} method.</p>
 *
 * <p>Toolbar supports a more focused feature set than ActionBar. From start to end, a toolbar
 * may contain a combination of the following optional elements:
 *
 * <ul>
 *     <li><em>A navigation button.</em> This may be an Up arrow, navigation menu toggle, close,
 *     collapse, done or another glyph of the app's choosing. This button should always be used
 *     to access other navigational destinations within the container of the Toolbar and
 *     its signified content or otherwise leave the current context signified by the Toolbar.
 *     The navigation button is vertically aligned within the Toolbar's minimum height,
 *     if set.</li>
 *     <li><em>A branded logo image.</em> This may extend to the height of the bar and can be
 *     arbitrarily wide.</li>
 *     <li><em>A title and subtitle.</em> The title should be a signpost for the Toolbar's current
 *     position in the navigation hierarchy and the content contained there. The subtitle,
 *     if present should indicate any extended information about the current content.
 *     If an app uses a logo image it should strongly consider omitting a title and subtitle.</li>
 *     <li><em>One or more custom views.</em> The application may add arbitrary child views
 *     to the Toolbar. They will appear at this position within the layout. If a child view's
 *     {@link LayoutParams} indicates a {@link Gravity} value of
 *     {@link Gravity#CENTER_HORIZONTAL CENTER_HORIZONTAL} the view will attempt to center
 *     within the available space remaining in the Toolbar after all other elements have been
 *     measured.</li>
 *     <li><em>An {@link ActionMenuView action menu}.</em> The menu of actions will pin to the
 *     end of the Toolbar offering a few
 *     <a href="http://developer.android.com/design/patterns/actionbar.html#ActionButtons">
 *     frequent, important or typical</a> actions along with an optional overflow menu for
 *     additional actions. Action buttons are vertically aligned within the Toolbar's
 *     minimum height, if set.</li>
 * </ul>
 * </p>
 *
 * <p>In modern Android UIs developers should lean more on a visually distinct color scheme for
 * toolbars than on their application icon. The use of application icon plus title as a standard
 * layout is discouraged on API 21 devices and newer.</p>
 *
 * {@link androidx.appcompat.R.attr#buttonGravity}
 * {@link androidx.appcompat.R.attr#collapseContentDescription}
 * {@link androidx.appcompat.R.attr#collapseIcon}
 * {@link androidx.appcompat.R.attr#contentInsetEnd}
 * {@link androidx.appcompat.R.attr#contentInsetLeft}
 * {@link androidx.appcompat.R.attr#contentInsetRight}
 * {@link androidx.appcompat.R.attr#contentInsetStart}
 * {@link androidx.appcompat.R.attr#contentInsetStartWithNavigation}
 * {@link androidx.appcompat.R.attr#contentInsetEndWithActions}
 * {@link android.R.attr#gravity}
 * {@link androidx.appcompat.R.attr#logo}
 * {@link androidx.appcompat.R.attr#logoDescription}
 * {@link androidx.appcompat.R.attr#maxButtonHeight}
 * {@link androidx.appcompat.R.attr#navigationContentDescription}
 * {@link androidx.appcompat.R.attr#navigationIcon}
 * {@link androidx.appcompat.R.attr#popupTheme}
 * {@link androidx.appcompat.R.attr#subtitle}
 * {@link androidx.appcompat.R.attr#subtitleTextAppearance}
 * {@link androidx.appcompat.R.attr#subtitleTextColor}
 * {@link androidx.appcompat.R.attr#title}
 * {@link androidx.appcompat.R.attr#titleMargin}
 * {@link androidx.appcompat.R.attr#titleMarginBottom}
 * {@link androidx.appcompat.R.attr#titleMarginEnd}
 * {@link androidx.appcompat.R.attr#titleMarginStart}
 * {@link androidx.appcompat.R.attr#titleMarginTop}
 * {@link androidx.appcompat.R.attr#titleTextAppearance}
 * {@link androidx.appcompat.R.attr#titleTextColor}
 * {@link androidx.appcompat.R.attr#menu}
 */
public class Toolbar extends ViewGroup implements MenuHost {
    private static final String TAG = "Toolbar";

    private ActionMenuView mMenuView;
    private TextView mTitleTextView;
    private TextView mSubtitleTextView;
    private ImageButton mNavButtonView;
    private ImageView mLogoView;

    private Drawable mCollapseIcon;
    private CharSequence mCollapseDescription;
    ImageButton mCollapseButtonView;
    View mExpandedActionView;

    /** Context against which to inflate popup menus. */
    private Context mPopupContext;

    /** Theme resource against which to inflate popup menus. */
    private int mPopupTheme;

    private int mTitleTextAppearance;
    private int mSubtitleTextAppearance;

    int mButtonGravity;

    private int mMaxButtonHeight;

    private int mTitleMarginStart;
    private int mTitleMarginEnd;
    private int mTitleMarginTop;
    private int mTitleMarginBottom;

    private RtlSpacingHelper mContentInsets;
    private int mContentInsetStartWithNavigation;
    private int mContentInsetEndWithActions;

    private int mGravity = GravityCompat.START | Gravity.CENTER_VERTICAL;

    private CharSequence mTitleText;
    private CharSequence mSubtitleText;

    private ColorStateList mTitleTextColor;
    private ColorStateList mSubtitleTextColor;

    private boolean mEatingTouch;
    private boolean mEatingHover;

    // Clear me after use.
    private final ArrayList<View> mTempViews = new ArrayList<View>();

    // Used to hold views that will be removed while we have an expanded action view.
    private final ArrayList<View> mHiddenViews = new ArrayList<>();

    private final int[] mTempMargins = new int[2];

    final MenuHostHelper mMenuHostHelper = new MenuHostHelper(this::invalidateMenu);
    private ArrayList<MenuItem> mProvidedMenuItems = new ArrayList<>();
    OnMenuItemClickListener mOnMenuItemClickListener;

    private final ActionMenuView.OnMenuItemClickListener mMenuViewItemClickListener =
            new ActionMenuView.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    boolean consumed = mMenuHostHelper.onMenuItemSelected(item);
                    if (consumed) {
                        return true;
                    } else if (mOnMenuItemClickListener != null) {
                        return mOnMenuItemClickListener.onMenuItemClick(item);
                    }
                    return false;
                }
            };

    private ToolbarWidgetWrapper mWrapper;
    private ActionMenuPresenter mOuterActionMenuPresenter;
    private ExpandedActionViewMenuPresenter mExpandedMenuPresenter;
    private MenuPresenter.Callback mActionMenuPresenterCallback;
    private MenuBuilder.Callback mMenuBuilderCallback;

    private boolean mCollapsible;

    // The callback handling back events. If this is non-null, the
    // callback has been registered at least once.
    private OnBackInvokedCallback mBackInvokedCallback;

    // The dispatcher on which the callback was registered. If this
    // value is null, the callback is not registered anywhere.
    private OnBackInvokedDispatcher mBackInvokedDispatcher;

    // Whether this Toolbar should register a back invocation handler
    // when its action view is expanded.
    private boolean mBackInvokedCallbackEnabled;

    private final Runnable mShowOverflowMenuRunnable = new Runnable() {
        @Override public void run() {
            showOverflowMenu();
        }
    };

    public Toolbar(@NonNull Context context) {
        this(context, null);
    }

    public Toolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.toolbarStyle);
    }

    public Toolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Need to use getContext() here so that we use the themed context
        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(getContext(), attrs,
                R.styleable.Toolbar, defStyleAttr, 0);
        ViewCompat.saveAttributeDataForStyleable(this, context, R.styleable.Toolbar, attrs,
                    a.getWrappedTypeArray(), defStyleAttr, 0);

        mTitleTextAppearance = a.getResourceId(R.styleable.Toolbar_titleTextAppearance, 0);
        mSubtitleTextAppearance = a.getResourceId(R.styleable.Toolbar_subtitleTextAppearance, 0);
        mGravity = a.getInteger(R.styleable.Toolbar_android_gravity, mGravity);
        mButtonGravity = a.getInteger(R.styleable.Toolbar_buttonGravity, Gravity.TOP);

        // First read the correct attribute
        int titleMargin = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMargin, 0);
        if (a.hasValue(R.styleable.Toolbar_titleMargins)) {
            // Now read the deprecated attribute, if it has a value
            titleMargin = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMargins, titleMargin);
        }
        mTitleMarginStart = mTitleMarginEnd = mTitleMarginTop = mTitleMarginBottom = titleMargin;

        final int marginStart = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginStart, -1);
        if (marginStart >= 0) {
            mTitleMarginStart = marginStart;
        }

        final int marginEnd = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginEnd, -1);
        if (marginEnd >= 0) {
            mTitleMarginEnd = marginEnd;
        }

        final int marginTop = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginTop, -1);
        if (marginTop >= 0) {
            mTitleMarginTop = marginTop;
        }

        final int marginBottom = a.getDimensionPixelOffset(R.styleable.Toolbar_titleMarginBottom,
                -1);
        if (marginBottom >= 0) {
            mTitleMarginBottom = marginBottom;
        }

        mMaxButtonHeight = a.getDimensionPixelSize(R.styleable.Toolbar_maxButtonHeight, -1);

        final int contentInsetStart =
                a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetStart,
                        RtlSpacingHelper.UNDEFINED);
        final int contentInsetEnd =
                a.getDimensionPixelOffset(R.styleable.Toolbar_contentInsetEnd,
                        RtlSpacingHelper.UNDEFINED);
        final int contentInsetLeft =
                a.getDimensionPixelSize(R.styleable.Toolbar_contentInsetLeft, 0);
        final int contentInsetRight =
                a.getDimensionPixelSize(R.styleable.Toolbar_contentInsetRight, 0);

        ensureContentInsets();
        mContentInsets.setAbsolute(contentInsetLeft, contentInsetRight);

        if (contentInsetStart != RtlSpacingHelper.UNDEFINED ||
                contentInsetEnd != RtlSpacingHelper.UNDEFINED) {
            mContentInsets.setRelative(contentInsetStart, contentInsetEnd);
        }

        mContentInsetStartWithNavigation = a.getDimensionPixelOffset(
                R.styleable.Toolbar_contentInsetStartWithNavigation, RtlSpacingHelper.UNDEFINED);
        mContentInsetEndWithActions = a.getDimensionPixelOffset(
                R.styleable.Toolbar_contentInsetEndWithActions, RtlSpacingHelper.UNDEFINED);

        mCollapseIcon = a.getDrawable(R.styleable.Toolbar_collapseIcon);
        mCollapseDescription = a.getText(R.styleable.Toolbar_collapseContentDescription);

        final CharSequence title = a.getText(R.styleable.Toolbar_title);
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        }

        final CharSequence subtitle = a.getText(R.styleable.Toolbar_subtitle);
        if (!TextUtils.isEmpty(subtitle)) {
            setSubtitle(subtitle);
        }

        // Set the default context, since setPopupTheme() may be a no-op.
        mPopupContext = getContext();
        setPopupTheme(a.getResourceId(R.styleable.Toolbar_popupTheme, 0));

        final Drawable navIcon = a.getDrawable(R.styleable.Toolbar_navigationIcon);
        if (navIcon != null) {
            setNavigationIcon(navIcon);
        }
        final CharSequence navDesc = a.getText(R.styleable.Toolbar_navigationContentDescription);
        if (!TextUtils.isEmpty(navDesc)) {
            setNavigationContentDescription(navDesc);
        }

        final Drawable logo = a.getDrawable(R.styleable.Toolbar_logo);
        if (logo != null) {
            setLogo(logo);
        }

        final CharSequence logoDesc = a.getText(R.styleable.Toolbar_logoDescription);
        if (!TextUtils.isEmpty(logoDesc)) {
            setLogoDescription(logoDesc);
        }

        if (a.hasValue(R.styleable.Toolbar_titleTextColor)) {
            setTitleTextColor(a.getColorStateList(R.styleable.Toolbar_titleTextColor));
        }

        if (a.hasValue(R.styleable.Toolbar_subtitleTextColor)) {
            setSubtitleTextColor(a.getColorStateList(R.styleable.Toolbar_subtitleTextColor));
        }

        if (a.hasValue(R.styleable.Toolbar_menu)) {
            inflateMenu(a.getResourceId(R.styleable.Toolbar_menu, 0));
        }

        a.recycle();
    }

    /**
     * Sets whether the toolbar will attempt to register its own {@link OnBackInvokedCallback} in
     * supported configurations to handle collapsing expanded action items when a back invocation
     * occurs.
     * <p>
     * This feature is only supported on SDK 33 and above for applications that have enabled back
     * invocation callback handling.
     *
     * @param enabled {@code true} to attempt to register a back invocation callback in supported
     *                configurations or {@code false} to not automatically handle back invocations
     *
     * @see #isBackInvokedCallbackEnabled()
     */
    public void setBackInvokedCallbackEnabled(boolean enabled) {
        if (mBackInvokedCallbackEnabled != enabled) {
            mBackInvokedCallbackEnabled = enabled;

            // mShouldHandleBackInvoked changed
            updateBackInvokedCallbackState();
        }
    }

    /**
     * Returns whether the toolbar will attempt to register its own {@link OnBackInvokedCallback}
     * in supported configurations to handle collapsing expanded action items when a back
     * invocation occurs.
     *
     * @see #setBackInvokedCallbackEnabled(boolean)
     */
    public boolean isBackInvokedCallbackEnabled() {
        return mBackInvokedCallbackEnabled;
    }

    /**
     * Specifies the theme to use when inflating popup menus. By default, uses
     * the same theme as the toolbar itself.
     *
     * @param resId theme used to inflate popup menus
     * @see #getPopupTheme()
     */
    public void setPopupTheme(@StyleRes int resId) {
        if (mPopupTheme != resId) {
            mPopupTheme = resId;
            if (resId == 0) {
                mPopupContext = getContext();
            } else {
                mPopupContext = new ContextThemeWrapper(getContext(), resId);
            }
        }
    }

    /**
     * @return resource identifier of the theme used to inflate popup menus, or
     *         0 if menus are inflated against the toolbar theme
     * @see #setPopupTheme(int)
     */
    @Attribute("androidx.appcompat:popupTheme")
    @StyleRes
    public int getPopupTheme() {
        return mPopupTheme;
    }

    /**
     * Sets the title margin.
     *
     * @param start the starting title margin in pixels
     * @param top the top title margin in pixels
     * @param end the ending title margin in pixels
     * @param bottom the bottom title margin in pixels
     * @see #getTitleMarginStart()
     * @see #getTitleMarginTop()
     * @see #getTitleMarginEnd()
     * @see #getTitleMarginBottom()
     * {@link androidx.appcompat.R.attr#titleMargin}
     */
    public void setTitleMargin(int start, int top, int end, int bottom) {
        mTitleMarginStart = start;
        mTitleMarginTop = top;
        mTitleMarginEnd = end;
        mTitleMarginBottom = bottom;

        requestLayout();
    }

    /**
     * @return the starting title margin in pixels
     * @see #setTitleMarginStart(int)
     * {@link androidx.appcompat.R.attr#titleMarginStart}
     */
    @Attribute("androidx.appcompat:titleMarginStart")
    public int getTitleMarginStart() {
        return mTitleMarginStart;
    }

    /**
     * Sets the starting title margin in pixels.
     *
     * @param margin the starting title margin in pixels
     * @see #getTitleMarginStart()
     * {@link androidx.appcompat.R.attr#titleMarginStart}
     */
    public void setTitleMarginStart(int margin) {
        mTitleMarginStart = margin;

        requestLayout();
    }

    /**
     * @return the top title margin in pixels
     * @see #setTitleMarginTop(int)
     * {@link androidx.appcompat.R.attr#titleMarginTop}
     */
    @Attribute("androidx.appcompat:titleMarginTop")
    public int getTitleMarginTop() {
        return mTitleMarginTop;
    }

    /**
     * Sets the top title margin in pixels.
     *
     * @param margin the top title margin in pixels
     * @see #getTitleMarginTop()
     * {@link androidx.appcompat.R.attr#titleMarginTop}
     */
    public void setTitleMarginTop(int margin) {
        mTitleMarginTop = margin;

        requestLayout();
    }

    /**
     * @return the ending title margin in pixels
     * @see #setTitleMarginEnd(int)
     * {@link androidx.appcompat.R.attr#titleMarginEnd}
     */
    @Attribute("androidx.appcompat:titleMarginEnd")
    public int getTitleMarginEnd() {
        return mTitleMarginEnd;
    }

    /**
     * Sets the ending title margin in pixels.
     *
     * @param margin the ending title margin in pixels
     * @see #getTitleMarginEnd()
     * {@link androidx.appcompat.R.attr#titleMarginEnd}
     */
    public void setTitleMarginEnd(int margin) {
        mTitleMarginEnd = margin;

        requestLayout();
    }

    /**
     * @return the bottom title margin in pixels
     * @see #setTitleMarginBottom(int)
     * {@link androidx.appcompat.R.attr#titleMarginBottom}
     */
    @Attribute("androidx.appcompat:titleMarginBottom")
    public int getTitleMarginBottom() {
        return mTitleMarginBottom;
    }

    /**
     * Sets the bottom title margin in pixels.
     *
     * @param margin the bottom title margin in pixels
     * @see #getTitleMarginBottom()
     * {@link androidx.appcompat.R.attr#titleMarginBottom}
     */
    public void setTitleMarginBottom(int margin) {
        mTitleMarginBottom = margin;
        requestLayout();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (Build.VERSION.SDK_INT >= 17) {
            super.onRtlPropertiesChanged(layoutDirection);
        }

        ensureContentInsets();
        mContentInsets.setDirection(layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL);
    }

    /**
     * Set a logo drawable from a resource id.
     *
     * <p>This drawable should generally take the place of title text. The logo cannot be
     * clicked. Apps using a logo should also supply a description using
     * {@link #setLogoDescription(int)}.</p>
     *
     * @param resId ID of a drawable resource
     */
    public void setLogo(@DrawableRes int resId) {
        setLogo(AppCompatResources.getDrawable(getContext(), resId));
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public boolean canShowOverflowMenu() {
        return getVisibility() == VISIBLE && mMenuView != null && mMenuView.isOverflowReserved();
    }

    /**
     * Check whether the overflow menu is currently showing. This may not reflect
     * a pending show operation in progress.
     *
     * @return true if the overflow menu is currently showing
     */
    public boolean isOverflowMenuShowing() {
        return mMenuView != null && mMenuView.isOverflowMenuShowing();
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public boolean isOverflowMenuShowPending() {
        return mMenuView != null && mMenuView.isOverflowMenuShowPending();
    }

    /**
     * Show the overflow items from the associated menu.
     *
     * @return true if the menu was able to be shown, false otherwise
     */
    public boolean showOverflowMenu() {
        return mMenuView != null && mMenuView.showOverflowMenu();
    }

    /**
     * Hide the overflow items from the associated menu.
     *
     * @return true if the menu was able to be hidden, false otherwise
     */
    public boolean hideOverflowMenu() {
        return mMenuView != null && mMenuView.hideOverflowMenu();
    }

    /** @hide */
    @RestrictTo(LIBRARY)
    public void setMenu(MenuBuilder menu, ActionMenuPresenter outerPresenter) {
        if (menu == null && mMenuView == null) {
            return;
        }

        ensureMenuView();
        final MenuBuilder oldMenu = mMenuView.peekMenu();
        if (oldMenu == menu) {
            return;
        }

        if (oldMenu != null) {
            oldMenu.removeMenuPresenter(mOuterActionMenuPresenter);
            oldMenu.removeMenuPresenter(mExpandedMenuPresenter);
        }

        if (mExpandedMenuPresenter == null) {
            mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
        }

        outerPresenter.setExpandedActionViewsExclusive(true);
        if (menu != null) {
            menu.addMenuPresenter(outerPresenter, mPopupContext);
            menu.addMenuPresenter(mExpandedMenuPresenter, mPopupContext);
        } else {
            outerPresenter.initForMenu(mPopupContext, null);
            mExpandedMenuPresenter.initForMenu(mPopupContext, null);
            outerPresenter.updateMenuView(true);
            mExpandedMenuPresenter.updateMenuView(true);
        }
        mMenuView.setPopupTheme(mPopupTheme);
        mMenuView.setPresenter(outerPresenter);
        mOuterActionMenuPresenter = outerPresenter;

        // mExpandedMenuPresenter has changed.
        updateBackInvokedCallbackState();
    }

    /**
     * Dismiss all currently showing popup menus, including overflow or submenus.
     */
    public void dismissPopupMenus() {
        if (mMenuView != null) {
            mMenuView.dismissPopupMenus();
        }
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public boolean isTitleTruncated() {
        if (mTitleTextView == null) {
            return false;
        }

        final Layout titleLayout = mTitleTextView.getLayout();
        if (titleLayout == null) {
            return false;
        }

        final int lineCount = titleLayout.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            if (titleLayout.getEllipsisCount(i) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set a logo drawable.
     *
     * <p>This drawable should generally take the place of title text. The logo cannot be
     * clicked. Apps using a logo should also supply a description using
     * {@link #setLogoDescription(int)}.</p>
     *
     * @param drawable Drawable to use as a logo
     */
    public void setLogo(Drawable drawable) {
        if (drawable != null) {
            ensureLogoView();
            if (!isChildOrHidden(mLogoView)) {
                addSystemView(mLogoView, true);
            }
        } else if (mLogoView != null && isChildOrHidden(mLogoView)) {
            removeView(mLogoView);
            mHiddenViews.remove(mLogoView);
        }
        if (mLogoView != null) {
            mLogoView.setImageDrawable(drawable);
        }
    }

    /**
     * Return the current logo drawable.
     *
     * @return The current logo drawable
     * @see #setLogo(int)
     * @see #setLogo(android.graphics.drawable.Drawable)
     */
    @Attribute("androidx.appcompat:logo")
    public Drawable getLogo() {
        return mLogoView != null ? mLogoView.getDrawable() : null;
    }

    /**
     * Set a description of the toolbar's logo.
     *
     * <p>This description will be used for accessibility or other similar descriptions
     * of the UI.</p>
     *
     * @param resId String resource id
     */
    public void setLogoDescription(@StringRes int resId) {
        setLogoDescription(getContext().getText(resId));
    }

    /**
     * Set a description of the toolbar's logo.
     *
     * <p>This description will be used for accessibility or other similar descriptions
     * of the UI.</p>
     *
     * @param description Description to set
     */
    public void setLogoDescription(CharSequence description) {
        if (!TextUtils.isEmpty(description)) {
            ensureLogoView();
        }
        if (mLogoView != null) {
            mLogoView.setContentDescription(description);
        }
    }

    /**
     * Return the description of the toolbar's logo.
     *
     * @return A description of the logo
     */
    @Attribute("androidx.appcompat:logoDescription")
    public CharSequence getLogoDescription() {
        return mLogoView != null ? mLogoView.getContentDescription() : null;
    }

    private void ensureLogoView() {
        if (mLogoView == null) {
            mLogoView = new AppCompatImageView(getContext());
        }
    }

    /**
     * Check whether this Toolbar is currently hosting an expanded action view.
     *
     * <p>An action view may be expanded either directly from the
     * {@link android.view.MenuItem MenuItem} it belongs to or by user action. If the Toolbar
     * has an expanded action view it can be collapsed using the {@link #collapseActionView()}
     * method.</p>
     *
     * @return true if the Toolbar has an expanded action view
     */
    public boolean hasExpandedActionView() {
        return mExpandedMenuPresenter != null &&
                mExpandedMenuPresenter.mCurrentExpandedItem != null;
    }

    /**
     * Collapse a currently expanded action view. If this Toolbar does not have an
     * expanded action view this method has no effect.
     *
     * <p>An action view may be expanded either directly from the
     * {@link android.view.MenuItem MenuItem} it belongs to or by user action.</p>
     *
     * @see #hasExpandedActionView()
     */
    public void collapseActionView() {
        final MenuItemImpl item = mExpandedMenuPresenter == null ? null :
                mExpandedMenuPresenter.mCurrentExpandedItem;
        if (item != null) {
            item.collapseActionView();
        }
    }

    /**
     * Returns the title of this toolbar.
     *
     * @return The current title.
     */
    @Attribute("androidx.appcompat:title")
    public CharSequence getTitle() {
        return mTitleText;
    }

    /**
     * Set the title of this toolbar.
     *
     * <p>A title should be used as the anchor for a section of content. It should
     * describe or name the content being viewed.</p>
     *
     * @param resId Resource ID of a string to set as the title
     */
    public void setTitle(@StringRes int resId) {
        setTitle(getContext().getText(resId));
    }

    /**
     * Set the title of this toolbar.
     *
     * <p>A title should be used as the anchor for a section of content. It should
     * describe or name the content being viewed.</p>
     *
     * @param title Title to set
     */
    public void setTitle(CharSequence title) {
        if (!TextUtils.isEmpty(title)) {
            if (mTitleTextView == null) {
                final Context context = getContext();
                mTitleTextView = new AppCompatTextView(context);
                mTitleTextView.setSingleLine();
                mTitleTextView.setEllipsize(TextUtils.TruncateAt.END);
                if (mTitleTextAppearance != 0) {
                    mTitleTextView.setTextAppearance(context, mTitleTextAppearance);
                }
                if (mTitleTextColor != null) {
                    mTitleTextView.setTextColor(mTitleTextColor);
                }
            }
            if (!isChildOrHidden(mTitleTextView)) {
                addSystemView(mTitleTextView, true);
            }
        } else if (mTitleTextView != null && isChildOrHidden(mTitleTextView)) {
            removeView(mTitleTextView);
            mHiddenViews.remove(mTitleTextView);
        }
        if (mTitleTextView != null) {
            mTitleTextView.setText(title);
        }
        mTitleText = title;
    }

    /**
     * Return the subtitle of this toolbar.
     *
     * @return The current subtitle
     */
    @Attribute("androidx.appcompat:subtitle")
    public CharSequence getSubtitle() {
        return mSubtitleText;
    }

    /**
     * Set the subtitle of this toolbar.
     *
     * <p>Subtitles should express extended information about the current content.</p>
     *
     * @param resId String resource ID
     */
    public void setSubtitle(@StringRes int resId) {
        setSubtitle(getContext().getText(resId));
    }

    /**
     * Set the subtitle of this toolbar.
     *
     * <p>Subtitles should express extended information about the current content.</p>
     *
     * @param subtitle Subtitle to set
     */
    public void setSubtitle(CharSequence subtitle) {
        if (!TextUtils.isEmpty(subtitle)) {
            if (mSubtitleTextView == null) {
                final Context context = getContext();
                mSubtitleTextView = new AppCompatTextView(context);
                mSubtitleTextView.setSingleLine();
                mSubtitleTextView.setEllipsize(TextUtils.TruncateAt.END);
                if (mSubtitleTextAppearance != 0) {
                    mSubtitleTextView.setTextAppearance(context, mSubtitleTextAppearance);
                }
                if (mSubtitleTextColor != null) {
                    mSubtitleTextView.setTextColor(mSubtitleTextColor);
                }
            }
            if (!isChildOrHidden(mSubtitleTextView)) {
                addSystemView(mSubtitleTextView, true);
            }
        } else if (mSubtitleTextView != null && isChildOrHidden(mSubtitleTextView)) {
            removeView(mSubtitleTextView);
            mHiddenViews.remove(mSubtitleTextView);
        }
        if (mSubtitleTextView != null) {
            mSubtitleTextView.setText(subtitle);
        }
        mSubtitleText = subtitle;
    }

    /**
     * Sets the text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     */
    public void setTitleTextAppearance(Context context, @StyleRes int resId) {
        mTitleTextAppearance = resId;
        if (mTitleTextView != null) {
            mTitleTextView.setTextAppearance(context, resId);
        }
    }

    /**
     * Sets the text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     */
    public void setSubtitleTextAppearance(Context context, @StyleRes int resId) {
        mSubtitleTextAppearance = resId;
        if (mSubtitleTextView != null) {
            mSubtitleTextView.setTextAppearance(context, resId);
        }
    }

    /**
     * Sets the text color of the title, if present.
     *
     * @param color The new text color in 0xAARRGGBB format
     */
    public void setTitleTextColor(@ColorInt int color) {
        setTitleTextColor(ColorStateList.valueOf(color));
    }

    /**
     * Sets the text color of the title, if present.
     *
     * @param color The new text color
     */
    public void setTitleTextColor(@NonNull ColorStateList color) {
        mTitleTextColor = color;
        if (mTitleTextView != null) {
            mTitleTextView.setTextColor(color);
        }
    }

    /**
     * Sets the text color of the subtitle, if present.
     *
     * @param color The new text color in 0xAARRGGBB format
     */
    public void setSubtitleTextColor(@ColorInt int color) {
        setSubtitleTextColor(ColorStateList.valueOf(color));
    }

    /**
     * Sets the text color of the subtitle, if present.
     *
     * @param color The new text color
     */
    public void setSubtitleTextColor(@NonNull ColorStateList color) {
        mSubtitleTextColor = color;
        if (mSubtitleTextView != null) {
            mSubtitleTextView.setTextColor(color);
        }
    }

    /**
     * Retrieve the currently configured content description for the navigation button view.
     * This will be used to describe the navigation action to users through mechanisms such
     * as screen readers or tooltips.
     *
     * @return The navigation button's content description
     *
     * {@link androidx.appcompat.R.attr#navigationContentDescription}
     */
    @Attribute("androidx.appcompat:navigationContentDescription")
    @Nullable
    public CharSequence getNavigationContentDescription() {
        return mNavButtonView != null ? mNavButtonView.getContentDescription() : null;
    }

    /**
     * Set a content description for the navigation button if one is present. The content
     * description will be read via screen readers or other accessibility systems to explain
     * the action of the navigation button.
     *
     * @param resId Resource ID of a content description string to set, or 0 to
     *              clear the description
     *
     * {@link androidx.appcompat.R.attr#navigationContentDescription}
     */
    public void setNavigationContentDescription(@StringRes int resId) {
        setNavigationContentDescription(resId != 0 ? getContext().getText(resId) : null);
    }

    /**
     * Set a content description for the navigation button if one is present. The content
     * description will be read via screen readers or other accessibility systems to explain
     * the action of the navigation button.
     *
     * @param description Content description to set, or <code>null</code> to
     *                    clear the content description
     *
     * {@link androidx.appcompat.R.attr#navigationContentDescription}
     */
    public void setNavigationContentDescription(@Nullable CharSequence description) {
        if (!TextUtils.isEmpty(description)) {
            ensureNavButtonView();
        }
        if (mNavButtonView != null) {
            mNavButtonView.setContentDescription(description);
            TooltipCompat.setTooltipText(mNavButtonView, description);
        }
    }

    /**
     * Set the icon to use for the toolbar's navigation button.
     *
     * <p>The navigation button appears at the start of the toolbar if present. Setting an icon
     * will make the navigation button visible.</p>
     *
     * <p>If you use a navigation icon you should also set a description for its action using
     * {@link #setNavigationContentDescription(int)}. This is used for accessibility and
     * tooltips.</p>
     *
     * @param resId Resource ID of a drawable to set
     *
     * {@link androidx.appcompat.R.attr#navigationIcon}
     */
    public void setNavigationIcon(@DrawableRes int resId) {
        setNavigationIcon(AppCompatResources.getDrawable(getContext(), resId));
    }

    /**
     * Set the icon to use for the toolbar's navigation button.
     *
     * <p>The navigation button appears at the start of the toolbar if present. Setting an icon
     * will make the navigation button visible.</p>
     *
     * <p>If you use a navigation icon you should also set a description for its action using
     * {@link #setNavigationContentDescription(int)}. This is used for accessibility and
     * tooltips.</p>
     *
     * @param icon Drawable to set, may be null to clear the icon
     *
     * {@link androidx.appcompat.R.attr#navigationIcon}
     */
    public void setNavigationIcon(@Nullable Drawable icon) {
        if (icon != null) {
            ensureNavButtonView();
            if (!isChildOrHidden(mNavButtonView)) {
                addSystemView(mNavButtonView, true);
            }
        } else if (mNavButtonView != null && isChildOrHidden(mNavButtonView)) {
            removeView(mNavButtonView);
            mHiddenViews.remove(mNavButtonView);
        }
        if (mNavButtonView != null) {
            mNavButtonView.setImageDrawable(icon);
        }
    }

    /**
     * Return the current drawable used as the navigation icon.
     *
     * @return The navigation icon drawable
     *
     * {@link androidx.appcompat.R.attr#navigationIcon}
     */
    @Attribute("androidx.appcompat:navigationIcon")
    @Nullable
    public Drawable getNavigationIcon() {
        return mNavButtonView != null ? mNavButtonView.getDrawable() : null;
    }

    /**
     * Set a listener to respond to navigation events.
     *
     * <p>This listener will be called whenever the user clicks the navigation button
     * at the start of the toolbar. An icon must be set for the navigation button to appear.</p>
     *
     * @param listener Listener to set
     * @see #setNavigationIcon(android.graphics.drawable.Drawable)
     */
    public void setNavigationOnClickListener(OnClickListener listener) {
        ensureNavButtonView();
        mNavButtonView.setOnClickListener(listener);
    }

    /**
     * Retrieve the currently configured content description for the collapse button view.
     * This will be used to describe the collapse action to users through mechanisms such
     * as screen readers or tooltips.
     *
     * @return The collapse button's content description
     *
     * {@link androidx.appcompat.R.attr#collapseContentDescription}
     */
    @Attribute("androidx.appcompat:collapseContentDescription")
    @Nullable
    public CharSequence getCollapseContentDescription() {
        return mCollapseButtonView != null ? mCollapseButtonView.getContentDescription() : null;
    }

    /**
     * Set a content description for the collapse button if one is present. The content description
     * will be read via screen readers or other accessibility systems to explain the action of the
     * collapse button.
     *
     * @param resId Resource ID of a content description string to set, or 0 to
     *              clear the description
     *
     * {@link androidx.appcompat.R.attr#collapseContentDescription}
     */
    public void setCollapseContentDescription(@StringRes int resId) {
        setCollapseContentDescription(resId != 0 ? getContext().getText(resId) : null);
    }

    /**
     * Set a content description for the collapse button if one is present. The content description
     * will be read via screen readers or other accessibility systems to explain the action of the
     * navigation button.
     *
     * @param description Content description to set, or <code>null</code> to
     *                    clear the content description
     *
     * {@link androidx.appcompat.R.attr#collapseContentDescription}
     */
    public void setCollapseContentDescription(@Nullable CharSequence description) {
        if (!TextUtils.isEmpty(description)) {
            ensureCollapseButtonView();
        }
        if (mCollapseButtonView != null) {
            mCollapseButtonView.setContentDescription(description);
        }
    }

    /**
     * Return the current drawable used as the collapse icon.
     *
     * @return The collapse icon drawable
     *
     * {@link androidx.appcompat.R.attr#collapseIcon}
     */
    @Attribute("androidx.appcompat:collapseIcon")
    @Nullable
    public Drawable getCollapseIcon() {
        return mCollapseButtonView != null ? mCollapseButtonView.getDrawable() : null;
    }

    /**
     * Set the icon to use for the toolbar's collapse button.
     *
     * <p>The collapse button appears at the start of the toolbar when an action view is present
     * .</p>
     *
     * @param resId Resource ID of a drawable to set
     *
     * {@link androidx.appcompat.R.attr#collapseIcon}
     */
    public void setCollapseIcon(@DrawableRes int resId) {
        setCollapseIcon(AppCompatResources.getDrawable(getContext(), resId));
    }

    /**
     * Set the icon to use for the toolbar's collapse button.
     *
     * <p>The collapse button appears at the start of the toolbar when an action view is present
     * .</p>
     *
     * @param icon Drawable to set, may be null to use the default icon
     *
     * {@link androidx.appcompat.R.attr#collapseIcon}
     */
    public void setCollapseIcon(@Nullable Drawable icon) {
        if (icon != null) {
            ensureCollapseButtonView();
            mCollapseButtonView.setImageDrawable(icon);
        } else if (mCollapseButtonView != null) {
            mCollapseButtonView.setImageDrawable(mCollapseIcon);
        }
    }

    /**
     * Return the Menu shown in the toolbar.
     *
     * <p>Applications that wish to populate the toolbar's menu can do so from here. To use
     * an XML menu resource, use {@link #inflateMenu(int)}.</p>
     *
     * @return The toolbar's Menu
     * {@link androidx.appcompat.R.attr#menu}
     */
    @Attribute("androidx.appcompat:menu")
    public Menu getMenu() {
        ensureMenu();
        return mMenuView.getMenu();
    }

    /**
     * Set the icon to use for the overflow button.
     *
     * @param icon Drawable to set, may be null to clear the icon
     */
    public void setOverflowIcon(@Nullable Drawable icon) {
        ensureMenu();
        mMenuView.setOverflowIcon(icon);
    }

    /**
     * Return the current drawable used as the overflow icon.
     *
     * @return The overflow icon drawable
     */
    @Nullable
    public Drawable getOverflowIcon() {
        ensureMenu();
        return mMenuView.getOverflowIcon();
    }

    private void ensureMenu() {
        ensureMenuView();
        if (mMenuView.peekMenu() == null) {
            // Initialize a new menu for the first time.
            final MenuBuilder menu = (MenuBuilder) mMenuView.getMenu();
            if (mExpandedMenuPresenter == null) {
                mExpandedMenuPresenter = new ExpandedActionViewMenuPresenter();
            }
            mMenuView.setExpandedActionViewsExclusive(true);
            menu.addMenuPresenter(mExpandedMenuPresenter, mPopupContext);

            // mExpandedMenuPresenter has changed.
            updateBackInvokedCallbackState();
        }
    }

    private void ensureMenuView() {
        if (mMenuView == null) {
            mMenuView = new ActionMenuView(getContext());
            mMenuView.setPopupTheme(mPopupTheme);
            mMenuView.setOnMenuItemClickListener(mMenuViewItemClickListener);
            mMenuView.setMenuCallbacks(mActionMenuPresenterCallback, mMenuBuilderCallback);
            final LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.END | (mButtonGravity & Gravity.VERTICAL_GRAVITY_MASK);
            mMenuView.setLayoutParams(lp);
            addSystemView(mMenuView, false);
        }
    }

    private MenuInflater getMenuInflater() {
        return new SupportMenuInflater(getContext());
    }

    /**
     * Inflate a menu resource into this toolbar.
     *
     * <p>Inflate an XML menu resource into this toolbar. Existing items in the menu will not
     * be modified or removed.</p>
     *
     * @param resId ID of a menu resource to inflate
     * {@link androidx.appcompat.R.attr#menu}
     */
    public void inflateMenu(@MenuRes int resId) {
        getMenuInflater().inflate(resId, getMenu());
    }

    /**
     * Set a listener to respond to menu item click events.
     *
     * <p>This listener will be invoked whenever a user selects a menu item from
     * the action buttons presented at the end of the toolbar or the associated overflow.</p>
     *
     * @param listener Listener to set
     */
    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        mOnMenuItemClickListener = listener;
    }

    /**
     * Sets the content insets for this toolbar relative to layout direction.
     *
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @param contentInsetStart Content inset for the toolbar starting edge
     * @param contentInsetEnd Content inset for the toolbar ending edge
     *
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetEnd()
     * @see #getContentInsetLeft()
     * @see #getContentInsetRight()
     * {@link androidx.appcompat.R.attr#contentInsetEnd}
     * {@link androidx.appcompat.R.attr#contentInsetStart}
     */
    public void setContentInsetsRelative(int contentInsetStart, int contentInsetEnd) {
        ensureContentInsets();
        mContentInsets.setRelative(contentInsetStart, contentInsetEnd);
    }

    /**
     * Gets the starting content inset for this toolbar.
     *
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @return The starting content inset for this toolbar
     *
     * @see #setContentInsetsRelative(int, int)
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetEnd()
     * @see #getContentInsetLeft()
     * @see #getContentInsetRight()
     * {@link androidx.appcompat.R.attr#contentInsetStart}
     */
    @Attribute("androidx.appcompat:contentInsetStart")
    public int getContentInsetStart() {
        return mContentInsets != null ? mContentInsets.getStart() : 0;
    }

    /**
     * Gets the ending content inset for this toolbar.
     *
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @return The ending content inset for this toolbar
     *
     * @see #setContentInsetsRelative(int, int)
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetLeft()
     * @see #getContentInsetRight()
     * {@link androidx.appcompat.R.attr#contentInsetEnd}
     */
    @Attribute("androidx.appcompat:contentInsetEnd")
    public int getContentInsetEnd() {
        return mContentInsets != null ? mContentInsets.getEnd() : 0;
    }

    /**
     * Sets the content insets for this toolbar.
     *
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @param contentInsetLeft Content inset for the toolbar's left edge
     * @param contentInsetRight Content inset for the toolbar's right edge
     *
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetEnd()
     * @see #getContentInsetLeft()
     * @see #getContentInsetRight()
     * {@link androidx.appcompat.R.attr#contentInsetLeft}
     * {@link androidx.appcompat.R.attr#contentInsetRight}
     */
    public void setContentInsetsAbsolute(int contentInsetLeft, int contentInsetRight) {
        ensureContentInsets();
        mContentInsets.setAbsolute(contentInsetLeft, contentInsetRight);
    }

    /**
     * Gets the left content inset for this toolbar.
     *
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @return The left content inset for this toolbar
     *
     * @see #setContentInsetsRelative(int, int)
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetEnd()
     * @see #getContentInsetRight()
     * {@link androidx.appcompat.R.attr#contentInsetLeft}
     */
    @Attribute("androidx.appcompat:contentInsetLeft")
    public int getContentInsetLeft() {
        return mContentInsets != null ? mContentInsets.getLeft() : 0;
    }

    /**
     * Gets the right content inset for this toolbar.
     *
     * <p>The content inset affects the valid area for Toolbar content other than
     * the navigation button and menu. Insets define the minimum margin for these components
     * and can be used to effectively align Toolbar content along well-known gridlines.</p>
     *
     * @return The right content inset for this toolbar
     *
     * @see #setContentInsetsRelative(int, int)
     * @see #setContentInsetsAbsolute(int, int)
     * @see #getContentInsetStart()
     * @see #getContentInsetEnd()
     * @see #getContentInsetLeft()
     * {@link androidx.appcompat.R.attr#contentInsetRight}
     */
    @Attribute("androidx.appcompat:contentInsetRight")
    public int getContentInsetRight() {
        return mContentInsets != null ? mContentInsets.getRight() : 0;
    }

    /**
     * Gets the start content inset to use when a navigation button is present.
     *
     * <p>Different content insets are often called for when additional buttons are present
     * in the toolbar, as well as at different toolbar sizes. The larger value of
     * {@link #getContentInsetStart()} and this value will be used during layout.</p>
     *
     * @return the start content inset used when a navigation icon has been set in pixels
     *
     * @see #setContentInsetStartWithNavigation(int)
     * {@link androidx.appcompat.R.attr#contentInsetStartWithNavigation}
     */
    @Attribute("androidx.appcompat:contentInsetStartWithNavigation")
    public int getContentInsetStartWithNavigation() {
        return mContentInsetStartWithNavigation != RtlSpacingHelper.UNDEFINED
                ? mContentInsetStartWithNavigation
                : getContentInsetStart();
    }

    /**
     * Sets the start content inset to use when a navigation button is present.
     *
     * <p>Different content insets are often called for when additional buttons are present
     * in the toolbar, as well as at different toolbar sizes. The larger value of
     * {@link #getContentInsetStart()} and this value will be used during layout.</p>
     *
     * @param insetStartWithNavigation the inset to use when a navigation icon has been set
     *                                 in pixels
     *
     * @see #getContentInsetStartWithNavigation()
     * {@link androidx.appcompat.R.attr#contentInsetStartWithNavigation}
     */
    public void setContentInsetStartWithNavigation(int insetStartWithNavigation) {
        if (insetStartWithNavigation < 0) {
            insetStartWithNavigation = RtlSpacingHelper.UNDEFINED;
        }
        if (insetStartWithNavigation != mContentInsetStartWithNavigation) {
            mContentInsetStartWithNavigation = insetStartWithNavigation;
            if (getNavigationIcon() != null) {
                requestLayout();
            }
        }
    }

    /**
     * Gets the end content inset to use when action buttons are present.
     *
     * <p>Different content insets are often called for when additional buttons are present
     * in the toolbar, as well as at different toolbar sizes. The larger value of
     * {@link #getContentInsetEnd()} and this value will be used during layout.</p>
     *
     * @return the end content inset used when a menu has been set in pixels
     *
     * @see #setContentInsetEndWithActions(int)
     * {@link androidx.appcompat.R.attr#contentInsetEndWithActions}
     */
    @Attribute("androidx.appcompat:contentInsetEndWithActions")
    public int getContentInsetEndWithActions() {
        return mContentInsetEndWithActions != RtlSpacingHelper.UNDEFINED
                ? mContentInsetEndWithActions
                : getContentInsetEnd();
    }

    /**
     * Sets the start content inset to use when action buttons are present.
     *
     * <p>Different content insets are often called for when additional buttons are present
     * in the toolbar, as well as at different toolbar sizes. The larger value of
     * {@link #getContentInsetEnd()} and this value will be used during layout.</p>
     *
     * @param insetEndWithActions the inset to use when a menu has been set in pixels
     *
     * @see #getContentInsetEndWithActions()
     * {@link androidx.appcompat.R.attr#contentInsetEndWithActions}
     */
    public void setContentInsetEndWithActions(int insetEndWithActions) {
        if (insetEndWithActions < 0) {
            insetEndWithActions = RtlSpacingHelper.UNDEFINED;
        }
        if (insetEndWithActions != mContentInsetEndWithActions) {
            mContentInsetEndWithActions = insetEndWithActions;
            if (getNavigationIcon() != null) {
                requestLayout();
            }
        }
    }

    /**
     * Gets the content inset that will be used on the starting side of the bar in the current
     * toolbar configuration.
     *
     * @return the current content inset start in pixels
     *
     * @see #getContentInsetStartWithNavigation()
     */
    public int getCurrentContentInsetStart() {
        return getNavigationIcon() != null
                ? Math.max(getContentInsetStart(), Math.max(mContentInsetStartWithNavigation, 0))
                : getContentInsetStart();
    }

    /**
     * Gets the content inset that will be used on the ending side of the bar in the current
     * toolbar configuration.
     *
     * @return the current content inset end in pixels
     *
     * @see #getContentInsetEndWithActions()
     */
    public int getCurrentContentInsetEnd() {
        boolean hasActions = false;
        if (mMenuView != null) {
            final MenuBuilder mb = mMenuView.peekMenu();
            hasActions = mb != null && mb.hasVisibleItems();
        }
        return hasActions
                ? Math.max(getContentInsetEnd(), Math.max(mContentInsetEndWithActions, 0))
                : getContentInsetEnd();
    }

    /**
     * Gets the content inset that will be used on the left side of the bar in the current
     * toolbar configuration.
     *
     * @return the current content inset left in pixels
     *
     * @see #getContentInsetStartWithNavigation()
     * @see #getContentInsetEndWithActions()
     */
    public int getCurrentContentInsetLeft() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
                ? getCurrentContentInsetEnd()
                : getCurrentContentInsetStart();
    }

    /**
     * Gets the content inset that will be used on the right side of the bar in the current
     * toolbar configuration.
     *
     * @return the current content inset right in pixels
     *
     * @see #getContentInsetStartWithNavigation()
     * @see #getContentInsetEndWithActions()
     */
    public int getCurrentContentInsetRight() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
                ? getCurrentContentInsetStart()
                : getCurrentContentInsetEnd();
    }

    private void ensureNavButtonView() {
        if (mNavButtonView == null) {
            mNavButtonView = new AppCompatImageButton(getContext(), null,
                    R.attr.toolbarNavigationButtonStyle);
            final LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.START | (mButtonGravity & Gravity.VERTICAL_GRAVITY_MASK);
            mNavButtonView.setLayoutParams(lp);
        }
    }

    /**
     * Returns the navigation button view.
     *
     * @hide Only for use in tests.
     */
    @RestrictTo(RestrictTo.Scope.TESTS)
    @Nullable
    View getNavButtonView() {
        return mNavButtonView;
    }

    void ensureCollapseButtonView() {
        if (mCollapseButtonView == null) {
            mCollapseButtonView = new AppCompatImageButton(getContext(), null,
                    R.attr.toolbarNavigationButtonStyle);
            mCollapseButtonView.setImageDrawable(mCollapseIcon);
            mCollapseButtonView.setContentDescription(mCollapseDescription);
            final LayoutParams lp = generateDefaultLayoutParams();
            lp.gravity = GravityCompat.START | (mButtonGravity & Gravity.VERTICAL_GRAVITY_MASK);
            lp.mViewType = LayoutParams.EXPANDED;
            mCollapseButtonView.setLayoutParams(lp);
            mCollapseButtonView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    collapseActionView();
                }
            });
        }
    }

    private void addSystemView(View v, boolean allowHide) {
        final ViewGroup.LayoutParams vlp = v.getLayoutParams();
        final LayoutParams lp;
        if (vlp == null) {
            lp = generateDefaultLayoutParams();
        } else if (!checkLayoutParams(vlp)) {
            lp = generateLayoutParams(vlp);
        } else {
            lp = (LayoutParams) vlp;
        }
        lp.mViewType = LayoutParams.SYSTEM;

        if (allowHide && mExpandedActionView != null) {
            v.setLayoutParams(lp);
            mHiddenViews.add(v);
        } else {
            addView(v, lp);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());

        if (mExpandedMenuPresenter != null && mExpandedMenuPresenter.mCurrentExpandedItem != null) {
            state.expandedMenuItemId = mExpandedMenuPresenter.mCurrentExpandedItem.getItemId();
        }

        state.isOverflowOpen = isOverflowMenuShowing();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        final Menu menu = mMenuView != null ? mMenuView.peekMenu() : null;
        if (ss.expandedMenuItemId != 0 && mExpandedMenuPresenter != null && menu != null) {
            final MenuItem item = menu.findItem(ss.expandedMenuItemId);
            if (item != null) {
                item.expandActionView();
            }
        }

        if (ss.isOverflowOpen) {
            postShowOverflowMenu();
        }
    }

    private void postShowOverflowMenu() {
        removeCallbacks(mShowOverflowMenuRunnable);
        post(mShowOverflowMenuRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mShowOverflowMenuRunnable);
        updateBackInvokedCallbackState();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateBackInvokedCallbackState();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Toolbars always eat touch events, but should still respect the touch event dispatch
        // contract. If the normal View implementation doesn't want the events, we'll just silently
        // eat the rest of the gesture without reporting the events to the default implementation
        // since that's what it expects.

        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mEatingTouch = false;
        }

        if (!mEatingTouch) {
            final boolean handled = super.onTouchEvent(ev);
            if (action == MotionEvent.ACTION_DOWN && !handled) {
                mEatingTouch = true;
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mEatingTouch = false;
        }

        return true;
    }

    @Override
    public boolean onHoverEvent(MotionEvent ev) {
        // Same deal as onTouchEvent() above. Eat all hover events, but still
        // respect the touch event dispatch contract.

        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_HOVER_ENTER) {
            mEatingHover = false;
        }

        if (!mEatingHover) {
            final boolean handled = super.onHoverEvent(ev);
            if (action == MotionEvent.ACTION_HOVER_ENTER && !handled) {
                mEatingHover = true;
            }
        }

        if (action == MotionEvent.ACTION_HOVER_EXIT || action == MotionEvent.ACTION_CANCEL) {
            mEatingHover = false;
        }

        return true;
    }

    private void measureChildConstrained(View child, int parentWidthSpec, int widthUsed,
            int parentHeightSpec, int heightUsed, int heightConstraint) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        int childWidthSpec = getChildMeasureSpec(parentWidthSpec,
                getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        int childHeightSpec = getChildMeasureSpec(parentHeightSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);

        final int childHeightMode = MeasureSpec.getMode(childHeightSpec);
        if (childHeightMode != MeasureSpec.EXACTLY && heightConstraint >= 0) {
            final int size = childHeightMode != MeasureSpec.UNSPECIFIED ?
                    Math.min(MeasureSpec.getSize(childHeightSpec), heightConstraint) :
                    heightConstraint;
            childHeightSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    /**
     * Returns the width + uncollapsed margins
     */
    private int measureChildCollapseMargins(View child,
            int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed, int[] collapsingMargins) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int leftDiff = lp.leftMargin - collapsingMargins[0];
        final int rightDiff = lp.rightMargin - collapsingMargins[1];
        final int leftMargin = Math.max(0, leftDiff);
        final int rightMargin = Math.max(0, rightDiff);
        final int hMargins = leftMargin + rightMargin;
        collapsingMargins[0] = Math.max(0, -leftDiff);
        collapsingMargins[1] = Math.max(0, -rightDiff);

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + hMargins + widthUsed, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        return child.getMeasuredWidth() + hMargins;
    }

    /**
     * Returns true if the Toolbar is collapsible and has no child views with a measured size > 0.
     */
    private boolean shouldCollapse() {
        if (!mCollapsible) return false;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (shouldLayout(child) && child.getMeasuredWidth() > 0 &&
                    child.getMeasuredHeight() > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;
        int childState = 0;

        final int[] collapsingMargins = mTempMargins;
        final int marginStartIndex;
        final int marginEndIndex;
        if (ViewUtils.isLayoutRtl(this)) {
            marginStartIndex = 1;
            marginEndIndex = 0;
        } else {
            marginStartIndex = 0;
            marginEndIndex = 1;
        }

        // System views measure first.

        int navWidth = 0;
        if (shouldLayout(mNavButtonView)) {
            measureChildConstrained(mNavButtonView, widthMeasureSpec, width, heightMeasureSpec, 0,
                    mMaxButtonHeight);
            navWidth = mNavButtonView.getMeasuredWidth() + getHorizontalMargins(mNavButtonView);
            height = Math.max(height, mNavButtonView.getMeasuredHeight() +
                    getVerticalMargins(mNavButtonView));
            childState = View.combineMeasuredStates(childState,
                    mNavButtonView.getMeasuredState());
        }

        if (shouldLayout(mCollapseButtonView)) {
            measureChildConstrained(mCollapseButtonView, widthMeasureSpec, width,
                    heightMeasureSpec, 0, mMaxButtonHeight);
            navWidth = mCollapseButtonView.getMeasuredWidth() +
                    getHorizontalMargins(mCollapseButtonView);
            height = Math.max(height, mCollapseButtonView.getMeasuredHeight() +
                    getVerticalMargins(mCollapseButtonView));
            childState = View.combineMeasuredStates(childState,
                    mCollapseButtonView.getMeasuredState());
        }

        final int contentInsetStart = getCurrentContentInsetStart();
        width += Math.max(contentInsetStart, navWidth);
        collapsingMargins[marginStartIndex] = Math.max(0, contentInsetStart - navWidth);

        int menuWidth = 0;
        if (shouldLayout(mMenuView)) {
            measureChildConstrained(mMenuView, widthMeasureSpec, width, heightMeasureSpec, 0,
                    mMaxButtonHeight);
            menuWidth = mMenuView.getMeasuredWidth() + getHorizontalMargins(mMenuView);
            height = Math.max(height, mMenuView.getMeasuredHeight() +
                    getVerticalMargins(mMenuView));
            childState = View.combineMeasuredStates(childState,
                    mMenuView.getMeasuredState());
        }

        final int contentInsetEnd = getCurrentContentInsetEnd();
        width += Math.max(contentInsetEnd, menuWidth);
        collapsingMargins[marginEndIndex] = Math.max(0, contentInsetEnd - menuWidth);

        if (shouldLayout(mExpandedActionView)) {
            width += measureChildCollapseMargins(mExpandedActionView, widthMeasureSpec, width,
                    heightMeasureSpec, 0, collapsingMargins);
            height = Math.max(height, mExpandedActionView.getMeasuredHeight() +
                    getVerticalMargins(mExpandedActionView));
            childState = View.combineMeasuredStates(childState,
                    mExpandedActionView.getMeasuredState());
        }

        if (shouldLayout(mLogoView)) {
            width += measureChildCollapseMargins(mLogoView, widthMeasureSpec, width,
                    heightMeasureSpec, 0, collapsingMargins);
            height = Math.max(height, mLogoView.getMeasuredHeight() +
                    getVerticalMargins(mLogoView));
            childState = View.combineMeasuredStates(childState,
                    mLogoView.getMeasuredState());
        }

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.mViewType != LayoutParams.CUSTOM || !shouldLayout(child)) {
                // We already got all system views above. Skip them and GONE views.
                continue;
            }

            width += measureChildCollapseMargins(child, widthMeasureSpec, width,
                    heightMeasureSpec, 0, collapsingMargins);
            height = Math.max(height, child.getMeasuredHeight() + getVerticalMargins(child));
            childState = View.combineMeasuredStates(childState, child.getMeasuredState());
        }

        int titleWidth = 0;
        int titleHeight = 0;
        final int titleVertMargins = mTitleMarginTop + mTitleMarginBottom;
        final int titleHorizMargins = mTitleMarginStart + mTitleMarginEnd;
        if (shouldLayout(mTitleTextView)) {
            titleWidth = measureChildCollapseMargins(mTitleTextView, widthMeasureSpec,
                    width + titleHorizMargins, heightMeasureSpec, titleVertMargins,
                    collapsingMargins);
            titleWidth = mTitleTextView.getMeasuredWidth() + getHorizontalMargins(mTitleTextView);
            titleHeight = mTitleTextView.getMeasuredHeight() + getVerticalMargins(mTitleTextView);
            childState = View.combineMeasuredStates(childState, mTitleTextView.getMeasuredState());
        }
        if (shouldLayout(mSubtitleTextView)) {
            titleWidth = Math.max(titleWidth, measureChildCollapseMargins(mSubtitleTextView,
                    widthMeasureSpec, width + titleHorizMargins,
                    heightMeasureSpec, titleHeight + titleVertMargins,
                    collapsingMargins));
            titleHeight += mSubtitleTextView.getMeasuredHeight() +
                    getVerticalMargins(mSubtitleTextView);
            childState = View.combineMeasuredStates(childState,
                    mSubtitleTextView.getMeasuredState());
        }

        width += titleWidth;
        height = Math.max(height, titleHeight);

        // Measurement already took padding into account for available space for the children,
        // add it in for the final size.
        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();

        final int measuredWidth = View.resolveSizeAndState(
                Math.max(width, getSuggestedMinimumWidth()),
                widthMeasureSpec, childState & View.MEASURED_STATE_MASK);
        final int measuredHeight = View.resolveSizeAndState(
                Math.max(height, getSuggestedMinimumHeight()),
                heightMeasureSpec, childState << View.MEASURED_HEIGHT_STATE_SHIFT);

        setMeasuredDimension(measuredWidth, shouldCollapse() ? 0 : measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final boolean isRtl =  ViewCompat.getLayoutDirection(this) ==  ViewCompat.LAYOUT_DIRECTION_RTL;
        final int width = getWidth();
        final int height = getHeight();
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        int left = paddingLeft;
        int right = width - paddingRight;

        final int[] collapsingMargins = mTempMargins;
        collapsingMargins[0] = collapsingMargins[1] = 0;

        // Align views within the minimum toolbar height, if set.
        final int minHeight = ViewCompat.getMinimumHeight(this);
        final int alignmentHeight = minHeight >= 0 ? Math.min(minHeight, b - t) : 0;

        if (shouldLayout(mNavButtonView)) {
            if (isRtl) {
                right = layoutChildRight(mNavButtonView, right, collapsingMargins,
                        alignmentHeight);
            } else {
                left = layoutChildLeft(mNavButtonView, left, collapsingMargins,
                        alignmentHeight);
            }
        }

        if (shouldLayout(mCollapseButtonView)) {
            if (isRtl) {
                right = layoutChildRight(mCollapseButtonView, right, collapsingMargins,
                        alignmentHeight);
            } else {
                left = layoutChildLeft(mCollapseButtonView, left, collapsingMargins,
                        alignmentHeight);
            }
        }

        if (shouldLayout(mMenuView)) {
            if (isRtl) {
                left = layoutChildLeft(mMenuView, left, collapsingMargins,
                        alignmentHeight);
            } else {
                right = layoutChildRight(mMenuView, right, collapsingMargins,
                        alignmentHeight);
            }
        }

        final int contentInsetLeft = getCurrentContentInsetLeft();
        final int contentInsetRight = getCurrentContentInsetRight();
        collapsingMargins[0] = Math.max(0, contentInsetLeft - left);
        collapsingMargins[1] = Math.max(0, contentInsetRight - (width - paddingRight - right));
        left = Math.max(left, contentInsetLeft);
        right = Math.min(right, width - paddingRight - contentInsetRight);

        if (shouldLayout(mExpandedActionView)) {
            if (isRtl) {
                right = layoutChildRight(mExpandedActionView, right, collapsingMargins,
                        alignmentHeight);
            } else {
                left = layoutChildLeft(mExpandedActionView, left, collapsingMargins,
                        alignmentHeight);
            }
        }

        if (shouldLayout(mLogoView)) {
            if (isRtl) {
                right = layoutChildRight(mLogoView, right, collapsingMargins,
                        alignmentHeight);
            } else {
                left = layoutChildLeft(mLogoView, left, collapsingMargins,
                        alignmentHeight);
            }
        }

        final boolean layoutTitle = shouldLayout(mTitleTextView);
        final boolean layoutSubtitle = shouldLayout(mSubtitleTextView);
        int titleHeight = 0;
        if (layoutTitle) {
            final LayoutParams lp = (LayoutParams) mTitleTextView.getLayoutParams();
            titleHeight += lp.topMargin + mTitleTextView.getMeasuredHeight() + lp.bottomMargin;
        }
        if (layoutSubtitle) {
            final LayoutParams lp = (LayoutParams) mSubtitleTextView.getLayoutParams();
            titleHeight += lp.topMargin + mSubtitleTextView.getMeasuredHeight() + lp.bottomMargin;
        }

        if (layoutTitle || layoutSubtitle) {
            int titleTop;
            final View topChild = layoutTitle ? mTitleTextView : mSubtitleTextView;
            final View bottomChild = layoutSubtitle ? mSubtitleTextView : mTitleTextView;
            final LayoutParams toplp = (LayoutParams) topChild.getLayoutParams();
            final LayoutParams bottomlp = (LayoutParams) bottomChild.getLayoutParams();
            final boolean titleHasWidth = (layoutTitle && (mTitleTextView.getMeasuredWidth() > 0))
                    || (layoutSubtitle && mSubtitleTextView.getMeasuredWidth() > 0);

            switch (mGravity & Gravity.VERTICAL_GRAVITY_MASK) {
                case Gravity.TOP:
                    titleTop = getPaddingTop() + toplp.topMargin + mTitleMarginTop;
                    break;
                default:
                case Gravity.CENTER_VERTICAL:
                    final int space = height - paddingTop - paddingBottom;
                    int spaceAbove = (space - titleHeight) / 2;
                    if (spaceAbove < toplp.topMargin + mTitleMarginTop) {
                        spaceAbove = toplp.topMargin + mTitleMarginTop;
                    } else {
                        final int spaceBelow = height - paddingBottom - titleHeight -
                                spaceAbove - paddingTop;
                        if (spaceBelow < toplp.bottomMargin + mTitleMarginBottom) {
                            spaceAbove = Math.max(0, spaceAbove -
                                    (bottomlp.bottomMargin + mTitleMarginBottom - spaceBelow));
                        }
                    }
                    titleTop = paddingTop + spaceAbove;
                    break;
                case Gravity.BOTTOM:
                    titleTop = height - paddingBottom - bottomlp.bottomMargin - mTitleMarginBottom -
                            titleHeight;
                    break;
            }
            if (isRtl) {
                final int rd = (titleHasWidth ? mTitleMarginStart : 0) - collapsingMargins[1];
                right -= Math.max(0, rd);
                collapsingMargins[1] = Math.max(0, -rd);
                int titleRight = right;
                int subtitleRight = right;

                if (layoutTitle) {
                    final LayoutParams lp = (LayoutParams) mTitleTextView.getLayoutParams();
                    final int titleLeft = titleRight - mTitleTextView.getMeasuredWidth();
                    final int titleBottom = titleTop + mTitleTextView.getMeasuredHeight();
                    mTitleTextView.layout(titleLeft, titleTop, titleRight, titleBottom);
                    titleRight = titleLeft - mTitleMarginEnd;
                    titleTop = titleBottom + lp.bottomMargin;
                }
                if (layoutSubtitle) {
                    final LayoutParams lp = (LayoutParams) mSubtitleTextView.getLayoutParams();
                    titleTop += lp.topMargin;
                    final int subtitleLeft = subtitleRight - mSubtitleTextView.getMeasuredWidth();
                    final int subtitleBottom = titleTop + mSubtitleTextView.getMeasuredHeight();
                    mSubtitleTextView.layout(subtitleLeft, titleTop, subtitleRight, subtitleBottom);
                    subtitleRight = subtitleRight - mTitleMarginEnd;
                    titleTop = subtitleBottom + lp.bottomMargin;
                }
                if (titleHasWidth) {
                    right = Math.min(titleRight, subtitleRight);
                }
            } else {
                final int ld = (titleHasWidth ? mTitleMarginStart : 0) - collapsingMargins[0];
                left += Math.max(0, ld);
                collapsingMargins[0] = Math.max(0, -ld);
                int titleLeft = left;
                int subtitleLeft = left;

                if (layoutTitle) {
                    final LayoutParams lp = (LayoutParams) mTitleTextView.getLayoutParams();
                    final int titleRight = titleLeft + mTitleTextView.getMeasuredWidth();
                    final int titleBottom = titleTop + mTitleTextView.getMeasuredHeight();
                    mTitleTextView.layout(titleLeft, titleTop, titleRight, titleBottom);
                    titleLeft = titleRight + mTitleMarginEnd;
                    titleTop = titleBottom + lp.bottomMargin;
                }
                if (layoutSubtitle) {
                    final LayoutParams lp = (LayoutParams) mSubtitleTextView.getLayoutParams();
                    titleTop += lp.topMargin;
                    final int subtitleRight = subtitleLeft + mSubtitleTextView.getMeasuredWidth();
                    final int subtitleBottom = titleTop + mSubtitleTextView.getMeasuredHeight();
                    mSubtitleTextView.layout(subtitleLeft, titleTop, subtitleRight, subtitleBottom);
                    subtitleLeft = subtitleRight + mTitleMarginEnd;
                    titleTop = subtitleBottom + lp.bottomMargin;
                }
                if (titleHasWidth) {
                    left = Math.max(titleLeft, subtitleLeft);
                }
            }
        }

        // Get all remaining children sorted for layout. This is all prepared
        // such that absolute layout direction can be used below.

        addCustomViewsWithGravity(mTempViews, Gravity.LEFT);
        final int leftViewsCount = mTempViews.size();
        for (int i = 0; i < leftViewsCount; i++) {
            left = layoutChildLeft(mTempViews.get(i), left, collapsingMargins,
                    alignmentHeight);
        }

        addCustomViewsWithGravity(mTempViews, Gravity.RIGHT);
        final int rightViewsCount = mTempViews.size();
        for (int i = 0; i < rightViewsCount; i++) {
            right = layoutChildRight(mTempViews.get(i), right, collapsingMargins,
                    alignmentHeight);
        }

        // Centered views try to center with respect to the whole bar, but views pinned
        // to the left or right can push the mass of centered views to one side or the other.
        addCustomViewsWithGravity(mTempViews, Gravity.CENTER_HORIZONTAL);
        final int centerViewsWidth = getViewListMeasuredWidth(mTempViews, collapsingMargins);
        final int parentCenter = paddingLeft + (width - paddingLeft - paddingRight) / 2;
        final int halfCenterViewsWidth = centerViewsWidth / 2;
        int centerLeft = parentCenter - halfCenterViewsWidth;
        final int centerRight = centerLeft + centerViewsWidth;
        if (centerLeft < left) {
            centerLeft = left;
        } else if (centerRight > right) {
            centerLeft -= centerRight - right;
        }

        final int centerViewsCount = mTempViews.size();
        for (int i = 0; i < centerViewsCount; i++) {
            centerLeft = layoutChildLeft(mTempViews.get(i), centerLeft, collapsingMargins,
                    alignmentHeight);
        }

        mTempViews.clear();
    }

    private int getViewListMeasuredWidth(List<View> views, int[] collapsingMargins) {
        int collapseLeft = collapsingMargins[0];
        int collapseRight = collapsingMargins[1];
        int width = 0;
        final int count = views.size();
        for (int i = 0; i < count; i++) {
            final View v = views.get(i);
            final LayoutParams lp = (LayoutParams) v.getLayoutParams();
            final int l = lp.leftMargin - collapseLeft;
            final int r = lp.rightMargin - collapseRight;
            final int leftMargin = Math.max(0, l);
            final int rightMargin = Math.max(0, r);
            collapseLeft = Math.max(0, -l);
            collapseRight = Math.max(0, -r);
            width += leftMargin + v.getMeasuredWidth() + rightMargin;
        }
        return width;
    }

    private int layoutChildLeft(View child, int left, int[] collapsingMargins,
            int alignmentHeight) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int l = lp.leftMargin - collapsingMargins[0];
        left += Math.max(0, l);
        collapsingMargins[0] = Math.max(0, -l);
        final int top = getChildTop(child, alignmentHeight);
        final int childWidth = child.getMeasuredWidth();
        child.layout(left, top, left + childWidth, top + child.getMeasuredHeight());
        left += childWidth + lp.rightMargin;
        return left;
    }

    private int layoutChildRight(View child, int right, int[] collapsingMargins,
            int alignmentHeight) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int r = lp.rightMargin - collapsingMargins[1];
        right -= Math.max(0, r);
        collapsingMargins[1] = Math.max(0, -r);
        final int top = getChildTop(child, alignmentHeight);
        final int childWidth = child.getMeasuredWidth();
        child.layout(right - childWidth, top, right, top + child.getMeasuredHeight());
        right -= childWidth + lp.leftMargin;
        return right;
    }

    private int getChildTop(View child, int alignmentHeight) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int childHeight = child.getMeasuredHeight();
        final int alignmentOffset = alignmentHeight > 0 ? (childHeight - alignmentHeight) / 2 : 0;
        switch (getChildVerticalGravity(lp.gravity)) {
            case Gravity.TOP:
                return getPaddingTop() - alignmentOffset;

            case Gravity.BOTTOM:
                return getHeight() - getPaddingBottom() - childHeight
                        - lp.bottomMargin - alignmentOffset;

            default:
            case Gravity.CENTER_VERTICAL:
                final int paddingTop = getPaddingTop();
                final int paddingBottom = getPaddingBottom();
                final int height = getHeight();
                final int space = height - paddingTop - paddingBottom;
                int spaceAbove = (space - childHeight) / 2;
                if (spaceAbove < lp.topMargin) {
                    spaceAbove = lp.topMargin;
                } else {
                    final int spaceBelow = height - paddingBottom - childHeight -
                            spaceAbove - paddingTop;
                    if (spaceBelow < lp.bottomMargin) {
                        spaceAbove = Math.max(0, spaceAbove - (lp.bottomMargin - spaceBelow));
                    }
                }
                return paddingTop + spaceAbove;
        }
    }

    private int getChildVerticalGravity(int gravity) {
        final int vgrav = gravity & Gravity.VERTICAL_GRAVITY_MASK;
        switch (vgrav) {
            case Gravity.TOP:
            case Gravity.BOTTOM:
            case Gravity.CENTER_VERTICAL:
                return vgrav;
            default:
                return mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        }
    }

    /**
     * Prepare a list of non-SYSTEM child views. If the layout direction is RTL
     * this will be in reverse child order.
     *
     * @param views List to populate. It will be cleared before use.
     * @param gravity Horizontal gravity to match against
     */
    private void addCustomViewsWithGravity(List<View> views, int gravity) {
        final boolean isRtl =  ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
        final int childCount = getChildCount();
        final int absGrav = GravityCompat.getAbsoluteGravity(gravity,
                ViewCompat.getLayoutDirection(this));

        views.clear();

        if (isRtl) {
            for (int i = childCount - 1; i >= 0; i--) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.mViewType == LayoutParams.CUSTOM && shouldLayout(child) &&
                        getChildHorizontalGravity(lp.gravity) == absGrav) {
                    views.add(child);
                }
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.mViewType == LayoutParams.CUSTOM && shouldLayout(child) &&
                        getChildHorizontalGravity(lp.gravity) == absGrav) {
                    views.add(child);
                }
            }
        }
    }

    private int getChildHorizontalGravity(int gravity) {
        final int ld =  ViewCompat.getLayoutDirection(this);
        final int absGrav = GravityCompat.getAbsoluteGravity(gravity, ld);
        final int hGrav = absGrav & Gravity.HORIZONTAL_GRAVITY_MASK;
        switch (hGrav) {
            case Gravity.LEFT:
            case Gravity.RIGHT:
            case Gravity.CENTER_HORIZONTAL:
                return hGrav;
            default:
                return ld == ViewCompat.LAYOUT_DIRECTION_RTL ? Gravity.RIGHT : Gravity.LEFT;
        }
    }

    private boolean shouldLayout(View view) {
        return view != null && view.getParent() == this && view.getVisibility() != GONE;
    }

    private int getHorizontalMargins(View v) {
        final MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        return MarginLayoutParamsCompat.getMarginStart(mlp) +
                MarginLayoutParamsCompat.getMarginEnd(mlp);
    }

    private int getVerticalMargins(View v) {
        final MarginLayoutParams mlp = (MarginLayoutParams) v.getLayoutParams();
        return mlp.topMargin + mlp.bottomMargin;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) p);
        } else if (p instanceof ActionBar.LayoutParams) {
            return new LayoutParams((ActionBar.LayoutParams) p);
        } else if (p instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) p);
        } else {
            return new LayoutParams(p);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return super.checkLayoutParams(p) && p instanceof LayoutParams;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public DecorToolbar getWrapper() {
        if (mWrapper == null) {
            mWrapper = new ToolbarWidgetWrapper(this, true);
        }
        return mWrapper;
    }

    void removeChildrenForExpandedActionView() {
        final int childCount = getChildCount();
        // Go backwards since we're removing from the list
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.mViewType != LayoutParams.EXPANDED && child != mMenuView) {
                removeViewAt(i);
                mHiddenViews.add(child);
            }
        }
    }

    void addChildrenForExpandedActionView() {
        final int count = mHiddenViews.size();
        // Re-add in reverse order since we removed in reverse order
        for (int i = count - 1; i >= 0; i--) {
            addView(mHiddenViews.get(i));
        }
        mHiddenViews.clear();
    }

    private boolean isChildOrHidden(View child) {
        return child.getParent() == this || mHiddenViews.contains(child);
    }

    /**
     * Force the toolbar to collapse to zero-height during measurement if
     * it could be considered "empty" (no visible elements with nonzero measured size)
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setCollapsible(boolean collapsible) {
        mCollapsible = collapsible;
        requestLayout();
    }

    /**
     * Must be called before the menu is accessed
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void setMenuCallbacks(MenuPresenter.Callback pcb, MenuBuilder.Callback mcb) {
        mActionMenuPresenterCallback = pcb;
        mMenuBuilderCallback = mcb;
        if (mMenuView != null) {
            mMenuView.setMenuCallbacks(pcb, mcb);
        }
    }

    private void ensureContentInsets() {
        if (mContentInsets == null) {
            mContentInsets = new RtlSpacingHelper();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(TESTS)
    @Nullable
    final TextView getTitleTextView() {
        return mTitleTextView;
    }

    /**
     * @hide
     */
    @RestrictTo(TESTS)
    @Nullable
    final TextView getSubtitleTextView() {
        return mSubtitleTextView;
    }

    /**
     * Accessor to enable LayoutLib to get ActionMenuPresenter directly.
     */
    ActionMenuPresenter getOuterActionMenuPresenter() {
        return mOuterActionMenuPresenter;
    }

    Context getPopupContext() {
        return mPopupContext;
    }

    private ArrayList<MenuItem> getCurrentMenuItems() {
        ArrayList<MenuItem> menuItems = new ArrayList<>();

        Menu menu = getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menuItems.add(menu.getItem(i));
        }

        return menuItems;
    }

    private void onCreateMenu() {
        Menu menu = getMenu();
        ArrayList<MenuItem> oldMenuItemList = getCurrentMenuItems();
        mMenuHostHelper.onCreateMenu(menu, getMenuInflater());

        ArrayList<MenuItem> newMenuItemList = getCurrentMenuItems();
        newMenuItemList.removeAll(oldMenuItemList);
        mProvidedMenuItems = newMenuItemList;

        mMenuHostHelper.onPrepareMenu(menu);
    }

    @Override
    @MainThread
    public void addMenuProvider(@NonNull MenuProvider provider) {
        mMenuHostHelper.addMenuProvider(provider);
    }

    @Override
    @MainThread
    public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner) {
        mMenuHostHelper.addMenuProvider(provider, owner);
    }

    @Override
    @MainThread
    @SuppressLint("LambdaLast")
    public void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner,
            @NonNull Lifecycle.State state) {
        mMenuHostHelper.addMenuProvider(provider, owner, state);
    }

    @Override
    @MainThread
    public void removeMenuProvider(@NonNull MenuProvider provider) {
        mMenuHostHelper.removeMenuProvider(provider);
    }

    /**
     * {@inheritDoc}
     *
     * Only the {@link MenuItem items} in the {@link Menu} that were provided by
     * {@link MenuProvider}s should be removed and repopulated, leaving all manually
     * inflated menu items untouched, as they should continue to be managed manually.
     */
    @Override
    @MainThread
    public void invalidateMenu() {
        for (MenuItem menuItem : mProvidedMenuItems) {
            getMenu().removeItem(menuItem.getItemId());
        }
        onCreateMenu();
    }

    /**
     * Call this method whenever a property changes that affects whether the view will handle a
     * back press, which is the combination of {@link #hasExpandedActionView()} and properties that
     * affect whether this view would normally receive key press events.
     */
    void updateBackInvokedCallbackState() {
        if (Build.VERSION.SDK_INT >= 33) {
            OnBackInvokedDispatcher currentDispatcher =
                    Api33Impl.findOnBackInvokedDispatcher(this);
            boolean shouldBeRegistered = hasExpandedActionView()
                    && currentDispatcher != null
                    && ViewCompat.isAttachedToWindow(this)
                    && mBackInvokedCallbackEnabled;

            if (shouldBeRegistered && mBackInvokedDispatcher == null) {
                if (mBackInvokedCallback == null) {
                    mBackInvokedCallback = Api33Impl.newOnBackInvokedCallback(
                            this::collapseActionView);
                }
                Api33Impl.tryRegisterOnBackInvokedCallback(
                        currentDispatcher, mBackInvokedCallback);
                mBackInvokedDispatcher = currentDispatcher;
            } else if (!shouldBeRegistered && mBackInvokedDispatcher != null) {
                Api33Impl.tryUnregisterOnBackInvokedCallback(
                        mBackInvokedDispatcher, mBackInvokedCallback);
                mBackInvokedDispatcher = null;
            }
        }
    }

    /**
     * Interface responsible for receiving menu item click events if the items themselves
     * do not have individual item click listeners.
     */
    public interface OnMenuItemClickListener {
        /**
         * This method will be invoked when a menu item is clicked if the item itself did
         * not already handle the event.
         *
         * @param item {@link MenuItem} that was clicked
         * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
         */
        public boolean onMenuItemClick(MenuItem item);
    }

    /**
     * Layout information for child views of Toolbars.
     *
     * <p>Toolbar.LayoutParams extends ActionBar.LayoutParams for compatibility with existing
     * ActionBar API. See
     * {@link androidx.appcompat.app.AppCompatActivity#setSupportActionBar(Toolbar)
     * AppCompatActivity.setSupportActionBar}
     * for more info on how to use a Toolbar as your Activity's ActionBar.</p>
     */
    public static class LayoutParams extends ActionBar.LayoutParams {
        static final int CUSTOM = 0;
        static final int SYSTEM = 1;
        static final int EXPANDED = 2;

        int mViewType = CUSTOM;

        public LayoutParams(@NonNull Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = Gravity.CENTER_VERTICAL | GravityCompat.START;
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(int gravity) {
            this(WRAP_CONTENT, MATCH_PARENT, gravity);
        }

        public LayoutParams(LayoutParams source) {
            super(source);

            mViewType = source.mViewType;
        }

        public LayoutParams(ActionBar.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            // ActionBar.LayoutParams doesn't have a MarginLayoutParams constructor.
            // Fake it here and copy over the relevant data.
            copyMarginsFromCompat(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        void copyMarginsFromCompat(MarginLayoutParams source) {
            this.leftMargin = source.leftMargin;
            this.topMargin = source.topMargin;
            this.rightMargin = source.rightMargin;
            this.bottomMargin = source.bottomMargin;
        }
    }

    public static class SavedState extends AbsSavedState {
        int expandedMenuItemId;
        boolean isOverflowOpen;

        public SavedState(Parcel source) {
            this(source, null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            expandedMenuItemId = source.readInt();
            isOverflowOpen = source.readInt() != 0;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(expandedMenuItemId);
            out.writeInt(isOverflowOpen ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private class ExpandedActionViewMenuPresenter implements MenuPresenter {
        MenuBuilder mMenu;
        MenuItemImpl mCurrentExpandedItem;

        ExpandedActionViewMenuPresenter() {
        }

        @Override
        public void initForMenu(Context context, MenuBuilder menu) {
            // Clear the expanded action view when menus change.
            if (mMenu != null && mCurrentExpandedItem != null) {
                mMenu.collapseItemActionView(mCurrentExpandedItem);
            }
            mMenu = menu;
        }

        @Override
        public MenuView getMenuView(ViewGroup root) {
            return null;
        }

        @Override
        public void updateMenuView(boolean cleared) {
            // Make sure the expanded item we have is still there.
            if (mCurrentExpandedItem != null) {
                boolean found = false;

                if (mMenu != null) {
                    final int count = mMenu.size();
                    for (int i = 0; i < count; i++) {
                        final MenuItem item = mMenu.getItem(i);
                        if (item == mCurrentExpandedItem) {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    // The item we had expanded disappeared. Collapse.
                    collapseItemActionView(mMenu, mCurrentExpandedItem);
                }
            }
        }

        @Override
        public void setCallback(Callback cb) {
        }

        @Override
        public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        @Override
        public boolean flagActionItems() {
            return false;
        }

        @Override
        public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
            ensureCollapseButtonView();
            ViewParent collapseButtonParent = mCollapseButtonView.getParent();
            if (collapseButtonParent != Toolbar.this) {
                if (collapseButtonParent instanceof ViewGroup) {
                    ((ViewGroup) collapseButtonParent).removeView(mCollapseButtonView);
                }
                addView(mCollapseButtonView);
            }
            mExpandedActionView = item.getActionView();
            mCurrentExpandedItem = item;
            ViewParent expandedActionParent = mExpandedActionView.getParent();
            if (expandedActionParent != Toolbar.this) {
                if (expandedActionParent instanceof ViewGroup) {
                    ((ViewGroup) expandedActionParent).removeView(mExpandedActionView);
                }
                final LayoutParams lp = generateDefaultLayoutParams();
                lp.gravity = GravityCompat.START | (mButtonGravity & Gravity.VERTICAL_GRAVITY_MASK);
                lp.mViewType = LayoutParams.EXPANDED;
                mExpandedActionView.setLayoutParams(lp);
                addView(mExpandedActionView);
            }

            removeChildrenForExpandedActionView();
            requestLayout();
            item.setActionViewExpanded(true);

            if (mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) mExpandedActionView).onActionViewExpanded();
            }

            // mCurrentExpandedItem has changed.
            updateBackInvokedCallbackState();

            return true;
        }

        @Override
        public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
            // Do this before detaching the actionview from the hierarchy, in case
            // it needs to dismiss the soft keyboard, etc.
            if (mExpandedActionView instanceof CollapsibleActionView) {
                ((CollapsibleActionView) mExpandedActionView).onActionViewCollapsed();
            }

            removeView(mExpandedActionView);
            removeView(mCollapseButtonView);
            mExpandedActionView = null;

            addChildrenForExpandedActionView();
            mCurrentExpandedItem = null;
            requestLayout();
            item.setActionViewExpanded(false);

            // mCurrentExpandedItem has changed.
            updateBackInvokedCallbackState();

            return true;
        }

        @Override
        public int getId() {
            return 0;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            return null;
        }

        @Override
        public void onRestoreInstanceState(Parcelable state) {
        }
    }

    @RequiresApi(33)
    static class Api33Impl {
        private Api33Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void tryRegisterOnBackInvokedCallback(@NonNull Object dispatcherObj,
                @NonNull Object callback) {
            OnBackInvokedDispatcher dispatcher = (OnBackInvokedDispatcher) dispatcherObj;
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                    (OnBackInvokedCallback) callback);
        }

        @DoNotInline
        static void tryUnregisterOnBackInvokedCallback(@NonNull Object dispatcherObj,
                @NonNull Object callbackObj) {
            OnBackInvokedDispatcher dispatcher = (OnBackInvokedDispatcher) dispatcherObj;
            dispatcher.unregisterOnBackInvokedCallback((OnBackInvokedCallback) callbackObj);
        }

        @Nullable
        @DoNotInline
        static OnBackInvokedDispatcher findOnBackInvokedDispatcher(@NonNull View view) {
            return view.findOnBackInvokedDispatcher();
        }

        @NonNull
        @DoNotInline
        static OnBackInvokedCallback newOnBackInvokedCallback(@NonNull Runnable action) {
            return action::run;
        }
    }
}
