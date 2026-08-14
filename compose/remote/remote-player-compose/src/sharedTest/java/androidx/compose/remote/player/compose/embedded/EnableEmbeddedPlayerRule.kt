/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded

import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that enables the embedded player
 * ([RemoteComposePlayerFlags.isEmbeddedPlayerEnabled]) for the duration of a test and restores the
 * previous value afterwards.
 */
@OptIn(ExperimentalRemotePlayerApi::class)
class EnableEmbeddedPlayerRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                val previous = RemoteComposePlayerFlags.isEmbeddedPlayerEnabled
                try {
                    RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
                    base.evaluate()
                } finally {
                    RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = previous
                }
            }
        }
}
