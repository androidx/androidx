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

import androidx.annotation.NonNull;

/**
 * A Writable Profile Section for ART profiles on Android 12.
 */
class WritableFileSection {
    final FileSectionType mType;
    final int mExpectedInflateSize;
    final byte[] mContents;
    final boolean mNeedsCompression;

    WritableFileSection(
            @NonNull FileSectionType type,
            int expectedInflateSize,
            @NonNull byte[] contents,
            boolean needsCompression) {
        this.mType = type;
        this.mExpectedInflateSize = expectedInflateSize;
        this.mContents = contents;
        this.mNeedsCompression = needsCompression;
    }
}
