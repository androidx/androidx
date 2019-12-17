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
public final class UseCaseGroupTest {
    private final UseCaseGroup.StateChangeCallback mMockCallback =
            Mockito.mock(UseCaseGroup.StateChangeCallback.class);
    private UseCaseGroup mUseCaseGroup;
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
        mUseCaseGroup = new UseCaseGroup();
        mFakeUseCase = new FakeUseCase(fakeUseCaseConfig);
        mFakeOtherUseCase = new FakeOtherUseCase(fakeOtherUseCaseConfig);
    }

    @Test
    public void groupStartsEmpty() {
        assertThat(mUseCaseGroup.getUseCases()).isEmpty();
    }

    @Test
    public void newUseCaseIsAdded_whenNoneExistsInGroup() {
        assertThat(mUseCaseGroup.addUseCase(mFakeUseCase)).isTrue();
        assertThat(mUseCaseGroup.getUseCases()).containsExactly(mFakeUseCase);
    }

    @Test
    public void multipleUseCases_canBeAdded() {
        assertThat(mUseCaseGroup.addUseCase(mFakeUseCase)).isTrue();
        assertThat(mUseCaseGroup.addUseCase(mFakeOtherUseCase)).isTrue();

        assertThat(mUseCaseGroup.getUseCases()).containsExactly(mFakeUseCase, mFakeOtherUseCase);
    }

    @Test
    public void groupBecomesEmpty_afterGroupIsCleared() {
        mUseCaseGroup.addUseCase(mFakeUseCase);
        mUseCaseGroup.clear();

        assertThat(mUseCaseGroup.getUseCases()).isEmpty();
    }

    @Test
    public void useCaseIsCleared_afterGroupIsCleared() {
        mUseCaseGroup.addUseCase(mFakeUseCase);
        assertThat(mFakeUseCase.isCleared()).isFalse();

        mUseCaseGroup.clear();

        assertThat(mFakeUseCase.isCleared()).isTrue();
    }

    @Test
    public void useCaseRemoved_afterRemovedCalled() {
        mUseCaseGroup.addUseCase(mFakeUseCase);

        mUseCaseGroup.removeUseCase(mFakeUseCase);

        assertThat(mUseCaseGroup.getUseCases()).isEmpty();
    }

    @Test
    public void listenerOnGroupActive_ifUseCaseGroupStarted() {
        mUseCaseGroup.setListener(mMockCallback);
        mUseCaseGroup.start();

        verify(mMockCallback, times(1)).onGroupActive(mUseCaseGroup);
    }

    @Test
    public void listenerOnGroupInactive_ifUseCaseGroupStopped() {
        mUseCaseGroup.setListener(mMockCallback);
        mUseCaseGroup.stop();

        verify(mMockCallback, times(1)).onGroupInactive(mUseCaseGroup);
    }

    @Test
    public void setListener_replacesPreviousListener() {
        mUseCaseGroup.setListener(mMockCallback);
        mUseCaseGroup.setListener(null);

        mUseCaseGroup.start();
        verify(mMockCallback, never()).onGroupActive(mUseCaseGroup);
    }
}
