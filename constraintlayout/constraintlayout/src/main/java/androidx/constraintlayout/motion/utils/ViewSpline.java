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

import android.os.Build;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import androidx.constraintlayout.core.motion.utils.CurveFit;
import androidx.constraintlayout.core.motion.utils.SplineSet;
import androidx.constraintlayout.motion.widget.Key;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.ConstraintAttribute;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class ViewSpline extends SplineSet {
    private static final String TAG = "ViewSpline";


    // @TODO: add description

    /**
     * @param str
     * @param attrList
     * @return
     */
    public static ViewSpline makeCustomSpline(String str,
                                              SparseArray<ConstraintAttribute> attrList) {
        return new CustomSet(str, attrList);
    }

    // @TODO: add description

    /**
     * @param str
     * @return
     */
    public static ViewSpline makeSpline(String str) {
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
            case Key.PIVOT_X:
                return new PivotXset();
            case Key.PIVOT_Y:
                return new PivotYset();
            case Key.TRANSITION_PATH_ROTATE:
                return new PathRotate();
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

    /**
     * the main interface to setting a view property
     * @param view the view
     * @param t the point of time
     */
    public abstract void setProperty(View view, float t);

    static class ElevationSet extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setElevation(get(t));
            }
        }
    }

    static class AlphaSet extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setAlpha(get(t));
        }
    }

    static class RotationSet extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setRotation(get(t));
        }
    }

    static class RotationXset extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setRotationX(get(t));
        }
    }

    static class RotationYset extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setRotationY(get(t));
        }
    }

    static class PivotXset extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setPivotX(get(t));
        }
    }

    static class PivotYset extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setPivotY(get(t));
        }
    }

    public static class PathRotate extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
        }

        /**
         * Use to set the rotation relative to the path
         * @param view the view to set
         * @param t the time point
         * @param dx the path velocity in x
         * @param dy the path velocity in y
         */
        public void setPathRotate(View view, float t, double dx, double dy) {
            view.setRotation(get(t) + (float) Math.toDegrees(Math.atan2(dy, dx)));
        }
    }

    static class ScaleXset extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setScaleX(get(t));
        }
    }

    static class ScaleYset extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setScaleY(get(t));
        }
    }

    static class TranslationXset extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setTranslationX(get(t));
        }
    }

    static class TranslationYset extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            view.setTranslationY(get(t));
        }
    }

    static class TranslationZset extends ViewSpline {
        @Override
        public void setProperty(View view, float t) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setTranslationZ(get(t));
            }
        }
    }

    public static class CustomSet extends ViewSpline {
        String mAttributeName;
        SparseArray<ConstraintAttribute> mConstraintAttributeList;
        float[] mTempValues;

        public CustomSet(String attribute, SparseArray<ConstraintAttribute> attrList) {
            mAttributeName = attribute.split(",")[1];
            mConstraintAttributeList = attrList;
        }

        // @TODO: add description

        /**
         * @param curveType
         */
        @Override
        public void setup(int curveType) {
            int size = mConstraintAttributeList.size();
            int dimensionality =
                    mConstraintAttributeList.valueAt(0).numberOfInterpolatedValues();
            double[] time = new double[size];
            mTempValues = new float[dimensionality];
            double[][] values = new double[size][dimensionality];
            for (int i = 0; i < size; i++) {

                int key = mConstraintAttributeList.keyAt(i);
                ConstraintAttribute ca = mConstraintAttributeList.valueAt(i);

                time[i] = key * 1E-2;
                ca.getValuesToInterpolate(mTempValues);
                for (int k = 0; k < mTempValues.length; k++) {
                    values[i][k] = mTempValues[k];
                }

            }
            mCurveFit = CurveFit.get(curveType, time, values);
        }

        /**
         * this call will throw RuntimeException
         * @param position the position
         * @param value the value
         */
        @Override
        public void setPoint(int position, float value) {
            throw new RuntimeException("call of custom attribute setPoint");
        }

        /**
         * set the CustomAttribute
         * @param position
         * @param value
         */
        public void setPoint(int position, ConstraintAttribute value) {
            mConstraintAttributeList.append(position, value);
        }

        @Override
        public void setProperty(View view, float t) {
            mCurveFit.getPos(t, mTempValues);
            CustomSupport.setInterpolatedValue(mConstraintAttributeList.valueAt(0),
                    view, mTempValues);
        }
    }

    static class ProgressSet extends ViewSpline {
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
