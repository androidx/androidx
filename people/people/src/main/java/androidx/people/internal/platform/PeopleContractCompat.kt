/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people.internal.platform

import android.net.Uri

// Mirrors android.provider.PeopleContract
internal object PeopleContractCompat {

    // Mirrors android.provider.PeopleContract.AUTHORITY
    public const val AUTHORITY: String = "com.android.people"

    // Mirrors android.provider.PeopleContract.AUTHORITY_URI
    @JvmField public val AUTHORITY_URI: Uri = Uri.parse("content://$AUTHORITY")

    // Mirrors android.provider.PeopleContract.CONTENT_NOTIFICATION_URI
    @JvmField
    public val CONTENT_NOTIFICATION_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "notification")

    // Mirrors android.provider.PeopleContract.EXTRA_NOTIFICATION_URI
    public const val EXTRA_NOTIFICATION_URI: String = "android.provider.extra.NOTIFICATION_URI"

    // TODO(b/562550480): Mirror android.provider.PeopleContract.EXTRA_UNHANDLED_QUERIES once the
    //  platform adds it in a future release. Until then the platform People Provider never sets it.
    /**
     * `Cursor` extra carrying the criteria a People Directory application did not evaluate, as a
     * [android.os.Bundle] with the same structure as a query arguments bundle
     * ([QUERY_ARG_QUERIES]). Set both by People Directory applications and by the People Provider
     * for its clients.
     */
    public const val EXTRA_UNHANDLED_QUERIES: String = "android.provider.extra.UNHANDLED_QUERIES"

    // Mirrors android.provider.PeopleContract.QUERY_ARG_QUERIES
    public const val QUERY_ARG_QUERIES: String = "android:query-arg-queries"

    // Mirrors android.provider.PeopleContract.QUERY_ARG_PACKAGE
    public const val QUERY_ARG_PACKAGE: String = "android:query-arg-package"

    // Mirrors android.provider.PeopleContract.QUERY_ARG_SELECTION_TYPE
    public const val QUERY_ARG_SELECTION_TYPE: String = "android:query-arg-selection-type"

    // Mirrors android.provider.PeopleContract.QUERY_ARG_SELECTION_VALUE
    public const val QUERY_ARG_SELECTION_VALUE: String = "android:query-arg-selection-value"

    // Mirrors android.provider.PeopleContract.QUERY_ARG_DEPENDENCY_URI
    public const val QUERY_ARG_DEPENDENCY_URI: String = "android:query-arg-dependency-uri"

    // Mirrors android.provider.PeopleContract.Directory
    public object Directory {

        // Mirrors android.provider.PeopleContract.Directory.CONTENT_URI
        @JvmField public val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "directories")

        // Mirrors android.provider.PeopleContract.Directory.CONTENT_ITEM_TYPE
        public const val CONTENT_ITEM_TYPE: String = "vnd.android.cursor.item/people_directory"

        // Mirrors android.provider.PeopleContract.Directory.CONTENT_TYPE
        public const val CONTENT_TYPE: String = "vnd.android.cursor.dir/people_directories"

        // Mirrors android.provider.PeopleContract.Directory.DIRECTORY_AUTHORITY
        public const val DIRECTORY_AUTHORITY: String = "authority"

        // Mirrors android.provider.PeopleContract.Directory.PACKAGE_NAME
        public const val PACKAGE_NAME: String = "package_name"

        // Mirrors android.provider.PeopleContract.Directory.DIRECTORY_PROPERTY
        public const val DIRECTORY_PROPERTY: String = "android.content.PeopleDirectory"
    }

    // Mirrors android.provider.PeopleContract.Persons
    public object Persons {

        // Mirrors android.provider.PeopleContract.Persons.CONTENT_ITEM_TYPE
        public const val CONTENT_ITEM_TYPE: String = "vnd.android.cursor.item/people_person"

        // Mirrors android.provider.PeopleContract.Persons.CONTENT_TYPE
        public const val CONTENT_TYPE: String = "vnd.android.cursor.dir/people_persons"

        // Mirrors android.provider.PeopleContract.Persons.CONTENT_URI
        @JvmField public val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "persons")

        // Mirrors android.provider.PeopleContract.Persons.CONTENT_LOOKUP_URI
        @JvmField public val CONTENT_LOOKUP_URI: Uri = Uri.withAppendedPath(CONTENT_URI, "lookup")

        // Mirrors android.provider.PeopleContract.Persons.DISPLAY_NAME
        public const val DISPLAY_NAME: String = "display_name"

        // Mirrors android.provider.PeopleContract.Persons.PHOTO_URI
        public const val PHOTO_URI: String = "photo_uri"

        // Mirrors android.provider.PeopleContract.Persons.LOOKUP_KEY
        public const val LOOKUP_KEY: String = "lookup_key"

        // Mirrors android.provider.PeopleContract.Persons.CAPABILITY
        public const val CAPABILITY: String = "capability"

        // Mirrors android.provider.PeopleContract.Persons.IS_KNOWN
        public const val IS_KNOWN: String = "is_known"

        // Mirrors android.provider.PeopleContract.Persons.Query
        public object Query {

            // Mirrors android.provider.PeopleContract.Persons.Query.SELECTION_TYPE_PHONE
            public const val SELECTION_TYPE_PHONE: String = "selection-type-phone"

            // Mirrors android.provider.PeopleContract.Persons.Query.SELECTION_TYPE_EMAIL
            public const val SELECTION_TYPE_EMAIL: String = "selection-type-email"

            // Mirrors android.provider.PeopleContract.Persons.Query.SELECTION_TYPE_GROUP_LOOKUP_KEY
            public const val SELECTION_TYPE_GROUP_LOOKUP_KEY: String =
                "selection-type-group-lookup-key"

            // Mirrors android.provider.PeopleContract.Persons.Query.SELECTION_TYPE_LOOKUP_KEY
            public const val SELECTION_TYPE_LOOKUP_KEY: String = "selection-type-lookup-key"
        }
    }

    // Mirrors android.provider.PeopleContract.Groups
    public object Groups {

        // Mirrors android.provider.PeopleContract.Groups.CONTENT_ITEM_TYPE
        public const val CONTENT_ITEM_TYPE: String = "vnd.android.cursor.item/people_group"

        // Mirrors android.provider.PeopleContract.Groups.CONTENT_TYPE
        public const val CONTENT_TYPE: String = "vnd.android.cursor.dir/people_groups"

        // Mirrors android.provider.PeopleContract.Groups.CONTENT_URI
        @JvmField public val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "groups")

        // Mirrors android.provider.PeopleContract.Groups.CONTENT_LOOKUP_URI
        @JvmField public val CONTENT_LOOKUP_URI: Uri = Uri.withAppendedPath(CONTENT_URI, "lookup")

        // Mirrors android.provider.PeopleContract.Groups.DISPLAY_NAME
        public const val DISPLAY_NAME: String = "display_name"

        // Mirrors android.provider.PeopleContract.Groups.LOOKUP_KEY
        public const val LOOKUP_KEY: String = "lookup_key"

        // Mirrors android.provider.PeopleContract.Groups.Query
        public object Query {

            // Mirrors android.provider.PeopleContract.Groups.Query.SELECTION_TYPE_LOOKUP_KEY
            public const val SELECTION_TYPE_LOOKUP_KEY: String = "selection-type-lookup-key"
        }
    }

    // Mirrors android.provider.PeopleContract.Capability
    public object Capability {

        // Mirrors android.provider.PeopleContract.Capability.VOICE_REACHABILITY
        public const val VOICE_REACHABILITY: Int = 1 shl 0

        // Mirrors android.provider.PeopleContract.Capability.VIDEO_REACHABILITY
        public const val VIDEO_REACHABILITY: Int = 1 shl 1

        // Mirrors android.provider.PeopleContract.Capability.TEXT_REACHABILITY
        public const val TEXT_REACHABILITY: Int = 1 shl 2
    }

    // Mirrors android.provider.PeopleContract.Intents
    public object Intents {

        // Mirrors android.provider.PeopleContract.Intents.ACTION_START_PEOPLE_INTERACTION
        public const val ACTION_START_PEOPLE_INTERACTION: String =
            "android.provider.action.START_PEOPLE_INTERACTION"

        // Mirrors android.provider.PeopleContract.Intents.EXTRA_PEOPLE_INTERACTION_TYPE
        public const val EXTRA_PEOPLE_INTERACTION_TYPE: String =
            "android.provider.extra.PEOPLE_INTERACTION_TYPE"

        // Mirrors android.provider.PeopleContract.Intents.EXTRA_PEOPLE_QUERY_ARGS
        public const val EXTRA_PEOPLE_QUERY_ARGS: String =
            "android.provider.extra.PEOPLE_QUERY_ARGS"

        // Mirrors android.provider.PeopleContract.Intents.METHOD_CREATE_INTERACTION_INTENT
        public const val METHOD_CREATE_INTERACTION_INTENT: String = "create_interaction_intent"

        // Mirrors android.provider.PeopleContract.Intents.EXTRA_PEOPLE_INTERACTION_PENDING_INTENT
        public const val EXTRA_PEOPLE_INTERACTION_PENDING_INTENT: String =
            "android.provider.extra.PEOPLE_INTERACTION_PENDING_INTENT"

        // Mirrors android.provider.PeopleContract.Intents.PEOPLE_INTERACTION_TYPE_MESSAGE
        public const val PEOPLE_INTERACTION_TYPE_MESSAGE: String =
            "com.android.people.PEOPLE_INTERACTION_TYPE_MESSAGE"

        // Mirrors android.provider.PeopleContract.Intents.PEOPLE_INTERACTION_TYPE_VOICE
        public const val PEOPLE_INTERACTION_TYPE_VOICE: String =
            "com.android.people.PEOPLE_INTERACTION_TYPE_VOICE"

        // Mirrors android.provider.PeopleContract.Intents.PEOPLE_INTERACTION_TYPE_VIDEO
        public const val PEOPLE_INTERACTION_TYPE_VIDEO: String =
            "com.android.people.PEOPLE_INTERACTION_TYPE_VIDEO"
    }
}
