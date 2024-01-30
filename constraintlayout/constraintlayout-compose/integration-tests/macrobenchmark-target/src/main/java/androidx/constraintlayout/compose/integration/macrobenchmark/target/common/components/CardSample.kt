/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose.integration.macrobenchmark.target.common.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.integration.macrobenchmark.target.common.sampledata.LoremIpsum
import androidx.constraintlayout.compose.integration.macrobenchmark.target.common.sampledata.newHourMinuteTimeStamp
import androidx.constraintlayout.compose.integration.macrobenchmark.target.common.sampledata.randomAvatarId
import androidx.constraintlayout.compose.integration.macrobenchmark.target.common.sampledata.randomFullName

@Preview
@Composable
private fun CardSamplePreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (i in 0 until 15) {
            CardSample(
                Modifier
                    .height(80.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
            )
        }
    }
}

private val cardSampleConstraintSet = ConstraintSet {
    val image = createRefFor("image")
    val title = createRefFor("title")
    val description = createRefFor("description")
    val time = createRefFor("timestamp")

    constrain(image) {
        width = Dimension.ratio("1:1")
        height = Dimension.fillToConstraints
        top.linkTo(parent.top)
        bottom.linkTo(parent.bottom)
        start.linkTo(parent.start)
    }
    constrain(title) {
        width = Dimension.preferredWrapContent
        top.linkTo(parent.top)

        linkTo(image.end, time.start, 8.dp, 8.dp, bias = 0f)
    }
    constrain(description) {
        width = Dimension.fillToConstraints

        linkTo(image.end, parent.end, 8.dp, bias = 0f)
        bottom.linkTo(parent.bottom)
    }
    constrain(time) {
        top.linkTo(parent.top)
        end.linkTo(parent.end)
    }
}

@Composable
fun CardSample(
    modifier: Modifier = Modifier,
    @DrawableRes drawableRes: Int = randomAvatarId(),
    title: String = randomFullName(),
    description: String = LoremIpsum.words(50).shuffled().joinToString(" "),
    timeStamp: String = newHourMinuteTimeStamp()
) {
    ConstraintLayout(
        modifier = Modifier
            .defaultMinSize(minWidth = 200.dp, minHeight = 50.dp)
            .then(modifier)
            .padding(4.dp),
        constraintSet = cardSampleConstraintSet
    ) {
        Image(
            modifier = Modifier
                .layoutId("image")
                .clip(RoundedCornerShape(10.dp)),
            painter = painterResource(id = drawableRes),
            contentDescription = null
        )
        Text(
            modifier = Modifier.layoutId("title"),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(modifier = Modifier.layoutId("timestamp"), text = timeStamp)
        Text(
            modifier = Modifier.layoutId("description"),
            text = description,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
