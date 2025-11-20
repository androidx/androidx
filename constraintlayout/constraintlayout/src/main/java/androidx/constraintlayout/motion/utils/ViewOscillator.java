/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.util.Log;
import android.view.View;

import androidx.constraintlayout.core.motion.utils.KeyCycleOscillator;
import androidx.constraintlayout.motion.widget.Key;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.ConstraintAttribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provide the engine for executing cycles.
 * KeyCycleOscillator
 *
 *
 */
public abstract class ViewOscillator extends KeyCycleOscillator {
    private static final String TAG = "ViewOscillator";

    /**
     * Set the property of that view
     * @param view
     * @param t
     */
    public abstract void setProperty(View view, float t);

    /**
     * Create a spline that manipulates a specific property of a view
     * @param str the property to manipulate
     * @return
     */
    public static ViewOscillator makeSpline(String str) {
        if (str.startsWith(Key.CUSTOM)) {
            return new CustomSet();
        }
        switch (str) {
            case Key.ALPHA:
                return new AlphaSet();
            case Key.ELEVATION:
                return new ElevationSet();
            case Key.ROTATION:
                return new RotationSet();
            case Key.ROTATION_X:
                return new RotationXset();
            case Key.ROTATION_Y:
                return new RotationYset();
            case Key.TRANSITION_PATH_ROTATE:
                return new PathRotateSet();
            case Key.SCALE_X:
                return new ScaleXset();
            case Key.SCALE_Y:
                return new ScaleYset();
            case Key.WAVE_OFFSET:
                return new AlphaSet();
            case Key.WAVE_VARIES_BY:
                return new AlphaSet();
            case Key.TRANSLATION_X:
                return new TranslationXset();
            case Key.TRANSLATION_Y:
                return new TranslationYset();
            case Key.TRANSLATION_Z:
                return new TranslationZset();
            case Key.PROGRESS:
                return new ProgressSet();
            default:
                return null;
        }
    }

    static class ElevationSet extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setElevation(get(t));
        }
    }

    static class AlphaSet extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setAlpha(get(t));
        }
    }

    static class RotationSet extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setRotation(get(t));
        }
    }

    static class RotationXset extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setRotationX(get(t));
        }
    }

    static class RotationYset extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setRotationY(get(t));
        }
    }

    public static class PathRotateSet extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
        }

        /**
         *  use to modify the rotation relative to the current path
         * @param view the view to modify
         * @param t the point in time to manipulate
         * @param dx of the path
         * @param dy of the path
         */
        public void setPathRotate(View view, float t, double dx, double dy) {
            view.setRotation(get(t) + (float) Math.toDegrees(Math.atan2(dy, dx)));
        }
    }

    static class ScaleXset extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setScaleX(get(t));
        }
    }

    static class ScaleYset extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setScaleY(get(t));
        }
    }

    static class TranslationXset extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setTranslationX(get(t));
        }
    }

    static class TranslationYset extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setTranslationY(get(t));
        }
    }

    static class TranslationZset extends ViewOscillator {
        @Override
        public void setProperty(View view, float t) {
            view.setTranslationZ(get(t));
        }
    }

    static class CustomSet extends ViewOscillator {
        float[] mValue = new float[1];
        protected ConstraintAttribute mCustom;

        @Override
        protected void setCustom(Object custom) {
            mCustom = (ConstraintAttribute) custom;
        }

        @Override
        public void setProperty(View view, float t) {
            mValue[0] = get(t);
            CustomSupport.setInterpolatedValue(mCustom, view, mValue);
        }
    }

    static class ProgressSet extends ViewOscillator {
        boolean mNoMethod = false;

        @Override
        public void setProperty(View view, float t) {
            if (view instanceof MotionLayout) {
                ((MotionLayout) view).setProgress(get(t));
            } else {
                if (mNoMethod) {
                    return;
                }
                Method method = null;
                try {
                    method = view.getClass().getMethod("setProgress", Float.TYPE);
                } catch (NoSuchMethodException e) {
                    mNoMethod = true;
                }
                if (method != null) {
                    try {
                        method.invoke(view, get(t));
                    } catch (IllegalAccessException e) {
                        Log.e(TAG, "unable to setProgress", e);
                    } catch (InvocationTargetException e) {
                        Log.e(TAG, "unable to setProgress", e);
                    }
                }
            }
        }
    }

}
