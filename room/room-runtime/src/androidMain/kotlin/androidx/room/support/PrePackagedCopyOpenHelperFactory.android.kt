/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.room.support

import androidx.sqlite.db.SupportSQLiteOpenHelper
import java.io.File
import java.io.InputStream
import java.util.concurrent.Callable

/** Implementation of [SupportSQLiteOpenHelper.Factory] that creates [PrePackagedCopyOpenHelper]. */
internal class PrePackagedCopyOpenHelperFactory(
    private val copyFromAssetPath: String?,
    private val copyFromFile: File?,
    private val copyFromInputStream: Callable<InputStream>?,
    private val delegate: SupportSQLiteOpenHelper.Factory
) : SupportSQLiteOpenHelper.Factory {
    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration
    ): SupportSQLiteOpenHelper {
        return PrePackagedCopyOpenHelper(
            configuration.context,
            copyFromAssetPath,
            copyFromFile,
            copyFromInputStream,
            configuration.callback.version,
            delegate.create(configuration)
        )
    }
}
