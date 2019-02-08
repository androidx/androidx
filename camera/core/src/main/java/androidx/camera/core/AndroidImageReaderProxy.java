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

import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import android.view.Surface;

/**
 * An {@link ImageReaderProxy} which wraps around an {@link ImageReader}.
 *
 * <p>All methods map one-to-one between this {@link ImageReaderProxy} and the wrapped {@link
 * ImageReader}.
 */
final class AndroidImageReaderProxy implements ImageReaderProxy {
  @GuardedBy("this")
  private final ImageReader imageReader;

  /**
   * Creates a new instance which wraps the given image reader.
   *
   * @param imageReader to wrap
   * @return new {@link AndroidImageReaderProxy} instance
   */
  AndroidImageReaderProxy(ImageReader imageReader) {
    this.imageReader = imageReader;
  }

  @Override
  @Nullable
  public synchronized ImageProxy acquireLatestImage() {
    Image image = imageReader.acquireLatestImage();
    if (image == null) {
      return null;
    }
    return new AndroidImageProxy(image);
  }

  @Override
  @Nullable
  public synchronized ImageProxy acquireNextImage() {
    Image image = imageReader.acquireNextImage();
    if (image == null) {
      return null;
    }
    return new AndroidImageProxy(image);
  }

  @Override
  public synchronized void close() {
    imageReader.close();
  }

  @Override
  public synchronized int getHeight() {
    return imageReader.getHeight();
  }

  @Override
  public synchronized int getWidth() {
    return imageReader.getWidth();
  }

  @Override
  public synchronized int getImageFormat() {
    return imageReader.getImageFormat();
  }

  @Override
  public synchronized int getMaxImages() {
    return imageReader.getMaxImages();
  }

  @Override
  public synchronized Surface getSurface() {
    return imageReader.getSurface();
  }

  @Override
  public synchronized void setOnImageAvailableListener(
      @Nullable ImageReaderProxy.OnImageAvailableListener listener, @Nullable Handler handler) {
    ImageReader.OnImageAvailableListener transformedListener =
        reader -> {
          listener.onImageAvailable(AndroidImageReaderProxy.this);
        };
    imageReader.setOnImageAvailableListener(transformedListener, handler);
  }
}
