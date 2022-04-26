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

package androidx.appcompat.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.LayoutInflater;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.appcompat.R;

/**
 * A context wrapper that allows you to modify or replace the theme of the wrapped context.
 */
public class ContextThemeWrapper extends ContextWrapper {
    private final boolean mCheckedHandlesConfigChanges;
    private final boolean mHandlesConfigChanges;

    private int mThemeResource;
    private Resources.Theme mTheme;
    private LayoutInflater mInflater;
    private Configuration mOverrideConfiguration;
    private Resources mResources;

    /**
     * Creates a new context wrapper with no theme and no base context.
     * <p class="note">
     * <strong>Note:</strong> A base context <strong>must</strong> be attached
     * using {@link #attachBaseContext(Context)} before calling any other
     * method on the newly constructed context wrapper.
     */
    public ContextThemeWrapper() {
        super(null);

        mCheckedHandlesConfigChanges = false;
        mHandlesConfigChanges = false;
    }

    /**
     * Creates a new context wrapper with the specified theme.
     * <p>
     * The specified theme will be applied on top of the base context's theme.
     * Any attributes not explicitly defined in the theme identified by
     * <var>themeResId</var> will retain their original values.
     *
     * @param base the base context
     * @param themeResId the resource ID of the theme to be applied on top of
     *                   the base context's theme
     */
    public ContextThemeWrapper(Context base, @StyleRes int themeResId) {
        super(base);
        mThemeResource = themeResId;

        mCheckedHandlesConfigChanges = false;
        mHandlesConfigChanges = false;
    }

    /**
     * Creates a new context wrapper with the specified theme.
     * <p>
     * Unlike {@link #ContextThemeWrapper(Context, int)}, the theme passed to
     * this constructor will completely replace the base context's theme.
     *
     * @param base the base context
     * @param theme the theme against which resources should be inflated
     */
    public ContextThemeWrapper(Context base, Resources.Theme theme) {
        super(base);
        mTheme = theme;

        mCheckedHandlesConfigChanges = false;
        mHandlesConfigChanges = false;
    }

    /**
     * Creates a new context wrapper with the specified theme.
     *
     * @param base the base context
     * @param themeResId the resource ID of the theme to be applied on top of
     *                   the base context's theme
     * @param handlesConfigChanges whether the host Activity handles configuration
     *                             changes relevant (e.g. uiMode, locale) to AppCompat
     *
     * @hide For internal use only.
     */
    @RestrictTo(LIBRARY_GROUP)
    public ContextThemeWrapper(Context base, int themeResId, boolean handlesConfigChanges) {
        super(base);
        mThemeResource = themeResId;

        mCheckedHandlesConfigChanges = true;
        mHandlesConfigChanges = handlesConfigChanges;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    /**
     * Call to set an "override configuration" on this context -- this is
     * a configuration that replies one or more values of the standard
     * configuration that is applied to the context.  See
     * {@link Context#createConfigurationContext(Configuration)} for more
     * information.
     *
     * <p>This method can only be called once, and must be called before any
     * calls to {@link #getResources()} or {@link #getAssets()} are made.
     */
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (mResources != null) {
            throw new IllegalStateException(
                    "getResources() or getAssets() has already been called");
        }
        if (mOverrideConfiguration != null) {
            throw new IllegalStateException("Override configuration has already been set");
        }
        mOverrideConfiguration = new Configuration(overrideConfiguration);
    }

    @Override
    public Resources getResources() {
        return getResourcesInternal();
    }

    private Resources getResourcesInternal() {
        if (mResources == null) {
            if (mOverrideConfiguration == null) {
                mResources = super.getResources();
            } else if (Build.VERSION.SDK_INT >= 17 && shouldUseManagedResources()) {
                final Context resContext =
                        Api17Impl.createConfigurationContext(this, mOverrideConfiguration);
                mResources = resContext.getResources();
            } else {
                Resources res = super.getResources();
                Configuration newConfig = new Configuration(res.getConfiguration());
                newConfig.updateFrom(mOverrideConfiguration);
                mResources = new Resources(res.getAssets(), res.getDisplayMetrics(), newConfig);
            }
        }
        return mResources;
    }

    /**
     * Returns whether this wrapper should attempt to use a Resources object that receives
     * updates from the global ResourceManager.
     * <p>
     * This is typically only necessary for apps that are handling uiMode or locale changes and are
     * setting up their custom configuration outside of attachBaseContext, e.g. their initial
     * wrapper will be created with a default override configuration.
     */
    private boolean shouldUseManagedResources() {
        return !(mCheckedHandlesConfigChanges && mHandlesConfigChanges
                && isDefaultConfiguration(mOverrideConfiguration));
    }

    /**
     * Returns whether the parts of the Configuration that we care about (locale and uiMode) are set
     * to their default values.
     */
    private static boolean isDefaultConfiguration(Configuration config) {
        return config.uiMode == 0 && config.locale == null;
    }

    @Override
    public void setTheme(int resid) {
        if (mThemeResource != resid) {
            mThemeResource = resid;
            initializeTheme();
        }
    }

    /**
     * Returns the resource ID of the theme that is to be applied on top of the base context's
     * theme.
     */
    public int getThemeResId() {
        return mThemeResource;
    }

    @Override
    public Resources.Theme getTheme() {
        if (mTheme != null) {
            return mTheme;
        }

        if (mThemeResource == 0) {
            mThemeResource = R.style.Theme_AppCompat_Light;
        }
        initializeTheme();

        return mTheme;
    }

    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mInflater == null) {
                mInflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
            }
            return mInflater;
        }
        return getBaseContext().getSystemService(name);
    }

    /**
     * Called by {@link #setTheme} and {@link #getTheme} to apply a theme
     * resource to the current Theme object.  Can override to change the
     * default (simple) behavior.  This method will not be called in multiple
     * threads simultaneously.
     *
     * @param theme The Theme object being modified.
     * @param resid The theme style resource being applied to <var>theme</var>.
     * @param first Set to true if this is the first time a style is being
     *              applied to <var>theme</var>.
     */
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        theme.applyStyle(resid, true);
    }

    private void initializeTheme() {
        final boolean first = mTheme == null;
        if (first) {
            mTheme = getResources().newTheme();
            Resources.Theme theme = getBaseContext().getTheme();
            if (theme != null) {
                mTheme.setTo(theme);
            }
        }
        onApplyThemeResource(mTheme, mThemeResource, first);
    }

    @Override
    public AssetManager getAssets() {
        // Ensure we're returning assets with the correct configuration.
        return getResources().getAssets();
    }

    @RequiresApi(17)
    static class Api17Impl {
        private Api17Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static Context createConfigurationContext(ContextThemeWrapper contextThemeWrapper,
                Configuration overrideConfiguration) {
            return contextThemeWrapper.createConfigurationContext(overrideConfiguration);
        }
    }
}

