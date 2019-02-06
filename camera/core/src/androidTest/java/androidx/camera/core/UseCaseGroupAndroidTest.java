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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.test.runner.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
public final class UseCaseGroupAndroidTest {
  private FakeUseCaseConfiguration fakeUseCaseConfiguration;
  private FakeOtherUseCaseConfiguration fakeOtherUseCaseConfiguration;
  private UseCaseGroup useCaseGroup;
  private FakeUseCase fakeUseCase;
  private FakeOtherUseCase fakeOtherUseCase;
  private final UseCaseGroup.StateChangeListener mockListener =
      Mockito.mock(UseCaseGroup.StateChangeListener.class);

  @Before
  public void setUp() {
    fakeUseCaseConfiguration =
        new FakeUseCaseConfiguration.Builder().setTargetName("fakeUseCaseConfiguration").build();
    fakeOtherUseCaseConfiguration =
        new FakeOtherUseCaseConfiguration.Builder()
            .setTargetName("fakeOtherUseCaseConfiguration")
            .build();
    useCaseGroup = new UseCaseGroup();
    fakeUseCase = new FakeUseCase(fakeUseCaseConfiguration);
    fakeOtherUseCase = new FakeOtherUseCase(fakeOtherUseCaseConfiguration);
  }

  @Test
  public void groupStartsEmpty() {
    assertThat(useCaseGroup.getUseCases()).isEmpty();
  }

  @Test
  public void newUseCaseIsAdded_whenNoneExistsInGroup() {
    assertThat(useCaseGroup.addUseCase(fakeUseCase)).isTrue();
    assertThat(useCaseGroup.getUseCases()).containsExactly(fakeUseCase);
  }

  @Test
  public void multipleUseCases_canBeAdded() {
    assertThat(useCaseGroup.addUseCase(fakeUseCase)).isTrue();
    assertThat(useCaseGroup.addUseCase(fakeOtherUseCase)).isTrue();

    assertThat(useCaseGroup.getUseCases()).containsExactly(fakeUseCase, fakeOtherUseCase);
  }

  @Test
  public void groupBecomesEmpty_afterGroupIsCleared() {
    useCaseGroup.addUseCase(fakeUseCase);
    useCaseGroup.clear();

    assertThat(useCaseGroup.getUseCases()).isEmpty();
  }

  @Test
  public void useCaseIsCleared_afterGroupIsCleared() {
    useCaseGroup.addUseCase(fakeUseCase);
    assertThat(fakeUseCase.isCleared()).isFalse();

    useCaseGroup.clear();

    assertThat(fakeUseCase.isCleared()).isTrue();
  }

  @Test
  public void useCaseRemoved_afterRemovedCalled() {
    useCaseGroup.addUseCase(fakeUseCase);

    useCaseGroup.removeUseCase(fakeUseCase);

    assertThat(useCaseGroup.getUseCases()).isEmpty();
  }

  @Test
  public void listenerOnGroupActive_ifUseCaseGroupStarted() {
    useCaseGroup.setListener(mockListener);
    useCaseGroup.start();

    verify(mockListener, times(1)).onGroupActive(useCaseGroup);
  }

  @Test
  public void listenerOnGroupInactive_ifUseCaseGroupStopped() {
    useCaseGroup.setListener(mockListener);
    useCaseGroup.stop();

    verify(mockListener, times(1)).onGroupInactive(useCaseGroup);
  }

  @Test
  public void setListener_replacesPreviousListener() {
    useCaseGroup.setListener(mockListener);
    useCaseGroup.setListener(null);

    useCaseGroup.start();
    verify(mockListener, never()).onGroupActive(useCaseGroup);
  }
}
