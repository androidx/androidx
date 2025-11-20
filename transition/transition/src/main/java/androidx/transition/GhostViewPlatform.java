/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.graphics.Matrix;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class GhostViewPlatform implements GhostView {

    private static final String TAG = "GhostViewApi21";

    private static Class<?> sGhostViewClass;
    private static boolean sGhostViewClassFetched;
    private static Method sAddGhostMethod;
    private static boolean sAddGhostMethodFetched;
    private static Method sRemoveGhostMethod;
    private static boolean sRemoveGhostMethodFetched;

    @SuppressLint("BanUncheckedReflection") // This method is called only on API 28
    static GhostView addGhost(View view, ViewGroup viewGroup, Matrix matrix) {
        fetchAddGhostMethod();
        if (sAddGhostMethod != null) {
            try {
                return new GhostViewPlatform(
                        (View) sAddGhostMethod.invoke(null, view, viewGroup, matrix));
            } catch (IllegalAccessException e) {
                // Do nothing
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }
        return null;
    }

    @SuppressLint("BanUncheckedReflection") // This method is called only on API 28
    static void removeGhost(View view) {
        fetchRemoveGhostMethod();
        if (sRemoveGhostMethod != null) {
            try {
                sRemoveGhostMethod.invoke(null, view);
            } catch (IllegalAccessException e) {
                // Do nothing
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    /** A handle to the platform android.view.GhostView. */
    private final View mGhostView;

    private GhostViewPlatform(@NonNull View ghostView) {
        mGhostView = ghostView;
    }

    @Override
    public void setVisibility(int visibility) {
        mGhostView.setVisibility(visibility);
    }

    @Override
    public void reserveEndViewTransition(ViewGroup viewGroup, View view) {
        // No need
    }

    private static void fetchGhostViewClass() {
        if (!sGhostViewClassFetched) {
            try {
                sGhostViewClass = Class.forName("android.view.GhostView");
            } catch (ClassNotFoundException e) {
                Log.i(TAG, "Failed to retrieve GhostView class", e);
            }
            sGhostViewClassFetched = true;
        }
    }

    private static void fetchAddGhostMethod() {
        if (!sAddGhostMethodFetched) {
            try {
                fetchGhostViewClass();
                sAddGhostMethod = sGhostViewClass.getDeclaredMethod("addGhost", View.class,
                        ViewGroup.class, Matrix.class);
                sAddGhostMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "Failed to retrieve addGhost method", e);
            }
            sAddGhostMethodFetched = true;
        }
    }

    private static void fetchRemoveGhostMethod() {
        if (!sRemoveGhostMethodFetched) {
            try {
                fetchGhostViewClass();
                sRemoveGhostMethod = sGhostViewClass.getDeclaredMethod("removeGhost", View.class);
                sRemoveGhostMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "Failed to retrieve removeGhost method", e);
            }
            sRemoveGhostMethodFetched = true;
        }
    }

}
