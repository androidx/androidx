/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.volley.toolbox;

import java.io.IOException;
import java.util.Arrays;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

@SmallTest
public class PoolingByteArrayOutputStreamTest extends AndroidTestCase {
    @Override
    public void tearDown() {
        PoolingByteArrayOutputStream.setSizeLimit(0);
    }

    public void testPooledOneBuffer() throws IOException {
        PoolingByteArrayOutputStream.setSizeLimit(32768);
        writeOneBuffer();
        writeOneBuffer();
        writeOneBuffer();
    }

    public void testPooledIndividualWrites() throws IOException {
        PoolingByteArrayOutputStream.setSizeLimit(32768);
        writeBytesIndividually();
        writeBytesIndividually();
        writeBytesIndividually();
    }

    public void testUnpooled() throws IOException {
        PoolingByteArrayOutputStream.setSizeLimit(0);
        writeOneBuffer();
        writeOneBuffer();
        writeOneBuffer();
    }

    public void testUnpooledIndividualWrites() throws IOException {
        PoolingByteArrayOutputStream.setSizeLimit(0);
        writeBytesIndividually();
        writeBytesIndividually();
        writeBytesIndividually();
    }

    private void writeOneBuffer() throws IOException {
        byte[] data = new byte[16384];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xff);
        }
        PoolingByteArrayOutputStream os = new PoolingByteArrayOutputStream();
        os.write(data);

        assertTrue(Arrays.equals(data, os.toByteArray()));
    }

    private void writeBytesIndividually() {
        byte[] data = new byte[16384];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xff);
        }
        PoolingByteArrayOutputStream os = new PoolingByteArrayOutputStream();
        for (int i = 0; i < data.length; i++) {
            os.write(data[i]);
        }

        assertTrue(Arrays.equals(data, os.toByteArray()));
    }
}
