/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.mediarouter.media;

import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_ID;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_REGISTER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_DYNAMIC_ROUTE_CREATED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_DYNAMIC_ROUTE_DESCRIPTORS_CHANGED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_REGISTERED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_VERSION_CURRENT;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

/** Test for {@link RegisteredMediaRouteProvider}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RegisteredMediaRouteProviderTest {
    private TestContext mContext;
    private RegisteredMediaRouteProvider mProvider;
    private Messenger mReceiveMessenger;
    private HandlerThread mServiceThread;

    @Before
    public void setUp() {
        Context appContext = ApplicationProvider.getApplicationContext();
        mContext = new TestContext(appContext);
        ComponentName componentName = new ComponentName(mContext, "FakeService");
        AtomicReference<Messenger> receiveMessengerRef = new AtomicReference<>();
        mServiceThread = new HandlerThread("FakeServiceThread");
        mServiceThread.start();
        Handler serviceHandler =
                new Handler(mServiceThread.getLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        if (msg.what == CLIENT_MSG_REGISTER) {
                            receiveMessengerRef.set(msg.replyTo);
                            Message reply = Message.obtain();
                            reply.what = SERVICE_MSG_REGISTERED;
                            reply.arg1 = msg.arg1;
                            reply.arg2 = SERVICE_VERSION_CURRENT;
                            reply.obj =
                                    new MediaRouteProviderDescriptor.Builder().build().asBundle();
                            try {
                                msg.replyTo.send(reply);
                            } catch (RemoteException ignored) {
                            }
                        }
                    }
                };
        Messenger serviceMessenger = new Messenger(serviceHandler);
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mProvider = new RegisteredMediaRouteProvider(mContext, componentName);
                            mProvider.start();
                            mProvider.setDiscoveryRequest(
                                    new MediaRouteDiscoveryRequest(
                                            MediaRouteSelector.EMPTY, false));
                            mProvider.deliverDiscoveryRequestChanged();
                            mProvider.onServiceConnected(
                                    componentName, serviceMessenger.getBinder());
                        });
        PollingCheck.waitFor(3000, () -> receiveMessengerRef.get() != null);
        mReceiveMessenger = receiveMessengerRef.get();
    }

    @After
    public void tearDown() {
        if (mProvider != null) {
            getInstrumentation().runOnMainSync(() -> mProvider.stop());
        }
        if (mServiceThread != null) {
            mServiceThread.quitSafely();
        }
    }

    @Test
    public void onDynamicGroupRouteControllerCreated_withUnsolicitedRequestId_doesNotCrash()
            throws Exception {
        Message msgWithData = Message.obtain();
        msgWithData.what = SERVICE_MSG_DYNAMIC_ROUTE_CREATED;
        msgWithData.arg1 = 999;
        Bundle data = new Bundle();
        data.putString(CLIENT_DATA_ROUTE_ID, "testRouteId");
        msgWithData.obj = data;
        Message msgWithNullData = Message.obtain();
        msgWithNullData.what = SERVICE_MSG_DYNAMIC_ROUTE_CREATED;
        msgWithNullData.arg1 = 999;

        mReceiveMessenger.send(msgWithData);
        mReceiveMessenger.send(msgWithNullData);
        getInstrumentation().waitForIdleSync();

        assertThat(mProvider.getDescriptor()).isNotNull();
    }

    @Test
    public void onDynamicRouteDescriptorsChanged_withNullOrEmptyDescriptors_doesNotCrash()
            throws Exception {
        Message msgNullBundle = Message.obtain();
        msgNullBundle.what = SERVICE_MSG_DYNAMIC_ROUTE_DESCRIPTORS_CHANGED;
        msgNullBundle.arg2 = 1;
        Message msgEmptyBundle = Message.obtain();
        msgEmptyBundle.what = SERVICE_MSG_DYNAMIC_ROUTE_DESCRIPTORS_CHANGED;
        msgEmptyBundle.arg2 = 1;
        msgEmptyBundle.obj = new Bundle();

        mReceiveMessenger.send(msgNullBundle);
        mReceiveMessenger.send(msgEmptyBundle);
        getInstrumentation().waitForIdleSync();

        assertThat(mProvider.getDescriptor()).isNotNull();
    }

    private static class TestContext extends ContextWrapper {
        TestContext(Context base) {
            super(base);
        }

        @Override
        public boolean bindService(Intent service, ServiceConnection conn, int flags) {
            return true;
        }

        @Override
        public void unbindService(ServiceConnection conn) {}
    }
}
