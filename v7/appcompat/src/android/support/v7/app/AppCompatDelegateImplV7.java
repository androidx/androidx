/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v4.view.LayoutInflaterFactory;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.WindowCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.PopupWindowCompat;
import android.support.v7.appcompat.R;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.view.StandaloneActionMode;
import android.support.v7.view.menu.ListMenuPresenter;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPresenter;
import android.support.v7.view.menu.MenuView;
import android.support.v7.widget.ActionBarContextView;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.ContentFrameLayout;
import android.support.v7.widget.DecorContentParent;
import android.support.v7.widget.FitWindowsViewGroup;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.ViewStubCompat;
import android.support.v7.widget.ViewUtils;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.Window.FEATURE_OPTIONS_PANEL;

class AppCompatDelegateImplV7 extends AppCompatDelegateImplBase
        implements MenuBuilder.Callback, LayoutInflaterFactory {

    private DecorContentParent mDecorContentParent;
    private ActionMenuPresenterCallback mActionMenuPresenterCallback;
    private PanelMenuPresenterCallback mPanelMenuPresenterCallback;

    ActionMode mActionMode;
    ActionBarContextView mActionModeView;
    PopupWindow mActionModePopup;
    Runnable mShowActionModePopup;
    ViewPropertyAnimatorCompat mFadeAnim = null;

    // true if we have installed a window sub-decor layout.
    private boolean mSubDecorInstalled;
    private ViewGroup mWindowDecor;
    private ViewGroup mSubDecor;

    private TextView mTitleView;
    private View mStatusGuard;

    // Used to keep track of Progress Bar Window features
    private boolean mFeatureProgress, mFeatureIndeterminateProgress;

    // Used for emulating PanelFeatureState
    private boolean mClosingActionMenu;
    private PanelFeatureState[] mPanels;
    private PanelFeatureState mPreparedPanel;

    private boolean mLongPressBackDown;

    private boolean mInvalidatePanelMenuPosted;
    private int mInvalidatePanelMenuFeatures;
    private final Runnable mInvalidatePanelMenuRunnable = new Runnable() {
        @Override
        public void run() {
            if ((mInvalidatePanelMenuFeatures & 1 << FEATURE_OPTIONS_PANEL) != 0) {
                doInvalidatePanelMenu(FEATURE_OPTIONS_PANEL);
            }
            if ((mInvalidatePanelMenuFeatures & 1 << FEATURE_SUPPORT_ACTION_BAR) != 0) {
                doInvalidatePanelMenu(FEATURE_SUPPORT_ACTION_BAR);
            }
            mInvalidatePanelMenuPosted = false;
            mInvalidatePanelMenuFeatures = 0;
        }
    };

    private boolean mEnableDefaultActionBarUp;

    private Rect mTempRect1;
    private Rect mTempRect2;

    private AppCompatViewInflater mAppCompatViewInflater;

    AppCompatDelegateImplV7(Context context, Window window, AppCompatCallback callback) {
        super(context, window, callback);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mWindowDecor = (ViewGroup) mWindow.getDecorView();

        if (mOriginalWindowCallback instanceof Activity) {
            if (NavUtils.getParentActivityName((Activity) mOriginalWindowCallback) != null) {
                // Peek at the Action Bar and update it if it already exists
                ActionBar ab = peekSupportActionBar();
                if (ab == null) {
                    mEnableDefaultActionBarUp = true;
                } else {
                    ab.setDefaultDisplayHomeAsUpEnabled(true);
                }
            }
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        // Make sure that the sub decor is installed
        ensureSubDecor();
    }

    @Override
    public void initWindowDecorActionBar() {
        ensureSubDecor();

        if (!mHasActionBar || mActionBar != null) {
            return;
        }

        if (mOriginalWindowCallback instanceof Activity) {
            mActionBar = new WindowDecorActionBar((Activity) mOriginalWindowCallback,
                    mOverlayActionBar);
        } else if (mOriginalWindowCallback instanceof Dialog) {
            mActionBar = new WindowDecorActionBar((Dialog) mOriginalWindowCallback);
        }
        if (mActionBar != null) {
            mActionBar.setDefaultDisplayHomeAsUpEnabled(mEnableDefaultActionBarUp);
        }
    }

    @Override
    public void setSupportActionBar(Toolbar toolbar) {
        if (!(mOriginalWindowCallback instanceof Activity)) {
            // Only Activities support custom Action Bars
            return;
        }

        final ActionBar ab = getSupportActionBar();
        if (ab instanceof WindowDecorActionBar) {
            throw new IllegalStateException("This Activity already has an action bar supplied " +
                    "by the window decor. Do not request Window.FEATURE_SUPPORT_ACTION_BAR and set " +
                    "windowActionBar to false in your theme to use a Toolbar instead.");
        }
        // Clear out the MenuInflater to make sure that it is valid for the new Action Bar
        mMenuInflater = null;

        ToolbarActionBar tbab = new ToolbarActionBar(toolbar, ((Activity) mContext).getTitle(),
                mAppCompatWindowCallback);
        mActionBar = tbab;
        mWindow.setCallback(tbab.getWrappedWindowCallback());
        tbab.invalidateOptionsMenu();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // If this is called before sub-decor is installed, ActionBar will not
        // be properly initialized.
        if (mHasActionBar && mSubDecorInstalled) {
            // Note: The action bar will need to access
            // view changes from superclass.
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.onConfigurationChanged(newConfig);
            }
        }
    }

    @Override
    public void onStop() {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setShowHideAnimationEnabled(false);
        }
    }

    @Override
    public void onPostResume() {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setShowHideAnimationEnabled(true);
        }
    }

    @Override
    public void setContentView(View v) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v);
        mOriginalWindowCallback.onContentChanged();
    }

    @Override
    public void setContentView(int resId) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        LayoutInflater.from(mContext).inflate(resId, contentParent);
        mOriginalWindowCallback.onContentChanged();
    }

    @Override
    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v, lp);
        mOriginalWindowCallback.onContentChanged();
    }

    @Override
    public void addContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mSubDecor.findViewById(android.R.id.content);
        contentParent.addView(v, lp);
        mOriginalWindowCallback.onContentChanged();
    }

    private void ensureSubDecor() {
        if (!mSubDecorInstalled) {
            mSubDecor = createSubDecor();

            // If a title was set before we installed the decor, propogate it now
            CharSequence title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                onTitleChanged(title);
            }

            applyFixedSizeWindow();

            onSubDecorInstalled(mSubDecor);

            mSubDecorInstalled = true;

            // Invalidate if the panel menu hasn't been created before this.
            // Panel menu invalidation is deferred avoiding application onCreateOptionsMenu
            // being called in the middle of onCreate or similar.
            // A pending invalidation will typically be resolved before the posted message
            // would run normally in order to satisfy instance state restoration.
            PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
            if (!isDestroyed() && (st == null || st.menu == null)) {
                invalidatePanelMenu(FEATURE_SUPPORT_ACTION_BAR);
            }
        }
    }

    private ViewGroup createSubDecor() {
        TypedArray a = mContext.obtainStyledAttributes(R.styleable.Theme);

        if (!a.hasValue(R.styleable.Theme_windowActionBar)) {
            a.recycle();
            throw new IllegalStateException(
                    "You need to use a Theme.AppCompat theme (or descendant) with this activity.");
        }

        if (a.getBoolean(R.styleable.Theme_windowNoTitle, false)) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else if (a.getBoolean(R.styleable.Theme_windowActionBar, false)) {
            // Don't allow an action bar if there is no title.
            requestWindowFeature(FEATURE_SUPPORT_ACTION_BAR);
        }
        if (a.getBoolean(R.styleable.Theme_windowActionBarOverlay, false)) {
            requestWindowFeature(FEATURE_SUPPORT_ACTION_BAR_OVERLAY);
        }
        if (a.getBoolean(R.styleable.Theme_windowActionModeOverlay, false)) {
            requestWindowFeature(FEATURE_ACTION_MODE_OVERLAY);
        }
        mIsFloating = a.getBoolean(R.styleable.Theme_android_windowIsFloating, false);
        a.recycle();

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup subDecor = null;


        if (!mWindowNoTitle) {
            if (mIsFloating) {
                // If we're floating, inflate the dialog title decor
                subDecor = (ViewGroup) inflater.inflate(
                        R.layout.abc_dialog_title_material, null);

                // Floating windows can never have an action bar, reset the flags
                mHasActionBar = mOverlayActionBar = false;
            } else if (mHasActionBar) {
                /**
                 * This needs some explanation. As we can not use the android:theme attribute
                 * pre-L, we emulate it by manually creating a LayoutInflater using a
                 * ContextThemeWrapper pointing to actionBarTheme.
                 */
                TypedValue outValue = new TypedValue();
                mContext.getTheme().resolveAttribute(R.attr.actionBarTheme, outValue, true);

                Context themedContext;
                if (outValue.resourceId != 0) {
                    themedContext = new ContextThemeWrapper(mContext, outValue.resourceId);
                } else {
                    themedContext = mContext;
                }

                // Now inflate the view using the themed context and set it as the content view
                subDecor = (ViewGroup) LayoutInflater.from(themedContext)
                        .inflate(R.layout.abc_screen_toolbar, null);

                mDecorContentParent = (DecorContentParent) subDecor
                        .findViewById(R.id.decor_content_parent);
                mDecorContentParent.setWindowCallback(getWindowCallback());

                /**
                 * Propagate features to DecorContentParent
                 */
                if (mOverlayActionBar) {
                    mDecorContentParent.initFeature(FEATURE_SUPPORT_ACTION_BAR_OVERLAY);
                }
                if (mFeatureProgress) {
                    mDecorContentParent.initFeature(Window.FEATURE_PROGRESS);
                }
                if (mFeatureIndeterminateProgress) {
                    mDecorContentParent.initFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                }
            }
        } else {
            if (mOverlayActionMode) {
                subDecor = (ViewGroup) inflater.inflate(
                        R.layout.abc_screen_simple_overlay_action_mode, null);
            } else {
                subDecor = (ViewGroup) inflater.inflate(R.layout.abc_screen_simple, null);
            }

            if (Build.VERSION.SDK_INT >= 21) {
                // If we're running on L or above, we can rely on ViewCompat's
                // setOnApplyWindowInsetsListener
                ViewCompat.setOnApplyWindowInsetsListener(subDecor,
                        new OnApplyWindowInsetsListener() {
                            @Override
                            public WindowInsetsCompat onApplyWindowInsets(View v,
                                    WindowInsetsCompat insets) {
                                final int top = insets.getSystemWindowInsetTop();
                                final int newTop = updateStatusGuard(top);

                                if (top != newTop) {
                                    insets = insets.replaceSystemWindowInsets(
                                            insets.getSystemWindowInsetLeft(),
                                            newTop,
                                            insets.getSystemWindowInsetRight(),
                                            insets.getSystemWindowInsetBottom());
                                }

                                // Now apply the insets on our view
                                return ViewCompat.onApplyWindowInsets(v, insets);
                            }
                        });
            } else {
                // Else, we need to use our own FitWindowsViewGroup handling
                ((FitWindowsViewGroup) subDecor).setOnFitSystemWindowsListener(
                        new FitWindowsViewGroup.OnFitSystemWindowsListener() {
                            @Override
                            public void onFitSystemWindows(Rect insets) {
                                insets.top = updateStatusGuard(insets.top);
                            }
                        });
            }
        }

        if (subDecor == null) {
            throw new IllegalArgumentException(
                    "AppCompat does not support the current theme features: { "
                            + "windowActionBar: " + mHasActionBar
                            + ", windowActionBarOverlay: "+ mOverlayActionBar
                            + ", android:windowIsFloating: " + mIsFloating
                            + ", windowActionModeOverlay: " + mOverlayActionMode
                            + ", windowNoTitle: " + mWindowNoTitle
                            + " }");
        }

        if (mDecorContentParent == null) {
            mTitleView = (TextView) subDecor.findViewById(R.id.title);
        }

        // Make the decor optionally fit system windows, like the window's decor
        ViewUtils.makeOptionalFitsSystemWindows(subDecor);

        final ViewGroup decorContent = (ViewGroup) mWindow.findViewById(android.R.id.content);
        final ContentFrameLayout abcContent = (ContentFrameLayout) subDecor.findViewById(
                R.id.action_bar_activity_content);

        // There might be Views already added to the Window's content view so we need to
        // migrate them to our content view
        while (decorContent.getChildCount() > 0) {
            final View child = decorContent.getChildAt(0);
            decorContent.removeViewAt(0);
            abcContent.addView(child);
        }

        // Now set the Window's content view with the decor
        mWindow.setContentView(subDecor);

        // Change our content FrameLayout to use the android.R.id.content id.
        // Useful for fragments.
        decorContent.setId(View.NO_ID);
        abcContent.setId(android.R.id.content);

        // The decorContent may have a foreground drawable set (windowContentOverlay).
        // Remove this as we handle it ourselves
        if (decorContent instanceof FrameLayout) {
            ((FrameLayout) decorContent).setForeground(null);
        }

        abcContent.setAttachListener(new ContentFrameLayout.OnAttachListener() {
            @Override
            public void onAttachedFromWindow() {}

            @Override
            public void onDetachedFromWindow() {
                dismissPopups();
            }
        });

        return subDecor;
    }

    void onSubDecorInstalled(ViewGroup subDecor) {}

    private void applyFixedSizeWindow() {
        ContentFrameLayout cfl = (ContentFrameLayout) mSubDecor.findViewById(android.R.id.content);

        // This is a bit weird. In the framework, the window sizing attributes control
        // the decor view's size, meaning that any padding is inset for the min/max widths below.
        // We don't control measurement at that level, so we need to workaround it by making sure
        // that the decor view's padding is taken into account.
        cfl.setDecorPadding(mWindowDecor.getPaddingLeft(),
                mWindowDecor.getPaddingTop(), mWindowDecor.getPaddingRight(),
                mWindowDecor.getPaddingBottom());

        TypedArray a = mContext.obtainStyledAttributes(R.styleable.Theme);
        a.getValue(R.styleable.Theme_windowMinWidthMajor, cfl.getMinWidthMajor());
        a.getValue(R.styleable.Theme_windowMinWidthMinor, cfl.getMinWidthMinor());

        if (a.hasValue(R.styleable.Theme_windowFixedWidthMajor)) {
            a.getValue(R.styleable.Theme_windowFixedWidthMajor, cfl.getFixedWidthMajor());
        }
        if (a.hasValue(R.styleable.Theme_windowFixedWidthMinor)) {
            a.getValue(R.styleable.Theme_windowFixedWidthMinor, cfl.getFixedWidthMinor());
        }
        if (a.hasValue(R.styleable.Theme_windowFixedHeightMajor)) {
            a.getValue(R.styleable.Theme_windowFixedHeightMajor, cfl.getFixedHeightMajor());
        }
        if (a.hasValue(R.styleable.Theme_windowFixedHeightMinor)) {
            a.getValue(R.styleable.Theme_windowFixedHeightMinor, cfl.getFixedHeightMinor());
        }
        a.recycle();

        cfl.requestLayout();
    }

    @Override
    public boolean requestWindowFeature(int featureId) {
        featureId = sanitizeWindowFeatureId(featureId);

        if (mWindowNoTitle && featureId == FEATURE_SUPPORT_ACTION_BAR) {
            return false; // Ignore. No title dominates.
        }
        if (mHasActionBar && featureId == Window.FEATURE_NO_TITLE) {
            // Remove the action bar feature if we have no title. No title dominates.
            mHasActionBar = false;
        }

        switch (featureId) {
            case FEATURE_SUPPORT_ACTION_BAR:
                throwFeatureRequestIfSubDecorInstalled();
                mHasActionBar = true;
                return true;
            case FEATURE_SUPPORT_ACTION_BAR_OVERLAY:
                throwFeatureRequestIfSubDecorInstalled();
                mOverlayActionBar = true;
                return true;
            case FEATURE_ACTION_MODE_OVERLAY:
                throwFeatureRequestIfSubDecorInstalled();
                mOverlayActionMode = true;
                return true;
            case Window.FEATURE_PROGRESS:
                throwFeatureRequestIfSubDecorInstalled();
                mFeatureProgress = true;
                return true;
            case Window.FEATURE_INDETERMINATE_PROGRESS:
                throwFeatureRequestIfSubDecorInstalled();
                mFeatureIndeterminateProgress = true;
                return true;
            case Window.FEATURE_NO_TITLE:
                throwFeatureRequestIfSubDecorInstalled();
                mWindowNoTitle = true;
                return true;
        }

        return mWindow.requestFeature(featureId);
    }

    @Override
    public boolean hasWindowFeature(int featureId) {
        featureId = sanitizeWindowFeatureId(featureId);
        switch (featureId) {
            case FEATURE_SUPPORT_ACTION_BAR:
                return mHasActionBar;
            case FEATURE_SUPPORT_ACTION_BAR_OVERLAY:
                return mOverlayActionBar;
            case FEATURE_ACTION_MODE_OVERLAY:
                return mOverlayActionMode;
            case Window.FEATURE_PROGRESS:
                return mFeatureProgress;
            case Window.FEATURE_INDETERMINATE_PROGRESS:
                return mFeatureIndeterminateProgress;
            case Window.FEATURE_NO_TITLE:
                return mWindowNoTitle;
        }
        return mWindow.hasFeature(featureId);
    }

    @Override
    void onTitleChanged(CharSequence title) {
        if (mDecorContentParent != null) {
            mDecorContentParent.setWindowTitle(title);
        } else if (peekSupportActionBar() != null) {
            peekSupportActionBar().setWindowTitle(title);
        } else if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }

    @Override
    void onPanelClosed(final int featureId, Menu menu) {
        if (featureId == FEATURE_SUPPORT_ACTION_BAR) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.dispatchMenuVisibilityChanged(false);
            }
        } else if (featureId == FEATURE_OPTIONS_PANEL) {
            // Make sure that the options panel is closed. This is mainly used when we're using a
            // ToolbarActionBar
            PanelFeatureState st = getPanelState(featureId, true);
            if (st.isOpen) {
                closePanel(st, false);
            }
        }
    }

    @Override
    boolean onMenuOpened(final int featureId, Menu menu) {
        if (featureId == FEATURE_SUPPORT_ACTION_BAR) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.dispatchMenuVisibilityChanged(true);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        final Window.Callback cb = getWindowCallback();
        if (cb != null && !isDestroyed()) {
            final PanelFeatureState panel = findMenuPanel(menu.getRootMenu());
            if (panel != null) {
                return cb.onMenuItemSelected(panel.featureId, item);
            }
        }
        return false;
    }

    @Override
    public void onMenuModeChange(MenuBuilder menu) {
        reopenMenu(menu, true);
    }

    @Override
    public ActionMode startSupportActionMode(ActionMode.Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("ActionMode callback can not be null.");
        }

        if (mActionMode != null) {
            mActionMode.finish();
        }

        final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapperV7(callback);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            mActionMode = ab.startActionMode(wrappedCallback);
            if (mActionMode != null && mAppCompatCallback != null) {
                mAppCompatCallback.onSupportActionModeStarted(mActionMode);
            }
        }

        if (mActionMode == null) {
            // If the action bar didn't provide an action mode, start the emulated window one
            mActionMode = startSupportActionModeFromWindow(wrappedCallback);
        }

        return mActionMode;
    }

    @Override
    public void invalidateOptionsMenu() {
        final ActionBar ab = getSupportActionBar();
        if (ab != null && ab.invalidateOptionsMenu()) return;

        invalidatePanelMenu(FEATURE_OPTIONS_PANEL);
    }

    @Override
    ActionMode startSupportActionModeFromWindow(ActionMode.Callback callback) {
        endOnGoingFadeAnimation();
        if (mActionMode != null) {
            mActionMode.finish();
        }

        final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapperV7(callback);
        ActionMode mode = null;
        if (mAppCompatCallback != null && !isDestroyed()) {
            try {
                mode = mAppCompatCallback.onWindowStartingSupportActionMode(wrappedCallback);
            } catch (AbstractMethodError ame) {
                // Older apps might not implement this callback method.
            }
        }

        if (mode != null) {
            mActionMode = mode;
        } else {
            if (mActionModeView == null) {
                if (mIsFloating) {
                    // Use the action bar theme.
                    final TypedValue outValue = new TypedValue();
                    final Resources.Theme baseTheme = mContext.getTheme();
                    baseTheme.resolveAttribute(R.attr.actionBarTheme, outValue, true);

                    final Context actionBarContext;
                    if (outValue.resourceId != 0) {
                        final Resources.Theme actionBarTheme = mContext.getResources().newTheme();
                        actionBarTheme.setTo(baseTheme);
                        actionBarTheme.applyStyle(outValue.resourceId, true);

                        actionBarContext = new ContextThemeWrapper(mContext, 0);
                        actionBarContext.getTheme().setTo(actionBarTheme);
                    } else {
                        actionBarContext = mContext;
                    }

                    mActionModeView = new ActionBarContextView(actionBarContext);
                    mActionModePopup = new PopupWindow(actionBarContext, null,
                            R.attr.actionModePopupWindowStyle);
                    PopupWindowCompat.setWindowLayoutType(mActionModePopup,
                            WindowManager.LayoutParams.TYPE_APPLICATION);
                    mActionModePopup.setContentView(mActionModeView);
                    mActionModePopup.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);

                    actionBarContext.getTheme().resolveAttribute(
                            R.attr.actionBarSize, outValue, true);
                    final int height = TypedValue.complexToDimensionPixelSize(outValue.data,
                            actionBarContext.getResources().getDisplayMetrics());
                    mActionModeView.setContentHeight(height);
                    mActionModePopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                    mShowActionModePopup = new Runnable() {
                        public void run() {
                            mActionModePopup.showAtLocation(
                                    mActionModeView,
                                    Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
                            endOnGoingFadeAnimation();
                            ViewCompat.setAlpha(mActionModeView, 0f);
                            mFadeAnim = ViewCompat.animate(mActionModeView).alpha(1f);
                            mFadeAnim.setListener(new ViewPropertyAnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(View view) {
                                    ViewCompat.setAlpha(mActionModeView, 1f);
                                    mFadeAnim.setListener(null);
                                    mFadeAnim = null;
                                }

                                @Override
                                public void onAnimationStart(View view) {
                                    mActionModeView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    };
                } else {
                    ViewStubCompat stub = (ViewStubCompat) mSubDecor
                            .findViewById(R.id.action_mode_bar_stub);
                    if (stub != null) {
                        // Set the layout inflater so that it is inflated with the action bar's context
                        stub.setLayoutInflater(LayoutInflater.from(getActionBarThemedContext()));
                        mActionModeView = (ActionBarContextView) stub.inflate();
                    }
                }
            }

            if (mActionModeView != null) {
                endOnGoingFadeAnimation();
                mActionModeView.killMode();
                mode = new StandaloneActionMode(mActionModeView.getContext(), mActionModeView,
                        wrappedCallback, mActionModePopup == null);
                if (callback.onCreateActionMode(mode, mode.getMenu())) {
                    mode.invalidate();
                    mActionModeView.initForMode(mode);
                    mActionMode = mode;
                    ViewCompat.setAlpha(mActionModeView, 0f);
                    mFadeAnim = ViewCompat.animate(mActionModeView).alpha(1f);
                    mFadeAnim.setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            ViewCompat.setAlpha(mActionModeView, 1f);
                            mFadeAnim.setListener(null);
                            mFadeAnim = null;
                        }

                        @Override
                        public void onAnimationStart(View view) {
                            mActionModeView.setVisibility(View.VISIBLE);
                            mActionModeView.sendAccessibilityEvent(
                                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                            if (mActionModeView.getParent() != null) {
                                ViewCompat.requestApplyInsets((View) mActionModeView.getParent());
                            }
                        }
                    });
                    if (mActionModePopup != null) {
                        mWindow.getDecorView().post(mShowActionModePopup);
                    }
                } else {
                    mActionMode = null;
                }
            }
        }
        if (mActionMode != null && mAppCompatCallback != null) {
            mAppCompatCallback.onSupportActionModeStarted(mActionMode);
        }
        return mActionMode;
    }

    private void endOnGoingFadeAnimation() {
        if (mFadeAnim != null) {
            mFadeAnim.cancel();
        }
    }

    boolean onBackPressed() {
        // Back cancels action modes first.
        if (mActionMode != null) {
            mActionMode.finish();
            return true;
        }

        // Next collapse any expanded action views.
        ActionBar ab = getSupportActionBar();
        if (ab != null && ab.collapseActionView()) {
            return true;
        }

        // Let the call through...
        return false;
    }

    @Override
    boolean onKeyShortcut(int keyCode, KeyEvent ev) {
        // Let the Action Bar have a chance at handling the shortcut
        ActionBar ab = getSupportActionBar();
        if (ab != null && ab.onKeyShortcut(keyCode, ev)) {
            return true;
        }

        // If the panel is already prepared, then perform the shortcut using it.
        boolean handled;
        if (mPreparedPanel != null) {
            handled = performPanelShortcut(mPreparedPanel, ev.getKeyCode(), ev,
                    Menu.FLAG_PERFORM_NO_CLOSE);
            if (handled) {
                if (mPreparedPanel != null) {
                    mPreparedPanel.isHandled = true;
                }
                return true;
            }
        }

        // If the panel is not prepared, then we may be trying to handle a shortcut key
        // combination such as Control+C.  Temporarily prepare the panel then mark it
        // unprepared again when finished to ensure that the panel will again be prepared
        // the next time it is shown for real.
        if (mPreparedPanel == null) {
            PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, true);
            preparePanel(st, ev);
            handled = performPanelShortcut(st, ev.getKeyCode(), ev, Menu.FLAG_PERFORM_NO_CLOSE);
            st.isPrepared = false;
            if (handled) {
                return true;
            }
        }
        return false;
    }

    @Override
    boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            // If this is a MENU event, let the Activity have a go.
            if (mOriginalWindowCallback.dispatchKeyEvent(event)) {
                return true;
            }
        }

        final int keyCode = event.getKeyCode();
        final int action = event.getAction();
        final boolean isDown = action == KeyEvent.ACTION_DOWN;

        return isDown ? onKeyDown(keyCode, event) : onKeyUp(keyCode, event);
    }

    boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                onKeyUpPanel(Window.FEATURE_OPTIONS_PANEL, event);
                return true;
            case KeyEvent.KEYCODE_BACK:
                final boolean wasLongPressBackDown = mLongPressBackDown;
                mLongPressBackDown = false;

                PanelFeatureState st = getPanelState(Window.FEATURE_OPTIONS_PANEL, false);
                if (st != null && st.isOpen) {
                    if (!wasLongPressBackDown) {
                        // Certain devices allow opening the options menu via a long press of the
                        // back button. We should only close the open options menu if it wasn't
                        // opened via a long press gesture.
                        closePanel(st, true);
                    }
                    return true;
                }
                if (onBackPressed()) {
                    return true;
                }
                break;
        }
        return false;
    }

    boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                onKeyDownPanel(Window.FEATURE_OPTIONS_PANEL, event);
                // We need to return true here and not let it bubble up to the Window.
                // For empty menus, PhoneWindow's KEYCODE_BACK handling will steals all events,
                // not allowing the Activity to call onBackPressed().
                return true;
            case KeyEvent.KEYCODE_BACK:
                // Certain devices allow opening the options menu via a long press of the back
                // button. We keep a record of whether the last event is from a long press.
                mLongPressBackDown = (event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0;
                break;
        }

        // On API v7-10 we need to manually call onKeyShortcut() as this is not called
        // from the Activity
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // We do not return true here otherwise dispatchKeyEvent will not reach the Activity
            // (which results in the back button not working)
            onKeyShortcut(keyCode, event);
        }
        return false;
    }

    @Override
    public View createView(View parent, final String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        final boolean isPre21 = Build.VERSION.SDK_INT < 21;

        if (mAppCompatViewInflater == null) {
            mAppCompatViewInflater = new AppCompatViewInflater();
        }

        // We only want the View to inherit it's context if we're running pre-v21
        final boolean inheritContext = isPre21 && mSubDecorInstalled
                && shouldInheritContext((ViewParent) parent);

        return mAppCompatViewInflater.createView(parent, name, context, attrs, inheritContext,
                isPre21, /* Only read android:theme pre-L (L+ handles this anyway) */
                true /* Read read app:theme as a fallback at all times for legacy reasons */
        );
    }

    private boolean shouldInheritContext(ViewParent parent) {
        if (parent == null) {
            // The initial parent is null so just return false
            return false;
        }
        while (true) {
            if (parent == null) {
                // Bingo. We've hit a view which has a null parent before being terminated from
                // the loop. This is (most probably) because it's the root view in an inflation
                // call, therefore we should inherit. This works as the inflated layout is only
                // added to the hierarchy at the end of the inflate() call.
                return true;
            } else if (parent == mWindowDecor || !(parent instanceof View)
                    || ViewCompat.isAttachedToWindow((View) parent)) {
                // We have either hit the window's decor view, a parent which isn't a View
                // (i.e. ViewRootImpl), or an attached view, so we know that the original parent
                // is currently added to the view hierarchy. This means that it has not be
                // inflated in the current inflate() call and we should not inherit the context.
                return false;
            }
            parent = parent.getParent();
        }
    }

    @Override
    public void installViewFactory() {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        if (layoutInflater.getFactory() == null) {
            LayoutInflaterCompat.setFactory(layoutInflater, this);
        } else {
            Log.i(TAG, "The Activity's LayoutInflater already has a Factory installed"
                    + " so we can not install AppCompat's");
        }
    }

    /**
     * From {@link android.support.v4.view.LayoutInflaterFactory}
     */
    @Override
    public final View onCreateView(View parent, String name,
            Context context, AttributeSet attrs) {
        // First let the Activity's Factory try and inflate the view
        final View view = callActivityOnCreateView(parent, name, context, attrs);
        if (view != null) {
            return view;
        }

        // If the Factory didn't handle it, let our createView() method try
        return createView(parent, name, context, attrs);
    }

    View callActivityOnCreateView(View parent, String name, Context context, AttributeSet attrs) {
        // Let the Activity's LayoutInflater.Factory try and handle it
        if (mOriginalWindowCallback instanceof LayoutInflater.Factory) {
            final View result = ((LayoutInflater.Factory) mOriginalWindowCallback)
                    .onCreateView(name, context, attrs);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private void openPanel(final PanelFeatureState st, KeyEvent event) {
        // Already open, return
        if (st.isOpen || isDestroyed()) {
            return;
        }

        // Don't open an options panel for honeycomb apps on xlarge devices.
        // (The app should be using an action bar for menu items.)
        if (st.featureId == FEATURE_OPTIONS_PANEL) {
            Context context = mContext;
            Configuration config = context.getResources().getConfiguration();
            boolean isXLarge = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) ==
                    Configuration.SCREENLAYOUT_SIZE_XLARGE;
            boolean isHoneycombApp = context.getApplicationInfo().targetSdkVersion >=
                    android.os.Build.VERSION_CODES.HONEYCOMB;

            if (isXLarge && isHoneycombApp) {
                return;
            }
        }

        Window.Callback cb = getWindowCallback();
        if ((cb != null) && (!cb.onMenuOpened(st.featureId, st.menu))) {
            // Callback doesn't want the menu to open, reset any state
            closePanel(st, true);
            return;
        }

        final WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return;
        }

        // Prepare panel (should have been done before, but just in case)
        if (!preparePanel(st, event)) {
            return;
        }

        int width = WRAP_CONTENT;
        if (st.decorView == null || st.refreshDecorView) {
            if (st.decorView == null) {
                // Initialize the panel decor, this will populate st.decorView
                if (!initializePanelDecor(st) || (st.decorView == null))
                    return;
            } else if (st.refreshDecorView && (st.decorView.getChildCount() > 0)) {
                // Decor needs refreshing, so remove its views
                st.decorView.removeAllViews();
            }

            // This will populate st.shownPanelView
            if (!initializePanelContent(st) || !st.hasPanelItems()) {
                return;
            }

            ViewGroup.LayoutParams lp = st.shownPanelView.getLayoutParams();
            if (lp == null) {
                lp = new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            }

            int backgroundResId = st.background;
            st.decorView.setBackgroundResource(backgroundResId);

            ViewParent shownPanelParent = st.shownPanelView.getParent();
            if (shownPanelParent != null && shownPanelParent instanceof ViewGroup) {
                ((ViewGroup) shownPanelParent).removeView(st.shownPanelView);
            }
            st.decorView.addView(st.shownPanelView, lp);

            /*
             * Give focus to the view, if it or one of its children does not
             * already have it.
             */
            if (!st.shownPanelView.hasFocus()) {
                st.shownPanelView.requestFocus();
            }
        } else if (st.createdPanelView != null) {
            // If we already had a panel view, carry width=MATCH_PARENT through
            // as we did above when it was created.
            ViewGroup.LayoutParams lp = st.createdPanelView.getLayoutParams();
            if (lp != null && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                width = MATCH_PARENT;
            }
        }

        st.isHandled = false;

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                width, WRAP_CONTENT,
                st.x, st.y, WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);

        lp.gravity = st.gravity;
        lp.windowAnimations = st.windowAnimations;

        wm.addView(st.decorView, lp);
        st.isOpen = true;
    }

    private boolean initializePanelDecor(PanelFeatureState st) {
        st.setStyle(getActionBarThemedContext());
        st.decorView = new ListMenuDecorView(st.listPresenterContext);
        st.gravity = Gravity.CENTER | Gravity.BOTTOM;
        return true;
    }

    private void reopenMenu(MenuBuilder menu, boolean toggleMenuMode) {
        if (mDecorContentParent != null && mDecorContentParent.canShowOverflowMenu() &&
                (!ViewConfigurationCompat.hasPermanentMenuKey(ViewConfiguration.get(mContext)) ||
                        mDecorContentParent.isOverflowMenuShowPending())) {

            final Window.Callback cb = getWindowCallback();

            if (!mDecorContentParent.isOverflowMenuShowing() || !toggleMenuMode) {
                if (cb != null && !isDestroyed()) {
                    // If we have a menu invalidation pending, do it now.
                    if (mInvalidatePanelMenuPosted &&
                            (mInvalidatePanelMenuFeatures & (1 << FEATURE_OPTIONS_PANEL)) != 0) {
                        mWindowDecor.removeCallbacks(mInvalidatePanelMenuRunnable);
                        mInvalidatePanelMenuRunnable.run();
                    }

                    final PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, true);

                    // If we don't have a menu or we're waiting for a full content refresh,
                    // forget it. This is a lingering event that no longer matters.
                    if (st.menu != null && !st.refreshMenuContent &&
                            cb.onPreparePanel(FEATURE_OPTIONS_PANEL, st.createdPanelView, st.menu)) {
                        cb.onMenuOpened(FEATURE_SUPPORT_ACTION_BAR, st.menu);
                        mDecorContentParent.showOverflowMenu();
                    }
                }
            } else {
                mDecorContentParent.hideOverflowMenu();
                if (!isDestroyed()) {
                    final PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, true);
                    cb.onPanelClosed(FEATURE_SUPPORT_ACTION_BAR, st.menu);
                }
            }
            return;
        }

        PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, true);

        st.refreshDecorView = true;
        closePanel(st, false);

        openPanel(st, null);
    }

    private boolean initializePanelMenu(final PanelFeatureState st) {
        Context context = mContext;

        // If we have an action bar, initialize the menu with the right theme.
        if ((st.featureId == FEATURE_OPTIONS_PANEL || st.featureId == FEATURE_SUPPORT_ACTION_BAR) &&
                mDecorContentParent != null) {
            final TypedValue outValue = new TypedValue();
            final Resources.Theme baseTheme = context.getTheme();
            baseTheme.resolveAttribute(R.attr.actionBarTheme, outValue, true);

            Resources.Theme widgetTheme = null;
            if (outValue.resourceId != 0) {
                widgetTheme = context.getResources().newTheme();
                widgetTheme.setTo(baseTheme);
                widgetTheme.applyStyle(outValue.resourceId, true);
                widgetTheme.resolveAttribute(
                        R.attr.actionBarWidgetTheme, outValue, true);
            } else {
                baseTheme.resolveAttribute(
                        R.attr.actionBarWidgetTheme, outValue, true);
            }

            if (outValue.resourceId != 0) {
                if (widgetTheme == null) {
                    widgetTheme = context.getResources().newTheme();
                    widgetTheme.setTo(baseTheme);
                }
                widgetTheme.applyStyle(outValue.resourceId, true);
            }

            if (widgetTheme != null) {
                context = new ContextThemeWrapper(context, 0);
                context.getTheme().setTo(widgetTheme);
            }
        }

        final MenuBuilder menu = new MenuBuilder(context);
        menu.setCallback(this);
        st.setMenu(menu);

        return true;
    }

    private boolean initializePanelContent(PanelFeatureState st) {
        if (st.createdPanelView != null) {
            st.shownPanelView = st.createdPanelView;
            return true;
        }

        if (st.menu == null) {
            return false;
        }

        if (mPanelMenuPresenterCallback == null) {
            mPanelMenuPresenterCallback = new PanelMenuPresenterCallback();
        }

        MenuView menuView = st.getListMenuView(mPanelMenuPresenterCallback);

        st.shownPanelView = (View) menuView;

        return st.shownPanelView != null;
    }

    private boolean preparePanel(PanelFeatureState st, KeyEvent event) {
        if (isDestroyed()) {
            return false;
        }

        // Already prepared (isPrepared will be reset to false later)
        if (st.isPrepared) {
            return true;
        }

        if ((mPreparedPanel != null) && (mPreparedPanel != st)) {
            // Another Panel is prepared and possibly open, so close it
            closePanel(mPreparedPanel, false);
        }

        final Window.Callback cb = getWindowCallback();

        if (cb != null) {
            st.createdPanelView = cb.onCreatePanelView(st.featureId);
        }

        final boolean isActionBarMenu =
                (st.featureId == FEATURE_OPTIONS_PANEL || st.featureId == FEATURE_SUPPORT_ACTION_BAR);

        if (isActionBarMenu && mDecorContentParent != null) {
            // Enforce ordering guarantees around events so that the action bar never
            // dispatches menu-related events before the panel is prepared.
            mDecorContentParent.setMenuPrepared();
        }

        if (st.createdPanelView == null &&
                (!isActionBarMenu || !(peekSupportActionBar() instanceof ToolbarActionBar))) {
            // Since ToolbarActionBar handles the list options menu itself, we only want to
            // init this menu panel if we're not using a TAB.
            if (st.menu == null || st.refreshMenuContent) {
                if (st.menu == null) {
                    if (!initializePanelMenu(st) || (st.menu == null)) {
                        return false;
                    }
                }

                if (isActionBarMenu && mDecorContentParent != null) {
                    if (mActionMenuPresenterCallback == null) {
                        mActionMenuPresenterCallback = new ActionMenuPresenterCallback();
                    }
                    mDecorContentParent.setMenu(st.menu, mActionMenuPresenterCallback);
                }

                // Creating the panel menu will involve a lot of manipulation;
                // don't dispatch change events to presenters until we're done.
                st.menu.stopDispatchingItemsChanged();
                if (!cb.onCreatePanelMenu(st.featureId, st.menu)) {
                    // Ditch the menu created above
                    st.setMenu(null);

                    if (isActionBarMenu && mDecorContentParent != null) {
                        // Don't show it in the action bar either
                        mDecorContentParent.setMenu(null, mActionMenuPresenterCallback);
                    }

                    return false;
                }

                st.refreshMenuContent = false;
            }

            // Preparing the panel menu can involve a lot of manipulation;
            // don't dispatch change events to presenters until we're done.
            st.menu.stopDispatchingItemsChanged();

            // Restore action view state before we prepare. This gives apps
            // an opportunity to override frozen/restored state in onPrepare.
            if (st.frozenActionViewState != null) {
                st.menu.restoreActionViewStates(st.frozenActionViewState);
                st.frozenActionViewState = null;
            }

            // Callback and return if the callback does not want to show the menu
            if (!cb.onPreparePanel(FEATURE_OPTIONS_PANEL, st.createdPanelView, st.menu)) {
                if (isActionBarMenu && mDecorContentParent != null) {
                    // The app didn't want to show the menu for now but it still exists.
                    // Clear it out of the action bar.
                    mDecorContentParent.setMenu(null, mActionMenuPresenterCallback);
                }
                st.menu.startDispatchingItemsChanged();
                return false;
            }

            // Set the proper keymap
            KeyCharacterMap kmap = KeyCharacterMap.load(
                    event != null ? event.getDeviceId() : KeyCharacterMap.VIRTUAL_KEYBOARD);
            st.qwertyMode = kmap.getKeyboardType() != KeyCharacterMap.NUMERIC;
            st.menu.setQwertyMode(st.qwertyMode);
            st.menu.startDispatchingItemsChanged();
        }

        // Set other state
        st.isPrepared = true;
        st.isHandled = false;
        mPreparedPanel = st;

        return true;
    }

    private void checkCloseActionMenu(MenuBuilder menu) {
        if (mClosingActionMenu) {
            return;
        }

        mClosingActionMenu = true;
        mDecorContentParent.dismissPopups();
        Window.Callback cb = getWindowCallback();
        if (cb != null && !isDestroyed()) {
            cb.onPanelClosed(FEATURE_SUPPORT_ACTION_BAR, menu);
        }
        mClosingActionMenu = false;
    }

    private void closePanel(int featureId) {
        closePanel(getPanelState(featureId, true), true);
    }

    private void closePanel(PanelFeatureState st, boolean doCallback) {
        if (doCallback && st.featureId == FEATURE_OPTIONS_PANEL &&
                mDecorContentParent != null && mDecorContentParent.isOverflowMenuShowing()) {
            checkCloseActionMenu(st.menu);
            return;
        }

        final WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null && st.isOpen && st.decorView != null) {
            wm.removeView(st.decorView);

            if (doCallback) {
                callOnPanelClosed(st.featureId, st, null);
            }
        }

        st.isPrepared = false;
        st.isHandled = false;
        st.isOpen = false;

        // This view is no longer shown, so null it out
        st.shownPanelView = null;

        // Next time the menu opens, it should not be in expanded mode, so
        // force a refresh of the decor
        st.refreshDecorView = true;

        if (mPreparedPanel == st) {
            mPreparedPanel = null;
        }
    }

    private boolean onKeyDownPanel(int featureId, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            PanelFeatureState st = getPanelState(featureId, true);
            if (!st.isOpen) {
                return preparePanel(st, event);
            }
        }

        return false;
    }

    private boolean onKeyUpPanel(int featureId, KeyEvent event) {
        if (mActionMode != null) {
            return false;
        }

        boolean handled = false;
        final PanelFeatureState st = getPanelState(featureId, true);
        if (featureId == FEATURE_OPTIONS_PANEL && mDecorContentParent != null &&
                mDecorContentParent.canShowOverflowMenu() &&
                !ViewConfigurationCompat.hasPermanentMenuKey(ViewConfiguration.get(mContext))) {
            if (!mDecorContentParent.isOverflowMenuShowing()) {
                if (!isDestroyed() && preparePanel(st, event)) {
                    handled = mDecorContentParent.showOverflowMenu();
                }
            } else {
                handled = mDecorContentParent.hideOverflowMenu();
            }
        } else {
            if (st.isOpen || st.isHandled) {
                // Play the sound effect if the user closed an open menu (and not if
                // they just released a menu shortcut)
                handled = st.isOpen;
                // Close menu
                closePanel(st, true);
            } else if (st.isPrepared) {
                boolean show = true;
                if (st.refreshMenuContent) {
                    // Something may have invalidated the menu since we prepared it.
                    // Re-prepare it to refresh.
                    st.isPrepared = false;
                    show = preparePanel(st, event);
                }

                if (show) {
                    // Show menu
                    openPanel(st, event);
                    handled = true;
                }
            }
        }

        if (handled) {
            AudioManager audioManager = (AudioManager) mContext.getSystemService(
                    Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
            } else {
                Log.w(TAG, "Couldn't get audio manager");
            }
        }
        return handled;
    }

    private void callOnPanelClosed(int featureId, PanelFeatureState panel, Menu menu) {
        // Try to get a menu
        if (menu == null) {
            // Need a panel to grab the menu, so try to get that
            if (panel == null) {
                if ((featureId >= 0) && (featureId < mPanels.length)) {
                    panel = mPanels[featureId];
                }
            }

            if (panel != null) {
                // menu still may be null, which is okay--we tried our best
                menu = panel.menu;
            }
        }

        // If the panel is not open, do not callback
        if ((panel != null) && (!panel.isOpen))
            return;

        if (!isDestroyed()) {
            // We need to be careful which callback we dispatch the call to. We can not dispatch
            // this to the Window's callback since that will call back into this method and cause a
            // crash. Instead we need to dispatch down to the original Activity/Dialog/etc.
            mOriginalWindowCallback.onPanelClosed(featureId, menu);
        }
    }

    private PanelFeatureState findMenuPanel(Menu menu) {
        final PanelFeatureState[] panels = mPanels;
        final int N = panels != null ? panels.length : 0;
        for (int i = 0; i < N; i++) {
            final PanelFeatureState panel = panels[i];
            if (panel != null && panel.menu == menu) {
                return panel;
            }
        }
        return null;
    }

    private PanelFeatureState getPanelState(int featureId, boolean required) {
        PanelFeatureState[] ar;
        if ((ar = mPanels) == null || ar.length <= featureId) {
            PanelFeatureState[] nar = new PanelFeatureState[featureId + 1];
            if (ar != null) {
                System.arraycopy(ar, 0, nar, 0, ar.length);
            }
            mPanels = ar = nar;
        }

        PanelFeatureState st = ar[featureId];
        if (st == null) {
            ar[featureId] = st = new PanelFeatureState(featureId);
        }
        return st;
    }

    private boolean performPanelShortcut(PanelFeatureState st, int keyCode, KeyEvent event,
            int flags) {
        if (event.isSystem()) {
            return false;
        }

        boolean handled = false;

        // Only try to perform menu shortcuts if preparePanel returned true (possible false
        // return value from application not wanting to show the menu).
        if ((st.isPrepared || preparePanel(st, event)) && st.menu != null) {
            // The menu is prepared now, perform the shortcut on it
            handled = st.menu.performShortcut(keyCode, event, flags);
        }

        if (handled) {
            // Only close down the menu if we don't have an action bar keeping it open.
            if ((flags & Menu.FLAG_PERFORM_NO_CLOSE) == 0 && mDecorContentParent == null) {
                closePanel(st, true);
            }
        }

        return handled;
    }

    private void invalidatePanelMenu(int featureId) {
        mInvalidatePanelMenuFeatures |= 1 << featureId;

        if (!mInvalidatePanelMenuPosted && mWindowDecor != null) {
            ViewCompat.postOnAnimation(mWindowDecor, mInvalidatePanelMenuRunnable);
            mInvalidatePanelMenuPosted = true;
        }
    }

    private void doInvalidatePanelMenu(int featureId) {
        PanelFeatureState st = getPanelState(featureId, true);
        Bundle savedActionViewStates = null;
        if (st.menu != null) {
            savedActionViewStates = new Bundle();
            st.menu.saveActionViewStates(savedActionViewStates);
            if (savedActionViewStates.size() > 0) {
                st.frozenActionViewState = savedActionViewStates;
            }
            // This will be started again when the panel is prepared.
            st.menu.stopDispatchingItemsChanged();
            st.menu.clear();
        }
        st.refreshMenuContent = true;
        st.refreshDecorView = true;

        // Prepare the options panel if we have an action bar
        if ((featureId == FEATURE_SUPPORT_ACTION_BAR || featureId == FEATURE_OPTIONS_PANEL)
                && mDecorContentParent != null) {
            st = getPanelState(Window.FEATURE_OPTIONS_PANEL, false);
            if (st != null) {
                st.isPrepared = false;
                preparePanel(st, null);
            }
        }
    }

    /**
     * Updates the status bar guard
     *
     * @param insetTop the current top system window inset
     * @return the new top system window inset
     */
    private int updateStatusGuard(int insetTop) {
        boolean showStatusGuard = false;
        // Show the status guard when the non-overlay contextual action bar is showing
        if (mActionModeView != null) {
            if (mActionModeView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)
                        mActionModeView.getLayoutParams();
                boolean mlpChanged = false;

                if (mActionModeView.isShown()) {
                    if (mTempRect1 == null) {
                        mTempRect1 = new Rect();
                        mTempRect2 = new Rect();
                    }
                    final Rect insets = mTempRect1;
                    final Rect localInsets = mTempRect2;
                    insets.set(0, insetTop, 0, 0);

                    ViewUtils.computeFitSystemWindows(mSubDecor, insets, localInsets);
                    final int newMargin = localInsets.top == 0 ? insetTop : 0;
                    if (mlp.topMargin != newMargin) {
                        mlpChanged = true;
                        mlp.topMargin = insetTop;

                        if (mStatusGuard == null) {
                            mStatusGuard = new View(mContext);
                            mStatusGuard.setBackgroundColor(mContext.getResources()
                                    .getColor(R.color.abc_input_method_navigation_guard));
                            mSubDecor.addView(mStatusGuard, -1,
                                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                            insetTop));
                        } else {
                            ViewGroup.LayoutParams lp = mStatusGuard.getLayoutParams();
                            if (lp.height != insetTop) {
                                lp.height = insetTop;
                                mStatusGuard.setLayoutParams(lp);
                            }
                        }
                    }

                    // The action mode's theme may differ from the app, so
                    // always show the status guard above it.
                    showStatusGuard = mStatusGuard != null;

                    // We only need to consume the insets if the action
                    // mode is overlaid on the app content (e.g. it's
                    // sitting in a FrameLayout, see
                    // screen_simple_overlay_action_mode.xml).
                    if (!mOverlayActionMode && showStatusGuard) {
                        insetTop = 0;
                    }
                } else {
                    // reset top margin
                    if (mlp.topMargin != 0) {
                        mlpChanged = true;
                        mlp.topMargin = 0;
                    }
                }
                if (mlpChanged) {
                    mActionModeView.setLayoutParams(mlp);
                }
            }
        }
        if (mStatusGuard != null) {
            mStatusGuard.setVisibility(showStatusGuard ? View.VISIBLE : View.GONE);
        }

        return insetTop;
    }

    private void throwFeatureRequestIfSubDecorInstalled() {
        if (mSubDecorInstalled) {
            throw new AndroidRuntimeException(
                    "Window feature must be requested before adding content");
        }
    }

    private int sanitizeWindowFeatureId(int featureId) {
        if (featureId == WindowCompat.FEATURE_ACTION_BAR) {
            Log.i(TAG, "You should now use the AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR"
                    + " id when requesting this feature.");
            return FEATURE_SUPPORT_ACTION_BAR;
        } else if (featureId == WindowCompat.FEATURE_ACTION_BAR_OVERLAY) {
            Log.i(TAG, "You should now use the AppCompatDelegate.FEATURE_SUPPORT_ACTION_BAR_OVERLAY"
                    + " id when requesting this feature.");
            return FEATURE_SUPPORT_ACTION_BAR_OVERLAY;
        }
        // Else we'll just return the original id
        return featureId;
    }

    ViewGroup getSubDecor() {
        return mSubDecor;
    }

    private void dismissPopups() {
        if (mDecorContentParent != null) {
            mDecorContentParent.dismissPopups();
        }

        if (mActionModePopup != null) {
            mWindowDecor.removeCallbacks(mShowActionModePopup);
            if (mActionModePopup.isShowing()) {
                try {
                    mActionModePopup.dismiss();
                } catch (IllegalArgumentException e) {
                    // Pre-v18, there are times when the Window will remove the popup before us.
                    // In these cases we need to swallow the resulting exception.
                }
            }
            mActionModePopup = null;
        }
        endOnGoingFadeAnimation();

        PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
        if (st != null && st.menu != null) {
            st.menu.close();
        }
    }

    /**
     * Clears out internal reference when the action mode is destroyed.
     */
    class ActionModeCallbackWrapperV7 implements ActionMode.Callback {
        private ActionMode.Callback mWrapped;

        public ActionModeCallbackWrapperV7(ActionMode.Callback wrapped) {
            mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            if (mActionModePopup != null) {
                mWindow.getDecorView().removeCallbacks(mShowActionModePopup);
            }

            if (mActionModeView != null) {
                endOnGoingFadeAnimation();
                mFadeAnim = ViewCompat.animate(mActionModeView).alpha(0f);
                mFadeAnim.setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(View view) {
                        mActionModeView.setVisibility(View.GONE);
                        if (mActionModePopup != null) {
                            mActionModePopup.dismiss();
                        } else if (mActionModeView.getParent() instanceof View) {
                            ViewCompat.requestApplyInsets((View) mActionModeView.getParent());
                        }
                        mActionModeView.removeAllViews();
                        mFadeAnim.setListener(null);
                        mFadeAnim = null;
                    }
                });
            }
            if (mAppCompatCallback != null) {
                mAppCompatCallback.onSupportActionModeFinished(mActionMode);
            }
            mActionMode = null;
        }
    }

    private final class PanelMenuPresenterCallback implements MenuPresenter.Callback {
        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            final Menu parentMenu = menu.getRootMenu();
            final boolean isSubMenu = parentMenu != menu;
            final PanelFeatureState panel = findMenuPanel(isSubMenu ? parentMenu : menu);
            if (panel != null) {
                if (isSubMenu) {
                    callOnPanelClosed(panel.featureId, panel, parentMenu);
                    closePanel(panel, true);
                } else {
                    // Close the panel and only do the callback if the menu is being
                    // closed completely, not if opening a sub menu
                    closePanel(panel, allMenusAreClosing);
                }
            }
        }

        @Override
        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            if (subMenu == null && mHasActionBar) {
                Window.Callback cb = getWindowCallback();
                if (cb != null && !isDestroyed()) {
                    cb.onMenuOpened(FEATURE_SUPPORT_ACTION_BAR, subMenu);
                }
            }
            return true;
        }
    }

    private final class ActionMenuPresenterCallback implements MenuPresenter.Callback {
        @Override
        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            Window.Callback cb = getWindowCallback();
            if (cb != null) {
                cb.onMenuOpened(FEATURE_SUPPORT_ACTION_BAR, subMenu);
            }
            return true;
        }

        @Override
        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
            checkCloseActionMenu(menu);
        }
    }

    private static final class PanelFeatureState {

        /** Feature ID for this panel. */
        int featureId;

        int background;

        int gravity;

        int x;

        int y;

        int windowAnimations;

        /** Dynamic state of the panel. */
        ViewGroup decorView;

        /** The panel that we are actually showing. */
        View shownPanelView;

        /** The panel that was returned by onCreatePanelView(). */
        View createdPanelView;

        /** Use {@link #setMenu} to set this. */
        MenuBuilder menu;

        ListMenuPresenter listMenuPresenter;

        Context listPresenterContext;

        /**
         * Whether the panel has been prepared (see
         * {@link #preparePanel}).
         */
        boolean isPrepared;

        /**
         * Whether an item's action has been performed. This happens in obvious
         * scenarios (user clicks on menu item), but can also happen with
         * chording menu+(shortcut key).
         */
        boolean isHandled;

        boolean isOpen;

        public boolean qwertyMode;

        boolean refreshDecorView;

        boolean refreshMenuContent;

        boolean wasLastOpen;

        /**
         * Contains the state of the menu when told to freeze.
         */
        Bundle frozenMenuState;

        /**
         * Contains the state of associated action views when told to freeze.
         * These are saved across invalidations.
         */
        Bundle frozenActionViewState;

        PanelFeatureState(int featureId) {
            this.featureId = featureId;

            refreshDecorView = false;
        }

        public boolean hasPanelItems() {
            if (shownPanelView == null) return false;
            if (createdPanelView != null) return true;

            return listMenuPresenter.getAdapter().getCount() > 0;
        }

        /**
         * Unregister and free attached MenuPresenters. They will be recreated as needed.
         */
        public void clearMenuPresenters() {
            if (menu != null) {
                menu.removeMenuPresenter(listMenuPresenter);
            }
            listMenuPresenter = null;
        }

        void setStyle(Context context) {
            final TypedValue outValue = new TypedValue();
            final Resources.Theme widgetTheme = context.getResources().newTheme();
            widgetTheme.setTo(context.getTheme());

            // First apply the actionBarPopupTheme
            widgetTheme.resolveAttribute(R.attr.actionBarPopupTheme, outValue, true);
            if (outValue.resourceId != 0) {
                widgetTheme.applyStyle(outValue.resourceId, true);
            }

            // Now apply the panelMenuListTheme
            widgetTheme.resolveAttribute(R.attr.panelMenuListTheme, outValue, true);
            if (outValue.resourceId != 0) {
                widgetTheme.applyStyle(outValue.resourceId, true);
            } else {
                widgetTheme.applyStyle(R.style.Theme_AppCompat_CompactMenu, true);
            }

            context = new ContextThemeWrapper(context, 0);
            context.getTheme().setTo(widgetTheme);

            listPresenterContext = context;

            TypedArray a = context.obtainStyledAttributes(R.styleable.Theme);
            background = a.getResourceId(
                    R.styleable.Theme_panelBackground, 0);
            windowAnimations = a.getResourceId(
                    R.styleable.Theme_android_windowAnimationStyle, 0);
            a.recycle();
        }

        void setMenu(MenuBuilder menu) {
            if (menu == this.menu) return;

            if (this.menu != null) {
                this.menu.removeMenuPresenter(listMenuPresenter);
            }
            this.menu = menu;
            if (menu != null) {
                if (listMenuPresenter != null) menu.addMenuPresenter(listMenuPresenter);
            }
        }

        MenuView getListMenuView(MenuPresenter.Callback cb) {
            if (menu == null) return null;

            if (listMenuPresenter == null) {
                listMenuPresenter = new ListMenuPresenter(listPresenterContext,
                        R.layout.abc_list_menu_item_layout);
                listMenuPresenter.setCallback(cb);
                menu.addMenuPresenter(listMenuPresenter);
            }

            MenuView result = listMenuPresenter.getMenuView(decorView);

            return result;
        }

        Parcelable onSaveInstanceState() {
            SavedState savedState = new SavedState();
            savedState.featureId = featureId;
            savedState.isOpen = isOpen;

            if (menu != null) {
                savedState.menuState = new Bundle();
                menu.savePresenterStates(savedState.menuState);
            }

            return savedState;
        }

        void onRestoreInstanceState(Parcelable state) {
            SavedState savedState = (SavedState) state;
            featureId = savedState.featureId;
            wasLastOpen = savedState.isOpen;
            frozenMenuState = savedState.menuState;

            shownPanelView = null;
            decorView = null;
        }

        void applyFrozenState() {
            if (menu != null && frozenMenuState != null) {
                menu.restorePresenterStates(frozenMenuState);
                frozenMenuState = null;
            }
        }

        private static class SavedState implements Parcelable {
            int featureId;
            boolean isOpen;
            Bundle menuState;

            public int describeContents() {
                return 0;
            }

            public void writeToParcel(Parcel dest, int flags) {
                dest.writeInt(featureId);
                dest.writeInt(isOpen ? 1 : 0);

                if (isOpen) {
                    dest.writeBundle(menuState);
                }
            }

            private static SavedState readFromParcel(Parcel source, ClassLoader loader) {
                SavedState savedState = new SavedState();
                savedState.featureId = source.readInt();
                savedState.isOpen = source.readInt() == 1;

                if (savedState.isOpen) {
                    savedState.menuState = source.readBundle(loader);
                }

                return savedState;
            }

            public static final Parcelable.Creator<SavedState> CREATOR
                    = ParcelableCompat.newCreator(
                    new ParcelableCompatCreatorCallbacks<SavedState>() {
                        @Override
                        public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                            return readFromParcel(in, loader);
                        }

                        @Override
                        public SavedState[] newArray(int size) {
                            return new SavedState[size];
                        }
                    });
        }
    }

    private class ListMenuDecorView extends ContentFrameLayout {
        public ListMenuDecorView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return AppCompatDelegateImplV7.this.dispatchKeyEvent(event)
                    || super.dispatchKeyEvent(event);
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (isOutOfBounds(x, y)) {
                    closePanel(Window.FEATURE_OPTIONS_PANEL);
                    return true;
                }
            }
            return super.onInterceptTouchEvent(event);
        }

        @Override
        public void setBackgroundResource(int resid) {
            setBackgroundDrawable(AppCompatDrawableManager.get().getDrawable(getContext(), resid));
        }

        private boolean isOutOfBounds(int x, int y) {
            return x < -5 || y < -5 || x > (getWidth() + 5) || y > (getHeight() + 5);
        }
    }
}
