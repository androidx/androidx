/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.model.constraints;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Tab;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the constraints to apply when creating {@link TabTemplate}.
 *
 * @hide
 */
@ExperimentalCarApi
@RequiresCarApi(6)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TabsConstraints {
    private static final int MAXIMUM_ALLOWED_TABS = 4;
    private static final int MINIMUM_REQUIRED_TABS = 2;

    @NonNull
    public static final TabsConstraints DEFAULT =
            new TabsConstraints.Builder()
                    .setMaxTabs(MAXIMUM_ALLOWED_TABS)
                    .setMinTabs(MINIMUM_REQUIRED_TABS)
                    .build();

    private final int mMaxTabs;
    private final int mMinTabs;

    /**
     * Validates that the {@link Tab}s satisfies this {@link TabConstraints} instance.
     *
     * @throws IllegalArgumentException if the constraints are not met
     */
    public void validateOrThrow(@NonNull List<Tab> tabs, @NonNull String activeTabContentId) {
        if (tabs.size() < mMinTabs) {
            throw new IllegalArgumentException(
                    "There must be at least " + mMinTabs + " tab(s) added, but only found "
                            + tabs.size());
        }

        if (tabs.size() > mMaxTabs) {
            throw new IllegalArgumentException(
                    "There cannot be more than " + mMaxTabs + " tabs added, found " + tabs.size());
        }

        boolean hasTabWithActiveTabContentId = false;
        Set<String> contentIdSet = new HashSet<>();
        for (Tab tab : tabs) {
            if (activeTabContentId.equals(tab.getContentId())) {
                hasTabWithActiveTabContentId = true;
            }
            if (!contentIdSet.add(tab.getContentId())) {
                throw new IllegalArgumentException(
                        "Found duplicate tab ID: " + tab.getContentId() + ". Each tab must have a"
                                + " unique ID."
                );
            }
        }

        if (!hasTabWithActiveTabContentId) {
            throw new IllegalArgumentException(
                    "There is no tab with content ID matching the active tab content ID set on "
                            + "the template");
        }
    }

    TabsConstraints(Builder builder) {
        mMaxTabs = builder.mMaxTabs;
        mMinTabs = builder.mMinTabs;
    }

    /**
     * A builder of {@link TabsConstraints}.
     */
    public static final class Builder {
        int mMaxTabs = Integer.MAX_VALUE;
        int mMinTabs = 0;

        /** Sets the maximum number of tabs allowed to be added. */
        @NonNull
        public TabsConstraints.Builder setMaxTabs(int maxTabs) {
            mMaxTabs = maxTabs;
            return this;
        }

        /** Sets the minimum number of tabs required to be added. */
        @NonNull
        public TabsConstraints.Builder setMinTabs(int minTabs) {
            mMinTabs = minTabs;
            return this;
        }

        /**
         * Constructs the {@link TabsConstraints} defined by this builder.
         */
        @NonNull
        public TabsConstraints build() {
            return new TabsConstraints(this);
        }

        public Builder() {
        }
    }
}
