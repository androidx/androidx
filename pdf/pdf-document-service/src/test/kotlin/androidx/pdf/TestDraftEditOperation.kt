/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf

import android.os.Parcel

// A fake operation that allows us to control the Parcel size for unflatten testing
// and acts as a generic operation for process testing.
internal class TestDraftEditOperation(val id: String, private val simulatedSize: Int) :
    DraftEditOperation {
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        // Pad the parcel to reach simulatedSize
        if (simulatedSize > 0) {
            dest.writeByteArray(ByteArray(simulatedSize))
        }
    }

    override fun describeContents(): Int = 0
}
