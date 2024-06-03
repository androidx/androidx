/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.samples.embedding

import android.app.Activity
import androidx.annotation.Sampled
import androidx.core.app.ActivityOptionsCompat
import androidx.window.embedding.ActivityStack
import androidx.window.embedding.OverlayController
import androidx.window.embedding.SplitController
import androidx.window.embedding.setLaunchingActivityStack

@Sampled
suspend fun launchingOnPrimaryActivityStack() {
    var primaryActivityStack: ActivityStack? = null

    SplitController.getInstance(primaryActivity).splitInfoList(primaryActivity).collect {
        splitInfoList ->
        primaryActivityStack = splitInfoList.last().primaryActivityStack
    }

    primaryActivity.startActivity(
        INTENT,
        ActivityOptionsCompat.makeBasic()
            .toBundle()!!
            .setLaunchingActivityStack(primaryActivity, primaryActivityStack!!)
    )
}

@Sampled
suspend fun launchingOnOverlayActivityStack() {
    var overlayActivityStack: ActivityStack? = null

    OverlayController.getInstance(context).overlayInfo(TAG_OVERLAY).collect { overlayInfo ->
        overlayActivityStack = overlayInfo.activityStack
    }

    // The use case is to launch an Activity to an existing overlay ActivityStack from the overlain
    // Activity. If activityStack is not specified, the activity is launched to the top of the
    // host task behind the overlay ActivityStack.
    overlainActivity.startActivity(
        INTENT,
        ActivityOptionsCompat.makeBasic()
            .toBundle()!!
            .setLaunchingActivityStack(overlainActivity, overlayActivityStack!!)
    )
}

const val TAG_OVERLAY = "overlay"
val overlainActivity = Activity()
