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

import android.graphics.Outline;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

@RequiresApi(21)
class ActionBarBackgroundDrawableV21 extends ActionBarBackgroundDrawable {

    public ActionBarBackgroundDrawableV21(ActionBarContainer container) {
        super(container);
    }

    @Override
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
