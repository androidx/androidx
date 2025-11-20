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

import android.animation.PropertyValuesHolder;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.Property;

class PropertyValuesHolderUtils {

    /**
     * Constructs and returns a PropertyValuesHolder with a given property and
     * a Path along which the values should be animated. This variant supports a
     * <code>TypeConverter</code> to convert from <code>PointF</code> to the target
     * type.
     *
     * @param property The property being animated. Should not be null.
     * @param path     The Path along which the values should be animated.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    static PropertyValuesHolder ofPointF(Property<?, PointF> property, Path path) {
        return PropertyValuesHolder.ofObject(property, null, path);
    }

    private PropertyValuesHolderUtils() { }
}
