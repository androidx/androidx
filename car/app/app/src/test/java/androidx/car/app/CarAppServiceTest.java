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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.content.Intent;

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
    private static final Session TEST_SESSION = new Session() {
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
    public void onUnbind_destroysSession() {
        CarAppBinder carApp = (CarAppBinder) mCarAppService.onBind(new Intent());
        carApp.setHandshakeInfo(new HandshakeInfo("foo",
                CarAppApiLevels.getLatest()));
        carApp.onAppCreate(mMockCarHost, new Intent(), mContext.getResources().getConfiguration(),
                mMockOnDoneCallback);
        carApp.onAppStart(mMockOnDoneCallback);

        assertThat(carApp.getCurrentSession()).isNotNull();
        assertThat(mCarAppService.onUnbind(new Intent())).isTrue();

        assertThat(carApp.getCurrentSession()).isNull();
    }

    @Test
    public void onBind() {
        CarAppBinder result = (CarAppBinder) mCarAppService.onBind(new Intent());

        assertThat(result.getCurrentSessionInfo()).isEqualTo(SessionInfo.DEFAULT_SESSION_INFO);
    }

    @Test
    public void onCreateSession_withoutNewOnCreateSession_usesOldOnCreateSession() {
        ServiceController<? extends CarAppService> serviceController =
                Robolectric.buildService(TestCarAppServiceWithoutNewOnCreateSession.class);
        serviceController.get().setHostInfo(TEST_HOST_INFO);
        CarAppService oldSessionCarAppService = serviceController.create().get();

        Session result = oldSessionCarAppService.onCreateSession(SessionInfo.DEFAULT_SESSION_INFO);

        assertThat(result).isEqualTo(TEST_SESSION);
    }

    @Test
    public void onCreateSession_doesNotCallOldMethod() {
        Session result = mCarAppService.onCreateSession(SessionInfo.DEFAULT_SESSION_INFO);

        assertThat(result).isEqualTo(TEST_SESSION);
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
            return TEST_SESSION;
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
            return TEST_SESSION;
        }
    }
}
