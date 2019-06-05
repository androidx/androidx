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

package androidx.browser.trusted;

import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.trusted.ITrustedWebActivityService;

import androidx.browser.customtabs.EnableComponentsTestRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TrustedWebActivityServiceTest {
    @Rule
    public final ServiceTestRule mServiceRule;
    @Rule
    public final VerifiedProviderTestRule mVerifiedProvider = new VerifiedProviderTestRule();
    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestTrustedWebActivityService.class
    );

    private Context mContext;
    private ITrustedWebActivityService mService;

    public TrustedWebActivityServiceTest() {
        mServiceRule = new ServiceTestRule();
    }

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();

        Intent intent = new Intent();
        intent.setClassName(mContext.getPackageName(),
                TestTrustedWebActivityService.class.getName());
        try {
            mService = ITrustedWebActivityService.Stub.asInterface(
                    mServiceRule.bindService(intent, mConnection, Context.BIND_AUTO_CREATE));
        } catch (TimeoutException e) {
            fail();
        }
    }

    @After
    public void tearDown() {
        mServiceRule.unbindService();
        TrustedWebActivityService.setVerifiedProvider(mContext, null);
    }

    // Our ServiceConnection doesn't need to do anything since the binder is returned by the
    // ServiceRule.
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) { }
        @Override
        public void onServiceDisconnected(ComponentName componentName) { }
    };

    @Test
    public void testVerification() throws RemoteException {
        // This only works because we're in the same process as the service, otherwise this would
        // have to be called in the Service's process.
        mService.getSmallIconId();
    }

    @Test(expected = SecurityException.class)
    public void testVerificationFailure() throws RemoteException {
        mVerifiedProvider.manuallyDisable();
        mService.getSmallIconId();
    }
}
