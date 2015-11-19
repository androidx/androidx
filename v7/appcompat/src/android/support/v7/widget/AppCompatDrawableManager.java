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

package android.support.v7.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.LruCache;
import android.support.v7.appcompat.R;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.WeakHashMap;

import static android.support.v7.widget.ThemeUtils.getDisabledThemeAttrColor;
import static android.support.v7.widget.ThemeUtils.getThemeAttrColor;
import static android.support.v7.widget.ThemeUtils.getThemeAttrColorStateList;

/**
 * @hide
 */
public final class AppCompatDrawableManager {

    public interface InflateDelegate {
        /**
         * Allows custom inflation of a drawable resource.
         *
         * @param context Context to inflate/create with
         * @param resId Resource ID of the drawable
         * @return the created drawable, or {@code null} to leave inflation to
         * AppCompatDrawableManager.
         */
        @Nullable
        Drawable onInflateDrawable(@NonNull Context context, @DrawableRes int resId);
    }

    private static final String TAG = "TintManager";
    private static final boolean DEBUG = false;
    private static final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;

    private static AppCompatDrawableManager INSTANCE;

    public static AppCompatDrawableManager get() {
        if (INSTANCE == null) {
            INSTANCE = new AppCompatDrawableManager();
        }
        return INSTANCE;
    }

    private static final ColorFilterLruCache COLOR_FILTER_CACHE = new ColorFilterLruCache(6);

    /**
     * Drawables which should be tinted with the value of {@code R.attr.colorControlNormal},
     * using the default mode using a raw color filter.
     */
    private static final int[] COLORFILTER_TINT_COLOR_CONTROL_NORMAL = {
            R.drawable.abc_textfield_search_default_mtrl_alpha,
            R.drawable.abc_textfield_default_mtrl_alpha,
            R.drawable.abc_ab_share_pack_mtrl_alpha
    };

    /**
     * Drawables which should be tinted with the value of {@code R.attr.colorControlNormal}, using
     * {@link DrawableCompat}'s tinting functionality.
     */
    private static final int[] TINT_COLOR_CONTROL_NORMAL = {
            R.drawable.abc_ic_ab_back_mtrl_am_alpha,
            R.drawable.abc_ic_go_search_api_mtrl_alpha,
            R.drawable.abc_ic_search_api_mtrl_alpha,
            R.drawable.abc_ic_commit_search_api_mtrl_alpha,
            R.drawable.abc_ic_clear_mtrl_alpha,
            R.drawable.abc_ic_menu_share_mtrl_alpha,
            R.drawable.abc_ic_menu_copy_mtrl_am_alpha,
            R.drawable.abc_ic_menu_cut_mtrl_alpha,
            R.drawable.abc_ic_menu_selectall_mtrl_alpha,
            R.drawable.abc_ic_menu_paste_mtrl_am_alpha,
            R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha,
            R.drawable.abc_ic_voice_search_api_mtrl_alpha
    };

    /**
     * Drawables which should be tinted with the value of {@code R.attr.colorControlActivated},
     * using a color filter.
     */
    private static final int[] COLORFILTER_COLOR_CONTROL_ACTIVATED = {
            R.drawable.abc_textfield_activated_mtrl_alpha,
            R.drawable.abc_textfield_search_activated_mtrl_alpha,
            R.drawable.abc_cab_background_top_mtrl_alpha,
            R.drawable.abc_text_cursor_material
    };

    /**
     * Drawables which should be tinted with the value of {@code android.R.attr.colorBackground},
     * using the {@link android.graphics.PorterDuff.Mode#MULTIPLY} mode and a color filter.
     */
    private static final int[] COLORFILTER_COLOR_BACKGROUND_MULTIPLY = {
            R.drawable.abc_popup_background_mtrl_mult,
            R.drawable.abc_cab_background_internal_bg,
            R.drawable.abc_menu_hardkey_panel_mtrl_mult
    };

    /**
     * Drawables which should be tinted using a state list containing values of
     * {@code R.attr.colorControlNormal} and {@code R.attr.colorControlActivated}
     */
    private static final int[] TINT_COLOR_CONTROL_STATE_LIST = {
            R.drawable.abc_edit_text_material,
            R.drawable.abc_tab_indicator_material,
            R.drawable.abc_textfield_search_material,
            R.drawable.abc_spinner_mtrl_am_alpha,
            R.drawable.abc_spinner_textfield_background_material,
            R.drawable.abc_ratingbar_full_material,
            R.drawable.abc_switch_track_mtrl_alpha,
            R.drawable.abc_switch_thumb_material,
            R.drawable.abc_btn_default_mtrl_shape,
            R.drawable.abc_btn_borderless_material
    };

    /**
     * Drawables which should be tinted using a state list containing values of
     * {@code R.attr.colorControlNormal} and {@code R.attr.colorControlActivated} for the checked
     * state.
     */
    private static final int[] TINT_CHECKABLE_BUTTON_LIST = {
            R.drawable.abc_btn_check_material,
            R.drawable.abc_btn_radio_material
    };

    private WeakHashMap<Context, SparseArray<ColorStateList>> mTintLists;
    private ArrayList<InflateDelegate> mDelegates;

    public Drawable getDrawable(@NonNull Context context, @DrawableRes int resId) {
        return getDrawable(context, resId, false);
    }

    public Drawable getDrawable(@NonNull Context context, @DrawableRes int resId,
            boolean failIfNotKnown) {
        // Let the InflateDelegates have a go first
        if (mDelegates != null) {
            for (int i = 0, count = mDelegates.size(); i < count; i++) {
                final InflateDelegate delegate = mDelegates.get(i);
                final Drawable result = delegate.onInflateDrawable(context, resId);
                if (result != null) {
                    return result;
                }
            }
        }

        // The delegates failed so we'll carry on
        Drawable drawable = ContextCompat.getDrawable(context, resId);

        if (drawable != null) {
            if (Build.VERSION.SDK_INT >= 8) {
                // Mutate can cause NPEs on 2.1
                drawable = drawable.mutate();
            }

            final ColorStateList tintList = getTintList(context, resId);
            if (tintList != null) {
                // First wrap the Drawable and set the tint list
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTintList(drawable, tintList);

                // If there is a blending mode specified for the drawable, use it
                final PorterDuff.Mode tintMode = getTintMode(resId);
                if (tintMode != null) {
                    DrawableCompat.setTintMode(drawable, tintMode);
                }
            } else if (resId == R.drawable.abc_cab_background_top_material) {
                return new LayerDrawable(new Drawable[]{
                        getDrawable(context, R.drawable.abc_cab_background_internal_bg),
                        getDrawable(context, R.drawable.abc_cab_background_top_mtrl_alpha)
                });
            } else if (resId == R.drawable.abc_seekbar_track_material) {
                LayerDrawable ld = (LayerDrawable) drawable;
                setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.background),
                        getThemeAttrColor(context, R.attr.colorControlNormal), DEFAULT_MODE);
                setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.secondaryProgress),
                        getThemeAttrColor(context, R.attr.colorControlNormal), DEFAULT_MODE);
                setPorterDuffColorFilter(ld.findDrawableByLayerId(android.R.id.progress),
                        getThemeAttrColor(context, R.attr.colorControlActivated), DEFAULT_MODE);
            } else {
                final boolean tinted = tintDrawableUsingColorFilter(context, resId, drawable);
                if (!tinted && failIfNotKnown) {
                    // If we didn't tint using a ColorFilter, and we're set to fail if we don't
                    // know the id, return null
                    drawable = null;
                }
            }
        }
        return drawable;
    }

    public final boolean tintDrawableUsingColorFilter(@NonNull Context context,
            @DrawableRes final int resId, @NonNull Drawable drawable) {
        PorterDuff.Mode tintMode = DEFAULT_MODE;
        boolean colorAttrSet = false;
        int colorAttr = 0;
        int alpha = -1;

        if (arrayContains(COLORFILTER_TINT_COLOR_CONTROL_NORMAL, resId)) {
            colorAttr = R.attr.colorControlNormal;
            colorAttrSet = true;
        } else if (arrayContains(COLORFILTER_COLOR_CONTROL_ACTIVATED, resId)) {
            colorAttr = R.attr.colorControlActivated;
            colorAttrSet = true;
        } else if (arrayContains(COLORFILTER_COLOR_BACKGROUND_MULTIPLY, resId)) {
            colorAttr = android.R.attr.colorBackground;
            colorAttrSet = true;
            tintMode = PorterDuff.Mode.MULTIPLY;
        } else if (resId == R.drawable.abc_list_divider_mtrl_alpha) {
            colorAttr = android.R.attr.colorForeground;
            colorAttrSet = true;
            alpha = Math.round(0.16f * 255);
        }

        if (colorAttrSet) {
            final int color = getThemeAttrColor(context, colorAttr);
            drawable.setColorFilter(getPorterDuffColorFilter(color, tintMode));

            if (alpha != -1) {
                drawable.setAlpha(alpha);
            }

            if (DEBUG) {
                Log.d(TAG, "Tinted Drawable: " + context.getResources().getResourceName(resId) +
                        " with color: #" + Integer.toHexString(color));
            }
            return true;
        }
        return false;
    }

    public void addDelegate(@NonNull InflateDelegate delegate) {
        if (mDelegates == null) {
            mDelegates = new ArrayList<>();
        }
        if (!mDelegates.contains(delegate)) {
            mDelegates.add(delegate);
        }
    }

    public void removeDelegate(@NonNull InflateDelegate delegate) {
        if (mDelegates != null) {
            mDelegates.remove(delegate);
        }
    }

    private static boolean arrayContains(int[] array, int value) {
        for (int id : array) {
            if (id == value) {
                return true;
            }
        }
        return false;
    }

    final PorterDuff.Mode getTintMode(final int resId) {
        PorterDuff.Mode mode = null;

        if (resId == R.drawable.abc_switch_thumb_material) {
            mode = PorterDuff.Mode.MULTIPLY;
        }

        return mode;
    }

    public final ColorStateList getTintList(@NonNull Context context, @DrawableRes int resId) {
        // Try the cache first (if it exists)
        ColorStateList tint = getTintListFromCache(context, resId);

        if (tint == null) {
            // ...if the cache did not contain a color state list, try and create one
            if (resId == R.drawable.abc_edit_text_material) {
                tint = createEditTextColorStateList(context);
            } else if (resId == R.drawable.abc_switch_track_mtrl_alpha) {
                tint = createSwitchTrackColorStateList(context);
            } else if (resId == R.drawable.abc_switch_thumb_material) {
                tint = createSwitchThumbColorStateList(context);
            } else if (resId == R.drawable.abc_btn_default_mtrl_shape
                    || resId == R.drawable.abc_btn_borderless_material) {
                tint = createDefaultButtonColorStateList(context);
            } else if (resId == R.drawable.abc_btn_colored_material) {
                tint = createColoredButtonColorStateList(context);
            } else if (resId == R.drawable.abc_spinner_mtrl_am_alpha
                    || resId == R.drawable.abc_spinner_textfield_background_material) {
                tint = createSpinnerColorStateList(context);
            } else if (arrayContains(TINT_COLOR_CONTROL_NORMAL, resId)) {
                tint = getThemeAttrColorStateList(context, R.attr.colorControlNormal);
            } else if (arrayContains(TINT_COLOR_CONTROL_STATE_LIST, resId)) {
                tint = createDefaultColorStateList(context);
            } else if (arrayContains(TINT_CHECKABLE_BUTTON_LIST, resId)) {
                tint = createCheckableButtonColorStateList(context);
            } else if (resId == R.drawable.abc_seekbar_thumb_material) {
                tint = createSeekbarThumbColorStateList(context);
            }

            if (tint != null) {
                addTintListToCache(context, resId, tint);
            }
        }
        return tint;
    }

    private ColorStateList getTintListFromCache(@NonNull Context context, @DrawableRes int resId) {
        if (mTintLists != null) {
            final SparseArray<ColorStateList> tints = mTintLists.get(context);
            return tints != null ? tints.get(resId) : null;
        }
        return null;
    }

    private void addTintListToCache(@NonNull Context context, @DrawableRes int resId,
            @NonNull ColorStateList tintList) {
        if (mTintLists == null) {
            mTintLists = new WeakHashMap<>();
        }
        SparseArray<ColorStateList> themeTints = mTintLists.get(context);
        if (themeTints == null) {
            themeTints = new SparseArray<>();
            mTintLists.put(context, themeTints);
        }
        themeTints.append(resId, tintList);
    }

    private ColorStateList createDefaultColorStateList(Context context) {
        /**
         * Generate the default color state list which uses the colorControl attributes.
         * Order is important here. The default enabled state needs to go at the bottom.
         */

        final int colorControlNormal = getThemeAttrColor(context, R.attr.colorControlNormal);
        final int colorControlActivated = getThemeAttrColor(context, R.attr.colorControlActivated);

        final int[][] states = new int[7][];
        final int[] colors = new int[7];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = ThemeUtils.FOCUSED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;

        states[i] = ThemeUtils.ACTIVATED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;

        states[i] = ThemeUtils.PRESSED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;

        states[i] = ThemeUtils.CHECKED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;

        states[i] = ThemeUtils.SELECTED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;

        // Default enabled state
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = colorControlNormal;
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createCheckableButtonColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = ThemeUtils.CHECKED_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        // Default enabled state
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createSwitchTrackColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getThemeAttrColor(context, android.R.attr.colorForeground, 0.1f);
        i++;

        states[i] = ThemeUtils.CHECKED_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated, 0.3f);
        i++;

        // Default enabled state
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = getThemeAttrColor(context, android.R.attr.colorForeground, 0.3f);
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createSwitchThumbColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        final ColorStateList thumbColor = getThemeAttrColorStateList(context,
                R.attr.colorSwitchThumbNormal);

        if (thumbColor != null && thumbColor.isStateful()) {
            // If colorSwitchThumbNormal is a valid ColorStateList, extract the default and
            // disabled colors from it

            // Disabled state
            states[i] = ThemeUtils.DISABLED_STATE_SET;
            colors[i] = thumbColor.getColorForState(states[i], 0);
            i++;

            states[i] = ThemeUtils.CHECKED_STATE_SET;
            colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
            i++;

            // Default enabled state
            states[i] = ThemeUtils.EMPTY_STATE_SET;
            colors[i] = thumbColor.getDefaultColor();
            i++;
        } else {
            // Else we'll use an approximation using the default disabled alpha

            // Disabled state
            states[i] = ThemeUtils.DISABLED_STATE_SET;
            colors[i] = getDisabledThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
            i++;

            states[i] = ThemeUtils.CHECKED_STATE_SET;
            colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
            i++;

            // Default enabled state
            states[i] = ThemeUtils.EMPTY_STATE_SET;
            colors[i] = getThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
            i++;
        }

        return new ColorStateList(states, colors);
    }

    private ColorStateList createEditTextColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = ThemeUtils.NOT_PRESSED_OR_FOCUSED_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        // Default enabled state
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createDefaultButtonColorStateList(Context context) {
        return createButtonColorStateList(context, R.attr.colorButtonNormal);
    }

    private ColorStateList createColoredButtonColorStateList(Context context) {
        return createButtonColorStateList(context, R.attr.colorAccent);
    }

    private ColorStateList createButtonColorStateList(Context context, int baseColorAttr) {
        final int[][] states = new int[4][];
        final int[] colors = new int[4];
        int i = 0;

        final int baseColor = getThemeAttrColor(context, baseColorAttr);
        final int colorControlHighlight = getThemeAttrColor(context, R.attr.colorControlHighlight);

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorButtonNormal);
        i++;

        states[i] = ThemeUtils.PRESSED_STATE_SET;
        colors[i] = ColorUtils.compositeColors(colorControlHighlight, baseColor);
        i++;

        states[i] = ThemeUtils.FOCUSED_STATE_SET;
        colors[i] = ColorUtils.compositeColors(colorControlHighlight, baseColor);
        i++;

        // Default enabled state
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = baseColor;
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createSpinnerColorStateList(Context context) {
        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = ThemeUtils.NOT_PRESSED_OR_FOCUSED_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlNormal);
        i++;

        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        return new ColorStateList(states, colors);
    }

    private ColorStateList createSeekbarThumbColorStateList(Context context) {
        final int[][] states = new int[2][];
        final int[] colors = new int[2];
        int i = 0;

        // Disabled state
        states[i] = ThemeUtils.DISABLED_STATE_SET;
        colors[i] = getDisabledThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
        i++;

        return new ColorStateList(states, colors);
    }

    private static class ColorFilterLruCache extends LruCache<Integer, PorterDuffColorFilter> {

        public ColorFilterLruCache(int maxSize) {
            super(maxSize);
        }

        PorterDuffColorFilter get(int color, PorterDuff.Mode mode) {
            return get(generateCacheKey(color, mode));
        }

        PorterDuffColorFilter put(int color, PorterDuff.Mode mode, PorterDuffColorFilter filter) {
            return put(generateCacheKey(color, mode), filter);
        }

        private static int generateCacheKey(int color, PorterDuff.Mode mode) {
            int hashCode = 1;
            hashCode = 31 * hashCode + color;
            hashCode = 31 * hashCode + mode.hashCode();
            return hashCode;
        }
    }

    public static void tintDrawable(Drawable drawable, TintInfo tint, int[] state) {
        if (shouldMutateBackground(drawable) && drawable.mutate() != drawable) {
            Log.d(TAG, "Mutated drawable is not the same instance as the input.");
            return;
        }

        if (tint.mHasTintList || tint.mHasTintMode) {
            drawable.setColorFilter(createTintFilter(
                    tint.mHasTintList ? tint.mTintList : null,
                    tint.mHasTintMode ? tint.mTintMode : DEFAULT_MODE,
                    state));
        } else {
            drawable.clearColorFilter();
        }

        if (Build.VERSION.SDK_INT <= 23) {
            // Pre-v23 there is no guarantee that a state change will invoke an invalidation,
            // so we force it ourselves
            drawable.invalidateSelf();
        }
    }

    private static boolean shouldMutateBackground(Drawable drawable) {
        if (drawable instanceof LayerDrawable) {
            return Build.VERSION.SDK_INT >= 16;
        } else if (drawable instanceof InsetDrawable) {
            return Build.VERSION.SDK_INT >= 14;
        } else if (drawable instanceof StateListDrawable) {
            // StateListDrawable has a bug in mutate() on API 7
            return Build.VERSION.SDK_INT >= 8;
        } else if (drawable instanceof DrawableContainer) {
            // If we have a DrawableContainer, let's traverse it's child array
            final Drawable.ConstantState state = drawable.getConstantState();
            if (state instanceof DrawableContainer.DrawableContainerState) {
                final DrawableContainer.DrawableContainerState containerState =
                        (DrawableContainer.DrawableContainerState) state;
                for (Drawable child : containerState.getChildren()) {
                    if (!shouldMutateBackground(child)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static PorterDuffColorFilter createTintFilter(ColorStateList tint,
            PorterDuff.Mode tintMode, final int[] state) {
        if (tint == null || tintMode == null) {
            return null;
        }
        final int color = tint.getColorForState(state, Color.TRANSPARENT);
        return getPorterDuffColorFilter(color, tintMode);
    }

    public static PorterDuffColorFilter getPorterDuffColorFilter(int color, PorterDuff.Mode mode) {
        // First, lets see if the cache already contains the color filter
        PorterDuffColorFilter filter = COLOR_FILTER_CACHE.get(color, mode);

        if (filter == null) {
            // Cache miss, so create a color filter and add it to the cache
            filter = new PorterDuffColorFilter(color, mode);
            COLOR_FILTER_CACHE.put(color, mode, filter);
        }

        return filter;
    }

    private static void setPorterDuffColorFilter(Drawable d, int color, PorterDuff.Mode mode) {
        d.setColorFilter(getPorterDuffColorFilter(color, mode == null ? DEFAULT_MODE : mode));
    }
}
