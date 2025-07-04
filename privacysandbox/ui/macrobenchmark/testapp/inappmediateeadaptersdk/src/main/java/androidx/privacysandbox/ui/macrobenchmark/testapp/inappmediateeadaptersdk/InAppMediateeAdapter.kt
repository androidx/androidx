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

package androidx.privacysandbox.ui.macrobenchmark.testapp.inappmediateeadaptersdk

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.ui.macrobenchmark.testapp.inappmediateesdk.InAppMediateeSdkApi
import androidx.privacysandbox.ui.macrobenchmark.testapp.testsdkprovider.MediateeAdapterInterface

/**
 * Runtime aware class that implements the interface declared by the Mediator.
 *
 * This is registered with Mediator from app.
 */
class InAppMediateeAdapter(private val context: Context) : MediateeAdapterInterface {
    val inAppMediateeSdkApi = InAppMediateeSdkApi(context)

    override suspend fun loadAd(
        adFormat: Int,
        adType: Int,
        waitInsideOnDraw: Boolean,
        drawViewability: Boolean,
    ): Bundle {
        return inAppMediateeSdkApi.loadAd(adFormat, adType, waitInsideOnDraw, drawViewability)
    }
}
