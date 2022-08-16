import androidx.build.AndroidXPluginTestContext

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

/**
 * Avoid calling git in tests by taking advantage of environment variables with changelist info
 * and manifest of changed files.  (These are usually set by busytown and detected in our
 * builds, cf b/203692753)
 */
fun AndroidXPluginTestContext.environmentForExplicitChangeInfo(): Map<String, String> {
    val gitChangeFilesDir = tmpFolder.newFolder()
    val gitChangeInfoFilename = gitChangeFilesDir.resolve("CHANGE_INFO").apply {
        writeText("{}")
    }
    val gitManifestFilename = gitChangeFilesDir.resolve("MANIFEST").apply {
        writeText("path=\"frameworks/support\" revision=\"testRev\" ")
    }
    return mapOf(
        "CHANGE_INFO" to gitChangeInfoFilename.path,
        "MANIFEST" to gitManifestFilename.path
    )
}