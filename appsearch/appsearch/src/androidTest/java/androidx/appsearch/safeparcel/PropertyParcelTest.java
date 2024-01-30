/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.safeparcel;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;

import org.junit.Test;

public class PropertyParcelTest {
    @Test
    public void testTwoDimensionByteArray_serializationSupported() {
        int row = 20;
        int col = 10;
        byte[][] bytesArray = new byte[row][col];
        for (int i = 0; i < row; ++i) {
            for (int j = 0; j < col; ++j) {
                bytesArray[i][j] = (byte) (i + j);
            }
        }

        String propertyName = "propertyName";
        PropertyParcel expectedPropertyParcel =
                new PropertyParcel.Builder(propertyName).setBytesValues(bytesArray).build();
        Parcel data = Parcel.obtain();
        try {
            data.writeParcelable(expectedPropertyParcel, /* flags= */ 0);
            data.setDataPosition(0);
            @SuppressWarnings("deprecation")
            PropertyParcel actualPropertyParcel = data.readParcelable(
                    PropertyParcelTest.class.getClassLoader());
            assertThat(expectedPropertyParcel).isEqualTo(actualPropertyParcel);
        } finally {
            data.recycle();
        }
    }
}
