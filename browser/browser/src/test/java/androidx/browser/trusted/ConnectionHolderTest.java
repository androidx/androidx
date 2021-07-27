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

package androidx.browser.trusted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for {@link ConnectionHolder}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(instrumentedPackages = { "androidx.browser.trusted" })
@DoNotInstrument
public class ConnectionHolderTest {
    private final TestWrapperFactory mWrapperFactory = new TestWrapperFactory();

    private static class TestWrapperFactory extends ConnectionHolder.WrapperFactory {
        private final TrustedWebActivityServiceConnection mService;

        TestWrapperFactory() {
            mService = Mockito.mock(TrustedWebActivityServiceConnection.class);
        }

        TrustedWebActivityServiceConnection getService() {
            return mService;
        }

        @NonNull
        @Override
        TrustedWebActivityServiceConnection create(ComponentName name, IBinder iBinder) {
            return mService;
        }
    }

    @Test
    public void futuresSetOnceConnected() {
        AtomicBoolean closed = new AtomicBoolean();
        ConnectionHolder holder = new ConnectionHolder(() -> closed.set(true), mWrapperFactory);

        ListenableFuture<TrustedWebActivityServiceConnection> future1 = holder.getServiceWrapper();
        ListenableFuture<TrustedWebActivityServiceConnection> future2 = holder.getServiceWrapper();

        assertFalse(future1.isDone());
        assertFalse(future2.isDone());

        holder.onServiceConnected(null, null);

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertFalse(closed.get());
    }

    @Test
    public void futuresExceptionOnceCancelled() {
        AtomicBoolean closed = new AtomicBoolean();
        ConnectionHolder holder = new ConnectionHolder(() -> closed.set(true), mWrapperFactory);

        ListenableFuture<TrustedWebActivityServiceConnection> future = holder.getServiceWrapper();

        assertFalse(future.isDone());

        Exception exception = new NullPointerException();
        holder.cancel(exception);
        assertTrue(closed.get());

        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertEquals(exception, e.getCause());
        } catch (InterruptedException e) {
            fail();
        }
    }

    @Test
    public void futuresExceptionOnceDisconnected() {
        AtomicBoolean closed = new AtomicBoolean();
        ConnectionHolder holder = new ConnectionHolder(() -> closed.set(true), mWrapperFactory);

        ListenableFuture<TrustedWebActivityServiceConnection> future = holder.getServiceWrapper();

        assertFalse(future.isDone());

        holder.onServiceConnected(null, null);
        assertTrue(future.isDone());
        assertFalse(closed.get());

        holder.onServiceDisconnected(null);
        assertTrue(closed.get());

        future = holder.getServiceWrapper();

        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        } catch (InterruptedException e) {
            fail();
        }
    }

    @Test
    @SuppressWarnings("deprecation") /* AsyncTask */
    public void futureSetWithCorrectObject() throws InterruptedException, RemoteException {
        final int smallIconId = 56;

        CountDownLatch methodCalledLatch = new CountDownLatch(1);
        CountDownLatch noExceptionLatch = new CountDownLatch(1);

        when(mWrapperFactory.getService().getSmallIconId()).then(invocation -> {
            methodCalledLatch.countDown();
            return smallIconId;
        });

        ConnectionHolder holder = new ConnectionHolder(() -> { }, mWrapperFactory);
        ListenableFuture<TrustedWebActivityServiceConnection> future = holder.getServiceWrapper();

        holder.onServiceConnected(null, null);

        future.addListener(() -> {
            try {
                assertEquals(smallIconId, future.get().getSmallIconId());
                noExceptionLatch.countDown();
            } catch (ExecutionException | InterruptedException | RemoteException e) {
                e.printStackTrace();
            }
        }, android.os.AsyncTask.THREAD_POOL_EXECUTOR);

        assertTrue(methodCalledLatch.await(200, TimeUnit.MILLISECONDS));
        assertTrue(noExceptionLatch.await(200, TimeUnit.MILLISECONDS));
    }
}
