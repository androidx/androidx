/*
 * Copyright 2020 The Android Open Source Project
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

package android.support.mediacompat.service;

import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_SESSION_TAG;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaSessionCompat.RegistrationCallback}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaSessionCompatRegistrationCallbackTest {
    private static final String TAG = "RegistrationCallbackTest";

    private static final long TIME_OUT_MS = 3000L;

    private Context mContext;
    private MediaSessionCompat mSession;
    private MediaSessionCompat.Callback mCallback;
    private Handler mHandler;

    @Before
    public void setUp() throws Exception {
        mContext = getApplicationContext();
        mCallback = new MediaSessionCompat.Callback() {};
        mSession = new MediaSessionCompat(getApplicationContext(), TEST_SESSION_TAG, null, null);
        mHandler = new Handler(Looper.getMainLooper());
        mSession.setCallback(mCallback, mHandler);
    }

    @After
    public void tearDown() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mSession != null) {
            mSession.release();
            mSession = null;
        }
    }

    @Test
    public void registersCallback_byController_notifiesOnCallbackRegistered()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        mSession.setRegistrationCallback(
                new MediaSessionCompat.RegistrationCallback() {
                    @Override
                    public void onCallbackRegistered(int callingPid, int callingUid) {
                        if (callingPid == Process.myPid() && callingUid == Process.myUid()) {
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onCallbackUnregistered(int callingPid, int callingUid) {
                        // no-op
                    }
                }, mHandler);

        MediaControllerCompat controllerCompat =
                new MediaControllerCompat(mContext, mSession.getSessionToken());
        MediaControllerCompat.Callback controllerCallback =
                new MediaControllerCompat.Callback() {};
        controllerCompat.registerCallback(controllerCallback, mHandler);

        assertTrue(latch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void unregistersCallback_byController_notifiesOnCallbackUnregistered()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        mSession.setRegistrationCallback(
                new MediaSessionCompat.RegistrationCallback() {
                    @Override
                    public void onCallbackRegistered(int callingPid, int callingUid) {
                        // no-op
                    }

                    @Override
                    public void onCallbackUnregistered(int callingPid, int callingUid) {
                        if (callingPid == Process.myPid() && callingUid == Process.myUid()) {
                            latch.countDown();
                        }
                    }
                }, mHandler);

        MediaControllerCompat controllerCompat =
                new MediaControllerCompat(mContext, mSession.getSessionToken());
        MediaControllerCompat.Callback controllerCallback =
                new MediaControllerCompat.Callback() {};
        controllerCompat.registerCallback(controllerCallback, mHandler);
        controllerCompat.unregisterCallback(controllerCallback);

        assertTrue(latch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }
}
