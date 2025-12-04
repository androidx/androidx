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

package androidx.compose.remote.integration.view.demos.examples

import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.Rc.TextFromFloat.PAD_PRE_SPACE
import androidx.compose.remote.creation.Rc.TextFromFloat.PAD_PRE_ZERO
import androidx.compose.remote.creation.RemoteComposeContextAndroid

private var mTime: Int = -1

/** Return and ID for a clock String 12:31:23 */
@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.getTimeString(): Int {
    if (mTime == -1) {
        Utils.log("calc")
        beginGlobal()
        val gap = addText(":")
        val tid1 = (Seconds() % 60f).genTextId(2, 0, PAD_PRE_ZERO)
        val tid2 = (Minutes() % 60f).genTextId(2, 0, PAD_PRE_ZERO)
        val tid3 = (Hour() % 12f).genTextId(2, 0, PAD_PRE_SPACE)
        mTime = textMerge(tid3, gap, tid2, gap, tid1)
        endGlobal()
    }
    Utils.log("String" + mTime)
    return mTime
}

private var mBackground: Short = -1

@Suppress("RestrictedApiAndroidX")
fun RemoteComposeContextAndroid.backgroundColor(): Short {
    if (mBackground == (-1).toShort()) {
        beginGlobal()
        mBackground =
            writer.addThemedColor(
                "color.system_accent2_10",
                0xFFDDDDDD.toInt(),
                "color.system_accent2_900",
                0xFF222222.toInt(),
            )
        endGlobal()
    }
    return mBackground
}
