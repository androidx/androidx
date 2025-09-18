/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.measure;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;

import org.jspecify.annotations.NonNull;

/** Interface describing the measure/layout contract for components */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Measurable {

    /**
     * Measure a component and store the result of the measure in the provided MeasurePass. This
     * does not apply the measure to the component.
     */
    void measure(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure);

    /** Apply a given measure to the component */
    void layout(@NonNull RemoteContext context, @NonNull MeasurePass measure);

    /**
     * Return true if the component needs to be remeasured
     *
     * @return true if need to remeasured, false otherwise
     */
    boolean needsMeasure();

    /**
     * Animate bounds of the component
     *
     * @param context
     */
    void animatingBounds(@NonNull RemoteContext context);
}
