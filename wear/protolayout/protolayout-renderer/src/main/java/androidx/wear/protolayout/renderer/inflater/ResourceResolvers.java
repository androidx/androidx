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

package androidx.wear.protolayout.renderer.inflater;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;

import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.proto.ResourceProto;
import androidx.wear.protolayout.proto.ResourceProto.AndroidAnimatedImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AndroidImageResourceByContentUri;
import androidx.wear.protolayout.proto.ResourceProto.AndroidImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.AndroidSeekableAnimatedImageResourceByResId;
import androidx.wear.protolayout.proto.ResourceProto.InlineImageResource;
import androidx.wear.protolayout.proto.TriggerProto.Trigger;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Class for resolving resources. Delegates the actual work to different types of resolver classes,
 * and allows each type of resolver to be configured individually, as well as instantiation from
 * common resolver implementations.
 */
public class ResourceResolvers {
    private final ResourceProto.@NonNull Resources mProtoResources;

    private final @Nullable AndroidImageResourceByResIdResolver
            mAndroidImageResourceByResIdResolver;

    private final @Nullable AndroidAnimatedImageResourceByResIdResolver
            mAndroidAnimatedImageResourceByResIdResolver;

    private final @Nullable AndroidSeekableAnimatedImageResourceByResIdResolver
            mAndroidSeekableAnimatedImageResourceByResIdResolver;

    private final @Nullable InlineImageResourceResolver mInlineImageResourceResolver;

    private final @Nullable AndroidImageResourceByContentUriResolver
            mAndroidImageResourceByContentUriResolver;

    ResourceResolvers(
            ResourceProto.@NonNull Resources protoResources,
            @Nullable AndroidImageResourceByResIdResolver androidImageResourceByResIdResolver,
            @Nullable AndroidAnimatedImageResourceByResIdResolver
                    androidAnimatedImageResourceByResIdResolver,
            @Nullable AndroidSeekableAnimatedImageResourceByResIdResolver
                    androidSeekableAnimatedImageResourceByResIdResolver,
            @Nullable InlineImageResourceResolver inlineImageResourceResolver,
            @Nullable AndroidImageResourceByContentUriResolver androidContentUriResolver) {
        this.mProtoResources = protoResources;
        this.mAndroidImageResourceByResIdResolver = androidImageResourceByResIdResolver;
        this.mAndroidAnimatedImageResourceByResIdResolver =
                androidAnimatedImageResourceByResIdResolver;
        this.mAndroidSeekableAnimatedImageResourceByResIdResolver =
                androidSeekableAnimatedImageResourceByResIdResolver;
        this.mInlineImageResourceResolver = inlineImageResourceResolver;
        this.mAndroidImageResourceByContentUriResolver = androidContentUriResolver;
    }

    /** Exception thrown when accessing resources. */
    public static final class ResourceAccessException extends Exception {
        public ResourceAccessException(@NonNull String description) {
            super(description);
        }

        public ResourceAccessException(@NonNull String description, @NonNull Exception cause) {
            super(description, cause);
        }
    }

    /** Interface that can provide a Drawable for an AndroidImageResourceByResId */
    public interface AndroidImageResourceByResIdResolver {
        /**
         * Should immediately return the drawable specified by {@code resource}.
         *
         * @throws ResourceAccessException If the drawable cannot be found
         */
        @NonNull Drawable getDrawableOrThrow(@NonNull AndroidImageResourceByResId resource)
                throws ResourceAccessException;
    }

    /** Interface that can provide a Drawable for an AndroidAnimatedImageResourceByResId */
    public interface AndroidAnimatedImageResourceByResIdResolver {
        /**
         * Should immediately return the drawable specified by {@code resource}.
         *
         * @throws ResourceAccessException If the drawable cannot be found.
         */
        @NonNull Drawable getDrawableOrThrow(@NonNull AndroidAnimatedImageResourceByResId resource)
                throws ResourceAccessException;
    }

    /** Interface that can provide a Drawable for an AndroidSeekableAnimatedImageResourceByResId */
    public interface AndroidSeekableAnimatedImageResourceByResIdResolver {
        /**
         * Should immediately return the drawable specified by {@code resource}.
         *
         * @throws ResourceAccessException If the drawable cannot be found.
         */
        @NonNull Drawable getDrawableOrThrow(
                @NonNull AndroidSeekableAnimatedImageResourceByResId resource)
                throws ResourceAccessException;
    }

    /** Interface that can provide a Drawable for an InlineImageResource */
    public interface InlineImageResourceResolver {
        /**
         * Should immediately return the drawable specified by {@code resource}.
         *
         * @throws ResourceAccessException If the drawable cannot be found,.
         */
        @NonNull Drawable getDrawableOrThrow(@NonNull InlineImageResource resource)
                throws ResourceAccessException;
    }

    /** Interface that can provide a Drawable for an AndroidContentUriResource. */
    public interface AndroidImageResourceByContentUriResolver {
        /** Get the drawable as specified by {@code resource}, to be loaded asynchronously. */
        @NonNull ListenableFuture<Drawable> getDrawable(
                @NonNull AndroidImageResourceByContentUri resource);
    }

    /** Get an empty builder to build {@link ResourceResolvers} with. */
    public static @NonNull Builder builder(ResourceProto.@NonNull Resources protoResources) {
        return new Builder(protoResources);
    }

    /**
     * Returns whether the resource specified by {@code protoResourceId} has a placeholder resource
     * associated with it.
     */
    public boolean hasPlaceholderDrawable(@NonNull String protoResourceId) {
        return getPlaceholderResourceId(protoResourceId) != null;
    }

    /**
     * Returns the placeholder drawable for the resource specified by {@code protoResourceId}.
     *
     * @throws ResourceAccessException If the specified resource does not have a placeholder
     *     associated, or the placeholder could not be loaded.
     * @throws IllegalArgumentException If the specified resource, or its placeholder, does not
     *     exist.
     * @see ResourceResolvers#hasPlaceholderDrawable(String)
     */
    public @NonNull Drawable getPlaceholderDrawableOrThrow(@NonNull String protoResourceId)
            throws ResourceAccessException {
        String placeholderResourceId = getPlaceholderResourceId(protoResourceId);

        if (placeholderResourceId == null) {
            throw new ResourceAccessException(
                    "Resource " + protoResourceId + " does not have a placeholder resource.");
        }

        ResourceProto.ImageResource placeholderImageResource =
                mProtoResources.getIdToImageMap().get(placeholderResourceId);

        if (placeholderImageResource == null) {
            throw new IllegalArgumentException(
                    "Resource " + placeholderResourceId + " is not defined in resources bundle");
        }

        Drawable placeHolderDrawable =
                getDrawableForImageResourceSynchronously(placeholderImageResource);
        if (placeHolderDrawable != null) {
            return placeHolderDrawable;
        }

        if (placeholderImageResource.hasAndroidContentUri()) {
            throw new ResourceAccessException("Content URI images cannot be used as placeholders");
        }

        throw new ResourceAccessException("Can't find resolver for image resource.");
    }

    /** Get the drawable corresponding to the given resource ID. */
    public @NonNull ListenableFuture<Drawable> getDrawable(@NonNull String protoResourceId) {
        ResourceProto.ImageResource imageResource =
                mProtoResources.getIdToImageMap().get(protoResourceId);

        if (imageResource == null) {
            return Futures.immediateFailedFuture(
                    new IllegalArgumentException(
                            "Resource " + protoResourceId + " is not defined in resources bundle"));
        }

        ListenableFuture<Drawable> drawableFutureOrNull =
                getDrawableForImageResource(imageResource);
        if (drawableFutureOrNull == null) {
            return Futures.immediateFailedFuture(
                    new ResourceAccessException(
                            "Can't find resolver for image resource " + protoResourceId));
        }
        return drawableFutureOrNull;
    }

    /**
     * Get the animation trigger for the given animated image resource id
     *
     * @throws IllegalArgumentException If the resource is not an animated resource.
     */
    public @Nullable Trigger getAnimationTrigger(@NonNull String protoResourceId) {
        ResourceProto.ImageResource imageResource =
                mProtoResources.getIdToImageMap().get(protoResourceId);
        if (imageResource != null && imageResource.hasAndroidAnimatedResourceByResId()) {
            return imageResource.getAndroidAnimatedResourceByResId().getStartTrigger();
        }
        throw new IllegalArgumentException(
                "Resource "
                        + protoResourceId
                        + " is not an animated resource, thus no animation trigger");
    }

    /**
     * Get the animation bound progress for the given animated image resource id
     *
     * @throws IllegalArgumentException If the resource is not a seekable animated resource.
     */
    public @Nullable DynamicFloat getBoundProgress(@NonNull String protoResourceId) {
        ResourceProto.ImageResource imageResource =
                mProtoResources.getIdToImageMap().get(protoResourceId);
        if (imageResource != null && imageResource.hasAndroidSeekableAnimatedResourceByResId()) {
            return imageResource.getAndroidSeekableAnimatedResourceByResId().getProgress();
        }
        throw new IllegalArgumentException(
                "Resource "
                        + protoResourceId
                        + " is not a seekable animated resource, thus no bound progress to a"
                        + " DynamicFloat");
    }

    @Nullable Drawable getDrawableForImageResourceSynchronously(
            ResourceProto.@NonNull ImageResource imageResource) throws ResourceAccessException {
        if (imageResource.hasAndroidAnimatedResourceByResId()
                && mAndroidAnimatedImageResourceByResIdResolver != null) {
            AndroidAnimatedImageResourceByResIdResolver resolver =
                    mAndroidAnimatedImageResourceByResIdResolver;
            return resolver.getDrawableOrThrow(imageResource.getAndroidAnimatedResourceByResId());
        }

        if (imageResource.hasAndroidSeekableAnimatedResourceByResId()
                && mAndroidSeekableAnimatedImageResourceByResIdResolver != null) {
            AndroidSeekableAnimatedImageResourceByResIdResolver resolver =
                    mAndroidSeekableAnimatedImageResourceByResIdResolver;
            return resolver.getDrawableOrThrow(
                    imageResource.getAndroidSeekableAnimatedResourceByResId());
        }

        if (imageResource.hasAndroidResourceByResId()
                && mAndroidImageResourceByResIdResolver != null) {
            AndroidImageResourceByResIdResolver resolver = mAndroidImageResourceByResIdResolver;
            return resolver.getDrawableOrThrow(imageResource.getAndroidResourceByResId());
        }

        if (imageResource.hasInlineResource() && mInlineImageResourceResolver != null) {
            InlineImageResourceResolver resolver = mInlineImageResourceResolver;
            return resolver.getDrawableOrThrow(imageResource.getInlineResource());
        }

        return null;
    }

    /**
     * Get the drawable for the known ImageResource. Can return null if there's no resolver for the
     * image resource.
     */
    protected @Nullable ListenableFuture<Drawable> getDrawableForImageResource(
            ResourceProto.@NonNull ImageResource imageResource) {
        try {
            Drawable drawable = getDrawableForImageResourceSynchronously(imageResource);
            if (drawable != null) {
                return Futures.immediateFuture(drawable);
            }
        } catch (ResourceAccessException e) {
            return Futures.immediateFailedFuture(e);
        }

        if (imageResource.hasAndroidContentUri()
                && mAndroidImageResourceByContentUriResolver != null) {
            AndroidImageResourceByContentUriResolver resolver =
                    mAndroidImageResourceByContentUriResolver;
            return resolver.getDrawable(imageResource.getAndroidContentUri());
        }

        // Can't find resolver for image resource.
        return null;
    }

    public boolean canImageBeTinted(@NonNull String protoResourceId) {
        // Only Android image resources can be tinted for now. This is because we don't really know
        // what is in an inline image.
        ResourceProto.ImageResource imageResource =
                mProtoResources.getIdToImageMap().get(protoResourceId);

        if (imageResource == null) {
            throw new IllegalArgumentException(
                    "Resource " + protoResourceId + " is not defined in resources bundle");
        }

        if (imageResource.hasAndroidResourceByResId()
                || imageResource.hasAndroidAnimatedResourceByResId()
                || imageResource.hasAndroidSeekableAnimatedResourceByResId()) {
            return true;
        }

        return false;
    }

    protected @Nullable String getPlaceholderResourceId(@NonNull String originalResourceId) {
        ResourceProto.ImageResource imageResource =
                mProtoResources.getIdToImageMap().get(originalResourceId);

        if (imageResource == null) {
            throw new IllegalArgumentException(
                    "Resource " + originalResourceId + " is not defined in resources bundle");
        }

        return null;
    }

    /** Builder for ResourceResolvers */
    public static final class Builder {
        private final ResourceProto.@NonNull Resources mProtoResources;
        private @Nullable AndroidImageResourceByResIdResolver mAndroidImageResourceByResIdResolver;

        private @Nullable AndroidAnimatedImageResourceByResIdResolver
                mAndroidAnimatedImageResourceByResIdResolver;

        private @Nullable AndroidSeekableAnimatedImageResourceByResIdResolver
                mAndroidSeekableAnimatedImageResourceByResIdResolver;

        private @Nullable InlineImageResourceResolver mInlineImageResourceResolver;

        private @Nullable AndroidImageResourceByContentUriResolver
                mAndroidImageResourceByContentUriResolver;

        Builder(ResourceProto.@NonNull Resources protoResources) {
            this.mProtoResources = protoResources;
        }

        /** Set the resource loader for {@link AndroidImageResourceByResIdResolver} resources. */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setAndroidImageResourceByResIdResolver(
                @NonNull AndroidImageResourceByResIdResolver resolver) {
            mAndroidImageResourceByResIdResolver = resolver;
            return this;
        }

        /**
         * Set the resource loader for {@link AndroidAnimatedImageResourceByResIdResolver}
         * resources.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setAndroidAnimatedImageResourceByResIdResolver(
                @NonNull AndroidAnimatedImageResourceByResIdResolver resolver) {
            mAndroidAnimatedImageResourceByResIdResolver = resolver;
            return this;
        }

        /**
         * Set the resource loader for {@link AndroidSeekableAnimatedImageResourceByResIdResolver}
         * resources.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setAndroidSeekableAnimatedImageResourceByResIdResolver(
                @NonNull AndroidSeekableAnimatedImageResourceByResIdResolver resolver) {
            mAndroidSeekableAnimatedImageResourceByResIdResolver = resolver;
            return this;
        }

        /** Set the resource loader for {@link InlineImageResourceResolver} resources. */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setInlineImageResourceResolver(
                @NonNull InlineImageResourceResolver resolver) {
            mInlineImageResourceResolver = resolver;
            return this;
        }

        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setAndroidImageResourceByContentUriResolver(
                @NonNull AndroidImageResourceByContentUriResolver resolver) {
            mAndroidImageResourceByContentUriResolver = resolver;
            return this;
        }

        /** Build a {@link ResourceResolvers} instance. */
        public @NonNull ResourceResolvers build() {
            return new ResourceResolvers(
                    mProtoResources,
                    mAndroidImageResourceByResIdResolver,
                    mAndroidAnimatedImageResourceByResIdResolver,
                    mAndroidSeekableAnimatedImageResourceByResIdResolver,
                    mInlineImageResourceResolver,
                    mAndroidImageResourceByContentUriResolver);
        }
    }
}
