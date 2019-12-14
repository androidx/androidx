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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    private static final int ANY_WIDTH = 1200;
    private static final int ANY_HEIGHT = 1600;
    private static final Size ANY_SIZE = new Size(ANY_WIDTH, ANY_HEIGHT);

    private TextureViewImplementation mImplementation;
    private SurfaceTexture mSurfaceTexture;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener;
    private ListenableFuture<Void> mSafeToReleaseFuture;
    private CallbackToFutureAdapter.Completer<Void> mCompleter;

    @Before
    public void setUp() {
        final Context mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mSurfaceTexture = new SurfaceTexture(0);
        mImplementation = new TextureViewImplementation();
        mImplementation.init(new FrameLayout(mContext));
        mSurfaceTextureListener = mImplementation.mTextureView.getSurfaceTextureListener();
    }

    @After
    public void tearDown() {
        if (mSafeToReleaseFuture != null) {
            mCompleter.set(null);
            mSafeToReleaseFuture = null;
        }
    }

    @Test(expected = TimeoutException.class)
    public void doNotProvideSurface_ifSurfaceTextureNotAvailableYet() throws Exception {
        mImplementation.getPreviewSurfaceProvider()
                .provideSurface(ANY_SIZE, getSafeToReleaseFuture())
                .get(0, TimeUnit.SECONDS);
    }

    @Test
    public void provideSurface_ifSurfaceTextureAvailable() throws Exception {
        mSurfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);
        final Surface surface = mImplementation.getPreviewSurfaceProvider()
                .provideSurface(ANY_SIZE, getSafeToReleaseFuture())
                .get();

        assertNotNull(surface);
        assertNull(mImplementation.mSurfaceCompleter);
    }

    @Test
    public void doNotDestroySurface_whenSurfaceTextureBeingDestroyed_andCameraUsingSurface()
            throws Exception {
        mSurfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH,
                ANY_HEIGHT);

        mImplementation.getPreviewSurfaceProvider()
                .provideSurface(ANY_SIZE, getSafeToReleaseFuture())
                .get();

        assertNotNull(mImplementation.mSurfaceReleaseFuture);
        assertFalse(mSurfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture));
    }

    @Test
    @LargeTest
    public void destroySurface_whenSurfaceTextureBeingDestroyed_andCameraNotUsingSurface()
            throws Exception {
        mSurfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH,
                ANY_HEIGHT);

        final ListenableFuture<Void> surfaceReleaseFuture = getSafeToReleaseFuture();
        mImplementation.getPreviewSurfaceProvider()
                .provideSurface(ANY_SIZE, surfaceReleaseFuture)
                .get();

        mCompleter.set(null);

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1_000);

        assertNull(mImplementation.mSurfaceReleaseFuture);
        assertTrue(mSurfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture));
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    @LargeTest
    public void releaseSurfaceTexture_afterSurfaceTextureDestroyed_andCameraNoLongerUsingSurface_1()
            throws Exception {
        mSurfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH,
                ANY_HEIGHT);

        final ListenableFuture<Void> surfaceReleaseFuture = getSafeToReleaseFuture();
        mImplementation.getPreviewSurfaceProvider()
                .provideSurface(ANY_SIZE, surfaceReleaseFuture)
                .get();

        mSurfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture);
        mCompleter.set(null);

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1_000);

        assertNull(mImplementation.mSurfaceReleaseFuture);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @LargeTest
    public void releaseSurfaceTexture_afterSurfaceTextureDestroyed_andCameraNoLongerUsingSurface_2()
            throws Exception {
        mSurfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH,
                ANY_HEIGHT);

        final ListenableFuture<Void> surfaceReleaseFuture = getSafeToReleaseFuture();
        mImplementation.getPreviewSurfaceProvider()
                .provideSurface(ANY_SIZE, surfaceReleaseFuture)
                .get();

        mSurfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture);
        mCompleter.set(null);

        // Wait enough time for surfaceReleaseFuture's listener to be called
        Thread.sleep(1_000);

        assertNull(mImplementation.mSurfaceReleaseFuture);
        assertTrue(mSurfaceTexture.isReleased());
    }

    @Test
    @LargeTest
    public void nullSurfaceCompleterAndSurfaceReleaseFuture_whenSurfaceProviderCancelled()
            throws Exception {
        mImplementation.getPreviewSurfaceProvider()
                .provideSurface(ANY_SIZE, getSafeToReleaseFuture())
                .cancel(true);

        // Wait enough time for mCompleter's cancellation listener to be called
        Thread.sleep(1_000);

        assertNull(mImplementation.mSurfaceCompleter);
        assertNull(mImplementation.mSurfaceReleaseFuture);
    }

    @Test
    public void releaseSurface_whenSurfaceTextureDestroyed_beforeCameraRequestsSurface() {
        mSurfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, ANY_WIDTH, ANY_HEIGHT);

        assertTrue(mSurfaceTextureListener.onSurfaceTextureDestroyed(mSurfaceTexture));
        assertNull(mImplementation.mSurfaceTexture);
    }

    private ListenableFuture<Void> getSafeToReleaseFuture() {
        if (mSafeToReleaseFuture == null) {
            mSafeToReleaseFuture = CallbackToFutureAdapter.getFuture(completer -> {
                mCompleter = completer;
                return "future";
            });
        }

        return mSafeToReleaseFuture;
    }
}
