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

package androidx.car.app.model;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.model.constraints.CarTextConstraints;

import java.util.Objects;

/**
 * Represents an {@link ItemList} that is contained inside a section.
 */
@CarProtocol
@KeepFields
public final class SectionedItemList {
    @Nullable
    private final ItemList mItemList;
    @Nullable
    private final CarText mHeader;

    /**
     * Creates an instance of a {@link SectionedItemList} with the given {@code itemList} and
     * {@code sectionHeader}.
     *
     * <p>Only {@link DistanceSpan}s and {@link DurationSpan}s are supported in the section header.
     *
     * @throws IllegalArgumentException if {@code sectionHeader} contains unsupported spans
     */
    @NonNull
    public static SectionedItemList create(
            @NonNull ItemList itemList, @NonNull CharSequence sectionHeader) {
        CarText sectionHeaderText = CarText.create(requireNonNull(sectionHeader));
        CarTextConstraints.TEXT_ONLY.validateOrThrow(sectionHeaderText);
        return new SectionedItemList(requireNonNull(itemList), sectionHeaderText);
    }

    /** Returns the {@link ItemList} for the section. */
    @NonNull
    public ItemList getItemList() {
        return requireNonNull(mItemList);
    }

    /** Returns the title of the section. */
    @NonNull
    public CarText getHeader() {
        return requireNonNull(mHeader);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ items: " + mItemList + ", has header: " + (mHeader != null) + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mItemList, mHeader);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SectionedItemList)) {
            return false;
        }
        SectionedItemList otherList = (SectionedItemList) other;

        return Objects.equals(mItemList, otherList.mItemList) && Objects.equals(mHeader,
                otherList.mHeader);
    }

    private SectionedItemList(@Nullable ItemList itemList, @Nullable CarText header) {
        mItemList = itemList;
        mHeader = header;
    }

    private SectionedItemList() {
        mItemList = null;
        mHeader = null;
    }
}
