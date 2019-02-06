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
import static org.junit.Assert.assertThrows;

import android.arch.lifecycle.LifecycleOwner;
import androidx.camera.testing.fakes.FakeLifecycleOwner;
import androidx.test.runner.AndroidJUnit4;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class UseCaseGroupRepositoryAndroidTest {

  private FakeLifecycleOwner lifecycle;
  private UseCaseGroupRepository repository;
  private Map<LifecycleOwner, UseCaseGroupLifecycleController> useCasesMap;

  @Before
  public void setUp() {
    lifecycle = new FakeLifecycleOwner();
    repository = new UseCaseGroupRepository();
    useCasesMap = repository.getUseCasesMap();
  }

  @Test
  public void repositoryStartsEmpty() {
    assertThat(useCasesMap).isEmpty();
  }

  @Test
  public void newUseCaseGroupIsCreated_whenNoGroupExistsForLifecycleInRepository() {
    UseCaseGroupLifecycleController group = repository.getOrCreateUseCaseGroup(lifecycle);

    assertThat(useCasesMap).containsExactly(lifecycle, group);
  }

  @Test
  public void existingUseCaseGroupIsReturned_whenGroupExistsForLifecycleInRepository() {
    UseCaseGroupLifecycleController firstGroup = repository.getOrCreateUseCaseGroup(lifecycle);
    UseCaseGroupLifecycleController secondGroup = repository.getOrCreateUseCaseGroup(lifecycle);

    assertThat(firstGroup).isSameAs(secondGroup);
    assertThat(useCasesMap).containsExactly(lifecycle, firstGroup);
  }

  @Test
  public void differentUseCaseGroupsAreCreated_forDifferentLifecycles() {
    UseCaseGroupLifecycleController firstGroup = repository.getOrCreateUseCaseGroup(lifecycle);
    FakeLifecycleOwner secondLifecycle = new FakeLifecycleOwner();
    UseCaseGroupLifecycleController secondGroup =
        repository.getOrCreateUseCaseGroup(secondLifecycle);

    assertThat(useCasesMap).containsExactly(lifecycle, firstGroup, secondLifecycle, secondGroup);
  }

  @Test
  public void useCaseGroupObservesLifecycle() {
    repository.getOrCreateUseCaseGroup(lifecycle);

    // One observer is the use case group. The other observer removes the use case from the
    // repository when the lifecycle is destroyed.
    assertThat(lifecycle.getObserverCount()).isEqualTo(2);
  }

  @Test
  public void useCaseGroupIsRemovedFromRepository_whenLifecycleIsDestroyed() {
    repository.getOrCreateUseCaseGroup(lifecycle);
    lifecycle.destroy();

    assertThat(useCasesMap).isEmpty();
  }

  @Test
  public void useCaseIsCleared_whenLifecycleIsDestroyed() {
    UseCaseGroupLifecycleController group = repository.getOrCreateUseCaseGroup(lifecycle);
    FakeUseCase useCase = new FakeUseCase();
    group.getUseCaseGroup().addUseCase(useCase);

    assertThat(useCase.isCleared()).isFalse();

    lifecycle.destroy();

    assertThat(useCase.isCleared()).isTrue();
  }

  @Test
  public void exception_whenCreatingWithDestroyedLifecycle() {
    lifecycle.destroy();

    assertThrows(
        IllegalArgumentException.class, () -> repository.getOrCreateUseCaseGroup(lifecycle));
  }
}
