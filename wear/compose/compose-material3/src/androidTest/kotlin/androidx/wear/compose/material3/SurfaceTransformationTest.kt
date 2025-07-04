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

package androidx.wear.compose.material3

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SurfaceTransformationSpecTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun surface_transformation_spec_defaults() {
        var transformation: SurfaceTransformation? = null
        rule.setContent {
            val spec = rememberTransformationSpec()
            TransformingLazyColumn {
                item {
                    Button(
                        onClick = {},
                        transformation = SurfaceTransformation(spec).also { transformation = it },
                    ) {
                        Text("bugaga")
                    }
                }
            }
        }

        assertThat(transformation).isNotNull()
        assertThat(transformation).isNotEqualTo(NoOpSurfaceTransformation)
    }

    @Test
    fun surface_transformation_spec_reduced_motion() {
        var transformation: SurfaceTransformation? = null
        rule.setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                val spec = rememberTransformationSpec()
                TransformingLazyColumn {
                    item {
                        Button(
                            onClick = {},
                            transformation =
                                SurfaceTransformation(spec).also { transformation = it },
                        ) {
                            Text("bugaga")
                        }
                    }
                }
            }
        }

        assertThat(transformation).isEqualTo(NoOpSurfaceTransformation)
    }
}
