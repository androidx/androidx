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

package androidx.wear.tiles.renderer.internal;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.wear.tiles.proto.ResourceProto;
import androidx.wear.tiles.proto.ResourceProto.AndroidImageResourceByResId;
import androidx.wear.tiles.proto.ResourceProto.InlineImageResource;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Class for resolving resources. Delegates the actual work to different types of resolver classes,
 * and allows each type of resolver to be configured individually, as well as instantiation from
 * common resolver implementations.
 */
public class ResourceResolvers {
    private final ResourceProto.Resources mProtoResources;

    @Nullable
    private final AndroidImageResourceByResIdResolver mAndroidImageResourceByResIdResolver;

    @Nullable private final InlineImageResourceResolver mInlineImageResourceResolver;

    ResourceResolvers(
            @NonNull ResourceProto.Resources protoResources,
            @Nullable AndroidImageResourceByResIdResolver androidImageResourceByResIdResolver,
            @Nullable InlineImageResourceResolver inlineImageResourceResolver) {
        this.mProtoResources = protoResources;
        this.mAndroidImageResourceByResIdResolver = androidImageResourceByResIdResolver;
        this.mInlineImageResourceResolver = inlineImageResourceResolver;
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
         * @throws ResourceAccessException If the drawable cannot be found, or has to be loaded
         *     asynchronously.
         */
        @NonNull
        Drawable getDrawableOrThrow(@NonNull AndroidImageResourceByResId resource)
                throws ResourceAccessException;

        /** Get the drawable as specified by {@code resource}. */
        @NonNull
        ListenableFuture<Drawable> getDrawable(@NonNull AndroidImageResourceByResId resource);
    }

    /** Interface that can provide a Drawable for an InlineImageResource */
    public interface InlineImageResourceResolver {
        /**
         * Should immediately return the drawable specified by {@code resource}.
         *
         * @throws ResourceAccessException If the drawable cannot be found, or has to be loaded
         *     asynchronously.
         */
        @NonNull
        Drawable getDrawableOrThrow(@NonNull InlineImageResource resource)
                throws ResourceAccessException;

        /** Get the drawable as specified by {@code resource}. */
        @NonNull
        ListenableFuture<Drawable> getDrawable(@NonNull InlineImageResource resource);
    }

    /** Get an empty builder to build {@link ResourceResolvers} with. */
    @NonNull
    public static Builder builder(@NonNull ResourceProto.Resources protoResources) {
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
    @NonNull
    public Drawable getPlaceholderDrawableOrThrow(@NonNull String protoResourceId)
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

        if (placeholderImageResource.hasAndroidResourceByResId()
                && mAndroidImageResourceByResIdResolver != null) {
            AndroidImageResourceByResIdResolver resolver = mAndroidImageResourceByResIdResolver;
            return resolver.getDrawableOrThrow(
                    placeholderImageResource.getAndroidResourceByResId());
        }

        if (placeholderImageResource.hasInlineResource() && mInlineImageResourceResolver != null) {
            InlineImageResourceResolver resolver = mInlineImageResourceResolver;
            return resolver.getDrawableOrThrow(placeholderImageResource.getInlineResource());
        }

        throw new ResourceAccessException("Can't find resolver for image resource.");
    }

    /** Get the drawable corresponding to the given resource ID. */
    @NonNull
    public ListenableFuture<Drawable> getDrawable(@NonNull String protoResourceId) {
        ResourceProto.ImageResource imageResource =
                mProtoResources.getIdToImageMap().get(protoResourceId);

        if (imageResource == null) {
            return createFailedFuture(
                    new IllegalArgumentException(
                            "Resource " + protoResourceId + " is not defined in resources bundle"));
        }

        if (imageResource.hasAndroidResourceByResId()
                && mAndroidImageResourceByResIdResolver != null) {
            AndroidImageResourceByResIdResolver resolver = mAndroidImageResourceByResIdResolver;
            return resolver.getDrawable(imageResource.getAndroidResourceByResId());
        }

        if (imageResource.hasInlineResource() && mInlineImageResourceResolver != null) {
            InlineImageResourceResolver resolver = mInlineImageResourceResolver;
            return resolver.getDrawable(imageResource.getInlineResource());
        }

        return createFailedFuture(
                new ResourceAccessException(
                        "Can't find resolver for image resource " + protoResourceId));
    }

    /** Returns whether an image can be tinted or not. */
    public boolean canImageBeTinted(@NonNull String protoResourceId) {
        // Only Android image resources can be tinted for now. This is because we don't really know
        // what
        // is in an inline image.
        ResourceProto.ImageResource imageResource =
                mProtoResources.getIdToImageMap().get(protoResourceId);

        if (imageResource == null) {
            throw new IllegalArgumentException(
                    "Resource " + protoResourceId + " is not defined in resources bundle");
        }

        if (imageResource.hasAndroidResourceByResId()) {
            return true;
        }

        return false;
    }

    @Nullable
    private String getPlaceholderResourceId(@NonNull String originalResourceId) {
        ResourceProto.ImageResource imageResource =
                mProtoResources.getIdToImageMap().get(originalResourceId);

        if (imageResource == null) {
            throw new IllegalArgumentException(
                    "Resource " + originalResourceId + " is not defined in resources bundle");
        }

        return null;
    }

    @SuppressLint("RestrictedApi") // TODO(b/183006740): Remove when prefix check is fixed.
    static <T> ListenableFuture<T> createImmediateFuture(@NonNull T value) {
        ResolvableFuture<T> future = ResolvableFuture.create();
        future.set(value);
        return future;
    }

    @SuppressLint("RestrictedApi") // TODO(b/183006740): Remove when prefix check is fixed.
    static <T> ListenableFuture<T> createFailedFuture(@NonNull Throwable throwable) {
        ResolvableFuture<T> errorFuture = ResolvableFuture.create();
        errorFuture.setException(throwable);
        return errorFuture;
    }

    /** Builder for ResourceProviders */
    public static final class Builder {
        @NonNull private final ResourceProto.Resources mProtoResources;
        @Nullable private AndroidImageResourceByResIdResolver mAndroidImageResourceByResIdResolver;
        @Nullable private InlineImageResourceResolver mInlineImageResourceResolver;

        Builder(@NonNull ResourceProto.Resources protoResources) {
            this.mProtoResources = protoResources;
        }

        /** Set the resource loader for {@link AndroidImageResourceByResIdResolver} resources. */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setAndroidImageResourceByResIdResolver(
                @NonNull AndroidImageResourceByResIdResolver resolver) {
            mAndroidImageResourceByResIdResolver = resolver;
            return this;
        }

        /** Set the resource loader for {@link InlineImageResourceResolver} resources. */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setInlineImageResourceResolver(
                @NonNull InlineImageResourceResolver resolver) {
            mInlineImageResourceResolver = resolver;
            return this;
        }

        /** Build a {@link ResourceResolvers} instance. */
        @NonNull
        public ResourceResolvers build() {
            return new ResourceResolvers(
                    mProtoResources,
                    mAndroidImageResourceByResIdResolver,
                    mInlineImageResourceResolver);
        }
    }
}
