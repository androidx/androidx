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

package androidx.versionedparcelable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@SmallTest
public class VersionedParcelStreamTest {

    private ByteArrayOutputStream mOutput;
    private VersionedParcelStream mOutputParcel;

    @Before
    public void setup() {
        mOutput = new ByteArrayOutputStream();
        mOutputParcel = new VersionedParcelStream(null, mOutput);
    }

    @Test
    public void testInt() {
        mOutputParcel.writeInt(42, 0);
        assertEquals(42, createInputParcel().readInt(0, 0));
    }

    @Test
    public void testBoolean() {
        mOutputParcel.writeBoolean(true, 0);
        assertEquals(true, createInputParcel().readBoolean(false, 0));
    }

    @Test
    public void testByte() {
        mOutputParcel.writeByte((byte) 5, 0);
        assertEquals((byte) 5, createInputParcel().readByte((byte) 0, 0));
    }

    @Test
    public void testString() {
        mOutputParcel.writeString("My string", 0);
        assertEquals("My string", createInputParcel().readString(null, 0));
    }

    @Test
    public void testNoException() {
        mOutputParcel.writeException(null, 0);
        assertNull(createInputParcel().readException(null, 0));
    }

    @Test
    public void testIllegalArgumentException() {
        mOutputParcel.writeException(new IllegalArgumentException(), 0);
        assertEquals(IllegalArgumentException.class,
                createInputParcel().readException(null, 0).getClass());
    }

    private VersionedParcelStream createInputParcel() {
        mOutputParcel.closeField();
        return new VersionedParcelStream(new ByteArrayInputStream(mOutput.toByteArray()), null);
    }
}
