/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.util.Size;
import android.util.SizeF;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.internal.utils.ImageUtil;
import androidx.core.util.Preconditions;

import java.util.Locale;

/**
 * Utility class for transform.
 *
 * <p> The vertices representation uses a float array to represent a rectangle with arbitrary
 * rotation and rotation-direction. It could be otherwise represented by a triple of a
 * {@link RectF}, a rotation degrees integer and a boolean flag for the rotation-direction
 * (clockwise v.s. counter-clockwise).
 *
 * TODO(b/179827713): merge this with {@link ImageUtil}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class TransformUtils {

    // Normalized space (-1, -1) - (1, 1).
    public static final RectF NORMALIZED_RECT = new RectF(-1, -1, 1, 1);

    private TransformUtils() {
    }

    /**
     * Gets the size of the {@link Rect}.
     */
    @NonNull
    public static Size rectToSize(@NonNull Rect rect) {
        return new Size(rect.width(), rect.height());
    }

    /** Returns a formatted string for a Rect. */
    @NonNull
    public static String rectToString(@NonNull Rect rect) {
        return String.format(Locale.US, "%s(%dx%d)", rect, rect.width(), rect.height());
    }

    /**
     * Transforms size to a {@link Rect} with zero left and top.
     */
    @NonNull
    public static Rect sizeToRect(@NonNull Size size) {
        return sizeToRect(size, 0, 0);
    }

    /**
     * Transforms a size to a {@link Rect} with given left and top.
     */
    @NonNull
    public static Rect sizeToRect(@NonNull Size size, int left, int top) {
        return new Rect(left, top, left + size.getWidth(), top + size.getHeight());
    }

    /**
     * Returns true if the crop rect does not match the size.
     */
    public static boolean hasCropping(@NonNull Rect cropRect, @NonNull Size size) {
        return cropRect.left != 0 || cropRect.top != 0 || cropRect.width() != size.getWidth()
                || cropRect.height() != size.getHeight();
    }

    /**
     * Transforms size to a {@link RectF} with zero left and top.
     */
    @NonNull
    public static RectF sizeToRectF(@NonNull Size size) {
        return sizeToRectF(size, 0, 0);
    }

    /**
     * Transforms a size to a {@link RectF} with given left and top.
     */
    @NonNull
    public static RectF sizeToRectF(@NonNull Size size, int left, int top) {
        return new RectF(left, top, left + size.getWidth(), top + size.getHeight());
    }

    /**
     * Reverses width and height for a {@link Size}.
     *
     * @param size the size to reverse
     * @return reversed size
     */
    @NonNull
    public static Size reverseSize(@NonNull Size size) {
        return new Size(size.getHeight(), size.getWidth());
    }

    /**
     * Reverses width and height for a {@link SizeF}.
     *
     * @param sizeF the float size to reverse
     * @return reversed float size
     */
    @NonNull
    public static SizeF reverseSizeF(@NonNull SizeF sizeF) {
        return new SizeF(sizeF.getHeight(), sizeF.getWidth());
    }

    /**
     * Rotates a {@link Size} according to the rotation degrees.
     *
     * @param size            the size to rotate
     * @param rotationDegrees the rotation degrees
     * @return rotated size
     * @throws IllegalArgumentException if the rotation degrees is not a multiple of 90
     */
    @NonNull
    public static Size rotateSize(@NonNull Size size, int rotationDegrees) {
        Preconditions.checkArgument(rotationDegrees % 90 == 0,
                "Invalid rotation degrees: " + rotationDegrees);
        return is90or270(within360(rotationDegrees)) ? reverseSize(size) : size;
    }

    /**
     * Rotates {@link SizeF} according to the rotation degrees.
     *
     * <p> A 640, 480 rect rotated 90 degrees clockwise will become a 480, 640 rect.
     */
    @NonNull
    public static RectF rotateRect(@NonNull RectF rect, int rotationDegrees) {
        Preconditions.checkArgument(rotationDegrees % 90 == 0,
                "Invalid rotation degrees: " + rotationDegrees);
        if (is90or270(within360(rotationDegrees))) {
            return new RectF(0, 0, /*right=*/rect.height(),  /*bottom=*/rect.width());
        } else {
            return rect;
        }
    }

    /**
     * Gets the size after cropping and rotating.
     *
     * @return rotated size
     * @throws IllegalArgumentException if the rotation degrees is not a multiple of.
     */
    @NonNull
    public static Size getRotatedSize(@NonNull Rect cropRect, int rotationDegrees) {
        return rotateSize(rectToSize(cropRect), rotationDegrees);
    }

    /**
     * Converts the degrees to within 360 degrees [0 - 359].
     */
    public static int within360(int degrees) {
        return (degrees % 360 + 360) % 360;
    }

    /**
     * Converts an array of vertices to a {@link RectF}.
     */
    @NonNull
    public static RectF verticesToRect(@NonNull float[] vertices) {
        return new RectF(
                min(vertices[0], vertices[2], vertices[4], vertices[6]),
                min(vertices[1], vertices[3], vertices[5], vertices[7]),
                max(vertices[0], vertices[2], vertices[4], vertices[6]),
                max(vertices[1], vertices[3], vertices[5], vertices[7])
        );
    }

    /**
     * Returns the max value.
     */
    public static float max(float value1, float value2, float value3, float value4) {
        return Math.max(Math.max(value1, value2), Math.max(value3, value4));
    }

    /**
     * Returns the min value.
     */
    public static float min(float value1, float value2, float value3, float value4) {
        return Math.min(Math.min(value1, value2), Math.min(value3, value4));
    }

    /**
     * Returns true if the rotation degrees is 90 or 270.
     */
    public static boolean is90or270(int rotationDegrees) {
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            return true;
        }
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            return false;
        }
        throw new IllegalArgumentException("Invalid rotation degrees: " + rotationDegrees);
    }

    /**
     * Converts a {@link Size} to a float array of vertices.
     */
    @NonNull
    public static float[] sizeToVertices(@NonNull Size size) {
        return new float[]{0, 0, size.getWidth(), 0, size.getWidth(), size.getHeight(), 0,
                size.getHeight()};
    }

    /**
     * Converts a {@link RectF} defined by top, left, right and bottom to an array of vertices.
     */
    @NonNull
    public static float[] rectToVertices(@NonNull RectF rectF) {
        return new float[]{rectF.left, rectF.top, rectF.right, rectF.top, rectF.right, rectF.bottom,
                rectF.left, rectF.bottom};
    }

    /**
     * Checks if aspect ratio matches while tolerating rounding error.
     *
     * @see #isAspectRatioMatchingWithRoundingError(Size, boolean, Size, boolean)
     */
    public static boolean isAspectRatioMatchingWithRoundingError(
            @NonNull Size size1, @NonNull Size size2) {
        return isAspectRatioMatchingWithRoundingError(
                size1, /*isAccurate1=*/ false, size2, /*isAccurate2=*/ false);
    }

    /**
     * Checks if aspect ratio matches while tolerating rounding error.
     *
     * <p> One example of the usage is comparing the viewport-based crop rect from different use
     * cases. The crop rect is rounded because pixels are integers, which may introduce an error
     * when we check if the aspect ratio matches. For example, when
     * {@linkplain androidx.camera.view.PreviewView}'s
     * width/height are prime numbers 601x797, the crop rect from other use cases cannot have a
     * matching aspect ratio even if they are based on the same viewport. This method checks the
     * aspect ratio while tolerating a rounding error.
     *
     * @param size1       the rounded size1
     * @param isAccurate1 if size1 is accurate. e.g. it's true if it's the PreviewView's
     *                    dimension which viewport is based on
     * @param size2       the rounded size2
     * @param isAccurate2 if size2 is accurate.
     */
    public static boolean isAspectRatioMatchingWithRoundingError(
            @NonNull Size size1, boolean isAccurate1, @NonNull Size size2, boolean isAccurate2) {
        // The crop rect coordinates are rounded values. Each value is at most .5 away from their
        // true values. So the width/height, which is the difference of 2 coordinates, are at most
        // 1.0 away from their true value.
        // First figure out the possible range of the aspect ratio's ture value.
        float ratio1UpperBound;
        float ratio1LowerBound;
        if (isAccurate1) {
            ratio1UpperBound = (float) size1.getWidth() / size1.getHeight();
            ratio1LowerBound = ratio1UpperBound;
        } else {
            ratio1UpperBound = (size1.getWidth() + 1F) / (size1.getHeight() - 1F);
            ratio1LowerBound = (size1.getWidth() - 1F) / (size1.getHeight() + 1F);
        }
        float ratio2UpperBound;
        float ratio2LowerBound;
        if (isAccurate2) {
            ratio2UpperBound = (float) size2.getWidth() / size2.getHeight();
            ratio2LowerBound = ratio2UpperBound;
        } else {
            ratio2UpperBound = (size2.getWidth() + 1F) / (size2.getHeight() - 1F);
            ratio2LowerBound = (size2.getWidth() - 1F) / (size2.getHeight() + 1F);
        }
        // Then we check if the true value range overlaps.
        return ratio1UpperBound >= ratio2LowerBound && ratio2UpperBound >= ratio1LowerBound;
    }

    /**
     * Gets the transform from one {@link RectF} to another with rotation degrees.
     *
     * <p> Following is how the source is mapped to the target with a 90° rotation. The rect
     * <a, b, c, d> is mapped to <a', b', c', d'>.
     *
     * <pre>
     *  a----------b               d'-----------a'
     *  |  source  |    -90°->     |            |
     *  d----------c               |   target   |
     *                             |            |
     *                             c'-----------b'
     * </pre>
     */
    @NonNull
    public static Matrix getRectToRect(
            @NonNull RectF source, @NonNull RectF target, int rotationDegrees) {
        return getRectToRect(source, target, rotationDegrees, /*mirroring=*/false);
    }

    /**
     * Gets the transform from one {@link RectF} to another with rotation degrees and mirroring.
     *
     * <p> Following is how the source is mapped to the target with a 90° rotation and a mirroring.
     * The rect <a, b, c, d> is mapped to <a', b', c', d'>.
     *
     * <pre>
     *  a----------b                           a'-----------d'
     *  |  source  |    -90° + mirroring ->    |            |
     *  d----------c                           |   target   |
     *                                         |            |
     *                                         b'-----------c'
     * </pre>
     */
    @NonNull
    public static Matrix getRectToRect(
            @NonNull RectF source, @NonNull RectF target, int rotationDegrees, boolean mirroring) {
        // Map source to normalized space.
        Matrix matrix = new Matrix();
        matrix.setRectToRect(source, NORMALIZED_RECT, Matrix.ScaleToFit.FILL);
        // Add rotation.
        matrix.postRotate(rotationDegrees);
        if (mirroring) {
            matrix.postScale(-1, 1);
        }
        // Restore the normalized space to target's coordinates.
        matrix.postConcat(getNormalizedToBuffer(target));
        return matrix;
    }

    /**
     * Gets the transform from a normalized space (-1, -1) - (1, 1) to the given rect.
     */
    @NonNull
    public static Matrix getNormalizedToBuffer(@NonNull Rect viewPortRect) {
        return getNormalizedToBuffer(new RectF(viewPortRect));
    }

    /**
     * Updates sensor to buffer transform based on crop rect.
     */
    @NonNull
    public static Matrix updateSensorToBufferTransform(
            @NonNull Matrix original,
            @NonNull Rect cropRect) {
        Matrix matrix = new Matrix(original);
        matrix.postTranslate(-cropRect.left, -cropRect.top);
        return matrix;
    }

    /**
     * Gets the transform from a normalized space (-1, -1) - (1, 1) to the given rect.
     */
    @NonNull
    public static Matrix getNormalizedToBuffer(@NonNull RectF viewPortRect) {
        Matrix normalizedToBuffer = new Matrix();
        normalizedToBuffer.setRectToRect(NORMALIZED_RECT, viewPortRect, Matrix.ScaleToFit.FILL);
        return normalizedToBuffer;
    }

    /**
     * Gets the transform matrix based on exif orientation.
     */
    @NonNull
    public static Matrix getExifTransform(int exifOrientation, int width, int height) {
        Matrix matrix = new Matrix();

        // Map the bitmap to a normalized space and perform transform. It's more readable, and it
        // can be tested with Robolectric's ShadowMatrix (Matrix#setPolyToPoly is currently not
        // shadowed by ShadowMatrix).
        RectF rect = new RectF(0, 0, width, height);
        matrix.setRectToRect(rect, NORMALIZED_RECT, Matrix.ScaleToFit.FILL);

        // A flag that checks if the image has been rotated 90/270.
        boolean isWidthHeightSwapped = false;

        // Transform the normalized space based on exif orientation.
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.postScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                // Flipped about top-left <--> bottom-right axis, it can also be represented by
                // flip horizontally and then rotate 270 degree clockwise.
                matrix.postScale(-1f, 1f);
                matrix.postRotate(270);
                isWidthHeightSwapped = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                isWidthHeightSwapped = true;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                // Flipped about top-right <--> bottom left axis, it can also be represented by
                // flip horizontally and then rotate 90 degree clockwise.
                matrix.postScale(-1f, 1f);
                matrix.postRotate(90);
                isWidthHeightSwapped = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                isWidthHeightSwapped = true;
                break;
            case ExifInterface.ORIENTATION_NORMAL:
                // Fall-through
            case ExifInterface.ORIENTATION_UNDEFINED:
                // Fall-through
            default:
                break;
        }

        // Map the normalized space back to the bitmap coordinates.
        @SuppressWarnings("SuspiciousNameCombination")
        RectF restoredRect = isWidthHeightSwapped ? new RectF(0, 0, height, width) : rect;
        Matrix restore = new Matrix();
        restore.setRectToRect(NORMALIZED_RECT, restoredRect, Matrix.ScaleToFit.FILL);
        matrix.postConcat(restore);

        return matrix;
    }
}
