/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.playground

import com.gradle.scan.plugin.BuildScanDataObfuscation

class GradleWorkaround {
    /**
     * This is needed because Gradle configuration caching fails to serialize these lambdas
     * if they are written in Kotlin
     * https://github.com/gradle/gradle/issues/19047
     */
    static void obfuscate(BuildScanDataObfuscation obfuscation) {
        obfuscation.hostname {"unset" }
        obfuscation.ipAddresses { addresses -> addresses.collect { address -> "0.0.0.0"} }
    }
}