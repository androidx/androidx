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

package androidx.appcompat.app;

import static android.view.View.GONE;
import static android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.Window.FEATURE_OPTIONS_PANEL;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.appcompat.app.LocaleOverlayHelper.combineLocalesIfOverlayExists;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.R;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.view.StandaloneActionMode;
import androidx.appcompat.view.SupportActionModeWrapper;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.WindowCallbackWrapper;
import androidx.appcompat.view.menu.ListMenuPresenter;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.widget.ActionBarContextView;
import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.appcompat.widget.ContentFrameLayout;
import androidx.appcompat.widget.DecorContentParent;
import androidx.appcompat.widget.FitWindowsViewGroup;
import androidx.appcompat.widget.TintTypedArray;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.VectorEnabledTintResources;
import androidx.appcompat.widget.ViewStubCompat;
import androidx.appcompat.widget.ViewUtils;
import androidx.collection.SimpleArrayMap;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.KeyEventDispatcher;
import androidx.core.view.LayoutInflaterCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.core.view.ViewPropertyAnimatorListenerAdapter;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.PopupWindowCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.xmlpull.v1.XmlPullParser;

import java.util.List;
import java.util.Locale;

/**
 */
@RestrictTo(LIBRARY)
class AppCompatDelegateImpl extends AppCompatDelegate
        implements MenuBuilder.Callback, LayoutInflater.Factory2 {

    private static final SimpleArrayMap<String, Integer> sLocalNightModes = new SimpleArrayMap<>();
    private static final boolean IS_PRE_LOLLIPOP = Build.VERSION.SDK_INT < 21;

    private static final int[] sWindowBackgroundStyleable = {android.R.attr.windowBackground};

    /**
     * Flag indicating whether we can return a different context from attachBaseContext().
     * Unfortunately, doing so breaks Robolectric tests, so we skip night mode application there.
     */
    private static final boolean sCanReturnDifferentContext =
            !"robolectric".equals(Build.FINGERPRINT);

    private static boolean sInstalledExceptionHandler;

    static final String EXCEPTION_HANDLER_MESSAGE_SUFFIX= ". If the resource you are"
            + " trying to use is a vector resource, you may be referencing it in an unsupported"
            + " way. See AppCompatDelegate.setCompatVectorFromResourcesEnabled() for more info.";

    static {
        if (IS_PRE_LOLLIPOP && !sInstalledExceptionHandler) {
            final Thread.UncaughtExceptionHandler defHandler
                    = Thread.getDefaultUncaughtExceptionHandler();

            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(@NonNull Thread thread,
                        final @NonNull Throwable throwable) {
                    if (shouldWrapException(throwable)) {
                        // Now wrap the throwable, but append some extra information to the message
                        final Throwable wrapped = new Resources.NotFoundException(
                                throwable.getMessage() + EXCEPTION_HANDLER_MESSAGE_SUFFIX);
                        wrapped.initCause(throwable.getCause());
                        wrapped.setStackTrace(throwable.getStackTrace());
                        defHandler.uncaughtException(thread, wrapped);
                    } else {
                        defHandler.uncaughtException(thread, throwable);
                    }
                }

                private boolean shouldWrapException(Throwable throwable) {
                    if (throwable instanceof Resources.NotFoundException) {
                        final String message = throwable.getMessage();
                        return message != null && (message.contains("drawable")
                                || message.contains("Drawable"));
                    }
                    return false;
                }
            });

            sInstalledExceptionHandler = true;
        }
    }

    final Object mHost;
    final Context mContext;
    Window mWindow;
    private AppCompatWindowCallback mAppCompatWindowCallback;
    final AppCompatCallback mAppCompatCallback;

    ActionBar mActionBar;
    MenuInflater mMenuInflater;

    private CharSequence mTitle;

    private DecorContentParent mDecorContentParent;
    private ActionMenuPresenterCallback mActionMenuPresenterCallback;
    private PanelMenuPresenterCallback mPanelMenuPresenterCallback;

    ActionMode mActionMode;
    ActionBarContextView mActionModeView;
    PopupWindow mActionModePopup;
    Runnable mShowActionModePopup;
    ViewPropertyAnimatorCompat mFadeAnim = null;

    private boolean mHandleNativeActionModes = true; // defaults to true

    // true if we have installed a window sub-decor layout.
    private boolean mSubDecorInstalled;
    ViewGroup mSubDecor;

    private TextView mTitleView;
    private View mStatusGuard;

    // Used to keep track of Progress Bar Window features
    private boolean mFeatureProgress, mFeatureIndeterminateProgress;

    // true if this activity has an action bar.
    boolean mHasActionBar;
    // true if this activity's action bar overlays other activity content.
    boolean mOverlayActionBar;
    // true if this any action modes should overlay the activity content
    boolean mOverlayActionMode;
    // true if this activity is floating (e.g. Dialog)
    boolean mIsFloating;
    // true if this activity has no title
    boolean mWindowNoTitle;

    // Used for emulating PanelFeatureState
    private boolean mClosingActionMenu;
    private PanelFeatureState[] mPanels;
    private PanelFeatureState mPreparedPanel;

    private boolean mLongPressBackDown;

    private boolean mBaseContextAttached;
    // true after the first call to onCreated.
    private boolean mCreated;
    // true after the first (and only) call to onDestroyed.
    boolean mDestroyed;

    /**
     * The configuration from the most recent call to either onConfigurationChanged or onCreate.
     * May be null if neither method has been called yet.
     */
    private Configuration mEffectiveConfiguration;

    @NightMode
    private int mLocalNightMode = MODE_NIGHT_UNSPECIFIED;

    private int mThemeResId;
    private int mActivityHandlesConfigFlags;
    private boolean mActivityHandlesConfigFlagsChecked;
    private AutoNightModeManager mAutoTimeNightModeManager;
    private AutoNightModeManager mAutoBatteryNightModeManager;

    boolean mInvalidatePanelMenuPosted;
    int mInvalidatePanelMenuFeatures;
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
    private LayoutIncludeDetector mLayoutIncludeDetector;
    private OnBackInvokedDispatcher mDispatcher;
    private OnBackInvokedCallback mBackCallback;

    AppCompatDelegateImpl(Activity activity, AppCompatCallback callback) {
        this(activity, null, callback, activity);
    }

    AppCompatDelegateImpl(Dialog dialog, AppCompatCallback callback) {
        this(dialog.getContext(), dialog.getWindow(), callback, dialog);
    }

    AppCompatDelegateImpl(Context context, Window window, AppCompatCallback callback) {
        this(context, window, callback, context);
    }

    AppCompatDelegateImpl(Context context, Activity activity, AppCompatCallback callback) {
        this(context, null, callback, activity);
    }

    private AppCompatDelegateImpl(Context context, Window window, AppCompatCallback callback,
            Object host) {
        mContext = context;
        mAppCompatCallback = callback;
        mHost = host;

        if (mLocalNightMode == MODE_NIGHT_UNSPECIFIED && mHost instanceof Dialog) {
            final AppCompatActivity activity = tryUnwrapContext();
            if (activity != null) {
                // This code path is used to detect when this Delegate is a child Delegate from
                // an Activity, primarily for Dialogs. Dialogs use the Activity as it's Context,
                // so we want to make sure that the this 'child' delegate does not interfere
                // with the Activity config. The simplest way to do that is to match the
                // outer Activity's local night mode
                mLocalNightMode = activity.getDelegate().getLocalNightMode();
            }
        }
        if (mLocalNightMode == MODE_NIGHT_UNSPECIFIED) {
            // Try and read the current night mode from our static store
            final Integer value = sLocalNightModes.get(mHost.getClass().getName());
            if (value != null) {
                mLocalNightMode = value;
                // Finally remove the value
                sLocalNightModes.remove(mHost.getClass().getName());
            }
        }

        if (window != null) {
            attachToWindow(window);
        }

        // Preload appcompat-specific handling of drawables that should be handled in a special
        // way (for tinting etc). After the following line completes, calls from AppCompatResources
        // to ResourceManagerInternal (in appcompat-resources) will handle those internal drawable
        // paths correctly without having to go through AppCompatDrawableManager APIs.
        AppCompatDrawableManager.preload();
    }

    @Override
    @RequiresApi(33)
    public void setOnBackInvokedDispatcher(@Nullable OnBackInvokedDispatcher dispatcher) {
        super.setOnBackInvokedDispatcher(dispatcher);

        // Clean up the callback on the previous dispatcher, if necessary.
        if (mDispatcher != null && mBackCallback != null) {
            Api33Impl.unregisterOnBackInvokedCallback(mDispatcher, mBackCallback);
            mBackCallback = null;
        }

        if (dispatcher == null && mHost instanceof Activity
                && ((Activity) mHost).getWindow() != null) {
            mDispatcher = Api33Impl.getOnBackInvokedDispatcher((Activity) mHost);
        } else {
            mDispatcher = dispatcher;
        }

        // Register a callback on the new dispatcher, if necessary.
        updateBackInvokedCallbackState();
    }

    void updateBackInvokedCallbackState() {
        if (Build.VERSION.SDK_INT >= 33) {
            boolean shouldRegister = shouldRegisterBackInvokedCallback();
            if (shouldRegister && mBackCallback == null) {
                mBackCallback = Api33Impl.registerOnBackPressedCallback(mDispatcher, this);
            } else if (!shouldRegister && mBackCallback != null) {
                Api33Impl.unregisterOnBackInvokedCallback(mDispatcher, mBackCallback);
                mBackCallback = null;
            }
        }
    }

    @Override
    @CallSuper
    public @NonNull Context attachBaseContext2(final @NonNull Context baseContext) {
        mBaseContextAttached = true;

        // This is a tricky method. Here are some things to avoid:
        // 1. Don't modify the configuration of the Application context. All changes should remain
        //    local to the Activity to avoid conflicting with other Activities and internal logic.
        // 2. Don't use createConfigurationContext() with Robolectric because Robolectric relies on
        //    method overrides.
        // 3. Don't use createConfigurationContext() unless you're able to retain the base context's
        //    theme stack. Not the last theme applied -- the entire stack of applied themes.
        // 4. Don't use applyOverrideConfiguration() unless you're able to retain the base context's
        //    configuration overrides (as distinct from the entire configuration).

        final int modeToApply = mapNightMode(baseContext, calculateNightMode());

        if (isAutoStorageOptedIn(baseContext)) {
            // If the developer has opted in to auto store the locales, then we use
            // syncRequestedAndStoredLocales() to load the saved locales from storage. This is
            // performed only during cold app start-ups because in other cases the locales can be
            // found in the static storage.
            syncRequestedAndStoredLocales(baseContext);
        }
        final LocaleListCompat localesToApply = calculateApplicationLocales(baseContext);

        // If the base context is a ContextThemeWrapper (thus not an Application context)
        // and nobody's touched its Resources yet, we can shortcut and directly apply our
        // override configuration.
        if (baseContext instanceof android.view.ContextThemeWrapper) {
            final Configuration config = createOverrideAppConfiguration(
                    baseContext, modeToApply, localesToApply, null, false);
            if (DEBUG) {
                Log.d(TAG, String.format("Attempting to apply config to base context: %s",
                        config.toString()));
            }

            try {
                ((android.view.ContextThemeWrapper) baseContext).applyOverrideConfiguration(config);
                return baseContext;
            } catch (IllegalStateException e) {
                if (DEBUG) {
                    Log.d(TAG, "Failed to apply configuration to base context", e);
                }
            }
        }

        // Again, but using the AppCompat version of ContextThemeWrapper.
        if (baseContext instanceof ContextThemeWrapper) {
            final Configuration config = createOverrideAppConfiguration(
                    baseContext, modeToApply, localesToApply, null, false);
            if (DEBUG) {
                Log.d(TAG, String.format("Attempting to apply config to base context: %s",
                        config.toString()));
            }

            try {
                ((ContextThemeWrapper) baseContext).applyOverrideConfiguration(config);
                return baseContext;
            } catch (IllegalStateException e) {
                if (DEBUG) {
                    Log.d(TAG, "Failed to apply configuration to base context", e);
                }
            }
        }

        // We can't apply the configuration directly to the existing base context, so we need to
        // wrap it. We can't create a new configuration context since the app may rely on method
        // overrides or a specific theme -- neither of which are preserved when creating a
        // configuration context. Instead, we'll make a best-effort at wrapping the context and
        // rebasing the original theme.
        if (!sCanReturnDifferentContext) {
            return super.attachBaseContext2(baseContext);
        }

        Configuration configOverlay = null;

        // There is a bug in createConfigurationContext where it applies overrides to the
        // canonical configuration, e.g. ActivityThread.mCurrentConfig, rather than the base
        // configuration, e.g. Activity.getResources().getConfiguration(). We can lean on this
        // bug to obtain a reference configuration and reconstruct any custom configuration
        // that may have been applied by the app, thereby avoiding the bug later on.
        Configuration overrideConfig = new Configuration();
        // We have to modify a value to receive a new Configuration, so use one that developers
        // can't override.
        overrideConfig.uiMode = -1;
        // Workaround for incorrect default fontScale on earlier SDKs.
        overrideConfig.fontScale = 0f;
        Configuration referenceConfig =
                baseContext.createConfigurationContext(overrideConfig)
                        .getResources().getConfiguration();
        // Revert the uiMode change so that the diff doesn't include uiMode.
        Configuration baseConfig = baseContext.getResources().getConfiguration();
        referenceConfig.uiMode = baseConfig.uiMode;

        // Extract any customizations as an overlay.
        if (!referenceConfig.equals(baseConfig)) {
            configOverlay = generateConfigDelta(referenceConfig, baseConfig);
            if (DEBUG) {
                Log.d(TAG, "Application config (" + referenceConfig + ") does not match base "
                        + "config (" + baseConfig + "), using base overlay: " + configOverlay);
            }
        }

        final Configuration config = createOverrideAppConfiguration(
                baseContext, modeToApply, localesToApply, configOverlay, true);
        if (DEBUG) {
            Log.d(TAG, String.format("Applying night mode using ContextThemeWrapper and "
                    + "applyOverrideConfiguration(). Config: %s", config.toString()));
        }

        // Next, we'll wrap the base context to ensure any method overrides or themes are left
        // intact. Since ThemeOverlay.AppCompat theme is empty, we'll get the base context's theme.
        final ContextThemeWrapper wrappedContext = new ContextThemeWrapper(baseContext,
                R.style.Theme_AppCompat_Empty);
        wrappedContext.applyOverrideConfiguration(config);

        // Check whether the base context has an explicit theme or is able to obtain one
        // from its outer context. If it throws an NPE because we're at an invalid point in app
        // initialization, we don't need to worry about rebasing under the new configuration.
        boolean needsThemeRebase;
        try {
            needsThemeRebase = baseContext.getTheme() != null;
        } catch (NullPointerException e) {
            needsThemeRebase = false;
        }

        if (needsThemeRebase) {
            // Attempt to rebase the old theme within the new configuration. This will only
            // work on SDK 23 and up, but it's unlikely that we're keeping the base theme
            // anyway so maybe nobody will notice. Note that calling getTheme() will clone
            // the base context's theme into the wrapped context's theme.
            ResourcesCompat.ThemeCompat.rebase(wrappedContext.getTheme());
        }

        return super.attachBaseContext2(wrappedContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // attachBaseContext will only be called from an Activity, so make sure we switch this for
        // Dialogs, etc
        mBaseContextAttached = true;

        // Our implicit call to applyApplicationSpecificConfig() should not recreate
        // until after the Activity is created
        applyApplicationSpecificConfig(false);

        // We lazily fetch the Window for Activities, to allow DayNight to apply in
        // attachBaseContext
        ensureWindow();

        if (mHost instanceof Activity) {
            String parentActivityName = null;
            try {
                parentActivityName = NavUtils.getParentActivityName((Activity) mHost);
            } catch (IllegalArgumentException iae) {
                // Ignore in this case
            }
            if (parentActivityName != null) {
                // Peek at the Action Bar and update it if it already exists
                ActionBar ab = peekSupportActionBar();
                if (ab == null) {
                    mEnableDefaultActionBarUp = true;
                } else {
                    ab.setDefaultDisplayHomeAsUpEnabled(true);
                }
            }

            // Only activity-hosted delegates should apply night mode changes.
            addActiveDelegate(this);
        }

        mEffectiveConfiguration = new Configuration(mContext.getResources().getConfiguration());
        mCreated = true;
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        // Make sure that the sub decor is installed
        ensureSubDecor();
    }

    @Override
    public ActionBar getSupportActionBar() {
        // The Action Bar should be lazily created as hasActionBar
        // could change after onCreate
        initWindowDecorActionBar();
        return mActionBar;
    }

    final ActionBar peekSupportActionBar() {
        return mActionBar;
    }

    final Window.Callback getWindowCallback() {
        return mWindow.getCallback();
    }

    private void initWindowDecorActionBar() {
        ensureSubDecor();

        if (!mHasActionBar || mActionBar != null) {
            return;
        }

        if (mHost instanceof Activity) {
            mActionBar = new WindowDecorActionBar((Activity) mHost, mOverlayActionBar);
        } else if (mHost instanceof Dialog) {
            mActionBar = new WindowDecorActionBar((Dialog) mHost);
        }
        if (mActionBar != null) {
            mActionBar.setDefaultDisplayHomeAsUpEnabled(mEnableDefaultActionBarUp);
        }
    }

    @Override
    public void setSupportActionBar(Toolbar toolbar) {
        if (!(mHost instanceof Activity)) {
            // Only Activities support custom Action Bars
            return;
        }

        final ActionBar ab = getSupportActionBar();
        if (ab instanceof WindowDecorActionBar) {
            throw new IllegalStateException("This Activity already has an action bar supplied " +
                    "by the window decor. Do not request Window.FEATURE_SUPPORT_ACTION_BAR and set " +
                    "windowActionBar to false in your theme to use a Toolbar instead.");
        }

        // If we reach here then we're setting a new action bar
        // First clear out the MenuInflater to make sure that it is valid for the new Action Bar
        mMenuInflater = null;

        // If we have an action bar currently, destroy it
        if (ab != null) {
            ab.onDestroy();
        }
        mActionBar = null;

        if (toolbar != null) {
            final ToolbarActionBar tbab = new ToolbarActionBar(toolbar, getTitle(),
                    mAppCompatWindowCallback);
            mActionBar = tbab;
            // Set the nested action bar window callback so that it receive menu events
            mAppCompatWindowCallback.setActionBarCallback(tbab.mMenuCallback);
            // Toolbars managed by AppCompat should handle their own back invocations.
            toolbar.setBackInvokedCallbackEnabled(true);
        } else {
            // Clear the nested action bar window callback
            mAppCompatWindowCallback.setActionBarCallback(null);
        }

        invalidateOptionsMenu();
    }

    final Context getActionBarThemedContext() {
        Context context = null;

        // If we have an action bar, let it return a themed context
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            context = ab.getThemedContext();
        }

        if (context == null) {
            context = mContext;
        }
        return context;
    }

    @Override
    public MenuInflater getMenuInflater() {
        // Make sure that action views can get an appropriate theme.
        if (mMenuInflater == null) {
            initWindowDecorActionBar();
            mMenuInflater = new SupportMenuInflater(
                    mActionBar != null ? mActionBar.getThemedContext() : mContext);
        }
        return mMenuInflater;
    }

    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    @Override
    public <T extends View> @Nullable T findViewById(@IdRes int id) {
        ensureSubDecor();
        return (T) mWindow.findViewById(id);
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

        // Make sure that the DrawableManager knows about the new config
        AppCompatDrawableManager.get().onConfigurationChanged(mContext);

        // Cache the last-seen configuration before calling applyDayNight, since applyDayNight
        // inspects the last-seen configuration. Otherwise, we'll recurse back to this method.
        mEffectiveConfiguration = new Configuration(mContext.getResources().getConfiguration());

        // Re-apply Day/Night with the new configuration but disable recreations.
        // Since this configuration change has only just happened we can safely just update the
        // resources now.
        // For locales, no re-application is required since locales must have already been applied
        // to the configuration when AppCompatDelegate.setApplicationLocales() is called.
        // Also, in the case where an invalid locale is passed at the top position in the input
        // locales, framework re-adjusts the list to bring forward the most suitable locale in
        // the configuration. Therefore if we apply the new locales here again, the code will get
        // stuck in a loop of attempted re-application since the configuration locales are never
        // the same as we applied. The same thing is valid during the case when configChanges are
        // handled by the application.
        applyApplicationSpecificConfig(false,
                /* isLocalesApplicationRequired */ false);
    }

    @Override
    public void onStart() {
        // This will apply day/night if the time has changed, it will also call through to
        // setupAutoNightModeIfNeeded().
        // For locales, no re-application is required since the requested locales must have
        // already been updated before this point in activity lifecycle.
        // Also, in the case where an invalid locale is passed at the top position in the input
        // locales, framework re-adjusts the list to bring forward the most suitable locale in
        // the configuration. Therefore if we apply the new locales here again, the code will get
        // stuck in a loop of attempted re-application since the configuration locales are never
        // the same as we applied.
        applyApplicationSpecificConfig(true,
                /* isLocalesApplicationRequired */false);
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
        ViewGroup contentParent = mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v);
        mAppCompatWindowCallback.bypassOnContentChanged(mWindow.getCallback());
    }

    @Override
    public void setContentView(int resId) {
        ensureSubDecor();
        ViewGroup contentParent = mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        LayoutInflater.from(mContext).inflate(resId, contentParent);
        mAppCompatWindowCallback.bypassOnContentChanged(mWindow.getCallback());
    }

    @Override
    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        ViewGroup contentParent = mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v, lp);
        mAppCompatWindowCallback.bypassOnContentChanged(mWindow.getCallback());
    }

    @Override
    public void addContentView(View v, ViewGroup.LayoutParams lp) {
        ensureSubDecor();
        ViewGroup contentParent = mSubDecor.findViewById(android.R.id.content);
        contentParent.addView(v, lp);
        mAppCompatWindowCallback.bypassOnContentChanged(mWindow.getCallback());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public void onDestroy() {
        if (mHost instanceof Activity) {
            removeActivityDelegate(this);
        }

        if (mInvalidatePanelMenuPosted) {
            mWindow.getDecorView().removeCallbacks(mInvalidatePanelMenuRunnable);
        }

        mDestroyed = true;

        if (mLocalNightMode != MODE_NIGHT_UNSPECIFIED
                && mHost instanceof Activity
                && ((Activity) mHost).isChangingConfigurations()) {
            // If we have a local night mode set, save it
            sLocalNightModes.put(mHost.getClass().getName(), mLocalNightMode);
        } else {
            sLocalNightModes.remove(mHost.getClass().getName());
        }

        if (mActionBar != null) {
            mActionBar.onDestroy();
        }

        // Make sure we clean up any receivers setup for AUTO mode
        cleanupAutoManagers();
    }

    private void cleanupAutoManagers() {
        // Make sure we clean up any receivers setup for AUTO mode
        if (mAutoTimeNightModeManager != null) {
            mAutoTimeNightModeManager.cleanup();
        }
        if (mAutoBatteryNightModeManager != null) {
            mAutoBatteryNightModeManager.cleanup();
        }
    }

    @Override
    public void setTheme(@StyleRes int themeResId) {
        mThemeResId = themeResId;
    }

    private void ensureWindow() {
        // We lazily fetch the Window for Activities, to allow DayNight to apply in
        // attachBaseContext
        if (mWindow == null && mHost instanceof Activity) {
            attachToWindow(((Activity) mHost).getWindow());
        }
        if (mWindow == null) {
            throw new IllegalStateException("We have not been given a Window");
        }
    }

    private void attachToWindow(@NonNull Window window) {
        if (mWindow != null) {
            throw new IllegalStateException(
                    "AppCompat has already installed itself into the Window");
        }

        final Window.Callback callback = window.getCallback();
        if (callback instanceof AppCompatWindowCallback) {
            throw new IllegalStateException(
                    "AppCompat has already installed itself into the Window");
        }
        mAppCompatWindowCallback = new AppCompatWindowCallback(callback);
        // Now install the new callback
        window.setCallback(mAppCompatWindowCallback);

        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(
                mContext, null, sWindowBackgroundStyleable);
        final Drawable winBg = a.getDrawableIfKnown(0);
        if (winBg != null) {
            // Now set the background drawable
            window.setBackgroundDrawable(winBg);
        }
        a.recycle();

        mWindow = window;

        // Obtain a default dispatcher, if we still need one.
        if (Build.VERSION.SDK_INT >= 33 && mDispatcher == null) {
            setOnBackInvokedDispatcher(null);
        }
    }

    private void ensureSubDecor() {
        if (!mSubDecorInstalled) {
            mSubDecor = createSubDecor();

            // If a title was set before we installed the decor, propagate it now
            CharSequence title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                if (mDecorContentParent != null) {
                    mDecorContentParent.setWindowTitle(title);
                } else if (peekSupportActionBar() != null) {
                    peekSupportActionBar().setWindowTitle(title);
                } else if (mTitleView != null) {
                    mTitleView.setText(title);
                }
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
            if (!mDestroyed && (st == null || st.menu == null)) {
                invalidatePanelMenu(FEATURE_SUPPORT_ACTION_BAR);
            }
        }
    }

    private ViewGroup createSubDecor() {
        TypedArray a = mContext.obtainStyledAttributes(R.styleable.AppCompatTheme);

        if (!a.hasValue(R.styleable.AppCompatTheme_windowActionBar)) {
            a.recycle();
            throw new IllegalStateException(
                    "You need to use a Theme.AppCompat theme (or descendant) with this activity.");
        }

        if (a.getBoolean(R.styleable.AppCompatTheme_windowNoTitle, false)) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else if (a.getBoolean(R.styleable.AppCompatTheme_windowActionBar, false)) {
            // Don't allow an action bar if there is no title.
            requestWindowFeature(FEATURE_SUPPORT_ACTION_BAR);
        }
        if (a.getBoolean(R.styleable.AppCompatTheme_windowActionBarOverlay, false)) {
            requestWindowFeature(FEATURE_SUPPORT_ACTION_BAR_OVERLAY);
        }
        if (a.getBoolean(R.styleable.AppCompatTheme_windowActionModeOverlay, false)) {
            requestWindowFeature(FEATURE_ACTION_MODE_OVERLAY);
        }
        mIsFloating = a.getBoolean(R.styleable.AppCompatTheme_android_windowIsFloating, false);
        a.recycle();

        // Now let's make sure that the Window has installed its decor by retrieving it
        ensureWindow();
        mWindow.getDecorView();

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
                /*
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

                /*
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

        if (Build.VERSION.SDK_INT >= 21) {
            // If we're running on L or above, we can rely on ViewCompat's
            // setOnApplyWindowInsetsListener
            ViewCompat.setOnApplyWindowInsetsListener(subDecor, new OnApplyWindowInsetsListener() {
                        @Override
                        public WindowInsetsCompat onApplyWindowInsets(View v,
                                WindowInsetsCompat insets) {
                            final int top = insets.getSystemWindowInsetTop();
                            final int newTop = updateStatusGuard(insets, null);

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
        } else if (subDecor instanceof FitWindowsViewGroup) {
            // Else, we need to use our own FitWindowsViewGroup handling
            ((FitWindowsViewGroup) subDecor).setOnFitSystemWindowsListener(
                    new FitWindowsViewGroup.OnFitSystemWindowsListener() {
                        @Override
                        public void onFitSystemWindows(Rect insets) {
                            insets.top = updateStatusGuard(null, insets);
                        }
                    });
        }

        if (mDecorContentParent == null) {
            mTitleView = (TextView) subDecor.findViewById(R.id.title);
        }

        // Make the decor optionally fit system windows, like the window's decor
        ViewUtils.makeOptionalFitsSystemWindows(subDecor);

        final ContentFrameLayout contentView = (ContentFrameLayout) subDecor.findViewById(
                R.id.action_bar_activity_content);

        final ViewGroup windowContentView = (ViewGroup) mWindow.findViewById(android.R.id.content);
        if (windowContentView != null) {
            // There might be Views already added to the Window's content view so we need to
            // migrate them to our content view
            while (windowContentView.getChildCount() > 0) {
                final View child = windowContentView.getChildAt(0);
                windowContentView.removeViewAt(0);
                contentView.addView(child);
            }

            // Change our content FrameLayout to use the android.R.id.content id.
            // Useful for fragments.
            windowContentView.setId(View.NO_ID);
            contentView.setId(android.R.id.content);

            // The decorContent may have a foreground drawable set (windowContentOverlay).
            // Remove this as we handle it ourselves
            if (windowContentView instanceof FrameLayout) {
                ((FrameLayout) windowContentView).setForeground(null);
            }
        }

        // Now set the Window's content view with the decor
        mWindow.setContentView(subDecor);

        contentView.setAttachListener(new ContentFrameLayout.OnAttachListener() {
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
        final View windowDecor = mWindow.getDecorView();
        cfl.setDecorPadding(windowDecor.getPaddingLeft(),
                windowDecor.getPaddingTop(), windowDecor.getPaddingRight(),
                windowDecor.getPaddingBottom());

        TypedArray a = mContext.obtainStyledAttributes(R.styleable.AppCompatTheme);
        a.getValue(R.styleable.AppCompatTheme_windowMinWidthMajor, cfl.getMinWidthMajor());
        a.getValue(R.styleable.AppCompatTheme_windowMinWidthMinor, cfl.getMinWidthMinor());

        if (a.hasValue(R.styleable.AppCompatTheme_windowFixedWidthMajor)) {
            a.getValue(R.styleable.AppCompatTheme_windowFixedWidthMajor,
                    cfl.getFixedWidthMajor());
        }
        if (a.hasValue(R.styleable.AppCompatTheme_windowFixedWidthMinor)) {
            a.getValue(R.styleable.AppCompatTheme_windowFixedWidthMinor,
                    cfl.getFixedWidthMinor());
        }
        if (a.hasValue(R.styleable.AppCompatTheme_windowFixedHeightMajor)) {
            a.getValue(R.styleable.AppCompatTheme_windowFixedHeightMajor,
                    cfl.getFixedHeightMajor());
        }
        if (a.hasValue(R.styleable.AppCompatTheme_windowFixedHeightMinor)) {
            a.getValue(R.styleable.AppCompatTheme_windowFixedHeightMinor,
                    cfl.getFixedHeightMinor());
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
        boolean result = false;
        switch (sanitizeWindowFeatureId(featureId)) {
            case FEATURE_SUPPORT_ACTION_BAR:
                result = mHasActionBar;
                break;
            case FEATURE_SUPPORT_ACTION_BAR_OVERLAY:
                result = mOverlayActionBar;
                break;
            case FEATURE_ACTION_MODE_OVERLAY:
                result = mOverlayActionMode;
                break;
            case Window.FEATURE_PROGRESS:
                result = mFeatureProgress;
                break;
            case Window.FEATURE_INDETERMINATE_PROGRESS:
                result = mFeatureIndeterminateProgress;
                break;
            case Window.FEATURE_NO_TITLE:
                result = mWindowNoTitle;
                break;
        }
        return result || mWindow.hasFeature(featureId);
    }

    @Override
    public final void setTitle(CharSequence title) {
        mTitle = title;

        if (mDecorContentParent != null) {
            mDecorContentParent.setWindowTitle(title);
        } else if (peekSupportActionBar() != null) {
            peekSupportActionBar().setWindowTitle(title);
        } else if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }

    final CharSequence getTitle() {
        // If the original window callback is an Activity, we'll use its title
        if (mHost instanceof Activity) {
            return ((Activity) mHost).getTitle();
        }
        // Else, we'll return the title we have recorded ourselves
        return mTitle;
    }

    void onPanelClosed(final int featureId) {
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

    void onMenuOpened(final int featureId) {
        if (featureId == FEATURE_SUPPORT_ACTION_BAR) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.dispatchMenuVisibilityChanged(true);
            }
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
        final Window.Callback cb = getWindowCallback();
        if (cb != null && !mDestroyed) {
            final PanelFeatureState panel = findMenuPanel(menu.getRootMenu());
            if (panel != null) {
                return cb.onMenuItemSelected(panel.featureId, item);
            }
        }
        return false;
    }

    @Override
    public void onMenuModeChange(@NonNull MenuBuilder menu) {
        reopenMenu(true);
    }

    @Override
    public ActionMode startSupportActionMode(final ActionMode.@NonNull Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("ActionMode callback can not be null.");
        }

        if (mActionMode != null) {
            mActionMode.finish();
        }

        final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapperV9(callback);

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

        // mActionMode changed.
        updateBackInvokedCallbackState();

        return mActionMode;
    }

    @Override
    public void invalidateOptionsMenu() {
        if (peekSupportActionBar() == null || getSupportActionBar().invalidateOptionsMenu()) return;

        invalidatePanelMenu(FEATURE_OPTIONS_PANEL);
    }

    ActionMode startSupportActionModeFromWindow(ActionMode.@NonNull Callback callback) {
        endOnGoingFadeAnimation();
        if (mActionMode != null) {
            mActionMode.finish();
        }

        if (!(callback instanceof ActionModeCallbackWrapperV9)) {
            // If the callback hasn't been wrapped yet, wrap it
            callback = new ActionModeCallbackWrapperV9(callback);
        }

        ActionMode mode = null;
        if (mAppCompatCallback != null && !mDestroyed) {
            try {
                mode = mAppCompatCallback.onWindowStartingSupportActionMode(callback);
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
                        @Override
                        public void run() {
                            mActionModePopup.showAtLocation(
                                    mActionModeView,
                                    Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
                            endOnGoingFadeAnimation();

                            if (shouldAnimateActionModeView()) {
                                mActionModeView.setAlpha(0f);
                                mFadeAnim = ViewCompat.animate(mActionModeView).alpha(1f);
                                mFadeAnim.setListener(new ViewPropertyAnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationStart(View view) {
                                        mActionModeView.setVisibility(VISIBLE);
                                    }

                                    @Override
                                    public void onAnimationEnd(View view) {
                                        mActionModeView.setAlpha(1f);
                                        mFadeAnim.setListener(null);
                                        mFadeAnim = null;
                                    }
                                });
                            } else {
                                mActionModeView.setAlpha(1f);
                                mActionModeView.setVisibility(VISIBLE);
                            }
                        }
                    };
                } else {
                    ViewStubCompat stub = mSubDecor.findViewById(R.id.action_mode_bar_stub);
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
                        callback, mActionModePopup == null);
                if (callback.onCreateActionMode(mode, mode.getMenu())) {
                    mode.invalidate();
                    mActionModeView.initForMode(mode);
                    mActionMode = mode;

                    if (shouldAnimateActionModeView()) {
                        mActionModeView.setAlpha(0f);
                        mFadeAnim = ViewCompat.animate(mActionModeView).alpha(1f);
                        mFadeAnim.setListener(new ViewPropertyAnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(View view) {
                                mActionModeView.setVisibility(VISIBLE);
                                if (mActionModeView.getParent() instanceof View) {
                                    ViewCompat.requestApplyInsets((View) mActionModeView.getParent());
                                }
                            }

                            @Override
                            public void onAnimationEnd(View view) {
                                mActionModeView.setAlpha(1f);
                                mFadeAnim.setListener(null);
                                mFadeAnim = null;
                            }
                        });
                    } else {
                        mActionModeView.setAlpha(1f);
                        mActionModeView.setVisibility(VISIBLE);
                        if (mActionModeView.getParent() instanceof View) {
                            ViewCompat.requestApplyInsets((View) mActionModeView.getParent());
                        }
                    }

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

        // mActionMode changed.
        updateBackInvokedCallbackState();

        return mActionMode;
    }

    final boolean shouldAnimateActionModeView() {
        // We only to animate the action mode in if the sub decor has already been laid out.
        // If it hasn't been laid out, it hasn't been drawn to screen yet.
        return mSubDecorInstalled && mSubDecor != null && mSubDecor.isLaidOut();
    }

    @Override
    public void setHandleNativeActionModesEnabled(boolean enabled) {
        mHandleNativeActionModes = enabled;
    }

    @Override
    public boolean isHandleNativeActionModesEnabled() {
        return mHandleNativeActionModes;
    }

    void endOnGoingFadeAnimation() {
        if (mFadeAnim != null) {
            mFadeAnim.cancel();
        }
    }

    /**
     * Computes whether the delegate should intercept back invocations.
     * <p>
     * This method must be kept in sync with {@link #onBackPressed()}. If any of the properties
     * observed by this method change, we must call {@link #updateBackInvokedCallbackState()}.
     */
    boolean shouldRegisterBackInvokedCallback() {
        if (mDispatcher == null) {
            return false;
        }

        PanelFeatureState st = getPanelState(Window.FEATURE_OPTIONS_PANEL, false);
        if (st != null && st.isOpen) {
            return true;
        }

        if (mActionMode != null) {
            return true;
        }

        // Don't check canCollapseActionView() since the support
        // action bar manages its own back invocation callback.
        return false;
    }

    /**
     * Handles back press, returning {@code true} if the press was handled.
     * <p>
     * This method must be kept in sync with {@link #shouldRegisterBackInvokedCallback()}.
     */
    boolean onBackPressed() {
        final boolean wasLongPressBackDown = mLongPressBackDown;
        mLongPressBackDown = false;

        // Certain devices allow opening the options menu via a long press of the back button. We
        // should only close the open options menu if it wasn't opened via a long press gesture.
        PanelFeatureState st = getPanelState(Window.FEATURE_OPTIONS_PANEL, false);
        if (st != null && st.isOpen) {
            if (!wasLongPressBackDown) {
                closePanel(st, true);
            }
            return true;
        }

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

    boolean dispatchKeyEvent(KeyEvent event) {
        // Check AppCompatDialog directly since it isn't able to implement KeyEventDispatcher
        if (mHost instanceof KeyEventDispatcher.Component || mHost instanceof AppCompatDialog) {
            View root = mWindow.getDecorView();
            if (root != null && KeyEventDispatcher.dispatchBeforeHierarchy(root, event)) {
                return true;
            }
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            // If this is a MENU event, let the Activity have a go.
            if (mAppCompatWindowCallback.bypassDispatchKeyEvent(mWindow.getCallback(), event)) {
                return true;
            }
        }

        final int keyCode = event.getKeyCode();
        final int action = event.getAction();
        final boolean isDown = action == KeyEvent.ACTION_DOWN;

        return (isDown ? onKeyDown(keyCode, event) : onKeyUp(keyCode, event));
    }

    boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                onKeyUpPanel(Window.FEATURE_OPTIONS_PANEL, event);
                return true;
            case KeyEvent.KEYCODE_BACK:
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
                // button. We keep a record of whether the last event is from a long press. On SDK
                // 33 and above, back invocation handling may prevent us from receiving this event;
                // however, devices running SDK 33 are unlikely to support this feature anyway.
                mLongPressBackDown = (event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0;
                break;
        }
        return false;
    }

    @Override
    public View createView(View parent, final String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        if (mAppCompatViewInflater == null) {
            TypedArray a = mContext.obtainStyledAttributes(R.styleable.AppCompatTheme);
            String viewInflaterClassName =
                    a.getString(R.styleable.AppCompatTheme_viewInflaterClass);
            a.recycle();
            if (viewInflaterClassName == null) {
                // Set to null (the default in all AppCompat themes). Create the base inflater
                // (no reflection)
                mAppCompatViewInflater = new AppCompatViewInflater();
            } else {
                try {
                    Class<?> viewInflaterClass =
                            mContext.getClassLoader().loadClass(viewInflaterClassName);
                    mAppCompatViewInflater =
                            (AppCompatViewInflater) viewInflaterClass.getDeclaredConstructor()
                                    .newInstance();
                } catch (Throwable t) {
                    Log.i(TAG, "Failed to instantiate custom view inflater "
                            + viewInflaterClassName + ". Falling back to default.", t);
                    mAppCompatViewInflater = new AppCompatViewInflater();
                }
            }
        }

        boolean inheritContext = false;
        if (IS_PRE_LOLLIPOP) {
            if (mLayoutIncludeDetector == null) {
                mLayoutIncludeDetector = new LayoutIncludeDetector();
            }
            if (mLayoutIncludeDetector.detect(attrs)) {
                // The view being inflated is the root of an <include>d view, so make sure
                // we carry over any themed context.
                inheritContext = true;
            } else {
                inheritContext = (attrs instanceof XmlPullParser)
                        // If we have a XmlPullParser, we can detect where we are in the layout
                        ? ((XmlPullParser) attrs).getDepth() > 1
                        // Otherwise we have to use the old heuristic
                        : shouldInheritContext((ViewParent) parent);
            }
        }

        return mAppCompatViewInflater.createView(parent, name, context, attrs, inheritContext,
                IS_PRE_LOLLIPOP, /* Only read android:theme pre-L (L+ handles this anyway) */
                true, /* Read read app:theme as a fallback at all times for legacy reasons */
                VectorEnabledTintResources.shouldBeUsed() /* Only tint wrap the context if enabled */
        );
    }

    private boolean shouldInheritContext(ViewParent parent) {
        if (parent == null) {
            // The initial parent is null so just return false
            return false;
        }
        final View windowDecor = mWindow.getDecorView();
        while (true) {
            if (parent == null) {
                // Bingo. We've hit a view which has a null parent before being terminated from
                // the loop. This is (most probably) because it's the root view in an inflation
                // call, therefore we should inherit. This works as the inflated layout is only
                // added to the hierarchy at the end of the inflate() call.
                return true;
            } else if (parent == windowDecor || !(parent instanceof View)
                    || ((View) parent).isAttachedToWindow()) {
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
            LayoutInflaterCompat.setFactory2(layoutInflater, this);
        } else {
            if (!(layoutInflater.getFactory2() instanceof AppCompatDelegateImpl)) {
                Log.i(TAG, "The Activity's LayoutInflater already has a Factory installed"
                        + " so we can not install AppCompat's");
            }
        }
    }

    /**
     * From {@link LayoutInflater.Factory2}.
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public final View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return createView(parent, name, context, attrs);
    }

    /**
     * From {@link LayoutInflater.Factory2}.
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return onCreateView(null, name, context, attrs);
    }

    private @Nullable AppCompatActivity tryUnwrapContext() {
        Context context = mContext;
        while (context != null) {
            if (context instanceof AppCompatActivity) {
                return (AppCompatActivity) context;
            }
            if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else {
                return null;
            }
        }
        return null;
    }

    private void openPanel(final PanelFeatureState st, KeyEvent event) {
        // Already open, return
        if (st.isOpen || mDestroyed) {
            return;
        }

        // Don't open an options panel on xlarge devices.
        // (The app should be using an action bar for menu items.)
        if (st.featureId == FEATURE_OPTIONS_PANEL) {
            Configuration config = mContext.getResources().getConfiguration();
            boolean isXLarge = (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                    == Configuration.SCREENLAYOUT_SIZE_XLARGE;
            if (isXLarge) {
                return;
            }
        }

        Window.Callback cb = getWindowCallback();
        if ((cb != null) && !cb.onMenuOpened(st.featureId, st.menu)) {
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
                // If st.decorView was populated but we're not showing the menu for some reason,
                // make sure we try again rather than showing a potentially empty st.decorView.
                st.refreshDecorView = true;
                return;
            }

            ViewGroup.LayoutParams lp = st.shownPanelView.getLayoutParams();
            if (lp == null) {
                lp = new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            }

            int backgroundResId = st.background;
            st.decorView.setBackgroundResource(backgroundResId);

            ViewParent shownPanelParent = st.shownPanelView.getParent();
            if (shownPanelParent instanceof ViewGroup) {
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

        // st.isOpen for feature FEATURE_OPTIONS_PANEL changed.
        if (st.featureId == Window.FEATURE_OPTIONS_PANEL) {
            updateBackInvokedCallbackState();
        }
    }

    private boolean initializePanelDecor(PanelFeatureState st) {
        st.setStyle(getActionBarThemedContext());
        st.decorView = new ListMenuDecorView(st.listPresenterContext);
        st.gravity = Gravity.CENTER | Gravity.BOTTOM;
        return true;
    }

    private void reopenMenu(boolean toggleMenuMode) {
        if (mDecorContentParent != null && mDecorContentParent.canShowOverflowMenu()
                && (!ViewConfiguration.get(mContext).hasPermanentMenuKey()
                        || mDecorContentParent.isOverflowMenuShowPending())) {

            final Window.Callback cb = getWindowCallback();

            if (!mDecorContentParent.isOverflowMenuShowing() || !toggleMenuMode) {
                if (cb != null && !mDestroyed) {
                    // If we have a menu invalidation pending, do it now.
                    if (mInvalidatePanelMenuPosted &&
                            (mInvalidatePanelMenuFeatures & (1 << FEATURE_OPTIONS_PANEL)) != 0) {
                        mWindow.getDecorView().removeCallbacks(mInvalidatePanelMenuRunnable);
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
                if (!mDestroyed) {
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
        if (mDestroyed) {
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

    void checkCloseActionMenu(@NonNull MenuBuilder menu) {
        if (mClosingActionMenu) {
            return;
        }

        mClosingActionMenu = true;
        mDecorContentParent.dismissPopups();
        Window.Callback cb = getWindowCallback();
        if (cb != null && !mDestroyed) {
            cb.onPanelClosed(FEATURE_SUPPORT_ACTION_BAR, menu);
        }
        mClosingActionMenu = false;
    }

    void closePanel(int featureId) {
        closePanel(getPanelState(featureId, true), true);
    }

    void closePanel(PanelFeatureState st, boolean doCallback) {
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

        // st.isOpen for feature FEATURE_OPTIONS_PANEL changed.
        if (st.featureId == Window.FEATURE_OPTIONS_PANEL) {
            updateBackInvokedCallbackState();
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
                !ViewConfiguration.get(mContext).hasPermanentMenuKey()) {
            if (!mDecorContentParent.isOverflowMenuShowing()) {
                if (!mDestroyed && preparePanel(st, event)) {
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
            AudioManager audioManager = (AudioManager) mContext.getApplicationContext()
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
            } else {
                Log.w(TAG, "Couldn't get audio manager");
            }
        }
        return handled;
    }

    void callOnPanelClosed(int featureId, PanelFeatureState panel, Menu menu) {
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
        if ((panel != null) && !panel.isOpen) {
            return;
        }

        if (!mDestroyed) {
            // We need to be careful which callback we dispatch the call to. We can not dispatch
            // this to the Window's callback since that will call back into this method and cause a
            // crash. Instead we need to dispatch down to the original Activity/Dialog/etc.
            mAppCompatWindowCallback.bypassOnPanelClosed(mWindow.getCallback(), featureId, menu);
        }
    }

    PanelFeatureState findMenuPanel(Menu menu) {
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

    protected PanelFeatureState getPanelState(int featureId, boolean required) {
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

        if (!mInvalidatePanelMenuPosted) {
            ViewCompat.postOnAnimation(mWindow.getDecorView(), mInvalidatePanelMenuRunnable);
            mInvalidatePanelMenuPosted = true;
        }
    }

    void doInvalidatePanelMenu(int featureId) {
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
     * @param insets the current system window insets, or null if not available
     * @param rectInsets the current system window insets if {@code insets} is not available
     * @return the new top system window inset
     */
    final int updateStatusGuard(final @Nullable WindowInsetsCompat insets,
            final @Nullable Rect rectInsets) {
        int systemWindowInsetTop = 0;
        if (insets != null) {
            systemWindowInsetTop = insets.getSystemWindowInsetTop();
        } else if (rectInsets != null) {
            systemWindowInsetTop = rectInsets.top;
        }
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
                    final Rect innerInsets = mTempRect1;
                    final Rect rect = mTempRect2;
                    if (insets == null) {
                        innerInsets.set(rectInsets);
                    } else {
                        innerInsets.set(
                                insets.getSystemWindowInsetLeft(),
                                insets.getSystemWindowInsetTop(),
                                insets.getSystemWindowInsetRight(),
                                insets.getSystemWindowInsetBottom());
                    }

                    ViewUtils.computeFitSystemWindows(mSubDecor, innerInsets, rect);
                    int newTopMargin = innerInsets.top;
                    int newLeftMargin = innerInsets.left;
                    int newRightMargin = innerInsets.right;

                    // Must use root window insets for the guard, because the color views consume
                    // the navigation bar inset if the window does not request LAYOUT_HIDE_NAV - but
                    // the status guard is attached at the root.
                    WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(mSubDecor);
                    int newGuardLeftMargin =
                            rootInsets == null ? 0 : rootInsets.getSystemWindowInsetLeft();
                    int newGuardRightMargin =
                            rootInsets == null ? 0 : rootInsets.getSystemWindowInsetRight();

                    if (mlp.topMargin != newTopMargin || mlp.leftMargin != newLeftMargin
                            || mlp.rightMargin != newRightMargin) {
                        mlpChanged = true;
                        mlp.topMargin = newTopMargin;
                        mlp.leftMargin = newLeftMargin;
                        mlp.rightMargin = newRightMargin;
                    }

                    if (newTopMargin > 0 && mStatusGuard == null) {
                        mStatusGuard = new View(mContext);
                        mStatusGuard.setVisibility(GONE);
                        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                MATCH_PARENT, mlp.topMargin, Gravity.LEFT | Gravity.TOP);
                        lp.leftMargin = newGuardLeftMargin;
                        lp.rightMargin = newGuardRightMargin;
                        mSubDecor.addView(mStatusGuard, -1, lp);
                    } else if (mStatusGuard != null) {
                        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)
                                mStatusGuard.getLayoutParams();
                        if (lp.height != mlp.topMargin || lp.leftMargin != newGuardLeftMargin
                                || lp.rightMargin != newGuardRightMargin) {
                            lp.height = mlp.topMargin;
                            lp.leftMargin = newGuardLeftMargin;
                            lp.rightMargin = newGuardRightMargin;
                            mStatusGuard.setLayoutParams(lp);
                        }
                    }

                    // The action mode's theme may differ from the app, so
                    // always show the status guard above it.
                    showStatusGuard = mStatusGuard != null;

                    if (showStatusGuard && mStatusGuard.getVisibility() != VISIBLE) {
                        // If it wasn't previously shown, the color may be stale
                        updateStatusGuardColor(mStatusGuard);
                    }

                    // We only need to consume the insets if the action
                    // mode is overlaid on the app content (e.g. it's
                    // sitting in a FrameLayout, see
                    // screen_simple_overlay_action_mode.xml).
                    if (!mOverlayActionMode && showStatusGuard) {
                        systemWindowInsetTop = 0;
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
            mStatusGuard.setVisibility(showStatusGuard ? VISIBLE : GONE);
        }

        return systemWindowInsetTop;
    }

    private void updateStatusGuardColor(View v) {
        boolean lightStatusBar = (ViewCompat.getWindowSystemUiVisibility(v)
                & SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0;
        v.setBackgroundColor(lightStatusBar
                ? ContextCompat.getColor(mContext, R.color.abc_decor_view_status_guard_light)
                : ContextCompat.getColor(mContext, R.color.abc_decor_view_status_guard));
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

    void dismissPopups() {
        if (mDecorContentParent != null) {
            mDecorContentParent.dismissPopups();
        }

        if (mActionModePopup != null) {
            mWindow.getDecorView().removeCallbacks(mShowActionModePopup);
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

    @Override
    public Context getContextForDelegate() {
        return mContext;
    }

    @Override
    public boolean applyDayNight() {
        return applyApplicationSpecificConfig(true);
    }

    @Override
    boolean applyAppLocales() {
        // This method is only reached when there is an explicit call to setApplicationLocales().
        if (isAutoStorageOptedIn(mContext)
                && getRequestedAppLocales() != null
                && !getRequestedAppLocales().equals(getStoredAppLocales())) {
            // If the developer has opted in to autoStore the locales, we need to store the locales
            // for the application here. This is done using the syncRequestedAndStoredLocales,
            // called asynchronously on a worker thread.
            asyncExecuteSyncRequestedAndStoredLocales(mContext);
        }
        return applyApplicationSpecificConfig(true);
    }

    /**
     * Applies application configuration to the activity.
     */
    private boolean applyApplicationSpecificConfig(final boolean allowRecreation) {
        return applyApplicationSpecificConfig(allowRecreation,
                /* isLocalesApplicationRequired */ true);
    }

    /**
     * Applies application configuration to the activity.
     *
     * <p>By passing appropriate values to the input parameter {@param isLocalesApplicationRequired}
     * application of locales config on the current activity can be triggered or suppressed.</p>
     */
    @SuppressWarnings("deprecation")
    private boolean applyApplicationSpecificConfig(final boolean allowRecreation,
            final boolean isLocalesApplicationRequired) {
        if (mDestroyed) {
            if (DEBUG) {
                Log.d(TAG, "applyApplicationSpecificConfig. Skipping because host is "
                        + "destroyed");
            }
            // If we're destroyed, ignore the call
            return false;
        }

        @NightMode final int nightMode = calculateNightMode();
        @ApplyableNightMode final int modeToApply = mapNightMode(mContext, nightMode);

        LocaleListCompat localesToBeApplied = null;
        if (Build.VERSION.SDK_INT < 33) {
            localesToBeApplied = calculateApplicationLocales(mContext);
        }

        if (!isLocalesApplicationRequired && localesToBeApplied != null) {
            // Reaching here would mean that the requested locales has already been applied and
            // no modification is required. Hence, localesToBeApplied is kept same as the current
            // configuration locales.
            localesToBeApplied =
                    getConfigurationLocales(mContext.getResources()
                            .getConfiguration());
        }

        final boolean applied = updateAppConfiguration(modeToApply, localesToBeApplied,
                allowRecreation);

        if (nightMode == MODE_NIGHT_AUTO_TIME) {
            getAutoTimeNightModeManager(mContext).setup();
        } else if (mAutoTimeNightModeManager != null) {
            // Make sure we clean up the existing manager
            mAutoTimeNightModeManager.cleanup();
        }
        if (nightMode == MODE_NIGHT_AUTO_BATTERY) {
            getAutoBatteryNightModeManager(mContext).setup();
        } else if (mAutoBatteryNightModeManager != null) {
            // Make sure we clean up the existing manager
            mAutoBatteryNightModeManager.cleanup();
        }

        return applied;
    }

    /**
     * Returns the required {@link LocaleListCompat}  for the current application. This method
     * checks for requested app-specific locales and returns them after an overlay
     * with the system locales. If requested app-specific do not exist, it returns a null.
     */
    @Nullable LocaleListCompat calculateApplicationLocales(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return null;
        }
        LocaleListCompat requestedLocales = getRequestedAppLocales();
        if (requestedLocales == null) {
            return null;
        }
        LocaleListCompat systemLocales = getConfigurationLocales(
                context.getApplicationContext()
                        .getResources().getConfiguration());

        LocaleListCompat localesToBeApplied;
        if (Build.VERSION.SDK_INT >= 24) {
            // For API>=24 the application locales are applied as a localeList. The localeList
            // to be applied is an overlay of app-specific locales and the system locales.
            localesToBeApplied = combineLocalesIfOverlayExists(requestedLocales,
                    systemLocales);
        } else {
            // For API<24 the application does not have a localeList instead it has a single
            // locale, which we have set as the locale with the highest preference i.e. the first
            // one from the requested locales.
            if (requestedLocales.isEmpty()) {
                localesToBeApplied = LocaleListCompat.getEmptyLocaleList();
            } else if (Build.VERSION.SDK_INT >= 21) {
                localesToBeApplied =
                        LocaleListCompat.forLanguageTags(Api21Impl.toLanguageTag(
                                requestedLocales.get(0)));
            } else {
                // The method Locale.forLanguageTag() was introduced in API level 21,
                // using Locale.toString() method for APIs below that.
                localesToBeApplied =
                        LocaleListCompat.forLanguageTags(requestedLocales.get(0).toString());
            }
        }

        if (localesToBeApplied.isEmpty()) {
            // If the localesToBeApplied is empty, it implies that there are no app-specific locales
            // set for this application and systemLocales should be followed.
            localesToBeApplied = systemLocales;
        }
        return localesToBeApplied;
    }

    @Override
    public void setLocalNightMode(@NightMode int mode) {
        if (DEBUG) {
            Log.d(TAG, String.format("setLocalNightMode. New: %d, Current: %d",
                    mode, mLocalNightMode));
        }
        if (mLocalNightMode != mode) {
            mLocalNightMode = mode;
            if (mBaseContextAttached) {
                // If we the base context is attached, we call through to apply the new value.
                // Otherwise we just wait for attachBaseContext/onCreate
                applyDayNight();
            }
        }
    }

    @Override
    public int getLocalNightMode() {
        return mLocalNightMode;
    }

    @SuppressWarnings("deprecation")
    @ApplyableNightMode
    int mapNightMode(@NonNull Context context, @NightMode final int mode) {
        switch (mode) {
            case MODE_NIGHT_NO:
            case MODE_NIGHT_YES:
            case MODE_NIGHT_FOLLOW_SYSTEM:
                // $FALLTHROUGH since these are all valid modes to return
                return mode;
            case MODE_NIGHT_AUTO_TIME:
                if (Build.VERSION.SDK_INT >= 23) {
                    UiModeManager uiModeManager = (UiModeManager) context.getApplicationContext()
                            .getSystemService(Context.UI_MODE_SERVICE);
                    if (uiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_AUTO) {
                        // If we're set to AUTO and the system's auto night mode is already enabled,
                        // we'll just let the system handle it by returning FOLLOW_SYSTEM
                        return MODE_NIGHT_FOLLOW_SYSTEM;
                    }
                }
                return getAutoTimeNightModeManager(context).getApplyableNightMode();
            case MODE_NIGHT_AUTO_BATTERY:
                return getAutoBatteryNightModeManager(context).getApplyableNightMode();
            case MODE_NIGHT_UNSPECIFIED:
                // If we don't have a mode specified, let the system handle it
                return MODE_NIGHT_FOLLOW_SYSTEM;
            default:
                throw new IllegalStateException("Unknown value set for night mode. Please use one"
                        + " of the MODE_NIGHT values from AppCompatDelegate.");
        }
    }

    @NightMode
    private int calculateNightMode() {
        return mLocalNightMode != MODE_NIGHT_UNSPECIFIED ? mLocalNightMode : getDefaultNightMode();
    }

    void setConfigurationLocales(Configuration conf, @NonNull LocaleListCompat locales) {
        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.setLocales(conf, locales);
        } else {
            conf.setLocale(locales.get(0));
            conf.setLayoutDirection(locales.get(0));
        }
    }

    LocaleListCompat getConfigurationLocales(Configuration conf) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.getLocales(conf);
        } else if (Build.VERSION.SDK_INT >= 21) {
            return LocaleListCompat.forLanguageTags(Api21Impl.toLanguageTag(conf.locale));
        } else {
            return LocaleListCompat.create(conf.locale);
        }
    }

    void setDefaultLocalesForLocaleList(LocaleListCompat locales) {
        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.setDefaultLocales(locales);
        } else {
            Locale.setDefault(locales.get(0));
        }
    }

    private @NonNull Configuration createOverrideAppConfiguration(@NonNull Context context,
            @ApplyableNightMode int mode, @Nullable LocaleListCompat locales,
            @Nullable Configuration configOverlay, boolean ignoreFollowSystem) {
        int newNightMode;
        switch (mode) {
            case MODE_NIGHT_YES:
                newNightMode = Configuration.UI_MODE_NIGHT_YES;
                break;
            case MODE_NIGHT_NO:
                newNightMode = Configuration.UI_MODE_NIGHT_NO;
                break;
            default:
            case MODE_NIGHT_FOLLOW_SYSTEM:
                if (ignoreFollowSystem) {
                    // We're generating an overlay to be used on top of the system configuration,
                    // so use whatever's already there.
                    newNightMode = Configuration.UI_MODE_NIGHT_UNDEFINED;
                } else {
                    // If we're following the system, we just use the system default from the
                    // application context
                    final Configuration appConfig =
                            context.getApplicationContext().getResources().getConfiguration();
                    newNightMode = appConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
                }
                break;
        }

        // If we're here then we can try and apply an override configuration on the Context.
        final Configuration overrideConf = new Configuration();
        overrideConf.fontScale = 0;
        if (configOverlay != null) {
            overrideConf.setTo(configOverlay);
        }
        overrideConf.uiMode = newNightMode
                | (overrideConf.uiMode & ~Configuration.UI_MODE_NIGHT_MASK);

        if (locales != null) {
            setConfigurationLocales(overrideConf, locales);
        }
        return overrideConf;
    }

    /**
     * Updates the {@link Resources} configuration {@code uiMode}  and {@Link LocaleList} with the
     * chosen configuration values.
     *
     * @param nightMode The new night mode to apply
     * @param locales The new Locales to be applied
     * @param allowRecreation whether to attempt activity recreate
     * @return true if an action has been taken (recreation, resources updating, etc)
     */
    private boolean updateAppConfiguration(int nightMode, @Nullable LocaleListCompat
            locales, final boolean allowRecreation) {
        boolean handled = false;

        final Configuration overrideConfig =
                createOverrideAppConfiguration(mContext, nightMode, locales, null, false);

        final int activityHandlingConfigChange = getActivityHandlesConfigChangesFlags(mContext);
        final Configuration currentConfiguration = mEffectiveConfiguration == null
                ? mContext.getResources().getConfiguration() : mEffectiveConfiguration;
        final int currentNightMode = currentConfiguration.uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        final int newNightMode = overrideConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        final LocaleListCompat currentLocales = getConfigurationLocales(currentConfiguration);
        final LocaleListCompat newLocales;
        if (locales == null) {
            newLocales = null;
        } else {
            newLocales = getConfigurationLocales(overrideConfig);
        }

        // Bitmask representing if there is a change in nightMode or Locales, mapped by bits
        // ActivityInfo.CONFIG_UI_MODE and ActivityInfo.CONFIG_LOCALE respectively.
        int configChanges = 0;
        if (currentNightMode != newNightMode) {
            configChanges |= ActivityInfo.CONFIG_UI_MODE;
        }
        if (newLocales != null && !currentLocales.equals(newLocales)) {
            configChanges |= ActivityInfo.CONFIG_LOCALE;
            configChanges |= ActivityInfo.CONFIG_LAYOUT_DIRECTION;
        }

        if (DEBUG) {
            Log.d(TAG, String.format(
                    "updateAppConfiguration [allowRecreation:%s, "
                            + "currentNightMode:%s, newNightMode:%s, currentLocales:%s, "
                            + "newLocales:%s, activityHandlingNightModeChanges:%s, "
                            + "activityHandlingLocalesChanges:%s, "
                            + "activityHandlingLayoutDirectionChanges:%s, "
                            + "baseContextAttached:%s, "
                            + "created:%s, canReturnDifferentContext:%s, host:%s]",
                    allowRecreation, currentNightMode, newNightMode,
                    currentLocales,
                    newLocales,
                    ((activityHandlingConfigChange & ActivityInfo.CONFIG_UI_MODE) != 0),
                    ((activityHandlingConfigChange & ActivityInfo.CONFIG_LOCALE) != 0),
                    ((activityHandlingConfigChange & ActivityInfo.CONFIG_LAYOUT_DIRECTION) != 0),
                    mBaseContextAttached, mCreated,
                    sCanReturnDifferentContext, mHost));
        }
        if ((~activityHandlingConfigChange & configChanges) != 0
                && allowRecreation
                && mBaseContextAttached
                && (sCanReturnDifferentContext || mCreated)
                && mHost instanceof Activity
                && !((Activity) mHost).isChild()) {
            // If we're an attached, standalone Activity, we can recreate() to apply using the
            // attachBaseContext() + createConfigurationContext() code path.
            // Else, we need to use updateConfiguration() before we're 'created' (below)
            if (DEBUG) {
                Log.d(TAG, "updateAppConfiguration attempting to recreate Activity: "
                        + mHost);
            }

            // To workaround the android framework issue(b/242026447) which doesn't update the
            // layout direction after recreating in Android S.
            if (Build.VERSION.SDK_INT >= 31
                    && (configChanges & ActivityInfo.CONFIG_LAYOUT_DIRECTION) != 0) {
                View view = ((Activity) mHost).getWindow().getDecorView();
                view.setLayoutDirection(overrideConfig.getLayoutDirection());
            }
            ActivityCompat.recreate((Activity) mHost);
            handled = true;
        } else if (DEBUG) {
            Log.d(TAG, "updateAppConfiguration not recreating Activity: " + mHost);
        }

        if (!handled && (configChanges != 0)) {
            // Else we need to use the updateConfiguration path
            if (DEBUG) {
                Log.d(TAG, "updateAppConfiguration. Updating resources config on host: "
                        + mHost);
            }
            // If all the configurations that need to be altered are handled by the activity,
            // only then callOnConfigChange is set to true.
            updateResourcesConfiguration(newNightMode, newLocales,
                    /* callOnConfigChange = */(configChanges & activityHandlingConfigChange)
                            == configChanges, null);

            handled = true;
        }

        if (DEBUG && !handled) {
            Log.d(TAG,
                    "updateAppConfiguration. Skipping. nightMode: " + nightMode + " and "
                            + "locales: " +  locales + " for host:" + mHost);
        }

        if (handled && mHost instanceof AppCompatActivity) {
            if ((configChanges & ActivityInfo.CONFIG_UI_MODE) != 0) {
                ((AppCompatActivity) mHost).onNightModeChanged(nightMode);
            }
            if ((configChanges & ActivityInfo.CONFIG_LOCALE) != 0) {
                ((AppCompatActivity) mHost).onLocalesChanged(locales);
            }
        }

        if (newLocales != null) {
            // LocaleListCompat's default locales are updated here using the configuration
            // locales to keep default locales in sync with application locales and also to cover
            // the case where framework re-adjusts input locales by bringing forward the most
            // suitable locale.
            setDefaultLocalesForLocaleList(getConfigurationLocales(
                    mContext.getResources().getConfiguration()));
        }
        return handled;
    }

    private void updateResourcesConfiguration(int uiModeNightModeValue,
            final @Nullable LocaleListCompat locales, final boolean callOnConfigChange,
            @Nullable Configuration configOverlay) {
        // If the Activity is not set to handle config changes we will
        // update the Resources with a new Configuration with  updated nightMode and locales.
        final Resources res = mContext.getResources();
        final Configuration conf = new Configuration(res.getConfiguration());
        if (configOverlay != null) {
            conf.updateFrom(configOverlay);
        }
        conf.uiMode = uiModeNightModeValue
                | (res.getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        if (locales != null) {
            setConfigurationLocales(conf, locales);
        }
        res.updateConfiguration(conf, null);

        // We may need to flush the Resources' drawable cache due to framework bugs.
        if (Build.VERSION.SDK_INT < 26) {
            ResourcesFlusher.flush(res);
        }

        if (mThemeResId != 0) {
            // We need to re-apply the theme so that it reflected the new
            // configuration
            mContext.setTheme(mThemeResId);

            if (Build.VERSION.SDK_INT >= 23) {
                // On M+ setTheme only applies if the themeResId actually changes,
                // since we have no way to publicly check what the Theme's current
                // themeResId is, we just manually apply it anyway. Most of the time
                // this is what we need anyway (since the themeResId does not
                // often change)
                mContext.getTheme().applyStyle(mThemeResId, true);
            }
        }

        if (callOnConfigChange && mHost instanceof Activity) {
            updateActivityConfiguration(conf);
        }
    }

    private void updateActivityConfiguration(Configuration conf) {
        final Activity activity = (Activity) mHost;
        if (activity instanceof LifecycleOwner) {
            // If the Activity is a LifecyleOwner, check that it is after onCreate() and
            // before onDestroy(), which includes STOPPED.
            Lifecycle lifecycle = ((LifecycleOwner) activity).getLifecycle();
            if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
                activity.onConfigurationChanged(conf);
            }
        } else {
            // Otherwise, we'll fallback to our internal created and destroyed flags.
            if (mCreated && !mDestroyed) {
                activity.onConfigurationChanged(conf);
            }
        }
    }

    /**
     */
    @RestrictTo(LIBRARY)
    @VisibleForTesting
    final @NonNull AutoNightModeManager getAutoTimeNightModeManager() {
        return getAutoTimeNightModeManager(mContext);
    }

    private AutoNightModeManager getAutoTimeNightModeManager(@NonNull Context context) {
        if (mAutoTimeNightModeManager == null) {
            mAutoTimeNightModeManager = new AutoTimeNightModeManager(
                    TwilightManager.getInstance(context));
        }
        return mAutoTimeNightModeManager;
    }

    private AutoNightModeManager getAutoBatteryNightModeManager(@NonNull Context context) {
        if (mAutoBatteryNightModeManager == null) {
            mAutoBatteryNightModeManager = new AutoBatteryNightModeManager(context);
        }
        return mAutoBatteryNightModeManager;
    }

    private int getActivityHandlesConfigChangesFlags(Context baseContext) {
        if (!mActivityHandlesConfigFlagsChecked
                && mHost instanceof Activity) {
            final PackageManager pm = baseContext.getPackageManager();
            if (pm == null) {
                // If we don't have a PackageManager, return 0. Don't set
                // the checked flag though so we still check again later
                return 0;
            }
            try {
                int flags = 0;
                // On newer versions of the OS we need to pass direct boot
                // flags so that getActivityInfo doesn't crash under strict
                // mode checks
                if (Build.VERSION.SDK_INT >= 29) {
                    flags = PackageManager.MATCH_DIRECT_BOOT_AUTO
                            | PackageManager.MATCH_DIRECT_BOOT_AWARE
                            | PackageManager.MATCH_DIRECT_BOOT_UNAWARE;
                } else if (Build.VERSION.SDK_INT >= 24) {
                    flags = PackageManager.MATCH_DIRECT_BOOT_AWARE
                            | PackageManager.MATCH_DIRECT_BOOT_UNAWARE;
                }
                final ActivityInfo info = pm.getActivityInfo(
                        new ComponentName(baseContext, mHost.getClass()), flags);
                if (info != null) {
                    mActivityHandlesConfigFlags = info.configChanges;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // This shouldn't happen but let's not crash because of it, we'll just log and
                // return false (since most apps won't be handling it)
                Log.d(TAG, "Exception while getting ActivityInfo", e);
                mActivityHandlesConfigFlags = 0;
            }
        }
        // Flip the checked flag so we don't check again
        mActivityHandlesConfigFlagsChecked = true;
        return mActivityHandlesConfigFlags;
    }

    /**
     * Clears out internal reference when the action mode is destroyed.
     */
    class ActionModeCallbackWrapperV9 implements ActionMode.Callback {
        private ActionMode.Callback mWrapped;

        public ActionModeCallbackWrapperV9(ActionMode.Callback wrapped) {
            mWrapped = wrapped;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ViewCompat.requestApplyInsets(mSubDecor);
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        @Override
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
                        mActionModeView.setVisibility(GONE);
                        if (mActionModePopup != null) {
                            mActionModePopup.dismiss();
                        } else if (mActionModeView.getParent() instanceof View) {
                            ViewCompat.requestApplyInsets((View) mActionModeView.getParent());
                        }
                        mActionModeView.killMode();
                        mFadeAnim.setListener(null);
                        mFadeAnim = null;
                        ViewCompat.requestApplyInsets(mSubDecor);
                    }
                });
            }
            if (mAppCompatCallback != null) {
                mAppCompatCallback.onSupportActionModeFinished(mActionMode);
            }
            mActionMode = null;
            ViewCompat.requestApplyInsets(mSubDecor);

            // mActionMode changed.
            updateBackInvokedCallbackState();
        }
    }

    private final class PanelMenuPresenterCallback implements MenuPresenter.Callback {
        PanelMenuPresenterCallback() {
        }

        @Override
        public void onCloseMenu(@NonNull MenuBuilder menu, boolean allMenusAreClosing) {
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
        public boolean onOpenSubMenu(@NonNull MenuBuilder subMenu) {
            // Only dispatch for the root menu
            if (subMenu == subMenu.getRootMenu() && mHasActionBar) {
                Window.Callback cb = getWindowCallback();
                if (cb != null && !mDestroyed) {
                    cb.onMenuOpened(FEATURE_SUPPORT_ACTION_BAR, subMenu);
                }
            }
            return true;
        }
    }

    private final class ActionMenuPresenterCallback implements MenuPresenter.Callback {
        ActionMenuPresenterCallback() {
        }

        @Override
        public boolean onOpenSubMenu(@NonNull MenuBuilder subMenu) {
            Window.Callback cb = getWindowCallback();
            if (cb != null) {
                cb.onMenuOpened(FEATURE_SUPPORT_ACTION_BAR, subMenu);
            }
            return true;
        }

        @Override
        public void onCloseMenu(@NonNull MenuBuilder menu, boolean allMenusAreClosing) {
            checkCloseActionMenu(menu);
        }
    }

    protected static final class PanelFeatureState {

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

            TypedArray a = context.obtainStyledAttributes(R.styleable.AppCompatTheme);
            background = a.getResourceId(
                    R.styleable.AppCompatTheme_panelBackground, 0);
            windowAnimations = a.getResourceId(
                    R.styleable.AppCompatTheme_android_windowAnimationStyle, 0);
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
        @SuppressLint("BanParcelableUsage")
        private static class SavedState implements Parcelable {
            int featureId;
            boolean isOpen;
            Bundle menuState;

            SavedState() {
            }

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flags) {
                dest.writeInt(featureId);
                dest.writeInt(isOpen ? 1 : 0);

                if (isOpen) {
                    dest.writeBundle(menuState);
                }
            }

            static SavedState readFromParcel(Parcel source, ClassLoader loader) {
                SavedState savedState = new SavedState();
                savedState.featureId = source.readInt();
                savedState.isOpen = source.readInt() == 1;

                if (savedState.isOpen) {
                    savedState.menuState = source.readBundle(loader);
                }

                return savedState;
            }

            public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
                @Override
                public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                    return readFromParcel(in, loader);
                }

                @Override
                public SavedState createFromParcel(Parcel in) {
                    return readFromParcel(in, null);
                }

                @Override
                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
        }
    }

    private class ListMenuDecorView extends ContentFrameLayout {
        public ListMenuDecorView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return AppCompatDelegateImpl.this.dispatchKeyEvent(event)
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
            setBackgroundDrawable(AppCompatResources.getDrawable(getContext(), resid));
        }

        private boolean isOutOfBounds(int x, int y) {
            return x < -5 || y < -5 || x > (getWidth() + 5) || y > (getHeight() + 5);
        }
    }

    /**
     * Interface which allows ToolbarActionBar to receive menu events without
     * needing to wrap the Window.Callback
     */
    interface ActionBarMenuCallback {
        boolean onPreparePanel(int featureId);

        @Nullable View onCreatePanelView(int featureId);
    }

    class AppCompatWindowCallback extends WindowCallbackWrapper {
        private ActionBarMenuCallback mActionBarCallback;
        private boolean mOnContentChangedBypassEnabled;
        private boolean mDispatchKeyEventBypassEnabled;
        private boolean mOnPanelClosedBypassEnabled;

        AppCompatWindowCallback(Window.Callback callback) {
            super(callback);
        }

        void setActionBarCallback(@Nullable ActionBarMenuCallback callback) {
            mActionBarCallback = callback;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (mDispatchKeyEventBypassEnabled) {
                return getWrapped().dispatchKeyEvent(event);
            }

            return AppCompatDelegateImpl.this.dispatchKeyEvent(event)
                    || super.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return super.dispatchKeyShortcutEvent(event)
                    || AppCompatDelegateImpl.this.onKeyShortcut(event.getKeyCode(), event);
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            if (featureId == Window.FEATURE_OPTIONS_PANEL && !(menu instanceof MenuBuilder)) {
                // If this is an options menu but it's not an AppCompat menu, we eat the event
                // and return false
                return false;
            }
            return super.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public View onCreatePanelView(int featureId) {
            if (mActionBarCallback != null) {
                View created = mActionBarCallback.onCreatePanelView(featureId);
                if (created != null) {
                    return created;
                }
            }
            return super.onCreatePanelView(featureId);
        }

        @Override
        public void onContentChanged() {
            if (mOnContentChangedBypassEnabled) {
                getWrapped().onContentChanged();
                return;
            }

            // We purposely do not propagate this call as this is called when we install
            // our sub-decor rather than the user's content
        }

        @Override
        public boolean onPreparePanel(int featureId, View view, Menu menu) {
            final MenuBuilder mb = menu instanceof MenuBuilder ? (MenuBuilder) menu : null;

            if (featureId == Window.FEATURE_OPTIONS_PANEL && mb == null) {
                // If this is an options menu but it's not an AppCompat menu, we eat the event
                // and return false
                return false;
            }

            // On ICS and below devices, onPreparePanel calls menu.hasVisibleItems() to determine
            // if a panel is prepared. This interferes with any initially invisible items, which
            // are later made visible. We workaround it by making hasVisibleItems() always
            // return true during the onPreparePanel call.
            if (mb != null) {
                mb.setOverrideVisibleItems(true);
            }

            boolean handled = false;
            if (mActionBarCallback != null && mActionBarCallback.onPreparePanel(featureId)) {
                handled = true;
            }
            if (!handled) {
                handled = super.onPreparePanel(featureId, view, menu);
            }

            if (mb != null) {
                mb.setOverrideVisibleItems(false);
            }

            return handled;
        }

        @Override
        public boolean onMenuOpened(int featureId, Menu menu) {
            super.onMenuOpened(featureId, menu);
            AppCompatDelegateImpl.this.onMenuOpened(featureId);
            return true;
        }

        @Override
        public void onPanelClosed(int featureId, Menu menu) {
            if (mOnPanelClosedBypassEnabled) {
                getWrapped().onPanelClosed(featureId, menu);
                return;
            }

            super.onPanelClosed(featureId, menu);
            AppCompatDelegateImpl.this.onPanelClosed(featureId);
        }

        @Override
        public android.view.ActionMode onWindowStartingActionMode(
                android.view.ActionMode.Callback callback) {
            if (Build.VERSION.SDK_INT >= 23) {
                // No-op on API 23+
                return null;
            }
            // We wrap in a support action mode on v14+ if enabled
            if (isHandleNativeActionModesEnabled()) {
                return startAsSupportActionMode(callback);
            }
            // Else, let the call fall through to the wrapped callback
            return super.onWindowStartingActionMode(callback);
        }

        /**
         * Wrap the framework {@link android.view.ActionMode.Callback} in a support action mode and
         * let AppCompat display it.
         */
        final android.view.ActionMode startAsSupportActionMode(
                android.view.ActionMode.Callback callback) {
            // Wrap the callback as a v7 ActionMode.Callback
            final SupportActionModeWrapper.CallbackWrapper callbackWrapper =
                    new SupportActionModeWrapper.CallbackWrapper(mContext, callback);

            // Try and start a support action mode using the wrapped callback
            final androidx.appcompat.view.ActionMode supportActionMode =
                    startSupportActionMode(callbackWrapper);

            if (supportActionMode != null) {
                // If we received a support action mode, wrap and return it
                return callbackWrapper.getActionModeWrapper(supportActionMode);
            }
            return null;
        }

        @Override
        @RequiresApi(23)
        public android.view.ActionMode onWindowStartingActionMode(
            android.view.ActionMode.Callback callback, int type) {
            if (isHandleNativeActionModesEnabled()) {
                switch (type) {
                    case android.view.ActionMode.TYPE_PRIMARY:
                        // We only take over if the type is TYPE_PRIMARY
                        return startAsSupportActionMode(callback);
                }
            }
            // Else, let the call fall through to the wrapped callback
            return super.onWindowStartingActionMode(callback, type);
        }

        @Override
        @RequiresApi(24)
        public void onProvideKeyboardShortcuts(
            List<KeyboardShortcutGroup> data, Menu menu, int deviceId) {
            final PanelFeatureState panel = getPanelState(Window.FEATURE_OPTIONS_PANEL, true);
            if (panel != null && panel.menu != null) {
                // The menu provided is one created by PhoneWindow which we don't actually use.
                // Instead we'll pass through our own...
                super.onProvideKeyboardShortcuts(data, panel.menu, deviceId);
            } else {
                // If we don't have a menu, jump pass through the original instead
                super.onProvideKeyboardShortcuts(data, menu, deviceId);
            }
        }

        /**
         * Performs a call to {@link Window.Callback#onContentChanged()}, ensuring that if the
         * delegate's own {@link #onContentChanged()} is called then it delegates directly to the
         * wrapped callback.
         *
         * @param c the callback
         */
        public void bypassOnContentChanged(Window.Callback c) {
            try {
                mOnContentChangedBypassEnabled = true;
                c.onContentChanged();
            } finally {
                mOnContentChangedBypassEnabled = false;
            }
        }

        /**
         * Performs a call to {@link Window.Callback#dispatchKeyEvent(KeyEvent)}, ensuring that if
         * the delegate's own {@link #dispatchKeyEvent(KeyEvent)} is called then it delegates
         * directly to the wrapped callback.
         *
         * @param c the callback
         * @param e the key event to dispatch
         * @return whether the key event was handled
         */
        public boolean bypassDispatchKeyEvent(Window.Callback c, KeyEvent e) {
            try {
                mDispatchKeyEventBypassEnabled = true;
                return c.dispatchKeyEvent(e);
            } finally {
                mDispatchKeyEventBypassEnabled = false;
            }
        }

        /**
         * Performs a call to {@link Window.Callback#onPanelClosed(int, Menu)}, ensuring that if the
         * delegate's own {@link #onPanelClosed(int, Menu)} is called then it delegates directly to
         * the wrapped callback.
         *
         * @param c the callback
         * @param featureId the feature ID for the panel
         * @param menu the menu represented by the panel
         */
        public void bypassOnPanelClosed(Window.Callback c, int featureId, Menu menu) {
            try {
                mOnPanelClosedBypassEnabled = true;
                c.onPanelClosed(featureId, menu);
            } finally {
                mOnPanelClosedBypassEnabled = false;
            }
        }
    }

    /**
     */
    @VisibleForTesting
    @RestrictTo(LIBRARY)
    abstract class AutoNightModeManager {
        private BroadcastReceiver mReceiver;

        @ApplyableNightMode
        abstract int getApplyableNightMode();

        abstract void onChange();

        void setup() {
            cleanup();

            final IntentFilter filter = createIntentFilterForBroadcastReceiver();
            if (filter == null || filter.countActions() == 0) {
                // Null or empty IntentFilter, skip
                return;
            }

            if (mReceiver == null) {
                mReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        onChange();
                    }
                };
            }
            mContext.registerReceiver(mReceiver, filter);
        }

        abstract @Nullable IntentFilter createIntentFilterForBroadcastReceiver();

        void cleanup() {
            if (mReceiver != null) {
                try {
                    mContext.unregisterReceiver(mReceiver);
                } catch (IllegalArgumentException e) {
                    // If the receiver has already been unregistered, unregisterReceiver() will
                    // throw an exception. Just ignore and carry-on...
                }
                mReceiver = null;
            }
        }

        boolean isListening() {
            return mReceiver != null;
        }
    }

    private class AutoTimeNightModeManager extends AutoNightModeManager {
        private final TwilightManager mTwilightManager;

        AutoTimeNightModeManager(@NonNull TwilightManager twilightManager) {
            mTwilightManager = twilightManager;
        }

        @ApplyableNightMode
        @Override
        public int getApplyableNightMode() {
            return mTwilightManager.isNight() ? MODE_NIGHT_YES : MODE_NIGHT_NO;
        }

        @Override
        public void onChange() {
            applyDayNight();
        }

        @Override
        IntentFilter createIntentFilterForBroadcastReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_TIME_TICK);
            return filter;
        }
    }

    private class AutoBatteryNightModeManager extends AutoNightModeManager {
        private final PowerManager mPowerManager;

        AutoBatteryNightModeManager(@NonNull Context context) {
            mPowerManager = (PowerManager) context.getApplicationContext()
                    .getSystemService(Context.POWER_SERVICE);
        }

        @ApplyableNightMode
        @Override
        public int getApplyableNightMode() {
            if (Build.VERSION.SDK_INT >= 21) {
                return Api21Impl.isPowerSaveMode(mPowerManager) ? MODE_NIGHT_YES : MODE_NIGHT_NO;
            }
            return MODE_NIGHT_NO;
        }

        @Override
        public void onChange() {
            applyDayNight();
        }

        @Override
        IntentFilter createIntentFilterForBroadcastReceiver() {
            if (Build.VERSION.SDK_INT >= 21) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
                return filter;
            }
            return null;
        }
    }

    @Override
    public final ActionBarDrawerToggle.Delegate getDrawerToggleDelegate() {
        return new ActionBarDrawableToggleImpl();
    }

    private class ActionBarDrawableToggleImpl implements ActionBarDrawerToggle.Delegate {
        ActionBarDrawableToggleImpl() {
        }

        @Override
        public Drawable getThemeUpIndicator() {
            final TintTypedArray a = TintTypedArray.obtainStyledAttributes(
                    getActionBarThemedContext(), null, new int[]{ R.attr.homeAsUpIndicator });
            final Drawable result = a.getDrawable(0);
            a.recycle();
            return result;
        }

        @Override
        public Context getActionBarThemedContext() {
            return AppCompatDelegateImpl.this.getActionBarThemedContext();
        }

        @Override
        public boolean isNavigationVisible() {
            final ActionBar ab = getSupportActionBar();
            return ab != null && (ab.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0;
        }

        @Override
        public void setActionBarUpIndicator(Drawable upDrawable, int contentDescRes) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setHomeAsUpIndicator(upDrawable);
                ab.setHomeActionContentDescription(contentDescRes);
            }
        }

        @Override
        public void setActionBarDescription(int contentDescRes) {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setHomeActionContentDescription(contentDescRes);
            }
        }
    }

    /**
     * Copied from the platform's private method in Configuration. This is <strong>not</strong>
     * suitable for general use, as it cannot handle some properties including any added after
     * API 29. See comments inside method for specific properties that could not be handled.
     * <p>
     * Generate a delta Configuration between <code>base</code> and <code>change</code>. The
     * resulting delta can be used with {@link Configuration#updateFrom(Configuration)}.
     * <p>
     * Caveat: If the any of the Configuration's members becomes undefined, then
     * {@link Configuration#updateFrom(Configuration)} will treat it as a no-op and not update that
     * member.
     * <p>
     * This is fine for device configurations as no member is ever undefined.
     */
    private static @NonNull Configuration generateConfigDelta(@NonNull Configuration base,
            @Nullable Configuration change) {
        final Configuration delta = new Configuration();
        delta.fontScale = 0;

        if (change == null || base.diff(change) == 0) {
            return delta;
        }

        if (base.fontScale != change.fontScale) {
            delta.fontScale = change.fontScale;
        }

        if (base.mcc != change.mcc) {
            delta.mcc = change.mcc;
        }

        if (base.mnc != change.mnc) {
            delta.mnc = change.mnc;
        }

        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.generateConfigDelta_locale(base, change, delta);
        } else {
            if (!ObjectsCompat.equals(base.locale, change.locale)) {
                delta.locale = change.locale;
            }
        }

        if (base.touchscreen != change.touchscreen) {
            delta.touchscreen = change.touchscreen;
        }

        if (base.keyboard != change.keyboard) {
            delta.keyboard = change.keyboard;
        }

        if (base.keyboardHidden != change.keyboardHidden) {
            delta.keyboardHidden = change.keyboardHidden;
        }

        if (base.navigation != change.navigation) {
            delta.navigation = change.navigation;
        }

        if (base.navigationHidden != change.navigationHidden) {
            delta.navigationHidden = change.navigationHidden;
        }

        if (base.orientation != change.orientation) {
            delta.orientation = change.orientation;
        }

        if ((base.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                != (change.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)) {
            delta.screenLayout |= change.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        }

        if ((base.screenLayout & Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)
                != (change.screenLayout & Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)) {
            delta.screenLayout |= change.screenLayout & Configuration.SCREENLAYOUT_LAYOUTDIR_MASK;
        }

        if ((base.screenLayout & Configuration.SCREENLAYOUT_LONG_MASK)
                != (change.screenLayout & Configuration.SCREENLAYOUT_LONG_MASK)) {
            delta.screenLayout |= change.screenLayout & Configuration.SCREENLAYOUT_LONG_MASK;
        }

        if ((base.screenLayout & Configuration.SCREENLAYOUT_ROUND_MASK)
                != (change.screenLayout & Configuration.SCREENLAYOUT_ROUND_MASK)) {
            delta.screenLayout |= change.screenLayout & Configuration.SCREENLAYOUT_ROUND_MASK;
        }

        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.generateConfigDelta_colorMode(base, change, delta);
        }

        if ((base.uiMode & Configuration.UI_MODE_TYPE_MASK)
                != (change.uiMode & Configuration.UI_MODE_TYPE_MASK)) {
            delta.uiMode |= change.uiMode & Configuration.UI_MODE_TYPE_MASK;
        }

        if ((base.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != (change.uiMode & Configuration.UI_MODE_NIGHT_MASK)) {
            delta.uiMode |= change.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        }

        if (base.screenWidthDp != change.screenWidthDp) {
            delta.screenWidthDp = change.screenWidthDp;
        }

        if (base.screenHeightDp != change.screenHeightDp) {
            delta.screenHeightDp = change.screenHeightDp;
        }

        if (base.smallestScreenWidthDp != change.smallestScreenWidthDp) {
            delta.smallestScreenWidthDp = change.smallestScreenWidthDp;
        }

        if (base.densityDpi != change.densityDpi) {
            delta.densityDpi = change.densityDpi;
        }

        // Assets sequence and window configuration are not supported.

        return delta;
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() { }

        static boolean isPowerSaveMode(PowerManager powerManager) {
            return powerManager.isPowerSaveMode();
        }

        static String toLanguageTag(Locale locale) {
            return locale.toLanguageTag();
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() { }

        // Most methods of LocaleListCompat requires a minimum API of 24 to be used and these are
        // the helper implementations of those methods, used to indirectly invoke them in our code.
        static void generateConfigDelta_locale(@NonNull Configuration base,
                @NonNull Configuration change, @NonNull Configuration delta) {
            final LocaleList baseLocales = base.getLocales();
            final LocaleList changeLocales = change.getLocales();
            if (!baseLocales.equals(changeLocales)) {
                delta.setLocales(changeLocales);
                delta.locale = change.locale;
            }
        }

        static LocaleListCompat getLocales(Configuration configuration) {
            return LocaleListCompat.forLanguageTags(configuration.getLocales().toLanguageTags());
        }

        static void setLocales(Configuration configuration, LocaleListCompat locales) {
            configuration.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()));
        }

        public static void setDefaultLocales(LocaleListCompat locales) {
            LocaleList.setDefault(LocaleList.forLanguageTags(locales.toLanguageTags()));
        }
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() { }

        static void generateConfigDelta_colorMode(@NonNull Configuration base,
                @NonNull Configuration change, @NonNull Configuration delta) {
            if ((base.colorMode & Configuration.COLOR_MODE_WIDE_COLOR_GAMUT_MASK)
                    != (change.colorMode & Configuration.COLOR_MODE_WIDE_COLOR_GAMUT_MASK)) {
                delta.colorMode |=
                        change.colorMode & Configuration.COLOR_MODE_WIDE_COLOR_GAMUT_MASK;
            }

            if ((base.colorMode & Configuration.COLOR_MODE_HDR_MASK)
                    != (change.colorMode & Configuration.COLOR_MODE_HDR_MASK)) {
                delta.colorMode |= change.colorMode & Configuration.COLOR_MODE_HDR_MASK;
            }
        }
    }

    @RequiresApi(33)
    static class Api33Impl {
        private Api33Impl() {
            // This class is not instantiable.
        }

        static OnBackInvokedCallback registerOnBackPressedCallback(
                Object dispatcher, AppCompatDelegateImpl delegate) {
            OnBackInvokedCallback onBackInvokedCallback = delegate::onBackPressed;
            OnBackInvokedDispatcher typedDispatcher = (OnBackInvokedDispatcher) dispatcher;
            typedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_OVERLAY, onBackInvokedCallback);
            return onBackInvokedCallback;
        }

        static void unregisterOnBackInvokedCallback(Object dispatcher, Object callback) {
            OnBackInvokedCallback onBackInvokedCallback = (OnBackInvokedCallback) callback;
            OnBackInvokedDispatcher typedDispatcher = (OnBackInvokedDispatcher) dispatcher;
            typedDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback);
        }

        static OnBackInvokedDispatcher getOnBackInvokedDispatcher(Activity activity) {
            return activity.getOnBackInvokedDispatcher();
        }
    }
}
