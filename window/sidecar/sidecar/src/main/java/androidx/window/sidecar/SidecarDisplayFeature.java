/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window.sidecar;

import android.graphics.Rect;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Description of a physical feature on the display.
 * @deprecated Use androidx.window.extensions instead of this package.
 */
@Deprecated
public final class SidecarDisplayFeature {
    /**
     * The bounding rectangle of the feature within the application window in the window
     * coordinate space.
     */
    @NonNull
    private Rect mRect = new Rect();

    /**
     * The physical type of the feature.
     */
    @Type
    private int mType;

    /**
     * A fold in the flexible screen without a physical gap.
     */
    public static final int TYPE_FOLD = 1;

    /**
     * A physical separation with a hinge that allows two display panels to fold.
     */
    public static final int TYPE_HINGE = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            TYPE_FOLD,
            TYPE_HINGE,
    })
    @interface Type{}

    /** Gets the bounding rect of the display feature in window coordinate space. */
    @NonNull
    public Rect getRect() {
        return mRect;
    }

    /** Sets the bounding rect of the display feature in window coordinate space. */
    public void setRect(@NonNull Rect rect) {
        mRect.set(rect);
    }

    /** Gets the type of the display feature. */
    public @Type int getType() {
        return mType;
    }

    /** Sets the type of the display feature. */
    public void setType(@Type int type) {
        mType = type;
    }
}
