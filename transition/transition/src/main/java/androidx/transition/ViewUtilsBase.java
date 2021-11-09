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
import android.graphics.Matrix;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ViewUtilsBase {

    private static final String TAG = "ViewUtilsBase";

    private static Method sSetFrameMethod;
    private static boolean sSetFrameFetched;

    private static Field sViewFlagsField;
    private static boolean sViewFlagsFieldFetched;
    private static final int VISIBILITY_MASK = 0x0000000C;

    private float[] mMatrixValues;

    public void setTransitionAlpha(@NonNull View view, float alpha) {
        Float savedAlpha = (Float) view.getTag(R.id.save_non_transition_alpha);
        if (savedAlpha != null) {
            view.setAlpha(savedAlpha * alpha);
        } else {
            view.setAlpha(alpha);
        }
    }

    public float getTransitionAlpha(@NonNull View view) {
        Float savedAlpha = (Float) view.getTag(R.id.save_non_transition_alpha);
        if (savedAlpha != null) {
            return view.getAlpha() / savedAlpha;
        } else {
            return view.getAlpha();
        }
    }

    public void saveNonTransitionAlpha(@NonNull View view) {
        if (view.getTag(R.id.save_non_transition_alpha) == null) {
            view.setTag(R.id.save_non_transition_alpha, view.getAlpha());
        }
    }

    public void clearNonTransitionAlpha(@NonNull View view) {
        // We don't clear the saved value when the view is hidden; that's the situation we are
        // saving this value for.
        if (view.getVisibility() == View.VISIBLE) {
            view.setTag(R.id.save_non_transition_alpha, null);
        }
    }

    public void transformMatrixToGlobal(@NonNull View view, @NonNull Matrix matrix) {
        final ViewParent parent = view.getParent();
        if (parent instanceof View) {
            final View vp = (View) parent;
            transformMatrixToGlobal(vp, matrix);
            matrix.preTranslate(-vp.getScrollX(), -vp.getScrollY());
        }
        matrix.preTranslate(view.getLeft(), view.getTop());
        final Matrix vm = view.getMatrix();
        if (!vm.isIdentity()) {
            matrix.preConcat(vm);
        }
    }

    public void transformMatrixToLocal(@NonNull View view, @NonNull Matrix matrix) {
        final ViewParent parent = view.getParent();
        if (parent instanceof View) {
            final View vp = (View) parent;
            transformMatrixToLocal(vp, matrix);
            matrix.postTranslate(vp.getScrollX(), vp.getScrollY());
        }
        matrix.postTranslate(-view.getLeft(), -view.getTop());
        final Matrix vm = view.getMatrix();
        if (!vm.isIdentity()) {
            final Matrix inverted = new Matrix();
            if (vm.invert(inverted)) {
                matrix.postConcat(inverted);
            }
        }
    }

    public void setAnimationMatrix(@NonNull View view, @Nullable Matrix matrix) {
        if (matrix == null || matrix.isIdentity()) {
            view.setPivotX(view.getWidth() / 2);
            view.setPivotY(view.getHeight() / 2);
            view.setTranslationX(0);
            view.setTranslationY(0);
            view.setScaleX(1);
            view.setScaleY(1);
            view.setRotation(0);
        } else {
            float[] values = mMatrixValues;
            if (values == null) {
                mMatrixValues = values = new float[9];
            }
            matrix.getValues(values);
            final float sin = values[Matrix.MSKEW_Y];
            final float cos = (float) Math.sqrt(1 - sin * sin)
                    * (values[Matrix.MSCALE_X] < 0 ? -1 : 1);
            final float rotation = (float) Math.toDegrees(Math.atan2(sin, cos));
            final float scaleX = values[Matrix.MSCALE_X] / cos;
            final float scaleY = values[Matrix.MSCALE_Y] / cos;
            final float dx = values[Matrix.MTRANS_X];
            final float dy = values[Matrix.MTRANS_Y];
            view.setPivotX(0);
            view.setPivotY(0);
            view.setTranslationX(dx);
            view.setTranslationY(dy);
            view.setRotation(rotation);
            view.setScaleX(scaleX);
            view.setScaleY(scaleY);
        }
    }

    public void setLeftTopRightBottom(@NonNull View v, int left, int top, int right, int bottom) {
        fetchSetFrame();
        if (sSetFrameMethod != null) {
            try {
                sSetFrameMethod.invoke(v, left, top, right, bottom);
            } catch (IllegalAccessException e) {
                // Do nothing
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @SuppressLint("SoonBlockedPrivateApi") // Only called on API <23
    public void setTransitionVisibility(@NonNull View view, int visibility) {
        if (!sViewFlagsFieldFetched) {
            try {
                sViewFlagsField = View.class.getDeclaredField("mViewFlags");
                sViewFlagsField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.i(TAG, "fetchViewFlagsField: ");
            }
            sViewFlagsFieldFetched = true;
        }
        if (sViewFlagsField != null) {
            try {
                int viewFlags = sViewFlagsField.getInt(view);
                sViewFlagsField.setInt(view, (viewFlags & ~VISIBILITY_MASK) | visibility);
            } catch (IllegalAccessException e) {
                // Do nothing
            }
        }
    }

    /**
     * Note, this is only called on API 18 and older.
     */
    @SuppressLint({"PrivateApi", "SoonBlockedPrivateApi"})
    private void fetchSetFrame() {
        if (!sSetFrameFetched) {
            try {
                sSetFrameMethod = View.class.getDeclaredMethod("setFrame",
                        int.class, int.class, int.class, int.class);
                sSetFrameMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Log.i(TAG, "Failed to retrieve setFrame method", e);
            }
            sSetFrameFetched = true;
        }
    }

}
