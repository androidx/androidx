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

package androidx.room.gradle

import javax.inject.Inject
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

open class RoomExtension @Inject constructor(private val providers: ProviderFactory) {
    internal var schemaDirectory: Provider<String>? = null

    // TODO(b/279748243): Consider adding overload that takes `org.gradle.api.file.Director`.

    /**
     * Sets the schema location where Room will output exported schema files.
     *
     * The location specified will be used as the base directory for schema files that will be
     * generated per build variant. i.e. for a 'debug' build of the product flavor 'free' then a
     * schema will be generated in
     * `<schemaDirectory>/freeDebug/<database-package>/<database-version>.json`.
     *
     * See [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(path: String) {
        schemaDirectory(providers.provider { path })
    }

    /**
     * Sets the schema location where Room will output exported schema files.
     *
     * The location specified will be used as the base directory for schema files that will be
     * generated per build variant. i.e. for a 'debug' build of the product flavor 'free' then a
     * schema will be generated in
     * `<schemaDirectory>/freeDebug/<database-package>/<database-version>.json`.
     *
     * See [Export Schemas Documentation](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
     */
    open fun schemaDirectory(path: Provider<String>) {
        schemaDirectory = path
    }
}