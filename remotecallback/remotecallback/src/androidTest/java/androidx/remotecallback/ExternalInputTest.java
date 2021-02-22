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

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ExternalInputTest {

    private static final String EXTRA_ARG = "extra_arg";
    private static CountDownLatch sLatch;

    private static int sArg1;
    private static int sArg2;

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void testArg() throws Exception {
        run(new RemoteInputReceiver().createRemoteCallback(mContext).setMethod(0 /* unused */, 4),
                3);
        assertEquals(3, sArg1);
        assertEquals(4, sArg2);
    }

    @Test
    public void testArgString() throws Exception {
        run(new RemoteInputReceiver().createRemoteCallback(mContext).setMethodString(0/* unused */,
                4), 3);
        assertEquals(3, sArg1);
        assertEquals(4, sArg2);
    }

    private void run(RemoteCallback callback, int extraValue)
            throws PendingIntent.CanceledException, InterruptedException {
        sLatch = new CountDownLatch(1);
        sArg1 = 0;
        sArg2 = 0;
        Intent intent = new Intent()
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(EXTRA_ARG, extraValue);
        callback.toPendingIntent().send(mContext, 0, intent);
        sLatch.await(2, TimeUnit.SECONDS);
    }

    public static class RemoteInputReceiver extends
            BroadcastReceiverWithCallbacks<RemoteInputReceiver> {
        @RemoteCallable
        public RemoteCallback setMethod(@ExternalInput(EXTRA_ARG) int arg1, int arg2) {
            sArg1 = arg1;
            sArg2 = arg2;
            if (sLatch != null) sLatch.countDown();
            return RemoteCallback.LOCAL;
        }

        @RemoteCallable
        public RemoteCallback setMethodString(@ExternalInput("extra_arg") int arg1, int arg2) {
            sArg1 = arg1;
            sArg2 = arg2;
            if (sLatch != null) sLatch.countDown();
            return RemoteCallback.LOCAL;
        }

    }
}

