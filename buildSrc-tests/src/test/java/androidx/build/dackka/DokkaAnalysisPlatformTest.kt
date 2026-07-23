/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build.dackka

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DokkaAnalysisPlatformTest {
    @Test
    fun toJson_serializesToJsonString() {
        val gson = DokkaUtils.createGson()
        assertThat(gson.toJson(DokkaAnalysisPlatform.JVM)).isEqualTo("\"jvm\"")
        assertThat(gson.toJson(DokkaAnalysisPlatform.ANDROID)).isEqualTo("\"jvm\"")
        assertThat(gson.toJson(DokkaAnalysisPlatform.JS)).isEqualTo("\"js\"")
        assertThat(gson.toJson(DokkaAnalysisPlatform.NATIVE)).isEqualTo("\"native\"")
        assertThat(gson.toJson(DokkaAnalysisPlatform.COMMON)).isEqualTo("\"common\"")
    }

    @Test
    fun fromJson_deserializesToDokkaAnalysisPlatform() {
        val gson = DokkaUtils.createGson()
        // "jvm" is the serialized version of both JVM and ANDROID
        assertThat(gson.fromJson("jvm", DokkaAnalysisPlatform::class.java))
            .isEqualTo(DokkaAnalysisPlatform.JVM)
        assertThat(gson.fromJson("js", DokkaAnalysisPlatform::class.java))
            .isEqualTo(DokkaAnalysisPlatform.JS)
        assertThat(gson.fromJson("native", DokkaAnalysisPlatform::class.java))
            .isEqualTo(DokkaAnalysisPlatform.NATIVE)
        assertThat(gson.fromJson("common", DokkaAnalysisPlatform::class.java))
            .isEqualTo(DokkaAnalysisPlatform.COMMON)
    }

    @Test
    fun `Test merging platforms`() {
        assertThat(DokkaAnalysisPlatform.JVM.merge(DokkaAnalysisPlatform.JVM))
            .isEqualTo(DokkaAnalysisPlatform.JVM)
        assertThat(DokkaAnalysisPlatform.JVM.merge(DokkaAnalysisPlatform.ANDROID))
            .isEqualTo(DokkaAnalysisPlatform.JVM)
        assertThat(DokkaAnalysisPlatform.JVM.merge(DokkaAnalysisPlatform.NATIVE))
            .isEqualTo(DokkaAnalysisPlatform.COMMON)
        assertThat(DokkaAnalysisPlatform.COMMON.merge(DokkaAnalysisPlatform.NATIVE))
            .isEqualTo(DokkaAnalysisPlatform.COMMON)
        assertThat(DokkaAnalysisPlatform.JS.merge(DokkaAnalysisPlatform.NATIVE))
            .isEqualTo(DokkaAnalysisPlatform.COMMON)
        assertThat(DokkaAnalysisPlatform.NATIVE.merge(DokkaAnalysisPlatform.NATIVE))
            .isEqualTo(DokkaAnalysisPlatform.NATIVE)
        assertThat(DokkaAnalysisPlatform.NATIVE.merge(null)).isEqualTo(DokkaAnalysisPlatform.NATIVE)
    }
}
