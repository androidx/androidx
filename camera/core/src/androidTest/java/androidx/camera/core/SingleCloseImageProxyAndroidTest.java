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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.test.runner.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class SingleCloseImageProxyAndroidTest {

  private final ImageProxy imageProxy = mock(ImageProxy.class);
  private SingleCloseImageProxy singleCloseImageProxy;

  @Before
  public void setUp() {
    singleCloseImageProxy = new SingleCloseImageProxy(imageProxy);
  }

  @Test
  public void wrappedImageIsClosedOnce_whenWrappingImageIsClosedOnce() {
    singleCloseImageProxy.close();

    verify(imageProxy, times(1)).close();
  }

  @Test
  public void wrappedImageIsClosedOnce_whenWrappingImageIsClosedTwice() {
    singleCloseImageProxy.close();
    singleCloseImageProxy.close();

    verify(imageProxy, times(1)).close();
  }
}
