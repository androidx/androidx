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

package androidx.camera.core

import com.google.common.truth.Truth.assertWithMessage
import java.lang.reflect.Method
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CameraXDslJavaInvisibilityTest {

    private val factoryClasses =
        listOf(
            Class.forName("androidx.camera.core.CameraXDslKt"),
            Class.forName("androidx.camera.core.PreviewScope"),
            Class.forName("androidx.camera.core.ImageCaptureScope"),
            Class.forName("androidx.camera.core.ImageAnalysisScope"),
        )

    @Test
    fun factoryFunctionsAndExtensionPropertiesAreSynthetic() {
        for (clazz in factoryClasses) {
            for (method in clazz.declaredMethods) {
                if (method.isStandardObjectMethod() || method.name == "getBuilder") continue

                assertWithMessage(
                        "Method ${method.name} in ${clazz.name} is not synthetic, " +
                            "making it visible to Java."
                    )
                    .that(method.isSynthetic)
                    .isTrue()
            }
        }
    }

    private fun Method.isStandardObjectMethod(): Boolean {
        return name in listOf("equals", "hashCode", "toString", "clone", "finalize")
    }
}
