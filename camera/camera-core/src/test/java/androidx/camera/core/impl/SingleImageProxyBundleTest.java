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

import android.os.Build;

import androidx.camera.testing.impl.fakes.FakeCaptureStage;
import androidx.camera.testing.impl.fakes.FakeImageInfo;
import androidx.camera.testing.impl.fakes.FakeImageProxy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public final class SingleImageProxyBundleTest {
    @Test
    public void successfulCreationFromImageProxy() {
        FakeImageInfo imageInfo = new FakeImageInfo();
        FakeCaptureStage fakeCaptureStage = new FakeCaptureStage(0, null);
        String tagBundleKey = Integer.toString(fakeCaptureStage.hashCode());
        imageInfo.setTag(tagBundleKey, 1);
        FakeImageProxy imageProxy = new FakeImageProxy(imageInfo);

        SingleImageProxyBundle bundle = new SingleImageProxyBundle(imageProxy, tagBundleKey);

        assertThat(bundle).isNotNull();
    }

    @Test
    public void successfulCreationFromImageProxyAndCaptureId() {
        FakeImageProxy imageProxy = new FakeImageProxy(new FakeImageInfo());

        SingleImageProxyBundle bundle = new SingleImageProxyBundle(imageProxy, 1);

        assertThat(bundle).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsCreationIfNoImageInfo() {
        FakeImageProxy imageProxy = new FakeImageProxy(new FakeImageInfo());
        FakeCaptureStage fakeCaptureStage = new FakeCaptureStage(0, null);
        String tagBundleKey = Integer.toString(fakeCaptureStage.hashCode());
        imageProxy.setImageInfo(null);

        // Should throw IllegalArgumentException
        new SingleImageProxyBundle(imageProxy, tagBundleKey);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsCreationIfNoTag() {
        FakeImageProxy imageProxy = new FakeImageProxy(new FakeImageInfo());
        FakeCaptureStage fakeCaptureStage = new FakeCaptureStage(0, null);
        String tagBundleKey = Integer.toString(fakeCaptureStage.hashCode());
        FakeImageInfo imageInfo = new FakeImageInfo();
        imageInfo.setTag(tagBundleKey, null);
        imageProxy.setImageInfo(imageInfo);

        // Should throw IllegalArgumentException
        new SingleImageProxyBundle(imageProxy, tagBundleKey);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failsCreationIfTagIsNotInteger() {
        FakeImageProxy imageProxy = new FakeImageProxy(new FakeImageInfo());
        FakeCaptureStage fakeCaptureStage = new FakeCaptureStage(0, null);
        String tagBundleKey = Integer.toString(fakeCaptureStage.hashCode());
        FakeImageInfo imageInfo = new FakeImageInfo();
        imageInfo.setTag(tagBundleKey, null);
        imageProxy.setImageInfo(imageInfo);

        // Should throw IllegalArgumentException
        new SingleImageProxyBundle(imageProxy, tagBundleKey);
    }
}
