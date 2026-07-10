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

package androidx.annotation.keep

import com.android.build.api.instrumentation.InstrumentationParameters
import java.io.Serializable
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.OutputFile

/** The parameters necessary for the keep annotation plugin. */
abstract class AnnotationPluginParameters : InstrumentationParameters, Serializable {
    /**
     * The file holding on to the keep rules. This will be initialized by the code instrumenting the
     * classes that belong to the project eventually.
     */
    @get:OutputFile internal abstract var keepRules: Provider<RegularFile>
}
