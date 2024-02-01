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

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.ViewGroup;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Compatibility utilities for platform features of {@link ViewGroup}.
 */
class ViewGroupUtils {

    /**
     * False when linking of the hidden suppressLayout method has previously failed.
     */
    private static boolean sTryHiddenSuppressLayout = true;

    private static Method sGetChildDrawingOrderMethod;
    private static boolean sGetChildDrawingOrderMethodFetched;

    /**
     * Provides access to the hidden ViewGroup#suppressLayout method.
     */
    static void suppressLayout(@NonNull ViewGroup group, boolean suppress) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.suppressLayout(group, suppress);
        } else {
            hiddenSuppressLayout(group, suppress);
        }
    }

    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    private static void hiddenSuppressLayout(@NonNull ViewGroup group, boolean suppress) {
        if (sTryHiddenSuppressLayout) {
            // Since this was an @hide method made public, we can link directly against it with
            // a try/catch for its absence instead of doing the same through reflection.
            try {
                Api29Impl.suppressLayout(group, suppress);
            } catch (NoSuchMethodError e) {
                sTryHiddenSuppressLayout = false;
            }
        }
    }

    /**
     * Returns the index of the child to draw for this iteration.
     */
    static int getChildDrawingOrder(@NonNull ViewGroup viewGroup, int i) {
        if (Build.VERSION.SDK_INT >= 29) {
            return Api29Impl.getChildDrawingOrder(viewGroup, i);
        } else {
            if (!sGetChildDrawingOrderMethodFetched) {
                try {
                    sGetChildDrawingOrderMethod = ViewGroup.class.getDeclaredMethod(
                            "getChildDrawingOrder", int.class, int.class);
                    sGetChildDrawingOrderMethod.setAccessible(true);
                } catch (NoSuchMethodException ignore) {

                }
                sGetChildDrawingOrderMethodFetched = true;
            }
            if (sGetChildDrawingOrderMethod != null) {
                try {
                    return (Integer) sGetChildDrawingOrderMethod.invoke(viewGroup,
                            viewGroup.getChildCount(), i);
                } catch (IllegalAccessException ignore) {
                } catch (InvocationTargetException ignore) {
                }
            }
            // fallback implementation
            return i;
        }
    }


    private ViewGroupUtils() { }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void suppressLayout(ViewGroup viewGroup, boolean suppress) {
            viewGroup.suppressLayout(suppress);
        }

        @DoNotInline
        static int getChildDrawingOrder(ViewGroup viewGroup, int drawingPosition) {
            return viewGroup.getChildDrawingOrder(drawingPosition);
        }
    }
}
