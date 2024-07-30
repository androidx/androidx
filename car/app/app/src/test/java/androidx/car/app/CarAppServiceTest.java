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

package androidx.car.app;

import static androidx.car.app.SessionInfo.DEFAULT_SESSION_INFO;
import static androidx.car.app.SessionInfo.DISPLAY_TYPE_CLUSTER;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.validation.HostValidator;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;


/** Tests for {@link CarAppService} and related classes for establishing a host connection. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class CarAppServiceTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private static final HostInfo TEST_HOST_INFO = new HostInfo("foo", 1);
    private static final Template TEST_RETURN_TEMPLATE =
            new PlaceListMapTemplate.Builder()
                    .setTitle("Title")
                    .setItemList(new ItemList.Builder().build())
                    .build();
    private static final SessionInfo TEST_CLUSTER_SESSION_INFO =
            new SessionInfo(DISPLAY_TYPE_CLUSTER, "test-cluster-session-id");

    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private IOnDoneCallback mMockOnDoneCallback;
    private CarAppService mCarAppService;

    @Before
    public void setUp() {
        ServiceController<? extends CarAppService> serviceController =
                Robolectric.buildService(TestCarAppService.class);
        serviceController.get().setHostInfo(TEST_HOST_INFO);
        mCarAppService = serviceController.create().get();
    }

    @Test
    public void onUnbind_destroysSessionOnly() {
        Intent bindIntent = new Intent();
        SessionInfo clusterSessionInfo = new SessionInfo(DISPLAY_TYPE_CLUSTER, "test-id");
        SessionInfoIntentEncoder.encode(clusterSessionInfo, bindIntent);
        CarAppBinder binder = bindAndStart(bindIntent);

        assertThat(binder.getCurrentSession()).isNotNull();
        assertThat(mCarAppService.getSession(clusterSessionInfo)).isNotNull();
        assertThat(binder.getCarAppService()).isNotNull();
        assertThat(mCarAppService.onUnbind(bindIntent)).isTrue();

        assertThat(binder.getCurrentSession()).isNull();
        assertThat(binder.getCarAppService()).isNotNull();
        assertThat(mCarAppService.getSession(clusterSessionInfo)).isNull();
    }

    // Tests old host with new client
    @Test
    @SuppressWarnings("deprecation") // Testing a deprecated method
    public void onUnbind_destroysDefaultSession_whenNoSessionInfoIncluded() {
        Intent bindIntent = new Intent();
        CarAppBinder binder = bindAndStart(bindIntent);

        assertThat(binder.getCurrentSession()).isNotNull();
        assertThat(mCarAppService.getCurrentSession()).isNotNull();
        assertThat(mCarAppService.onUnbind(bindIntent)).isTrue();

        assertThat(binder.getCurrentSession()).isNull();
        assertThat(mCarAppService.getCurrentSession()).isNull();
    }

    // Test encoding/decoding implementations across SDK versions
    @Test
    @Config(minSdk = Build.VERSION_CODES.P, maxSdk = Build.VERSION_CODES.Q)
    public void onBind_returnsSameBinder_forSimilarIntents() {
        // Create two intent instances with the same data
        Intent intent1 = new Intent();
        SessionInfoIntentEncoder.encode(DEFAULT_SESSION_INFO, intent1);
        Intent intent2 = new Intent();
        SessionInfoIntentEncoder.encode(DEFAULT_SESSION_INFO, intent2);

        IBinder result1 = mCarAppService.onBind(intent1);
        IBinder result2 = mCarAppService.onBind(intent2);

        assertThat(result1).isSameInstanceAs(result2);
    }

    // Test encoding/decoding implementations across SDK versions
    @Test
    @Config(minSdk = Build.VERSION_CODES.P, maxSdk = Build.VERSION_CODES.Q)
    public void onBind_returnsDifferentBinders_forUniqueIntents() {
        // Create two intent instances with different data
        SessionInfo sessionInfo1 = new SessionInfo(DISPLAY_TYPE_CLUSTER, "1");
        Intent intent1 = new Intent();
        SessionInfoIntentEncoder.encode(sessionInfo1, intent1);
        SessionInfo sessionInfo2 = new SessionInfo(DISPLAY_TYPE_CLUSTER, "2");
        Intent intent2 = new Intent();
        SessionInfoIntentEncoder.encode(sessionInfo2, intent2);

        IBinder result1 = mCarAppService.onBind(intent1);
        IBinder result2 = mCarAppService.onBind(intent2);

        assertThat(result1).isNotEqualTo(result2);
    }

    // Test encoding/decoding implementations across SDK versions
    @Test
    @Config(minSdk = Build.VERSION_CODES.P, maxSdk = Build.VERSION_CODES.Q)
    public void onBind_returnsDefaultBinder_whenNoSessionInfoSet() {
        Intent intent = new Intent();

        CarAppBinder result = (CarAppBinder) mCarAppService.onBind(intent);

        assertThat(result.getCurrentSessionInfo()).isEqualTo(DEFAULT_SESSION_INFO);
    }

    @Test
    public void onCreateSession_withoutNewOnCreateSession_usesOldOnCreateSession() {
        ServiceController<? extends CarAppService> serviceController =
                Robolectric.buildService(TestCarAppServiceWithoutNewOnCreateSession.class);
        serviceController.get().setHostInfo(TEST_HOST_INFO);
        CarAppService oldSessionCarAppService = serviceController.create().get();

        Session result = oldSessionCarAppService.onCreateSession(DEFAULT_SESSION_INFO);

        assertThat(result).isNotNull();
    }

    @Test
    public void onCreateSession_doesNotCallOldMethod() {
        Session result = mCarAppService.onCreateSession(DEFAULT_SESSION_INFO);

        assertThat(result).isNotNull();
    }

    @Test
    public void getSession() {
        Intent intent = new Intent();
        SessionInfoIntentEncoder.encode(TEST_CLUSTER_SESSION_INFO, intent);
        CarAppBinder binder = bindAndStart(intent);

        Session result = mCarAppService.getSession(TEST_CLUSTER_SESSION_INFO);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(binder.getCurrentSession());
    }

    @Test
    @SuppressWarnings("deprecation") // Testing a deprecated method
    public void getCurrentSession() {
        // Bind with a cluster session
        Intent clusterIntent = new Intent();
        SessionInfoIntentEncoder.encode(TEST_CLUSTER_SESSION_INFO, clusterIntent);
        CarAppBinder clusterBinder = bindAndStart(clusterIntent);
        // Bind with a main display session
        Intent mainScreenIntent = new Intent();
        SessionInfoIntentEncoder.encode(DEFAULT_SESSION_INFO, mainScreenIntent);
        CarAppBinder mainScreenBinder = bindAndStart(mainScreenIntent);

        Session result = mCarAppService.getCurrentSession();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mainScreenBinder.getCurrentSession());
        assertThat(result).isNotEqualTo(clusterBinder.getCurrentSession());
    }

    /**
     * Binds and runs through the binder lifecycle to start the app session passing in the test's
     * mock objects. Returns the created {@link CarAppBinder}.
     */
    private CarAppBinder bindAndStart(Intent intent) {
        CarAppBinder binder = (CarAppBinder) mCarAppService.onBind(intent);
        binder.setHandshakeInfo(
                new HandshakeInfo(TEST_HOST_INFO.getPackageName(), CarAppApiLevels.getLatest()));
        binder.onAppCreate(mMockCarHost, intent, mContext.getResources().getConfiguration(),
                mMockOnDoneCallback);
        binder.onAppStart(mMockOnDoneCallback);
        return binder;
    }

    private static Session createSession() {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                return new Screen(getCarContext()) {
                    @NonNull
                    @Override
                    public Template onGetTemplate() {
                        return TEST_RETURN_TEMPLATE;
                    }
                };
            }
        };
    }

    @Test
    public void onCreateSession_onInvalidCarAppService_throwsException() {
        ServiceController<? extends CarAppService> serviceController =
                Robolectric.buildService(TestCarAppServiceNoOnCreateSession.class);
        serviceController.get().setHostInfo(TEST_HOST_INFO);
        CarAppService invalidCarAppService = serviceController.create().get();

        try {
            invalidCarAppService.onCreateSession(SessionInfo.DEFAULT_SESSION_INFO);
            assertWithMessage("Expected CarAppService to throw an exception about implementing "
                    + "#onCreateSession(SessionInfo), but it didn't.").fail();
        } catch (RuntimeException e) {
            assertThat(e).hasMessageThat().contains("CarAppService#onCreateSession(SessionInfo)");
        }
    }

    private static class TestCarAppServiceNoOnCreateSession extends CarAppService {
        @NonNull
        @Override
        public HostValidator createHostValidator() {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        }
    }

    private static class TestCarAppServiceWithoutNewOnCreateSession extends CarAppService {
        @NonNull
        @Override
        public HostValidator createHostValidator() {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        }

        @NonNull
        @Override
        @SuppressWarnings("deprecation")
        public Session onCreateSession() {
            return createSession();
        }
    }

    private static class TestCarAppService extends CarAppService {
        @NonNull
        @Override
        public HostValidator createHostValidator() {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        }

        @NonNull
        @Override
        public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
            return createSession();
        }
    }
}
