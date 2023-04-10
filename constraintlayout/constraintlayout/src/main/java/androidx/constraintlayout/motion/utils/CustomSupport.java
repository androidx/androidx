/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.motion.utils;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.constraintlayout.motion.widget.Debug;
import androidx.constraintlayout.widget.ConstraintAttribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomSupport {
    private static final String TAG = "CustomSupport";
    private static final boolean DEBUG = false;
    /**
     * sets the interpolated value
     * @param att
     * @param view
     * @param value
     */
    public static void setInterpolatedValue(ConstraintAttribute att, View view, float[] value) {
        Class<? extends View> viewClass = view.getClass();

        String methodName = "set" + att.getName();
        try {
            Method method;
            switch (att.getType()) {
                case INT_TYPE:
                    method = viewClass.getMethod(methodName, Integer.TYPE);
                    method.invoke(view, (int) value[0]);
                    break;
                case FLOAT_TYPE:
                    method = viewClass.getMethod(methodName, Float.TYPE);
                    method.invoke(view, value[0]);
                    break;
                case COLOR_DRAWABLE_TYPE: {
                    method = viewClass.getMethod(methodName, Drawable.class);
                    int r = clamp((int) ((float) Math.pow(value[0], 1.0 / 2.2) * 255.0f));
                    int g = clamp((int) ((float) Math.pow(value[1], 1.0 / 2.2) * 255.0f));
                    int b = clamp((int) ((float) Math.pow(value[2], 1.0 / 2.2) * 255.0f));
                    int a = clamp((int) (value[3] * 255.0f));
                    int color = a << 24 | (r << 16) | (g << 8) | b;
                    ColorDrawable drawable = new ColorDrawable(); // TODO cache
                    drawable.setColor(color);
                    method.invoke(view, drawable);
                }
                break;
                case COLOR_TYPE:
                    method = viewClass.getMethod(methodName, Integer.TYPE);
                    int r = clamp((int) ((float) Math.pow(value[0], 1.0 / 2.2) * 255.0f));
                    int g = clamp((int) ((float) Math.pow(value[1], 1.0 / 2.2) * 255.0f));
                    int b = clamp((int) ((float) Math.pow(value[2], 1.0 / 2.2) * 255.0f));
                    int a = clamp((int) (value[3] * 255.0f));
                    int color = a << 24 | (r << 16) | (g << 8) | b;
                    method.invoke(view, color);
                    break;
                case STRING_TYPE:
                    throw new RuntimeException("unable to interpolate strings " + att.getName());

                case BOOLEAN_TYPE:
                    method = viewClass.getMethod(methodName, Boolean.TYPE);
                    method.invoke(view, value[0] > 0.5f);
                    break;
                case DIMENSION_TYPE:
                    method = viewClass.getMethod(methodName, Float.TYPE);
                    method.invoke(view, value[0]);
                    break;
                default:
                    if (DEBUG) {
                        Log.v(TAG, att.getType().toString());
                    }
            }
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "No method " + methodName + " on View \"" + Debug.getName(view) + "\"", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Cannot access method " + methodName + " on View \""
                    + Debug.getName(view) + "\"", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Cannot invoke method " + methodName + " on View \""
                    + Debug.getName(view) + "\"", e);
        }
    }

    private static int clamp(int c) {
        int n = 255;
        c &= ~(c >> 31);
        c -= n;
        c &= (c >> 31);
        c += n;
        return c;
    }
}
