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
package androidx.compose.ui.tooling.preview

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_APPLIANCE
import android.content.res.Configuration.UI_MODE_TYPE_CAR
import android.content.res.Configuration.UI_MODE_TYPE_DESK
import android.content.res.Configuration.UI_MODE_TYPE_MASK
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
import android.content.res.Configuration.UI_MODE_TYPE_UNDEFINED
import android.content.res.Configuration.UI_MODE_TYPE_VR_HEADSET
import android.content.res.Configuration.UI_MODE_TYPE_WATCH
import junit.framework.TestCase.assertSame
import org.junit.Test

class AndroidUiModesTest {

    @Test
    fun testAndroidUiModes() {
        assertSame(UI_MODE_TYPE_MASK, AndroidUiModes.UI_MODE_TYPE_MASK)
        assertSame(UI_MODE_TYPE_UNDEFINED, AndroidUiModes.UI_MODE_TYPE_UNDEFINED)
        assertSame(UI_MODE_TYPE_APPLIANCE, AndroidUiModes.UI_MODE_TYPE_APPLIANCE)
        assertSame(UI_MODE_TYPE_CAR, AndroidUiModes.UI_MODE_TYPE_CAR)
        assertSame(UI_MODE_TYPE_DESK, AndroidUiModes.UI_MODE_TYPE_DESK)
        assertSame(UI_MODE_TYPE_NORMAL, AndroidUiModes.UI_MODE_TYPE_NORMAL)
        assertSame(UI_MODE_TYPE_TELEVISION, AndroidUiModes.UI_MODE_TYPE_TELEVISION)
        assertSame(UI_MODE_TYPE_VR_HEADSET, AndroidUiModes.UI_MODE_TYPE_VR_HEADSET)
        assertSame(UI_MODE_TYPE_WATCH, AndroidUiModes.UI_MODE_TYPE_WATCH)
        assertSame(UI_MODE_NIGHT_MASK, AndroidUiModes.UI_MODE_NIGHT_MASK)
        assertSame(UI_MODE_NIGHT_UNDEFINED, AndroidUiModes.UI_MODE_NIGHT_UNDEFINED)
        assertSame(UI_MODE_NIGHT_NO, AndroidUiModes.UI_MODE_NIGHT_NO)
        assertSame(UI_MODE_NIGHT_YES, AndroidUiModes.UI_MODE_NIGHT_YES)
    }
}
