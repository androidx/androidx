/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.media.Image;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.impl.ImageProxyBundle;
import androidx.camera.core.impl.SingleImageProxyBundle;
import androidx.camera.extensions.impl.PreviewImageProcessorImpl;
import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class AdaptingPreviewProcessorTest {
    private AdaptingPreviewProcessor mAdaptingPreviewProcessor;
    private PreviewImageProcessorImpl mImpl;
    private ImageProxyBundle mImageProxyBundle;
    private String mTagBundleKey = "FakeTagBundleKey";

    @Before
    public void setup() {
        mImpl = mock(PreviewImageProcessorImpl.class);

        FakeImageInfo fakeImageInfo = new FakeImageInfo();
        // Use the key which SingleImageProxyBundle is used to get tag.
        fakeImageInfo.setTag(mTagBundleKey, 1);

        FakeImageProxy fakeImageProxy = new FakeImageProxy(fakeImageInfo);
        fakeImageProxy.setImage(mock(Image.class));

        mImageProxyBundle = new SingleImageProxyBundle(fakeImageProxy, mTagBundleKey);
        mAdaptingPreviewProcessor = new AdaptingPreviewProcessor(mImpl);
    }

    @Test
    public void processDoesNotCallImplAfterClose() {
        mAdaptingPreviewProcessor.close();
        mAdaptingPreviewProcessor.process(mImageProxyBundle);
        mAdaptingPreviewProcessor.onInit();

        verifyZeroInteractions(mImpl);
    }

    @Test
    public void onImageFormatUpdateDoesNotCallImplAfterClose() {
        mAdaptingPreviewProcessor.close();
        mAdaptingPreviewProcessor.onOutputSurface(mock(Surface.class), 0);
        mAdaptingPreviewProcessor.onInit();

        verifyZeroInteractions(mImpl);
    }

    @Test
    public void onResolutionUpdateDoesNotCallImplAfterClose() {
        mAdaptingPreviewProcessor.close();
        mAdaptingPreviewProcessor.onResolutionUpdate(new Size(640, 480));
        mAdaptingPreviewProcessor.onInit();

        verifyZeroInteractions(mImpl);
    }

    @Test
    public void processCanCallImplBeforeDeInit() {
        callOnInitAndVerify();
        mAdaptingPreviewProcessor.process(mImageProxyBundle);
        verify(mImpl, times(1)).process(any(), any());
    }

    @Test
    public void processDoesNotCallImplAfterDeInit() {
        callOnInitAndVerify();
        mAdaptingPreviewProcessor.onDeInit();
        mAdaptingPreviewProcessor.process(mImageProxyBundle);

        verifyZeroInteractions(mImpl);
    }

    private void callOnInitAndVerify() {
        mAdaptingPreviewProcessor.onInit();

        InOrder inOrderImpl = inOrder(mImpl);
        inOrderImpl.verify(mImpl).onResolutionUpdate(any());
        inOrderImpl.verify(mImpl).onOutputSurface(any(), anyInt());
        inOrderImpl.verify(mImpl).onImageFormatUpdate(anyInt());
    }
}
