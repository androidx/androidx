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

package androidx.media.test.service;

import static androidx.media.test.lib.TestHelperUtil.ACTION_TEST_HELPER;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.mediacompat.testlib.ITestHelperForServiceApp;

import androidx.media.MediaSession2;

/**
 * A Service that creates {@link MediaSession2} and calls its methods according to the client app's
 * requests.
 */
public class TestHelperService extends Service {

    MediaSession2 mSession2;
    ServiceBinder mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new ServiceBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (ACTION_TEST_HELPER.equals(intent.getAction())) {
            return mBinder;
        }
        return null;
    }

    private class ServiceBinder extends ITestHelperForServiceApp.Stub {
        @Override
        public Bundle getSessionToken2(String testName) throws RemoteException {
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }

            // TODO: Create the right session according to testName, and return its token here.
            mSession2 = new MediaSession2.Builder(TestHelperService.this)
                    .setPlayer(new MockPlayer(0))
                    .build();
            return mSession2.getToken().toBundle();
        }

        @Override
        public void callMediaSession2Method(int method, Bundle args) throws RemoteException {
            // TODO: Call appropriate method (mSession2.~~~)
        }

        // TODO: call(MediaPlayerBase/Agent)Method may be also needed.
        // If so, do not create mPlayer/mAgent, but get them from mSession.getPlayer()/getAgent().
    }
}
