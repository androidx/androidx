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

import static org.junit.Assert.assertEquals;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ParamTypesTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private static Object sParam;
    private static CountDownLatch sLatch;

    @Test
    public void testContext() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setContext(mContext, 5));
        String pkg = mContext.getPackageName();
        assertEquals(pkg, ((Context) sParam).getPackageName());
    }

    @Test
    public void testByte() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setByte((byte) 5));
        assertEquals((byte) 5, sParam);
    }

    @Test
    public void testByte_Nullable() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setByteN((byte) 5));
        assertEquals((byte) 5, sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setByteN((Byte) null));
        assertEquals(null, sParam);
    }

    @Test
    public void testChar() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setChar((char) 5));
        assertEquals((char) 5, sParam);
    }

    @Test
    public void testCharacter() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setCharacter((char) 5));
        assertEquals((char) 5, sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setCharacter((Character) null));
        assertEquals(null, sParam);
    }

    @Test
    public void testShort() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setShort((short) 5));
        assertEquals((short) 5, sParam);
    }

    @Test
    public void testShort_Nullable() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setShortN((short) 5));
        assertEquals((short) 5, sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setShortN((Short) null));
        assertEquals(null, sParam);
    }

    @Test
    public void testInt() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setInt((int) 5));
        assertEquals((int) 5, sParam);
    }

    @Test
    public void testInteger() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setInteger((int) 5));
        assertEquals((int) 5, sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setInteger((Integer) null));
        assertEquals(null, sParam);
    }

    @Test
    public void testLong() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setLong((long) 5));
        assertEquals((long) 5, sParam);
    }

    @Test
    public void testLong_Nullable() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setLongN((long) 5));
        assertEquals((long) 5, sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setLongN((Long) null));
        assertEquals(null, sParam);
    }

    @Test
    public void testFloat() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setFloat((float) 5.5f));
        assertEquals((float) 5.5f, sParam);
    }

    @Test
    public void testFloat_Nullable() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setFloatN((float) 5.5f));
        assertEquals((float) 5.5f, sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setFloatN((Float) null));
        assertEquals(null, sParam);
    }

    @Test
    public void testDouble() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setDouble((double) 5.5f));
        assertEquals((double) 5.5f, sParam);
    }

    @Test
    public void testDouble_Nullable() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setDoubleN((double) 5.5f));
        assertEquals((double) 5.5f, sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setDoubleN((Double) null));
        assertEquals(null, sParam);
    }

    @Test
    public void testBoolean() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setBoolean((boolean) true));
        assertEquals((boolean) true, sParam);
    }

    @Test
    public void testBoolean_Nullable() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setBooleanN((boolean) true));
        assertEquals((boolean) true, sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setBooleanN((Boolean) null));
        assertEquals(null, sParam);
    }

    @Test
    public void testString() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setString("Some string"));
        assertEquals("Some string", (String) sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setString((String) null));
        assertEquals(null, sParam);
    }

    @Test
    public void testUri() throws Exception {
        run(new TypesReceiver().createRemoteCallback(mContext).setUri(
                Uri.parse("content://x/y")));
        assertEquals(Uri.parse("content://x/y"), sParam);

        run(new TypesReceiver().createRemoteCallback(mContext).setUri((Uri) null));
        assertEquals(null, sParam);
    }

    private void run(RemoteCallback callback)
            throws PendingIntent.CanceledException, InterruptedException {
        sLatch = new CountDownLatch(1);
        sParam = null;
        callback.toPendingIntent().send();
        sLatch.await(2, TimeUnit.SECONDS);
    }

    public static class TypesReceiver extends BroadcastReceiverWithCallbacks<TypesReceiver> {

        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            if (sLatch != null) sLatch.countDown();
        }

        @RemoteCallable
        public RemoteCallback setByte(byte i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setByteN(Byte i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setChar(char i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setCharacter(Character i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setShort(short i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setShortN(Short i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setInt(int i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setInteger(Integer i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setLong(long i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setLongN(Long i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setFloat(float i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setFloatN(Float i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setDouble(double i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setDoubleN(Double i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setBoolean(boolean i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setBooleanN(Boolean i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setString(String i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setUri(Uri i) {
            sParam = i;
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setContext(Context context, int id) {
            Log.d("TestTest", "setContext " + context + " " + getClass(), new Throwable());
            sParam = context;
            return RemoteCallback.LOCAL;
        }
    }
}
