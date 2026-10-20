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

package androidx.compose.remote.core.operations

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.testing.HeadlessRemoteContext
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PathDataTest {

    @Test
    fun apply_reinitializesPathDataAfterContextReset() {
        val pathFloats =
            floatArrayOf(
                PathData.MOVE_NAN,
                START_X,
                START_Y,
                PathData.LINE_NAN,
                END_X,
                END_Y,
                PathData.CLOSE_NAN,
            )
        val pathData = PathData(PATH_ID, pathFloats, WINDING_EVEN_ODD)
        val doc =
            CoreDocument().apply {
                operations.add(RootLayoutComponent(ROOT_COMPONENT_ID))
                operations.add(pathData)
            }
        val context =
            object : HeadlessRemoteContext() {
                override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
                    mRemoteComposeState.putPathData(instanceId, floatPath)
                    mRemoteComposeState.putPathWinding(instanceId, winding)
                }

                override fun getPathData(instanceId: Int): FloatArray? {
                    return mRemoteComposeState.getPathData(instanceId)
                }
            }

        doc.initializeContext(context)
        val firstPassPath = context.getPathData(PATH_ID)
        doc.initializeContext(context)
        val secondPassPath = context.getPathData(PATH_ID)

        assertThat(firstPassPath).isNotNull()
        assertThat(secondPassPath).isSameInstanceAs(firstPassPath)
        assertThat(secondPassPath).isEqualTo(pathFloats)
        assertThat(doc.remoteComposeState.getPathWinding(PATH_ID)).isEqualTo(WINDING_EVEN_ODD)
    }

    @Test
    fun apply_skipsReloadWhenPathUnchangedAndAlreadyInContext() {
        val pathFloats =
            floatArrayOf(
                PathData.MOVE_NAN,
                START_X,
                START_Y,
                PathData.LINE_NAN,
                END_X,
                END_Y,
                PathData.CLOSE_NAN,
            )
        val pathData = PathData(PATH_ID, pathFloats, WINDING_NON_ZERO)
        var loadCount = 0
        val context =
            object : HeadlessRemoteContext() {
                override fun loadPathData(instanceId: Int, winding: Int, floatPath: FloatArray) {
                    loadCount++
                    mRemoteComposeState.putPathData(instanceId, floatPath)
                }

                override fun getPathData(instanceId: Int): FloatArray? {
                    return mRemoteComposeState.getPathData(instanceId)
                }
            }

        pathData.apply(context)
        pathData.apply(context)

        assertThat(loadCount).isEqualTo(EXPECTED_SINGLE_LOAD_COUNT)
        assertThat(PathData.id()).isEqualTo(Operations.DATA_PATH)
    }

    companion object {
        private const val ROOT_COMPONENT_ID = 1
        private const val PATH_ID = 42
        private const val WINDING_NON_ZERO = 0
        private const val WINDING_EVEN_ODD = 1
        private const val START_X = 0f
        private const val START_Y = 0f
        private const val END_X = 10f
        private const val END_Y = 20f
        private const val EXPECTED_SINGLE_LOAD_COUNT = 1
    }
}
