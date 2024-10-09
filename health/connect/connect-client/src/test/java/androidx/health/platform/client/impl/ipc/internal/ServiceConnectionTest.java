/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.platform.client.impl.ipc.internal;

import static android.os.Looper.getMainLooper;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.SettableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ServiceConnectionTest {

    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();
    @Mock
    private IBinder mBinder;

    private final FakeExecutionTracker mTracker = new FakeExecutionTracker();
    private final FakeConnectionCallback mConnectionCallback = new FakeConnectionCallback();
    private final VersionQueueOperation mVersionOperation = new VersionQueueOperation();

    private ConnectionConfiguration mClientConfiguration;
    private ServiceConnection mConnection;

    @Before
    public void setUp() {
        mClientConfiguration =
                new ConnectionConfiguration("", "client_name", "bind_action", mVersionOperation);
        Intent bindIntent =
                new Intent()
                        .setPackage(mClientConfiguration.getPackageName())
                        .setAction(mClientConfiguration.getBindAction());
        shadowOf((Application) getApplicationContext())
                .setComponentNameAndServiceForBindServiceForIntent(
                        bindIntent,
                        new ComponentName(
                                mClientConfiguration.getPackageName(),
                                mClientConfiguration.getClientName()),
                        mBinder);
        mConnection =
                new ServiceConnection(
                        ApplicationProvider.getApplicationContext(),
                        mClientConfiguration,
                        mTracker,
                        mConnectionCallback);
    }

    @Test
    public void execute_tracksExecution() {
        SettableFuture<Void> settableFuture = SettableFuture.create();
        QueueOperation queueOperation =
                new QueueOperation() {
                    @Override
                    public void execute(IBinder binder) throws RemoteException {
                    }

                    @Override
                    public void setException(Throwable exception) {
                    }

                    @Override
                    public QueueOperation trackExecution(ExecutionTracker tracker) {
                        tracker.track(settableFuture);
                        return this;
                    }

                    @Override
                    public ConnectionConfiguration getConnectionConfiguration() {
                        return null;
                    }
                };

        mConnection.execute(queueOperation);

        assertThat(mTracker.mTrackedFuture).isEqualTo(settableFuture);
    }

    @Test
    public void onServiceConnected_callsCallback() {
        mConnection.onServiceConnected(new ComponentName("package", "package.ClassName"), mBinder);

        assertThat(mConnectionCallback.mOnConnectedCalled).isTrue();
    }

    @Test
    public void onServiceDisconnected_futuresNotCanceled() {
        mConnection.onServiceDisconnected(new ComponentName("package", "package.ClassName"));

        assertThat(mTracker.mFuturesCanceled).isFalse();
    }

    @Test
    public void onNullBinding_serviceNotConnected_callsDisconnectedCallback() {
        mConnection.onNullBinding(new ComponentName("package", "package.ClassName"));

        assertThat(mConnectionCallback.mOnDisconnectedCalled).isTrue();
    }

    @Test
    public void onNullBinding_serviceConnected_doesNothing() {
        when(mBinder.isBinderAlive()).thenReturn(true);
        mConnection.connect();
        shadowOf(getMainLooper()).idle();

        mConnection.onNullBinding(new ComponentName("package", "package.ClassName"));

        assertThat(mConnection.mBinder).isNotNull();
        assertThat(mConnectionCallback.mOnDisconnectedCalled).isFalse();
    }

    @Test
    public void registerListener_serviceNotConnected_bindsService() {
        FakeQueueOperation registerListenerOperation =
                new FakeQueueOperation(
                        new ConnectionConfiguration("package", "clientName", "bindAction",
                                mVersionOperation));

        mConnection.registerListener(new ListenerKey("listener_key"), registerListenerOperation);
        shadowOf(getMainLooper()).idle();

        assertThat(mConnectionCallback.mOnConnectedCalled).isTrue();
    }

    @Test
    public void enqueueOperation_unbindableService_throwsRemoteException() {
        shadowOf((Application) getApplicationContext())
                .declareComponentUnbindable(new ComponentName(mClientConfiguration.getPackageName(),
                        mClientConfiguration.getClientName()
                ));

        FakeQueueOperation queueOperation = new FakeQueueOperation(mClientConfiguration);

        mConnection.enqueue(queueOperation);
        shadowOf(getMainLooper()).idle();

        assertThat(queueOperation.isExecuted()).isFalse();
        assertThat(queueOperation.mThrowable).isInstanceOf(RemoteException.class);
    }

    @Test
    public void clearConnection_failQueuedOperation() {
        SettableFuture<Void> settableFuture = SettableFuture.create();
        FakeQueueOperation queueOperation = new FakeQueueOperation(
                new ConnectionConfiguration("package", "clientName", "bindAction",
                        mVersionOperation));

        mConnection.enqueue(queueOperation);
        mConnection.clearConnection(new RemoteException());
        shadowOf(getMainLooper()).idle();

        assertThat(queueOperation.isExecuted()).isFalse();
        assertThat(queueOperation.mThrowable).isInstanceOf(RemoteException.class);
    }

    @Test
    public void registerListener_serviceConnected_executesOperation() {
        when(mBinder.isBinderAlive()).thenReturn(true);
        mConnection.connect();
        shadowOf(getMainLooper()).idle();
        FakeQueueOperation registerListenerOperation =
                new FakeQueueOperation(
                        new ConnectionConfiguration("package", "clientName", "bindAction",
                                mVersionOperation));

        mConnection.registerListener(new ListenerKey("listener_key"), registerListenerOperation);
        shadowOf(getMainLooper()).idle();

        assertThat(registerListenerOperation.isExecuted()).isTrue();
    }

    @Test
    public void unregisterListener_serviceNotConnected_bindsService() {
        FakeQueueOperation unregisterListenerOperation =
                new FakeQueueOperation(
                        new ConnectionConfiguration("package", "clientName", "bindAction",
                                mVersionOperation));

        mConnection.unregisterListener(
                new ListenerKey("listener_key"),
                unregisterListenerOperation);
        shadowOf(getMainLooper()).idle();

        assertThat(mConnectionCallback.mOnConnectedCalled).isTrue();
    }

    @Test
    public void unregisterListener_serviceConnected_executesOperation() {
        when(mBinder.isBinderAlive()).thenReturn(true);
        mConnection.connect();
        shadowOf(getMainLooper()).idle();
        FakeQueueOperation unregisterListenerOperation =
                new FakeQueueOperation(
                        new ConnectionConfiguration("package", "clientName", "bindAction",
                                mVersionOperation));

        mConnection.unregisterListener(
                new ListenerKey("listener_key"),
                unregisterListenerOperation);
        shadowOf(getMainLooper()).idle();

        assertThat(unregisterListenerOperation.isExecuted()).isTrue();
    }

    @Test
    public void reRegisterAllListeners_reconnectsRegisteredListeners() {
        mConnection.connect();
        FakeQueueOperation registerListenerOperation =
                new FakeQueueOperation(
                        new ConnectionConfiguration("package", "clientName", "bindAction",
                                mVersionOperation));
        mConnection.registerListener(new ListenerKey("listener_key"), registerListenerOperation);
        registerListenerOperation.mExecuted = false;
        shadowOf(getMainLooper()).idle();

        mConnection.reRegisterAllListeners();

        assertThat(registerListenerOperation.isExecuted()).isTrue();
    }

    @Test
    public void maybeReconnect_reconnectsRegisteredListener() {
        FakeQueueOperation registerListenerOperation =
                new FakeQueueOperation(
                        new ConnectionConfiguration("package", "clientName", "bindAction",
                                mVersionOperation));
        mConnection.registerListener(new ListenerKey("listener_key"), registerListenerOperation);
        shadowOf(getMainLooper()).idle();
        mConnection.clearConnection(null);

        mConnection.maybeReconnect();
        shadowOf(getMainLooper()).idle();

        assertThat(mConnection.mBinder).isNotNull();
    }

    @Test
    public void refreshServiceVersion_addsVersionRefreshToQueue() {
        mConnection.connect();
        shadowOf(getMainLooper()).idle();
        mConnection.refreshServiceVersion();

        mConnection.flushQueue();

        assertThat(mVersionOperation.mWasExecuted).isTrue();
    }

    private static class FakeConnectionCallback implements ServiceConnection.Callback {
        boolean mOnConnectedCalled;
        boolean mOnDisconnectedCalled;

        @Override
        public void onConnected(ServiceConnection connection) {
            mOnConnectedCalled = true;
        }

        @Override
        public void onDisconnected(ServiceConnection connection, long reconnectDelayMs) {
            mOnDisconnectedCalled = true;
        }

        @Override
        public boolean isBindToSelfEnabled() {
            return false;
        }
    }

    private static class FakeExecutionTracker implements ExecutionTracker {
        SettableFuture<?> mTrackedFuture;
        boolean mFuturesCanceled = false;

        @Override
        public void track(SettableFuture<?> future) {
            mTrackedFuture = future;
        }

        @Override
        public void cancelPendingFutures(Throwable throwable) {
            mFuturesCanceled = true;
        }
    }

    private static class FakeQueueOperation extends BaseQueueOperation {
        boolean mExecuted = false;
        Throwable mThrowable = null;

        FakeQueueOperation(ConnectionConfiguration connectionConfiguration) {
            super(connectionConfiguration);
        }

        @Override
        public void execute(@NonNull IBinder binder) {
            mExecuted = true;
        }

        @Override
        public void setException(@NonNull Throwable exception) {
            mThrowable = exception;
        }

        public boolean isExecuted() {
            return mExecuted;
        }
    }

    private static class VersionQueueOperation implements QueueOperation {
        public boolean mWasExecuted = false;

        @Override
        public void execute(IBinder binder) throws RemoteException {
            mWasExecuted = true;
        }

        @Override
        public void setException(Throwable exception) {
        }

        @Override
        public QueueOperation trackExecution(ExecutionTracker tracker) {
            return this;
        }

        @Override
        public ConnectionConfiguration getConnectionConfiguration() {
            return new ConnectionConfiguration("", "client_name", "bind_action", this);
        }
    }
}
