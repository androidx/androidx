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
import androidx.test.runner.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
public class UseCaseGroupLifecycleControllerAndroidTest {
  private UseCaseGroupLifecycleController useCaseGroupLifecycleController;
  private FakeLifecycleOwner lifecycleOwner;
  private final UseCaseGroup.StateChangeListener mockListener =
      Mockito.mock(UseCaseGroup.StateChangeListener.class);

  @Before
  public void setUp() {
    lifecycleOwner = new FakeLifecycleOwner();
  }

  @Test
  public void groupCanBeMadeObserverOfLifecycle() {
    assertThat(lifecycleOwner.getObserverCount()).isEqualTo(0);

    useCaseGroupLifecycleController =
        new UseCaseGroupLifecycleController(lifecycleOwner.getLifecycle(), new UseCaseGroup());

    assertThat(lifecycleOwner.getObserverCount()).isEqualTo(1);
  }

  @Test
  public void groupCanStopObservingALifeCycle() {
    useCaseGroupLifecycleController =
        new UseCaseGroupLifecycleController(lifecycleOwner.getLifecycle(), new UseCaseGroup());
    assertThat(lifecycleOwner.getObserverCount()).isEqualTo(1);

    useCaseGroupLifecycleController.release();

    assertThat(lifecycleOwner.getObserverCount()).isEqualTo(0);
  }

  @Test
  public void groupCanBeReleasedMultipleTimes() {
    useCaseGroupLifecycleController =
        new UseCaseGroupLifecycleController(lifecycleOwner.getLifecycle(), new UseCaseGroup());

    useCaseGroupLifecycleController.release();
    useCaseGroupLifecycleController.release();
  }

  @Test
  public void lifecycleStart_triggersOnActive() {
    useCaseGroupLifecycleController =
        new UseCaseGroupLifecycleController(lifecycleOwner.getLifecycle(), new UseCaseGroup());
    useCaseGroupLifecycleController.getUseCaseGroup().setListener(mockListener);

    lifecycleOwner.start();

    verify(mockListener, times(1))
        .onGroupActive(useCaseGroupLifecycleController.getUseCaseGroup());
  }

  @Test
  public void lifecycleStop_triggersOnInactive() {
    useCaseGroupLifecycleController =
        new UseCaseGroupLifecycleController(lifecycleOwner.getLifecycle(), new UseCaseGroup());
    useCaseGroupLifecycleController.getUseCaseGroup().setListener(mockListener);
    lifecycleOwner.start();

    lifecycleOwner.stop();

    verify(mockListener, times(1))
        .onGroupInactive(useCaseGroupLifecycleController.getUseCaseGroup());
  }
}
