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

package androidx.remotecallback;

import static androidx.remotecallback.RemoteCallback.LOCAL;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ArrayParamTypesTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private static Object sParam;
    private static CountDownLatch sLatch;

    @Test
    public void testByte() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setByte(new byte[]{(byte) 5}));
        assertArrayEquals(new byte[]{(byte) 5}, (byte[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setByte(null));
        assertEquals(null, sParam);
    }

    @Test
    public void testChar() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setChar(new char[]{(char) 5}));
        assertArrayEquals(new char[]{(char) 5}, (char[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setChar(null));
        assertEquals(null, sParam);
    }

    @Test
    public void testShort() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setShort(
                new short[]{(short) 5}));
        assertArrayEquals(new short[]{(short) 5}, (short[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setShort(null));
        assertEquals(null, sParam);
    }

    @Test
    public void testInt() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setInt(new int[]{(int) 5}));
        assertArrayEquals(new int[]{(int) 5}, (int[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setInt(null));
        assertEquals(null, sParam);
    }

    @Test
    public void testLong() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setLong(new long[]{(long) 5}));
        assertArrayEquals(new long[]{(long) 5}, (long[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setLong(null));
        assertEquals(null, sParam);
    }

    @Test
    public void testFloat() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setFloat(
                new float[]{(float) 5.5f}));
        assertArrayEquals(new float[]{(float) 5.5f}, (float[]) sParam, .1f);

        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setFloat(null));
        assertEquals(null, sParam);
    }

    @Test
    public void testDouble() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setDouble(
                new double[]{(double) 5.5}));
        assertArrayEquals(new double[]{(double) 5.5}, (double[]) sParam, .1);

        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setDouble(null));
        assertEquals(null, sParam);
    }

    @Test
    public void testBoolean() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setBoolean(
                new boolean[]{true, false}));
        assertArrayEquals(new boolean[]{true, false}, (boolean[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setBoolean(null));
        assertEquals(null, sParam);
    }

    @Test
    public void testString() throws Exception {
        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setString(new String[]{
                "Some string", null, "other"}));
        assertArrayEquals(new String[]{"Some string", null, "other"}, (String[]) sParam);

        run(new ArrayTypesReceiver().createRemoteCallback(mContext).setString(null));
        assertEquals(null, sParam);
    }

    private void run(RemoteCallback callback)
            throws PendingIntent.CanceledException, InterruptedException {
        sLatch = new CountDownLatch(1);
        sParam = null;
        callback.toPendingIntent().send();
        sLatch.await(2, TimeUnit.SECONDS);
    }

    public static class ArrayTypesReceiver extends
            BroadcastReceiverWithCallbacks<ArrayTypesReceiver> {

        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            if (sLatch != null) sLatch.countDown();
        }

        @RemoteCallable
        public RemoteCallback setByte(byte[] i) {
            sParam = i;
            return LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setChar(char[] i) {
            sParam = i;
            return LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setShort(short[] i) {
            sParam = i;
            return LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setInt(int[] i) {
            sParam = i;
            return LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setLong(long[] i) {
            sParam = i;
            return LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setFloat(float[] i) {
            sParam = i;
            return LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setDouble(double[] i) {
            sParam = i;
            return LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setBoolean(boolean[] i) {
            sParam = i;
            return LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setString(String[] i) {
            sParam = i;
            return LOCAL;
        }
    }
}
