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

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class ConnectionManagerTest {

    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();
    @Mock
    private IBinder mBinder;

    private ConnectionManager mConnectionManager;
    private final VersionQueueOperation mVersionOperation =
            new VersionQueueOperation();
    private final ConnectionConfiguration mClientConfiguration =
            new ConnectionConfiguration("", "client_name", "bind_action", mVersionOperation);

    @Before
    public void setUp() {
        mConnectionManager = new ConnectionManager(
                ApplicationProvider.getApplicationContext(),
                getMainLooper());
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
        when(mBinder.isBinderAlive()).thenReturn(true);
    }

    @Test
    public void scheduleExecution_connect() {
        QueueOperation queueOperation = new FakeQueueOperation(mClientConfiguration);

        mConnectionManager.scheduleForExecution(queueOperation);
        shadowOf(getMainLooper()).idle();

        assertThat(mConnectionManager.getConnection(mClientConfiguration).mIsServiceBound).isTrue();

        shadowOf(getMainLooper()).runToEndOfTasks();
        assertThat(
                mConnectionManager.getConnection(mClientConfiguration).mIsServiceBound).isFalse();
    }

    @Test
    public void scheduleExecution_delayDisconnection() {
        QueueOperation queueOperation = new FakeQueueOperation(mClientConfiguration);

        mConnectionManager.scheduleForExecution(queueOperation);
        shadowOf(getMainLooper()).idleFor(2000, TimeUnit.MILLISECONDS);
        assertThat(mConnectionManager.getConnection(mClientConfiguration).mIsServiceBound).isTrue();
        mConnectionManager.scheduleForExecution(queueOperation);

        shadowOf(getMainLooper()).idleFor(ConnectionManager.UNBIND_IDLE_DELAY_MILLISECONDS - 2000,
                TimeUnit.MILLISECONDS);
        assertThat(mConnectionManager.getConnection(mClientConfiguration).mIsServiceBound).isTrue();

        shadowOf(getMainLooper()).runToEndOfTasks();
        assertThat(
                mConnectionManager.getConnection(mClientConfiguration).mIsServiceBound).isFalse();
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
