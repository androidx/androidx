/*
 * Copyright 2023 The Android Open Source Project
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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.io.File
import java.lang.reflect.Type
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

internal object DokkaUtils {
    /** Creates a GSON instance that can be used to serialize Dokka CLI json models. */
    fun createGson(): Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(File::class.java, CanonicalFileSerializer())
            .registerTypeAdapter(FileCollection::class.java, FileCollectionSerializer())
            .create()

    /** Serializer for Gradle's [FileCollection] */
    private class FileCollectionSerializer : JsonSerializer<FileCollection> {
        override fun serialize(
            src: FileCollection,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return context.serialize(src.files)
        }
    }

    /**
     * Serializer for [File] instances in the Dokka CLI model.
     *
     * Dokka doesn't work well with relative paths hence we use a canonical paths while setting up
     * its parameters.
     */
    private class CanonicalFileSerializer : JsonSerializer<File> {
        override fun serialize(
            src: File,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            return JsonPrimitive(src.canonicalPath)
        }
    }
}

enum class DokkaAnalysisPlatform(val jsonName: String) {
    JVM("jvm"),
    ANDROID("jvm"), // intentionally same as JVM as dokka only support jvm
    JS("js"),
    NATIVE("native"),
    COMMON("common");

    fun androidOrJvm() = this == JVM || this == ANDROID
}

fun KotlinTarget.docsPlatform() =
    when (platformType) {
        KotlinPlatformType.common -> DokkaAnalysisPlatform.COMMON
        KotlinPlatformType.jvm -> DokkaAnalysisPlatform.JVM
        KotlinPlatformType.js -> DokkaAnalysisPlatform.JS
        KotlinPlatformType.wasm -> DokkaAnalysisPlatform.JS
        KotlinPlatformType.androidJvm -> DokkaAnalysisPlatform.ANDROID
        KotlinPlatformType.native -> DokkaAnalysisPlatform.NATIVE
    }
