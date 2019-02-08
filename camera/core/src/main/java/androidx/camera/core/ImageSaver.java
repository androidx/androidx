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

import android.graphics.ImageFormat;
import android.location.Location;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import androidx.camera.core.ImageUtil.EncodeFailedException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

final class ImageSaver implements Runnable {
  private static final String TAG = "ImageSaver";

  /** Type of error that occurred during save */
  public enum SaveError {
    /** Failed to write to or close the file */
    FILE_IO_FAILED,
    /** Failure when attempting to encode image */
    ENCODE_FAILED
  }

  public interface OnImageSavedListener {

    void onImageSaved(File file);

    void onError(SaveError saveError, String message, @Nullable Throwable cause);
  }

  private final @Nullable Location location;

  // The image that was captured
  private final ImageProxy image;

  // The orientation of the image
  private final int orientation;

  // If true, the picture taken is reversed horizontally and needs to be flipped.
  // Typical with front facing cameras.
  private final boolean isReversedHorizontal;

  // If true, the picture taken is reversed vertically and needs to be flipped.
  private final boolean isReversedVertical;

  // The file to save the image to
  private final File file;

  // The callback to call on completion
  private final OnImageSavedListener listener;

  // The handler to call back on
  private final Handler handler;

  // The width/height ratio output should be cropped to
  @Nullable private final Rational cropAspectRatio;

  ImageSaver(
      ImageProxy image,
      File file,
      int orientation,
      boolean reversedHorizontal,
      boolean reversedVertical,
      @Nullable Location location,
      @Nullable Rational cropAspectRatio,
      OnImageSavedListener listener,
      Handler handler) {
    this.image = image;
    this.file = file;
    this.orientation = orientation;
    isReversedHorizontal = reversedHorizontal;
    isReversedVertical = reversedVertical;
    this.listener = listener;
    this.handler = handler;
    this.location = location;

    // Fix cropRatio by orientation.
    if (orientation == 90 || orientation == 270) {
      this.cropAspectRatio = inverseRational(cropAspectRatio);
    } else {
      this.cropAspectRatio = cropAspectRatio;
    }
  }

  @Override
  public void run() {
    // Finally, we save the file to disk
    SaveError saveError = null;
    String errorMessage = null;
    Exception exception = null;
    try (ImageProxy imageToClose = image;
        FileOutputStream output = new FileOutputStream(file)) {
      byte[] bytes = getBytes();
      output.write(bytes);

      Exif exif = Exif.createFromFile(file);
      exif.attachTimestamp();
      exif.rotate(orientation);
      if (isReversedHorizontal) {
        exif.flipHorizontally();
      }
      if (isReversedVertical) {
        exif.flipVertically();
      }
      if (location != null) {
        exif.attachLocation(location);
      }
      exif.save();
    } catch (IOException e) {
      saveError = SaveError.FILE_IO_FAILED;
      errorMessage = "Failed to write or close the file";
      exception = e;
    } catch (EncodeFailedException e) {
      saveError = SaveError.ENCODE_FAILED;
      errorMessage = "Failed to encode image";
      exception = e;
    }

    if (saveError != null) {
      postError(saveError, errorMessage, exception);
    } else {
      postSuccess();
    }
  }

  private void postSuccess() {
    handler.post(() -> listener.onImageSaved(file));
  }

  private void postError(SaveError saveError, String message, @Nullable Throwable cause) {
    handler.post(() -> listener.onError(saveError, message, cause));
  }

  private byte[] getBytes() throws EncodeFailedException {
    byte[] data = null;
    Size sourceSize = new Size(image.getWidth(), image.getHeight());

    if (ImageUtil.isAspectRatioValid(sourceSize, cropAspectRatio)) {
      if (image.getFormat() == ImageFormat.JPEG) {
        data = ImageUtil.cropByteArray(
            ImageUtil.jpegImageToJpegByteArray(image),
            ImageUtil.computeCropRectFromAspectRatio(sourceSize, cropAspectRatio));
      } else if (image.getFormat() == ImageFormat.YUV_420_888) {
        data = ImageUtil.yuvImageToJpegByteArray(
            image,
            ImageUtil.computeCropRectFromAspectRatio(sourceSize, cropAspectRatio));
      } else {
        data = ImageUtil.imageToJpegByteArray(image);
        Log.w(TAG, "Unrecognized image format: " + image.getFormat());
      }
    } else {
      data = ImageUtil.imageToJpegByteArray(image);
    }

    return data;
  }

  private Rational inverseRational(Rational rational) {
    if (rational == null) {
      return rational;
    }
    return new Rational(
        /*numerator=*/ rational.getDenominator(),
        /*denominator=*/ rational.getNumerator());
  }
}

