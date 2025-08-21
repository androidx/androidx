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

package androidx.savedstate.serialization

import android.os.Parcel
import androidx.savedstate.SavedState

actual fun platformEncodeDecode(savedState: SavedState, doMarshalling: Boolean): SavedState {
    val inParcel =
        Parcel.obtain().apply {
            savedState.writeToParcel(this, 0)
            setDataPosition(0)
        }
    val outParcel =
        if (doMarshalling) {
            val bytes = inParcel.marshall()
            Parcel.obtain().apply {
                unmarshall(bytes, 0, bytes.size)
                setDataPosition(0)
            }
        } else {
            inParcel
        }

    try {
        return outParcel.readBundle(savedState::class.java.classLoader)!!
    } finally {
        // Always recycle the original parcel
        inParcel.recycle()
        // Recycle the decoded parcel only if it's different
        if (outParcel !== inParcel) {
            outParcel.recycle()
        }
    }
}
