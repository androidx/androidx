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

import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import androidx.annotation.GuardedBy;
import java.nio.ByteBuffer;

/** An {@link ImageProxy} which wraps around an {@link Image}. */
final class AndroidImageProxy implements ImageProxy {
  /**
   * Image.setTimestamp(long) was added in M. On lower API levels, we use our own timestamp field to
   * provide a more consistent behavior across more devices.
   */
  private static final boolean SET_TIMESTAMP_AVAILABLE_IN_FRAMEWORK =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

  @GuardedBy("this")
  private final Image image;

  @GuardedBy("this")
  private final PlaneProxy[] planes;

  @GuardedBy("this")
  private long timestamp;

  /**
   * Creates a new instance which wraps the given image.
   *
   * @param image to wrap
   * @return new {@link AndroidImageProxy} instance
   */
  AndroidImageProxy(Image image) {
    this.image = image;

    Image.Plane[] originalPlanes = image.getPlanes();
    if (originalPlanes != null) {
      this.planes = new PlaneProxy[originalPlanes.length];
      for (int i = 0; i < originalPlanes.length; ++i) {
        this.planes[i] = new PlaneProxy(originalPlanes[i]);
      }
    } else {
      this.planes = new PlaneProxy[0];
    }

    this.timestamp = image.getTimestamp();
  }

  @Override
  public synchronized void close() {
    image.close();
  }

  @Override
  public synchronized Rect getCropRect() {
    return image.getCropRect();
  }

  @Override
  public synchronized void setCropRect(Rect rect) {
    image.setCropRect(rect);
  }

  @Override
  public synchronized int getFormat() {
    return image.getFormat();
  }

  @Override
  public synchronized int getHeight() {
    return image.getHeight();
  }

  @Override
  public synchronized int getWidth() {
    return image.getWidth();
  }

  @Override
  public synchronized long getTimestamp() {
    if (SET_TIMESTAMP_AVAILABLE_IN_FRAMEWORK) {
      return image.getTimestamp();
    } else {
      return timestamp;
    }
  }

  @Override
  public synchronized void setTimestamp(long timestamp) {
    if (SET_TIMESTAMP_AVAILABLE_IN_FRAMEWORK) {
      image.setTimestamp(timestamp);
    } else {
      this.timestamp = timestamp;
    }
  }

  @Override
  public synchronized ImageProxy.PlaneProxy[] getPlanes() {
    return planes;
  }

  /** An {@link ImageProxy.PlaneProxy} which wraps around an {@link Image.Plane}. */
  private static final class PlaneProxy implements ImageProxy.PlaneProxy {
    @GuardedBy("this")
    private final Image.Plane plane;

    private PlaneProxy(Image.Plane plane) {
      this.plane = plane;
    }

    @Override
    public synchronized int getRowStride() {
      return plane.getRowStride();
    }

    @Override
    public synchronized int getPixelStride() {
      return plane.getPixelStride();
    }

    @Override
    public synchronized ByteBuffer getBuffer() {
      return plane.getBuffer();
    }
  }
}
