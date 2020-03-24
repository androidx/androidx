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

package androidx.camera.view.preview.transform.transformation;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Contains the required information to transform a camera preview.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Transformation {

    private final float mScaleX;
    private final float mScaleY;
    private final float mTransX;
    private final float mTransY;
    private final float mRotation;

    public Transformation() {
        this(1, 1, 0, 0, 0);
    }

    public Transformation(final float scaleX, final float scaleY, final float transX,
            final float transY, final float rotation) {
        this.mScaleX = scaleX;
        this.mScaleY = scaleY;
        this.mTransX = transX;
        this.mTransY = transY;
        this.mRotation = rotation;
    }

    public float getScaleX() {
        return mScaleX;
    }

    public float getScaleY() {
        return mScaleY;
    }

    public float getTransX() {
        return mTransX;
    }

    public float getTransY() {
        return mTransY;
    }

    public float getRotation() {
        return mRotation;
    }

    /** Performs the `sum` of two {@link Transformation} instances. */
    @NonNull
    public Transformation add(@NonNull final Transformation other) {
        return new Transformation(mScaleX * other.mScaleX,
                mScaleY * other.mScaleY,
                mTransX + other.mTransX,
                mTransY + other.mTransY,
                mRotation + other.mRotation);
    }

    /** Performs the `subtraction` of two {@link Transformation} instances. */
    @NonNull
    public Transformation subtract(@NonNull final Transformation other) {
        return new Transformation(mScaleX / other.mScaleX,
                mScaleY / other.mScaleY,
                mTransX - other.mTransX,
                mTransY - other.mTransY,
                mRotation - other.mRotation);
    }

    /** Returns a {@link Transformation} that represents the current state of the {@link View}. */
    @NonNull
    public static Transformation getTransformation(@NonNull final View view) {
        return new Transformation(view.getScaleX(), view.getScaleY(), view.getTranslationX(),
                view.getTranslationY(), view.getRotation());
    }
}
