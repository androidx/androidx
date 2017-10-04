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

package android.support.v7.app;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.WindowCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
 *     <li>{@link #startSupportActionMode(android.support.v7.view.ActionMode.Callback)}</li>
 *     <li>{@link #setSupportActionBar(android.support.v7.widget.Toolbar)}</li>
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

    static final String TAG = "AppCompatDelegate";

    /**
     * Mode which means to not use night mode, and therefore prefer {@code notnight} qualified
     * resources where available, regardless of the time.
     *
     * @see #setLocalNightMode(int)
     */
    public static final int MODE_NIGHT_NO = 1;

    /**
     * Mode which means to always use night mode, and therefore prefer {@code night} qualified
     * resources where available, regardless of the time.
     *
     * @see #setLocalNightMode(int)
     */
    public static final int MODE_NIGHT_YES = 2;

    /**
     * Mode which means to use night mode when it is determined that it is night or not.
     *
     * <p>The calculation used to determine whether it is night or not makes use of the location
     * APIs (if this app has the necessary permissions). This allows us to generate accurate
     * sunrise and sunset times. If this app does not have permission to access the location APIs
     * then we use hardcoded times which will be less accurate.</p>
     *
     * @see #setLocalNightMode(int)
     */
    public static final int MODE_NIGHT_AUTO = 0;

    /**
     * Mode which uses the system's night mode setting to determine if it is night or not.
     *
     * @see #setLocalNightMode(int)
     */
    public static final int MODE_NIGHT_FOLLOW_SYSTEM = -1;

    static final int MODE_NIGHT_UNSPECIFIED = -100;

    @NightMode
    private static int sDefaultNightMode = MODE_NIGHT_FOLLOW_SYSTEM;

    private static boolean sCompatVectorFromResourcesEnabled = false;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({MODE_NIGHT_NO, MODE_NIGHT_YES, MODE_NIGHT_AUTO, MODE_NIGHT_FOLLOW_SYSTEM,
            MODE_NIGHT_UNSPECIFIED})
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
     * Create a {@link android.support.v7.app.AppCompatDelegate} to use with {@code activity}.
     *
     * @param callback An optional callback for AppCompat specific events
     */
    public static AppCompatDelegate create(Activity activity, AppCompatCallback callback) {
        return create(activity, activity.getWindow(), callback);
    }

    /**
     * Create a {@link android.support.v7.app.AppCompatDelegate} to use with {@code dialog}.
     *
     * @param callback An optional callback for AppCompat specific events
     */
    public static AppCompatDelegate create(Dialog dialog, AppCompatCallback callback) {
        return create(dialog.getContext(), dialog.getWindow(), callback);
    }

    private static AppCompatDelegate create(Context context, Window window,
            AppCompatCallback callback) {
        if (Build.VERSION.SDK_INT >= 24) {
            return new AppCompatDelegateImplN(context, window, callback);
        } else if (Build.VERSION.SDK_INT >= 23) {
            return new AppCompatDelegateImplV23(context, window, callback);
        } else if (Build.VERSION.SDK_INT >= 14) {
            return new AppCompatDelegateImplV14(context, window, callback);
        } else if (Build.VERSION.SDK_INT >= 11) {
            return new AppCompatDelegateImplV11(context, window, callback);
        } else {
            return new AppCompatDelegateImplV9(context, window, callback);
        }
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
     * Allow AppCompat to apply the {@code night} and {@code notnight} resource qualifiers.
     *
     * <p>Doing this enables the
     * {@link
     * android.support.v7.appcompat.R.style#Theme_AppCompat_DayNight Theme.AppCompat.DayNight}
     * family of themes to work, using the computed twilight to automatically select a dark or
     * light theme.</p>
     *
     * <p>You can override the night mode using {@link #setLocalNightMode(int)}.</p>
     *
     * <p>This only works on devices running
     * {@link Build.VERSION_CODES#ICE_CREAM_SANDWICH ICE_CREAM_SANDWICH} and above.</p>
     *
     * <p>If this is called after the host component has been created, the component will either be
     * automatically recreated or its {@link Configuration} updated. Which one depends on how
     * the component is setup (via {@code android:configChanges} or similar).</p>
     *
     * @see #setDefaultNightMode(int)
     * @see #setLocalNightMode(int)
     *
     * @return true if the night mode was applied, false if not
     */
    public abstract boolean applyDayNight();

    /**
     * Override the night mode used for this delegate's host component. This method only takes
     * effect for those situations where {@link #applyDayNight()} works.
     *
     * <p>As this will call {@link #applyDayNight()}, the host component might be
     * recreated automatically.</p>
     */
    public abstract void setLocalNightMode(@NightMode int mode);

    /**
     * Sets the default night mode. This is used across all activities/dialogs but can be overridden
     * locally via {@link #setLocalNightMode(int)}.
     *
     * <p>This method only takes effect for those situations where {@link #applyDayNight()} works.
     * Defaults to {@link #MODE_NIGHT_NO}.</p>
     *
     * <p>This only takes effect for components which are created after the call. Any components
     * which are already open will not be updated.</p>
     *
     * @see #setLocalNightMode(int)
     * @see #getDefaultNightMode()
     */
    public static void setDefaultNightMode(@NightMode int mode) {
        switch (mode) {
            case MODE_NIGHT_AUTO:
            case MODE_NIGHT_NO:
            case MODE_NIGHT_YES:
            case MODE_NIGHT_FOLLOW_SYSTEM:
                sDefaultNightMode = mode;
                break;
            default:
                Log.d(TAG, "setDefaultNightMode() called with an unknown mode");
                break;
        }
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
     * {@link android.support.v7.widget.AppCompatImageView#setImageResource(int)} and its
     * {@code app:srcCompat} attribute. They can also be used in anything which AppCompat inflates
     * for you, such as menu resources.</p>
     *
     * <p>Please note: this only takes effect in Activities created after this call.</p>
     */
    public static void setCompatVectorFromResourcesEnabled(boolean enabled) {
        sCompatVectorFromResourcesEnabled = enabled;
    }

    /**
     * Returns whether vector drawables on older platforms (< API 21) can be accessed from within
     * resources.
     *
     * @see #setCompatVectorFromResourcesEnabled(boolean)
     */
    public static boolean isCompatVectorFromResourcesEnabled() {
        return sCompatVectorFromResourcesEnabled;
    }
}
