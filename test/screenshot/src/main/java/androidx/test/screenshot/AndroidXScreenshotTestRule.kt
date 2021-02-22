/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.test.screenshot

/**
 * Rule to be used in AndroidX project tests. Set's up the proper repository name and golden
 * directory.
 *
 * @param moduleDirectory Directory to be used for the module that contains the tests. This is
 * just a helper to avoid mixing goldens between different projects.
 * Example for module directory: "compose/material/material"
 *
 * @hide
 */
// TODO: Move this to internal module in case this module will be public one day.
class AndroidXScreenshotTestRule(
    moduleDirectory: String
) : ScreenshotTestRule(
    ScreenshotTestRuleConfig(
        "platform/frameworks/support-golden",
        moduleDirectory.trim('/')
    )
)
