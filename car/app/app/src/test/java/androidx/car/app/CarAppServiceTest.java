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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.validation.HostValidator;
import androidx.car.app.versioning.CarAppApiLevels;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;


/** Tests for {@link CarAppService} and related classes for establishing a host connection. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class CarAppServiceTest {
    @Mock
    ICarHost mMockCarHost;
    @Mock
    IOnDoneCallback mMockOnDoneCallback;

    private final Template mTemplate =
            new PlaceListMapTemplate.Builder()
                    .setTitle("Title")
                    .setItemList(new ItemList.Builder().build())
                    .build();

    private CarAppService mCarAppService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mCarAppService =
                new CarAppService() {
                    @Override
                    @NonNull
                    public HostValidator createHostValidator() {
                        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
                    }

                    @Override
                    @NonNull
                    public Session onCreateSession() {
                        // Recreate a new CarContext, because the previous one would have been
                        // destroyed in an unbind-rebind scenario.
                        return createTestSession();
                    }
                };
        HostInfo hostInfo = new HostInfo("foo", 1);
        mCarAppService.setHostInfo(hostInfo);
        mCarAppService.onCreate();
    }

    private Session createTestSession() {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                return new Screen(getCarContext()) {
                    @Override
                    @NonNull
                    public Template onGetTemplate() {
                        return mTemplate;
                    }
                };
            }

            @Override
            void configure(@NonNull Context baseContext,
                    @NonNull HandshakeInfo handshakeInfo,
                    @NonNull HostInfo hostInfo,
                    @NonNull ICarHost carHost,
                    @NonNull Configuration configuration) {}
        };
    }

    @Test
    public void onUnbind_destroysSession() {
        CarAppBinder carApp = (CarAppBinder) mCarAppService.onBind(null);
        carApp.setHandshakeInfo(new HandshakeInfo("foo",
                CarAppApiLevels.getLatest()));
        carApp.onAppCreate(mMockCarHost, null, new Configuration(), mMockOnDoneCallback);
        carApp.onAppStart(mMockOnDoneCallback);

        assertThat(carApp.getCurrentSession()).isNotNull();
        assertThat(mCarAppService.onUnbind(null)).isTrue();

        assertThat(carApp.getCurrentSession()).isNull();
    }
}
