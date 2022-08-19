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

package androidx.build.importMaven

/**
 * Interface that gets notified each time a file is downloaded from an artifactory.
 *
 * @see LocalMavenRepoDownloader
 */
fun interface DownloadObserver {
    /**
     * Called when a file is downloaded from an artifactory.
     *
     * @param path The path of the file relative to the artifactory URL
     * @param bytes The contents of the file
     */
    fun onDownload(path: String, bytes: ByteArray)
}