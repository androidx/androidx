/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v7.graphics;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.test.InstrumentationRegistry;
import android.support.v7.palette.test.R;

class TestUtils {

    static Bitmap loadSampleBitmap() {
        return BitmapFactory.decodeResource(
                InstrumentationRegistry.getContext().getResources(),
                R.drawable.photo);
    }

    static void assertCloseColors(int expected, int actual) {
        assertEquals(Color.red(expected), Color.red(actual), 2);
        assertEquals(Color.green(expected), Color.green(actual), 2);
        assertEquals(Color.blue(expected), Color.blue(actual), 2);
    }

}
