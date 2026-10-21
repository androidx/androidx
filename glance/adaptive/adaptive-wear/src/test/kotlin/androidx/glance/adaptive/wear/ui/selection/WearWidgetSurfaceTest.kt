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

package androidx.glance.adaptive.wear.ui.selection

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WearWidgetSurfaceTest {

    @Test
    fun wearGlanceSurface_aliasesAndTags_resolveCorrectly() {
        assertThat(WearWidgetSurface.WEAR_WIDGET).isEqualTo(WearWidgetSurface.WIDGET)

        assertThat(WearWidgetSurface.WIDGET.tag).isEqualTo("wear_widget")
    }
}
