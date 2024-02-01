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

package androidx.compose.integration.hero.macrobenchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

const val PACKAGE_NAME = "androidx.compose.integration.hero.macrobenchmark.target"
const val ITERATIONS = 10

private const val COMPOSE_IDLE = "COMPOSE-IDLE"

fun UiDevice.waitForComposeIdle(timeout: Long = 3000) {
    this.wait(Until.findObject(By.desc(COMPOSE_IDLE)), timeout)
}
