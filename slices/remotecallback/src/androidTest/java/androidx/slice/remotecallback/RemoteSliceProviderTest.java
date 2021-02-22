/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.slice.remotecallback;

import static junit.framework.TestCase.assertNotNull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.remotecallback.RemoteCallable;
import androidx.remotecallback.RemoteCallback;
import androidx.slice.Slice;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SdkSuppress(minSdkVersion = 26)
@RunWith(AndroidJUnit4.class)
@MediumTest
public class RemoteSliceProviderTest {

    private static Provider sProvider;

    private static Uri sUri;
    private static String sStr;
    private static int sInt;
    private static Integer sNullableInt;
    private static CountDownLatch sLatch;

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void testRemoteCallback() {
        Uri aUri = new Uri.Builder().authority("mine").build();
        String something = "something";
        int i = 42;

        RemoteCallback callback = sProvider.createRemoteCallback(mContext).myCallbackMethod(
                aUri, something, i, null);

        assertNotNull(callback);
        assertEquals("myCallbackMethod", callback.getMethodName());
        assertEquals(Provider.class.getName(), callback.getReceiverClass());
        assertEquals(RemoteCallback.TYPE_PROVIDER, callback.getType());
    }

    @Test
    public void testCreateStatic() throws PendingIntent.CanceledException, InterruptedException {
        Uri aUri = new Uri.Builder().authority("mine").build();
        String something = "something";
        int i = 42;

        resetState();

        RemoteCallback.create(Provider.class, mContext).myCallbackMethod(
                aUri, something, i, null).toPendingIntent().send();

        sLatch.await(2, TimeUnit.SECONDS);

        assertState(0, aUri, something, i);
    }

    @Test
    public void testCreateCallback() throws PendingIntent.CanceledException, InterruptedException {
        Uri aUri = new Uri.Builder().authority("mine").build();
        String something = "something";
        int i = 42;

        resetState();

        sProvider.createRemoteCallback(mContext).myCallbackMethod(
                aUri, something, i, null).toPendingIntent().send();

        sLatch.await(2, TimeUnit.SECONDS);

        assertState(0, aUri, something, i);
    }

    @Test
    public void testOverrideNull() throws PendingIntent.CanceledException, InterruptedException {
        Uri aUri = new Uri.Builder().authority("mine").build();
        String something = "something";
        int i = 42;

        resetState();

        Intent intent = new Intent()
                .putExtra("p3", 3);
        sProvider.createRemoteCallback(mContext).myCallbackMethod(
                aUri, something, i, null).toPendingIntent().send(mContext, 0, intent);

        sLatch.await(2, TimeUnit.SECONDS);

        assertState(0, aUri, something, i);
    }

    private void resetState() {
        sLatch = new CountDownLatch(1);

        sUri = null;
        sStr = null;
        sInt = -1;
        sNullableInt = 15;
    }

    private void assertState(int count, Uri aUri, String something, int i) {
        assertEquals(count, sLatch.getCount());
        assertEquals(aUri, sUri);
        assertEquals(something, sStr);
        assertEquals(i, sInt);
        assertNull(sNullableInt);
    }

    public static class Provider extends RemoteSliceProvider<Provider> {

        @RemoteCallable
        public RemoteCallback myCallbackMethod(Uri myUri, String myStr, int myInt,
                Integer myNullableInt) {
            sUri = myUri;
            sStr = myStr;
            sInt = myInt;
            sNullableInt = myNullableInt;
            if (sLatch != null) sLatch.countDown();
            return RemoteCallback.LOCAL;
        }

        @Override
        public boolean onCreateSliceProvider() {
            sProvider = this;
            return true;
        }

        @Override
        public Slice onBindSlice(@NonNull Uri sliceUri) {
            return null;
        }
    }
}
