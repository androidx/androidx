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
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.layout.Component;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * High-performance flat array-backed alternative to {@link MeasurePass}.
 * Replaces HashMap lookups and autoboxing with direct, O(1) constant-time array indexing.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FlatMeasurePass extends MeasurePass {
    @NonNull private ComponentMeasure[] mMetrics;
    private int mGeneration = 1;

    public FlatMeasurePass(int totalComponents) {
        super();
        mMetrics = new ComponentMeasure[totalComponents];
    }

    @Override
    public void setContext(@Nullable RemoteContext context) {
        mContext = context;
    }

    /** Clear the FlatMeasurePass using O(1) generation token incrementing */
    @Override
    public void clear() {
        super.clear();
        mGeneration++;
        if (mGeneration == Integer.MAX_VALUE) {
            mGeneration = 1;
            java.util.Arrays.fill(mMetrics, null);
        }
    }

    /**
     * Retrieve a ComponentMeasure instance from the context's pool if available,
     * or instantiate a new one.
     */
    @Override
    public @NonNull ComponentMeasure obtain(
            @NonNull Component c, float x, float y, float w, float h, int visibility) {
        ComponentMeasure m = obtain(c.getComponentId(), x, y, w, h, visibility);
        m.mInternalLayoutIndex = getLayoutIndex(c);
        return m;
    }

    /**
     * Retrieve a ComponentMeasure instance from the context's pool if available,
     * or instantiate a new one.
     */
    @Override
    public @NonNull ComponentMeasure obtain(
            int id, float x, float y, float w, float h, int visibility) {
        ComponentMeasure m;
        if (mContext != null) {
            m = mContext.getComponentMeasurePool().obtain(id, x, y, w, h, visibility);
        } else {
            m = new ComponentMeasure(id, x, y, w, h, visibility);
        }
        m.mGeneration = mGeneration;
        return m;
    }

    private int mFallbackLayoutIndex = 0;

    private int getLayoutIndex(@NonNull Component c) {
        if (c.mInternalLayoutIndex >= 0) {
            return c.mInternalLayoutIndex;
        }
        if (mContext != null && mContext.getDocument() != null) {
            return mContext.getDocument().assignLayoutIndex(c);
        }
        c.mInternalLayoutIndex = mFallbackLayoutIndex++;
        return c.mInternalLayoutIndex;
    }

    /** Recycle a ComponentMeasure instance back into the pool */
    @Override
    public void recycle(@NonNull ComponentMeasure measure) {
        if (mContext != null) {
            mContext.getComponentMeasurePool().recycle(measure);
        }
    }

    @Override
    public void add(@NonNull ComponentMeasure measure) throws Exception {
        if (measure.mId == -1) {
            throw new Exception("Component has no id!");
        }
        int idx = measure.mInternalLayoutIndex;
        if (idx < 0) {
            if (mContext != null && mContext.getDocument() != null) {
                Component c = mContext.getDocument().getComponent(measure.mId);
                if (c != null) {
                    idx = getLayoutIndex(c);
                    measure.mInternalLayoutIndex = idx;
                }
            }
            if (idx < 0) {
                idx = mFallbackLayoutIndex++;
                measure.mInternalLayoutIndex = idx;
            }
        }
        if (idx >= mMetrics.length) {
            ComponentMeasure[] expanded = new ComponentMeasure[idx + 16];
            System.arraycopy(mMetrics, 0, expanded, 0, mMetrics.length);
            mMetrics = expanded;
        }
        measure.mGeneration = mGeneration;
        mMetrics[idx] = measure;
    }

    /**
     * Returns true if the current MeasurePass already contains a ComponentMeasure for the given id.
     */
    @Override
    public boolean contains(int id) {
        if (mContext != null && mContext.getDocument() != null) {
            Component c = mContext.getDocument().getComponent(id);
            if (c != null) {
                int idx = getLayoutIndex(c);
                if (idx < mMetrics.length) {
                    ComponentMeasure m = mMetrics[idx];
                    return m != null && m.mGeneration == mGeneration;
                }
            }
        }
        return super.contains(id);
    }

    /**
     * Return the ComponentMeasure associated with a given component.
     * Direct O(1) constant-time array access using dense indexing.
     *
     * @param c the Component
     * @return the associated ComponentMeasure
     */
    @Override
    public @NonNull ComponentMeasure get(@NonNull Component c) {
        int idx = getLayoutIndex(c);
        if (idx >= mMetrics.length) {
            ComponentMeasure[] expanded = new ComponentMeasure[idx + 16];
            System.arraycopy(mMetrics, 0, expanded, 0, mMetrics.length);
            mMetrics = expanded;
        }
        ComponentMeasure measure = mMetrics[idx];
        if (measure != null) {
            if (measure.mGeneration != mGeneration) {
                measure.reset(c.getComponentId(), c.getX(), c.getY(),
                        c.getWidth(), c.getHeight(), c.mVisibility);
                measure.mGeneration = mGeneration;
            }
            return measure;
        }
        measure = obtain(c, c.getX(), c.getY(), c.getWidth(), c.getHeight(),
                c.mVisibility);
        mMetrics[idx] = measure;
        return measure;
    }

    /**
     * Returns the ComponentMeasure associated with the original id, creating one if none exists.
     * Resolves the original ID to dense layout index using the context's document mapping.
     *
     * @param id the component id
     * @return the associated ComponentMeasure
     */
    @Override
    public @NonNull ComponentMeasure get(int id) {
        if (mContext != null && mContext.getDocument() != null) {
            Component c = mContext.getDocument().getComponent(id);
            if (c != null) {
                return get(c);
            }
        }
        // Fallback if context is not yet wired or component not found
        ComponentMeasure fallback = super.get(id);
        fallback.mGeneration = mGeneration;
        return fallback;
    }
}
