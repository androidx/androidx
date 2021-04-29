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

package androidx.car.app.constraints;

import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_GRID;
import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_LIST;
import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_PANE;
import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST;
import static androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST;
import static androidx.car.app.constraints.ConstraintManager.create;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.os.RemoteException;

import androidx.car.app.HostDispatcher;
import androidx.car.app.ICarHost;
import androidx.car.app.testing.TestCarContext;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link ConstraintManager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ConstraintManagerTest {
    @Mock
    private ICarHost mMockCarHost;
    @Mock
    private IConstraintHost.Stub mMockConstraintHost;

    private final HostDispatcher mHostDispatcher = new HostDispatcher();

    private TestCarContext mTestCarContext;

    private ConstraintManager mConstraintManager;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);

        mTestCarContext =
                TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());

        IConstraintHost hostStub =
                new IConstraintHost.Stub() {
                    @Override
                    public int getContentLimit(int contentType) throws RemoteException {
                        return mMockConstraintHost.getContentLimit(contentType);
                    }
                };
        when(mMockCarHost.getHost(any())).thenReturn(hostStub.asBinder());
        mHostDispatcher.setCarHost(mMockCarHost);

        mConstraintManager = create(mTestCarContext, mHostDispatcher);
    }

    @Test
    public void host_throwsException_returnsDefaultLimits() throws RemoteException {
        when(mMockConstraintHost.getContentLimit(anyInt())).thenThrow(new RemoteException());

        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_LIST)).isEqualTo(6);
        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_GRID)).isEqualTo(6);
        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_PLACE_LIST)).isEqualTo(6);
        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_ROUTE_LIST)).isEqualTo(3);
        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_PANE)).isEqualTo(2);
    }

    @Test
    public void host_returnLimits() throws RemoteException {
        when(mMockConstraintHost.getContentLimit(CONTENT_LIMIT_TYPE_LIST)).thenReturn(1);
        when(mMockConstraintHost.getContentLimit(CONTENT_LIMIT_TYPE_GRID)).thenReturn(2);
        when(mMockConstraintHost.getContentLimit(CONTENT_LIMIT_TYPE_PLACE_LIST)).thenReturn(3);
        when(mMockConstraintHost.getContentLimit(CONTENT_LIMIT_TYPE_ROUTE_LIST)).thenReturn(4);
        when(mMockConstraintHost.getContentLimit(CONTENT_LIMIT_TYPE_PANE)).thenReturn(5);

        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_LIST)).isEqualTo(1);
        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_GRID)).isEqualTo(2);
        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_PLACE_LIST)).isEqualTo(3);
        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_ROUTE_LIST)).isEqualTo(4);
        assertThat(mConstraintManager.getContentLimit(CONTENT_LIMIT_TYPE_PANE)).isEqualTo(5);
    }
}
