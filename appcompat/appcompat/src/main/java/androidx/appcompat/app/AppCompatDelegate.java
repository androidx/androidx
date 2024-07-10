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

package androidx.appcompat.app;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import static java.util.Objects.requireNonNull;

import android.app.Activity;
import android.app.Dialog;
import android.app.LocaleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.AnyThread;
import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.VectorEnabledTintResources;
import androidx.collection.ArraySet;
import androidx.core.app.AppLocalesStorageHelper;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.FragmentActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * This class represents a delegate which you can use to extend AppCompat's support to any
 * {@link android.app.Activity}.
 *
 * <p>When using an {@link AppCompatDelegate}, you should call the following methods instead of the
 * {@link android.app.Activity} method of the same name:</p>
 * <ul>
 *     <li>{@link #addContentView(android.view.View, android.view.ViewGroup.LayoutParams)}</li>
 *     <li>{@link #setContentView(int)}</li>
 *     <li>{@link #setContentView(android.view.View)}</li>
 *     <li>{@link #setContentView(android.view.View, android.view.ViewGroup.LayoutParams)}</li>
 *     <li>{@link #requestWindowFeature(int)}</li>
 *     <li>{@link #hasWindowFeature(int)}</li>
 *     <li>{@link #invalidateOptionsMenu()}</li>
 *     <li>{@link #startSupportActionMode(androidx.appcompat.view.ActionMode.Callback)}</li>
 *     <li>{@link #setSupportActionBar(androidx.appcompat.widget.Toolbar)}</li>
 *     <li>{@link #getSupportActionBar()}</li>
 *     <li>{@link #getMenuInflater()}</li>
 *     <li>{@link #findViewById(int)}</li>
 * </ul>
 *
 * <p>The following methods should be called from the {@link android.app.Activity} method of the
 * same name:</p>
 * <ul>
 *     <li>{@link #onCreate(android.os.Bundle)}</li>
 *     <li>{@link #onPostCreate(android.os.Bundle)}</li>
 *     <li>{@link #onConfigurationChanged(android.content.res.Configuration)}</li>
 *     <li>{@link #onStart()}</li>
 *     <li>{@link #onStop()}</li>
 *     <li>{@link #onPostResume()}</li>
 *     <li>{@link #onSaveInstanceState(Bundle)}</li>
 *     <li>{@link #setTitle(CharSequence)}</li>
 *     <li>{@link #onStop()}</li>
 *     <li>{@link #onDestroy()}</li>
 * </ul>
 *
 * <p>An {@link Activity} can only be linked with one {@link AppCompatDelegate} instance,
 * therefore the instance returned from {@link #create(Activity, AppCompatCallback)} should be
 * retained until the Activity is destroyed.</p>
 */
public abstract class AppCompatDelegate {
    static final boolean DEBUG = false;
    static final String TAG = "AppCompatDelegate";

    static SerialExecutor sSerialExecutorForLocalesStorage = new
            SerialExecutor(new ThreadPerTaskExecutor());

    static final String APP_LOCALES_META_DATA_HOLDER_SERVICE_NAME = "androidx.appcompat.app"
            + ".AppLocalesMetadataHolderService";

    /**
     * Implementation of {@link java.util.concurrent.Executor} that executes runnables serially
     * by synchronizing the {@link Executor#execute(Runnable)} method and maintaining a tasks
     * queue.
     */
    static class SerialExecutor implements Executor {
        private final Object mLock = new Object();
        final Queue<Runnable> mTasks = new ArrayDeque<>();
        final Executor mExecutor;
        Runnable mActive;

        SerialExecutor(Executor executor) {
            this.mExecutor = executor;
        }

        @Override
        public void execute(final Runnable r) {
            synchronized (mLock) {
                mTasks.add(() -> {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                });
                if (mActive == null) {
                    scheduleNext();
                }
            }
        }

        protected void scheduleNext() {
            synchronized (mLock) {
                if ((mActive = mTasks.poll()) != null) {
                    mExecutor.execute(mActive);
                }
            }
        }
    }

    /**
     * Implementation of {@link java.util.concurrent.Executor} that executes each runnable on a
     * new thread.
     */
    static class ThreadPerTaskExecutor implements Executor {
        @Override
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    /**
     * Mode which uses the system's night mode setting to determine if it is night or not.
     *
     * @see #setLocalNightMode(int)
     */
    public static final int MODE_NIGHT_FOLLOW_SYSTEM = -1;

    /**
     * Night mode which switches between dark and light mode depending on the time of day
     * (dark at night, light in the day).
     *
     * <p>The calculation used to determine whether it is night or not makes use of the location
     * APIs (if this app has the necessary permissions). This allows us to generate accurate
     * sunrise and sunset times. If this app does not have permission to access the location APIs
     * then we use hardcoded times which will be less accurate.</p>
     *
     * @deprecated Automatic switching of dark/light based on the current time is deprecated.
     * Considering using an explicit setting, or {@link #MODE_NIGHT_AUTO_BATTERY}.
     */
    @Deprecated
    public static final int MODE_NIGHT_AUTO_TIME = 0;

    /**
     * @deprecated Use {@link AppCompatDelegate#MODE_NIGHT_AUTO_TIME} instead
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public static final int MODE_NIGHT_AUTO = MODE_NIGHT_AUTO_TIME;

    /**
     * Night mode which uses always uses a light mode, enabling {@code notnight} qualified
     * resources regardless of the time.
     *
     * @see #setLocalNightMode(int)
     */
    public static final int MODE_NIGHT_NO = 1;

    /**
     * Night mode which uses always uses a dark mode, enabling {@code night} qualified
     * resources regardless of the time.
     *
     * @see #setLocalNightMode(int)
     */
    public static final int MODE_NIGHT_YES = 2;

    /**
     * Night mode which uses a dark mode when the system's 'Battery Saver' feature is enabled,
     * otherwise it uses a 'light mode'. This mode can help the device to decrease power usage,
     * depending on the display technology in the device.
     *
     * <em>Please note: this mode should only be used when running on devices which do not
     * provide a similar device-wide setting.</em>
     *
     * @see #setLocalNightMode(int)
     */
    public static final int MODE_NIGHT_AUTO_BATTERY = 3;

    /**
     * An unspecified mode for night mode. This is primarily used with
     * {@link #setLocalNightMode(int)}, to allow the default night mode to be used.
     * If both the default and local night modes are set to this value, then the default value of
     * {@link #MODE_NIGHT_FOLLOW_SYSTEM} is applied.
     *
     * @see AppCompatDelegate#setDefaultNightMode(int)
     */
    public static final int MODE_NIGHT_UNSPECIFIED = -100;

    @NightMode
    private static int sDefaultNightMode = MODE_NIGHT_UNSPECIFIED;

    private static LocaleListCompat sRequestedAppLocales = null;
    private static LocaleListCompat sStoredAppLocales = null;
    private static Boolean sIsAutoStoreLocalesOptedIn = null;
    private static boolean sIsFrameworkSyncChecked = false;

    /**
     * All AppCompatDelegate instances associated with a "live" Activity, e.g. lifecycle state is
     * post-onCreate and pre-onDestroy. These instances are used to instrument night mode's uiMode
     * configuration changes.
     */
    private static final ArraySet<WeakReference<AppCompatDelegate>> sActivityDelegates =
            new ArraySet<>();
    private static final Object sActivityDelegatesLock = new Object();
    private static final Object sAppLocalesStorageSyncLock = new Object();

    @SuppressWarnings("deprecation")
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({MODE_NIGHT_NO, MODE_NIGHT_YES, MODE_NIGHT_AUTO_TIME, MODE_NIGHT_FOLLOW_SYSTEM,
            MODE_NIGHT_UNSPECIFIED, MODE_NIGHT_AUTO_BATTERY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface NightMode {}

    @IntDef({MODE_NIGHT_NO, MODE_NIGHT_YES, MODE_NIGHT_FOLLOW_SYSTEM})
    @Retention(RetentionPolicy.SOURCE)
    @interface ApplyableNightMode {}

    /**
     * Flag for enabling the support Action Bar.
     *
     * <p>This is enabled by default for some devices. The Action Bar replaces the title bar and
     * provides an alternate location for an on-screen menu button on some devices.
     */
    public static final int FEATURE_SUPPORT_ACTION_BAR = 100 + WindowCompat.FEATURE_ACTION_BAR;

    /**
     * Flag for requesting an support Action Bar that overlays window content.
     * Normally an Action Bar will sit in the space above window content, but if this
     * feature is requested along with {@link #FEATURE_SUPPORT_ACTION_BAR} it will be layered over
     * the window content itself. This is useful if you would like your app to have more control
     * over how the Action Bar is displayed, such as letting application content scroll beneath
     * an Action Bar with a transparent background or otherwise displaying a transparent/translucent
     * Action Bar over application content.
     *
     * <p>This mode is especially useful with {@code View.SYSTEM_UI_FLAG_FULLSCREEN}, which allows
     * you to seamlessly hide the action bar in conjunction with other screen decorations.
     * When an ActionBar is in this mode it will adjust the insets provided to
     * {@link View#fitSystemWindows(android.graphics.Rect) View.fitSystemWindows(Rect)}
     * to include the content covered by the action bar, so you can do layout within
     * that space.
     */
    public static final int FEATURE_SUPPORT_ACTION_BAR_OVERLAY =
            100 + WindowCompat.FEATURE_ACTION_BAR_OVERLAY;

    /**
     * Flag for specifying the behavior of action modes when an Action Bar is not present.
     * If overlay is enabled, the action mode UI will be allowed to cover existing window content.
     */
    public static final int FEATURE_ACTION_MODE_OVERLAY = WindowCompat.FEATURE_ACTION_MODE_OVERLAY;

    /**
     * Create an {@link androidx.appcompat.app.AppCompatDelegate} to use with {@code activity}.
     *
     * @param callback An optional callback for AppCompat specific events
     */
    @NonNull
    public static AppCompatDelegate create(@NonNull Activity activity,
            @Nullable AppCompatCallback callback) {
        return new AppCompatDelegateImpl(activity, callback);
    }

    /**
     * Create an {@link androidx.appcompat.app.AppCompatDelegate} to use with {@code dialog}.
     *
     * @param callback An optional callback for AppCompat specific events
     */
    @NonNull
    public static AppCompatDelegate create(@NonNull Dialog dialog,
            @Nullable AppCompatCallback callback) {
        return new AppCompatDelegateImpl(dialog, callback);
    }

    /**
     * Create an {@link androidx.appcompat.app.AppCompatDelegate} to use with a {@code context}
     * and a {@code window}.
     *
     * @param callback An optional callback for AppCompat specific events
     */
    @NonNull
    public static AppCompatDelegate create(@NonNull Context context, @NonNull Window window,
            @Nullable AppCompatCallback callback) {
        return new AppCompatDelegateImpl(context, window, callback);
    }

    /**
     * Create an {@link androidx.appcompat.app.AppCompatDelegate} to use with a {@code context}
     * and hosted by an {@code Activity}.
     *
     * @param callback An optional callback for AppCompat specific events
     */
    @NonNull
    public static AppCompatDelegate create(@NonNull Context context, @NonNull Activity activity,
            @Nullable AppCompatCallback callback) {
        return new AppCompatDelegateImpl(context, activity, callback);
    }

    /**
     * Private constructor
     */
    AppCompatDelegate() {}

    /**
     * Support library version of {@link Activity#getActionBar}.
     *
     * @return AppCompat's action bar, or null if it does not have one.
     */
    @Nullable
    public abstract ActionBar getSupportActionBar();

    /**
     * Set a {@link Toolbar} to act as the {@link ActionBar} for this delegate.
     *
     * <p>When set to a non-null value the {@link #getSupportActionBar()} ()} method will return
     * an {@link ActionBar} object that can be used to control the given toolbar as if it were
     * a traditional window decor action bar. The toolbar's menu will be populated with the
     * Activity's options menu and the navigation button will be wired through the standard
     * {@link android.R.id#home home} menu select action.</p>
     *
     * <p>In order to use a Toolbar within the Activity's window content the application
     * must not request the window feature
     * {@link AppCompatDelegate#FEATURE_SUPPORT_ACTION_BAR FEATURE_SUPPORT_ACTION_BAR}.</p>
     *
     * @param toolbar Toolbar to set as the Activity's action bar, or {@code null} to clear it
     */
    public abstract void setSupportActionBar(@Nullable Toolbar toolbar);

    /**
     * Return the value of this call from your {@link Activity#getMenuInflater()}
     */
    public abstract MenuInflater getMenuInflater();

    /**
     * Should be called from {@link Activity#onCreate Activity.onCreate()}.
     *
     * <p>This should be called before {@code super.onCreate()} as so:</p>
     * <pre class="prettyprint">
     * protected void onCreate(Bundle savedInstanceState) {
     *     getDelegate().onCreate(savedInstanceState);
     *     super.onCreate(savedInstanceState);
     *     // ...
     * }
     * </pre>
     */
    public abstract void onCreate(Bundle savedInstanceState);

    /**
     * Should be called from {@link Activity#onPostCreate(android.os.Bundle)}
     */
    public abstract void onPostCreate(Bundle savedInstanceState);

    /**
     * Should be called from
     * {@link Activity#onConfigurationChanged}
     */
    public abstract void onConfigurationChanged(Configuration newConfig);

    /**
     * Should be called from {@link Activity#onStart()} Activity.onStart()}
     */
    public abstract void onStart();

    /**
     * Should be called from {@link Activity#onStop Activity.onStop()}
     */
    public abstract void onStop();

    /**
     * Should be called from {@link Activity#onPostResume()}
     */
    public abstract void onPostResume();

    /**
     * This should be called from {@link Activity#setTheme(int)} to notify AppCompat of what
     * the current theme resource id is.
     */
    public void setTheme(@StyleRes int themeResId) {
    }

    /**
     * Finds a view that was identified by the id attribute from the XML that
     * was processed in {@link #onCreate}.
     *
     * @return The view if found or null otherwise.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    @Nullable
    public abstract <T extends View> T findViewById(@IdRes int id);

    /**
     * Should be called instead of {@link Activity#setContentView(android.view.View)}}
     */
    public abstract void setContentView(View v);

    /**
     * Should be called instead of {@link Activity#setContentView(int)}}
     */
    public abstract void setContentView(@LayoutRes int resId);

    /**
     * Should be called instead of
     * {@link Activity#setContentView(android.view.View, android.view.ViewGroup.LayoutParams)}}
     */
    public abstract void setContentView(View v, ViewGroup.LayoutParams lp);

    /**
     * Should be called instead of
     * {@link Activity#addContentView(android.view.View, android.view.ViewGroup.LayoutParams)}}
     */
    public abstract void addContentView(View v, ViewGroup.LayoutParams lp);

    /**
     * @deprecated use {@link #attachBaseContext2(Context)} instead.
     */
    @Deprecated
    public void attachBaseContext(Context context) {
    }

    /**
     * Should be called from {@link Activity#attachBaseContext(Context)}.
     */
    @NonNull
    @CallSuper
    public Context attachBaseContext2(@NonNull Context context) {
        attachBaseContext(context);
        return context;
    }

    /**
     * Should be called from {@link Activity#onTitleChanged(CharSequence, int)}}
     */
    public abstract void setTitle(@Nullable CharSequence title);

    /**
     * Should be called from {@link Activity#invalidateOptionsMenu()}} or
     * {@link FragmentActivity#supportInvalidateOptionsMenu()}.
     */
    public abstract void invalidateOptionsMenu();

    /**
     * Should be called from {@link Activity#onDestroy()}
     */
    public abstract void onDestroy();

    /**
     * Returns an {@link ActionBarDrawerToggle.Delegate} which can be returned from your Activity
     * if it implements {@link ActionBarDrawerToggle.DelegateProvider}.
     */
    @Nullable
    public abstract ActionBarDrawerToggle.Delegate getDrawerToggleDelegate();

    /**
     * Enable extended window features.  This should be called instead of
     * {@link android.app.Activity#requestWindowFeature(int)} or
     * {@link android.view.Window#requestFeature getWindow().requestFeature()}.
     *
     * @param featureId The desired feature as defined in {@link android.view.Window}.
     * @return Returns true if the requested feature is supported and now
     *         enabled.
     */
    public abstract boolean requestWindowFeature(int featureId);

    /**
     * Query for the availability of a certain feature.
     *
     * <p>This should be called instead of {@link android.view.Window#hasFeature(int)}.</p>
     *
     * @param featureId The feature ID to check
     * @return true if the feature is enabled, false otherwise.
     */
    public abstract boolean hasWindowFeature(int featureId);

    /**
     * Start an action mode.
     *
     * @param callback Callback that will manage lifecycle events for this context mode
     * @return The ContextMode that was started, or null if it was canceled
     */
    @Nullable
    public abstract ActionMode startSupportActionMode(@NonNull ActionMode.Callback callback);

    /**
     * Installs AppCompat's {@link android.view.LayoutInflater} Factory so that it can replace
     * the framework widgets with compatible tinted versions. This should be called before
     * {@code super.onCreate()} as so:
     * <pre class="prettyprint">
     * protected void onCreate(Bundle savedInstanceState) {
     *     getDelegate().installViewFactory();
     *     getDelegate().onCreate(savedInstanceState);
     *     super.onCreate(savedInstanceState);
     *
     *     // ...
     * }
     * </pre>
     * If you are using your own {@link android.view.LayoutInflater.Factory Factory} or
     * {@link android.view.LayoutInflater.Factory2 Factory2} then you can omit this call, and instead call
     * {@link #createView(android.view.View, String, android.content.Context, android.util.AttributeSet)}
     * from your factory to return any compatible widgets.
     */
    public abstract void installViewFactory();

    /**
     * This should be called from a
     * {@link android.view.LayoutInflater.Factory2 LayoutInflater.Factory2} in order
     * to return tint-aware widgets.
     * <p>
     * This is only needed if you are using your own
     * {@link android.view.LayoutInflater LayoutInflater} factory, and have therefore not
     * installed the default factory via {@link #installViewFactory()}.
     */
    public abstract View createView(@Nullable View parent, String name, @NonNull Context context,
            @NonNull AttributeSet attrs);

    /**
     * Whether AppCompat handles any native action modes itself.
     * <p>This methods only takes effect on
     * {@link android.os.Build.VERSION_CODES#ICE_CREAM_SANDWICH} and above.
     *
     * @param enabled whether AppCompat should handle native action modes.
     */
    public abstract void setHandleNativeActionModesEnabled(boolean enabled);

    /**
     * Returns whether AppCompat handles any native action modes itself.
     *
     * @return true if AppCompat should handle native action modes.
     */
    public abstract boolean isHandleNativeActionModesEnabled();

    /**
     * Allows AppCompat to save instance state.
     */
    public abstract void onSaveInstanceState(Bundle outState);

    /**
     * Applies the currently selected night mode to this delegate's host component.
     *
     * <p>This enables the
     * {@link
     * androidx.appcompat.R.style#Theme_AppCompat_DayNight Theme.AppCompat.DayNight}
     * family of themes to work, using the specified mode.</p>
     *
     * <p>You can be notified when the night changes by overriding the
     * {@link AppCompatActivity#onNightModeChanged(int)} method.</p>
     *
     * @see #setDefaultNightMode(int)
     * @see #setLocalNightMode(int)
     *
     * @return true if the night mode was applied, false if not
     */
    public abstract boolean applyDayNight();

    /**
     * Sets the {@link OnBackInvokedDispatcher} for handling system back for Android SDK 33 and
     * above.
     * <p>
     * If the delegate is hosted by an {@link Activity}, the default dispatcher is obtained via
     * {@link Activity#getOnBackInvokedDispatcher()}.
     *
     * @param dispatcher the OnBackInvokedDispatcher to be set on this delegate, or {@code null}
     *                   to use the default dispatcher
     */
    @CallSuper
    @RequiresApi(33)
    public void setOnBackInvokedDispatcher(@Nullable OnBackInvokedDispatcher dispatcher) {
        // Stub.
    }

    /**
     * Applies the current locales to this delegate's host component.
     *
     * <p>Apps can be notified when the locales are changed by overriding the
     * {@link AppCompatActivity#onLocalesChanged(LocaleListCompat)} method.</p>
     *
     * <p>This is a default implementation and it is overridden atin
     * {@link AppCompatDelegateImpl#applyAppLocales()} </p>
     *
     * @see #setApplicationLocales(LocaleListCompat)
     *
     * @return true if requested app-specific locales were applied, false if not.
     */
    boolean applyAppLocales() {
        return false;
    }

    /**
     * Returns the context for the current delegate.
     */
    @Nullable
    public Context getContextForDelegate() {
        return null;
    }

    /**
     * Override the night mode used for this delegate's host component.
     *
     * <p>When setting a mode to be used across an entire app, the
     * {@link #setDefaultNightMode(int)} method is preferred.</p>
     *
     * <p>If this is called after the host component has been created, a {@code uiMode}
     * configuration change will occur, which may result in the component being recreated.</p>
     *
     * <p>It is not recommended to use this method on a delegate attached to a {@link Dialog}.
     * Dialogs use the host Activity as their context, resulting in the dialog's night mode
     * overriding the Activity's night mode.
     *
     * <p><strong>Note:</strong> This method is not recommended for use on devices running SDK 16
     * or earlier, as the specified night mode configuration may leak to other activities. Instead,
     * consider using {@link #setDefaultNightMode(int)} to specify an app-wide night mode.
     *
     * @see #getLocalNightMode()
     * @see #setDefaultNightMode(int)
     */
    public abstract void setLocalNightMode(@NightMode int mode);

    /**
     * Returns the night mode previously set via {@link #getLocalNightMode()}.
     */
    @NightMode
    public int getLocalNightMode() {
        return MODE_NIGHT_UNSPECIFIED;
    }

    /**
     * Sets the default night mode. This is the default value used for all components, but can
     * be overridden locally via {@link #setLocalNightMode(int)}.
     *
     * <p>This is the primary method to control the DayNight functionality, since it allows
     * the delegates to avoid unnecessary recreations when possible.</p>
     *
     * <p>If this method is called after any host components with attached
     * {@link AppCompatDelegate}s have been 'created', a {@code uiMode} configuration change
     * will occur in each. This may result in those components being recreated, depending
     * on their manifest configuration.</p>
     *
     * <p>Defaults to {@link #MODE_NIGHT_FOLLOW_SYSTEM}.</p>
     *
     * @see #setLocalNightMode(int)
     * @see #getDefaultNightMode()
     */
    @SuppressWarnings("deprecation")
    public static void setDefaultNightMode(@NightMode int mode) {
        if (DEBUG) {
            Log.d(TAG, String.format("setDefaultNightMode. New:%d, Current:%d",
                    mode, sDefaultNightMode));
        }
        switch (mode) {
            case MODE_NIGHT_NO:
            case MODE_NIGHT_YES:
            case MODE_NIGHT_FOLLOW_SYSTEM:
            case MODE_NIGHT_AUTO_TIME:
            case MODE_NIGHT_AUTO_BATTERY:
                if (sDefaultNightMode != mode) {
                    sDefaultNightMode = mode;
                    applyDayNightToActiveDelegates();
                } else if (DEBUG) {
                    Log.d(TAG, String.format("Not applying changes, sDefaultNightMode already %d",
                            mode));
                }
                break;
            default:
                Log.d(TAG, "setDefaultNightMode() called with an unknown mode");
                break;
        }
    }

    /**
     * Sets the current locales for the calling app.
     *
     * <p>If this method is called after any host components with attached
     * {@link AppCompatDelegate}s have been 'created', a {@link LocaleList} configuration
     * change will occur in each. This may result in those components being recreated, depending
     * on their manifest configuration.</p>
     *
     * <p>This method accepts {@link LocaleListCompat} as an input parameter.</p>
     *
     * <p>Apps should continue to read Locales via their in-process {@link LocaleList}s.</p>
     *
     * <p>Pass a {@link LocaleListCompat#getEmptyLocaleList()} to reset to the system locale.</p>
     *
     * <p><b>Note: This API should always be called after Activity.onCreate(), apart from any
     * exceptions explicitly mentioned in this documentation.</b></p>
     *
     * <p>On API level 33 and above, this API will handle storage automatically.</p>
     *
     * <p>For API levels below that, the developer has two options:</p>
     * <ul>
     *     <li>They can opt-in to automatic storage handled through the library. They can do this by
     *     adding a special metaData entry in their {@code AndroidManifest.xml}, similar to :
     *     <pre><code>
     *     &lt;service
     *         android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
     *         android:enabled="false"
     *         android:exported="false"&gt;
     *         &lt;meta-data
     *             android:name="autoStoreLocales"
     *             android:value="true" /&gt;
     *     &lt;/service&gt;
     *     </code></pre>
     *     They should be mindful that this will cause a blocking diskRead and diskWrite
     *     strictMode violation, and they might need to suppress it at their end.</li>
     *
     *     <li>The second option is that they can choose to handle storage themselves. In order to
     *     do so they must use this API to initialize locales during app-start up and provide
     *     their stored locales. In this case, API should be called before Activity.onCreate()
     *     in the activity lifecycle, e.g. in attachBaseContext().
     *     <b>Note: Developers should gate this to API versions < 33.</b>
     *     <p><b>This API should be called after Activity.onCreate() for all other cases.</b></p>
     *     </li>
     * </ul>
     *
     * <p>When the application using this API with API versions < 33 updates to a
     * version >= 33, then there can be two scenarios for this transition:
     * <ul>
     *     <li>If the developer has opted-in for autoStorage then the locales will be automatically
     *     synced to the framework. Developers must specify android:enabled="false" for the
     *     AppLocalesMetadataHolderService as shown in the meta-data entry above.</li>
     *     <li>If the developer has not opted-in for autoStorage then they will need to handle
     *     this transition on their end.</li>
     * </ul>
     *
     * <p><b>Note: This API work with the AppCompatActivity context, not for others context, for
     * Android 12 (API level 32) and earlier. If there is a requirement to get the localized
     * string which respects the per-app locale in non-AppCompatActivity context, please consider
     * using {@link androidx.core.content.ContextCompat#getString(Context, int)} or
     * {@link androidx.core.content.ContextCompat#getContextForLanguage(Context)}. </b></p>
     *
     * @param locales a list of locales.
     */
    public static void setApplicationLocales(@NonNull LocaleListCompat locales) {
        requireNonNull(locales);
        if (Build.VERSION.SDK_INT >= 33) {
            // If the API version is 33 (version for T) or above we want to redirect the call to
            // the framework API.
            Object localeManager = getLocaleManagerForApplication();
            if (localeManager != null) {
                Api33Impl.localeManagerSetApplicationLocales(localeManager,
                        Api24Impl.localeListForLanguageTags(locales.toLanguageTags()));
            }
        } else {
            if (DEBUG) {
                Log.d(TAG, String.format("sRequestedAppLocales. New:%s, Current:%s",
                        locales, sRequestedAppLocales));
            }
            if (!locales.equals(sRequestedAppLocales)) {
                synchronized (sActivityDelegatesLock) {
                    sRequestedAppLocales = locales;
                    applyLocalesToActiveDelegates();
                }
            } else if (DEBUG) {
                Log.d(TAG, String.format("Not applying changes, sRequestedAppLocales is already %s",
                        locales));
            }
        }
    }

    /**
     * Returns application locales for the calling app as a {@link LocaleListCompat}.
     *
     * <p>Returns a {@link LocaleListCompat#getEmptyLocaleList()} if no app-specific locales are
     * set.
     *
     * <p><b>Note: This API only work at AppCompatDelegate and it should always be called after
     * Activity.onCreate().</b></p>
     */
    @AnyThread
    @NonNull
    public static LocaleListCompat getApplicationLocales() {
        if (Build.VERSION.SDK_INT >= 33) {
            // If the API version is 33 or above we want to redirect the call to the framework API.
            Object localeManager = getLocaleManagerForApplication();
            if (localeManager != null) {
                return LocaleListCompat.wrap(Api33Impl.localeManagerGetApplicationLocales(
                        localeManager));
            }
        } else {
            if (sRequestedAppLocales != null) {
                // If app-specific locales exists then sRequestedApplicationLocales contains the
                // latest locales.
                return sRequestedAppLocales;
            }
        }
        return LocaleListCompat.getEmptyLocaleList();
    }

    /**
     * Returns the default night mode.
     *
     * @see #setDefaultNightMode(int)
     */
    @NightMode
    public static int getDefaultNightMode() {
        return sDefaultNightMode;
    }

    /**
     * Returns the requested app locales.
     *
     * @see #setApplicationLocales(LocaleListCompat)
     */
    @Nullable
    static LocaleListCompat getRequestedAppLocales() {
        return sRequestedAppLocales;
    }

    /**
     * Returns the stored app locales.
     *
     * @see #setApplicationLocales(LocaleListCompat)
     */
    @Nullable
    static LocaleListCompat getStoredAppLocales() {
        return sStoredAppLocales;
    }

    /**
     * Resets the static variables for requested and stored locales to null. This method is used
     * for testing as it mimics activity restart which is difficult to do in a test.
     */
    @VisibleForTesting
    static void resetStaticRequestedAndStoredLocales() {
        sRequestedAppLocales = null;
        sStoredAppLocales = null;
    }

    /**
     * Sets {@link AppCompatDelegate#sIsAutoStoreLocalesOptedIn} to the provided value. This method
     * is used for testing, setting sIsAutoStoreLocalesOptedIn to true mimics adding an opt-in
     * "autoStoreLocales" meta-data entry.
     *
     * see {@link AppCompatDelegate#setApplicationLocales(LocaleListCompat)}.
     */
    @VisibleForTesting
    static void setIsAutoStoreLocalesOptedIn(boolean isAutoStoreLocalesOptedIn) {
        sIsAutoStoreLocalesOptedIn = isAutoStoreLocalesOptedIn;
    }

    /**
     * Returns the localeManager for the current application using active delegates to fetch
     * context, returns null if no active delegates present.
     */
    @RequiresApi(33)
    static Object getLocaleManagerForApplication() {
        for (WeakReference<AppCompatDelegate> activeDelegate : sActivityDelegates) {
            final AppCompatDelegate delegate = activeDelegate.get();
            if (delegate != null) {
                Context context = delegate.getContextForDelegate();
                if (context != null) {
                    return context.getSystemService(Context.LOCALE_SERVICE);
                }
            }
        }
        return null;
    }

    /**
     * Returns true is the "autoStoreLocales" metaData is marked true in the app manifest.
     */
    static boolean isAutoStorageOptedIn(Context context) {
        if (sIsAutoStoreLocalesOptedIn == null) {
            try {
                ServiceInfo serviceInfo = AppLocalesMetadataHolderService.getServiceInfo(
                        context);
                if (serviceInfo.metaData != null) {
                    sIsAutoStoreLocalesOptedIn = serviceInfo.metaData.getBoolean(
                            /* key= */ "autoStoreLocales");
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "Checking for metadata for AppLocalesMetadataHolderService "
                        + ": Service not found");
                sIsAutoStoreLocalesOptedIn = false;
            }
        }
        return sIsAutoStoreLocalesOptedIn;
    }

    /**
     * Executes {@link AppCompatDelegate#syncRequestedAndStoredLocales(Context)} asynchronously
     * on a worker thread, serialized using {@link
     * AppCompatDelegate#sSerialExecutorForLocalesStorage}.
     *
     * <p>This is done to perform the storage read operation without blocking the main thread.</p>
     */
    void asyncExecuteSyncRequestedAndStoredLocales(Context context) {
        sSerialExecutorForLocalesStorage.execute(() -> syncRequestedAndStoredLocales(context));
    }

    /**
     * Syncs requested and persisted app-specific locales.
     *
     * <p>This sync is only performed if the developer has opted in to use the autoStoredLocales
     * feature, marked by the metaData "autoStoreLocales" wrapped in the service
     * "AppLocalesMetadataHolderService". If the metaData is not found in the manifest or holds
     * the value false then we return from this function without doing anything. If the metaData
     * is set to true, then we perform a sync for app-locales.</p>
     *
     * <p>If the API version is >=33, then the storage is checked for app-specific locales, if
     * found they are synced to the framework by calling the
     * {@link AppCompatDelegate#setApplicationLocales(LocaleListCompat)}</p>
     *
     * <p>If the API version is <33, then there are two scenarios:</p>
     * <ul>
     * <li>If the requestedAppLocales are not set then the app-specific locales are read from
     * storage. If persisted app-specific locales are found then they are used to
     * update the requestedAppLocales.</li>
     * <li>If the requestedAppLocales are populated and are different from the stored locales
     * then in that case the requestedAppLocales are stored and the static variable for
     * storedAppLocales is updated accordingly.</li>
     * </ul>
     */
    static void syncRequestedAndStoredLocales(Context context) {
        if (!isAutoStorageOptedIn(context)) {
            return;
        } else if (Build.VERSION.SDK_INT >= 33) {
            if (!sIsFrameworkSyncChecked) {
                // syncs locales from androidX to framework, it only happens once after the
                // device is updated to API version 33(Tiramisu) or above.
                sSerialExecutorForLocalesStorage.execute(() -> {
                    syncLocalesToFramework(context);
                    sIsFrameworkSyncChecked = true;
                });
            }
        } else {
            synchronized (sAppLocalesStorageSyncLock) {
                if (sRequestedAppLocales == null) {
                    if (sStoredAppLocales == null) {
                        sStoredAppLocales =
                                LocaleListCompat.forLanguageTags(
                                        AppLocalesStorageHelper.readLocales(context));
                    }
                    if (sStoredAppLocales.isEmpty()) {
                        // if both requestedLocales and storedLocales not set, then the user has not
                        // specified any application-specific locales. So no alterations in current
                        // application locales should take place.
                        return;
                    }
                    sRequestedAppLocales = sStoredAppLocales;
                } else if (!sRequestedAppLocales.equals(sStoredAppLocales)) {
                    // if requestedLocales is set and is not equal to the storedLocales then in this
                    // case we need to store these locales in storage.
                    sStoredAppLocales = sRequestedAppLocales;
                    AppLocalesStorageHelper.persistLocales(context,
                            sRequestedAppLocales.toLanguageTags());
                }
            }
        }
    }

    /**
     * Sets whether vector drawables on older platforms (< API 21) can be used within
     * {@link android.graphics.drawable.DrawableContainer} resources.
     *
     * <p>When enabled, AppCompat can intercept some drawable inflation from the framework, which
     * enables implicit inflation of vector drawables within
     * {@link android.graphics.drawable.DrawableContainer} resources. You can then use those
     * drawables in places such as {@code android:src} on {@link android.widget.ImageView},
     * or {@code android:drawableLeft} on {@link android.widget.TextView}. Example usage:</p>
     *
     * <pre>
     * &lt;selector xmlns:android=&quot;...&quot;&gt;
     *     &lt;item android:state_checked=&quot;true&quot;
     *           android:drawable=&quot;@drawable/vector_checked_icon&quot; /&gt;
     *     &lt;item android:drawable=&quot;@drawable/vector_icon&quot; /&gt;
     * &lt;/selector&gt;
     *
     * &lt;TextView
     *         ...
     *         android:drawableLeft=&quot;@drawable/vector_state_list_icon&quot; /&gt;
     * </pre>
     *
     * <p>This feature defaults to disabled, since enabling it can cause issues with memory usage,
     * and problems updating {@link Configuration} instances. If you update the configuration
     * manually, then you probably do not want to enable this. You have been warned.</p>
     *
     * <p>Even with this disabled, you can still use vector resources through
     * {@link androidx.appcompat.widget.AppCompatImageView#setImageResource(int)} and its
     * {@code app:srcCompat} attribute. They can also be used in anything which AppCompat inflates
     * for you, such as menu resources.</p>
     *
     * <p>Please note: this only takes effect in Activities created after this call.</p>
     */
    public static void setCompatVectorFromResourcesEnabled(boolean enabled) {
        VectorEnabledTintResources.setCompatVectorFromResourcesEnabled(enabled);
    }

    /**
     * Returns whether vector drawables on older platforms (< API 21) can be accessed from within
     * resources.
     *
     * @see #setCompatVectorFromResourcesEnabled(boolean)
     */
    public static boolean isCompatVectorFromResourcesEnabled() {
        return VectorEnabledTintResources.isCompatVectorFromResourcesEnabled();
    }

    static void addActiveDelegate(@NonNull AppCompatDelegate delegate) {
        synchronized (sActivityDelegatesLock) {
            // Remove any existing records pointing to the delegate.
            // There should not be any, but we'll make sure
            removeDelegateFromActives(delegate);
            // Add a new record to the set
            sActivityDelegates.add(new WeakReference<>(delegate));
        }
    }

    static void removeActivityDelegate(@NonNull AppCompatDelegate delegate) {
        synchronized (sActivityDelegatesLock) {
            // Remove any WeakRef records pointing to the delegate in the set
            removeDelegateFromActives(delegate);
        }
    }

    /**
     * Syncs app-specific locales from androidX to framework. This is used to maintain a smooth
     * transition for a device that updates from pre-T API versions to T.
     *
     * <p><b>NOTE:</b> This should only be called when auto-storage is opted-in. This method
     * uses the meta-data service provided during the opt-in and hence if the service is not found
     * this method will throw an error.</p>
     */
    static void syncLocalesToFramework(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            ComponentName app_locales_component = new ComponentName(
                    context, APP_LOCALES_META_DATA_HOLDER_SERVICE_NAME);

            if (context.getPackageManager().getComponentEnabledSetting(app_locales_component)
                    != COMPONENT_ENABLED_STATE_ENABLED) {
                // ComponentEnabledSetting for the app component app_locales_component is used as a
                // marker to represent that the locales has been synced from AndroidX to framework
                // If this marker is found in ENABLED state then we do not need to sync again.
                if (AppCompatDelegate.getApplicationLocales().isEmpty()) {
                    // We check if some locales are applied by the framework or not (this is done to
                    // ensure that we don't overwrite newer locales set by the framework). If no
                    // app-locales are found then we need to sync the app-specific locales from
                    // androidX to framework.

                    String appLocales = AppLocalesStorageHelper.readLocales(context);
                    // if locales are present in storage, call the setApplicationLocales() API. As
                    // the API version is >= 33, this call will be directed to the framework API and
                    // the locales will be persisted there.
                    Object localeManager = context.getSystemService(Context.LOCALE_SERVICE);
                    if (localeManager != null) {
                        AppCompatDelegate.Api33Impl.localeManagerSetApplicationLocales(
                                localeManager,
                                AppCompatDelegate.Api24Impl.localeListForLanguageTags(appLocales));
                    }
                }
                // setting ComponentEnabledSetting for app component using
                // AppLocalesMetadataHolderService (used only for locales, thus minimizing
                // the chances of conflicts). Setting it as ENABLED marks the success of app-locales
                // sync from AndroidX to framework.
                // Flag DONT_KILL_APP indicates that you don't want to kill the app containing the
                // component.
                context.getPackageManager().setComponentEnabledSetting(app_locales_component,
                        COMPONENT_ENABLED_STATE_ENABLED, /* flags= */ DONT_KILL_APP);
            }
        }
    }

    private static void removeDelegateFromActives(@NonNull AppCompatDelegate toRemove) {
        synchronized (sActivityDelegatesLock) {
            final Iterator<WeakReference<AppCompatDelegate>> i = sActivityDelegates.iterator();
            while (i.hasNext()) {
                final AppCompatDelegate delegate = i.next().get();
                if (delegate == toRemove || delegate == null) {
                    // If the delegate is the one to remove, or it is null (because of the WeakRef),
                    // remove it from the set
                    i.remove();
                }
            }
        }
    }

    private static void applyDayNightToActiveDelegates() {
        synchronized (sActivityDelegatesLock) {
            for (WeakReference<AppCompatDelegate> activeDelegate : sActivityDelegates) {
                final AppCompatDelegate delegate = activeDelegate.get();
                if (delegate != null) {
                    if (DEBUG) {
                        Log.d(TAG, "applyDayNightToActiveDelegates. Applying to " + delegate);
                    }
                    delegate.applyDayNight();
                }
            }
        }
    }

    private static void applyLocalesToActiveDelegates() {
        for (WeakReference<AppCompatDelegate> activeDelegate : sActivityDelegates) {
            final AppCompatDelegate delegate = activeDelegate.get();
            if (delegate != null) {
                if (DEBUG) {
                    Log.d(TAG, "applyLocalesToActiveDelegates. Applying to " + delegate);
                }
                delegate.applyAppLocales();
            }
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static LocaleList localeListForLanguageTags(String list) {
            return LocaleList.forLanguageTags(list);
        }
    }

    @RequiresApi(33)
    static class Api33Impl {
        private Api33Impl() {
            // This class is not instantiable.
        }

        static void localeManagerSetApplicationLocales(Object localeManager,
                LocaleList locales) {
            LocaleManager mLocaleManager = (LocaleManager) localeManager;
            mLocaleManager.setApplicationLocales(locales);
        }

        static LocaleList localeManagerGetApplicationLocales(Object localeManager) {
            LocaleManager mLocaleManager = (LocaleManager) localeManager;
            return mLocaleManager.getApplicationLocales();
        }
    }
}
