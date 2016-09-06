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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Describes the capabilities of routes that applications would like to discover and use.
 * <p>
 * This object is immutable once created using a {@link Builder} instance.
 * </p>
 *
 * <h3>Example</h3>
 * <pre>
 * MediaRouteSelector selectorBuilder = new MediaRouteSelector.Builder()
 *         .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
 *         .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
 *         .build();
 *
 * MediaRouter router = MediaRouter.getInstance(context);
 * router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
 * </pre>
 */
public final class MediaRouteSelector {
    static final String KEY_CONTROL_CATEGORIES = "controlCategories";

    private final Bundle mBundle;
    List<String> mControlCategories;

    /**
     * An empty media route selector that will not match any routes.
     */
    public static final MediaRouteSelector EMPTY = new MediaRouteSelector(new Bundle(), null);

    MediaRouteSelector(Bundle bundle, List<String> controlCategories) {
        mBundle = bundle;
        mControlCategories = controlCategories;
    }

    /**
     * Gets the list of {@link MediaControlIntent media control categories} in the selector.
     *
     * @return The list of categories.
     */
    public List<String> getControlCategories() {
        ensureControlCategories();
        return mControlCategories;
    }

    void ensureControlCategories() {
        if (mControlCategories == null) {
            mControlCategories = mBundle.getStringArrayList(KEY_CONTROL_CATEGORIES);
            if (mControlCategories == null || mControlCategories.isEmpty()) {
                mControlCategories = Collections.<String>emptyList();
            }
        }
    }

    /**
     * Returns true if the selector contains the specified category.
     *
     * @param category The category to check.
     * @return True if the category is present.
     */
    public boolean hasControlCategory(String category) {
        if (category != null) {
            ensureControlCategories();
            final int categoryCount = mControlCategories.size();
            for (int i = 0; i < categoryCount; i++) {
                if (mControlCategories.get(i).equals(category)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the selector matches at least one of the specified control filters.
     *
     * @param filters The list of control filters to consider.
     * @return True if a match is found.
     */
    public boolean matchesControlFilters(List<IntentFilter> filters) {
        if (filters != null) {
            ensureControlCategories();
            final int categoryCount = mControlCategories.size();
            if (categoryCount != 0) {
                final int filterCount = filters.size();
                for (int i = 0; i < filterCount; i++) {
                    final IntentFilter filter = filters.get(i);
                    if (filter != null) {
                        for (int j = 0; j < categoryCount; j++) {
                            if (filter.hasCategory(mControlCategories.get(j))) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns true if this selector contains all of the capabilities described
     * by the specified selector.
     *
     * @param selector The selector to be examined.
     * @return True if this selector contains all of the capabilities described
     * by the specified selector.
     */
    public boolean contains(MediaRouteSelector selector) {
        if (selector != null) {
            ensureControlCategories();
            selector.ensureControlCategories();
            return mControlCategories.containsAll(selector.mControlCategories);
        }
        return false;
    }

    /**
     * Returns true if the selector does not specify any capabilities.
     */
    public boolean isEmpty() {
        ensureControlCategories();
        return mControlCategories.isEmpty();
    }

    /**
     * Returns true if the selector has all of the required fields.
     */
    public boolean isValid() {
        ensureControlCategories();
        if (mControlCategories.contains(null)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MediaRouteSelector) {
            MediaRouteSelector other = (MediaRouteSelector)o;
            ensureControlCategories();
            other.ensureControlCategories();
            return mControlCategories.equals(other.mControlCategories);
        }
        return false;
    }

    @Override
    public int hashCode() {
        ensureControlCategories();
        return mControlCategories.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MediaRouteSelector{ ");
        result.append("controlCategories=").append(
                Arrays.toString(getControlCategories().toArray()));
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
    public static MediaRouteSelector fromBundle(@Nullable Bundle bundle) {
        return bundle != null ? new MediaRouteSelector(bundle, null) : null;
    }

    /**
     * Builder for {@link MediaRouteSelector media route selectors}.
     */
    public static final class Builder {
        private ArrayList<String> mControlCategories;

        /**
         * Creates an empty media route selector builder.
         */
        public Builder() {
        }

        /**
         * Creates a media route selector descriptor builder whose initial contents are
         * copied from an existing selector.
         */
        public Builder(@NonNull MediaRouteSelector selector) {
            if (selector == null) {
                throw new IllegalArgumentException("selector must not be null");
            }

            selector.ensureControlCategories();
            if (!selector.mControlCategories.isEmpty()) {
                mControlCategories = new ArrayList<String>(selector.mControlCategories);
            }
        }

        /**
         * Adds a {@link MediaControlIntent media control category} to the builder.
         *
         * @param category The category to add to the set of desired capabilities, such as
         * {@link MediaControlIntent#CATEGORY_LIVE_AUDIO}.
         * @return The builder instance for chaining.
         */
        @NonNull
        public Builder addControlCategory(@NonNull String category) {
            if (category == null) {
                throw new IllegalArgumentException("category must not be null");
            }

            if (mControlCategories == null) {
                mControlCategories = new ArrayList<String>();
            }
            if (!mControlCategories.contains(category)) {
                mControlCategories.add(category);
            }
            return this;
        }

        /**
         * Adds a list of {@link MediaControlIntent media control categories} to the builder.
         *
         * @param categories The list categories to add to the set of desired capabilities,
         * such as {@link MediaControlIntent#CATEGORY_LIVE_AUDIO}.
         * @return The builder instance for chaining.
         */
        @NonNull
        public Builder addControlCategories(@NonNull Collection<String> categories) {
            if (categories == null) {
                throw new IllegalArgumentException("categories must not be null");
            }

            if (!categories.isEmpty()) {
                for (String category : categories) {
                    addControlCategory(category);
                }
            }
            return this;
        }

        /**
         * Adds the contents of an existing media route selector to the builder.
         *
         * @param selector The media route selector whose contents are to be added.
         * @return The builder instance for chaining.
         */
        @NonNull
        public Builder addSelector(@NonNull MediaRouteSelector selector) {
            if (selector == null) {
                throw new IllegalArgumentException("selector must not be null");
            }

            addControlCategories(selector.getControlCategories());
            return this;
        }

        /**
         * Builds the {@link MediaRouteSelector media route selector}.
         */
        @NonNull
        public MediaRouteSelector build() {
            if (mControlCategories == null) {
                return EMPTY;
            }
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(KEY_CONTROL_CATEGORIES, mControlCategories);
            return new MediaRouteSelector(bundle, mControlCategories);
        }
    }
}