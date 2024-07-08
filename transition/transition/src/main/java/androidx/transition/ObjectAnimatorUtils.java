/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import android.animation.ObjectAnimator;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;
import android.util.Property;

import androidx.annotation.RequiresApi;

class ObjectAnimatorUtils {

    static <T> ObjectAnimator ofPointF(T target, Property<T, PointF> property, Path path) {
        if (Build.VERSION.SDK_INT >= 21) {
            return Api21Impl.ofObject(target, property, path);
        }
        return ObjectAnimator.ofFloat(target, new PathProperty<>(property, path), 0f, 1f);
    }

    private ObjectAnimatorUtils() { }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        static <T, V> ObjectAnimator ofObject(T target, Property<T, V> property, Path path) {
            return ObjectAnimator.ofObject(target, property, null, path);
        }
    }
}
