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

package androidx.compose.remote.core

import androidx.compose.remote.core.operations.layout.ClickModifierOperation
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ClickReentrancyTest {

    @Test
    fun performClick_recursiveLoop_throwsException() {
        val doc = CoreDocument()
        val root = RootLayoutComponent(10, 0f, 0f, 100f, 100f, null, -1)
        doc.mRootLayoutComponent = root

        // Add ClickModifierOperation containing HostActionOperation targeting the same component
        val clickModifier = ClickModifierOperation()
        clickModifier.mList.add(HostActionOperation(10))
        root.mList.add(clickModifier)

        val context = mock<RemoteContext>()
        whenever(context.runAction(any(), any())).thenAnswer { invocation ->
            val id = invocation.getArgument<Int>(0)
            val metadata = invocation.getArgument<String>(1)
            doc.performClick(context, id, metadata)
        }

        val exception =
            assertThrows(RuntimeException::class.java) { doc.performClick(context, 10, "") }

        assertTrue(exception.message!!.contains("Maximum click re-entrancy depth exceeded"))
    }
}
