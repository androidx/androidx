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

package androidx.appsearch.builtintypes;

import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An AppSearch document representing an action. This action schema type is used for the nested
 * potentialActions field in entity schema types such as builtin:Thing or builtin:Timer.
 *
 * <ul>
 * <li><b>name</b> - This is a unique identifier for the action, such as
 * "actions.intent.CREATE_CALL". See <a
 * href=developer.android.com/reference/app-actions/built-in-intents/communications/create-call>
 * developer.android.com/reference/app-actions/built-in-intents/communications/create-call</a>
 * for an sample Action type.
 * </li>
 * <li><b>description</b> - A brief description of what the action does, such as "Create call" or
 * "Create Message".</li>
 * <li><b>uri</b> - A deeplink URI linking to an action. Invoking the action can be done by
 * creating an {@link android.content.Intent} object by calling
 * {@link android.content.Intent#parseUri} with the deeplink URI. Creating a deeplink URI, and
 * adding intent extras, can be done by building an intent and calling
 * {@link android.content.Intent#toUri}.
 * </li>
 * </ul>
 */
// TODO(b/274671459): Add additional information, if needed, to dispatch actions.
// TODO(b/328672505): Discuss with other API reviewers or council whether this API should be
//  stabilized, or whether the App Functions project replaces this effort entirely so this class
//  should be removed.
@ExperimentalAppSearchApi
@Document(name = "builtin:PotentialAction")
public class PotentialAction {
    @Document.Namespace
    final String mNamespace;

    @Document.Id
    final String mId;

    @Document.StringProperty
    private final String mName;

    @Document.StringProperty
    private final String mDescription;

    @Document.StringProperty
    private final String mUri;

    PotentialAction(@NonNull String namespace, @NonNull String id, @Nullable String name,
            @Nullable String description, @Nullable String uri) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mName = name;
        mDescription = description;
        mUri = uri;
    }

    /** Returns a string describing the action. */
    public @Nullable String getDescription() {
        return mDescription;
    }

    /**
     * Returns the BII action ID, which comes from Action IDs of Built-in intents listed at <a
     * href=developer.android.com/reference/app-actions/built-in-intents/bii-index>
     * developer.android.com/reference/app-actions/built-in-intents/bii-index</a>. For example,
     * the "Start Exercise" BII has an action id of "actions.intent.START_EXERCISE".
     */
    public @Nullable String getName() {
        return mName;
    }

    /**
     * Returns the deeplink URI.
     *
     * <p> A deeplink URI is a URI that lets a user access a specific content or feature within an
     * app directly. Users can create one by adding parameters to the app's base URI. To use a
     * deeplink URI in an Android application, users can create an {@link android.content.Intent}
     * object by calling {@link android.content.Intent#parseUri} with the deeplink URI. Creating a
     * deeplink URI, and adding intent extras, can be done by building an intent and calling
     * {@link android.content.Intent#toUri}.
     */
    public @Nullable String getUri() {
        return mUri;
    }

    /** Builder for {@link PotentialAction}. */
    public static final class Builder extends BuilderImpl<Builder> {
        /**
         * Constructor for {@link PotentialAction.Builder}.
         *
         * <p> As PotentialAction is used as a DocumentProperty of Thing, it does not need an id or
         * namespace.
         */
        public Builder() {}

        /**
         * Constructor with all the existing values.
         *
         * <p> As PotentialAction is used as a DocumentProperty of Thing, it does not need an id or
         * namespace.
         */
        public Builder(@NonNull PotentialAction potentialAction) {
            super(potentialAction);
        }
    }

    @SuppressWarnings("unchecked")
    static class BuilderImpl<T extends BuilderImpl<T>> {
        protected @Nullable String mName;
        protected @Nullable String mDescription;
        protected @Nullable String mUri;

        BuilderImpl() { }

        BuilderImpl(@NonNull PotentialAction potentialAction) {
            mName = potentialAction.getName();
            mDescription = potentialAction.getDescription();
            mUri = potentialAction.getUri();
        }

        /** Sets the name of the action. */
        public @NonNull T setName(@Nullable String name) {
            mName = name;
            return (T) this;
        }

        /** Sets the description of the action, such as "Call". */
        public @NonNull T setDescription(@Nullable String description) {
            mDescription = description;
            return (T) this;
        }

        /**
         * Sets the deeplink URI of the Action.
         *
         * <p> A deeplink URI is a URI that lets a user access a specific content or feature within
         * an app directly. Users can create one by adding parameters to the app's base URI. To use
         * a deeplink URI in an Android application, users can create an
         * {@link android.content.Intent} object by calling
         * {@link android.content.Intent#parseUri} with the deeplink URI. Creating a deeplink URI,
         * and adding intent extras, can be done by building an intent and calling
         * {@link android.content.Intent#toUri}.
         */
        public @NonNull T setUri(@Nullable String uri) {
            mUri = uri;
            return (T) this;
        }

        /** Builds the {@link PotentialAction}. */
        public @NonNull PotentialAction build() {
            // As PotentialAction is used as a DocumentProperty of Thing, it does not need an id or
            // namespace.
            return new PotentialAction(/*namespace=*/"", /*id=*/"", mName, mDescription, mUri);
        }
    }
}
