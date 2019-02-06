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

import androidx.camera.testing.fakes.FakeAppConfiguration;
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager;
import androidx.camera.testing.fakes.FakeCameraFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AppConfigurationRobolectricTest {

  private AppConfiguration appConfiguration;

  @Before
  public void setUp() {
    appConfiguration = FakeAppConfiguration.create();
  }

  @Test
  public void canGetConfigTarget() {
    Class<CameraX> configTarget = appConfiguration.getTargetClass(/*valueIfMissing=*/ null);
    assertThat(configTarget).isEqualTo(CameraX.class);
  }

  @Test
  public void canGetCameraFactory() {
    CameraFactory cameraFactory = appConfiguration.getCameraFactory(/*valueIfMissing=*/ null);
    assertThat(cameraFactory).isInstanceOf(FakeCameraFactory.class);
  }

  @Test
  public void canGetDeviceSurfaceManager() {
    CameraDeviceSurfaceManager surfaceManager =
        appConfiguration.getDeviceSurfaceManager(/*valueIfMissing=*/ null);
    assertThat(surfaceManager).isInstanceOf(FakeCameraDeviceSurfaceManager.class);
  }
}
