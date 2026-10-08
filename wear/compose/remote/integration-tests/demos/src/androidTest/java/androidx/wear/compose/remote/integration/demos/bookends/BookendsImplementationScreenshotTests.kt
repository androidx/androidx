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

package androidx.wear.compose.remote.integration.demos.bookends

import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rs
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.integration.demos.bookends.material3.BookendsImplementation
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteAppScaffold
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteButton
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteScreenScaffold
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteText
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteTimeText
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteTransformingLazyColumn
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * [BookendsScreenshotTest] with the bookends components emitted as `Custom` components and rendered
 * by the host with Wear Compose Material3.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class WearMaterial3BookendsScreenshotTest :
    BookendsScreenshotTest(BookendsImplementation.WearMaterial3) {

    @Test
    fun timeText() {
        runScreenshotTest(centered = false) {
            RemoteTimeText(modifier = RemoteModifier.fillMaxSize(), time = "10:09".rs)
        }
    }

    /** A full screen with the components that only exist as Wear Compose Material3 bookends. */
    @Test
    fun screen_scaffoldWithTransformingLazyColumn() {
        runScreenshotTest(centered = false) {
            RemoteAppScaffold(timeText = { RemoteTimeText(time = "10:09".rs) }) {
                RemoteScreenScaffold {
                    RemoteTransformingLazyColumn {
                        item { RemoteText("Bookends".rs) }
                        items(4) { index ->
                            RemoteButton(onClick = noopAction) { RemoteText("Item $index".rs) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * [BookendsScreenshotTest] with the bookends components delegating to the equivalent
 * `androidx.wear.compose.remote.material3` components.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteMaterial3BookendsScreenshotTest :
    BookendsScreenshotTest(BookendsImplementation.RemoteMaterial3)
