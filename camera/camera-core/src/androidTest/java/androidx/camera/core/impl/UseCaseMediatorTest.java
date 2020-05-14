/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.camera.core.FakeOtherUseCase;
import androidx.camera.core.FakeOtherUseCaseConfig;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class UseCaseMediatorTest {
    private final UseCaseMediator.StateChangeCallback mMockCallback =
            Mockito.mock(UseCaseMediator.StateChangeCallback.class);
    private UseCaseMediator mUseCaseMediator;
    private FakeUseCase mFakeUseCase;
    private FakeOtherUseCase mFakeOtherUseCase;

    @Before
    public void setUp() {
        FakeUseCaseConfig fakeUseCaseConfig = new FakeUseCaseConfig.Builder()
                .setTargetName("fakeUseCaseConfig")
                .getUseCaseConfig();
        FakeOtherUseCaseConfig fakeOtherUseCaseConfig =
                new FakeOtherUseCaseConfig.Builder()
                        .setTargetName("fakeOtherUseCaseConfig")
                        .getUseCaseConfig();
        mUseCaseMediator = new UseCaseMediator();
        mFakeUseCase = new FakeUseCase(fakeUseCaseConfig);
        mFakeOtherUseCase = new FakeOtherUseCase(fakeOtherUseCaseConfig);
    }

    @Test
    public void mediatorStartsEmpty() {
        assertThat(mUseCaseMediator.getUseCases()).isEmpty();
    }

    @Test
    public void newUseCaseIsAdded_whenNoneExistsInMediator() {
        assertThat(mUseCaseMediator.addUseCase(mFakeUseCase)).isTrue();
        assertThat(mUseCaseMediator.getUseCases()).containsExactly(mFakeUseCase);
    }

    @Test
    public void multipleUseCases_canBeAdded() {
        assertThat(mUseCaseMediator.addUseCase(mFakeUseCase)).isTrue();
        assertThat(mUseCaseMediator.addUseCase(mFakeOtherUseCase)).isTrue();

        assertThat(mUseCaseMediator.getUseCases()).containsExactly(mFakeUseCase, mFakeOtherUseCase);
    }

    @Test
    public void mediatorBecomesEmpty_afterMediatorIsCleared() {
        mUseCaseMediator.addUseCase(mFakeUseCase);
        mUseCaseMediator.destroy();

        assertThat(mUseCaseMediator.getUseCases()).isEmpty();
    }

    @Test
    public void useCaseIsCleared_afterMediatorIsCleared() {
        mUseCaseMediator.addUseCase(mFakeUseCase);
        assertThat(mFakeUseCase.isCleared()).isFalse();

        mUseCaseMediator.destroy();

        assertThat(mFakeUseCase.isCleared()).isTrue();
    }

    @Test
    public void useCaseRemoved_afterRemovedCalled() {
        mUseCaseMediator.addUseCase(mFakeUseCase);

        mUseCaseMediator.removeUseCase(mFakeUseCase);

        assertThat(mUseCaseMediator.getUseCases()).isEmpty();
    }

    @Test
    public void listenerOnMediatorActive_ifUseCaseMediatorStarted() {
        mUseCaseMediator.setListener(mMockCallback);
        mUseCaseMediator.start();

        verify(mMockCallback, times(1)).onActive(mUseCaseMediator);
    }

    @Test
    public void listenerOnMediatorInactive_ifUseCaseMediatorStopped() {
        mUseCaseMediator.setListener(mMockCallback);
        mUseCaseMediator.stop();

        verify(mMockCallback, times(1)).onInactive(mUseCaseMediator);
    }

    @Test
    public void setListener_replacesPreviousListener() {
        mUseCaseMediator.setListener(mMockCallback);
        mUseCaseMediator.setListener(null);

        mUseCaseMediator.start();
        verify(mMockCallback, never()).onActive(mUseCaseMediator);
    }
}
