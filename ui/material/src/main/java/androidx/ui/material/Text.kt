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

package androidx.ui.material

import androidx.ui.core.Text
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Composable

val CurrentTypography = Ambient<TypographyStyle>("current typography") {
    error("No current typography defined!")
}

@Composable
fun H1(@Children content: () -> Unit) {
    <Typography.Consumer> typography ->
        <CurrentTypography.Provider value=typography.h1>
            <content />
        </CurrentTypography.Provider>
    </Typography.Consumer>
}

@Composable
fun MaterialText(text: TextSpan) { // Should match text attrs
    <Colors.Consumer> colors ->
        <CurrentTypography.Consumer> style ->
            val styledText = TextSpan(style = TextStyle(
                color = style.color ?: colors.onPrimary,
                fontSize = style.size,
                fontWeight = style.weight
            ), children = listOf(text))
            <Text text=styledText />
        </CurrentTypography.Consumer>
    </Colors.Consumer>
}
