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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class TextureViewImplementationTest {

    private static final int ANY_WIDTH = 1600;
    private static final int ANY_HEIGHT = 1200;
    private static final Size ANY_SIZE = new Size(ANY_WIDTH, ANY_HEIGHT);

    private FrameLayout mParent;
    private TextureViewImplementation mImplementation;
    private SurfaceTexture mSurfaceTexture;
    private ListenableFuture<Void> mSurfaceSafeToReleaseFuture;
    private CallbackToFutureAdapter.Completer<Void> mSurfaceSafeToReleaseCompleter;

    @Before
    public void setUp() {
        final Context mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mSurfaceTexture = new SurfaceTexture(0);
        mImplementation = new TextureViewImplementation();
        mParent = new FrameLayout(mContext);
        mImplementation.init(mParent);
    }

    @After
    public void tearDown() {
        if (mSurfaceSafeToReleaseFuture != null) {
            mSurfaceSafeToReleaseCompleter.set(null);
            mSurfaceSafeToReleaseFuture = null;
        }
    }

    @LargeTest
    @Test(expected = TimeoutException.class)
    public void doNotProvideSurface_ifSurfaceTextureNotAvailableYet() throws Exception {
        mImplementation.getSurfaceProvider()
                .provideSurface(ANY_SIZE, getSurfaceSafeToReleaseFuture())
                .get(2, TimeUnit.SECONDS);
    }

    @Test
    public void provideSurface_ifSurfaceTextureAvailable() throws Exception {
        final ListenableFuture<Surface> surfaceListenableFuture =
                mImplementation.getSurfaceProvider()
                        .provideSurface(ANY_SIZE, getSurfaceSafeToReleaseFuture());

        mImplementation.mTextureView
                .getSurfaceTextureListener()
                .onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        final Surface surface = surfaceListenableFuture.get();
        assertThat(surface).isNotNull();
        assertThat(mImplementation.mSurfaceCompleter).isNull();
    }

    @Test
    public void doNotDestroySurface_whenSurfaceTextureBeingDestroyed_andCameraUsingSurface()
            throws Exception {
        final ListenableFuture<Surface> surfaceListenableFuture =
                mImplementation.getSurfaceProvider()
                        .provideSurface(ANY_SIZE, getSurfaceSafeToReleaseFuture());

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        surfaceListenableFuture.get();

        assertThat(mImplementation.mSurfaceReleaseFuture).isNotNull();
        assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture)).isFalse();
    }

    @Test
    @LargeTest
    public void destroySurface_whenSurfaceTextureBeingDestroyed_andCameraNotUsingSurface()
            throws Exception {
        final ListenableFuture<Surface> surfaceListenableFuture =
                mImplementation.getSurfaceProvider()
                        .provideSurface(ANY_SIZE, getSurfaceSafeToReleaseFuture());

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        surfaceListenableFuture.get();
        mSurfaceSafeToReleaseCompleter.set(null);

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1_000);

        assertThat(mImplementation.mSurfaceReleaseFuture).isNull();
        assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture)).isTrue();
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    @LargeTest
    public void releaseSurfaceTexture_afterSurfaceTextureDestroyed_andCameraNoLongerUsingSurface_1()
            throws Exception {
        final ListenableFuture<Surface> surfaceListenableFuture =
                mImplementation.getSurfaceProvider()
                        .provideSurface(ANY_SIZE, getSurfaceSafeToReleaseFuture());

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        surfaceListenableFuture.get();

        surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture);
        mSurfaceSafeToReleaseCompleter.set(null);

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1_000);

        assertThat(mImplementation.mSurfaceReleaseFuture).isNull();
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @LargeTest
    public void releaseSurfaceTexture_afterSurfaceTextureDestroyed_andCameraNoLongerUsingSurface_2()
            throws Exception {
        final ListenableFuture<Surface> surfaceListenableFuture =
                mImplementation.getSurfaceProvider()
                        .provideSurface(ANY_SIZE, getSurfaceSafeToReleaseFuture());

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        surfaceListenableFuture.get();

        surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture);
        mSurfaceSafeToReleaseCompleter.set(null);

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1_000);

        assertThat(mImplementation.mSurfaceReleaseFuture).isNull();
        assertThat(mSurfaceTexture.isReleased()).isTrue();
    }

    @Test
    @LargeTest
    public void nullSurfaceCompleterAndSurfaceReleaseFuture_whenSurfaceProviderCancelled()
            throws Exception {
        mImplementation.getSurfaceProvider()
                .provideSurface(ANY_SIZE, getSurfaceSafeToReleaseFuture())
                .cancel(true);

        // Wait enough time for mCompleter's cancellation listener to be called
        Thread.sleep(1_000);

        assertThat(mImplementation.mSurfaceCompleter).isNull();
        assertThat(mImplementation.mSurfaceReleaseFuture).isNull();
    }

    @Test
    public void releaseSurface_whenSurfaceTextureDestroyed_andCameraSurfaceRequestIsCancelled() {
        mImplementation.getSurfaceProvider().provideSurface(ANY_SIZE,
                getSurfaceSafeToReleaseFuture());
        mSurfaceSafeToReleaseCompleter.setCancelled();

        final TextureView.SurfaceTextureListener surfaceTextureListener =
                mImplementation.mTextureView.getSurfaceTextureListener();
        surfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        assertThat(surfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture)).isTrue();
        assertThat(mImplementation.mSurfaceTexture).isNull();
    }

    @Test
    public void doNotCreateTextureView_beforeSensorOutputSizeKnown() {
        assertThat(mParent.getChildCount()).isEqualTo(0);
    }

    @Test
    public void keepOnlyLatestTextureView_whenGetSurfaceProviderCalledMultipleTimes() {
        mImplementation.getSurfaceProvider().provideSurface(ANY_SIZE,
                getSurfaceSafeToReleaseFuture());

        assertThat(mParent.getChildAt(0)).isInstanceOf(TextureView.class);
        final TextureView textureView1 = (TextureView) mParent.getChildAt(0);

        mImplementation.getSurfaceProvider().provideSurface(ANY_SIZE,
                getSurfaceSafeToReleaseFuture());

        assertThat(mParent.getChildAt(0)).isInstanceOf(TextureView.class);
        final TextureView textureView2 = (TextureView) mParent.getChildAt(0);

        assertThat(textureView1).isNotSameInstanceAs(textureView2);
        assertThat(mParent.getChildCount()).isEqualTo(1);
    }

    private ListenableFuture<Void> getSurfaceSafeToReleaseFuture() {
        if (mSurfaceSafeToReleaseFuture == null) {
            mSurfaceSafeToReleaseFuture = CallbackToFutureAdapter.getFuture(completer -> {
                mSurfaceSafeToReleaseCompleter = completer;
                return "future";
            });
        }

        return mSurfaceSafeToReleaseFuture;
    }
}
