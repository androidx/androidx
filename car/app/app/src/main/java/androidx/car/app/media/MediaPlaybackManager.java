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

package androidx.car.app.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.car.app.utils.ThreadUtils.checkMainThread;

import static java.util.Objects.requireNonNull;

import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.managers.Manager;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;

/**
 * Manager for communicating Media Session Token the host.
 *
 * <p>Apps must use this interface to coordinate with the car system to allow media playback.
 *
 */
@ExperimentalCarApi
public class MediaPlaybackManager implements Manager {
    private final HostDispatcher mHostDispatcher;

    /**
     * <p>Sends a media session token to the host in order to
     * allow the host to read the media session playback state.
     *
     * @param token to be sent to the host
     * @throws IllegalArgumentException if any of the token cannot be serialized
     * @throws IllegalStateException    if this is not called from the main thread.
     */
    @MainThread
    public void registerMediaPlaybackToken(MediaSessionCompat.@NonNull Token token) {
        checkMainThread();
        Bundleable bundle;
        try {
            bundle = Bundleable.create(token);
        } catch (BundlerException e) {
            throw new IllegalArgumentException("Serialization failure", e);
        }

        mHostDispatcher.dispatch(
                CarContext.MEDIA_PLAYBACK_SERVICE,
                "registerMediaSessionToken", (IMediaPlaybackHost service) -> {
                    service.registerMediaSessionToken(bundle);
                    return null;
                }
        );
    }

    /**
     * Creates an instance of {@link MediaPlaybackManager}.
     */
    @RestrictTo(LIBRARY)
    public static @NonNull MediaPlaybackManager create(@NonNull CarContext carContext,
            @NonNull HostDispatcher hostDispatcher, @NonNull Lifecycle lifecycle) {
        requireNonNull(carContext);
        requireNonNull(hostDispatcher);
        requireNonNull(lifecycle);

        return new MediaPlaybackManager(carContext, hostDispatcher, lifecycle);
    }

    @RestrictTo(LIBRARY_GROUP) // Restrict to testing library
    @SuppressWarnings({"methodref.receiver.bound.invalid"})
    protected MediaPlaybackManager(@NonNull CarContext carContext,
            @NonNull HostDispatcher hostDispatcher, @NonNull Lifecycle lifecycle) {
        requireNonNull(carContext);
        mHostDispatcher = requireNonNull(hostDispatcher);
        LifecycleObserver observer = new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
                lifecycle.removeObserver(this);
            }
        };
        lifecycle.addObserver(observer);
    }
}
