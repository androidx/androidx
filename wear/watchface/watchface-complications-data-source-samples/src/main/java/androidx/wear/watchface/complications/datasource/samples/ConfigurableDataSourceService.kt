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

package androidx.wear.watchface.complications.datasource.samples

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

/* Example data source with configuration activity */
class ConfigurableDataSourceService : ComplicationDataSourceService() {
    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val value = getSharedPreferences(ConfigActivity.SHARED_PREF_NAME, 0).getInt(
            ConfigActivity.getKey(
                request.complicationInstanceId,
                ConfigActivity.SHARED_PREF_KEY),
            DEFAULT_VALUE)

        listener.onComplicationData(makeComplicationData(value))
    }

    override fun getPreviewData(type: ComplicationType) = when (type) {
        ComplicationType.SHORT_TEXT -> makeComplicationData(DEFAULT_VALUE)
        else -> null
    }

    private fun makeComplicationData(value: Int): ComplicationData {
        return ShortTextComplicationData.Builder(
            plainText(value.toString()),
            ComplicationText.EMPTY
        ).build()
    }

    companion object {
        // used as default value while data source has not been configured
        const val DEFAULT_VALUE = 0
    }
}