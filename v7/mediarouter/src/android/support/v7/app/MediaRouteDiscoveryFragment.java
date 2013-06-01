/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouteSelector;

/**
 * Media route discovery fragment.
 * <p>
 * This fragment takes care of registering a callback for media route discovery
 * during the activity's {@link android.app.Activity#onStart onStart()} phase
 * and removing it during the {@link android.app.Activity#onStart onStop()} phase.
 * </p><p>
 * The application must supply a route selector to specify the kinds of routes
 * to discover.  The application may also override {@link #onCreateCallback} to
 * provide the {@link MediaRouter} callback to register.
 * </p>
 */
public class MediaRouteDiscoveryFragment extends Fragment {
    private final String ARGUMENT_SELECTOR = "selector";

    private MediaRouter mRouter;
    private MediaRouteSelector mSelector;
    private MediaRouter.Callback mCallback;

    public MediaRouteDiscoveryFragment() {
    }

    /**
     * Gets the media router instance.
     */
    public MediaRouter getMediaRouter() {
        ensureRouter();
        return mRouter;
    }

    private void ensureRouter() {
        if (mRouter == null) {
            mRouter = MediaRouter.getInstance(getActivity());
        }
    }

    /**
     * Gets the media route selector for filtering the routes to be discovered.
     *
     * @return The selector, never null.
     */
    public MediaRouteSelector getRouteSelector() {
        ensureRouteSelector();
        return mSelector;
    }

    /**
     * Sets the media route selector for filtering the routes to be discovered.
     * This method must be called before the fragment is added.
     *
     * @param selector The selector to set.
     */
    public void setRouteSelector(MediaRouteSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        ensureRouteSelector();
        if (!mSelector.equals(selector)) {
            mSelector = selector;

            Bundle args = getArguments();
            if (args == null) {
                args = new Bundle();
            }
            args.putBundle(ARGUMENT_SELECTOR, selector.asBundle());
            setArguments(args);

            if (mCallback != null) {
                mRouter.removeCallback(mCallback);
                mRouter.addCallback(mSelector, mCallback,
                        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
            }
        }
    }

    private void ensureRouteSelector() {
        if (mSelector == null) {
            Bundle args = getArguments();
            if (args != null) {
                mSelector = MediaRouteSelector.fromBundle(args.getBundle(ARGUMENT_SELECTOR));
            }
            if (mSelector == null) {
                mSelector = MediaRouteSelector.EMPTY;
            }
        }
    }

    /**
     * Called to create the {@link android.support.v7.media.MediaRouter.Callback callback}
     * that will be registered.
     * <p>
     * The default callback does nothing.  The application may override this method to
     * supply its own callback.
     * </p>
     *
     * @return The new callback, or null if no callback should be registered.
     */
    public MediaRouter.Callback onCreateCallback() {
        return new MediaRouter.Callback() { };
    }

    /**
     * Called to prepare the callback flags that will be used when the
     * {@link android.support.v7.media.MediaRouter.Callback callback} is registered.
     * <p>
     * The default implementation returns {@link MediaRouter#CALLBACK_FLAG_REQUEST_DISCOVERY}.
     * </p>
     *
     * @return The desired callback flags.
     */
    public int onPrepareCallbackFlags() {
        return MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY;
    }

    @Override
    public void onStart() {
        super.onStart();

        ensureRouteSelector();
        ensureRouter();
        mCallback = onCreateCallback();
        if (mCallback != null) {
            mRouter.addCallback(mSelector, mCallback, onPrepareCallbackFlags());
        }
    }

    @Override
    public void onStop() {
        if (mCallback != null) {
            mRouter.removeCallback(mCallback);
            mCallback = null;
        }

        super.onStop();
    }
}
