/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.constraintlayout.core.state.helpers;

import androidx.constraintlayout.core.state.Reference;
import androidx.constraintlayout.core.state.State;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.Guideline;

public class GuidelineReference implements Facade, Reference {

    final State mState;
    private int mOrientation;
    private Guideline mGuidelineWidget;
    private int mStart = -1;
    private int mEnd = -1;
    private float mPercent = 0;

    private Object mKey;

    @Override
    public void setKey(Object key) {
        this.mKey = key;
    }

    @Override
    public Object getKey() {
        return mKey;
    }

    public GuidelineReference(State state) {
        mState = state;
    }

    // @TODO: add description
    public GuidelineReference start(Object margin) {
        mStart = mState.convertDimension(margin);
        mEnd = -1;
        mPercent = 0;
        return this;
    }

    // @TODO: add description
    public GuidelineReference end(Object margin) {
        mStart = -1;
        mEnd = mState.convertDimension(margin);
        mPercent = 0;
        return this;
    }

    // @TODO: add description
    public GuidelineReference percent(float percent) {
        mStart = -1;
        mEnd = -1;
        mPercent = percent;
        return this;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public int getOrientation() {
        return mOrientation;
    }

    // @TODO: add description
    @Override
    public void apply() {
        mGuidelineWidget.setOrientation(mOrientation);
        if (mStart != -1) {
            mGuidelineWidget.setGuideBegin(mStart);
        } else if (mEnd != -1) {
            mGuidelineWidget.setGuideEnd(mEnd);
        } else {
            mGuidelineWidget.setGuidePercent(mPercent);
        }
    }

    @Override
    public Facade getFacade() {
        return null;
    }

    @Override
    public ConstraintWidget getConstraintWidget() {
        if (mGuidelineWidget == null) {
            mGuidelineWidget = new Guideline();
        }
        return mGuidelineWidget;
    }

    @Override
    public void setConstraintWidget(ConstraintWidget widget) {
        if (widget instanceof Guideline) {
            mGuidelineWidget = (Guideline) widget;
        } else {
            mGuidelineWidget = null;
        }
    }
}
