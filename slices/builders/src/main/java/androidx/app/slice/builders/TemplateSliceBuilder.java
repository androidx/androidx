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


import android.app.slice.Slice;
import android.net.Uri;
import android.support.annotation.RestrictTo;

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
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public Slice.Builder getBuilder() {
        return mSliceBuilder;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
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
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract void apply(Slice.Builder builder);

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract void add(SubTemplateSliceBuilder builder);

    /**
     * Base class of builders for sub-slices of {@link TemplateSliceBuilder}s.
     * @param <T> Type of parent
     */
    public abstract static class SubTemplateSliceBuilder<T extends TemplateSliceBuilder> {

        private final Slice.Builder mBuilder;
        private final T mParent;

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public SubTemplateSliceBuilder(Slice.Builder builder, T parent) {
            mBuilder = builder;
            mParent = parent;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public SubTemplateSliceBuilder(Slice.Builder builder) {
            mBuilder = builder;
            mParent = null;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public Slice.Builder getBuilder() {
            return mBuilder;
        }

        /**
         * Construct the slice.
         */
        public Slice build() {
            return mBuilder.build();
        }

        /**
         * Construct the slice and return to the parent object. If this object was not
         * created from a {@link TemplateSliceBuilder} it will return null.
         * @return parent builder
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public T finish() {
            if (mParent != null) {
                mParent.add(this);
            }
            return mParent;
        }
    }
}
