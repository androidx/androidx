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

import android.os.Bundle;

import androidx.appsearch.util.BundleUtil;

import org.junit.Assert;
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
        // This simulates how we save properties in the GenericDocument.mProperties.
        Bundle bundle = new Bundle();
        String propertyName = "propertyName";
        bundle.putParcelable(propertyName,
                new PropertyParcel.Builder(propertyName).setBytesValues(
                        bytesArray).build());

        Bundle bundleCopy = BundleUtil.deepCopy(bundle);
        @SuppressWarnings("deprecation")
        byte[][] bytesArrayCopy =
                ((PropertyParcel) bundle.getParcelable(propertyName)).getBytesValues();

        Assert.assertArrayEquals(bytesArrayCopy, bytesArray);
    }
}
