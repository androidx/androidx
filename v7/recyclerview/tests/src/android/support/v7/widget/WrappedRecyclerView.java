/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.widget;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;

/**
 * RecyclerView wrapper used in tests. This class can fake behavior like layout direction w/o
 * playing with framework support.
 */
public class WrappedRecyclerView extends RecyclerView {

    Boolean mFakeRTL;

    public void setFakeRTL(Boolean fakeRTL) {
        mFakeRTL = fakeRTL;
    }

    public WrappedRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public WrappedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WrappedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        //initializeScrollbars(null);
    }

    @Override
    public int getLayoutDirection() {
        if (mFakeRTL == null) {
            return super.getLayoutDirection();
        }
        return Boolean.TRUE.equals(mFakeRTL) ? ViewCompat.LAYOUT_DIRECTION_RTL
                : ViewCompat.LAYOUT_DIRECTION_LTR;
    }
}