/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.HashSet;
import java.util.Set;

/**
 * A container for {@link Uri} {@link Trigger}s that caused a worker's {@link Constraints} to be
 * met.
 * <p>
 * When enqueuing work, you can add Uris or content authorities that should trigger the worker upon
 * update (see {@link Constraints.Builder#addContentUriTrigger(Uri, boolean)}).  This class is an
 * encapsulation of those triggers.
 * <p>
 * This class and its behavior is intrinsically tied to {@code JobScheduler}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ContentUriTriggers {

    private final Set<Trigger> mTriggers = new HashSet<>();

    /**
     * Adds a content {@link Uri} for system observation
     *
     * @param uri {@link Uri} to observe
     * @param triggerForDescendants {@code true} if any changes in descendants cause this
     *                              {@link WorkRequest} to run
     */
    public void add(@NonNull Uri uri, boolean triggerForDescendants) {
        Trigger trigger = new Trigger(uri, triggerForDescendants);
        mTriggers.add(trigger);
    }

    public @NonNull Set<Trigger> getTriggers() {
        return mTriggers;
    }

    /**
     * @return number of {@link Trigger} objects
     */
    public int size() {
        return mTriggers.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContentUriTriggers that = (ContentUriTriggers) o;

        return mTriggers.equals(that.mTriggers);
    }

    @Override
    public int hashCode() {
        return mTriggers.hashCode();
    }

    /**
     * Defines a content {@link Uri} trigger for a {@link WorkRequest}.
     */
    public static final class Trigger {
        private final @NonNull Uri mUri;
        private final boolean mTriggerForDescendants;

        Trigger(@NonNull Uri uri, boolean triggerForDescendants) {
            mUri = uri;
            mTriggerForDescendants = triggerForDescendants;
        }

        /**
         * @return The {@link Uri} associated with this trigger
         */
        public @NonNull Uri getUri() {
            return mUri;
        }

        /**
         * @return {@code true} if trigger also applies to descendants of the {@link Uri}
         */
        public boolean shouldTriggerForDescendants() {
            return mTriggerForDescendants;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Trigger trigger = (Trigger) o;

            return mTriggerForDescendants == trigger.mTriggerForDescendants
                    && mUri.equals(trigger.mUri);
        }

        @Override
        public int hashCode() {
            int result = mUri.hashCode();
            result = 31 * result + (mTriggerForDescendants ? 1 : 0);
            return result;
        }
    }
}
