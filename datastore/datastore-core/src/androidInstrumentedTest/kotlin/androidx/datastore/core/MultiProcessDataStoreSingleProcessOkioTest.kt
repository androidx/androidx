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

package androidx.datastore.core

import androidx.datastore.OkioPath
import androidx.datastore.OkioTestIO
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class, FlowPreview::class)
@InternalCoroutinesApi
class MultiProcessDataStoreSingleProcessOkioTest :
    MultiProcessDataStoreSingleProcessTest<OkioPath>(OkioTestIO()) {
    override fun getJavaFile(file: OkioPath): File {
        return file.path.toFile()
    }
}
