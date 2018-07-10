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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class ServiceManagerTest {
    private static final ComponentName SERVICE_COMPONENT = new ComponentName("dummy", "dummy");

    private ServiceManager mServiceManager;

    @Mock
    private Context mContext;

    @Mock
    private IBinder mBinder;

    private Handler mHandler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        // A handler that executes message immediately.
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public boolean sendMessageAtTime(Message m, long l) {
                m.getCallback().run();
                return true;
            }
        };

        mServiceManager = new ServiceManager(
                mContext, SERVICE_COMPONENT.getPackageName(), mHandler);
    }

    @Test
    public void testBindAndAwait_success() throws InterruptedException {
        assertBindSuccess();
    }

    @Test
    public void testBindAndAwait_bindServiceReturnFalse() throws InterruptedException {
        setupBindService(false, new ServiceConnectionCallback() {
            @Override
            public void onAvailable(ServiceConnection serviceConnection) {
                serviceConnection.onServiceConnected(SERVICE_COMPONENT, mBinder);
            }
        });
        ITextClassifierService service = mServiceManager.bindAndAwait();
        assertThat(service).isNull();
        assertThat(mServiceManager.isBound()).isFalse();
    }

    @Test
    public void testBindAndAwait_onServiceDisconnected() {
        ServiceConnection serviceConnection = assertBindSuccess();

        serviceConnection.onServiceDisconnected(SERVICE_COMPONENT);
        assertThat(mServiceManager.isBound()).isFalse();

        // rebind after onServiceDisconnected.
        assertBindSuccess();
    }

    @Test
    public void testBindAndAwait_scheduleUnbind() {
        ServiceConnection serviceConnection = assertBindSuccess();
        mServiceManager.scheduleUnbind();
        Mockito.verify(mContext).unbindService(serviceConnection);
        assertThat(mServiceManager.isBound()).isFalse();
    }

    private ServiceConnection assertBindSuccess() {
        ArgumentCaptor<ServiceConnection> captor = setupBindService(true,
                new ServiceConnectionCallback() {
                    @Override
                    public void onAvailable(ServiceConnection serviceConnection) {
                        serviceConnection.onServiceConnected(SERVICE_COMPONENT, mBinder);
                    }
                });
        ITextClassifierService service = mServiceManager.bindAndAwait();
        assertThat(service).isNotNull();
        assertThat(mServiceManager.isBound()).isTrue();
        return captor.getValue();
    }

    /**
     * Mocks the result of {@link Context#bindService(Intent, ServiceConnection, int)}.
     */
    private ArgumentCaptor<ServiceConnection> setupBindService(
            final boolean bindResult, final ServiceConnectionCallback serviceConnectionCallback) {
        final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        final ArgumentCaptor<ServiceConnection> captor =
                ArgumentCaptor.forClass(ServiceConnection.class);
        when(mContext.bindService(
                any(Intent.class), captor.capture(), anyInt())).then(new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable {
                        mainThreadHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ServiceConnection serviceConnection = captor.getValue();
                                serviceConnectionCallback.onAvailable(serviceConnection);
                            }
                        }, 500);
                        return bindResult;
                    }
                });
        return captor;
    }

    interface ServiceConnectionCallback {
        void onAvailable(ServiceConnection serviceConnection);
    }
}
