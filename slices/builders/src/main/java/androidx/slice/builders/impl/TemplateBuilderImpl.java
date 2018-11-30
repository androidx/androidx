/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice.builders.impl;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Clock;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;
import androidx.slice.SystemClock;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public abstract class TemplateBuilderImpl {

    private Slice.Builder mSliceBuilder;
    private final SliceSpec mSpec;
    private Clock mClock;

    protected TemplateBuilderImpl(Slice.Builder b, SliceSpec spec) {
        this(b, spec, new SystemClock());
    }

    protected TemplateBuilderImpl(Slice.Builder b, SliceSpec spec, Clock clock) {
        mSliceBuilder = b;
        mSpec = spec;
        mClock = clock;
    }

    protected void setBuilder(Slice.Builder builder) {
        mSliceBuilder = builder;
    }

    /**
     * Construct the slice.
     */
    public Slice build() {
        mSliceBuilder.setSpec(mSpec);
        apply(mSliceBuilder);
        return mSliceBuilder.build();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public Slice.Builder getBuilder() {
        return mSliceBuilder;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public Slice.Builder createChildBuilder() {
        return new Slice.Builder(mSliceBuilder);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public abstract void apply(Slice.Builder builder);

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public Clock getClock() {
        return mClock;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public SliceSpec getSpec() {
        return mSpec;
    }
}
