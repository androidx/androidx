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

import android.os.Build;

import androidx.camera.testing.fakes.FakeImageInfo;
import androidx.camera.testing.fakes.FakeImageProxy;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class SettableImageProxyBundleTest {

    private ImageProxy mImageProxy0;
    private ImageProxy mImageProxy1;
    private static final int CAPTURE_ID_0 = 0;
    private static final int CAPTURE_ID_1 = 1;
    private static final int CAPTURE_ID_NONEXISTANT = 5;
    private ImageInfo mImageInfo0;
    private ImageInfo mImageInfo1;
    private static final long TIMESTAMP_0 = 10L;
    private static final long TIMESTAMP_1 = 20L;
    private List<Integer> mCaptureIdList;

    @Before
    public void setup() {
        mImageProxy0 = new FakeImageProxy();
        mImageProxy0.setTimestamp(TIMESTAMP_0);
        mImageInfo0 = new FakeImageInfo();
        ((FakeImageInfo) mImageInfo0).setTimestamp(TIMESTAMP_0);

        mImageProxy1 = new FakeImageProxy();
        mImageProxy1.setTimestamp(TIMESTAMP_1);
        mImageInfo1 = new FakeImageInfo();
        ((FakeImageInfo) mImageInfo1).setTimestamp(TIMESTAMP_1);

        mCaptureIdList = new ArrayList<>();
        mCaptureIdList.add(CAPTURE_ID_0);
        mCaptureIdList.add(CAPTURE_ID_1);
    }

    @Test
    public void synchronizeSuccess() throws InterruptedException,
            ExecutionException, TimeoutException {
        SettableImageProxyBundle bundle = new SettableImageProxyBundle(mCaptureIdList);
        bundle.addImage(mImageProxy0);
        bundle.addImageInfo(CAPTURE_ID_0, mImageInfo0);

        ListenableFuture<ImageProxy> captureResultListenableFuture =
                bundle.getImageProxy(CAPTURE_ID_0);

        ImageProxy result = captureResultListenableFuture.get(0, TimeUnit.SECONDS);

        assertThat(result.getImageInfo()).isSameAs(mImageInfo0);
        assertThat(result.getTimestamp()).isSameAs(mImageProxy0.getTimestamp());
    }

    @Test(expected = TimeoutException.class)
    public void noneToSynchronize()
            throws InterruptedException, ExecutionException, TimeoutException {
        SettableImageProxyBundle bundle = new SettableImageProxyBundle(mCaptureIdList);
        bundle.addImage(mImageProxy0);
        bundle.addImageInfo(CAPTURE_ID_1, mImageInfo1);

        ListenableFuture<ImageProxy> captureResultListenableFuture =
                bundle.getImageProxy(CAPTURE_ID_0);

        // Expect this to timeout since there is no matching result
        captureResultListenableFuture.get(0, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void noneSynchronizedWhenClosed() {
        SettableImageProxyBundle bundle = new SettableImageProxyBundle(mCaptureIdList);
        bundle.addImage(mImageProxy0);
        bundle.addImageInfo(CAPTURE_ID_0, mImageInfo0);

        bundle.close();

        bundle.getImageProxy(CAPTURE_ID_0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionWhenRetrievingImageWithInvalidCaptureId() {
        SettableImageProxyBundle bundle = new SettableImageProxyBundle(mCaptureIdList);

        // Should throw exception
        bundle.getImageProxy(CAPTURE_ID_NONEXISTANT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionWhenAddingResultWithInvalidCaptureId() {
        SettableImageProxyBundle bundle = new SettableImageProxyBundle(mCaptureIdList);

        // Should throw exception
        bundle.addImageInfo(CAPTURE_ID_NONEXISTANT, mImageInfo0);
    }
}
