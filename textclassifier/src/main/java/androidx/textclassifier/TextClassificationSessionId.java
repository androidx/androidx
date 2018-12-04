/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import java.util.Locale;
import java.util.UUID;

/**
 * This class represents the id of a text classification session.
 */
public final class TextClassificationSessionId {
    private static final String EXTRA_VALUE = "value";

    private final @NonNull String mValue;

    /**
     * Creates a new instance.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public TextClassificationSessionId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Creates a new instance.
     *
     * @param value The internal value.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public TextClassificationSessionId(@NonNull String value) {
        mValue = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mValue.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TextClassificationSessionId other = (TextClassificationSessionId) obj;
        if (!mValue.equals(other.mValue)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "TextClassificationSessionId {%s}", mValue);
    }

    /**
     * Flattens this id to a string.
     *
     * @return The flattened id.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @NonNull String flattenToString() {
        return mValue;
    }

    /**
     * Unflattens a print job id from a string.
     *
     * @param string The string.
     * @return The unflattened id, or null if the string is malformed.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static @NonNull TextClassificationSessionId unflattenFromString(@NonNull String string) {
        return new TextClassificationSessionId(string);
    }

    /**
     * Adds this Icon to a Bundle that can be read back with the same parameters
     * to {@link #createFromBundle(Bundle)}.
     */
    @NonNull
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_VALUE, mValue);
        return bundle;
    }

    /**
     * Extracts a {@link TextClassificationSessionId} from a bundle that was added using
     * {@link #toBundle()}.
     */
    @NonNull
    @SuppressLint("RestrictedApi")
    public static TextClassificationSessionId createFromBundle(@NonNull Bundle bundle) {
        Preconditions.checkNotNull(bundle);

        String value = bundle.getString(EXTRA_VALUE);
        return new TextClassificationSessionId(value);
    }
}
