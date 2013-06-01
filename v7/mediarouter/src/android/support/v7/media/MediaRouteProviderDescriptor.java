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

import android.content.IntentFilter;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Describes the state of a media route provider and the routes that it publishes.
 * <p>
 * This object is immutable once created using a {@link Builder} instance.
 * </p>
 */
public final class MediaRouteProviderDescriptor {
    private static final String KEY_ROUTES = "routes";

    private final Bundle mBundle;
    private List<MediaRouteDescriptor> mRoutes;

    private MediaRouteProviderDescriptor(Bundle bundle,
            List<MediaRouteDescriptor> routes) {
        mBundle = bundle;
        mRoutes = routes;
    }

    /**
     * Gets the list of all routes that this provider has published.
     */
    public List<MediaRouteDescriptor> getRoutes() {
        ensureRoutes();
        return mRoutes;
    }

    private void ensureRoutes() {
        if (mRoutes == null) {
            ArrayList<Bundle> routeBundles = mBundle.<Bundle>getParcelableArrayList(KEY_ROUTES);
            if (routeBundles == null || routeBundles.isEmpty()) {
                mRoutes = Collections.<MediaRouteDescriptor>emptyList();
            } else {
                final int count = routeBundles.size();
                mRoutes = new ArrayList<MediaRouteDescriptor>(count);
                for (int i = 0; i < count; i++) {
                    mRoutes.add(MediaRouteDescriptor.fromBundle(routeBundles.get(i)));
                }
            }
        }
    }

    /**
     * Returns true if the route provider descriptor and all of the routes that
     * it contains have all of the required fields.
     * <p>
     * This verification is deep.  If the provider descriptor is known to be
     * valid then it is not necessary to call {@link #isValid} on each of its routes.
     * </p>
     */
    public boolean isValid() {
        ensureRoutes();
        final int routeCount = mRoutes.size();
        for (int i = 0; i < routeCount; i++) {
            MediaRouteDescriptor route = mRoutes.get(i);
            if (route == null || !route.isValid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MediaRouteProviderDescriptor{ ");
        result.append("routes=").append(
                Arrays.toString(getRoutes().toArray()));
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
    public static MediaRouteProviderDescriptor fromBundle(Bundle bundle) {
        return bundle != null ? new MediaRouteProviderDescriptor(bundle, null) : null;
    }

    /**
     * Builder for {@link MediaRouteProviderDescriptor media route provider descriptors}.
     */
    public static final class Builder {
        private final Bundle mBundle;
        private ArrayList<MediaRouteDescriptor> mRoutes;

        /**
         * Creates an empty media route provider descriptor builder.
         */
        public Builder() {
            mBundle = new Bundle();
        }

        /**
         * Creates a media route provider descriptor builder whose initial contents are
         * copied from an existing descriptor.
         */
        public Builder(MediaRouteProviderDescriptor descriptor) {
            if (descriptor == null) {
                throw new IllegalArgumentException("descriptor must not be null");
            }

            mBundle = new Bundle(descriptor.mBundle);

            descriptor.ensureRoutes();
            if (!descriptor.mRoutes.isEmpty()) {
                mRoutes = new ArrayList<MediaRouteDescriptor>(descriptor.mRoutes);
            }
        }

        /**
         * Adds a route.
         */
        public Builder addRoute(MediaRouteDescriptor route) {
            if (route == null) {
                throw new IllegalArgumentException("route must not be null");
            }

            if (mRoutes == null) {
                mRoutes = new ArrayList<MediaRouteDescriptor>();
            } else if (mRoutes.contains(route)) {
                throw new IllegalArgumentException("route descriptor already added");
            }
            mRoutes.add(route);
            return this;
        }

        /**
         * Adds a list of routes.
         */
        public Builder addRoutes(Collection<MediaRouteDescriptor> routes) {
            if (routes == null) {
                throw new IllegalArgumentException("routes must not be null");
            }

            if (!routes.isEmpty()) {
                for (MediaRouteDescriptor route : routes) {
                    addRoute(route);
                }
            }
            return this;
        }

        /**
         * Builds the {@link MediaRouteProviderDescriptor media route provider descriptor}.
         */
        public MediaRouteProviderDescriptor build() {
            if (mRoutes != null) {
                final int count = mRoutes.size();
                ArrayList<Bundle> routeBundles = new ArrayList<Bundle>(count);
                for (int i = 0; i < count; i++) {
                    routeBundles.add(mRoutes.get(i).asBundle());
                }
                mBundle.putParcelableArrayList(KEY_ROUTES, routeBundles);
            }
            return new MediaRouteProviderDescriptor(mBundle, mRoutes);
        }
    }
}