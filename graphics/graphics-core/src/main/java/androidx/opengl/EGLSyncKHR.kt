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

package androidx.opengl

/**
 * EGLHandle to wrap native EGLSync objects.
 * This is similar to EGL's EGLSync API except the KHR suffix indicates it is generated
 * as part of the extension APIs namely through [EGLExt.eglCreateSyncKHR].
 */
@Suppress("AcronymName")
class EGLSyncKHR(override val nativeHandle: Long) : EGLHandle {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EGLSyncKHR) return false

        if (nativeHandle != other.nativeHandle) return false

        return true
    }

    override fun hashCode(): Int {
        return nativeHandle.hashCode()
    }

    override fun toString(): String {
        return "EGLSyncKHR(nativeHandle=$nativeHandle)"
    }
}