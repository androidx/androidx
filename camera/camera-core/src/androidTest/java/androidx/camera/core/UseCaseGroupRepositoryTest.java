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

import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class UseCaseGroupRepositoryTest {

    private FakeLifecycleOwner mLifecycle;
    private UseCaseGroupRepository mRepository;
    private Map<LifecycleOwner, UseCaseGroupLifecycleController> mUseCasesMap;

    @Before
    public void setUp() {
        mLifecycle = new FakeLifecycleOwner();
        mRepository = new UseCaseGroupRepository();
        mUseCasesMap = mRepository.getUseCasesMap();
    }

    @Test
    public void repositoryStartsEmpty() {
        assertThat(mUseCasesMap).isEmpty();
    }

    @Test
    public void newUseCaseGroupIsCreated_whenNoGroupExistsForLifecycleInRepository() {
        UseCaseGroupLifecycleController group = mRepository.getOrCreateUseCaseGroup(mLifecycle);

        assertThat(mUseCasesMap).containsExactly(mLifecycle, group);
    }

    @Test
    public void existingUseCaseGroupIsReturned_whenGroupExistsForLifecycleInRepository() {
        UseCaseGroupLifecycleController firstGroup = mRepository.getOrCreateUseCaseGroup(
                mLifecycle);
        UseCaseGroupLifecycleController secondGroup = mRepository.getOrCreateUseCaseGroup(
                mLifecycle);

        assertThat(firstGroup).isSameInstanceAs(secondGroup);
        assertThat(mUseCasesMap).containsExactly(mLifecycle, firstGroup);
    }

    @Test
    public void differentUseCaseGroupsAreCreated_forDifferentLifecycles() {
        UseCaseGroupLifecycleController firstGroup = mRepository.getOrCreateUseCaseGroup(
                mLifecycle);
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        UseCaseGroupLifecycleController secondGroup =
                mRepository.getOrCreateUseCaseGroup(secondLifecycle);

        assertThat(mUseCasesMap)
                .containsExactly(mLifecycle, firstGroup, secondLifecycle, secondGroup);
    }

    @Test
    public void useCaseGroupObservesLifecycle() {
        mRepository.getOrCreateUseCaseGroup(mLifecycle);

        // One observer is the use case group. The other observer removes the use case from the
        // repository when the lifecycle is destroyed.
        assertThat(mLifecycle.getObserverCount()).isEqualTo(2);
    }

    @Test
    public void useCaseGroupIsRemovedFromRepository_whenLifecycleIsDestroyed() {
        mRepository.getOrCreateUseCaseGroup(mLifecycle);
        mLifecycle.destroy();

        assertThat(mUseCasesMap).isEmpty();
    }

    @Test
    public void useCaseIsCleared_whenLifecycleIsDestroyed() {
        UseCaseGroupLifecycleController group = mRepository.getOrCreateUseCaseGroup(mLifecycle);
        FakeUseCase useCase = new FakeUseCase();
        group.getUseCaseGroup().addUseCase(useCase);

        assertThat(useCase.isCleared()).isFalse();

        mLifecycle.destroy();

        assertThat(useCase.isCleared()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void exception_whenCreatingWithDestroyedLifecycle() {
        mLifecycle.destroy();

        // Should throw IllegalArgumentException
        mRepository.getOrCreateUseCaseGroup(mLifecycle);
    }

    @Test
    public void useCaseGroupIsStopped_whenNewLifecycleIsStarted() {
        // Starts first lifecycle and check UseCaseGroup active state is true.
        UseCaseGroupLifecycleController firstController = mRepository.getOrCreateUseCaseGroup(
                mLifecycle);
        mLifecycle.start();
        assertThat(firstController.getUseCaseGroup().isActive()).isTrue();

        // Starts second lifecycle and check previous UseCaseGroup is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        UseCaseGroupLifecycleController secondController = mRepository.getOrCreateUseCaseGroup(
                secondLifecycle);
        secondLifecycle.start();
        assertThat(secondController.getUseCaseGroup().isActive()).isTrue();
        assertThat(firstController.getUseCaseGroup().isActive()).isFalse();
    }

    @Test
    public void useCaseGroupOf2ndActiveLifecycleIsStarted_when1stActiveLifecycleIsStopped() {
        // Starts first lifecycle and check UseCaseGroup active state is true.
        UseCaseGroupLifecycleController firstController = mRepository.getOrCreateUseCaseGroup(
                mLifecycle);
        mLifecycle.start();
        assertThat(firstController.getUseCaseGroup().isActive()).isTrue();

        // Starts second lifecycle and check previous UseCaseGroup is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        UseCaseGroupLifecycleController secondController = mRepository.getOrCreateUseCaseGroup(
                secondLifecycle);
        secondLifecycle.start();
        assertThat(secondController.getUseCaseGroup().isActive()).isTrue();
        assertThat(firstController.getUseCaseGroup().isActive()).isFalse();

        // Stops second lifecycle and check previous UseCaseGroup is started again.
        secondLifecycle.stop();
        assertThat(secondController.getUseCaseGroup().isActive()).isFalse();
        assertThat(firstController.getUseCaseGroup().isActive()).isTrue();
    }
}
