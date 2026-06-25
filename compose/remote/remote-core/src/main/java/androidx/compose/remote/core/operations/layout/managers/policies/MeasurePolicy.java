/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.managers.policies;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.operations.layout.managers.LayoutManager;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;

import org.jspecify.annotations.NonNull;

/**
 * Strategy interface for resolving LayoutManager measurement.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MeasurePolicy {
    /** Measures the given layout component within constraints. */
    void measure(
            @NonNull LayoutManager layout,
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure);
}
