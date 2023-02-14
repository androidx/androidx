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

package androidx.constraintlayout.widget;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Control the visibility and elevation of the referenced views
 *
 * <b>Added in 1.1</b>
 * <p>
 *     This class controls the visibility of a set of referenced widgets.
 *     Widgets are referenced by being added to a comma separated list of ids, e.g.:
 *     <pre>
 *     {@code
 *          <androidx.constraintlayout.widget.Group
 *              android:id="@+id/group"
 *              android:layout_width="wrap_content"
 *              android:layout_height="wrap_content"
 *              android:visibility="visible"
 *              app:constraint_referenced_ids="button4,button9" />
 *     }
 *     </pre>
 *     <p>
 *         The visibility of the group will be applied to the referenced widgets.
 *         It's a convenient way to easily hide/show a set of widgets
 *         without having to maintain this set
 *         programmatically.
 *     <p>
 *     <h2>Multiple groups</h2>
 *     <p>
 *         Multiple groups can reference the same widgets
 *         -- in that case, the XML declaration order will
 *         define the final visibility state (the group declared last will have the last word).
 * </p>
 */
public class Group extends ConstraintHelper {

    public Group(Context context) {
        super(context);
    }

    public Group(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Group(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     *
     * @param attrs
     */
    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        mUseViewMeasure = false;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyLayoutFeatures();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        applyLayoutFeatures();
    }

    @Override
    public void setElevation(float elevation) {
        super.setElevation(elevation);
        applyLayoutFeatures();
    }

    /**
     *
     * @param container
     */
    @Override
    protected void applyLayoutFeaturesInConstraintSet(ConstraintLayout container) {
        applyLayoutFeatures(container);
    }

    /**
     *
     * @param container
     */
    @Override
    public void updatePostLayout(ConstraintLayout container) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        params.mWidget.setWidth(0);
        params.mWidget.setHeight(0);
    }
}
