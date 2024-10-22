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
package androidx.camera.core;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.hardware.camera2.params.SessionConfiguration;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.LensFacingCameraFilter;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A set of requirements and priorities used to select a camera or return a filtered set of
 * cameras.
 */
public final class CameraSelector {

    /** A camera on the devices that its lens facing is resolved. */
    public static final int LENS_FACING_UNKNOWN = -1;
    /** A camera on the device facing the same direction as the device's screen. */
    public static final int LENS_FACING_FRONT = 0;
    /** A camera on the device facing the opposite direction as the device's screen. */
    public static final int LENS_FACING_BACK = 1;
    /**
     * An external camera that has no fixed facing relative to the device's screen.
     *
     * <p>The behavior of an external camera highly depends on the manufacturer. Currently it's
     * treated similar to a front facing camera with little verification. So it's considered
     * experimental and should be used with caution.
     */
    @ExperimentalLensFacing
    public static final int LENS_FACING_EXTERNAL = 2;

    /** A static {@link CameraSelector} that selects the default front facing camera. */
    @NonNull
    public static final CameraSelector DEFAULT_FRONT_CAMERA =
            new CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build();
    /** A static {@link CameraSelector} that selects the default back facing camera. */
    @NonNull
    public static final CameraSelector DEFAULT_BACK_CAMERA =
            new CameraSelector.Builder().requireLensFacing(LENS_FACING_BACK).build();

    @NonNull
    private final LinkedHashSet<CameraFilter> mCameraFilterSet;

    @Nullable
    private final String mPhysicalCameraId;

    CameraSelector(@NonNull LinkedHashSet<CameraFilter> cameraFilterSet,
            @Nullable String physicalCameraId) {
        mCameraFilterSet = cameraFilterSet;
        mPhysicalCameraId = physicalCameraId;
    }

    /**
     * Selects the first camera that filtered by the {@link CameraFilter}s assigned to this
     * {@link CameraSelector}.
     *
     * <p>When filtering with {@link CameraFilter}, the output set must be contained in the input
     * set, otherwise an IllegalArgumentException will be thrown.
     *
     * @param cameras The camera set being filtered.
     * @return The first camera filtered.
     * @throws IllegalArgumentException If there's no available camera after filtering or the
     *                                  filtered cameras aren't contained in the input set.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public CameraInternal select(@NonNull LinkedHashSet<CameraInternal> cameras) {
        Iterator<CameraInternal> cameraInternalIterator = filter(cameras).iterator();
        if (cameraInternalIterator.hasNext()) {
            return cameraInternalIterator.next();
        } else {
            String errorMessage = String.format(
                    "No available camera can be found. %s %s", logCameras(cameras), logSelector());
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String logCameras(@NonNull Set<CameraInternal> cameras) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cams:").append(cameras.size());
        for (CameraInternal camera : cameras) {
            CameraInfoInternal info = camera.getCameraInfoInternal();
            sb.append(String.format(" Id:%s  Lens:%s", info.getCameraId(), info.getLensFacing()));
        }
        return sb.toString();
    }

    private String logSelector() {
        StringBuilder sb = new StringBuilder();
        sb.append(
                String.format("PhyId:%s  Filters:%s", mPhysicalCameraId, mCameraFilterSet.size()));
        for (CameraFilter filter : mCameraFilterSet) {
            sb.append(" Id:").append(filter.getIdentifier());
            if (filter instanceof LensFacingCameraFilter) {
                sb.append(" LensFilter:").append(
                        ((LensFacingCameraFilter) filter).getLensFacing());
            }
        }
        return sb.toString();
    }

    /**
     * Filters the input {@link CameraInfo}s using the {@link CameraFilter}s assigned to the
     * selector.
     *
     * <p>If the {@link CameraFilter}s assigned to this selector produce a camera info that
     * is not part of the input list, the output list will be empty.
     *
     * <p>An example use case for using this function is when you want to get all
     * {@link CameraInfo}s for all available back facing cameras.
     * <pre>
     * eg.
     * {@code
     * CameraInfo defaultBackCameraInfo = null;
     * CameraSelector selector = new CameraSelector.Builder()
     *      .requireLensFacing(LENS_FACING_BACK).build();
     * List<CameraInfo> cameraInfos = selector.filter(cameraProvider.getAvailableCameraInfos());
     * }
     * </pre>
     *
     * @param cameraInfos The camera infos list being filtered.
     * @return The remaining list of camera infos.
     * @throws UnsupportedOperationException If the {@link CameraFilter}s assigned to the selector
     *                                       try to modify the input camera infos list.
     * @throws IllegalArgumentException If the device cannot return the necessary information for
     *                                  filtering, it will throw this exception.
     */
    @NonNull
    public List<CameraInfo> filter(@NonNull List<CameraInfo> cameraInfos) {
        List<CameraInfo> output = new ArrayList<>(cameraInfos);
        for (CameraFilter filter : mCameraFilterSet) {
            output = filter.filter(Collections.unmodifiableList(output));
        }

        output.retainAll(cameraInfos);
        return output;
    }

    /**
     * Filters the input cameras using the {@link CameraFilter} assigned to the selector.
     *
     * <p>The cameras filtered must be contained in the input set. Otherwise it will throw an
     * exception.
     *
     * @param cameras The camera set being filtered.
     * @return The remaining set of cameras.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public LinkedHashSet<CameraInternal> filter(@NonNull LinkedHashSet<CameraInternal> cameras) {
        List<CameraInfo> input = new ArrayList<>();
        for (CameraInternal camera : cameras) {
            input.add(camera.getCameraInfo());
        }

        List<CameraInfo> result = filter(input);

        LinkedHashSet<CameraInternal> output = new LinkedHashSet<>();
        for (CameraInternal camera : cameras) {
            if (result.contains(camera.getCameraInfo())) {
                output.add(camera);
            }
        }

        return output;
    }

    /**
     * Gets the set of {@link CameraFilter} assigned to this camera selector.
     *
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public LinkedHashSet<CameraFilter> getCameraFilterSet() {
        return mCameraFilterSet;
    }

    /**
     * Returns a single lens facing from this camera selector, or null if lens facing has not
     * been set.
     *
     * @return The lens facing.
     * @throws IllegalStateException if a single lens facing cannot be resolved, such as if
     *                               multiple conflicting lens facing requirements exist in this
     *                               camera selector.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Integer getLensFacing() {
        Integer currentLensFacing = null;
        for (CameraFilter filter : mCameraFilterSet) {
            if (filter instanceof LensFacingCameraFilter) {
                Integer newLensFacing = ((LensFacingCameraFilter) filter).getLensFacing();
                if (currentLensFacing == null) {
                    currentLensFacing = newLensFacing;
                } else if (!currentLensFacing.equals(newLensFacing)) {
                    // TODO(b/122975195): Now we assume the lens facing of a camera is either
                    //  FRONT or BACK, so if there's conflicting lens facings set, throws an
                    //  exception. It needs to be revisited if we have a third lens facing enum
                    //  in the future.
                    throw new IllegalStateException(
                            "Multiple conflicting lens facing requirements exist.");
                }
            }
        }

        return currentLensFacing;
    }

    /**
     * Returns the physical camera id.
     *
     * <p>If physical camera id is not set via {@link Builder#setPhysicalCameraId(String)},
     * it will return null.
     *
     * @return physical camera id.
     * @see Builder#setPhysicalCameraId(String)
     */
    @Nullable
    public String getPhysicalCameraId() {
        return mPhysicalCameraId;
    }

    /** Builder for a {@link CameraSelector}. */
    public static final class Builder {
        @NonNull
        private final LinkedHashSet<CameraFilter> mCameraFilterSet;

        @Nullable
        private String mPhysicalCameraId;

        public Builder() {
            mCameraFilterSet = new LinkedHashSet<>();
        }

        private Builder(@NonNull LinkedHashSet<CameraFilter> cameraFilterSet) {
            mCameraFilterSet = new LinkedHashSet<>(cameraFilterSet);
        }

        /**
         * Requires a camera with the specified lens facing.
         *
         * <p>Valid values for lens facing are {@link CameraSelector#LENS_FACING_FRONT},
         * {@link CameraSelector#LENS_FACING_BACK} and
         * {@link CameraSelector#LENS_FACING_EXTERNAL}. However, requiring
         * {@link CameraSelector#LENS_FACING_EXTERNAL} is currently experimental and may produce
         * unexpected behaviors.
         *
         * <p>If lens facing is already set, this will add extra requirement for lens facing
         * instead of replacing the previous setting.
         *
         * @param lensFacing the lens facing for selecting cameras with.
         * @return this builder.
         */
        @NonNull
        public Builder requireLensFacing(@LensFacing int lensFacing) {
            Preconditions.checkState(lensFacing != LENS_FACING_UNKNOWN, "The specified lens "
                    + "facing is invalid.");
            mCameraFilterSet.add(new LensFacingCameraFilter(lensFacing));
            return this;
        }

        /**
         * Adds a {@link CameraFilter} to the current set of filters. It can be used to select a
         * specific camera based on customized criteria like Camera2 characteristics.
         *
         * <p>Multiple filters can be added. All filters will be applied by the order they were
         * added when the {@link CameraSelector} is used, and the first camera output from the
         * filters will be selected.
         *
         * @param cameraFilter the {@link CameraFilter} for selecting cameras with.
         * @return this builder.
         */
        @NonNull
        public Builder addCameraFilter(@NonNull CameraFilter cameraFilter) {
            mCameraFilterSet.add(cameraFilter);
            return this;
        }

        /**
         * Generates a Builder from another CameraSelector object.
         *
         * @param cameraSelector An existing CameraSelector.
         * @return The new Builder.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Builder fromSelector(@NonNull CameraSelector cameraSelector) {
            CameraSelector.Builder builder = new CameraSelector.Builder(
                    cameraSelector.getCameraFilterSet());
            return builder;
        }

        /**
         * Sets the physical camera id.
         *
         * <p>A logical camera is a grouping of two or more of those physical cameras.
         * See <a href="https://developer.android.com/media/camera/camera2/multi-camera">Multi-camera API</a>
         *
         * <p> If we want to open one physical camera, for example ultra wide, we just need to set
         * physical camera id in {@link CameraSelector} and bind to lifecycle. All CameraX features
         * will work normally when only a single physical camera is used.
         *
         * <p>If we want to open multiple physical cameras, we need to have multiple
         * {@link CameraSelector}s and set physical camera id on each, then bind to lifecycle with
         * the {@link CameraSelector}s. Internally each physical camera id will be set on
         * {@link UseCase}, for example, {@link Preview} and call
         * {@link android.hardware.camera2.params.OutputConfiguration#setPhysicalCameraId(String)}.
         *
         * <p>Currently only two physical cameras for the same logical camera id are allowed
         * and the device needs to support physical cameras by checking
         * {@link CameraInfo#isLogicalMultiCameraSupported()}. In addition, there is no guarantee
         * or API to query whether the device supports multiple physical camera opening or not.
         * Internally the library checks
         * {@link android.hardware.camera2.CameraDevice#isSessionConfigurationSupported(SessionConfiguration)},
         * if the device does not support the multiple physical camera configuration,
         * {@link IllegalArgumentException} will be thrown when binding to lifecycle.
         *
         * @param physicalCameraId physical camera id.
         * @return this builder.
         */
        @NonNull
        public Builder setPhysicalCameraId(@NonNull String physicalCameraId) {
            mPhysicalCameraId = physicalCameraId;
            return this;
        }

        /** Builds the {@link CameraSelector}. */
        @NonNull
        public CameraSelector build() {
            return new CameraSelector(mCameraFilterSet, mPhysicalCameraId);
        }
    }

    /**
     * The direction the camera faces relative to device screen.
     *
     */
    @Target({TYPE, TYPE_USE, FIELD, PARAMETER, LOCAL_VARIABLE})
    @OptIn(markerClass = ExperimentalLensFacing.class)
    @IntDef({LENS_FACING_UNKNOWN, LENS_FACING_FRONT, LENS_FACING_BACK, LENS_FACING_EXTERNAL})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface LensFacing {
    }
}
