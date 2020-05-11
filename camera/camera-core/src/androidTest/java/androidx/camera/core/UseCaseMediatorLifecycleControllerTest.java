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

import androidx.camera.core.impl.UseCaseMediator;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class UseCaseMediatorLifecycleControllerTest {
    private final UseCaseMediator.StateChangeCallback mMockCallback =
            Mockito.mock(UseCaseMediator.StateChangeCallback.class);
    private UseCaseMediatorLifecycleController mUseCaseMediatorLifecycleController;
    private FakeLifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        mLifecycleOwner = new FakeLifecycleOwner();
    }

    @Test
    public void mediatorCanBeMadeObserverOfLifecycle() {
        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);

        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseMediator());

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);
    }

    @Test
    public void mediatorCanStopObservingALifeCycle() {
        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseMediator());
        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(1);

        mUseCaseMediatorLifecycleController.release();

        assertThat(mLifecycleOwner.getObserverCount()).isEqualTo(0);
    }

    @Test
    public void mediatorCanBeReleasedMultipleTimes() {
        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseMediator());

        mUseCaseMediatorLifecycleController.release();
        mUseCaseMediatorLifecycleController.release();
    }

    @Test
    public void lifecycleStart_triggersOnActive() {
        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseMediator());
        mUseCaseMediatorLifecycleController.getUseCaseMediator().setListener(mMockCallback);

        mLifecycleOwner.start();

        verify(mMockCallback, times(1))
                .onActive(mUseCaseMediatorLifecycleController.getUseCaseMediator());
    }

    @Test
    public void lifecycleStop_triggersOnInactive() {
        mUseCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(
                        mLifecycleOwner.getLifecycle(), new UseCaseMediator());
        mUseCaseMediatorLifecycleController.getUseCaseMediator().setListener(mMockCallback);
        mLifecycleOwner.start();

        mLifecycleOwner.stop();

        verify(mMockCallback, times(1))
                .onInactive(mUseCaseMediatorLifecycleController.getUseCaseMediator());
    }
}
