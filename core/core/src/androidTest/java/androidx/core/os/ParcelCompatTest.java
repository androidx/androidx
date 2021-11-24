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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Rect;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParcelCompatTest {
    @Test
    public void readWriteBoolean() {
        Parcel p = Parcel.obtain();
        ParcelCompat.writeBoolean(p, true);
        ParcelCompat.writeBoolean(p, false);

        p.setDataPosition(0);
        assertTrue(ParcelCompat.readBoolean(p));
        assertFalse(ParcelCompat.readBoolean(p));
    }

    @Test
    public void readParcelable2Arg() {
        Rect r = new Rect(0, 0, 10, 10);
        Parcel p = Parcel.obtain();
        p.writeParcelable(r, 0);

        p.setDataPosition(0);
        Rect r2 = ParcelCompat.readParcelable(p, Rect.class.getClassLoader(), Rect.class);
        assertEquals(r, r2);
    }

    @Test
    public void readSerializable2Arg() {
        String s = "Hello World";
        Parcel p = Parcel.obtain();
        p.writeSerializable(s);

        p.setDataPosition(0);
        String s2 = ParcelCompat.readSerializable(p, String.class.getClassLoader(), String.class);
        assertEquals(s, s2);
    }
}
