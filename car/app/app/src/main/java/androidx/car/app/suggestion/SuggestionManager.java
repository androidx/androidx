/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.suggestion;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.car.app.utils.ThreadUtils.checkMainThread;

import static java.util.Objects.requireNonNull;

import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.HostException;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.managers.Manager;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.suggestion.model.Suggestion;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Manager for communicating Suggestions related events with the host.
 *
 * <p>Apps must use this interface to coordinate with the car system for suggestions
 * resources.
 */
@RequiresCarApi(5)
public class SuggestionManager implements Manager {
    private final HostDispatcher mHostDispatcher;

    /**
     * <p>Sends the list of suggestions that an application wants to expose, in order for a user
     * to take actions on the primary infotainment display  usually in the center column of
     * the vehicle.
     *
     * @param suggestions to be sent to the host
     * @throws HostException            if the call is invoked by an app that is not declared as
     *                                  a navigation app in the manifest
     * @throws IllegalArgumentException if any of the suggestions is not well formed
     */
    @MainThread
    public void updateSuggestions(@NonNull List<Suggestion> suggestions) {
        checkMainThread();

        Bundleable bundle;
        try {
            bundle = Bundleable.create(suggestions);
        } catch (BundlerException e) {
            throw new IllegalArgumentException("Serialization failure", e);
        }

        mHostDispatcher.dispatch(
                CarContext.SUGGESTION_SERVICE,
                "updateSuggestions", (ISuggestionHost service) -> {
                    service.updateSuggestions(bundle);
                    return null;
                }
        );
    }

    /**
     * Creates an instance of {@link SuggestionManager}.
     *
     */
    @RestrictTo(LIBRARY)
    public static @NonNull SuggestionManager create(@NonNull CarContext carContext,
            @NonNull HostDispatcher hostDispatcher, @NonNull Lifecycle lifecycle) {
        requireNonNull(carContext);
        requireNonNull(hostDispatcher);
        requireNonNull(lifecycle);

        return new SuggestionManager(carContext, hostDispatcher, lifecycle);
    }

    @RestrictTo(LIBRARY_GROUP) // Restrict to testing library
    @SuppressWarnings({"methodref.receiver.bound.invalid"})
    protected SuggestionManager(@NonNull CarContext carContext,
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
