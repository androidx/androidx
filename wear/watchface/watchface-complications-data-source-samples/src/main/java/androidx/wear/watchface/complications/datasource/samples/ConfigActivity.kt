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

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import kotlin.random.Random

/** Config activity for data source which generates random in [0..100) range */
class ConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.config_activity_layout)

        val complicationId =
            intent.getIntExtra(ComplicationDataSourceService.EXTRA_CONFIG_COMPLICATION_ID, -1)

        findViewById<View>(R.id.config_gen_button).setOnClickListener {
            val num = Random.nextInt(100)
            getSharedPreferences(SHARED_PREF_NAME, 0)
                .edit()
                .putInt(getKey(complicationId, SHARED_PREF_KEY), num)
                .apply()

            setResult(RESULT_OK)
            finish()
        }

        setResult(RESULT_CANCELED)
    }

    companion object {
        const val SHARED_PREF_NAME = "data_source_config"
        const val SHARED_PREF_KEY = "num"

        fun getKey(complicationId: Int, prefKey: String): String {
            return "$complicationId/$prefKey"
        }
    }
}
