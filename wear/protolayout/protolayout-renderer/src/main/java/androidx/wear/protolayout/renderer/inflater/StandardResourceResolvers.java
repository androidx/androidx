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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.wear.protolayout.proto.ResourceProto;

import java.util.concurrent.Executor;

/** Utility class to get {@link ResourceResolvers} populated with standard options. */
public class StandardResourceResolvers {
    private StandardResourceResolvers() {}

    /**
     * Get a builder pre-populated with resolvers for the resources of the app hosting the renderer.
     *
     * <p>Use {@code setFooResolver} calls to change the pre-populated ones or add others.
     *
     * @param protoResources ProtoLayout resources for the current layout.
     * @param appContext Context for the app that both owns the resources and displays the layout.
     * @param asyncLoadExecutor The executor to use to load any async resources (e.g. Content URI).
     */
    @SuppressLint("CheckResult")
    @NonNull
    public static ResourceResolvers.Builder forLocalApp(
            @NonNull ResourceProto.Resources protoResources,
            @NonNull Context appContext,
            @NonNull Executor asyncLoadExecutor,
            boolean animationEnabled) {
        DefaultAndroidImageResourceByResIdResolver androidResourceResolver =
                new DefaultAndroidImageResourceByResIdResolver(appContext.getResources());

        DefaultInlineImageResourceResolver inlineResourceResolver =
                new DefaultInlineImageResourceResolver(appContext);
        DefaultAndroidImageResourceByContentUriResolver contentUriResolver = null;

        if (asyncLoadExecutor != null) {
            contentUriResolver =
                    new DefaultAndroidImageResourceByContentUriResolver(
                            appContext,
                            appContext.getPackageName(),
                            appContext.getResources(),
                            appContext.getContentResolver(),
                            asyncLoadExecutor);
        }

        ResourceResolvers.Builder builder =
                ResourceResolvers.builder(protoResources)
                        .setAndroidImageResourceByResIdResolver(androidResourceResolver)
                        .setInlineImageResourceResolver(inlineResourceResolver);

        if (contentUriResolver != null) {
            builder.setAndroidImageResourceByContentUriResolver(contentUriResolver);
        }

        if (animationEnabled) {
            DefaultAndroidAnimatedImageResourceByResIdResolver androidAnimatedResourceResolver =
                    new DefaultAndroidAnimatedImageResourceByResIdResolver(
                            appContext.getResources());

            DefaultAndroidSeekableAnimatedImageResourceByResIdResolver
                    androidSeekableAnimatedResourceResolver =
                            new DefaultAndroidSeekableAnimatedImageResourceByResIdResolver(
                                    appContext.getResources());

            builder.setAndroidAnimatedImageResourceByResIdResolver(androidAnimatedResourceResolver)
                    .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                            androidSeekableAnimatedResourceResolver);
        }

        return builder;
    }

    /**
     * Get a builder pre-populated with resolvers for the resources of a {@link TileService}, hosted
     * within another app on the device.
     *
     * <p>Use {@code setFooAccessor} calls to change the pre-populated ones or add others.
     *
     * @param protoResources ProtoLayout resources for the current layout.
     * @param servicePackageName Package name for the service that owns the resources.
     * @param serviceAndroidResources Android resources from the service.
     * @param hostAppContext Context for the app hosting the renderer displaying the layout.
     * @param asyncLoadExecutor The executor to use to load any async resources (e.g. Content URI).
     * @param animationEnabled Whether animation is enabled, which decides whether to load AVD
     *     resources.
     */
    @SuppressLint("CheckResult")
    @NonNull
    public static ResourceResolvers.Builder forRemoteService(
            @NonNull ResourceProto.Resources protoResources,
            @NonNull String servicePackageName,
            @NonNull Resources serviceAndroidResources,
            @NonNull Context hostAppContext,
            @NonNull Executor asyncLoadExecutor,
            boolean animationEnabled) {
        DefaultAndroidImageResourceByResIdResolver androidResourceResolver =
                new DefaultAndroidImageResourceByResIdResolver(serviceAndroidResources);

        DefaultInlineImageResourceResolver inlineResourceResolver =
                new DefaultInlineImageResourceResolver(hostAppContext);
        DefaultAndroidImageResourceByContentUriResolver contentUriResolver = null;

        if (asyncLoadExecutor != null) {
            contentUriResolver =
                    new DefaultAndroidImageResourceByContentUriResolver(
                            hostAppContext,
                            servicePackageName,
                            serviceAndroidResources,
                            hostAppContext.getContentResolver(),
                            asyncLoadExecutor);
        }

        ResourceResolvers.Builder builder =
                ResourceResolvers.builder(protoResources)
                        .setAndroidImageResourceByResIdResolver(androidResourceResolver)
                        .setInlineImageResourceResolver(inlineResourceResolver);

        if (contentUriResolver != null) {
            builder.setAndroidImageResourceByContentUriResolver(contentUriResolver);
        }

        if (animationEnabled) {
            DefaultAndroidAnimatedImageResourceByResIdResolver androidAnimatedResourceResolver =
                    new DefaultAndroidAnimatedImageResourceByResIdResolver(serviceAndroidResources);

            DefaultAndroidSeekableAnimatedImageResourceByResIdResolver
                    androidSeekableAnimatedResourceResolver =
                            new DefaultAndroidSeekableAnimatedImageResourceByResIdResolver(
                                    serviceAndroidResources);

            builder.setAndroidAnimatedImageResourceByResIdResolver(androidAnimatedResourceResolver)
                    .setAndroidSeekableAnimatedImageResourceByResIdResolver(
                            androidSeekableAnimatedResourceResolver);
        }

        return builder;
    }
}
