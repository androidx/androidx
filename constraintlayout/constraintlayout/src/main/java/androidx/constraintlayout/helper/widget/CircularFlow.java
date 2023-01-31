/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.helper.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.R;
import androidx.constraintlayout.widget.VirtualLayout;

import java.util.Arrays;

/**
 *
 * CircularFlow virtual layout.
 *
 * Allows positioning of referenced widgets circular.
 *
 * The elements referenced are indicated via constraint_referenced_ids, as with other
 * ConstraintHelper implementations.
 *
 * XML attributes that are needed:
 * <ul>
 *     <li>constraint_referenced_ids = "view2, view3, view4,view5,view6".
 *     It receives id's of the views that will add the references.</li>
 *     <li>circularflow_viewCenter = "view1". It receives the id of the view of the center where
 *     the views received in constraint_referenced_ids will be referenced.</li>
 *     <li>circularflow_angles = "45,90,135,180,225". Receive the angles that you
 *     will assign to each view.</li>
 *     <li>circularflow_radiusInDP = "90,100,110,120,130". Receive the radios in DP that you
 *     will assign to each view.</li>
 * </ul>
 *
 * Example in XML:
 * <androidx.constraintlayout.helper.widget.CircularFlow
 *         android:id="@+id/circularFlow"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"
 *         app:circularflow_angles="0,40,80,120"
 *         app:circularflow_radiusInDP="90,100,110,120"
 *         app:circularflow_viewCenter="@+id/view1"
 *         app:constraint_referenced_ids="view2,view3,view4,view5" />
 *
 * DEFAULT radius - If you add a view and don't set its radius, the default value will be 0.
 * DEFAULT angles - If you add a view and don't set its angle, the default value will be 0.
 *
 * Recommendation - always set radius and angle for all views in <i>constraint_referenced_ids</i>
 *
 **/

public class CircularFlow extends VirtualLayout {
    private static final String TAG = "CircularFlow";
    ConstraintLayout mContainer;
    int mViewCenter;
    private static int sDefaultRadius = 0;
    private static float sDefaultAngle = 0F;
    /**
     *
     */
    private float[] mAngles;

    /**
     *
     */
    private int[] mRadius;

    /**
     *
     */
    private int mCountRadius;

    /**
     *
     */
    private int mCountAngle;

    /**
     *
     */
    private String mReferenceAngles;

    /**
     *
     */
    private String mReferenceRadius;

    /**
     *
     */
    private Float mReferenceDefaultAngle;

    /**
     *
     */
    private Integer mReferenceDefaultRadius;


    public CircularFlow(Context context) {
        super(context);
    }

    public CircularFlow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircularFlow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int[] getRadius() {
        return Arrays.copyOf(mRadius, mCountRadius);
    }


    public float[] getAngles() {
        return Arrays.copyOf(mAngles, mCountAngle);
    }


    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.ConstraintLayout_Layout);
            final int n = a.getIndexCount();

            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_circularflow_viewCenter) {
                    mViewCenter = a.getResourceId(attr, 0);
                } else if (attr == R.styleable.ConstraintLayout_Layout_circularflow_angles) {
                    mReferenceAngles = a.getString(attr);
                    setAngles(mReferenceAngles);
                } else if (attr == R.styleable.ConstraintLayout_Layout_circularflow_radiusInDP) {
                    mReferenceRadius = a.getString(attr);
                    setRadius(mReferenceRadius);
                } else if (attr == R.styleable.ConstraintLayout_Layout_circularflow_defaultAngle) {
                    mReferenceDefaultAngle = a.getFloat(attr, sDefaultAngle);
                    setDefaultAngle(mReferenceDefaultAngle);
                } else if (attr == R.styleable.ConstraintLayout_Layout_circularflow_defaultRadius) {
                    mReferenceDefaultRadius = a.getDimensionPixelSize(attr, sDefaultRadius);
                    setDefaultRadius(mReferenceDefaultRadius);
                }
            }
            a.recycle();
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mReferenceAngles != null) {
            mAngles = new float[1];
            setAngles(mReferenceAngles);
        }
        if (mReferenceRadius != null) {
            mRadius = new int[1];
            setRadius(mReferenceRadius);
        }
        if (mReferenceDefaultAngle != null) {
            setDefaultAngle(mReferenceDefaultAngle);
        }
        if (mReferenceDefaultRadius != null) {
            setDefaultRadius(mReferenceDefaultRadius);
        }
        anchorReferences();
    }

    private void anchorReferences() {
        mContainer = (ConstraintLayout) getParent();
        for (int i = 0; i < mCount; i++) {
            View view = mContainer.getViewById(mIds[i]);
            if (view == null) {
                continue;
            }
            int radius = sDefaultRadius;
            float angle = sDefaultAngle;

            if (mRadius != null && i < mRadius.length) {
                radius = mRadius[i];
            } else if (mReferenceDefaultRadius != null && mReferenceDefaultRadius != -1) {
                mCountRadius++;
                if (mRadius == null) {
                    mRadius = new int[1];
                }
                mRadius = getRadius();
                mRadius[mCountRadius - 1] = radius;
            } else {
                Log.e("CircularFlow", "Added radius to view with id: " + mMap.get(view.getId()));
            }

            if (mAngles != null && i < mAngles.length) {
                angle = mAngles[i];
            } else if (mReferenceDefaultAngle != null && mReferenceDefaultAngle != -1) {
                mCountAngle++;
                if (mAngles == null) {
                    mAngles = new float[1];
                }
                mAngles = getAngles();
                mAngles[mCountAngle - 1] = angle;
            } else {
                Log.e("CircularFlow",
                        "Added angle to view with id: " + mMap.get(view.getId()));
            }
            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) view.getLayoutParams();
            params.circleAngle = angle;
            params.circleConstraint = mViewCenter;
            params.circleRadius = radius;
            view.setLayoutParams(params);
        }
        applyLayoutFeatures();
    }

    /**
     * Add a view to the CircularFlow.
     * The referenced view need to be a child of the container parent.
     * The view also need to have its id set in order to be added.
     * The views previous need to have its radius and angle set in order
     * to be added correctly a new view.
     * @param view
     * @param radius
     * @param angle
     * @return
     */
    public void addViewToCircularFlow(View view, int radius, float angle) {
        if (containsId(view.getId())) {
            return;
        }
        addView(view);
        mCountAngle++;
        mAngles = getAngles();
        mAngles[mCountAngle - 1] = angle;
        mCountRadius++;
        mRadius = getRadius();
        mRadius[mCountRadius - 1] =
                (int) (radius * myContext.getResources().getDisplayMetrics().density);
        anchorReferences();
    }

    /**
     * Update radius from a view in CircularFlow.
     * The referenced view need to be a child of the container parent.
     * The view also need to have its id set in order to be added.
     * @param view
     * @param radius
     * @return
     */
    public void updateRadius(View view, int radius) {
        if (!isUpdatable(view)) {
            Log.e("CircularFlow",
                    "It was not possible to update radius to view with id: " + view.getId());
            return;
        }
        int indexView = indexFromId(view.getId());
        if (indexView > mRadius.length) {
            return;
        }
        mRadius = getRadius();
        mRadius[indexView] = (int) (radius * myContext.getResources().getDisplayMetrics().density);
        anchorReferences();
    }

    /**
     * Update angle from a view in CircularFlow.
     * The referenced view need to be a child of the container parent.
     * The view also need to have its id set in order to be added.
     * @param view
     * @param angle
     * @return
     */
    public void updateAngle(View view, float angle) {
        if (!isUpdatable(view)) {
            Log.e("CircularFlow",
                    "It was not possible to update angle to view with id: " + view.getId());
            return;
        }
        int indexView = indexFromId(view.getId());
        if (indexView > mAngles.length) {
            return;
        }
        mAngles = getAngles();
        mAngles[indexView] = angle;
        anchorReferences();
    }

    /**
     * Update angle and radius from a view in CircularFlow.
     * The referenced view need to be a child of the container parent.
     * The view also need to have its id set in order to be added.
     * @param view
     * @param radius
     * @param angle
     * @return
     */
    public void updateReference(View view, int radius, float angle) {
        if (!isUpdatable(view)) {
            Log.e("CircularFlow",
                    "It was not possible to update radius and angle to view with id: "
                            + view.getId());
            return;
        }
        int indexView = indexFromId(view.getId());
        if (getAngles().length  > indexView) {
            mAngles = getAngles();
            mAngles[indexView] = angle;
        }
        if (getRadius().length  > indexView) {
            mRadius = getRadius();
            mRadius[indexView] =
                    (int) (radius * myContext.getResources().getDisplayMetrics().density);
        }
        anchorReferences();
    }

    /**
     * Set default Angle for CircularFlow.
     *
     * @param angle
     * @return
     */
    public void setDefaultAngle(float angle) {
        sDefaultAngle = angle;
    }

    /**
     * Set default Radius for CircularFlow.
     *
     * @param radius
     * @return
     */
    public void setDefaultRadius(int radius) {
        sDefaultRadius = radius;
    }

    @Override
    public int removeView(View view) {
        int index = super.removeView(view);
        if (index == -1) {
            return index;
        }
        ConstraintSet c = new ConstraintSet();
        c.clone(mContainer);
        c.clear(view.getId(), ConstraintSet.CIRCLE_REFERENCE);
        c.applyTo(mContainer);

        if (index < mAngles.length) {
            mAngles = removeAngle(mAngles, index);
            mCountAngle--;
        }
        if (index < mRadius.length) {
            mRadius = removeRadius(mRadius, index);
            mCountRadius--;
        }
        anchorReferences();
        return index;
    }

    /**
     *
     */
    private float[] removeAngle(float[] angles, int index) {
        if (angles == null
                || index < 0
                || index >= mCountAngle) {
            return angles;
        }

        return removeElementFromArray(angles, index);
    }

    /**
     *
     */
    private int[] removeRadius(int[] radius, int index) {
        if (radius == null
                || index < 0
                || index >= mCountRadius) {
            return radius;
        }

        return removeElementFromArray(radius, index);
    }

    /**
     *
     */
    private void setAngles(String idList) {
        if (idList == null) {
            return;
        }
        int begin = 0;
        mCountAngle = 0;
        while (true) {
            int end = idList.indexOf(',', begin);
            if (end == -1) {
                addAngle(idList.substring(begin).trim());
                break;
            }
            addAngle(idList.substring(begin, end).trim());
            begin = end + 1;
        }
    }

    /**
     *
     */
    private void setRadius(String idList) {
        if (idList == null) {
            return;
        }
        int begin = 0;
        mCountRadius = 0;
        while (true) {
            int end = idList.indexOf(',', begin);
            if (end == -1) {
                addRadius(idList.substring(begin).trim());
                break;
            }
            addRadius(idList.substring(begin, end).trim());
            begin = end + 1;
        }
    }

    /**
     *
     */
    private void addAngle(String angleString) {
        if (angleString == null || angleString.length() == 0) {
            return;
        }
        if (myContext == null) {
            return;
        }
        if (mAngles == null) {
            return;
        }

        if (mCountAngle + 1 > mAngles.length) {
            mAngles = Arrays.copyOf(mAngles, mAngles.length + 1);
        }
        mAngles[mCountAngle] = Integer.parseInt(angleString);
        mCountAngle++;
    }

    /**
     *
     */
    private void addRadius(String radiusString) {
        if (radiusString == null || radiusString.length() == 0) {
            return;
        }
        if (myContext == null) {
            return;
        }
        if (mRadius == null) {
            return;
        }

        if (mCountRadius + 1 > mRadius.length) {
            mRadius = Arrays.copyOf(mRadius, mRadius.length + 1);
        }

        mRadius[mCountRadius] = (int) (Integer.parseInt(radiusString)
                * myContext.getResources().getDisplayMetrics().density);
        mCountRadius++;
    }

    private static int[] removeElementFromArray(int[] array, int index) {
        int[] newArray = new int[array.length - 1];

        for (int i = 0, k = 0; i < array.length; i++) {
            if (i == index) {
                continue;
            }
            newArray[k++] = array[i];
        }
        return newArray;
    }

    private static float[] removeElementFromArray(float[] array, int index) {
        float[] newArray = new float[array.length - 1];

        for (int i = 0, k = 0; i < array.length; i++) {
            if (i == index) {
                continue;
            }
            newArray[k++] = array[i];
        }
        return newArray;
    }

    /**
     * if view is part of circular flow
     * @param view
     * @return true if the flow contains the view
     */
    public boolean isUpdatable(View view) {
        if (!containsId(view.getId())) {
            return false;
        }
        int indexView = indexFromId(view.getId());
        return indexView != -1;
    }
}
