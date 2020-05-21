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
public final class UseCaseMediatorRepositoryTest {

    private FakeLifecycleOwner mLifecycle;
    private UseCaseMediatorRepository mRepository;
    private Map<LifecycleOwner, UseCaseMediatorLifecycleController> mUseCasesMap;

    @Before
    public void setUp() {
        mLifecycle = new FakeLifecycleOwner();
        mRepository = new UseCaseMediatorRepository();
        mUseCasesMap = mRepository.getUseCasesMap();
    }

    @Test
    public void repositoryStartsEmpty() {
        assertThat(mUseCasesMap).isEmpty();
    }

    @Test
    public void newUseCaseMediatorIsCreated_whenNoMediatorExistsForLifecycleInRepository() {
        UseCaseMediatorLifecycleController mediator = mRepository.getOrCreateUseCaseMediator(
                mLifecycle);

        assertThat(mUseCasesMap).containsExactly(mLifecycle, mediator);
    }

    @Test
    public void existingUseCaseMediatorIsReturned_whenMediatorExistsForLifecycleInRepository() {
        UseCaseMediatorLifecycleController firstMediator = mRepository.getOrCreateUseCaseMediator(
                mLifecycle);
        UseCaseMediatorLifecycleController secondMediator = mRepository.getOrCreateUseCaseMediator(
                mLifecycle);

        assertThat(firstMediator).isSameInstanceAs(secondMediator);
        assertThat(mUseCasesMap).containsExactly(mLifecycle, firstMediator);
    }

    @Test
    public void differentUseCaseMediatorsAreCreated_forDifferentLifecycles() {
        UseCaseMediatorLifecycleController firstMediator = mRepository.getOrCreateUseCaseMediator(
                mLifecycle);
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        UseCaseMediatorLifecycleController secondMediator =
                mRepository.getOrCreateUseCaseMediator(secondLifecycle);

        assertThat(mUseCasesMap)
                .containsExactly(mLifecycle, firstMediator, secondLifecycle, secondMediator);
    }

    @Test
    public void useCaseMediatorObservesLifecycle() {
        mRepository.getOrCreateUseCaseMediator(mLifecycle);

        // One observer is the use case mediator. The other observer removes the use case from the
        // repository when the lifecycle is destroyed.
        assertThat(mLifecycle.getObserverCount()).isEqualTo(2);
    }

    @Test
    public void useCaseMediatorIsRemovedFromRepository_whenLifecycleIsDestroyed() {
        mRepository.getOrCreateUseCaseMediator(mLifecycle);
        mLifecycle.destroy();

        assertThat(mUseCasesMap).isEmpty();
    }

    @Test
    public void useCaseIsCleared_whenLifecycleIsDestroyed() {
        UseCaseMediatorLifecycleController mediator = mRepository.getOrCreateUseCaseMediator(
                mLifecycle);
        FakeUseCase useCase = new FakeUseCase();
        mediator.getUseCaseMediator().addUseCase(useCase);

        assertThat(useCase.isCleared()).isFalse();

        mLifecycle.destroy();

        assertThat(useCase.isCleared()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void exception_whenCreatingWithDestroyedLifecycle() {
        mLifecycle.destroy();

        // Should throw IllegalArgumentException
        mRepository.getOrCreateUseCaseMediator(mLifecycle);
    }

    @Test
    public void useCaseMediatorIsStopped_whenNewLifecycleIsStarted() {
        // Starts first lifecycle and check UseCaseMediator active state is true.
        UseCaseMediatorLifecycleController firstController = mRepository.getOrCreateUseCaseMediator(
                mLifecycle);
        mLifecycle.start();
        assertThat(firstController.getUseCaseMediator().isActive()).isTrue();

        // Starts second lifecycle and check previous UseCaseMediator is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        UseCaseMediatorLifecycleController secondController =
                mRepository.getOrCreateUseCaseMediator(
                        secondLifecycle);
        secondLifecycle.start();
        assertThat(secondController.getUseCaseMediator().isActive()).isTrue();
        assertThat(firstController.getUseCaseMediator().isActive()).isFalse();
    }

    @Test
    public void useCaseMediatorOf2ndActiveLifecycleIsStarted_when1stActiveLifecycleIsStopped() {
        // Starts first lifecycle and check UseCaseMediator active state is true.
        UseCaseMediatorLifecycleController firstController = mRepository.getOrCreateUseCaseMediator(
                mLifecycle);
        mLifecycle.start();
        assertThat(firstController.getUseCaseMediator().isActive()).isTrue();

        // Starts second lifecycle and check previous UseCaseMediator is stopped.
        FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
        UseCaseMediatorLifecycleController secondController =
                mRepository.getOrCreateUseCaseMediator(
                        secondLifecycle);
        secondLifecycle.start();
        assertThat(secondController.getUseCaseMediator().isActive()).isTrue();
        assertThat(firstController.getUseCaseMediator().isActive()).isFalse();

        // Stops second lifecycle and check previous UseCaseMediator is started again.
        secondLifecycle.stop();
        assertThat(secondController.getUseCaseMediator().isActive()).isFalse();
        assertThat(firstController.getUseCaseMediator().isActive()).isTrue();
    }
}
