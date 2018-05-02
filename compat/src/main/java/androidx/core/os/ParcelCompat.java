/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.os;

import android.os.Parcel;

/**
 * Helper for accessing features in {@link Parcel}.
 */
public final class ParcelCompat {

    /**
     * Read a boolean value from the parcel at the current {@link Parcel#dataPosition()}.
     */
    public static boolean readBoolean(Parcel in) {
        return in.readInt() != 0;
    }

    /**
     * Write a boolean value into the parcel at the current {@link Parcel#dataPosition()},
     * growing {@link Parcel#dataCapacity()} if needed.
     *
     * <p>Note: This method currently delegates to {@link Parcel#writeInt} with a value of 1 or 0
     * for true or false, respectively, but may change in the future.
     */
    public static void writeBoolean(Parcel out, boolean value) {
        out.writeInt(value ? 1 : 0);
    }

    private ParcelCompat() {}
}
