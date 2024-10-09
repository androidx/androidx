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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class ViewUtils {
    private static final String TAG = "ViewUtils";

    private static boolean sInitComputeFitSystemWindowsMethod;
    private static Method sComputeFitSystemWindowsMethod;

    /**
     */
    @RestrictTo(LIBRARY)
    @ChecksSdkIntAtLeast(api = 27)
    static final boolean SDK_LEVEL_SUPPORTS_AUTOSIZE = Build.VERSION.SDK_INT >= 27;

    private ViewUtils() {}

    public static boolean isLayoutRtl(View view) {
        return view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Allow calling the hidden method {@code computeFitSystemWindows(Rect, Rect)} through
     * reflection on {@code view}.
     */
    public static void computeFitSystemWindows(@NonNull View view, @NonNull Rect inoutInsets,
            @NonNull Rect outLocalInsets) {
        if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.computeFitSystemWindows(view, inoutInsets, outLocalInsets);
        } else {
            if (!sInitComputeFitSystemWindowsMethod) {
                sInitComputeFitSystemWindowsMethod = true;
                try {
                    sComputeFitSystemWindowsMethod = View.class.getDeclaredMethod(
                            "computeFitSystemWindows", Rect.class, Rect.class);
                    if (!sComputeFitSystemWindowsMethod.isAccessible()) {
                        sComputeFitSystemWindowsMethod.setAccessible(true);
                    }
                } catch (NoSuchMethodException e) {
                    Log.d(TAG, "Could not find method computeFitSystemWindows. Oh well.");
                }
            }

            if (sComputeFitSystemWindowsMethod != null) {
                try {
                    sComputeFitSystemWindowsMethod.invoke(view, inoutInsets, outLocalInsets);
                } catch (Exception e) {
                    Log.d(TAG, "Could not invoke computeFitSystemWindows", e);
                }
            }
        }
    }

    /**
     * Allow calling the hidden method {@code makeOptionalFitsSystem()} through reflection on
     * {@code view}.
     */
    @SuppressLint("BanUncheckedReflection")
    public static void makeOptionalFitsSystemWindows(View view) {
        try {
            // We need to use getMethod() for makeOptionalFitsSystemWindows since both View
            // and ViewGroup implement the method
            Method method = view.getClass().getMethod("makeOptionalFitsSystemWindows");
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            method.invoke(view);
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "Could not find method makeOptionalFitsSystemWindows. Oh well...");
        } catch (InvocationTargetException e) {
            Log.d(TAG, "Could not invoke makeOptionalFitsSystemWindows", e);
        } catch (IllegalAccessException e) {
            Log.d(TAG, "Could not invoke makeOptionalFitsSystemWindows", e);
        }
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        static void computeFitSystemWindows(@NonNull View view, @NonNull Rect inoutInsets,
                @NonNull Rect outLocalInsets) {
            WindowInsets in =
                    new WindowInsets.Builder()
                            .setSystemWindowInsets(Insets.of(inoutInsets))
                            .build();
            WindowInsets innerInsets = view.computeSystemWindowInsets(in, outLocalInsets);
            Insets systemWindowInsets = innerInsets.getSystemWindowInsets();
            inoutInsets.set(systemWindowInsets.left, systemWindowInsets.top,
                    systemWindowInsets.right, systemWindowInsets.bottom);
        }
    }
}
