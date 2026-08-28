/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.test.backup.host

/**
 * Represents the platform-transport emulation mode utilized during the backup and restore phases.
 *
 * This class is designed to be non-exhaustive to allow future transport types to be introduced in a
 * binary-compatible manner. Standard pattern-matching `when` expressions on this type must use an
 * explicit `else` fallback branch.
 */
public class BackupTransportMode private constructor(private val name: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackupTransportMode) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    public companion object {
        /**
         * Simulates a physical peer-to-device direct migration (e.g. transfer via USB/Wi-Fi
         * Direct).
         */
        @JvmField
        public val DEVICE_TO_DEVICE: BackupTransportMode = BackupTransportMode("DEVICE_TO_DEVICE")

        /** Simulates a cloud-based backup utilizing secure client-side end-to-end encryption. */
        @JvmField
        public val CLOUD_ENCRYPTED: BackupTransportMode = BackupTransportMode("CLOUD_ENCRYPTED")

        /** Simulates standard cloud-based backup without client-side encryption bounds. */
        @JvmField
        public val CLOUD_UNENCRYPTED: BackupTransportMode = BackupTransportMode("CLOUD_UNENCRYPTED")

        /** Performs a local, low-latency offline filesystem transport backup. */
        @JvmField public val LOCAL: BackupTransportMode = BackupTransportMode("LOCAL")
    }
}
