/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.prepackage

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.Callable

/**
 * The pre-package configuration that provides Room with the input stream used to copy the
 * pre-packaged database into the database destined location.
 */
internal sealed class PrePackagedCopyConfig {
    public abstract fun getInputStream(): InputStream
}

/* Copy from asset path for pre-package database. */
internal class CopyFromAssetPath(private val context: Context, private val path: String) :
    PrePackagedCopyConfig() {
    override fun getInputStream(): InputStream {
        return context.assets.open(path)
    }
}

/* Copy from file for pre-package database. */
internal class CopyFromFile(private val file: File) : PrePackagedCopyConfig() {
    override fun getInputStream(): InputStream {
        return FileInputStream(file)
    }
}

/* Copy from input stream for pre-package database. */
internal class CopyFromInputStream(private val inputStreamFactory: Callable<InputStream>) :
    PrePackagedCopyConfig() {
    override fun getInputStream(): InputStream {
        return inputStreamFactory.call()
    }
}
