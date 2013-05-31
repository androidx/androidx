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
package android.support.v7.media;

import android.os.Bundle;

/**
 * Describes the kinds of routes that the media router would like to discover
 * and whether to perform active scanning.
 * <p>
 * This object is immutable once created.
 * </p>
 */
public final class MediaRouteDiscoveryRequest {
    private static final String KEY_SELECTOR = "selector";
    private static final String KEY_ACTIVE_SCAN = "activeScan";

    private final Bundle mBundle;
    private MediaRouteSelector mSelector;

    /**
     * Creates a media route discovery request.
     *
     * @param selector The route selector that specifies the kinds of routes to discover.
     * @param activeScan True if active scanning should be performed.
     */
    public MediaRouteDiscoveryRequest(MediaRouteSelector selector, boolean activeScan) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        mBundle = new Bundle();
        mSelector = selector;
        mBundle.putBundle(KEY_SELECTOR, selector.asBundle());
        mBundle.putBoolean(KEY_ACTIVE_SCAN, activeScan);
    }

    private MediaRouteDiscoveryRequest(Bundle bundle) {
        mBundle = bundle;
    }

    /**
     * Gets the route selector that specifies the kinds of routes to discover.
     */
    public MediaRouteSelector getSelector() {
        ensureSelector();
        return mSelector;
    }

    private void ensureSelector() {
        if (mSelector == null) {
            mSelector = MediaRouteSelector.fromBundle(mBundle.getBundle(KEY_SELECTOR));
            if (mSelector == null) {
                mSelector = MediaRouteSelector.EMPTY;
            }
        }
    }

    /**
     * Returns true if active scanning should be performed.
     *
     * @see MediaRouter#CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
     */
    public boolean isActiveScan() {
        return mBundle.getBoolean(KEY_ACTIVE_SCAN);
    }

    /**
     * Returns true if the discovery request has all of the required fields.
     */
    public boolean isValid() {
        ensureSelector();
        return mSelector.isValid();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MediaRouteDiscoveryRequest) {
            MediaRouteDiscoveryRequest other = (MediaRouteDiscoveryRequest)o;
            return getSelector().equals(other.getSelector())
                    && isActiveScan() == other.isActiveScan();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getSelector().hashCode() ^ (isActiveScan() ? 1 : 0);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("DiscoveryRequest{ selector=").append(getSelector());
        result.append(", activeScan=").append(isActiveScan());
        result.append(", isValid=").append(isValid());
        result.append(" }");
        return result.toString();
    }

    /**
     * Converts this object to a bundle for serialization.
     *
     * @return The contents of the object represented as a bundle.
     */
    public Bundle asBundle() {
        return mBundle;
    }

    /**
     * Creates an instance from a bundle.
     *
     * @param bundle The bundle, or null if none.
     * @return The new instance, or null if the bundle was null.
     */
    public static MediaRouteDiscoveryRequest fromBundle(Bundle bundle) {
        return bundle != null ? new MediaRouteDiscoveryRequest(bundle) : null;
    }
}
