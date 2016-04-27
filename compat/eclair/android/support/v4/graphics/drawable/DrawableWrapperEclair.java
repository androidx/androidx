/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

class DrawableWrapperEclair extends DrawableWrapperDonut {

    DrawableWrapperEclair(Drawable drawable) {
        super(drawable);
    }

    DrawableWrapperEclair(DrawableWrapperState state, Resources resources) {
        super(state, resources);
    }

    @Override
    DrawableWrapperState mutateConstantState() {
        return new DrawableWrapperStateEclair(mState, null);
    }

    @Override
    protected Drawable newDrawableFromState(Drawable.ConstantState state, Resources res) {
        return state.newDrawable(res);
    }

    private static class DrawableWrapperStateEclair extends DrawableWrapperState {
        DrawableWrapperStateEclair(@Nullable DrawableWrapperState orig,
                @Nullable Resources res) {
            super(orig, res);
        }

        @Override
        public Drawable newDrawable(@Nullable Resources res) {
            return new DrawableWrapperEclair(this, res);
        }
    }
}