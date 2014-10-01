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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.appcompat.R;
import android.support.v7.internal.VersionUtils;
import android.support.v7.internal.app.ToolbarActionBar;
import android.support.v7.internal.app.WindowCallback;
import android.support.v7.internal.app.WindowDecorActionBar;
import android.support.v7.internal.view.StandaloneActionMode;
import android.support.v7.internal.view.menu.ListMenuPresenter;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.internal.view.menu.MenuPresenter;
import android.support.v7.internal.view.menu.MenuView;
import android.support.v7.internal.widget.ActionBarContextView;
import android.support.v7.internal.widget.DecorContentParent;
import android.support.v7.internal.widget.FitWindowsViewGroup;
import android.support.v7.internal.widget.ProgressBarCompat;
import android.support.v7.internal.widget.TintCheckBox;
import android.support.v7.internal.widget.TintCheckedTextView;
import android.support.v7.internal.widget.TintEditText;
import android.support.v7.internal.widget.TintRadioButton;
import android.support.v7.internal.widget.TintSpinner;
import android.support.v7.internal.widget.ViewStubCompat;
import android.support.v7.internal.widget.ViewUtils;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import static android.support.v4.view.WindowCompat.FEATURE_ACTION_BAR;
import static android.support.v4.view.WindowCompat.FEATURE_ACTION_BAR_OVERLAY;
import static android.support.v4.view.WindowCompat.FEATURE_ACTION_MODE_OVERLAY;
import static android.view.Window.FEATURE_OPTIONS_PANEL;

class ActionBarActivityDelegateBase extends ActionBarActivityDelegate
        implements MenuBuilder.Callback {
    private static final String TAG = "ActionBarActivityDelegateBase";

    private DecorContentParent mDecorContentParent;
    private ActionMenuPresenterCallback mActionMenuPresenterCallback;
    private PanelMenuPresenterCallback mPanelMenuPresenterCallback;

    ActionMode mActionMode;
    ActionBarContextView mActionModeView;
    PopupWindow mActionModePopup;
    Runnable mShowActionModePopup;

    // true if we have installed a window sub-decor layout.
    private boolean mSubDecorInstalled;
    private ViewGroup mWindowDecor;
    private ViewGroup mSubDecor;

    private View mStatusGuard;

    private CharSequence mTitleToSet;

    // Used to keep track of Progress Bar Window features
    private boolean mFeatureProgress, mFeatureIndeterminateProgress;

    // Used for emulating PanelFeatureState
    private boolean mClosingActionMenu;
    private PanelFeatureState[] mPanels;
    private PanelFeatureState mPreparedPanel;

    private boolean mInvalidatePanelMenuPosted;
    private int mInvalidatePanelMenuFeatures;
    private final Runnable mInvalidatePanelMenuRunnable = new Runnable() {
        @Override
        public void run() {
            if ((mInvalidatePanelMenuFeatures & 1 << FEATURE_OPTIONS_PANEL) != 0) {
                doInvalidatePanelMenu(FEATURE_OPTIONS_PANEL);
            }
            if ((mInvalidatePanelMenuFeatures & 1 << FEATURE_ACTION_BAR) != 0) {
                doInvalidatePanelMenu(FEATURE_ACTION_BAR);
            }
            mInvalidatePanelMenuPosted = false;
            mInvalidatePanelMenuFeatures = 0;
        }
    };

    private boolean mEnableDefaultActionBarUp;

    private ListMenuPresenter mToolbarListMenuPresenter;

    private Rect mTempRect1;
    private Rect mTempRect2;

    ActionBarActivityDelegateBase(ActionBarActivity activity) {
        super(activity);
    }

    @Override
    void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWindowDecor = (ViewGroup) mActivity.getWindow().getDecorView();

        if (NavUtils.getParentActivityName(mActivity) != null) {
            ActionBar ab = getSupportActionBar();
            if (ab == null) {
                mEnableDefaultActionBarUp = true;
            } else {
                ab.setDefaultDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public ActionBar createSupportActionBar() {
        ensureSubDecor();
        ActionBar ab = new WindowDecorActionBar(mActivity, mOverlayActionBar);
        ab.setDefaultDisplayHomeAsUpEnabled(mEnableDefaultActionBarUp);
        return ab;
    }

    @Override
    void setSupportActionBar(Toolbar toolbar) {
        final ActionBar ab = getSupportActionBar();
        if (ab instanceof WindowDecorActionBar) {
            throw new IllegalStateException("This Activity already has an action bar supplied " +
                    "by the window decor. Do not request Window.FEATURE_ACTION_BAR and set " +
                    "windowActionBar to false in your theme to use a Toolbar instead.");
        } else if (ab instanceof ToolbarActionBar) {
            // Make sure we reset the old toolbar AB's list menu presenter
            ((ToolbarActionBar) ab).setListMenuPresenter(null);
        }

        // Need to make sure we give the action bar the default window callback. Otherwise multiple
        // setSupportActionBar() calls lead to memory leaks
        ToolbarActionBar tbab = new ToolbarActionBar(toolbar, mActivity.getTitle(),
                mActivity.getWindow(), mDefaultWindowCallback);
        ensureToolbarListMenuPresenter();
        tbab.setListMenuPresenter(mToolbarListMenuPresenter);
        setSupportActionBar(tbab);
        setWindowCallback(tbab.getWrappedWindowCallback());
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
        ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v);
        mActivity.onSupportContentChanged();
    }

    @Override
    public void setContentView(int resId) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        mActivity.getLayoutInflater().inflate(resId, contentParent);
        mActivity.onSupportContentChanged();
    }

    @Override
    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v, lp);
        mActivity.onSupportContentChanged();
    }

    @Override
    public void addContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mActivity.findViewById(android.R.id.content);
        contentParent.addView(v, lp);
        mActivity.onSupportContentChanged();
    }

    @Override
    public void onContentChanged() {
        // Ignore all calls to this method as we call onSupportContentChanged manually above
    }

    final void ensureSubDecor() {
        if (!mSubDecorInstalled) {
            if (mHasActionBar) {
                /**
                 * This needs some explanation. As we can not use the android:theme attribute
                 * pre-L, we emulate it by manually creating a LayoutInflater using a
                 * ContextThemeWrapper pointing to actionBarTheme.
                 */
                TypedValue outValue = new TypedValue();
                mActivity.getTheme().resolveAttribute(R.attr.actionBarTheme, outValue, true);

                Context themedContext;
                if (outValue.resourceId != 0) {
                    themedContext = new ContextThemeWrapper(mActivity, outValue.resourceId);
                } else {
                    themedContext = mActivity;
                }

                // Now inflate the view using the themed context and set it as the content view
                mSubDecor = (ViewGroup) LayoutInflater.from(themedContext)
                        .inflate(R.layout.abc_screen_toolbar, null);

                mDecorContentParent = (DecorContentParent) mSubDecor
                        .findViewById(R.id.decor_content_parent);
                mDecorContentParent.setWindowCallback(getWindowCallback());

                /**
                 * Propagate features to DecorContentParent
                 */
                if (mOverlayActionBar) {
                    mDecorContentParent.initFeature(FEATURE_ACTION_BAR_OVERLAY);
                }
                if (mFeatureProgress) {
                    mDecorContentParent.initFeature(Window.FEATURE_PROGRESS);
                }
                if (mFeatureIndeterminateProgress) {
                    mDecorContentParent.initFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                }
            } else {
                if (mOverlayActionMode) {
                    mSubDecor = (ViewGroup) LayoutInflater.from(mActivity)
                            .inflate(R.layout.abc_screen_simple_overlay_action_mode, null);
                } else {
                    mSubDecor = (ViewGroup) LayoutInflater.from(mActivity)
                            .inflate(R.layout.abc_screen_simple, null);
                }

                if (Build.VERSION.SDK_INT >= 21) {
                    // If we're running on L or above, we can rely on ViewCompat's
                    // setOnApplyWindowInsetsListener
                    ViewCompat.setOnApplyWindowInsetsListener(mSubDecor,
                            new OnApplyWindowInsetsListener() {
                                @Override
                                public WindowInsetsCompat onApplyWindowInsets(View v,
                                        WindowInsetsCompat insets) {
                                    final int top = insets.getSystemWindowInsetTop();
                                    final int newTop = updateStatusGuard(top);

                                    if (top != newTop) {
                                        return insets.replaceSystemWindowInsets(
                                                insets.getSystemWindowInsetLeft(),
                                                newTop,
                                                insets.getSystemWindowInsetRight(),
                                                insets.getSystemWindowInsetBottom());
                                    } else {
                                        return insets;
                                    }
                                }
                            });
                } else {
                    // Else, we need to use our own FitWindowsViewGroup handling
                    ((FitWindowsViewGroup) mSubDecor).setOnFitSystemWindowsListener(
                            new FitWindowsViewGroup.OnFitSystemWindowsListener() {
                                @Override
                                public void onFitSystemWindows(Rect insets) {
                                    insets.top = updateStatusGuard(insets.top);
                                }
                            });
                }
            }

            // Make the decor optionally fit system windows, like the window's decor
            ViewUtils.makeOptionalFitsSystemWindows(mSubDecor);

            // Now set the Activity's content view with the decor
            mActivity.superSetContentView(mSubDecor);

            // Change our content FrameLayout to use the android.R.id.content id.
            // Useful for fragments.
            final View decorContent = mActivity.findViewById(android.R.id.content);
            decorContent.setId(View.NO_ID);
            View abcContent = mActivity.findViewById(R.id.action_bar_activity_content);
            abcContent.setId(android.R.id.content);

            // The decorContent may have a foreground drawable set (windowContentOverlay).
            // Remove this as we handle it ourselves
            if (decorContent instanceof FrameLayout) {
                ((FrameLayout) decorContent).setForeground(null);
            }

            // A title was set before we've install the decor so set it now.
            if (mTitleToSet != null && mDecorContentParent != null) {
                mDecorContentParent.setWindowTitle(mTitleToSet);
                mTitleToSet = null;
            }

            applyFixedSizeWindow();

            onSubDecorInstalled();

            mSubDecorInstalled = true;

            // Invalidate if the panel menu hasn't been created before this.
            // Panel menu invalidation is deferred avoiding application onCreateOptionsMenu
            // being called in the middle of onCreate or similar.
            // A pending invalidation will typically be resolved before the posted message
            // would run normally in order to satisfy instance state restoration.
            PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
            if (!isDestroyed() && (st == null || st.menu == null)) {
                invalidatePanelMenu(FEATURE_ACTION_BAR);
            }
        }
    }

    void onSubDecorInstalled() {}

    private void applyFixedSizeWindow() {
        TypedArray a = mActivity.obtainStyledAttributes(R.styleable.Theme);

        TypedValue mFixedWidthMajor = null;
        TypedValue mFixedWidthMinor = null;
        TypedValue mFixedHeightMajor = null;
        TypedValue mFixedHeightMinor = null;

        if (a.hasValue(R.styleable.Theme_windowFixedWidthMajor)) {
            if (mFixedWidthMajor == null) mFixedWidthMajor = new TypedValue();
            a.getValue(R.styleable.Theme_windowFixedWidthMajor, mFixedWidthMajor);
        }
        if (a.hasValue(R.styleable.Theme_windowFixedWidthMinor)) {
            if (mFixedWidthMinor == null) mFixedWidthMinor = new TypedValue();
            a.getValue(R.styleable.Theme_windowFixedWidthMinor, mFixedWidthMinor);
        }
        if (a.hasValue(R.styleable.Theme_windowFixedHeightMajor)) {
            if (mFixedHeightMajor == null) mFixedHeightMajor = new TypedValue();
            a.getValue(R.styleable.Theme_windowFixedHeightMajor, mFixedHeightMajor);
        }
        if (a.hasValue(R.styleable.Theme_windowFixedHeightMinor)) {
            if (mFixedHeightMinor == null) mFixedHeightMinor = new TypedValue();
            a.getValue(R.styleable.Theme_windowFixedHeightMinor, mFixedHeightMinor);
        }

        final DisplayMetrics metrics = mActivity.getResources().getDisplayMetrics();
        final boolean isPortrait = metrics.widthPixels < metrics.heightPixels;
        int w = ViewGroup.LayoutParams.MATCH_PARENT;
        int h = ViewGroup.LayoutParams.MATCH_PARENT;

        final TypedValue tvw = isPortrait ? mFixedWidthMinor : mFixedWidthMajor;
        if (tvw != null && tvw.type != TypedValue.TYPE_NULL) {
            if (tvw.type == TypedValue.TYPE_DIMENSION) {
                w = (int) tvw.getDimension(metrics);
            } else if (tvw.type == TypedValue.TYPE_FRACTION) {
                w = (int) tvw.getFraction(metrics.widthPixels, metrics.widthPixels);
            }
        }

        final TypedValue tvh = isPortrait ? mFixedHeightMajor : mFixedHeightMinor;
        if (tvh != null && tvh.type != TypedValue.TYPE_NULL) {
            if (tvh.type == TypedValue.TYPE_DIMENSION) {
                h = (int) tvh.getDimension(metrics);
            } else if (tvh.type == TypedValue.TYPE_FRACTION) {
                h = (int) tvh.getFraction(metrics.heightPixels, metrics.heightPixels);
            }
        }

        if (w != ViewGroup.LayoutParams.MATCH_PARENT || h != ViewGroup.LayoutParams.MATCH_PARENT) {
            mActivity.getWindow().setLayout(w, h);
        }

        a.recycle();
    }

    @Override
    public boolean supportRequestWindowFeature(int featureId) {
        switch (featureId) {
            case FEATURE_ACTION_BAR:
                mHasActionBar = true;
                return true;
            case FEATURE_ACTION_BAR_OVERLAY:
                mOverlayActionBar = true;
                return true;
            case FEATURE_ACTION_MODE_OVERLAY:
                mOverlayActionMode = true;
                return true;
            case Window.FEATURE_PROGRESS:
                mFeatureProgress = true;
                return true;
            case Window.FEATURE_INDETERMINATE_PROGRESS:
                mFeatureIndeterminateProgress = true;
                return true;
            default:
                return mActivity.requestWindowFeature(featureId);
        }
    }

    @Override
    public void onTitleChanged(CharSequence title) {
        if (mDecorContentParent != null) {
            mDecorContentParent.setWindowTitle(title);
        } else if (getSupportActionBar() != null) {
            getSupportActionBar().setWindowTitle(title);
        } else {
            mTitleToSet = title;
        }
    }

    @Override
    public View onCreatePanelView(int featureId) {
        View panelView = null;

        // If there isn't an action mode currently being displayed
        if (mActionMode == null) {
            // Let our window callback try first
            WindowCallback callback = getWindowCallback();
            if (callback != null) {
                panelView = callback.onCreatePanelView(featureId);
            }

            if (panelView == null && mToolbarListMenuPresenter == null) {
                // Only check our panels if the callback didn't return a view and we do not have
                // a ListMenuPresenter for Toolbars. We check for the ListMenuPresenter because
                // once created, Toolbar needs to control the panel view regardless of whether it
                // has any non-action items to display.
                PanelFeatureState st = getPanelState(featureId, true);
                openPanel(st, null);
                if (st.isOpen) {
                    panelView = st.shownPanelView;
                }
            }
        }
        return panelView;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId != Window.FEATURE_OPTIONS_PANEL) {
            return getWindowCallback().onCreatePanelMenu(featureId, menu);
        }
        return false;
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId != Window.FEATURE_OPTIONS_PANEL) {
            return getWindowCallback().onPreparePanel(featureId, view, menu);
        }
        return false;
    }

    @Override
    public void onPanelClosed(final int featureId, Menu menu) {
        PanelFeatureState st = getPanelState(featureId, false);
        if (st != null) {
            // If we know about the feature id, update it's state
            closePanel(st, false);
        }

        if (featureId == FEATURE_ACTION_BAR) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.dispatchMenuVisibilityChanged(false);
            }
        } else if (!isDestroyed()) {
            // Only pass it through to the Activity's super impl if it's not ACTION_BAR. This is
            // because ICS+ will try and create a framework action bar due to this call
            mActivity.superOnPanelClosed(featureId, menu);
        }
    }

    @Override
    boolean onMenuOpened(final int featureId, Menu menu) {
        if (featureId == FEATURE_ACTION_BAR) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.dispatchMenuVisibilityChanged(true);
            }
            return true;
        } else {
            return mActivity.superOnMenuOpened(featureId, menu);
        }
    }

    @Override
    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        final WindowCallback cb = getWindowCallback();
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

        final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapper(callback);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            mActionMode = ab.startActionMode(wrappedCallback);
            if (mActionMode != null) {
                mActivity.onSupportActionModeStarted(mActionMode);
            }
        }

        if (mActionMode == null) {
            // If the action bar didn't provide an action mode, start the emulated window one
            mActionMode = startSupportActionModeFromWindow(wrappedCallback);
        }

        return mActionMode;
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        final ActionBar ab = getSupportActionBar();
        if (ab != null && ab.invalidateOptionsMenu()) return;

        invalidatePanelMenu(FEATURE_OPTIONS_PANEL);
    }

    @Override
    ActionMode startSupportActionModeFromWindow(ActionMode.Callback callback) {
        if (mActionMode != null) {
            mActionMode.finish();
        }

        final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapper(callback);
        final Context context = getActionBarThemedContext();

        if (mActionModeView == null) {
            if (mIsFloating) {
                mActionModeView = new ActionBarContextView(context);
                mActionModePopup = new PopupWindow(context, null,
                        R.attr.actionModePopupWindowStyle);
                mActionModePopup.setContentView(mActionModeView);
                mActionModePopup.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);

                TypedValue heightValue = new TypedValue();
                mActivity.getTheme().resolveAttribute(R.attr.actionBarSize, heightValue, true);
                final int height = TypedValue.complexToDimensionPixelSize(heightValue.data,
                        mActivity.getResources().getDisplayMetrics());
                mActionModeView.setContentHeight(height);
                mActionModePopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                mShowActionModePopup = new Runnable() {
                    public void run() {
                        mActionModePopup.showAtLocation(
                                mActionModeView,
                                Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
                    }
                };
            } else {
                ViewStubCompat stub = (ViewStubCompat) mActivity
                        .findViewById(R.id.action_mode_bar_stub);
                if (stub != null) {
                    // Set the layout inflater so that it is inflated with the action bar's context
                    stub.setLayoutInflater(LayoutInflater.from(context));
                    mActionModeView = (ActionBarContextView) stub.inflate();
                }
            }
        }

        if (mActionModeView != null) {
            mActionModeView.killMode();
            ActionMode mode = new StandaloneActionMode(context, mActionModeView, wrappedCallback,
                    mActionModePopup == null);
            if (callback.onCreateActionMode(mode, mode.getMenu())) {
                mode.invalidate();
                mActionModeView.initForMode(mode);
                mActionModeView.setVisibility(View.VISIBLE);
                mActionMode = mode;
                if (mActionModePopup != null) {
                    mActivity.getWindow().getDecorView().post(mShowActionModePopup);
                }
                mActionModeView.sendAccessibilityEvent(
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

                if (mActionModeView.getParent() != null) {
                    ViewCompat.requestApplyInsets((View) mActionModeView.getParent());
                }
            } else {
                mActionMode = null;
            }
        }
        if (mActionMode != null && mActivity != null) {
            mActivity.onSupportActionModeStarted(mActionMode);
        }
        return mActionMode;
    }

    @Override
    public boolean onBackPressed() {
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

        return false;
    }

    @Override
    void setSupportProgressBarVisibility(boolean visible) {
        updateProgressBars(visible ? Window.PROGRESS_VISIBILITY_ON :
                Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    void setSupportProgressBarIndeterminateVisibility(boolean visible) {
        updateProgressBars(visible ? Window.PROGRESS_VISIBILITY_ON :
                Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    void setSupportProgressBarIndeterminate(boolean indeterminate) {
        updateProgressBars(indeterminate ? Window.PROGRESS_INDETERMINATE_ON
                : Window.PROGRESS_INDETERMINATE_OFF);
    }

    @Override
    void setSupportProgress(int progress) {
        updateProgressBars(Window.PROGRESS_START + progress);
    }

    @Override
    int getHomeAsUpIndicatorAttrId() {
        return R.attr.homeAsUpIndicator;
    }

    @Override
    boolean onKeyShortcut(int keyCode, KeyEvent ev) {
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
    boolean onKeyDown(int keyCode, KeyEvent event) {
        // On API v7-10 we need to manually call onKeyShortcut() as this is not called
        // from the Activity
        return onKeyShortcut(keyCode, event);
    }

    @Override
    View createView(final String name, @NonNull AttributeSet attrs) {
        if (Build.VERSION.SDK_INT < 21) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            switch (name) {
                case "EditText":
                    return new TintEditText(mActivity, attrs);
                case "Spinner":
                    return new TintSpinner(mActivity, attrs);
                case "CheckBox":
                    return new TintCheckBox(mActivity, attrs);
                case "RadioButton":
                    return new TintRadioButton(mActivity, attrs);
                case "CheckedTextView":
                    return new TintCheckedTextView(mActivity, attrs);
            }
        }
        return null;
    }

    /**
     * Progress Bar function. Mostly extracted from PhoneWindow.java
     */
    private void updateProgressBars(int value) {
        ProgressBarCompat circularProgressBar = getCircularProgressBar();
        ProgressBarCompat horizontalProgressBar = getHorizontalProgressBar();

        if (value == Window.PROGRESS_VISIBILITY_ON) {
            if (mFeatureProgress) {
                int level = horizontalProgressBar.getProgress();
                int visibility = (horizontalProgressBar.isIndeterminate() || level < 10000) ?
                        View.VISIBLE : View.INVISIBLE;
                horizontalProgressBar.setVisibility(visibility);
            }
            if (mFeatureIndeterminateProgress) {
                circularProgressBar.setVisibility(View.VISIBLE);
            }
        } else if (value == Window.PROGRESS_VISIBILITY_OFF) {
            if (mFeatureProgress) {
                horizontalProgressBar.setVisibility(View.GONE);
            }
            if (mFeatureIndeterminateProgress) {
                circularProgressBar.setVisibility(View.GONE);
            }
        } else if (value == Window.PROGRESS_INDETERMINATE_ON) {
            horizontalProgressBar.setIndeterminate(true);
        } else if (value == Window.PROGRESS_INDETERMINATE_OFF) {
            horizontalProgressBar.setIndeterminate(false);
        } else if (Window.PROGRESS_START <= value && value <= Window.PROGRESS_END) {
            // We want to set the progress value before testing for visibility
            // so that when the progress bar becomes visible again, it has the
            // correct level.
            horizontalProgressBar.setProgress(value - Window.PROGRESS_START);

            if (value < Window.PROGRESS_END) {
                showProgressBars(horizontalProgressBar, circularProgressBar);
            } else {
                hideProgressBars(horizontalProgressBar, circularProgressBar);
            }
        }
    }

    private void openPanel(int featureId, KeyEvent event) {
        if (featureId == FEATURE_OPTIONS_PANEL && mDecorContentParent != null &&
                mDecorContentParent.canShowOverflowMenu() &&
                !ViewConfigurationCompat.hasPermanentMenuKey(ViewConfiguration.get(mActivity))) {
            mDecorContentParent.showOverflowMenu();
        } else {
            openPanel(getPanelState(featureId, true), event);
        }
    }

    private void openPanel(final PanelFeatureState st, KeyEvent event) {
        // Already open, return
        if (st.isOpen || isDestroyed()) {
            return;
        }

        // Don't open an options panel for honeycomb apps on xlarge devices.
        // (The app should be using an action bar for menu items.)
        if (st.featureId == FEATURE_OPTIONS_PANEL) {
            Context context = mActivity;
            Configuration config = context.getResources().getConfiguration();
            boolean isXLarge = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) ==
                    Configuration.SCREENLAYOUT_SIZE_XLARGE;
            boolean isHoneycombApp = context.getApplicationInfo().targetSdkVersion >=
                    android.os.Build.VERSION_CODES.HONEYCOMB;

            if (isXLarge && isHoneycombApp) {
                return;
            }
        }

        WindowCallback cb = getWindowCallback();
        if ((cb != null) && (!cb.onMenuOpened(st.featureId, st.menu))) {
            // Callback doesn't want the menu to open, reset any state
            closePanel(st, true);
            return;
        }

        // Prepare panel (should have been done before, but just in case)
        if (!preparePanel(st, event)) {
            return;
        }

        if (st.decorView == null || st.refreshDecorView) {
            initializePanelDecor(st);
        }

        // This will populate st.shownPanelView
        if (!initializePanelContent(st) || !st.hasPanelItems()) {
            return;
        }

        st.isHandled = false;
        st.isOpen = true;
    }

    private void initializePanelDecor(PanelFeatureState st) {
        st.decorView = mWindowDecor;
        st.setStyle(getActionBarThemedContext());
    }

    private void reopenMenu(MenuBuilder menu, boolean toggleMenuMode) {
        if (mDecorContentParent != null && mDecorContentParent.canShowOverflowMenu() &&
                (!ViewConfigurationCompat.hasPermanentMenuKey(ViewConfiguration.get(mActivity)) ||
                        mDecorContentParent.isOverflowMenuShowPending())) {

            WindowCallback cb = getWindowCallback();

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
                            cb.onPreparePanel(FEATURE_OPTIONS_PANEL, null, st.menu)) {
                        cb.onMenuOpened(FEATURE_ACTION_BAR, st.menu);
                        mDecorContentParent.showOverflowMenu();
                    }
                }
            } else {
                mDecorContentParent.hideOverflowMenu();
                if (!isDestroyed()) {
                    final PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, true);
                    mActivity.onPanelClosed(FEATURE_ACTION_BAR, st.menu);
                }
            }
            return;
        }

        PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, true);

        st.refreshDecorView = true;
        closePanel(st, false);

        openPanel(st, null);
    }

    private void showProgressBars(ProgressBarCompat horizontalProgressBar,
            ProgressBarCompat spinnyProgressBar) {
        if (mFeatureIndeterminateProgress && spinnyProgressBar.getVisibility() == View.INVISIBLE) {
            spinnyProgressBar.setVisibility(View.VISIBLE);
        }
        // Only show the progress bars if the primary progress is not complete
        if (mFeatureProgress && horizontalProgressBar.getProgress() < 10000) {
            horizontalProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBars(ProgressBarCompat horizontalProgressBar,
            ProgressBarCompat spinnyProgressBar) {
        if (mFeatureIndeterminateProgress && spinnyProgressBar.getVisibility() == View.VISIBLE) {
            spinnyProgressBar.setVisibility(View.INVISIBLE);
        }
        if (mFeatureProgress && horizontalProgressBar.getVisibility() == View.VISIBLE) {
            horizontalProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private ProgressBarCompat getCircularProgressBar() {
        ProgressBarCompat pb = (ProgressBarCompat) mActivity.findViewById(R.id.progress_circular);
        if (pb != null) {
            pb.setVisibility(View.INVISIBLE);
        }
        return pb;
    }

    private ProgressBarCompat getHorizontalProgressBar() {
        ProgressBarCompat pb = (ProgressBarCompat) mActivity.findViewById(R.id.progress_horizontal);
        if (pb != null) {
            pb.setVisibility(View.INVISIBLE);
        }
        return pb;
    }

    private boolean initializePanelMenu(final PanelFeatureState st) {
        Context context = mActivity;

        // If we have an action bar, initialize the menu with the right theme.
        if ((st.featureId == FEATURE_OPTIONS_PANEL || st.featureId == FEATURE_ACTION_BAR) &&
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

        final boolean isActionBarMenu =
                (st.featureId == FEATURE_OPTIONS_PANEL || st.featureId == FEATURE_ACTION_BAR);

        if (isActionBarMenu && mDecorContentParent != null) {
            // Enforce ordering guarantees around events so that the action bar never
            // dispatches menu-related events before the panel is prepared.
            mDecorContentParent.setMenuPrepared();
        }

        // Init the panel state's menu--return false if init failed
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
            if (!getWindowCallback().onCreatePanelMenu(st.featureId, st.menu)) {
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
        if (!getWindowCallback().onPreparePanel(FEATURE_OPTIONS_PANEL, null, st.menu)) {
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
        WindowCallback cb = getWindowCallback();
        if (cb != null && !isDestroyed()) {
            cb.onPanelClosed(FEATURE_ACTION_BAR, menu);
        }
        mClosingActionMenu = false;
    }

    private void closePanel(PanelFeatureState st, boolean doCallback) {
        if (doCallback && st.featureId == FEATURE_OPTIONS_PANEL &&
                mDecorContentParent != null && mDecorContentParent.isOverflowMenuShowing()) {
            checkCloseActionMenu(st.menu);
            return;
        }

        if (st.isOpen) {
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

        getWindowCallback().onPanelClosed(featureId, menu);
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

    final boolean performPanelShortcut(PanelFeatureState st, int keyCode, KeyEvent event,
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
        if ((featureId == FEATURE_ACTION_BAR || featureId == FEATURE_OPTIONS_PANEL)
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
                            mStatusGuard = new View(mActivity);
                            mStatusGuard.setBackgroundColor(mActivity.getResources()
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

    private void ensureToolbarListMenuPresenter() {
        if (mToolbarListMenuPresenter == null) {
            // First resolve panelMenuListTheme
            TypedValue outValue = new TypedValue();
            mActivity.getTheme().resolveAttribute(R.attr.panelMenuListTheme, outValue, true);

            Context context = new ContextThemeWrapper(mActivity,
                    outValue.resourceId != 0
                            ? outValue.resourceId
                            : R.style.Theme_AppCompat_CompactMenu);

            mToolbarListMenuPresenter = new ListMenuPresenter(context,
                    R.layout.abc_list_menu_item_layout);
        }
    }

    /**
     * Clears out internal reference when the action mode is destroyed.
     */
    private class ActionModeCallbackWrapper implements ActionMode.Callback {
        private ActionMode.Callback mWrapped;

        public ActionModeCallbackWrapper(ActionMode.Callback wrapped) {
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
                mActivity.getWindow().getDecorView().removeCallbacks(mShowActionModePopup);
                mActionModePopup.dismiss();
            } else if (mActionModeView != null) {
                mActionModeView.setVisibility(View.GONE);
                if (mActionModeView.getParent() != null) {
                    ViewCompat.requestApplyInsets((View) mActionModeView.getParent());
                }
            }
            if (mActionModeView != null) {
                mActionModeView.removeAllViews();
            }
            if (mActivity != null) {
                try {
                    mActivity.onSupportActionModeFinished(mActionMode);
                } catch (AbstractMethodError ame) {
                    // Older apps might not implement this callback method.
                }
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
                    mActivity.closeOptionsMenu();
                    closePanel(panel, allMenusAreClosing);
                }
            }
        }

        @Override
        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            if (subMenu == null && mHasActionBar) {
                WindowCallback cb = getWindowCallback();
                if (cb != null && !isDestroyed()) {
                    cb.onMenuOpened(FEATURE_ACTION_BAR, subMenu);
                }
            }
            return true;
        }
    }

    private final class ActionMenuPresenterCallback implements MenuPresenter.Callback {
        @Override
        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            WindowCallback cb = getWindowCallback();
            if (cb != null) {
                cb.onMenuOpened(FEATURE_ACTION_BAR, subMenu);
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

        /** Dynamic state of the panel. */
        ViewGroup decorView;

        /** The panel that we are actually showing. */
        View shownPanelView;

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

            private static SavedState readFromParcel(Parcel source) {
                SavedState savedState = new SavedState();
                savedState.featureId = source.readInt();
                savedState.isOpen = source.readInt() == 1;

                if (savedState.isOpen) {
                    savedState.menuState = source.readBundle();
                }

                return savedState;
            }

            public static final Parcelable.Creator<SavedState> CREATOR
                    = new Parcelable.Creator<SavedState>() {
                public SavedState createFromParcel(Parcel in) {
                    return readFromParcel(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
        }
    }

}
