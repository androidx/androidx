/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.Rect;
import android.support.annotation.RequiresApi;
import android.view.Gravity;
import android.view.View;

@RequiresApi(21)
class RoundedBitmapDrawable21 extends RoundedBitmapDrawable {
    protected RoundedBitmapDrawable21(Resources res, Bitmap bitmap) {
        super(res, bitmap);
    }

    @Override
    public void getOutline(Outline outline) {
        updateDstRect();
        outline.setRoundRect(mDstRect, getCornerRadius());
    }

    @Override
    public void setMipMap(boolean mipMap) {
        if (mBitmap != null) {
            mBitmap.setHasMipMap(mipMap);
            invalidateSelf();
        }
    }

    @Override
    public boolean hasMipMap() {
        return mBitmap != null && mBitmap.hasMipMap();
    }

    @Override
    void gravityCompatApply(int gravity, int bitmapWidth, int bitmapHeight,
            Rect bounds, Rect outRect) {
        Gravity.apply(gravity, bitmapWidth, bitmapHeight,
                bounds, outRect, View.LAYOUT_DIRECTION_LTR);
    }
}
