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
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
public final class CaptureConfig {
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

    /** The camera capture callback for a {@link CameraCaptureSession}. */
    final List<CameraCaptureCallback> mCameraCaptureCallbacks;

    /** True if this capture request needs a repeating surface */
    private final boolean mUseRepeatingSurface;

    /** The tag collection for associating capture result with capture request. */
    @NonNull
    private final TagBundle mTagBundle;

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
     */
    CaptureConfig(
            List<DeferrableSurface> surfaces,
            Config implementationOptions,
            int templateType,
            List<CameraCaptureCallback> cameraCaptureCallbacks,
            boolean useRepeatingSurface,
            @NonNull TagBundle tagBundle) {
        mSurfaces = surfaces;
        mImplementationOptions = implementationOptions;
        mTemplateType = templateType;
        mCameraCaptureCallbacks = Collections.unmodifiableList(cameraCaptureCallbacks);
        mUseRepeatingSurface = useRepeatingSurface;
        mTagBundle = tagBundle;
    }

    /** Returns an instance of a capture configuration with minimal configurations. */
    @NonNull
    public static CaptureConfig defaultEmptyCaptureConfig() {
        return new CaptureConfig.Builder().build();
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

    public int getTemplateType() {
        return mTemplateType;
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
        private int mTemplateType = -1;
        private List<CameraCaptureCallback> mCameraCaptureCallbacks = new ArrayList<>();
        private boolean mUseRepeatingSurface = false;
        private MutableTagBundle mMutableTagBundle = MutableTagBundle.create();

        public Builder() {
        }

        private Builder(CaptureConfig base) {
            mSurfaces.addAll(base.mSurfaces);
            mImplementationOptions = MutableOptionsBundle.from(base.mImplementationOptions);
            mTemplateType = base.mTemplateType;
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

        public int getTemplateType() {
            return mTemplateType;
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
         * Adds a {@link CameraCaptureSession.StateCallback} callback.
         */
        public void addCameraCaptureCallback(@NonNull CameraCaptureCallback cameraCaptureCallback) {
            if (mCameraCaptureCallbacks.contains(cameraCaptureCallback)) {
                return;
            }
            mCameraCaptureCallbacks.add(cameraCaptureCallback);
        }

        /**
         * Adds all {@link CameraCaptureSession.StateCallback} callbacks.
         */
        public void addAllCameraCaptureCallbacks(
                @NonNull Collection<CameraCaptureCallback> cameraCaptureCallbacks) {
            for (CameraCaptureCallback c : cameraCaptureCallbacks) {
                addCameraCaptureCallback(c);
            }
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

        boolean isUseRepeatingSurface() {
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
                    mCameraCaptureCallbacks,
                    mUseRepeatingSurface,
                    TagBundle.from(mMutableTagBundle));
        }
    }
}
