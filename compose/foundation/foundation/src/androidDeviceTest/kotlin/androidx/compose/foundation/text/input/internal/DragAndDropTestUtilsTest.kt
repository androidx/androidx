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

package androidx.compose.foundation.text.input.internal

import android.net.Uri
import android.view.DragEvent
import androidx.compose.foundation.content.createClipData
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class DragAndDropTestUtilsTest {

    @Test
    fun makeImageDragEvent_withContentUri_returnsEventWithClipDataContainingUri() {
        val event =
            DragAndDropTestUtils.makeImageDragEvent(
                DragEvent.ACTION_DROP,
                Uri.parse("content://com.example/content.png"),
            )
        assertWithMessage("dragEvent.action").that(event.action).isEqualTo(DragEvent.ACTION_DROP)
        val clipData = event.clipData
        assertWithMessage("dragEvent.clipData").that(clipData).isNotNull()
        assertWithMessage("dragEvent.clipData.itemCount").that(clipData.itemCount).isEqualTo(1)
        val item = clipData.getItemAt(0)
        assertWithMessage("dragEvent.clipData.getItemAt(0).uri")
            .that(item.uri)
            .isEqualTo(Uri.parse("content://com.example/content.png"))
    }

    @Test
    fun makeDragEvent_returnsEventWithInputParams() {
        val expectedClipData = createClipData { addText("test") }
        val dragEvent =
            DragAndDropTestUtils.makeDragEvent(
                action = DragEvent.ACTION_DRAG_STARTED,
                position = Offset(100f, 400f),
                clipData = expectedClipData,
            )
        assertWithMessage("dragEvent.action")
            .that(dragEvent.action)
            .isEqualTo(DragEvent.ACTION_DRAG_STARTED)
        assertWithMessage("dragEvent.x").that(dragEvent.x).isEqualTo(100f)
        assertWithMessage("dragEvent.y").that(dragEvent.y).isEqualTo(400f)
        // ClipData doesn't implement equals so toString() is the best we can do
        assertWithMessage("dragEvent.clipData.toString()")
            .that(dragEvent.clipData.toString())
            .isEqualTo(expectedClipData.toString())
        for (i in 0 until dragEvent.clipData.itemCount) {
            // ClipData.Item doesn't implement equals so toString() is the best we can do
            assertWithMessage("dragEvent.clipData.getItemAt($i).toString()")
                .that(dragEvent.clipData.getItemAt(i).toString())
                .isEqualTo(expectedClipData.getItemAt(i).toString())
        }
    }
}
