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

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_PARTIAL;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.slice.builders.ListBuilder.ACTION_WITH_LABEL;
import static androidx.slice.builders.ListBuilder.ICON_IMAGE;
import static androidx.slice.builders.ListBuilder.LARGE_IMAGE;
import static androidx.slice.builders.ListBuilder.RAW_IMAGE_LARGE;
import static androidx.slice.builders.ListBuilder.RAW_IMAGE_SMALL;
import static androidx.slice.core.SliceHints.HINT_RAW;
import static androidx.slice.core.SliceHints.HINT_SHOW_LABEL;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Clock;
import androidx.slice.Slice;
import androidx.slice.SliceSpec;
import androidx.slice.SystemClock;

import java.util.ArrayList;

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
    public abstract void apply(@NonNull Slice.Builder builder);

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

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    @NonNull
    protected ArrayList<String> parseImageMode(int imageMode, boolean isLoading) {
        ArrayList<String> hints = new ArrayList<>();
        if (imageMode == ACTION_WITH_LABEL) {
            hints.add(HINT_SHOW_LABEL);
        }
        if (imageMode != ICON_IMAGE) {
            hints.add(HINT_NO_TINT);
        }
        if (imageMode == LARGE_IMAGE || imageMode == RAW_IMAGE_LARGE) {
            hints.add(HINT_LARGE);
        }
        if (imageMode == RAW_IMAGE_SMALL || imageMode == RAW_IMAGE_LARGE) {
            hints.add(HINT_RAW);
        }
        if (isLoading) {
            hints.add(HINT_PARTIAL);
        }
        return hints;
    }
}
