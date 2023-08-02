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

package androidx.camera.core.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configurations needed for a capture request.
 *
 * <p>The CaptureConfig contains all the {@link android.hardware.camera2} parameters that are
 * required to issue a {@link CaptureRequest}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CaptureConfig {
    /** Indicates template type is not set. */
    public static final int TEMPLATE_TYPE_NONE = -1;

    /**
     * Request that the implementation rotate the image.
     *
     * <p> Currently only applicable for {@link androidx.camera.core.ImageProxy} which are of
     * JPEG format.
     *
     * Option: camerax.core.rotation
     */
    public static final Config.Option<Integer> OPTION_ROTATION =
            Config.Option.create("camerax.core.captureConfig.rotation", int.class);

    /**
     * Sets the compression quality of the captured JPEG image.
     *
     * See {@link CaptureRequest#JPEG_QUALITY}.
     *
     * Option: camerax.core.captureConfig.jpegQuality
     */
    public static final Config.Option<Integer> OPTION_JPEG_QUALITY =
            Config.Option.create("camerax.core.captureConfig.jpegQuality", Integer.class);

    /** The set of {@link Surface} that data from the camera will be put into. */
    final List<DeferrableSurface> mSurfaces;

    final Config mImplementationOptions;

    /**
     * The templates used for configuring a {@link CaptureRequest}. This must match the constants
     * defined by {@link CameraDevice}
     */
    final int mTemplateType;

    final Range<Integer> mExpectedFrameRateRange;

    /** The camera capture callback for a {@link CameraCaptureSession}. */
    final List<CameraCaptureCallback> mCameraCaptureCallbacks;

    /** True if this capture request needs a repeating surface */
    private final boolean mUseRepeatingSurface;

    /** The tag collection for associating capture result with capture request. */
    @NonNull
    private final TagBundle mTagBundle;

    /**
     * The camera capture result for reprocessing capture request.
     */
    @Nullable
    private final CameraCaptureResult mCameraCaptureResult;

    /**
     * Private constructor for a CaptureConfig.
     *
     * <p>In practice, the {@link CaptureConfig.Builder} will be used to construct a CaptureConfig.
     *
     * @param surfaces               The set of {@link Surface} where data will be put into.
     * @param implementationOptions  The generic parameters to be passed to the
     *                               {@link CameraInternal} class.
     * @param templateType           The template for parameters of the CaptureRequest. This
     *                               must match the
     *                               constants defined by {@link CameraDevice}.
     * @param cameraCaptureCallbacks All camera capture callbacks.
     * @param cameraCaptureResult     The {@link CameraCaptureResult} for reprocessing capture
     *                               request.
     */
    CaptureConfig(
            List<DeferrableSurface> surfaces,
            Config implementationOptions,
            int templateType,
            @NonNull Range<Integer> expectedFrameRateRange,
            List<CameraCaptureCallback> cameraCaptureCallbacks,
            boolean useRepeatingSurface,
            @NonNull TagBundle tagBundle,
            @Nullable CameraCaptureResult cameraCaptureResult) {
        mSurfaces = surfaces;
        mImplementationOptions = implementationOptions;
        mTemplateType = templateType;
        mExpectedFrameRateRange = expectedFrameRateRange;
        mCameraCaptureCallbacks = Collections.unmodifiableList(cameraCaptureCallbacks);
        mUseRepeatingSurface = useRepeatingSurface;
        mTagBundle = tagBundle;
        mCameraCaptureResult = cameraCaptureResult;
    }

    /** Returns an instance of a capture configuration with minimal configurations. */
    @NonNull
    public static CaptureConfig defaultEmptyCaptureConfig() {
        return new CaptureConfig.Builder().build();
    }

    /**
     * Returns an instance of {@link CameraCaptureResult} for reprocessing capture request.
     *
     * @return {@link CameraCaptureResult}.
     */
    @Nullable
    public CameraCaptureResult getCameraCaptureResult() {
        return mCameraCaptureResult;
    }

    /** Get all the surfaces that the request will write data to. */
    @NonNull
    public List<DeferrableSurface> getSurfaces() {
        return Collections.unmodifiableList(mSurfaces);
    }

    @NonNull
    public Config getImplementationOptions() {
        return mImplementationOptions;
    }

    /**
     * Gets the template type.
     *
     * <p>If not set, returns {@link #TEMPLATE_TYPE_NONE}.
     */
    public int getTemplateType() {
        return mTemplateType;
    }

    @NonNull
    public Range<Integer> getExpectedFrameRateRange() {
        return mExpectedFrameRateRange;
    }

    public boolean isUseRepeatingSurface() {
        return mUseRepeatingSurface;
    }

    /** Obtains all registered {@link CameraCaptureCallback} callbacks. */
    @NonNull
    public List<CameraCaptureCallback> getCameraCaptureCallbacks() {
        return mCameraCaptureCallbacks;
    }

    @NonNull
    public TagBundle getTagBundle() {
        return mTagBundle;
    }

    /**
     * Interface for unpacking a configuration into a CaptureConfig.Builder
     */
    public interface OptionUnpacker {

        /**
         * Apply the options from the config onto the builder
         *
         * @param config  the set of options to apply
         * @param builder the builder on which to apply the options
         */
        void unpack(@NonNull UseCaseConfig<?> config, @NonNull CaptureConfig.Builder builder);
    }

    /**
     * Builder for easy modification/rebuilding of a {@link CaptureConfig}.
     */
    public static final class Builder {
        private final Set<DeferrableSurface> mSurfaces = new HashSet<>();
        private MutableConfig mImplementationOptions = MutableOptionsBundle.create();
        private int mTemplateType = TEMPLATE_TYPE_NONE;
        private Range<Integer> mExpectedFrameRateRange = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED;
        private List<CameraCaptureCallback> mCameraCaptureCallbacks = new ArrayList<>();
        private boolean mUseRepeatingSurface = false;
        private MutableTagBundle mMutableTagBundle = MutableTagBundle.create();
        @Nullable
        private CameraCaptureResult mCameraCaptureResult;

        public Builder() {
        }

        private Builder(CaptureConfig base) {
            mSurfaces.addAll(base.mSurfaces);
            mImplementationOptions = MutableOptionsBundle.from(base.mImplementationOptions);
            mTemplateType = base.mTemplateType;
            mExpectedFrameRateRange = base.mExpectedFrameRateRange;
            mCameraCaptureCallbacks.addAll(base.getCameraCaptureCallbacks());
            mUseRepeatingSurface = base.isUseRepeatingSurface();
            mMutableTagBundle = MutableTagBundle.from(base.getTagBundle());
        }

        /**
         * Creates a {@link Builder} from a {@link UseCaseConfig}.
         *
         * <p>Populates the builder with all the properties defined in the base configuration.
         */
        @NonNull
        public static Builder createFrom(@NonNull UseCaseConfig<?> config) {
            OptionUnpacker unpacker = config.getCaptureOptionUnpacker(null);
            if (unpacker == null) {
                throw new IllegalStateException(
                        "Implementation is missing option unpacker for "
                                + config.getTargetName(config.toString()));
            }

            Builder builder = new Builder();

            // Unpack the configuration into this builder
            unpacker.unpack(config, builder);
            return builder;
        }

        /** Create a {@link Builder} from a {@link CaptureConfig} */
        @NonNull
        public static Builder from(@NonNull CaptureConfig base) {
            return new Builder(base);
        }

        /**
         * Set the {@link CameraCaptureResult} for reprocessable capture request.
         *
         * @param cameraCaptureResult {@link CameraCaptureResult}.
         */
        public void setCameraCaptureResult(@NonNull CameraCaptureResult cameraCaptureResult) {
            mCameraCaptureResult = cameraCaptureResult;
        }

        public int getTemplateType() {
            return mTemplateType;
        }

        @Nullable
        public Range<Integer> getExpectedFrameRateRange() {
            return mExpectedFrameRateRange;
        }

        /**
         * Set the template characteristics of the CaptureConfig.
         *
         * @param templateType Template constant that must match those defined by {@link
         *                     CameraDevice}
         */
        public void setTemplateType(int templateType) {
            mTemplateType = templateType;
        }

        /**
         * Set the expected frame rate range of the CaptureConfig.
         * @param expectedFrameRateRange The frame rate range calculated from the UseCases for
         * {@link CameraDevice}
         */
        public void setExpectedFrameRateRange(@NonNull Range<Integer> expectedFrameRateRange) {
            mExpectedFrameRateRange = expectedFrameRateRange;
        }

        /**
         * Adds a {@link CameraCaptureCallback} callback.
         */
        public void addCameraCaptureCallback(@NonNull CameraCaptureCallback cameraCaptureCallback) {
            if (mCameraCaptureCallbacks.contains(cameraCaptureCallback)) {
                return;
            }
            mCameraCaptureCallbacks.add(cameraCaptureCallback);
        }

        /**
         * Adds all {@link CameraCaptureCallback} callbacks.
         */
        public void addAllCameraCaptureCallbacks(
                @NonNull Collection<CameraCaptureCallback> cameraCaptureCallbacks) {
            for (CameraCaptureCallback c : cameraCaptureCallbacks) {
                addCameraCaptureCallback(c);
            }
        }

        /**
         * Removes a previously added {@link CameraCaptureCallback} callback.
         * @param cameraCaptureCallback The callback to remove.
         * @return {@code true} if the callback was successfully removed. {@code false} if the
         * callback wasn't present in this builder.
         */
        public boolean removeCameraCaptureCallback(
                @NonNull CameraCaptureCallback cameraCaptureCallback) {
            return mCameraCaptureCallbacks.remove(cameraCaptureCallback);
        }

        /** Add a surface that the request will write data to. */
        public void addSurface(@NonNull DeferrableSurface surface) {
            mSurfaces.add(surface);
        }

        /** Remove a surface that the request will write data to. */
        public void removeSurface(@NonNull DeferrableSurface surface) {
            mSurfaces.remove(surface);
        }

        /** Remove all the surfaces that the request will write data to. */
        public void clearSurfaces() {
            mSurfaces.clear();
        }

        /** Gets the surfaces attached to the request. */
        @NonNull
        public Set<DeferrableSurface> getSurfaces() {
            return mSurfaces;
        }

        public void setImplementationOptions(@NonNull Config config) {
            mImplementationOptions = MutableOptionsBundle.from(config);
        }

        /** Add a set of implementation specific options to the request. */
        @SuppressWarnings("unchecked")
        public void addImplementationOptions(@NonNull Config config) {
            for (Config.Option<?> option : config.listOptions()) {
                @SuppressWarnings("unchecked") // Options/values are being copied directly
                        Config.Option<Object> objectOpt = (Config.Option<Object>) option;

                Object existValue = mImplementationOptions.retrieveOption(objectOpt, null);
                Object newValue = config.retrieveOption(objectOpt);
                if (existValue instanceof MultiValueSet) {
                    ((MultiValueSet) existValue).addAll(((MultiValueSet) newValue).getAllItems());
                } else {
                    if (newValue instanceof MultiValueSet) {
                        newValue = ((MultiValueSet) newValue).clone();
                    }
                    mImplementationOptions.insertOption(objectOpt,
                            config.getOptionPriority(option), newValue);
                }
            }
        }

        /** Add a single implementation option to the request. */
        public <T> void addImplementationOption(@NonNull Config.Option<T> option,
                @NonNull T value) {
            mImplementationOptions.insertOption(option, value);
        }

        @NonNull
        public Config getImplementationOptions() {
            return mImplementationOptions;
        }

        public boolean isUseRepeatingSurface() {
            return mUseRepeatingSurface;
        }

        public void setUseRepeatingSurface(boolean useRepeatingSurface) {
            mUseRepeatingSurface = useRepeatingSurface;
        }

        /** Gets a tag's value by a key. */
        @Nullable
        public Object getTag(@NonNull String key) {
            return mMutableTagBundle.getTag(key);
        }

        /**
         * Sets a tag with a key to CaptureConfig.
         */
        public void addTag(@NonNull String key, @NonNull Object tag) {
            mMutableTagBundle.putTag(key, tag);
        }

        /**
         * Adds a TagBundle to CaptureConfig.
         */
        public void addAllTags(@NonNull TagBundle bundle) {
            mMutableTagBundle.addTagBundle(bundle);
        }

        /**
         * Builds an instance of a CaptureConfig that has all the combined parameters of the
         * CaptureConfig that have been added to the Builder.
         */
        @NonNull
        public CaptureConfig build() {
            return new CaptureConfig(
                    new ArrayList<>(mSurfaces),
                    OptionsBundle.from(mImplementationOptions),
                    mTemplateType,
                    mExpectedFrameRateRange,
                    new ArrayList<>(mCameraCaptureCallbacks),
                    mUseRepeatingSurface,
                    TagBundle.from(mMutableTagBundle),
                    mCameraCaptureResult);
        }
    }
}
