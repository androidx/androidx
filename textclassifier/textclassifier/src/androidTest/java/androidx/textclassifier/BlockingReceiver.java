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

package androidx.textclassifier;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.util.Preconditions;

import org.junit.Assert;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * BroadcastReceiver that can block for a PendingIntent.
 *
 * <p>May only be used <b>once</b> to assert the state of the PendingIntent. If no assert method is
 * called, caller must call {@link #unregister()} to unregister this broadcast receiver.
 */
final class BlockingReceiver extends BroadcastReceiver {

    public static final long TIMEOUT_MS = 500;

    private final Context mContext;
    private final String mExpectedAction;
    private final PendingIntent mIntent;
    private final CountDownLatch mLatch;

    private BlockingReceiver(Context context, String action) {
        mContext = Preconditions.checkNotNull(context);
        mExpectedAction = Preconditions.checkNotNull(action);
        mIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mExpectedAction),
                PendingIntent.FLAG_IMMUTABLE);
        mLatch = new CountDownLatch(1);
    }

    /**
     * Returns an instance of a {@link BlockingReceiver}.
     */
    public static BlockingReceiver registerForPendingIntent(Context context) {
        final String expectedAction = UUID.randomUUID().toString();
        final BlockingReceiver receiver = new BlockingReceiver(context, expectedAction);
        context.registerReceiver(receiver, new IntentFilter(expectedAction));
        return receiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(mExpectedAction)) {
            mLatch.countDown();
        }
    }

    /**
     * Returns the pending intent associated with this receiver.
     */
    public PendingIntent getPendingIntent() {
        return mIntent;
    }

    /**
     * Asserts that the pending intent was received.
     * This method may block for {@link #TIMEOUT_MS} milliseconds.
     * <p>Note</p> that this method also unregisters this broadcast receiver.
     */
    public void assertIntentReceived() throws InterruptedException {
        assertPendingIntent(true);
    }

    /**
     * Asserts that the pending intent was not received before the timeout.
     * This method may block for {@link #TIMEOUT_MS} milliseconds.
     * <p>Note</p> that this method also unregisters this broadcast receiver.
     */
    public void assertIntentNotReceived() throws InterruptedException {
        assertPendingIntent(false);
    }

    /**
     * Asserts the state of the pending intent.
     * @param expectReceived If true, will expect a pending intent before timing out.
     *      If false, will expect to timeout before receiving a pending intent
     */
    private void assertPendingIntent(boolean expectReceived) throws InterruptedException {
        try {
            if (expectReceived ^ mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                fail(expectReceived);
            }
        } finally {
            unregister();
        }
    }

    private void fail(boolean expectReceived) {
        if (expectReceived) {
            Assert.fail("Did not receive PendingIntent(action=" + mExpectedAction + ")");
        } else {
            Assert.fail("Received unexpected PendingIntent");
        }
    }

    /**
     * Unregisters this broadcast receiver.
     */
    public void unregister() {
        mContext.unregisterReceiver(this);
    }
}
