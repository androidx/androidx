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
import android.support.annotation.GuardedBy;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link ImageProxy} which forwards all calls to another {@link ImageProxy}.
 *
 * <p>This class enables subclasses to override a few methods to achieve a custom behavior, while
 * still delegating calls on the remaining methods to a wrapped {@link ImageProxy} instance.
 *
 * <p>Listeners for the image close call can be added. When the image is closed, the listeners will
 * be notified.
 */
abstract class ForwardingImageProxy implements ImageProxy {
  @GuardedBy("this")
  protected final ImageProxy image;

  @GuardedBy("this")
  private final Set<OnImageCloseListener> onImageCloseListeners = new HashSet<>();

  /**
   * Creates a new instance which wraps the given image.
   *
   * @param image to wrap
   * @return new {@link AndroidImageProxy} instance
   */
  protected ForwardingImageProxy(ImageProxy image) {
    this.image = image;
  }

  @Override
  public synchronized void close() {
    image.close();
    notifyOnImageCloseListeners();
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
    return image.getTimestamp();
  }

  @Override
  public synchronized void setTimestamp(long timestamp) {
    image.setTimestamp(timestamp);
  }

  @Override
  public synchronized ImageProxy.PlaneProxy[] getPlanes() {
    return image.getPlanes();
  }

  /**
   * Adds a listener for close calls on this image.
   *
   * @param listener to add
   */
  synchronized void addOnImageCloseListener(OnImageCloseListener listener) {
    onImageCloseListeners.add(listener);
  }

  /** Notifies the listeners that this image has been closed. */
  protected synchronized void notifyOnImageCloseListeners() {
    for (OnImageCloseListener listener : onImageCloseListeners) {
      listener.onImageClose(this);
    }
  }

  /** Listener for the image close event. */
  interface OnImageCloseListener {
    /**
     * Callback for image close.
     *
     * @param image which is closed
     */
    void onImageClose(ImageProxy image);
  }
}
