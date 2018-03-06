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

package android.support.transition;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RequiresApi(19)
class ViewUtilsApi19 extends ViewUtilsApi18 {

    private static final String TAG = "ViewUtilsApi19";

    private static Method sSetTransitionAlphaMethod;
    private static boolean sSetTransitionAlphaMethodFetched;
    private static Method sGetTransitionAlphaMethod;
    private static boolean sGetTransitionAlphaMethodFetched;

    @Override
    public void setTransitionAlpha(@NonNull View view, float alpha) {
        fetchSetTransitionAlphaMethod();
        if (sSetTransitionAlphaMethod != null) {
            try {
                sSetTransitionAlphaMethod.invoke(view, alpha);
            } catch (IllegalAccessException e) {
                // Do nothing
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        } else {
            view.setAlpha(alpha);
        }
    }

    @Override
    public float getTransitionAlpha(@NonNull View view) {
        fetchGetTransitionAlphaMethod();
        if (sGetTransitionAlphaMethod != null) {
            try {
                return (Float) sGetTransitionAlphaMethod.invoke(view);
            } catch (IllegalAccessException e) {
                // Do nothing
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }
        return super.getTransitionAlpha(view);
    }

    @Override
    public void saveNonTransitionAlpha(@NonNull View view) {
        // Do nothing
    }

    @Override
    public void clearNonTransitionAlpha(@NonNull View view) {
        // Do nothing
    }

    private void fetchSetTransitionAlphaMethod() {
        if (!sSetTransitionAlphaMethodFetched) {
            try {
                sSetTransitionAlphaMethod = View.class.getDeclaredMethod("setTransitionAlpha",
                        float.class);
                sSetTransitionAlphaMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "Failed to retrieve setTransitionAlpha method", e);
            }
            sSetTransitionAlphaMethodFetched = true;
        }
    }

    private void fetchGetTransitionAlphaMethod() {
        if (!sGetTransitionAlphaMethodFetched) {
            try {
                sGetTransitionAlphaMethod = View.class.getDeclaredMethod("getTransitionAlpha");
                sGetTransitionAlphaMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "Failed to retrieve getTransitionAlpha method", e);
            }
            sGetTransitionAlphaMethodFetched = true;
        }
    }

}
