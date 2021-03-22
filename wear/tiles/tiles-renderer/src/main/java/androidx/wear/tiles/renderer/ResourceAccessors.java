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

package androidx.wear.tiles.renderer;

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
 * Class for accessing resources. Delegates the actual work to different types of accessor classes,
 * and allows each type of accessor to be configured individually, as well as instantiation from
 * common accessor implementations.
 */
public class ResourceAccessors {
    private final ResourceProto.Resources mProtoResources;

    @Nullable
    private final AndroidImageResourceByResIdAccessor mAndroidImageResourceByResIdAccessor;

    @Nullable private final InlineImageResourceAccessor mInlineImageResourceAccessor;

    ResourceAccessors(
            ResourceProto.Resources protoResources,
            @Nullable AndroidImageResourceByResIdAccessor androidImageResourceByResIdAccessor,
            @Nullable InlineImageResourceAccessor inlineImageResourceAccessor) {
        this.mProtoResources = protoResources;
        this.mAndroidImageResourceByResIdAccessor = androidImageResourceByResIdAccessor;
        this.mInlineImageResourceAccessor = inlineImageResourceAccessor;
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
    public interface AndroidImageResourceByResIdAccessor {
        /** Get the drawable as specified by {@code resource}. */
        @NonNull
        ListenableFuture<Drawable> getDrawable(@NonNull AndroidImageResourceByResId resource);
    }

    /** Interface that can provide a Drawable for an InlineImageResource */
    public interface InlineImageResourceAccessor {
        /** Get the drawable as specified by {@code resource}. */
        @NonNull
        ListenableFuture<Drawable> getDrawable(@NonNull InlineImageResource resource);
    }

    /** Get an empty builder to build {@link ResourceAccessors} with. */
    @NonNull
    public static Builder builder(@NonNull ResourceProto.Resources protoResources) {
        return new Builder(protoResources);
    }

    /** Get the drawable corresponding to the given resource ID. */
    @NonNull
    @SuppressLint("RestrictedApi") // TODO(b/183006740): Remove when prefix check is fixed.
    public ListenableFuture<Drawable> getDrawable(@NonNull String protoResourceId) {
        ResolvableFuture<Drawable> errorFuture = ResolvableFuture.create();
        ResourceProto.ImageResource imageResource =
                mProtoResources.getIdToImageMap().get(protoResourceId);

        if (imageResource == null) {
            errorFuture.setException(
                    new IllegalArgumentException(
                            "Resource " + protoResourceId + " is not defined in resources bundle"));
            return errorFuture;
        }

        if (imageResource.hasAndroidResourceByResid()
                && mAndroidImageResourceByResIdAccessor != null) {
            AndroidImageResourceByResIdAccessor accessor = mAndroidImageResourceByResIdAccessor;
            return accessor.getDrawable(imageResource.getAndroidResourceByResid());
        }

        if (imageResource.hasInlineResource() && mInlineImageResourceAccessor != null) {
            InlineImageResourceAccessor accessor = mInlineImageResourceAccessor;
            return accessor.getDrawable(imageResource.getInlineResource());
        }

        errorFuture.setException(
                new IllegalArgumentException(
                        new ResourceAccessException("Can't find accessor for image resource.")));
        return errorFuture;
    }

    /** Builder for ResourceProviders */
    public static final class Builder {
        private final ResourceProto.Resources mProtoResources;
        @Nullable private AndroidImageResourceByResIdAccessor mAndroidImageResourceByResIdAccessor;
        @Nullable private InlineImageResourceAccessor mInlineImageResourceAccessor;

        Builder(ResourceProto.Resources protoResources) {
            this.mProtoResources = protoResources;
        }

        /** Set the resource loader for {@link AndroidImageResourceByResIdAccessor} resources. */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setAndroidImageResourceByResIdAccessor(
                @NonNull AndroidImageResourceByResIdAccessor accessor) {
            mAndroidImageResourceByResIdAccessor = accessor;
            return this;
        }

        /** Set the resource loader for {@link InlineImageResourceAccessor} resources. */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setInlineImageResourceAccessor(
                @NonNull InlineImageResourceAccessor accessor) {
            mInlineImageResourceAccessor = accessor;
            return this;
        }

        /** Build a {@link ResourceAccessors} instance. */
        @NonNull
        public ResourceAccessors build() {
            return new ResourceAccessors(
                    mProtoResources,
                    mAndroidImageResourceByResIdAccessor,
                    mInlineImageResourceAccessor);
        }
    }
}
