/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill.inline.common;

import android.app.PendingIntent;
import android.app.slice.Slice;
import android.app.slice.SliceSpec;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.autofill.inline.UiVersions;

/**
 * Base class representing a type that encodes the content information, and can be
 * exported to a Slice.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(api = Build.VERSION_CODES.Q) //TODO(b/147116534): Update to R.
public abstract class SlicedContent implements UiVersions.Content {

    static final Uri INLINE_SLICE_URI = Uri.parse("inline.slice");

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    protected final Slice mSlice;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected SlicedContent(@NonNull Slice slice) {
        mSlice = slice;
    }

    /**
     * Returns the wrapped slice containing the UI content.
     */
    @NonNull
    @Override
    public final Slice getSlice() {
        return mSlice;
    }

    /**
     * @hide
     * @see androidx.autofill.inline.Renderer#getAttributionIntent(Slice)
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    public abstract PendingIntent getAttributionIntent();

    /**
     * Returns true if the wrapped slice is valid according to the slice version.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract boolean isValid();

    /**
     * Returns the version of the {@code slice}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    public static String getVersion(@NonNull Slice slice) {
        return slice.getSpec().getType();
    }

    /**
     * Base builder class for the {@link SlicedContent}.
     *
     * @param <T> represents the type that this builder can build.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract static class Builder<T extends SlicedContent> {

        @NonNull
        protected final Slice.Builder mSliceBuilder;

        protected Builder(@NonNull String version) {
            mSliceBuilder = new Slice.Builder(INLINE_SLICE_URI, new SliceSpec(version, 1));
        }

        /**
         * Returns a subclass of {@link SlicedContent} built by this builder.
         */
        @NonNull
        public abstract T build();
    }
}
