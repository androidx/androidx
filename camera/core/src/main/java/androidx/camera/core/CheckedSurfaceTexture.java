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

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.Size;
import android.view.Surface;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.nio.IntBuffer;

/**
 * A {@link DeferrableSurface} which verifies the {@link SurfaceTexture} that backs the {@link
 * Surface} is unreleased before returning the Surface.
 */
final class CheckedSurfaceTexture implements DeferrableSurface {
  interface OnTextureChangedListener {
    void onTextureChanged(SurfaceTexture newOutput, Size newResolution);
  }

  @Nullable private SurfaceTexture surfaceTexture;
  @Nullable private Surface surface;
  private final OnTextureChangedListener outputChangedListener;
  private final Handler mainThreadHandler;
  @Nullable private Size resolution;

  CheckedSurfaceTexture(OnTextureChangedListener outputChangedListener, Handler mainThreadHandler) {
    this.outputChangedListener = outputChangedListener;
    this.mainThreadHandler = mainThreadHandler;
  }

  @UiThread
  void setResolution(Size resolution) {
    this.resolution = resolution;
  }

  @UiThread
  void resetSurfaceTexture() {
    if (resolution == null) {
      throw new IllegalStateException(
          "setResolution() must be called before resetSurfaceTexture()");
    }

    release();
    surfaceTexture = createDetachedSurfaceTexture(resolution);
    surface = new Surface(surfaceTexture);
    outputChangedListener.onTextureChanged(surfaceTexture, resolution);
  }

  private boolean surfaceTextureReleased(SurfaceTexture surfaceTexture) {
    boolean released = false;

    // TODO(b/121196683) Refactor workaround into a compatibility module
    if (26 <= android.os.Build.VERSION.SDK_INT) {
      released = surfaceTexture.isReleased();
    } else {
      // WARNING: This relies on some implementation details of the ViewFinderOutput native code.
      // If the ViewFinderOutput is released, we should get a RuntimeException. If not, we should
      // get an IllegalStateException since we are not in the same EGL context as the consumer.
      Exception exception = null;
      try {
        // TODO(b/121198329) Make sure updateTexImage() isn't called on consumer EGL context
        surfaceTexture.updateTexImage();
      } catch (IllegalStateException e) {
        exception = e;
        released = false;
      } catch (RuntimeException e) {
        exception = e;
        released = true;
      }

      if (!released && exception == null) {
        throw new RuntimeException("Unable to determine if ViewFinderOutput is released");
      }
    }

    return released;
  }

  /**
   * Returns the {@link Surface} that is backed by a {@link SurfaceTexture}.
   *
   * <p>If the {@link SurfaceTexture} has already been released then the surface will be reset using
   * a new {@link SurfaceTexture}.
   */
  @Override
  public ListenableFuture<Surface> getSurface() {
    SettableFuture<Surface> deferredSurface = SettableFuture.create();
    Runnable checkAndSetRunnable =
        () -> {
          if (surfaceTextureReleased(surfaceTexture)) {
            // Reset the surface texture and notify the listener
            resetSurfaceTexture();
          }

          deferredSurface.set(surface);
        };

    if (Looper.myLooper() == mainThreadHandler.getLooper()) {
      checkAndSetRunnable.run();
    } else {
      mainThreadHandler.post(checkAndSetRunnable);
    }

    return deferredSurface;
  }

  void release() {
    if (surface != null) {
      surface.release();
      surface = null;
    }
  }

  private static SurfaceTexture createDetachedSurfaceTexture(Size resolution) {
    IntBuffer buffer = IntBuffer.allocate(1);
    GLES20.glGenTextures(1, buffer);
    SurfaceTexture surfaceTexture = new FixedSizeSurfaceTexture(buffer.get(), resolution);
    surfaceTexture.detachFromGLContext();
    return surfaceTexture;
  }
}
