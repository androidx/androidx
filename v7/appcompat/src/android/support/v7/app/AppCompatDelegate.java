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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.WindowCompat;
import android.support.v7.appcompat.R;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class represents a delegate which you can use to extend AppCompat's support to any
 * {@link android.app.Activity}.
 * <p>
 * When using an {@link AppCompatDelegate}, you should any methods exposed in it rather than the
 * {@link android.app.Activity} method of the same name. This applies to:
 * <ul>
 *     <li>{@link #addContentView(android.view.View, android.view.ViewGroup.LayoutParams)}</li>
 *     <li>{@link #setContentView(int)}</li>
 *     <li>{@link #setContentView(android.view.View)}</li>
 *     <li>{@link #setContentView(android.view.View, android.view.ViewGroup.LayoutParams)}</li>
 *     <li>{@link #requestWindowFeature(int)}</li>
 *     <li>{@link #invalidateOptionsMenu()}</li>
 *     <li>{@link #startSupportActionMode(android.support.v7.view.ActionMode.Callback)}</li>
 *     <li>{@link #setSupportActionBar(android.support.v7.widget.Toolbar)}</li>
 *     <li>{@link #getSupportActionBar()}</li>
 *     <li>{@link #getMenuInflater()}</li>
 * </ul>
 * There also some Activity lifecycle methods which should be proxied to the delegate:
 * <ul>
 *     <li>{@link #onCreate(android.os.Bundle)}</li>
 *     <li>{@link #onPostCreate(android.os.Bundle)}</li>
 *     <li>{@link #onConfigurationChanged(android.content.res.Configuration)}</li>
 *     <li>{@link #setTitle(CharSequence)}</li>
 *     <li>{@link #onStop()}</li>
 *     <li>{@link #onDestroy()}</li>
 * </ul>
 * <p>
 * An {@link Activity} can only be linked with one {@link AppCompatDelegate} instance,
 * so the instance returned from {@link #create(Activity, AppCompatCallback)} should be kept
 * until the Activity is destroyed.
 */
public abstract class AppCompatDelegate {

    static final String TAG = "AppCompatDelegate";

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
        final int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 23) {
            return new AppCompatDelegateImplV23(context, window, callback);
        } else if (sdk >= 14) {
            return new AppCompatDelegateImplV14(context, window, callback);
        } else if (sdk >= 11) {
            return new AppCompatDelegateImplV11(context, window, callback);
        } else {
            return new AppCompatDelegateImplV7(context, window, callback);
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
     * @param toolbar Toolbar to set as the Activity's action bar
     */
    public abstract void setSupportActionBar(Toolbar toolbar);

    /**
     * Return the value of this call from your {@link Activity#getMenuInflater()}
     */
    public abstract MenuInflater getMenuInflater();

    /**
     * Should be called from {@link Activity#onCreate Activity.onCreate()}
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
     * Should be called from {@link Activity#onStop Activity.onStop()}
     */
    public abstract void onStop();

    /**
     * Should be called from {@link Activity#onPostResume()}
     */
    public abstract void onPostResume();

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
    public abstract void setTitle(CharSequence title);

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
    public abstract ActionMode startSupportActionMode(ActionMode.Callback callback);

    /**
     * Installs AppCompat's {@link android.view.LayoutInflater} Factory so that it can replace
     * the framework widgets with compatible tinted versions. This should be called before
     * {@code super.onCreate()} as so:
     * <pre class="prettyprint">
     * protected void onCreate(Bundle savedInstanceState) {
     *     getDelegate().installViewFactory();
     *     super.onCreate(savedInstanceState);
     *     getDelegate().onCreate(savedInstanceState);
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
     * {@link android.support.v4.view.LayoutInflaterFactory LayoutInflaterFactory} in order
     * to return tint-aware widgets.
     * <p>
     * This is only needed if you are using your own
     * {@link android.view.LayoutInflater LayoutInflater} factory, and have therefore not
     * installed the default factory via {@link #installViewFactory()}.
     */
    public abstract View createView(View parent, String name, @NonNull Context context,
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

}
