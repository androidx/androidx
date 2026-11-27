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

package androidx.compose.remote.document

import androidx.annotation.RestrictTo

/**
 * Thread-safe registry supplying supported operation IDs for externally defined RemoteCompose
 * profile bitmasks beyond the built-in [RcProfiles] opcodes.
 *
 * Each RemoteCompose client or document builder may hold its own [ProfileOperationRegistry]
 * instance so registrations do not leak across unrelated clients in the same process.
 *
 * Registered providers use additive union semantics: for documents at `apiLevel >= 7`, an opcode is
 * considered supported if either [RcProfiles.isOpcodeSupported] returns `true` or any provider
 * whose registered `profileMask` is satisfied by `profileFlags` includes the opcode. (Unlike
 * built-in [RcProfiles] platform flags, external providers never restrict built-in opcodes.)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProfileOperationRegistry() {
    @Volatile private var providers: Map<Int, (profileFlags: Int) -> Set<Int>> = emptyMap()

    /**
     * Registers [operationsProvider] for documents whose header profile bitmask includes all bits
     * in [profileMask] (`(profileFlags and profileMask) == profileMask`).
     *
     * Re-registering the same [profileMask] replaces the previous provider. Thread-safe via
     * synchronized copy-on-write updates.
     *
     * @throws IllegalArgumentException if [profileMask] is `0`
     */
    public fun register(profileMask: Int, operationsProvider: (profileFlags: Int) -> Set<Int>) {
        require(profileMask != 0) { "profileMask must be non-zero" }
        synchronized(this) {
            val updated = LinkedHashMap(providers)
            updated[profileMask] = operationsProvider
            providers = updated
        }
    }

    /**
     * Removes any provider previously registered for [profileMask]. No-op if [profileMask] is not
     * registered.
     */
    public fun unregister(profileMask: Int) {
        synchronized(this) {
            if (!providers.containsKey(profileMask)) return
            val updated = LinkedHashMap(providers)
            updated.remove(profileMask)
            providers = updated
        }
    }

    /**
     * Returns the union of operation IDs supplied by every registered provider whose `profileMask`
     * matches [profileFlags].
     */
    public fun resolveOperationIds(profileFlags: Int): Set<Int> {
        val snapshot = providers
        if (profileFlags == 0 || snapshot.isEmpty()) return emptySet()
        var result: MutableSet<Int>? = null
        for ((mask, provider) in snapshot) {
            if ((profileFlags and mask) == mask) {
                val ops = provider(profileFlags)
                if (ops.isNotEmpty()) {
                    if (result == null) result = LinkedHashSet()
                    result.addAll(ops)
                }
            }
        }
        return result ?: emptySet()
    }

    /**
     * Returns true if [opcode] is supported for [apiLevel] and [profileFlags] by either built-in
     * [RcProfiles] rules or (when `apiLevel >= 7`) any registered external profile provider.
     *
     * For `apiLevel <= 6`, profile bitmasks are not supported and this delegates solely to
     * [RcProfiles.isOpcodeSupported].
     */
    public fun isOpcodeSupported(opcode: Int, apiLevel: Int, profileFlags: Int): Boolean {
        if (RcProfiles.isOpcodeSupported(opcode, apiLevel, profileFlags)) return true
        if (apiLevel <= 6 || profileFlags == 0) return false
        val snapshot = providers
        if (snapshot.isEmpty()) return false
        for ((mask, provider) in snapshot) {
            if ((profileFlags and mask) == mask && opcode in provider(profileFlags)) {
                return true
            }
        }
        return false
    }
}
