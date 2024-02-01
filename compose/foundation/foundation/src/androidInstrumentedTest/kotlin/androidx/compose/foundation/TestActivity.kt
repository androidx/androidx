/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.foundation

import android.view.DragAndDropPermissions
import android.view.DragEvent
import androidx.activity.ComponentActivity
import java.util.concurrent.CountDownLatch

class TestActivity : ComponentActivity() {
    var hasFocusLatch = CountDownLatch(1)
    val requestedDragAndDropPermissions: MutableList<DragEvent> = mutableListOf()

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hasFocusLatch.countDown()
        }
    }

    override fun requestDragAndDropPermissions(event: DragEvent?): DragAndDropPermissions {
        event?.let { requestedDragAndDropPermissions += it }
        return super.requestDragAndDropPermissions(event)
    }
}
