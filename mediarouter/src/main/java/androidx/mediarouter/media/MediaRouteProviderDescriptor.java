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
package androidx.mediarouter.media;

import android.os.Bundle;

import androidx.annotation.NonNull;

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
    private static final String KEY_SUPPORTS_DYNAMIC_GROUP_ROUTE = "supportsDynamicGroupRoute";

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Bundle mBundle;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final List<MediaRouteDescriptor> mRoutes;
    final boolean mSupportsDynamicGroupRoute;

    MediaRouteProviderDescriptor(List<MediaRouteDescriptor> routes,
                                 boolean supportsDynamicGroupRoute) {
        mRoutes = (routes == null) ? Collections.<MediaRouteDescriptor>emptyList() : routes;
        mSupportsDynamicGroupRoute = supportsDynamicGroupRoute;
    }

    /**
     * Gets the list of all routes that this provider has published.
     * <p>
     * If it doesn't have any routes, it returns an empty list.
     * </p>
     */
    @NonNull
    public List<MediaRouteDescriptor> getRoutes() {
        return mRoutes;
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
        final int routeCount = getRoutes().size();
        for (int i = 0; i < routeCount; i++) {
            MediaRouteDescriptor route = mRoutes.get(i);
            if (route == null || !route.isValid()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates whether a {@link MediaRouteProvider} supports dynamic group route.
     *
     * @see androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController
     */
    public boolean supportsDynamicGroupRoute() {
        return mSupportsDynamicGroupRoute;
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
        if (mBundle != null) {
            return mBundle;
        }
        mBundle = new Bundle();
        if (mRoutes != null && !mRoutes.isEmpty()) {
            final int count = mRoutes.size();
            ArrayList<Bundle> routeBundles = new ArrayList<Bundle>(count);
            for (int i = 0; i < count; i++) {
                routeBundles.add(mRoutes.get(i).asBundle());
            }
            mBundle.putParcelableArrayList(KEY_ROUTES, routeBundles);
        }
        mBundle.putBoolean(KEY_SUPPORTS_DYNAMIC_GROUP_ROUTE, mSupportsDynamicGroupRoute);
        return mBundle;
    }

    /**
     * Creates an instance from a bundle.
     *
     * @param bundle The bundle, or null if none.
     * @return The new instance, or null if the bundle was null.
     */
    public static MediaRouteProviderDescriptor fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        List<MediaRouteDescriptor> routes = null;
        ArrayList<Bundle> routeBundles = bundle.<Bundle>getParcelableArrayList(KEY_ROUTES);
        if (routeBundles != null && !routeBundles.isEmpty()) {
            final int count = routeBundles.size();
            routes = new ArrayList<MediaRouteDescriptor>(count);
            for (int i = 0; i < count; i++) {
                routes.add(MediaRouteDescriptor.fromBundle(routeBundles.get(i)));
            }
        }
        boolean supportsDynamicGroupRoute =
                bundle.getBoolean(KEY_SUPPORTS_DYNAMIC_GROUP_ROUTE, false);
        return new MediaRouteProviderDescriptor(routes, supportsDynamicGroupRoute);
    }

    /**
     * Builder for {@link MediaRouteProviderDescriptor}.
     */
    public static final class Builder {
        private List<MediaRouteDescriptor> mRoutes;
        private boolean mSupportsDynamicGroupRoute = false;

        /**
         * Creates an empty media route provider descriptor builder.
         */
        public Builder() {
        }

        /**
         * Creates a media route provider descriptor builder whose initial contents are
         * copied from an existing descriptor.
         */
        public Builder(MediaRouteProviderDescriptor descriptor) {
            if (descriptor == null) {
                throw new IllegalArgumentException("descriptor must not be null");
            }
            mRoutes = descriptor.mRoutes;
            mSupportsDynamicGroupRoute = descriptor.mSupportsDynamicGroupRoute;
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
         * Sets the list of routes.
         */
        Builder setRoutes(Collection<MediaRouteDescriptor> routes) {
            if (routes == null || routes.isEmpty()) {
                mRoutes = null;
            } else {
                mRoutes = new ArrayList<>(routes);
            }
            return this;
        }

        /**
         * Sets if this provider supports dynamic group route.
         */
        public Builder setSupportsDynamicGroupRoute(boolean value) {
            mSupportsDynamicGroupRoute = value;
            return this;
        }


        /**
         * Builds the {@link MediaRouteProviderDescriptor}.
         */
        public MediaRouteProviderDescriptor build() {
            return new MediaRouteProviderDescriptor(mRoutes, mSupportsDynamicGroupRoute);
        }
    }
}
