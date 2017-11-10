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

package androidx.app.slice.builders;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;

import android.net.Uri;
import android.support.annotation.RestrictTo;

import androidx.app.slice.Slice;

/**
 * Base class of builders of various template types.
 */
public abstract class TemplateSliceBuilder {

    private final Slice.Builder mSliceBuilder;

    public TemplateSliceBuilder(Uri uri) {
        mSliceBuilder = new Slice.Builder(uri);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    protected TemplateSliceBuilder(Slice.Builder b) {
        mSliceBuilder = b;
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
     * Construct the slice.
     */
    public Slice build() {
        apply(mSliceBuilder);
        return mSliceBuilder.build();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public abstract void apply(Slice.Builder builder);
}
