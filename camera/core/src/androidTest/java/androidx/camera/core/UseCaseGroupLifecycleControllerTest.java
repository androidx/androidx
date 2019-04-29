/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class UseCaseGroupLifecycleControllerTest {
    private final UseCaseGroup.StateChangeListener mMockListener =
            Mockito.mock(UseCaseGroup.StateChangeListener.class);
    private UseCaseGroupLifecycleController mUseCaseGroupLifecycleController;
    private FakeLifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        mLifecycleOwner = new FakeLifecycleOwner();
    }

    @Test
    public void groupCanBeMadeObserverOfLifecycle() {
        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);

        mUseCaseGroupLifecycleController =
                new UseCaseGroupLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseGroup());

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);
    }

    @Test
    public void groupCanStopObservingALifeCycle() {
        mUseCaseGroupLifecycleController =
                new UseCaseGroupLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseGroup());
        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);

        mUseCaseGroupLifecycleController.release();

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);
    }

    @Test
    public void groupCanBeReleasedMultipleTimes() {
        mUseCaseGroupLifecycleController =
                new UseCaseGroupLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseGroup());

        mUseCaseGroupLifecycleController.release();
        mUseCaseGroupLifecycleController.release();
    }

    @Test
    public void lifecycleStart_triggersOnActive() {
        mUseCaseGroupLifecycleController =
                new UseCaseGroupLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseGroup());
        mUseCaseGroupLifecycleController.getUseCaseGroup().setListener(mMockListener);

        mLifecycleOwner.start();

        verify(mMockListener, times(1))
                .onGroupActive(mUseCaseGroupLifecycleController.getUseCaseGroup());
    }

    @Test
    public void lifecycleStop_triggersOnInactive() {
        mUseCaseGroupLifecycleController =
                new UseCaseGroupLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseGroup());
        mUseCaseGroupLifecycleController.getUseCaseGroup().setListener(mMockListener);
        mLifecycleOwner.start();

        mLifecycleOwner.stop();

        verify(mMockListener, times(1))
                .onGroupInactive(mUseCaseGroupLifecycleController.getUseCaseGroup());
    }
}
