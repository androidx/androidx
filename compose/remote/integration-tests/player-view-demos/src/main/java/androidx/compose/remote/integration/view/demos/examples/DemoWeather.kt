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

import android.graphics.BitmapFactory
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.remote.creation.compose.layout.Alignment
import androidx.compose.remote.creation.compose.layout.Arrangement
import androidx.compose.remote.creation.compose.layout.FitBox
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.integration.view.demos.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
@Preview
fun WeatherDemo() {
    RemoteBox {
        RemoteColumn(
            modifier = RemoteModifier.fillMaxWidth(),
            // .background(Color(219, 247, 239) )
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FitBox(RemoteModifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
                val res = LocalContext.current.resources
                val image = remember {
                    BitmapFactory.decodeResource(
                            res,
                            R.drawable.mostly_cloudy,
                            BitmapFactory.Options(),
                        )
                        .asImageBitmap()
                }
                RemoteCollapsibleColumn(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .widthIn(min = 180.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(219, 247, 239))
                ) {
                    WeatherHeader()
                    WeatherRow()
                    WeatherDays()
                }
                RemoteBox(
                    modifier = RemoteModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    RemoteColumn(
                        modifier =
                            RemoteModifier.height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(219, 247, 239))
                                //                            .background(Color.Blue)
                                .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        RemoteText("Rio de Janeiro")
                        RemoteText("100º", fontSize = 26.sp, fontWeight = FontWeight.Medium)
                        RemoteImage(image, RemoteString(""), modifier = RemoteModifier.size(48.dp))
                        RemoteText("H: 62º - L: 54º", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
@Preview
fun WeatherHeader() {
    val res = LocalContext.current.resources
    val image = remember {
        BitmapFactory.decodeResource(res, R.drawable.mostly_cloudy, BitmapFactory.Options())
            .asImageBitmap()
    }
    RemoteRow(
        modifier =
            RemoteModifier.widthIn(min = 180.dp)
                .width(400.dp)
                .heightIn(min = rowHeight)
                .padding(8.dp), // .background(Color.Red),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WeatherBox()
        RemoteBox(RemoteModifier.weight(1f).widthIn(min = 0.dp))
        WeatherBox2()
    }
}

val rowHeight = 90.dp
val rowHeight2 = 122.dp

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
@Preview
fun WeatherBox() {
    RemoteColumn(
        modifier = RemoteModifier.padding(8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        RemoteText("Rio de Janeiro")
        RemoteText("100º", fontSize = 38.sp, fontWeight = FontWeight.Medium)
        RemoteText("High: 62º - Low: 54º", fontSize = 10.sp)
    }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
@Preview
fun WeatherBox2() {
    val res = LocalContext.current.resources
    val image = remember {
        BitmapFactory.decodeResource(res, R.drawable.mostly_cloudy, BitmapFactory.Options())
            .asImageBitmap()
    }
    val refresh = remember {
        BitmapFactory.decodeResource(res, R.drawable.refresh, BitmapFactory.Options())
            .asImageBitmap()
    }
    RemoteColumn(
        modifier = RemoteModifier.height(rowHeight).padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End,
    ) {
        RemoteImage(refresh, RemoteString(""), modifier = RemoteModifier.size(20.dp))
        RemoteImage(image, RemoteString(""), modifier = RemoteModifier.size(48.dp))
        RemoteText("Mostly cloudy", fontSize = 10.sp)
    }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
@Preview
fun WeatherRow() {
    RemoteCollapsibleRow(
        modifier = RemoteModifier.fillMaxWidth().heightIn(min = rowHeight2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Weather("62º", "1 AM", R.drawable.mostly_cloudy)
        Weather("68º", "12 PM", R.drawable.partly_cloudy)
        Weather("82º", "1 PM", R.drawable.mostly_sunny)
        Weather("76º", "2 PM", R.drawable.partly_cloudy)
        Weather("56º", "3 PM", R.drawable.showers_rain)
        Weather("62º", "4 PM", R.drawable.windy_breezy)
    }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
fun Weather(temperature: String, hour: String, resource: Int) {
    val res = LocalContext.current.resources
    val image = remember {
        BitmapFactory.decodeResource(res, resource, BitmapFactory.Options()).asImageBitmap()
    }
    RemoteColumn(RemoteModifier.padding(16.dp)) {
        RemoteText(
            temperature,
            fontWeight = FontWeight.SemiBold,
            modifier = RemoteModifier.padding(bottom = 4.dp),
        )
        RemoteImage(image, RemoteString(""), RemoteModifier.size(24.dp))
        RemoteText(hour, modifier = RemoteModifier.padding(top = 4.dp))
    }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
@Preview
fun WeatherDays() {
    RemoteCollapsibleColumn(
        modifier =
            RemoteModifier.fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(205, 232, 225))
                .padding(8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        WeatherDay("Saturday", "70%", R.drawable.showers_rain, "62º/55ª")
        WeatherDay("Sunday", "", R.drawable.mostly_cloudy, "62º/55ª")
        WeatherDay("Monday", "", R.drawable.partly_cloudy, "62º/55ª")
        WeatherDay("Wednesday", "", R.drawable.windy_breezy, "62º/55ª")
        WeatherDay("Thursday", "70%", R.drawable.showers_rain, "62º/55ª")
        WeatherDay("Friday", "", R.drawable.mostly_cloudy, "62º/55ª")
        WeatherDay("Saturday", "", R.drawable.partly_cloudy, "62º/55ª")
        WeatherDay("Sunday", "", R.drawable.windy_breezy, "62º/55ª")
    }
}

@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
fun WeatherDay(day: String, precipitation: String, image: Int, temperature: String) {
    val res = LocalContext.current.resources
    val image = remember {
        BitmapFactory.decodeResource(res, image, BitmapFactory.Options()).asImageBitmap()
    }
    FitBox(modifier = RemoteModifier.fillMaxWidth()) {
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth().widthIn(min = 200.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteBox(RemoteModifier.width(70.dp), horizontalAlignment = Alignment.Start) {
                RemoteText(day, RemoteModifier.width(70.dp))
            }
            val modWeight = RemoteModifier.weight(1f)
            Temp(modWeight, precipitation, image, temperature)
        }
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteBox(RemoteModifier.width(26.dp), horizontalAlignment = Alignment.Start) {
                RemoteText(day.substring(IntRange(0, 2)), RemoteModifier.width(100.dp))
            }
            val modWeight = RemoteModifier.weight(1f)
            Temp(modWeight, precipitation, image, temperature)
        }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
private fun Temp(
    modWeight: RemoteModifier,
    precipitation: String,
    image: ImageBitmap,
    temperature: String,
) {
    RemoteBox(modWeight.widthIn(min = 0.dp))
    RemoteRow(
        modifier = RemoteModifier.width(70.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteBox(modifier = RemoteModifier.width(30.dp)) {
            RemoteText(precipitation, RemoteModifier.padding(right = 4.dp))
        }
        RemoteImage(image, RemoteString(""), RemoteModifier.size(24.dp))
    }
    RemoteBox(modWeight.widthIn(min = 0.dp))
    RemoteBox(modifier = RemoteModifier.width(60.dp)) { RemoteText(temperature) }
}
