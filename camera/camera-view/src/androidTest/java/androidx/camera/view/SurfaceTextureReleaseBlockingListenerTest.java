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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.TextureView;

import androidx.annotation.RequiresApi;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

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

    // isReleased() exists only on 23 and above. Is private prior to 26
    @RequiresApi(23)
    @Test
    public void surfaceIsValid_ifOnlyReleasedByTextureView() throws ExecutionException,
            InterruptedException {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        // Simulate the TextureView destroying the SurfaceTexture
        mSurfaceTextureReleaseBlockingListener.onSurfaceTextureDestroyed(surfaceTexture);

        assertFalse(surfaceTexture.isReleased());
    }

    // isReleased() exists only on 23 and above. Is private prior to 26
    @RequiresApi(23)
    @Test
    public void surfaceIsValid_ifOnlyReleasedBySurfaceReleaseFuture() throws ExecutionException,
            InterruptedException {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        mCompleter.set(null);

        assertFalse(surfaceTexture.isReleased());
    }

    // isReleased() exists only on 23 and above. Is private prior to 26
    @RequiresApi(23)
    @Test
    public void surfaceIsInvalid_ifReleasedByBothTextureViewAndSurfaceReleaseFuture()
            throws ExecutionException, InterruptedException {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        // Simulate the TextureView destroying the SurfaceTexture
        mSurfaceTextureReleaseBlockingListener.onSurfaceTextureDestroyed(surfaceTexture);
        mCompleter.set(null);

        assertTrue(surfaceTexture.isReleased());
    }

    @Test
    public void setSurfaceTextureSafely_callsSetSurfaceTextureOnTextureView() {
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        mSurfaceTextureReleaseBlockingListener.setSurfaceTextureSafely(surfaceTexture,
                mSurfaceReleaseFuture);

        assertThat(mTextureView.getSurfaceTexture()).isSameInstanceAs(surfaceTexture);
    }
}
