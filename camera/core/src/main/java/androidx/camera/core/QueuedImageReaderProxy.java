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

import android.os.Handler;
import android.support.annotation.GuardedBy;
import android.support.annotation.Nullable;
import android.view.Surface;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An {@link ImageReaderProxy} which maintains a queue of recently available images.
 *
 * <p>Like a conventional {@link android.media.ImageReader}, when the queue becomes full and the
 * user does not close older images quickly enough, newly available images will not be added to the
 * queue and become lost. The user is responsible for setting a listener for newly available images
 * and closing the acquired images quickly enough.
 */
final class QueuedImageReaderProxy
    implements ImageReaderProxy, ForwardingImageProxy.OnImageCloseListener {
  private final int width;
  private final int height;
  private final int format;
  private final int maxImages;

  @GuardedBy("this")
  private final Surface surface;

  // maxImages is not expected to be large, because images consume a lot of memory and there cannot
  // co-exist too many images simultaneously. So, just use a List to simplify the implementation.
  @GuardedBy("this")
  private final List<ImageProxy> images;

  @GuardedBy("this")
  private final Set<ImageProxy> acquiredImages = new HashSet<>();

  // Current access position in the queue.
  @GuardedBy("this")
  private int currentPosition;

  @GuardedBy("this")
  @Nullable
  private ImageReaderProxy.OnImageAvailableListener onImageAvailableListener;

  @GuardedBy("this")
  @Nullable
  private Handler onImageAvailableHandler;

  @GuardedBy("this")
  private final Set<OnReaderCloseListener> onReaderCloseListeners = new HashSet<>();

  @GuardedBy("this")
  private boolean closed;

  /**
   * Creates a new instance of a queued image reader proxy.
   *
   * @param width of the images
   * @param height of the images
   * @param format of the images
   * @param maxImages capacity of the queue
   * @param surface to which the reader is attached
   * @return new {@link QueuedImageReaderProxy} instance
   */
  QueuedImageReaderProxy(int width, int height, int format, int maxImages, Surface surface) {
    this.width = width;
    this.height = height;
    this.format = format;
    this.maxImages = maxImages;
    this.surface = surface;
    images = new ArrayList<>(maxImages);
    currentPosition = 0;
    closed = false;
  }

  @Override
  @Nullable
  public synchronized ImageProxy acquireLatestImage() {
    throwExceptionIfClosed();
    if (images.isEmpty()) {
      return null;
    }
    if (currentPosition >= images.size()) {
      throw new IllegalStateException("Max images have already been acquired without close.");
    }

    // Close all images up to the tail of the list, except for already acquired images.
    List<ImageProxy> imagesToClose = new ArrayList<>();
    for (int i = 0; i < images.size() - 1; ++i) {
      if (!acquiredImages.contains(images.get(i))) {
        imagesToClose.add(images.get(i));
      }
    }
    for (ImageProxy image : imagesToClose) {
      // Calling image.close() will cause this.onImageClosed(image) to be called.
      image.close();
    }

    // Move the current position to the tail of the list.
    currentPosition = images.size() - 1;
    ImageProxy acquiredImage = images.get(currentPosition++);
    acquiredImages.add(acquiredImage);
    return acquiredImage;
  }

  @Override
  @Nullable
  public synchronized ImageProxy acquireNextImage() {
    throwExceptionIfClosed();
    if (images.isEmpty()) {
      return null;
    }
    if (currentPosition >= images.size()) {
      throw new IllegalStateException("Max images have already been acquired without close.");
    }
    ImageProxy acquiredImage = images.get(currentPosition++);
    acquiredImages.add(acquiredImage);
    return acquiredImage;
  }

  /**
   * Adds an image to the tail of the queue.
   *
   * <p>If the queue already contains the max number of images, the given image is not added to the
   * queue and is closed. This is consistent with the documented behavior of an {@link
   * android.media.ImageReader}, where new images may be lost if older images are not closed quickly
   * enough.
   *
   * <p>If the image is added to the queue and an on-image-available listener has been previously
   * set, the listener is notified that the new image is available.
   *
   * @param image to add
   */
  synchronized void enqueueImage(ForwardingImageProxy image) {
    throwExceptionIfClosed();
    if (images.size() < maxImages) {
      images.add(image);
      image.addOnImageCloseListener(this);
      if (onImageAvailableListener != null && onImageAvailableHandler != null) {
        final OnImageAvailableListener listener = onImageAvailableListener;
        onImageAvailableHandler.post(
            () -> {
              if (!QueuedImageReaderProxy.this.isClosed()) {
                listener.onImageAvailable(QueuedImageReaderProxy.this);
              }
            });
      }
    } else {
      image.close();
    }
  }

  @Override
  public synchronized void close() {
    if (!closed) {
      setOnImageAvailableListener(null, null);
      // We need to copy into a different list, because closing an image triggers the on-close
      // listener which in turn modifies the original list.
      List<ImageProxy> imagesToClose = new ArrayList<>(images);
      for (ImageProxy image : imagesToClose) {
        image.close();
      }
      images.clear();
      closed = true;
      notifyOnReaderCloseListeners();
    }
  }

  @Override
  public int getHeight() {
    throwExceptionIfClosed();
    return height;
  }

  @Override
  public int getWidth() {
    throwExceptionIfClosed();
    return width;
  }

  @Override
  public int getImageFormat() {
    throwExceptionIfClosed();
    return format;
  }

  @Override
  public int getMaxImages() {
    throwExceptionIfClosed();
    return maxImages;
  }

  @Override
  public synchronized Surface getSurface() {
    throwExceptionIfClosed();
    return surface;
  }

  @Override
  public synchronized void setOnImageAvailableListener(
      @Nullable OnImageAvailableListener onImageAvailableListener,
      @Nullable Handler onImageAvailableHandler) {
    throwExceptionIfClosed();
    this.onImageAvailableListener = onImageAvailableListener;
    this.onImageAvailableHandler = onImageAvailableHandler;
  }

  @Override
  public synchronized void onImageClose(ImageProxy image) {
    int index = images.indexOf(image);
    if (index >= 0) {
      images.remove(index);
      if (index <= currentPosition) {
        currentPosition--;
      }
    }
    acquiredImages.remove(image);
  }

  /** Returns the current number of images in the queue. */
  synchronized int getCurrentImages() {
    throwExceptionIfClosed();
    return images.size();
  }

  /** Returns true if the reader is already closed. */
  synchronized boolean isClosed() {
    return closed;
  }

  /**
   * Adds a listener for close calls on this reader.
   *
   * @param listener to add
   */
  synchronized void addOnReaderCloseListener(OnReaderCloseListener listener) {
    onReaderCloseListeners.add(listener);
  }

  /** Listener for the reader close event. */
  interface OnReaderCloseListener {
    /**
     * Callback for reader close.
     *
     * @param imageReader which is closed
     */
    void onReaderClose(ImageReaderProxy imageReader);
  }

  private synchronized void throwExceptionIfClosed() {
    if (closed) {
      throw new IllegalStateException("This reader is already closed.");
    }
  }

  private synchronized void notifyOnReaderCloseListeners() {
    for (OnReaderCloseListener listener : onReaderCloseListeners) {
      listener.onReaderClose(this);
    }
  }
}
