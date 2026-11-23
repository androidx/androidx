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

package androidx.appfunctions.evaluation.model

import android.os.Build
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionName
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// ExecuteAppFunctionRequest and ExecuteAppFunctionResponse compare by identity, so the parts that
// wrap them are equal only when they wrap the same instance.
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.BAKLAVA)
class LlmPartTest {

    private val request =
        ExecuteAppFunctionRequest("com.example.notes", "createNote", AppFunctionData.EMPTY)
    private val otherRequest =
        ExecuteAppFunctionRequest("com.example.notes", "deleteNote", AppFunctionData.EMPTY)
    private val functionName = AppFunctionName("com.example.notes", "createNote")
    private val response = ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY)
    private val otherResponse =
        ExecuteAppFunctionResponse.Error(AppFunctionInvalidArgumentException("Unknown note"))

    // TextPart

    @Test
    fun textPartConstructor_givenText_isExposedAsProperty() {
        val part = LlmPart.TextPart("Hello")

        assertThat(part.text).isEqualTo("Hello")
    }

    @Test
    fun textPartEquals_sameText_isEqualWithSameHashCode() {
        val part = LlmPart.TextPart("Hello")
        val same = LlmPart.TextPart("Hello")

        assertThat(part).isEqualTo(same)
        assertThat(part.hashCode()).isEqualTo(same.hashCode())
    }

    @Test
    fun textPartEquals_differentText_isNotEqual() {
        val part = LlmPart.TextPart("Hello")
        val other = LlmPart.TextPart("World")

        assertThat(part).isNotEqualTo(other)
    }

    @Test
    fun toString_textPart_showsText() {
        val part = LlmPart.TextPart("Hello")

        assertThat(part.toString()).isEqualTo("TextPart(text='Hello')")
    }

    // AppFunctionRequestPart

    @Test
    fun appFunctionRequestPartConstructor_givenRequest_isExposedAsProperty() {
        val part = LlmPart.AppFunctionRequestPart(request)

        assertThat(part.request).isSameInstanceAs(request)
    }

    @Test
    fun appFunctionRequestPartEquals_sameRequestInstance_isEqualWithSameHashCode() {
        val part = LlmPart.AppFunctionRequestPart(request)
        val same = LlmPart.AppFunctionRequestPart(request)

        assertThat(part).isEqualTo(same)
        assertThat(part.hashCode()).isEqualTo(same.hashCode())
    }

    @Test
    fun appFunctionRequestPartEquals_requestToAnotherFunction_isNotEqual() {
        val part = LlmPart.AppFunctionRequestPart(request)
        val other = LlmPart.AppFunctionRequestPart(otherRequest)

        assertThat(part).isNotEqualTo(other)
    }

    @Test
    fun toString_appFunctionRequestPart_showsRequest() {
        val part = LlmPart.AppFunctionRequestPart(request)

        assertThat(part.toString()).isEqualTo("AppFunctionRequestPart(request=$request)")
    }

    // AppFunctionResponsePart

    @Test
    fun appFunctionResponsePartConstructor_givenValues_areExposedAsProperties() {
        val part = LlmPart.AppFunctionResponsePart(functionName, response)

        assertThat(part.functionName).isEqualTo(functionName)
        assertThat(part.response).isSameInstanceAs(response)
    }

    @Test
    fun appFunctionResponsePartEquals_sameNameAndResponseInstance_isEqualWithSameHashCode() {
        val part = LlmPart.AppFunctionResponsePart(functionName, response)
        val sameName = AppFunctionName("com.example.notes", "createNote")
        val same = LlmPart.AppFunctionResponsePart(sameName, response)

        assertThat(part).isEqualTo(same)
        assertThat(part.hashCode()).isEqualTo(same.hashCode())
    }

    @Test
    fun appFunctionResponsePartEquals_differentFunctionIdentifier_isNotEqual() {
        val part = LlmPart.AppFunctionResponsePart(functionName, response)
        val otherName = AppFunctionName("com.example.notes", "deleteNote")
        val other = LlmPart.AppFunctionResponsePart(otherName, response)

        assertThat(part).isNotEqualTo(other)
    }

    @Test
    fun appFunctionResponsePartEquals_sameFunctionIdentifierInAnotherPackage_isNotEqual() {
        val part = LlmPart.AppFunctionResponsePart(functionName, response)
        val otherName = AppFunctionName("com.example.tasks", "createNote")
        val other = LlmPart.AppFunctionResponsePart(otherName, response)

        assertThat(part).isNotEqualTo(other)
    }

    @Test
    fun appFunctionResponsePartEquals_differentResponse_isNotEqual() {
        val part = LlmPart.AppFunctionResponsePart(functionName, response)
        val other = LlmPart.AppFunctionResponsePart(functionName, otherResponse)

        assertThat(part).isNotEqualTo(other)
    }

    @Test
    fun toString_appFunctionResponsePart_showsNameAndResponse() {
        val part = LlmPart.AppFunctionResponsePart(functionName, response)

        assertThat(part.toString())
            .isEqualTo("AppFunctionResponsePart(functionName=$functionName, response=$response)")
    }

    // Different kinds of parts

    @Test
    fun equals_partOfAnotherKind_isNotEqual() {
        val text = LlmPart.TextPart("createNote")
        val call = LlmPart.AppFunctionRequestPart(request)
        val result = LlmPart.AppFunctionResponsePart(functionName, response)

        assertThat(text).isNotEqualTo(call)
        assertThat(call).isNotEqualTo(result)
        assertThat(result).isNotEqualTo(text)
    }
}
