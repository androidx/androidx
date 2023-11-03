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

package androidx.constraintlayout.compose.integration.macrobenchmark.target.common.sampledata

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.constraintlayout.compose.integration.macrobenchmark.target.R

@DrawableRes
private val avatarsIdList: Array<Int> = arrayOf(
    R.drawable.avatar_1,
    R.drawable.avatar_2,
    R.drawable.avatar_3,
    R.drawable.avatar_4,
    R.drawable.avatar_5,
    R.drawable.avatar_6,
    R.drawable.avatar_7,
    R.drawable.avatar_8,
    R.drawable.avatar_9,
    R.drawable.avatar_10,
    R.drawable.avatar_11,
    R.drawable.avatar_12,
    R.drawable.avatar_13,
    R.drawable.avatar_14,
    R.drawable.avatar_15,
    R.drawable.avatar_16,
)

@DrawableRes
internal fun randomAvatarId(): Int = avatarsIdList.random()

internal fun Context.drawableUriOf(@DrawableRes resourceId: Int): Uri =
    with(resources) {
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(getResourcePackageName(resourceId))
            .appendPath(getResourceTypeName(resourceId))
            .appendPath(getResourceEntryName(resourceId))
            .build()
    }
