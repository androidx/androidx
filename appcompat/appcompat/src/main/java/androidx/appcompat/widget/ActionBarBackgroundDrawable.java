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

package androidx.appcompat.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

class ActionBarBackgroundDrawable extends Drawable {

    final ActionBarContainer mContainer;

    public ActionBarBackgroundDrawable(ActionBarContainer container) {
        mContainer = container;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mContainer.mIsSplit) {
            if (mContainer.mSplitBackground != null) {
                mContainer.mSplitBackground.draw(canvas);
            }
        } else {
            if (mContainer.mBackground != null) {
                mContainer.mBackground.draw(canvas);
            }
            if (mContainer.mStackedBackground != null && mContainer.mIsStacked) {
                mContainer.mStackedBackground.draw(canvas);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    @Override
    @RequiresApi(21)
    public void getOutline(@NonNull Outline outline) {
        if (mContainer.mIsSplit) {
            if (mContainer.mSplitBackground != null) {
                mContainer.mSplitBackground.getOutline(outline);
            }
        } else {
            // ignore the stacked background for shadow casting
            if (mContainer.mBackground != null) {
                mContainer.mBackground.getOutline(outline);
            }
        }
    }
}
