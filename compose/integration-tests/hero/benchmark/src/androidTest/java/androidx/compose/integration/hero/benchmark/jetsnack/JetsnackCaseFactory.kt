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

package androidx.compose.integration.hero.benchmark.jetsnack

import androidx.compose.integration.hero.implementation.jetsnack.Feed
import androidx.compose.integration.hero.implementation.jetsnack.theme.JetsnackTheme
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase

class JetsnackCaseFactory : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        JetsnackTheme {
            Feed(onSnackClick = {})
        }
    }
}
