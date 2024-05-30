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

package androidx.sqlite.driver.test

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.io.path.createTempFile
import kotlin.io.path.pathString

class BundledSQLiteDriverTest : BaseBundledConformanceTest() {

    override val driverType = TestDriverType.BUNDLED

    override fun getDatabaseFileName(): String {
        return createTempFile("test.db").also { it.toFile().deleteOnExit() }.pathString
    }

    override fun getDriver(): BundledSQLiteDriver {
        return BundledSQLiteDriver()
    }
}
