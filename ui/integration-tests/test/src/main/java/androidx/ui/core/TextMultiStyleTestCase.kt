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

package androidx.ui.core

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.composer
import androidx.ui.graphics.Color
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Wrap
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.RandomTextGenerator
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextStyle

class TextMultiStyleTestCase(
    activity: Activity,
    private val textLength: Int,
    private val styleCount: Int,
    private val hasMetricAffectingStyle: Boolean,
    private val randomTextGenerator: RandomTextGenerator
) : ComposeTestCase(activity) {

    private lateinit var text: AnnotatedString

    override fun setupContentInternal(activity: Activity): ViewGroup {
        text = randomTextGenerator.nextAnnotatedString(
            length = textLength,
            styleCount = styleCount,
            hasMetricAffectingStyle = hasMetricAffectingStyle
        )
        return super.setupContentInternal(activity)
    }

    override fun setComposeContent(activity: Activity) = activity.setContent {
        Wrap {
            ConstrainedBox(constraints = DpConstraints.tightConstraintsForWidth(160.dp)) {
                Text(text = text, style = TextStyle(color = Color.Black, fontSize = 8.sp))
            }
        }
    }!!
}