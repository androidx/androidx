/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.core.widget;

import static android.view.View.TEXT_DIRECTION_ANY_RTL;
import static android.view.View.TEXT_DIRECTION_FIRST_STRONG;
import static android.view.View.TEXT_DIRECTION_FIRST_STRONG_LTR;
import static android.view.View.TEXT_DIRECTION_FIRST_STRONG_RTL;
import static android.view.View.TEXT_DIRECTION_LOCALE;
import static android.view.View.TEXT_DIRECTION_LTR;
import static android.view.View.TEXT_DIRECTION_RTL;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.icu.text.DecimalFormatSymbols;
import android.os.Build;
import android.text.Editable;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.core.os.BuildCompat;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * Helper for accessing features in {@link TextView}.
 */
public final class TextViewCompat {
    private static final String LOG_TAG = "TextViewCompat";

    /**
     * The TextView does not auto-size text (default).
     */
    public static final int AUTO_SIZE_TEXT_TYPE_NONE = TextView.AUTO_SIZE_TEXT_TYPE_NONE;

    /**
     * The TextView scales text size both horizontally and vertically to fit within the
     * container.
     */
    public static final int AUTO_SIZE_TEXT_TYPE_UNIFORM = TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({AUTO_SIZE_TEXT_TYPE_NONE, AUTO_SIZE_TEXT_TYPE_UNIFORM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AutoSizeTextType {}

    private static Field sMaximumField;
    private static boolean sMaximumFieldFetched;
    private static Field sMaxModeField;
    private static boolean sMaxModeFieldFetched;

    private static Field sMinimumField;
    private static boolean sMinimumFieldFetched;
    private static Field sMinModeField;
    private static boolean sMinModeFieldFetched;

    private static final int LINES = 1;

    // Hide constructor
    private TextViewCompat() {}

    private static Field retrieveField(String fieldName) {
        Field field = null;
        try {
            field = TextView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Log.e(LOG_TAG, "Could not retrieve " + fieldName + " field.");
        }
        return field;
    }

    private static int retrieveIntFromField(Field field, TextView textView) {
        try {
            return field.getInt(textView);
        } catch (IllegalAccessException e) {
            Log.d(LOG_TAG, "Could not retrieve value of " + field.getName() + " field.");
        }
        return -1;
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use {@code null} if you do not want a Drawable
     * there. The Drawables must already have had {@link Drawable#setBounds}
     * called.
     * <p/>
     * Calling this method will overwrite any Drawables previously set using
     * {@link TextView#setCompoundDrawables} or related methods.
     *
     * @param textView The TextView against which to invoke the method.
     * @attr name android:drawableStart
     * @attr name android:drawableTop
     * @attr name android:drawableEnd
     * @attr name android:drawableBottom
     */
    public static void setCompoundDrawablesRelative(@NonNull TextView textView,
            @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
            @Nullable Drawable bottom) {
        if (Build.VERSION.SDK_INT >= 18) {
            textView.setCompoundDrawablesRelative(start, top, end, bottom);
        } else if (Build.VERSION.SDK_INT >= 17) {
            boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            textView.setCompoundDrawables(rtl ? end : start, top, rtl ? start : end, bottom);
        } else {
            textView.setCompoundDrawables(start, top, end, bottom);
        }
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use {@code null} if you do not want a Drawable
     * there. The Drawables' bounds will be set to their intrinsic bounds.
     * <p/>
     * Calling this method will overwrite any Drawables previously set using
     * {@link TextView#setCompoundDrawables} or related methods.
     *
     * @param textView The TextView against which to invoke the method.
     * @attr name android:drawableStart
     * @attr name android:drawableTop
     * @attr name android:drawableEnd
     * @attr name android:drawableBottom
     */
    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
            @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
            @Nullable Drawable bottom) {
        if (Build.VERSION.SDK_INT >= 18) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
        } else if (Build.VERSION.SDK_INT >= 17) {
            boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            textView.setCompoundDrawablesWithIntrinsicBounds(rtl ? end : start, top,
                    rtl ? start : end,  bottom);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use 0 if you do not want a Drawable there. The
     * Drawables' bounds will be set to their intrinsic bounds.
     * <p/>
     * Calling this method will overwrite any Drawables previously set using
     * {@link TextView#setCompoundDrawables} or related methods.
     *
     * @param textView The TextView against which to invoke the method.
     * @param start    Resource identifier of the start Drawable.
     * @param top      Resource identifier of the top Drawable.
     * @param end      Resource identifier of the end Drawable.
     * @param bottom   Resource identifier of the bottom Drawable.
     * @attr name android:drawableStart
     * @attr name android:drawableTop
     * @attr name android:drawableEnd
     * @attr name android:drawableBottom
     */
    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
            @DrawableRes int start, @DrawableRes int top, @DrawableRes int end,
            @DrawableRes int bottom) {
        if (Build.VERSION.SDK_INT >= 18) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
        } else if (Build.VERSION.SDK_INT >= 17) {
            boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            textView.setCompoundDrawablesWithIntrinsicBounds(rtl ? end : start, top,
                    rtl ? start : end, bottom);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }
    }

    /**
     * Returns the maximum number of lines displayed in the given TextView, or -1 if the maximum
     * height was set in pixels instead.
     */
    public static int getMaxLines(@NonNull TextView textView) {
        if (Build.VERSION.SDK_INT >= 16) {
            return textView.getMaxLines();
        }

        if (!sMaxModeFieldFetched) {
            sMaxModeField = retrieveField("mMaxMode");
            sMaxModeFieldFetched = true;
        }
        if (sMaxModeField != null && retrieveIntFromField(sMaxModeField, textView) == LINES) {
            // If the max mode is using lines, we can grab the maximum value
            if (!sMaximumFieldFetched) {
                sMaximumField = retrieveField("mMaximum");
                sMaximumFieldFetched = true;
            }
            if (sMaximumField != null) {
                return retrieveIntFromField(sMaximumField, textView);
            }
        }
        return -1;
    }

    /**
     * Returns the minimum number of lines displayed in the given TextView, or -1 if the minimum
     * height was set in pixels instead.
     */
    public static int getMinLines(@NonNull TextView textView) {
        if (Build.VERSION.SDK_INT >= 16) {
            return textView.getMinLines();
        }

        if (!sMinModeFieldFetched) {
            sMinModeField = retrieveField("mMinMode");
            sMinModeFieldFetched = true;
        }
        if (sMinModeField != null && retrieveIntFromField(sMinModeField, textView) == LINES) {
            // If the min mode is using lines, we can grab the maximum value
            if (!sMinimumFieldFetched) {
                sMinimumField = retrieveField("mMinimum");
                sMinimumFieldFetched = true;
            }
            if (sMinimumField != null) {
                return retrieveIntFromField(sMinimumField, textView);
            }
        }
        return -1;
    }

    /**
     * Sets the text appearance from the specified style resource.
     * <p>
     * Use a framework-defined {@code TextAppearance} style like
     * {@link android.R.style#TextAppearance_Material_Body1 @android:style/TextAppearance.Material.Body1}.
     *
     * @param textView The TextView against which to invoke the method.
     * @param resId    The resource identifier of the style to apply.
     */
    public static void setTextAppearance(@NonNull TextView textView, @StyleRes int resId) {
        if (Build.VERSION.SDK_INT >= 23) {
            textView.setTextAppearance(resId);
        } else {
            textView.setTextAppearance(textView.getContext(), resId);
        }
    }

    /**
     * Returns drawables for the start, top, end, and bottom borders from the given text view.
     */
    @NonNull
    public static Drawable[] getCompoundDrawablesRelative(@NonNull TextView textView) {
        if (Build.VERSION.SDK_INT >= 18) {
            return textView.getCompoundDrawablesRelative();
        }
        if (Build.VERSION.SDK_INT >= 17) {
            final boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            final Drawable[] compounds = textView.getCompoundDrawables();
            if (rtl) {
                // If we're on RTL, we need to invert the horizontal result like above
                final Drawable start = compounds[2];
                final Drawable end = compounds[0];
                compounds[0] = start;
                compounds[2] = end;
            }
            return compounds;
        }
        return textView.getCompoundDrawables();
    }

    /**
     * Specify whether this widget should automatically scale the text to try to perfectly fit
     * within the layout bounds by using the default auto-size configuration.
     *
     * @param autoSizeTextType the type of auto-size. Must be one of
     *        {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_NONE} or
     *        {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}
     *
     * @attr name android:autoSizeTextType
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static void setAutoSizeTextTypeWithDefaults(@NonNull TextView textView,
            int autoSizeTextType) {
        if (Build.VERSION.SDK_INT >= 27) {
            textView.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
        } else if (textView instanceof AutoSizeableTextView) {
            ((AutoSizeableTextView) textView).setAutoSizeTextTypeWithDefaults(autoSizeTextType);
        }
    }

    /**
     * Specify whether this widget should automatically scale the text to try to perfectly fit
     * within the layout bounds. If all the configuration params are valid the type of auto-size is
     * set to {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}.
     *
     * @param autoSizeMinTextSize the minimum text size available for auto-size
     * @param autoSizeMaxTextSize the maximum text size available for auto-size
     * @param autoSizeStepGranularity the auto-size step granularity. It is used in conjunction with
     *                                the minimum and maximum text size in order to build the set of
     *                                text sizes the system uses to choose from when auto-sizing
     * @param unit the desired dimension unit for all sizes above. See {@link TypedValue} for the
     *             possible dimension units
     *
     * @throws IllegalArgumentException if any of the configuration params are invalid.
     *
     * @attr name android:autoSizeTextType
     * @attr name android:autoSizeTextType
     * @attr name android:autoSizeMinTextSize
     * @attr name android:autoSizeMaxTextSize
     * @attr name android:autoSizeStepGranularity
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static void setAutoSizeTextTypeUniformWithConfiguration(
            @NonNull TextView textView,
            int autoSizeMinTextSize,
            int autoSizeMaxTextSize,
            int autoSizeStepGranularity,
            int unit) throws IllegalArgumentException {
        if (Build.VERSION.SDK_INT >= 27) {
            textView.setAutoSizeTextTypeUniformWithConfiguration(
                    autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
        } else if (textView instanceof AutoSizeableTextView) {
            ((AutoSizeableTextView) textView).setAutoSizeTextTypeUniformWithConfiguration(
                    autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
        }
    }

    /**
     * Specify whether this widget should automatically scale the text to try to perfectly fit
     * within the layout bounds. If at least one value from the <code>presetSizes</code> is valid
     * then the type of auto-size is set to {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}.
     *
     * @param presetSizes an {@code int} array of sizes in pixels
     * @param unit the desired dimension unit for the preset sizes above. See {@link TypedValue} for
     *             the possible dimension units
     *
     * @throws IllegalArgumentException if all of the <code>presetSizes</code> are invalid.
     *_
     * @attr name android:autoSizeTextType
     * @attr name android:autoSizePresetSizes
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static void setAutoSizeTextTypeUniformWithPresetSizes(@NonNull TextView textView,
            @NonNull int[] presetSizes, int unit) throws IllegalArgumentException {
        if (Build.VERSION.SDK_INT >= 27) {
            textView.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
        } else if (textView instanceof AutoSizeableTextView) {
            ((AutoSizeableTextView) textView).setAutoSizeTextTypeUniformWithPresetSizes(
                    presetSizes, unit);
        }
    }

    /**
     * Returns the type of auto-size set for this widget.
     *
     * @return an {@code int} corresponding to one of the auto-size types:
     *         {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_NONE} or
     *         {@link TextViewCompat#AUTO_SIZE_TEXT_TYPE_UNIFORM}
     *
     * @attr name android:autoSizeTextType
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static int getAutoSizeTextType(@NonNull TextView textView) {
        if (Build.VERSION.SDK_INT >= 27) {
            return textView.getAutoSizeTextType();
        }
        if (textView instanceof AutoSizeableTextView) {
            return ((AutoSizeableTextView) textView).getAutoSizeTextType();
        }
        return AUTO_SIZE_TEXT_TYPE_NONE;
    }

    /**
     * @return the current auto-size step granularity in pixels.
     *
     * @attr name android:autoSizeStepGranularity
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static int getAutoSizeStepGranularity(@NonNull TextView textView) {
        if (Build.VERSION.SDK_INT >= 27) {
            return textView.getAutoSizeStepGranularity();
        }
        if (textView instanceof AutoSizeableTextView) {
            return ((AutoSizeableTextView) textView).getAutoSizeStepGranularity();
        }
        return -1;
    }

    /**
     * @return the current auto-size minimum text size in pixels (the default is 12sp). Note that
     *         if auto-size has not been configured this function returns {@code -1}.
     *
     * @attr name android:autoSizeMinTextSize
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static int getAutoSizeMinTextSize(@NonNull TextView textView) {
        if (Build.VERSION.SDK_INT >= 27) {
            return textView.getAutoSizeMinTextSize();
        }
        if (textView instanceof AutoSizeableTextView) {
            return ((AutoSizeableTextView) textView).getAutoSizeMinTextSize();
        }
        return -1;
    }

    /**
     * @return the current auto-size maximum text size in pixels (the default is 112sp). Note that
     *         if auto-size has not been configured this function returns {@code -1}.
     *
     * @attr name android:autoSizeMaxTextSize
     */
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static int getAutoSizeMaxTextSize(@NonNull TextView textView) {
        if (Build.VERSION.SDK_INT >= 27) {
            return textView.getAutoSizeMaxTextSize();
        }
        if (textView instanceof AutoSizeableTextView) {
            return ((AutoSizeableTextView) textView).getAutoSizeMaxTextSize();
        }
        return -1;
    }

    /**
     * @return the current auto-size {@code int} sizes array (in pixels).
     *
     * @attr name android:autoSizePresetSizes
     */
    @NonNull
    @SuppressWarnings("RedundantCast") // Intentionally invoking interface method.
    public static int[] getAutoSizeTextAvailableSizes(@NonNull TextView textView) {
        if (Build.VERSION.SDK_INT >= 27) {
            return textView.getAutoSizeTextAvailableSizes();
        }
        if (textView instanceof AutoSizeableTextView) {
            return ((AutoSizeableTextView) textView).getAutoSizeTextAvailableSizes();
        }
        return new int[0];
    }

    /**
     * Sets a selection action mode callback on a TextView.
     *
     * Also this method can be used to fix a bug in framework SDK 26/27. On these affected devices,
     * the bug causes the menu containing the options for handling ACTION_PROCESS_TEXT after text
     * selection to miss a number of items. This method can be used to fix this wrong behaviour for
     * a text view, by passing any custom callback implementation. If no custom callback is desired,
     * a no-op implementation should be provided.
     *
     * Note that, by default, the bug will only be fixed when the default floating toolbar menu
     * implementation is used. If a custom implementation of {@link Menu} is provided, this should
     * provide the method Menu#removeItemAt(int) which removes a menu item by its position,
     * as given by Menu#getItem(int). Also, the following post condition should hold: a call
     * to removeItemAt(i), should not modify the results of getItem(j) for any j < i. Intuitively,
     * removing an element from the menu should behave as removing an element from a list.
     * Note that this method does not exist in the {@link Menu} interface. However, it is required,
     * and going to be called by reflection, in order to display the correct process text items in
     * the menu.
     *
     * @param textView The TextView to set the action selection mode callback on.
     * @param callback The action selection mode callback to set on textView.
     */
    public static void setCustomSelectionActionModeCallback(@NonNull final TextView textView,
                @NonNull final ActionMode.Callback callback) {
        textView.setCustomSelectionActionModeCallback(
                wrapCustomSelectionActionModeCallback(textView, callback));
    }

    /**
     * @see #setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public static ActionMode.Callback wrapCustomSelectionActionModeCallback(
            @NonNull final TextView textView,
            @NonNull final ActionMode.Callback callback) {
        if (Build.VERSION.SDK_INT < 26 || Build.VERSION.SDK_INT > 27
                || callback instanceof OreoCallback) {
            // If the bug does not affect the current SDK version, or if
            // the callback was already wrapped, no need to wrap it.
            return callback;
        }
        // A bug in O and O_MR1 causes a number of options for handling the ACTION_PROCESS_TEXT
        // intent after selection to not be displayed in the menu, although they should be.
        // Here we fix this, by removing the menu items created by the framework code, and
        // adding them (and the missing ones) back correctly.
        return new OreoCallback(callback, textView);
    }

    @RequiresApi(26)
    private static class OreoCallback implements ActionMode.Callback {
        // This constant should be correlated with its definition in the
        // android.widget.Editor class.
        private static final int MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START = 100;
        private final ActionMode.Callback mCallback;
        private final TextView mTextView;

        // References to the MenuBuilder class and its removeItemAt(int) method.
        // Since in most cases the menu instance processed by this callback is going
        // to be a MenuBuilder, we keep these references to avoid querying for them
        // frequently by reflection in recomputeProcessTextMenuItems.
        private Class mMenuBuilderClass;
        private Method mMenuBuilderRemoveItemAtMethod;
        private boolean mCanUseMenuBuilderReferences;
        private boolean mInitializedMenuBuilderReferences;

        OreoCallback(ActionMode.Callback callback, TextView textView) {
            mCallback = callback;
            mTextView = textView;
            mInitializedMenuBuilderReferences = false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mCallback.onCreateActionMode(mode, menu);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            recomputeProcessTextMenuItems(menu);
            return mCallback.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mCallback.onActionItemClicked(mode, item);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mCallback.onDestroyActionMode(mode);
        }

        private void recomputeProcessTextMenuItems(final Menu menu) {
            final Context context = mTextView.getContext();
            final PackageManager packageManager = context.getPackageManager();

            if (!mInitializedMenuBuilderReferences) {
                mInitializedMenuBuilderReferences = true;
                try {
                    mMenuBuilderClass =
                            Class.forName("com.android.internal.view.menu.MenuBuilder");
                    mMenuBuilderRemoveItemAtMethod = mMenuBuilderClass
                            .getDeclaredMethod("removeItemAt", Integer.TYPE);
                    mCanUseMenuBuilderReferences = true;
                } catch (ClassNotFoundException | NoSuchMethodException e) {
                    mMenuBuilderClass = null;
                    mMenuBuilderRemoveItemAtMethod = null;
                    mCanUseMenuBuilderReferences = false;
                }
            }
            // Remove the menu items created for ACTION_PROCESS_TEXT handlers.
            try {
                final Method removeItemAtMethod =
                        (mCanUseMenuBuilderReferences && mMenuBuilderClass.isInstance(menu))
                                ? mMenuBuilderRemoveItemAtMethod
                                : menu.getClass()
                                        .getDeclaredMethod("removeItemAt", Integer.TYPE);
                for (int i = menu.size() - 1; i >= 0; --i) {
                    final MenuItem item = menu.getItem(i);
                    if (item.getIntent() != null && Intent.ACTION_PROCESS_TEXT
                            .equals(item.getIntent().getAction())) {
                        removeItemAtMethod.invoke(menu, i);
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException
                    | InvocationTargetException e) {
                // There is a menu custom implementation used which is not providing
                // a removeItemAt(int) menu. There is nothing we can do in this case.
                return;
            }

            // Populate the menu again with the ACTION_PROCESS_TEXT handlers.
            final List<ResolveInfo> supportedActivities =
                    getSupportedActivities(context, packageManager);
            for (int i = 0; i < supportedActivities.size(); ++i) {
                final ResolveInfo info = supportedActivities.get(i);
                menu.add(Menu.NONE, Menu.NONE,
                        MENU_ITEM_ORDER_PROCESS_TEXT_INTENT_ACTIONS_START + i,
                        info.loadLabel(packageManager))
                        .setIntent(createProcessTextIntentForResolveInfo(info, mTextView))
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }

        private List<ResolveInfo> getSupportedActivities(final Context context,
                final PackageManager packageManager) {
            final List<ResolveInfo> supportedActivities = new ArrayList<>();
            boolean canStartActivityForResult = context instanceof Activity;
            if (!canStartActivityForResult) {
                return supportedActivities;
            }
            final List<ResolveInfo> unfiltered =
                    packageManager.queryIntentActivities(createProcessTextIntent(), 0);
            for (ResolveInfo info : unfiltered) {
                if (isSupportedActivity(info, context)) {
                    supportedActivities.add(info);
                }
            }
            return supportedActivities;
        }

        private boolean isSupportedActivity(final ResolveInfo info, final Context context) {
            if (context.getPackageName().equals(info.activityInfo.packageName)) {
                return true;
            }
            if (!info.activityInfo.exported) {
                return false;
            }
            return info.activityInfo.permission == null
                    || context.checkSelfPermission(info.activityInfo.permission)
                        == PackageManager.PERMISSION_GRANTED;
        }

        private Intent createProcessTextIntentForResolveInfo(final ResolveInfo info,
                final TextView textView11) {
            return createProcessTextIntent()
                    .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, !isEditable(textView11))
                    .setClassName(info.activityInfo.packageName, info.activityInfo.name);
        }

        private boolean isEditable(final TextView textView11) {
            return textView11 instanceof Editable
                    && textView11.onCheckIsTextEditor()
                    && textView11.isEnabled();
        }

        private Intent createProcessTextIntent() {
            return new Intent().setAction(Intent.ACTION_PROCESS_TEXT).setType("text/plain");
        }
    }

    /**
     * Updates the top padding of the TextView so that {@code firstBaselineToTopHeight} is
     * equal to the distance between the first text baseline and the top of this TextView.
     * <strong>Note</strong> that if {@code FontMetrics.top} or {@code FontMetrics.ascent} was
     * already greater than {@code firstBaselineToTopHeight}, the top padding is not updated.
     *
     * @param firstBaselineToTopHeight distance between first baseline to top of the container
     *      in pixels
     *
     * @see #getFirstBaselineToTopHeight(TextView)
     * @see TextView#setPadding(int, int, int, int)
     * @see TextView#setPaddingRelative(int, int, int, int)
     *
     * @attr name android:firstBaselineToTopHeight
     */
    public static void setFirstBaselineToTopHeight(
            @NonNull final TextView textView,
            @Px @IntRange(from = 0) final int firstBaselineToTopHeight) {
        Preconditions.checkArgumentNonnegative(firstBaselineToTopHeight);
        if (Build.VERSION.SDK_INT >= 28) {
            textView.setFirstBaselineToTopHeight(firstBaselineToTopHeight);
            return;
        }

        final Paint.FontMetricsInt fontMetrics = textView.getPaint().getFontMetricsInt();
        final int fontMetricsTop;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
                // The includeFontPadding attribute was introduced
                // in SDK16, and it is true by default.
                || textView.getIncludeFontPadding()) {
            fontMetricsTop = fontMetrics.top;
        } else {
            fontMetricsTop = fontMetrics.ascent;
        }

        // TODO: Decide if we want to ignore density ratio (i.e. when the user changes font size
        // in settings). At the moment, we don't.

        if (firstBaselineToTopHeight > Math.abs(fontMetricsTop)) {
            final int paddingTop = firstBaselineToTopHeight - (-fontMetricsTop);
            textView.setPadding(textView.getPaddingLeft(), paddingTop,
                    textView.getPaddingRight(), textView.getPaddingBottom());
        }
    }

    /**
     * Updates the bottom padding of the TextView so that {@code lastBaselineToBottomHeight} is
     * equal to the distance between the last text baseline and the bottom of this TextView.
     * <strong>Note</strong> that if {@code FontMetrics.bottom} or {@code FontMetrics.descent} was
     * already greater than {@code lastBaselineToBottomHeight}, the bottom padding is not updated.
     *
     * @param lastBaselineToBottomHeight distance between last baseline to bottom of the container
     *      in pixels
     *
     * @see #getLastBaselineToBottomHeight(TextView)
     * @see TextView#setPadding(int, int, int, int)
     * @see TextView#setPaddingRelative(int, int, int, int)
     *
     * @attr name android:lastBaselineToBottomHeight
     */
    public static void setLastBaselineToBottomHeight(
            @NonNull final TextView textView,
            @Px @IntRange(from = 0) int lastBaselineToBottomHeight) {
        Preconditions.checkArgumentNonnegative(lastBaselineToBottomHeight);

        final Paint.FontMetricsInt fontMetrics = textView.getPaint().getFontMetricsInt();
        final int fontMetricsBottom;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
                // The includeFontPadding attribute was introduced
                // in SDK16, and it is true by default.
                || textView.getIncludeFontPadding()) {
            fontMetricsBottom = fontMetrics.bottom;
        } else {
            fontMetricsBottom = fontMetrics.descent;
        }

        // TODO: Decide if we want to ignore density ratio (i.e. when the user changes font size
        // in settings). At the moment, we don't.

        if (lastBaselineToBottomHeight > Math.abs(fontMetricsBottom)) {
            final int paddingBottom = lastBaselineToBottomHeight - fontMetricsBottom;
            textView.setPadding(textView.getPaddingLeft(), textView.getPaddingTop(),
                    textView.getPaddingRight(), paddingBottom);
        }
    }

    /**
     * Returns the distance between the first text baseline and the top of this TextView.
     *
     * @see #setFirstBaselineToTopHeight(TextView, int)
     * @attr name android:firstBaselineToTopHeight
     */
    public static int getFirstBaselineToTopHeight(@NonNull final TextView textView) {
        return textView.getPaddingTop() - textView.getPaint().getFontMetricsInt().top;
    }

    /**
     * Returns the distance between the last text baseline and the bottom of this TextView.
     *
     * @see #setLastBaselineToBottomHeight(TextView, int)
     * @attr name android:lastBaselineToBottomHeight
     */
    public static int getLastBaselineToBottomHeight(@NonNull final TextView textView) {
        return textView.getPaddingBottom() + textView.getPaint().getFontMetricsInt().bottom;
    }


    /**
     * Sets an explicit line height for this TextView. This is equivalent to the vertical distance
     * between subsequent baselines in the TextView.
     *
     * @param lineHeight the line height in pixels
     *
     * @see TextView#setLineSpacing(float, float)
     * @see TextView#getLineSpacingExtra()
     * @see TextView#getLineSpacingMultiplier()
     *
     * @attr name android:lineHeight
     */
    public static void setLineHeight(@NonNull final TextView textView,
                              @Px @IntRange(from = 0) int lineHeight) {
        Preconditions.checkArgumentNonnegative(lineHeight);

        final int fontHeight = textView.getPaint().getFontMetricsInt(null);
        // Make sure we don't setLineSpacing if it's not needed to avoid unnecessary redraw.
        if (lineHeight != fontHeight) {
            // Set lineSpacingExtra by the difference of lineSpacing with lineHeight
            textView.setLineSpacing(lineHeight - fontHeight, 1f);
        }
    }

    /**
     * Gets the parameters for text layout precomputation, for use with
     * {@link PrecomputedTextCompat}.
     *
     * @return a current {@link PrecomputedTextCompat.Params}
     * @see PrecomputedTextCompat
     */
    public static @NonNull PrecomputedTextCompat.Params getTextMetricsParams(
            @NonNull final TextView textView) {
        if (Build.VERSION.SDK_INT >= 28) {
            return new PrecomputedTextCompat.Params(textView.getTextMetricsParams());
        } else {
            PrecomputedTextCompat.Params.Builder builder =
                    new PrecomputedTextCompat.Params.Builder(new TextPaint(textView.getPaint()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.setBreakStrategy(textView.getBreakStrategy());
                builder.setHyphenationFrequency(textView.getHyphenationFrequency());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                builder.setTextDirection(getTextDirectionHeuristic(textView));
            }
            return builder.build();
        }
    }

    /**
     * Apply the text layout parameter.
     *
     * Update the TextView parameters to be compatible with {@link PrecomputedTextCompat.Params}.
     * @see PrecomputedTextCompat
     */
    public static void setTextMetricsParams(@NonNull TextView textView,
            @NonNull PrecomputedTextCompat.Params params) {

        // There is no way of setting text direction heuristics to TextView.
        // Convert to the View's text direction int values.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            textView.setTextDirection(getTextDirection(params.getTextDirection()));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            float paintTextScaleX = params.getTextPaint().getTextScaleX();

            // This is not a recommended way but there is no API to set paint to text view.
            textView.getPaint().set(params.getTextPaint());
            // On API 22 or before, doing following trick to invalidate internal layout objects.

            if (paintTextScaleX == textView.getTextScaleX()) {
                // Set the different value of the scaleX so that the following setTextScaleX will
                // trigger new layout request.
                textView.setTextScaleX(paintTextScaleX / 2.0f + 1.0f);
            }
            textView.setTextScaleX(paintTextScaleX);

        } else {  // API 23 or later
            // This is not a recommended way but there is no API to set paint to text view.
            textView.getPaint().set(params.getTextPaint());
            // getPaint().set() doesn't invalidaate the internal layout objects.
            // On API 23 or later, setBreakStrategy/setHyphenationFrequency invalidates internal
            // layout objects.
            textView.setBreakStrategy(params.getBreakStrategy());
            textView.setHyphenationFrequency(params.getHyphenationFrequency());
        }
    }

    /**
     * Sets the PrecomputedTextCompat to the TextView
     *
     * If the given PrecomputeTextCompat is not compatible with textView, throws an
     * IllegalArgumentException.
     *
     * @param textView the TextView
     * @param precomputed the precomputed text
     * @throws IllegalArgumentException if precomputed text is not compatible with textView.
     */
    public static void setPrecomputedText(@NonNull TextView textView,
                                          @NonNull PrecomputedTextCompat precomputed) {

        if (BuildCompat.isAtLeastQ()) {
            // Framework can not understand PrecomptedTextCompat. Pass underlying PrecomputedText.
            // Parameter check is also done by framework.
            textView.setText(precomputed.getPrecomputedText());
        } else {
            PrecomputedTextCompat.Params param = TextViewCompat.getTextMetricsParams(textView);
            if (!param.equalsWithoutTextDirection(precomputed.getParams())) {
                throw new IllegalArgumentException("Given text can not be applied to TextView.");
            }
            textView.setText(precomputed);
        }
    }

    /**
     * Returns the current {@link TextDirectionHeuristic}.
     *
     * This method is copy of TextView.getTextDirectionHeuristic() in framework.
     * TODO: Make TextView.getTextDirectionHeuristic() in framework public API.
     *
     * @return the current {@link TextDirectionHeuristic}.
     */
    @RequiresApi(18)
    private static TextDirectionHeuristic getTextDirectionHeuristic(@NonNull TextView textView) {
        if (textView.getTransformationMethod() instanceof PasswordTransformationMethod) {
            // passwords fields should be LTR
            return TextDirectionHeuristics.LTR;
        }

        if (Build.VERSION.SDK_INT >= 28) {
            if ((textView.getInputType() & EditorInfo.TYPE_MASK_CLASS)
                    == EditorInfo.TYPE_CLASS_PHONE) {
                // Phone numbers must be in the direction of the locale's digits. Most locales
                // have LTR digits, but some locales, such as those written in the Adlam or N'Ko
                // scripts, have RTL digits.
                final DecimalFormatSymbols symbols =
                        DecimalFormatSymbols.getInstance(textView.getTextLocale());
                final String zero = symbols.getDigitStrings()[0];
                // In case the zero digit is multi-codepoint, just use the first codepoint to
                // determine direction.
                final int firstCodepoint = zero.codePointAt(0);
                final byte digitDirection = Character.getDirectionality(firstCodepoint);
                if (digitDirection == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                        || digitDirection == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                    return TextDirectionHeuristics.RTL;
                } else {
                    return TextDirectionHeuristics.LTR;
                }
            }
        }

        // Always need to resolve layout direction first
        final boolean defaultIsRtl = (textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);

        // Now, we can select the heuristic
        switch (textView.getTextDirection()) {
            default:
            case TEXT_DIRECTION_FIRST_STRONG:
                return (defaultIsRtl ? TextDirectionHeuristics.FIRSTSTRONG_RTL :
                        TextDirectionHeuristics.FIRSTSTRONG_LTR);
            case TEXT_DIRECTION_ANY_RTL:
                return TextDirectionHeuristics.ANYRTL_LTR;
            case TEXT_DIRECTION_LTR:
                return TextDirectionHeuristics.LTR;
            case TEXT_DIRECTION_RTL:
                return TextDirectionHeuristics.RTL;
            case TEXT_DIRECTION_LOCALE:
                return TextDirectionHeuristics.LOCALE;
            case TEXT_DIRECTION_FIRST_STRONG_LTR:
                return TextDirectionHeuristics.FIRSTSTRONG_LTR;
            case TEXT_DIRECTION_FIRST_STRONG_RTL:
                return TextDirectionHeuristics.FIRSTSTRONG_RTL;
        }
    }

    /**
     * Convert TextDirectionHeuristic to TextDirection int values
     */
    @RequiresApi(18)
    private static int getTextDirection(@NonNull  TextDirectionHeuristic heuristic) {
        if (heuristic == TextDirectionHeuristics.FIRSTSTRONG_RTL) {
            return TEXT_DIRECTION_FIRST_STRONG;
        } else if (heuristic == TextDirectionHeuristics.FIRSTSTRONG_LTR) {
            return TEXT_DIRECTION_FIRST_STRONG;
        } else if (heuristic == TextDirectionHeuristics.ANYRTL_LTR) {
            return TEXT_DIRECTION_ANY_RTL;
        } else if (heuristic == TextDirectionHeuristics.LTR) {
            return TEXT_DIRECTION_LTR;
        } else if (heuristic == TextDirectionHeuristics.RTL) {
            return TEXT_DIRECTION_RTL;
        } else if (heuristic == TextDirectionHeuristics.LOCALE) {
            return TEXT_DIRECTION_LOCALE;
        } else if (heuristic == TextDirectionHeuristics.FIRSTSTRONG_LTR) {
            return TEXT_DIRECTION_FIRST_STRONG_LTR;
        } else if (heuristic == TextDirectionHeuristics.FIRSTSTRONG_RTL) {
            return TEXT_DIRECTION_FIRST_STRONG_RTL;
        } else {
            return TEXT_DIRECTION_FIRST_STRONG;
        }
    }
}
