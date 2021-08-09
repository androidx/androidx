/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.content;

import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.Q;

import static androidx.core.content.PackageManagerCompat.PERMISSION_REVOCATION_DISABLED;
import static androidx.core.content.PackageManagerCompat.PERMISSION_REVOCATION_ENABLED;
import static androidx.core.content.PackageManagerCompat.UNUSED_APP_RESTRICTION_STATUS_UNKNOWN;
import static androidx.core.content.PackageManagerCompatTest.setupPermissionRevocationApps;
import static androidx.core.content.UnusedAppRestrictionsBackportService.ACTION_UNUSED_APP_RESTRICTIONS_BACKPORT_CONNECTION;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportCallback;
import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportService;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(AndroidJUnit4.class)
/** Tests for {@link UnusedAppRestrictionsBackportServiceConnection}. */
public class UnusedAppRestrictionsBackportServiceConnectionTest {

    private static final String VERIFIER_PACKAGE_NAME = "verifier.package.name";

    private Context mContext;
    private PackageManager mPackageManager = mock(PackageManager.class);

    private UnusedAppRestrictionsBackportServiceConnection mServiceConnection;
    private ResolvableFuture<Integer> mResultFuture = ResolvableFuture.create();

    private ArgumentCaptor<Intent> mIntentCaptor = ArgumentCaptor.forClass(Intent.class);

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        // Return the fake package manager, as we don't want to check for any actual apps on the
        // device with the verifier role, e.g. the Play Store.
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mServiceConnection =
                new UnusedAppRestrictionsBackportServiceConnection(mContext);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void connectToService_bindsService() {
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        mServiceConnection.connectAndFetchResult(mResultFuture);

        Intent expectedIntent = new Intent(ACTION_UNUSED_APP_RESTRICTIONS_BACKPORT_CONNECTION)
                .setPackage(VERIFIER_PACKAGE_NAME);
        verify(mContext).bindService(mIntentCaptor.capture(), eq(mServiceConnection),
                eq(Context.BIND_AUTO_CREATE));
        assertCorrectIntent(mIntentCaptor.getValue(), expectedIntent);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void connectToService_serviceAlreadyBound_throwsIllegalStateException() {
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        // Connect to service for the first time, after this the stored service should no longer be
        // null.
        mServiceConnection.connectAndFetchResult(mResultFuture);

        // Try connecting to service for the second time, should throw.
        assertThrows(IllegalStateException.class,
                () -> mServiceConnection.connectAndFetchResult(mResultFuture));
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void disconnectFromService_unbindsService() {
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        // Connect to the service, so we have something to unbind
        mServiceConnection.connectAndFetchResult(mResultFuture);

        mServiceConnection.disconnectFromService();

        verify(mContext).unbindService(mServiceConnection);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void disconnectFromService_noConnectedService_doesNothing() {
        // We never bound to a service, so there is nothing to unbind
        assertThrows(IllegalStateException.class,
                () -> mServiceConnection.disconnectFromService());
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void onServiceConnected_setsServiceAndCallsIsPermissionRevocationEnabled() {
        FakeIUnusedAppRestrictionsBackportService fakeService =
                new FakeIUnusedAppRestrictionsBackportService(
                        /* success= */ true, /* isEnabled= */ true);

        // We need to call this method to mark the service as "bound"
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        mServiceConnection.connectAndFetchResult(mResultFuture);
        mServiceConnection.onServiceConnected(new ComponentName("package", "package.ClassName"),
                fakeService);

        assertThat(mServiceConnection.mUnusedAppRestrictionsService).isEqualTo(fakeService);
        assertThat(fakeService.getCallCount()).isEqualTo(1);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void onServiceConnected_serviceReturnsIsEnabled_triggersCallbackWithEnabledResult()
            throws ExecutionException, InterruptedException {
        FakeIUnusedAppRestrictionsBackportService fakeService =
                new FakeIUnusedAppRestrictionsBackportService(
                        /* success= */ true, /* isEnabled= */ true);

        // We need to call this method to mark the service as "bound"
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        mServiceConnection.connectAndFetchResult(mResultFuture);
        mServiceConnection.onServiceConnected(new ComponentName("package", "package.ClassName"),
                fakeService);

        assertThat(mResultFuture.get()).isEqualTo(PERMISSION_REVOCATION_ENABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void onServiceConnected_serviceReturnsIsDisabled_triggersCallbackWithDisabledResult()
            throws ExecutionException, InterruptedException {
        FakeIUnusedAppRestrictionsBackportService fakeService =
                new FakeIUnusedAppRestrictionsBackportService(
                        /* success= */ true, /* isEnabled= */ false);

        // We need to call this method to mark the service as "bound"
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        mServiceConnection.connectAndFetchResult(mResultFuture);
        mServiceConnection.onServiceConnected(new ComponentName("package", "package.ClassName"),
                fakeService);

        assertThat(mResultFuture.get()).isEqualTo(PERMISSION_REVOCATION_DISABLED);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void onServiceConnected_serviceReturnsFailure_triggersCallbackWithUnknownResult()
            throws ExecutionException, InterruptedException {
        FakeIUnusedAppRestrictionsBackportService fakeService =
                new FakeIUnusedAppRestrictionsBackportService(
                        /* success= */ false, /* isEnabled= */ false);

        // We need to call this method to mark the service as "bound"
        setupPermissionRevocationApps(mPackageManager, Arrays.asList(VERIFIER_PACKAGE_NAME));
        mServiceConnection.connectAndFetchResult(mResultFuture);
        mServiceConnection.onServiceConnected(new ComponentName("package", "package.ClassName"),
                fakeService);

        assertThat(mResultFuture.get()).isEqualTo(UNUSED_APP_RESTRICTION_STATUS_UNKNOWN);
    }

    @Test
    @SdkSuppress(minSdkVersion = M, maxSdkVersion = Q)
    public void onServiceDisconnected_resetsServiceVariable() {
        mServiceConnection.onServiceDisconnected(new ComponentName("package", "package.ClassName"));

        assertThat(mServiceConnection.mUnusedAppRestrictionsService).isNull();
    }

    private void assertCorrectIntent(Intent actualIntent, Intent expectedIntent) {
        assertThat(actualIntent.getAction()).isEqualTo(expectedIntent.getAction());
        assertThat(actualIntent.getPackage()).isEqualTo(expectedIntent.getPackage());
    }

    private static class FakeIUnusedAppRestrictionsBackportService
            extends IUnusedAppRestrictionsBackportService.Stub implements IBinder {
        private boolean mSuccess;
        private boolean mEnabled;
        private int mCallCount = 0;

        FakeIUnusedAppRestrictionsBackportService(boolean success, boolean isEnabled) {
            this.mSuccess = success;
            this.mEnabled = isEnabled;
        }

        public int getCallCount() {
            return mCallCount;
        }

        @Override
        public void isPermissionRevocationEnabledForApp(
                IUnusedAppRestrictionsBackportCallback callback) throws RemoteException {
            mCallCount++;
            callback.onIsPermissionRevocationEnabledForAppResult(mSuccess, mEnabled);
        }
    }
}
