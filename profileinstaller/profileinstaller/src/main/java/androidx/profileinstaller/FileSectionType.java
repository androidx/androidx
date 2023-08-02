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

package androidx.profileinstaller;

/**
 * A list of profile file section types for Android S.
 */
enum FileSectionType {
    /** Represents a dex file section. This is a required file section type. */
    DEX_FILES(0L),

    /**
     * Optional file sections. The only ones we care about are CLASSES and METHODS.
     * Listing EXTRA_DESCRIPTORS & AGGREGATION_COUNT for completeness.
     */
    EXTRA_DESCRIPTORS(1L),
    CLASSES(2L),
    METHODS(3L),
    AGGREGATION_COUNT(4L);

    private final long mValue;

    FileSectionType(long value) {
        this.mValue = value;
    }

    public long getValue() {
        return mValue;
    }

    static FileSectionType fromValue(long value) {
        FileSectionType[] values = FileSectionType.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getValue() == value) {
                return values[i];
            }
        }
        throw new IllegalArgumentException("Unsupported FileSection Type " + value);
    }
}
