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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class SingleImageProxyBundleTest {
    @Test
    public void successfulCreationFromImageProxy() {
        FakeImageProxy imageProxy = new FakeImageProxy();
        FakeImageInfo imageInfo = new FakeImageInfo();
        imageInfo.setTag(1);
        imageProxy.setImageInfo(imageInfo);

        SingleImageProxyBundle bundle = new SingleImageProxyBundle(imageProxy);

        assertThat(bundle).isNotNull();
    }

    @Test
    public void successfulCreationFromImageProxyAndCaptureId() {
        FakeImageProxy imageProxy = new FakeImageProxy();

        SingleImageProxyBundle bundle = new SingleImageProxyBundle(imageProxy, 1);

        assertThat(bundle).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsCreationIfNoImageInfo() {
        FakeImageProxy imageProxy = new FakeImageProxy();
        imageProxy.setImageInfo(null);

        // Should throw IllegalArgumentException
        new SingleImageProxyBundle(imageProxy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsCreationIfNoTag() {
        FakeImageProxy imageProxy = new FakeImageProxy();
        FakeImageInfo imageInfo = new FakeImageInfo();
        imageInfo.setTag(null);
        imageProxy.setImageInfo(imageInfo);

        // Should throw IllegalArgumentException
        new SingleImageProxyBundle(imageProxy);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsCreationIfTagIsNotInteger() {
        FakeImageProxy imageProxy = new FakeImageProxy();
        FakeImageInfo imageInfo = new FakeImageInfo();
        imageInfo.setTag(new Object());
        imageProxy.setImageInfo(imageInfo);

        // Should throw IllegalArgumentException
        new SingleImageProxyBundle(imageProxy);
    }
}
