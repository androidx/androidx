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

package androidx.camera.view;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.TextureView;

import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SurfaceTextureReleaseBlockingListenerTest {
    private TextureView mTextureView;
    private ListenableFuture<Void> mSurfaceReleaseFuture;
    private CallbackToFutureAdapter.Completer<Void> mCompleter;

    private SurfaceTextureReleaseBlockingListener mSurfaceTextureReleaseBlockingListener;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();

        mTextureView = new TextureView(context);
        mSurfaceReleaseFuture = CallbackToFutureAdapter.getFuture((completer -> {
            mCompleter = completer;
            return "";
        }));
        mSurfaceTextureReleaseBlockingListener =
                new SurfaceTextureReleaseBlockingListener(mTextureView);
    }

    /**
     * {@link SurfaceTexture#isReleased()} exists only on 23 and above. Is private prior to 26.
     * @see #surfaceIsValid_ifOnlyReleasedByTextureView22below() for equivalent test on API 22 and
     * below
      */
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void surfaceIsValid_ifOnlyReleasedByTextureView() {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.detachFromGLContext();
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        // Simulate the TextureView destroying the SurfaceTexture
        mSurfaceTextureReleaseBlockingListener.onSurfaceTextureDestroyed(surfaceTexture);

        assertFalse(surfaceTexture.isReleased());
    }

    /**
     * {@link SurfaceTexture#isReleased()} exists only on 23 and above. Is private prior to 26.
     * @see #surfaceIsValid_ifOnlyReleasedByTextureView() for equivalent test on API 23 and above
     */
    // See equivalent test for API >= 23 in SurfaceTextureReleaseBlockingListenerAndroidTest
    @SdkSuppress(maxSdkVersion = 22)
    @Test
    public void surfaceIsValid_ifOnlyReleasedByTextureView22below() {
        SurfaceTexture surfaceTexture = mock(SurfaceTexture.class);
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        // Simulate the TextureView destroying the SurfaceTexture
        mSurfaceTextureReleaseBlockingListener.onSurfaceTextureDestroyed(surfaceTexture);

        verify(surfaceTexture, never()).release();
    }

    /**
     * {@link SurfaceTexture#isReleased()} exists only on 23 and above. Is private prior to 26.
     * @see #surfaceIsValid_ifOnlyReleasedBySurfaceReleaseFuture22below() for equivalent test on API
     * 22 and below
     */
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void surfaceIsValid_ifOnlyReleasedBySurfaceReleaseFuture() {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.detachFromGLContext();
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        mCompleter.set(null);

        assertFalse(surfaceTexture.isReleased());

        // TODO(b/144878737) Remove when SurfaceTexture null dereference issue fixed
        mSurfaceTextureReleaseBlockingListener.onSurfaceTextureDestroyed(surfaceTexture);
    }

    /**
     * {@link SurfaceTexture#isReleased()} exists only on 23 and above. Is private prior to 26.
     * @see #surfaceIsValid_ifOnlyReleasedBySurfaceReleaseFuture() for equivalent test on API 23
     * and above
     */
    @SdkSuppress(maxSdkVersion = 22)
    @Test
    public void surfaceIsValid_ifOnlyReleasedBySurfaceReleaseFuture22below() {
        SurfaceTexture surfaceTexture = mock(SurfaceTexture.class);
        surfaceTexture.detachFromGLContext();
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        mCompleter.set(null);

        verify(surfaceTexture, never()).release();

        // TODO(b/144878737) Remove when SurfaceTexture null dereference issue fixed
        mSurfaceTextureReleaseBlockingListener.onSurfaceTextureDestroyed(surfaceTexture);
    }

    /**
     * {@link SurfaceTexture#isReleased()} exists only on 23 and above. Is private prior to 26.
     * @see #surfaceIsInvalid_ifReleasedByBothTextureViewAndSurfaceReleaseFuture22below() for
     * equivalent test on API 22 and below
     */
    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void surfaceIsInvalid_ifReleasedByBothTextureViewAndSurfaceReleaseFuture() {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.detachFromGLContext();
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        // Simulate the TextureView destroying the SurfaceTexture
        mSurfaceTextureReleaseBlockingListener.onSurfaceTextureDestroyed(surfaceTexture);
        mCompleter.set(null);

        assertTrue(surfaceTexture.isReleased());
    }

    /**
     * {@link SurfaceTexture#isReleased()} exists only on 23 and above. Is private prior to 26.
     * @see #surfaceIsInvalid_ifReleasedByBothTextureViewAndSurfaceReleaseFuture() for equivalent
     * test on API 23 and above
     */
    @SdkSuppress(maxSdkVersion = 22)
    @Test
    public void surfaceIsInvalid_ifReleasedByBothTextureViewAndSurfaceReleaseFuture22below() {
        SurfaceTexture surfaceTexture = mock(SurfaceTexture.class);
        surfaceTexture.detachFromGLContext();
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        // Simulate the TextureView destroying the SurfaceTexture
        mSurfaceTextureReleaseBlockingListener.onSurfaceTextureDestroyed(surfaceTexture);
        mCompleter.set(null);

        verify(surfaceTexture, atLeastOnce()).release();
    }

    @Test
    public void setSurfaceTextureSafely_callsSetSurfaceTextureOnTextureView() {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.detachFromGLContext();
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        assertThat(mTextureView.getSurfaceTexture()).isSameInstanceAs(surfaceTexture);

        // TODO(b/144878737) Remove when SurfaceTexture null dereference issue fixed
        mSurfaceTextureReleaseBlockingListener.onSurfaceTextureDestroyed(surfaceTexture);
    }
}
