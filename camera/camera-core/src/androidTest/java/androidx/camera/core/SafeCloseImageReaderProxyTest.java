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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.util.Pair;

import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.impl.fakes.FakeImageReaderProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SafeCloseImageReaderProxyTest {
    private FakeImageReaderProxy mFakeImageReaderProxy;
    private SafeCloseImageReaderProxy mSafeCloseImageReaderProxy;
    private String mTagBundleKey = "FakeTagBundleKey";
    private int mTag = 0;

    @Before
    public void setup() {
        mFakeImageReaderProxy = new FakeImageReaderProxy(3);
        mSafeCloseImageReaderProxy = new SafeCloseImageReaderProxy(mFakeImageReaderProxy);
    }

    @Test
    public void imageReaderNotClosed_ifOutstandingImages() throws InterruptedException {
        mFakeImageReaderProxy.triggerImageAvailable(TagBundle.create(new Pair<>(mTagBundleKey,
                mTag)), 1);
        mSafeCloseImageReaderProxy.acquireLatestImage();

        // Close safely instead of using normal ImageReaderProxy.close()
        mSafeCloseImageReaderProxy.safeClose();

        // The ImageReaderProxy is not closed because there are still ImageProxy which have not yet
        // been closed.
        assertThat(mFakeImageReaderProxy.isClosed()).isFalse();
    }

    @Test
    public void imageReaderClosed_onceAllOutstandingImagesClosed() throws InterruptedException {
        mFakeImageReaderProxy.triggerImageAvailable(TagBundle.create(new Pair<>(mTagBundleKey,
                mTag)), 1);
        ImageProxy imageProxy = mSafeCloseImageReaderProxy.acquireLatestImage();

        // Close safely instead of using normal ImageReaderProxy.close()
        mSafeCloseImageReaderProxy.safeClose();

        imageProxy.close();

        // The ImageReaderProxy should be closed since all acquire ImageProxys have been closed
        assertThat(mFakeImageReaderProxy.isClosed()).isTrue();
    }

    @Test
    public void onImageAvailableListener_calledWhenImageTriggered() throws InterruptedException {
        // Arrange
        ImageReaderProxy.OnImageAvailableListener onImageAvailableListener = mock(
                ImageReaderProxy.OnImageAvailableListener.class);
        mSafeCloseImageReaderProxy.setOnImageAvailableListener(onImageAvailableListener,
                CameraXExecutors.directExecutor());

        // Act
        mFakeImageReaderProxy.triggerImageAvailable(TagBundle.create(new Pair<>(mTagBundleKey,
                mTag)), 1);

        // Assert
        verify(onImageAvailableListener).onImageAvailable(any(ImageReaderProxy.class));
    }

    @Test
    public void noOnImageAvailableListener_afterSafeClose() throws InterruptedException {
        // Arrange
        ImageReaderProxy.OnImageAvailableListener onImageAvailableListener = mock(
                ImageReaderProxy.OnImageAvailableListener.class);
        mSafeCloseImageReaderProxy.setOnImageAvailableListener(onImageAvailableListener,
                CameraXExecutors.directExecutor());

        // Act
        // Close safely instead of using normal ImageReaderProxy.close()
        mSafeCloseImageReaderProxy.safeClose();
        mFakeImageReaderProxy.triggerImageAvailable(TagBundle.create(new Pair<>(mTagBundleKey,
                mTag)), 1);

        // Assert
        verifyZeroInteractions(onImageAvailableListener);
    }

    @Test
    public void noOnImageAvailableListenerAfterSafeClose_withOutstandingImages()
            throws InterruptedException {
        // Arrange
        ImageReaderProxy.OnImageAvailableListener onImageAvailableListener = mock(
                ImageReaderProxy.OnImageAvailableListener.class);
        mSafeCloseImageReaderProxy.setOnImageAvailableListener(onImageAvailableListener,
                CameraXExecutors.directExecutor());

        mFakeImageReaderProxy.triggerImageAvailable(TagBundle.create(new Pair<>(mTagBundleKey,
                mTag)), 1);
        mSafeCloseImageReaderProxy.acquireNextImage();

        reset(onImageAvailableListener);

        // Act
        // Close safely instead of using normal ImageReaderProxy.close()
        mSafeCloseImageReaderProxy.safeClose();
        mFakeImageReaderProxy.triggerImageAvailable(TagBundle.create(new Pair<>(mTagBundleKey,
                mTag)), 1);

        // Assert
        verifyZeroInteractions(onImageAvailableListener);
    }

    @Test
    public void closeTheImageInQueue_onImageCloseListenerGetsInvoked() throws InterruptedException {
        // Arrange
        ForwardingImageProxy.OnImageCloseListener onImageCloseListener = mock(
                ForwardingImageProxy.OnImageCloseListener.class);
        mSafeCloseImageReaderProxy.setOnImageCloseListener(onImageCloseListener);

        // Act: send and close image.
        mFakeImageReaderProxy.triggerImageAvailable(TagBundle.create(new Pair<>(mTagBundleKey,
                mTag)), 1);
        ImageProxy imageProxy = mSafeCloseImageReaderProxy.acquireLatestImage();
        imageProxy.close();

        // Assert: the callback gets invoked
        verify(onImageCloseListener).onImageClose(any(ImageProxy.class));
    }
}
