/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to help validate themes used to render the inline suggestion UI.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(api = Build.VERSION_CODES.Q) //TODO(b/147116534): Update to R.
public final class InlineSuggestionThemeUtils {

    private static final String TAG = "InlineThemeUtils";

    // The pattern to match the value can be obtained by calling {@code Resources#getResourceName
    // (int)}. This name is a single string of the form "package:type/entry".
    private static final Pattern RESOURCE_NAME_PATTERN = Pattern.compile("([^:]+):([^/]+)/(\\S+)");

    /**
     * Returns a context wrapping the theme in the provided {@code themeName}, or fallback to the
     * default theme if the {@code themeName} doesn't pass validation.
     */
    @NonNull
    public static Context getContextThemeWrapper(@NonNull Context context,
            @Nullable String themeName) {
        Context contextThemeWrapper = maybeGetContextThemeWrapperWithStyle(context, themeName);
        if (contextThemeWrapper == null) {
            contextThemeWrapper = getDefaultContextThemeWrapper(context);
        }
        return contextThemeWrapper;
    }

    /**
     * Returns a context wrapping the theme in the provided {@code themeName}, or null if the {@code
     * themeName} doesn't pass validation.
     */
    @Nullable
    private static Context maybeGetContextThemeWrapperWithStyle(@NonNull Context context,
            @Nullable String themeName) {
        if (themeName == null) {
            return null;
        }
        Matcher matcher = RESOURCE_NAME_PATTERN.matcher(themeName);
        if (!matcher.matches()) {
            Log.d(TAG, "Can not parse the theme=" + themeName);
            return null;
        }
        String packageName = matcher.group(1);
        String type = matcher.group(2);
        String entry = matcher.group(3);
        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(type) || TextUtils.isEmpty(entry)) {
            Log.d(TAG, "Can not proceed with empty field values in the theme=" + themeName);
            return null;
        }
        Resources resources = null;
        try {
            resources = context.getPackageManager().getResourcesForApplication(
                    packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        int resId = resources.getIdentifier(entry, type, packageName);
        if (resId == Resources.ID_NULL) {
            return null;
        }
        Resources.Theme theme = resources.newTheme();
        theme.applyStyle(resId, true);
        if (!isAutofillInlineSuggestionTheme(theme, resId)) {
            Log.d(TAG, "Provided theme is not a child of Theme.InlineSuggestion, ignoring it.");
            return null;
        }
        // TODO(b/146454892): add font checking to disallow passing in custom font.
        return new ContextThemeWrapper(context, theme);
    }

    private static Context getDefaultContextThemeWrapper(@NonNull Context context) {
        Resources.Theme theme = context.getResources().newTheme();
        theme.applyStyle(R.style.Theme_AutofillInlineSuggestion, true);
        return new ContextThemeWrapper(context, theme);
    }

    /**
     * Returns true if the provided {@code theme} is a child theme of the
     * {@code @style/Theme.AutofillInlineSuggestion}.
     */
    private static boolean isAutofillInlineSuggestionTheme(@NonNull Resources.Theme theme,
            int styleAttr) {
        TypedArray ta = null;
        try {
            ta = theme.obtainStyledAttributes(null,
                    new int[]{R.attr.isAutofillInlineSuggestionTheme}, styleAttr, 0);
            if (ta.getIndexCount() == 0) {
                return false;
            }
            return ta.getBoolean(ta.getIndex(0), false);
        } finally {
            if (ta != null) {
                ta.recycle();
            }
        }
    }

    private InlineSuggestionThemeUtils() {
    }
}
