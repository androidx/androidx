/*
 * Copyright 2023 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build.VERSION;

import androidx.concurrent.futures.ResolvableFuture;
import androidx.wear.protolayout.proto.ResourceProto.AndroidImageResourceByContentUri;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.AndroidImageResourceByContentUriResolver;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.ResourceAccessException;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

/** Resource resolver for Android resources, accessed by Content URI. */
public class DefaultAndroidImageResourceByContentUriResolver
        implements AndroidImageResourceByContentUriResolver {
    private static final @NonNull String TAG = "AndroidContentUriResolver";

    private final @NonNull ContentUriValidator mContentUriValidator;
    private final @NonNull Resources mPackageResources;
    private final @NonNull ContentResolver mContentResolver;
    private final @NonNull Executor mLoadExecutor;
    private final boolean mRestrictImageSize;

    public DefaultAndroidImageResourceByContentUriResolver(
            @NonNull Context appContext,
            @NonNull String packageName,
            @NonNull Resources packageResources,
            @NonNull ContentResolver contentResolver,
            @NonNull Executor loadExecutor) {
        this(
                appContext,
                packageName,
                packageResources,
                contentResolver,
                loadExecutor,
                /* restrictImageSize= */ false);
    }

    public DefaultAndroidImageResourceByContentUriResolver(
            @NonNull Context appContext,
            @NonNull String packageName,
            @NonNull Resources packageResources,
            @NonNull ContentResolver contentResolver,
            @NonNull Executor loadExecutor,
            boolean restrictImageSize) {
        this.mContentUriValidator = new ContentUriValidator(appContext, packageName);
        this.mPackageResources = packageResources;
        this.mContentResolver = contentResolver;
        this.mLoadExecutor = loadExecutor;
        this.mRestrictImageSize = restrictImageSize;
    }

    private @NonNull Drawable getDrawableBlocking(
            @NonNull AndroidImageResourceByContentUri resource) throws ResourceAccessException {
        Uri resourceUri = Uri.parse(resource.getContentUri());
        if (!mContentUriValidator.validateUri(resourceUri)) {
            throw new IllegalArgumentException(
                    "Provided content URI " + resource.getContentUri() + " cannot be opened");
        }

        if (VERSION.SDK_INT >= 28 && mRestrictImageSize) {
            return ConstrainedImageDecoder.decodeDrawable(mContentResolver, resourceUri);
        }

        try (InputStream inStream = mContentResolver.openInputStream(resourceUri)) {
            // Can happen if the content provider recently crashed...
            if (inStream == null) {
                throw new ResourceAccessException(
                        "Cannot read from URI " + resource.getContentUri());
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inStream);
            if (bitmap == null) {
                throw new ResourceAccessException(
                        "Failed to decode image from URI " + resource.getContentUri());
            }
            return new BitmapDrawable(mPackageResources, bitmap);
        } catch (FileNotFoundException ex) {
            throw new ResourceAccessException(
                    "Cannot open file for URI " + resource.getContentUri(), ex);
        } catch (IOException ex) {
            throw new ResourceAccessException(
                    "Error while reading URI " + resource.getContentUri(), ex);
        }
    }

    @Override
    public @NonNull ListenableFuture<Drawable> getDrawable(
            @NonNull AndroidImageResourceByContentUri resource) {
        ResolvableFuture<Drawable> resolvableFuture = ResolvableFuture.create();
        mLoadExecutor.execute(
                () -> {
                    try {
                        Drawable d = getDrawableBlocking(resource);
                        resolvableFuture.set(d);
                    } catch (Exception ex) {
                        resolvableFuture.setException(ex);
                    }
                });
        return resolvableFuture;
    }
}
