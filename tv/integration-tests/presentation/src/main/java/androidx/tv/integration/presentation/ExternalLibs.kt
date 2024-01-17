/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.integration.presentation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
// import coil.compose.AsyncImage
// import com.google.gson.Gson

fun getRootDataFromJson(jsonData: String): RootData {
    Log.d("LOL", "getRootDataFromJson: $jsonData")
//    return Gson().fromJson(jsonData, RootData::class.java)
    return RootData(listOf(), listOf(), "")
}

@Composable
fun AppAsyncImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    contentDescription: String? = null
) {
    Log.d(
        "LOL",
        "AppAsyncImage: $imageUrl, $modifier, $contentScale, $alignment, $contentDescription"
    )
//    AsyncImage(
//        model = imageUrl,
//        contentScale = contentScale,
//        alignment = alignment,
//        modifier = modifier,
//        contentDescription = contentDescription
//    )
}
