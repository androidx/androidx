/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.activity.integration.testapp

import android.app.Application
import com.squareup.leakcanary.AndroidExcludedRefs
import com.squareup.leakcanary.InstrumentationLeakDetector
import java.util.EnumSet

class LeakCanaryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val refs = EnumSet.allOf(AndroidExcludedRefs::class.java)
        refs.remove(AndroidExcludedRefs.INPUT_METHOD_MANAGER__LAST_SERVED_VIEW)
        refs.remove(AndroidExcludedRefs.INPUT_METHOD_MANAGER__ROOT_VIEW)
        refs.remove(AndroidExcludedRefs.INPUT_METHOD_MANAGER__SERVED_VIEW)
        val excludedRefs = AndroidExcludedRefs.createBuilder(refs).build()
        InstrumentationLeakDetector.instrumentationRefWatcher(this)
            .excludedRefs(excludedRefs).buildAndInstall()
    }
}
