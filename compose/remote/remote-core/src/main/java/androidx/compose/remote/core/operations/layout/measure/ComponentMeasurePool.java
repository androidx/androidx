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
package androidx.compose.remote.core.operations.layout.measure;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;

/**
 * Recycling pool for ComponentMeasure instances to reduce GC allocations during layout updates.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComponentMeasurePool {
    private final ArrayList<ComponentMeasure> mPool = new ArrayList<>();

    /**
      * Retrieve a ComponentMeasure instance from the pool if available,
      * or instantiate a new one if not.
      */
    public @NonNull ComponentMeasure obtain(
            int id, float x, float y, float w, float h, int visibility) {
        if (!mPool.isEmpty()) {
            ComponentMeasure measure = mPool.remove(mPool.size() - 1);
            measure.reset(id, x, y, w, h, visibility);
            return measure;
        }
        return new ComponentMeasure(id, x, y, w, h, visibility);
    }

    /** Recycle a ComponentMeasure instance back into the pool */
    public void recycle(@NonNull ComponentMeasure measure) {
        mPool.add(measure);
    }
}
