/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.transition;

import android.animation.LayoutTransition;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ViewGroupUtilsApi14 {

    private static final String TAG = "ViewGroupUtilsApi14";

    private static final int LAYOUT_TRANSITION_CHANGING = 4;

    private static LayoutTransition sEmptyLayoutTransition;

    private static Field sLayoutSuppressedField;
    private static boolean sLayoutSuppressedFieldFetched;

    private static Method sCancelMethod;
    private static boolean sCancelMethodFetched;

    static void suppressLayout(@NonNull ViewGroup group, boolean suppress) {
        // Prepare the dummy LayoutTransition
        if (sEmptyLayoutTransition == null) {
            sEmptyLayoutTransition = new LayoutTransition() {
                @Override
                public boolean isChangingLayout() {
                    return true;
                }
            };
            sEmptyLayoutTransition.setAnimator(LayoutTransition.APPEARING, null);
            sEmptyLayoutTransition.setAnimator(LayoutTransition.CHANGE_APPEARING, null);
            sEmptyLayoutTransition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, null);
            sEmptyLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, null);
            sEmptyLayoutTransition.setAnimator(LAYOUT_TRANSITION_CHANGING, null);
        }
        if (suppress) {
            // Save the current LayoutTransition
            final LayoutTransition layoutTransition = group.getLayoutTransition();
            if (layoutTransition != null) {
                if (layoutTransition.isRunning()) {
                    cancelLayoutTransition(layoutTransition);
                }
                if (layoutTransition != sEmptyLayoutTransition) {
                    group.setTag(R.id.transition_layout_save, layoutTransition);
                }
            }
            // Suppress the layout
            group.setLayoutTransition(sEmptyLayoutTransition);
        } else {
            // Thaw the layout suppression
            group.setLayoutTransition(null);
            // Request layout if necessary
            if (!sLayoutSuppressedFieldFetched) {
                try {
                    sLayoutSuppressedField = ViewGroup.class.getDeclaredField("mLayoutSuppressed");
                    sLayoutSuppressedField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    Log.i(TAG, "Failed to access mLayoutSuppressed field by reflection");
                }
                sLayoutSuppressedFieldFetched = true;
            }
            boolean layoutSuppressed = false;
            if (sLayoutSuppressedField != null) {
                try {
                    layoutSuppressed = sLayoutSuppressedField.getBoolean(group);
                    if (layoutSuppressed) {
                        sLayoutSuppressedField.setBoolean(group, false);
                    }
                } catch (IllegalAccessException e) {
                    Log.i(TAG, "Failed to get mLayoutSuppressed field by reflection");
                }
            }
            if (layoutSuppressed) {
                group.requestLayout();
            }
            // Restore the saved LayoutTransition
            final LayoutTransition layoutTransition =
                    (LayoutTransition) group.getTag(R.id.transition_layout_save);
            if (layoutTransition != null) {
                group.setTag(R.id.transition_layout_save, null);
                group.setLayoutTransition(layoutTransition);
            }
        }
    }

    private static void cancelLayoutTransition(LayoutTransition t) {
        if (!sCancelMethodFetched) {
            try {
                sCancelMethod = LayoutTransition.class.getDeclaredMethod("cancel");
                sCancelMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "Failed to access cancel method by reflection");
            }
            sCancelMethodFetched = true;
        }
        if (sCancelMethod != null) {
            try {
                sCancelMethod.invoke(t);
            } catch (IllegalAccessException e) {
                Log.i(TAG, "Failed to access cancel method by reflection");
            } catch (InvocationTargetException e) {
                Log.i(TAG, "Failed to invoke cancel method by reflection");
            }
        }
    }

    private ViewGroupUtilsApi14() {
    }
}
