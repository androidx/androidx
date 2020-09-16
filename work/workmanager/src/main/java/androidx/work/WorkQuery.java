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

package androidx.work;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A specification for querying {@link WorkRequest}s. This is comprised of 4 components; namely
 * ids, unique work names, tags & work states.
 * <p>
 * A {@link List} of {@link WorkRequest} ids, or a {@link List} of  unique work names, or a
 * {@link List} of {@link WorkRequest} tags, or a {@link List} of {@link WorkInfo.State} can be
 * specified.
 * <p>
 * Each component in a {@link WorkQuery} is {@code AND}-ed with the others. Each value in a
 * component is {@code OR}-ed.
 * <p>
 * Example:
 * {@code (id1 OR id2 OR ...) AND (name1 OR name2 OR ...) AND (tag1 OR tag2 OR ...) AND (state1
 * OR state2 OR ...)}
 */
public final class WorkQuery {
    private final List<UUID> mIds;
    private final List<String> mUniqueWorkNames;
    private final List<String> mTags;
    private final List<WorkInfo.State> mStates;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    WorkQuery(@NonNull Builder builder) {
        mIds = builder.mIds;
        mUniqueWorkNames = builder.mUniqueWorkNames;
        mTags = builder.mTags;
        mStates = builder.mStates;
    }

    /**
     * @return The {@link List} of {@link WorkRequest} ids being queried.
     */
    @NonNull
    public List<UUID> getIds() {
        return mIds;
    }

    /**
     * @return the {@link List} of unique works name being queried
     */
    @NonNull
    public List<String> getUniqueWorkNames() {
        return mUniqueWorkNames;
    }

    /**
     * @return the {@link List} of tags being queried
     */
    @NonNull
    public List<String> getTags() {
        return mTags;
    }

    /**
     * @return the {@link List} of {@link WorkInfo.State}s being queried
     */
    @NonNull
    public List<WorkInfo.State> getStates() {
        return mStates;
    }


    /**
     * A builder for {@link WorkQuery}.
     */
    public static final class Builder {
        // Synthetic access
        List<UUID> mIds;
        // Synthetic access
        List<String> mUniqueWorkNames;
        // Synthetic access
        List<String> mTags;
        // Synthetic access
        List<WorkInfo.State> mStates;

        private Builder() {
            mIds = new ArrayList<>();
            mUniqueWorkNames = new ArrayList<>();
            mTags = new ArrayList<>();
            mStates = new ArrayList<>();
        }

        /**
         * Creates a {@link WorkQuery.Builder} with a {@link List} of {@link WorkRequest} ids.
         *
         * @param ids The {@link List} of {@link WorkRequest} ids.
         * @return a {@link Builder} instance
         */
        @NonNull
        @SuppressLint("BuilderSetStyle")
        public static Builder fromIds(@NonNull List<UUID> ids) {
            Builder builder = new Builder();
            builder.addIds(ids);
            return builder;
        }

        /**
         * Creates a {@link WorkQuery.Builder} with a {@link List} of {@code uniqueWorkNames}.
         *
         * @param uniqueWorkNames The {@link List} of unique work names
         * @return a {@link Builder} instance
         */
        @NonNull
        @SuppressLint("BuilderSetStyle")
        public static Builder fromUniqueWorkNames(@NonNull List<String> uniqueWorkNames) {
            Builder builder = new Builder();
            builder.addUniqueWorkNames(uniqueWorkNames);
            return builder;
        }

        /**
         * Creates a {@link WorkQuery.Builder} with a {@link List} of {@link WorkRequest} tags.
         *
         * @param tags The {@link List} of {@link WorkRequest} tags
         * @return a {@link Builder} instance
         */
        @NonNull
        @SuppressLint("BuilderSetStyle")
        public static Builder fromTags(@NonNull List<String> tags) {
            Builder builder = new Builder();
            builder.addTags(tags);
            return builder;
        }

        /**
         * Creates a {@link WorkQuery.Builder} with a {@link List} of {@link WorkInfo.State} states.
         *
         * @param states The {@link List} of {@link WorkInfo.State} to add to the {@link WorkQuery}
         * @return a {@link Builder} instance
         */
        @NonNull
        @SuppressLint("BuilderSetStyle")
        public static Builder fromStates(@NonNull List<WorkInfo.State> states) {
            Builder builder = new Builder();
            builder.addStates(states);
            return builder;
        }

        /**
         * Adds a {@link List} of {@link WorkRequest} {@code ids} to the {@link WorkQuery}
         *
         * @param ids The {@link List} {@link WorkRequest} {@code ids} to add
         * @return the instance of the {@link Builder}
         */
        @NonNull
        public Builder addIds(@NonNull List<UUID> ids) {
            mIds.addAll(ids);
            return this;
        }

        /**
         * Adds a {@link List} of {@code uniqueWorkNames} to the {@link WorkQuery}
         *
         * @param uniqueWorkNames The {@link List} of unique work names to add
         * @return the instance of the {@link Builder}
         */
        @NonNull
        public Builder addUniqueWorkNames(@NonNull List<String> uniqueWorkNames) {
            mUniqueWorkNames.addAll(uniqueWorkNames);
            return this;
        }

        /**
         * Adds a {@link List} of {@link WorkRequest} tag to the {@link WorkQuery}.
         *
         * @param tags The {@link List} of {@link WorkRequest} tags to add
         * @return the instance of the {@link Builder}
         */
        @NonNull
        public Builder addTags(@NonNull List<String> tags) {
            mTags.addAll(tags);
            return this;
        }

        /**
         * Adds a {@link List} of {@link WorkInfo.State}s to the {@link WorkQuery}.
         *
         * @param states The {@link List} of {@link WorkInfo.State}s to add
         * @return the instance of the {@link Builder}
         */
        @NonNull
        public Builder addStates(@NonNull List<WorkInfo.State> states) {
            mStates.addAll(states);
            return this;
        }

        /**
         * Creates an instance of {@link WorkQuery}.
         *
         * @return the {@link WorkQuery} instance
         */
        @NonNull
        public WorkQuery build() {
            if (mIds.isEmpty()
                    && mUniqueWorkNames.isEmpty()
                    && mTags.isEmpty()
                    && mStates.isEmpty()) {

                String message =
                        "Must specify ids, uniqueNames, tags or states when building a WorkQuery";
                throw new IllegalArgumentException(message);
            }

            return new WorkQuery(this);
        }
    }
}
