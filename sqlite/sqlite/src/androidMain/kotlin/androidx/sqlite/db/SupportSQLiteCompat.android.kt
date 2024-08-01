/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.sqlite.db

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.io.File

/** Helper for accessing features in [SupportSQLiteOpenHelper]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SupportSQLiteCompat private constructor() {
    /** Helper for accessing functions that require SDK version 21 and higher. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(21)
    public object Api21Impl {
        /**
         * Returns the absolute path to the directory on the filesystem.
         *
         * @return The path of the directory holding application files that will not be
         *   automatically backed up to remote storage.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun getNoBackupFilesDir(context: Context): File {
            return context.noBackupFilesDir
        }
    }

    /** Helper for accessing functions that require SDK version 23 and higher. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(23)
    public object Api23Impl {
        /**
         * Sets a [Bundle] that will be returned by [Cursor.getExtras].
         *
         * @param extras [Bundle] to set, or null to set an empty bundle.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun setExtras(cursor: Cursor, extras: Bundle) {
            cursor.extras = extras
        }
    }

    /** Helper for accessing functions that require SDK version 29 and higher. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(29)
    public object Api29Impl {
        /**
         * Similar to [Cursor.setNotificationUri], except this version allows to watch multiple
         * content URIs for changes.
         *
         * @param cr The content resolver from the caller's context. The listener attached to this
         *   resolver will be notified.
         * @param uris The content URIs to watch.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun setNotificationUris(cursor: Cursor, cr: ContentResolver, uris: List<Uri?>) {
            cursor.setNotificationUris(cr, uris)
        }

        /**
         * Return the URIs at which notifications of changes in this Cursor's data will be
         * delivered, as previously set by [setNotificationUris].
         *
         * @return Returns URIs that can be used with [ContentResolver.registerContentObserver] to
         *   find out about changes to this Cursor's data. May be null if no notification URI has
         *   been set.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun getNotificationUris(cursor: Cursor): List<Uri> {
            return cursor.notificationUris!!
        }
    }
}
