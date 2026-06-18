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
import androidx.compose.remote.core.operations.layout.Component;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents the result of a measure pass on the entire hierarchy TODO: optimize to use a flat
 * array vs the current hashmap
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MeasurePass {
    @NonNull HashMap<Integer, ComponentMeasure> mList = new HashMap<>();
    @NonNull ArrayList<ComponentMeasure> mPool = new ArrayList<>();

    /** Clear the MeasurePass, returning active ComponentMeasure instances to the pool */
    public void clear() {
        for (ComponentMeasure measure : mList.values()) {
            mPool.add(measure);
        }
        mList.clear();
    }

    private ComponentMeasure obtain(int id, float x, float y, float w, float h, int visibility) {
        if (!mPool.isEmpty()) {
            ComponentMeasure measure = mPool.remove(mPool.size() - 1);
            measure.reset(id, x, y, w, h, visibility);
            return measure;
        }
        return new ComponentMeasure(id, x, y, w, h, visibility);
    }

    /**
     * Add a ComponentMeasure to the MeasurePass
     *
     * @param measure the ComponentMeasure to add
     * @throws Exception
     */
    public void add(@NonNull ComponentMeasure measure) throws Exception {
        if (measure.mId == -1) {
            throw new Exception("Component has no id!");
        }
        mList.put(measure.mId, measure);
    }

    /**
     * Returns true if the current MeasurePass already contains a ComponentMeasure for the given id.
     *
     * @param id
     * @return
     */
    public boolean contains(int id) {
        return mList.containsKey(id);
    }

    /**
     * return the ComponentMeasure associated with a given component
     *
     * @param c the Component
     * @return the associated ComponentMeasure
     */
    public @NonNull ComponentMeasure get(@NonNull Component c) {
        int id = c.getComponentId();
        ComponentMeasure measure = mList.get(id);
        if (measure == null) {
            measure = obtain(id, c.getX(), c.getY(), c.getWidth(), c.getHeight(),
                    Component.Visibility.VISIBLE);
            mList.put(id, measure);
        }
        return measure;
    }

    /**
     * Returns the ComponentMeasure associated with the id, creating one if none exists.
     *
     * @param id the component id
     * @return the associated ComponentMeasure
     */
    public @NonNull ComponentMeasure get(int id) {
        ComponentMeasure measure = mList.get(id);
        if (measure == null) {
            measure = obtain(id, 0f, 0f, 0f, 0f, Component.Visibility.GONE);
            mList.put(id, measure);
        }
        return measure;
    }
}
