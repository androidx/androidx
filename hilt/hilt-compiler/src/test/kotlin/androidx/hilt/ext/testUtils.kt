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

package androidx.hilt.ext

import androidx.hilt.ClassNames
import androidx.room.compiler.processing.util.Source
import java.io.File

val GENERATED_TYPE = try {
    Class.forName("javax.annotation.processing.Generated")
    "javax.annotation.processing.Generated"
} catch (_: ClassNotFoundException) {
    "javax.annotation.Generated"
}

val GENERATED_ANNOTATION =
    "@Generated(\"androidx.hilt.AndroidXHiltProcessor\")"

object Sources {
    val LISTENABLE_WORKER by lazy {
        loadJavaSource(
            "ListenableWorker.java",
            ClassNames.LISTENABLE_WORKER.toString()
        )
    }

    val WORKER by lazy {
        loadJavaSource(
            "Worker.java",
            ClassNames.WORKER.toString()
        )
    }

    val WORKER_PARAMETERS by lazy {
        loadJavaSource(
            "WorkerParameters.java",
            ClassNames.WORKER_PARAMETERS.toString()
        )
    }
}

fun loadJavaSource(fileName: String, qName: String) = Source.loadJavaSource(
    file = File("src/test/data/sources/$fileName"),
    qName = qName
)
