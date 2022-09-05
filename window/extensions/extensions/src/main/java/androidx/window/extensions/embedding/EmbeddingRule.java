/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.extensions.embedding;

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Base interface for activity embedding rules. Used to group different types of rules together when
 * updating from the core library.
 */
public abstract class EmbeddingRule {
    @Nullable
    private final String mTag;

    EmbeddingRule(@Nullable String tag) {
        mTag = tag;
    }

    // TODO(b/240912390): refer to the real API in later CLs.
    /**
     * A unique string to identify this {@link EmbeddingRule}.
     * The suggested usage is to set the tag in the corresponding rule builder to be able to
     * differentiate between different rules in the callbacks. For example, it can be used to
     * compute the right {@link SplitAttributes} for the right split rule in
     * {@code SplitAttributesCalculator#computeSplitAttributesForState}.
     *
     * @since {@link androidx.window.extensions.WindowExtensions#VENDOR_API_LEVEL_2}
     */
    @Nullable
    public String getTag() {
        return mTag;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EmbeddingRule)) return false;
        final EmbeddingRule otherRule = (EmbeddingRule) other;
        return Objects.equals(mTag, otherRule.mTag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mTag);
    }
}
