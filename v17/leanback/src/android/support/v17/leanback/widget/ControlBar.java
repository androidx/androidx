/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

class ControlBar extends LinearLayout {

    public ControlBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ControlBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        if (getChildCount() > 0) {
            if (getChildAt(getChildCount() / 2).requestFocus(direction, previouslyFocusedRect)) {
                return true;
            }
        }
        return super.requestFocus(direction, previouslyFocusedRect);
    }

}
