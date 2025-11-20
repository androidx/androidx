/*
 * Copyright 2020 The Android Open Source Project
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

@file:OptIn(InternalComposeApi::class)
@file:Suppress("EXPECT_ACTUAL_INCOMPATIBILITY") // b/418285824

package androidx.compose.runtime.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composer
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rol
import androidx.compose.runtime.updateChangedFlags

internal const val SLOTS_PER_INT = 10
private const val BITS_PER_SLOT = 3

internal fun bitsForSlot(bits: Int, slot: Int): Int {
    val realSlot = slot.rem(SLOTS_PER_INT)
    return bits shl (realSlot * BITS_PER_SLOT + 1)
}

internal fun sameBits(slot: Int): Int = bitsForSlot(0b01, slot)

internal fun differentBits(slot: Int): Int = bitsForSlot(0b10, slot)

/**
 * A Restart is created to hold composable lambdas to track when they are invoked allowing the
 * invocations to be invalidated when a new composable lambda is created during composition.
 *
 * This allows much of the call-graph to be skipped when a composable function is passed through
 * multiple levels of composable functions.
 */
@Suppress("NAME_SHADOWING", "UNCHECKED_CAST", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
@Stable
internal class ComposableLambdaImpl(val key: Int, private val tracked: Boolean, block: Any?) :
    ComposableLambda {
    private var _block: Any? = block
    private var scope: RecomposeScope? = null
    private var scopes: MutableList<RecomposeScope>? = null

    private fun trackWrite() {
        if (tracked) {
            val scope = this.scope
            if (scope != null) {
                scope.invalidate()
                this.scope = null
            }
            val scopes = this.scopes
            if (scopes != null) {
                for (index in 0 until scopes.size) {
                    val item = scopes[index]
                    item.invalidate()
                }
                scopes.clear()
            }
        }
    }

    private fun trackRead(composer: Composer) {
        if (tracked) {
            val scope = composer.recomposeScope
            if (scope != null) {
                // Find the first invalid scope and replace it or record it if no scopes are invalid
                composer.recordUsed(scope)
                val lastScope = this.scope
                if (lastScope.replacableWith(scope)) {
                    this.scope = scope
                } else {
                    val lastScopes = scopes
                    if (lastScopes == null) {
                        val newScopes = mutableListOf<RecomposeScope>()
                        scopes = newScopes
                        newScopes.add(scope)
                    } else {
                        for (index in 0 until lastScopes.size) {
                            val scopeAtIndex = lastScopes[index]
                            if (scopeAtIndex.replacableWith(scope)) {
                                lastScopes[index] = scope
                                return
                            }
                        }
                        lastScopes.add(scope)
                    }
                }
            }
        }
    }

    fun update(block: Any) {
        if (_block != block) {
            val oldBlockNull = _block == null
            _block = block
            if (!oldBlockNull) {
                trackWrite()
            }
        }
    }

    override operator fun invoke(c: Composer, changed: Int): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(0) else sameBits(0)
        val result = (_block as (c: Composer, changed: Int) -> Any?)(c, dirty)
        c.endRestartGroup()?.updateScope(this::invoke)
        return result
    }

    override operator fun invoke(p1: Any?, c: Composer, changed: Int): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(1) else sameBits(1)
        val result = (_block as (p1: Any?, c: Composer, changed: Int) -> Any?)(p1, c, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, nc, updateChangedFlags(changed) or 0b1)
        }
        return result
    }

    override operator fun invoke(p1: Any?, p2: Any?, c: Composer, changed: Int): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(2) else sameBits(2)
        val result =
            (_block as (p1: Any?, p2: Any?, c: Composer, changed: Int) -> Any?)(p1, p2, c, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, nc, updateChangedFlags(changed) or 0b1)
        }
        return result
    }

    override operator fun invoke(p1: Any?, p2: Any?, p3: Any?, c: Composer, changed: Int): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(3) else sameBits(3)
        val result =
            (_block as (p1: Any?, p2: Any?, p3: Any?, c: Composer, changed: Int) -> Any?)(
                p1,
                p2,
                p3,
                c,
                dirty,
            )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, nc, updateChangedFlags(changed) or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        c: Composer,
        changed: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(4) else sameBits(4)
        val result =
            (_block as (p1: Any?, p2: Any?, p3: Any?, p4: Any?, c: Composer, changed: Int) -> Any?)(
                p1,
                p2,
                p3,
                p4,
                c,
                dirty,
            )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, nc, updateChangedFlags(changed) or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        c: Composer,
        changed: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(5) else sameBits(5)
        val result =
            (_block
                as
                (
                    p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?, c: Composer, changed: Int,
                ) -> Any?)(p1, p2, p3, p4, p5, c, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, nc, updateChangedFlags(changed) or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        c: Composer,
        changed: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(6) else sameBits(6)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    c: Composer,
                    changed: Int,
                ) -> Any?)(p1, p2, p3, p4, p5, p6, c, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, nc, updateChangedFlags(changed) or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        c: Composer,
        changed: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(7) else sameBits(7)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    c: Composer,
                    changed: Int,
                ) -> Any?)(p1, p2, p3, p4, p5, p6, p7, c, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, nc, updateChangedFlags(changed) or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        c: Composer,
        changed: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(8) else sameBits(8)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    c: Composer,
                    changed: Int,
                ) -> Any?)(p1, p2, p3, p4, p5, p6, p7, p8, c, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, nc, updateChangedFlags(changed) or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        c: Composer,
        changed: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(9) else sameBits(9)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    c: Composer,
                    changed: Int,
                ) -> Any?)(p1, p2, p3, p4, p5, p6, p7, p8, p9, c, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, nc, updateChangedFlags(changed) or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        c: Composer,
        changed: Int,
        changed1: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(10) else sameBits(10)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    p10: Any?,
                    c: Composer,
                    changed: Int,
                    changed1: Int,
                ) -> Any?)(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, c, changed, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, nc, changed or 0b1, changed)
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        c: Composer,
        changed: Int,
        changed1: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(11) else sameBits(11)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    p10: Any?,
                    p11: Any?,
                    c: Composer,
                    changed: Int,
                    changed1: Int,
                ) -> Any?)(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, c, changed, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                nc,
                updateChangedFlags(changed) or 0b1,
                updateChangedFlags(changed1),
            )
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        c: Composer,
        changed: Int,
        changed1: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(12) else sameBits(12)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    p10: Any?,
                    p11: Any?,
                    p12: Any?,
                    c: Composer,
                    changed: Int,
                    changed1: Int,
                ) -> Any?)(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, c, changed, dirty)
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                nc,
                updateChangedFlags(changed) or 0b1,
                updateChangedFlags(changed1),
            )
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        c: Composer,
        changed: Int,
        changed1: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(13) else sameBits(13)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    p10: Any?,
                    p11: Any?,
                    p12: Any?,
                    p13: Any?,
                    c: Composer,
                    changed: Int,
                    changed1: Int,
                ) -> Any?)(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                c,
                changed,
                dirty,
            )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                nc,
                updateChangedFlags(changed) or 0b1,
                updateChangedFlags(changed1),
            )
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        c: Composer,
        changed: Int,
        changed1: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(14) else sameBits(14)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    p10: Any?,
                    p11: Any?,
                    p12: Any?,
                    p13: Any?,
                    p14: Any?,
                    c: Composer,
                    changed: Int,
                    changed1: Int,
                ) -> Any?)(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                c,
                changed,
                dirty,
            )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                nc,
                updateChangedFlags(changed) or 0b1,
                updateChangedFlags(changed1),
            )
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        c: Composer,
        changed: Int,
        changed1: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(15) else sameBits(15)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    p10: Any?,
                    p11: Any?,
                    p12: Any?,
                    p13: Any?,
                    p14: Any?,
                    p15: Any?,
                    c: Composer,
                    changed: Int,
                    changed1: Int,
                ) -> Any?)(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                c,
                changed,
                dirty,
            )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                nc,
                updateChangedFlags(changed) or 0b1,
                updateChangedFlags(changed1),
            )
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        c: Composer,
        changed: Int,
        changed1: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(16) else sameBits(16)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    p10: Any?,
                    p11: Any?,
                    p12: Any?,
                    p13: Any?,
                    p14: Any?,
                    p15: Any?,
                    p16: Any?,
                    c: Composer,
                    changed: Int,
                    changed1: Int,
                ) -> Any?)(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                p16,
                c,
                changed,
                dirty,
            )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                p16,
                nc,
                updateChangedFlags(changed) or 0b1,
                updateChangedFlags(changed1),
            )
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        p17: Any?,
        c: Composer,
        changed: Int,
        changed1: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(17) else sameBits(17)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    p10: Any?,
                    p11: Any?,
                    p12: Any?,
                    p13: Any?,
                    p14: Any?,
                    p15: Any?,
                    p16: Any?,
                    p17: Any?,
                    c: Composer,
                    changed: Int,
                    changed1: Int,
                ) -> Any?)(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                p16,
                p17,
                c,
                changed,
                dirty,
            )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                p16,
                p17,
                nc,
                updateChangedFlags(changed) or 0b1,
                updateChangedFlags(changed1),
            )
        }
        return result
    }

    override operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        p17: Any?,
        p18: Any?,
        c: Composer,
        changed: Int,
        changed1: Int,
    ): Any? {
        val c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(18) else sameBits(18)
        val result =
            (_block
                as
                (
                    p1: Any?,
                    p2: Any?,
                    p3: Any?,
                    p4: Any?,
                    p5: Any?,
                    p6: Any?,
                    p7: Any?,
                    p8: Any?,
                    p9: Any?,
                    p10: Any?,
                    p11: Any?,
                    p12: Any?,
                    p13: Any?,
                    p14: Any?,
                    p15: Any?,
                    p16: Any?,
                    p17: Any?,
                    p18: Any?,
                    c: Composer,
                    changed: Int,
                    changed1: Int,
                ) -> Any?)(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                p16,
                p17,
                p18,
                c,
                changed,
                dirty,
            )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                p16,
                p17,
                p18,
                nc,
                updateChangedFlags(changed) or 0b1,
                updateChangedFlags(changed1),
            )
        }
        return result
    }
}

internal fun RecomposeScope?.replacableWith(other: RecomposeScope) =
    this == null ||
        (this is RecomposeScopeImpl &&
            other is RecomposeScopeImpl &&
            (!this.valid || this == other || this.anchor == other.anchor))

@ComposeCompilerApi
@Stable
public expect interface ComposableLambda {
    public operator fun invoke(p1: Composer, p2: Int): Any?

    public operator fun invoke(p1: Any?, p2: Composer, p3: Int): Any?

    public operator fun invoke(p1: Any?, p2: Any?, p3: Composer, p4: Int): Any?

    public operator fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Composer, p5: Int): Any?

    public operator fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Composer, p6: Int): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Composer,
        p7: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Composer,
        p8: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Composer,
        p9: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Composer,
        p10: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Composer,
        p11: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Composer,
        p12: Int,
        p13: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Composer,
        p13: Int,
        p14: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Composer,
        p14: Int,
        p15: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Composer,
        p15: Int,
        p16: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Composer,
        p16: Int,
        p17: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Composer,
        p17: Int,
        p18: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        p17: Composer,
        p18: Int,
        p19: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        p17: Any?,
        p18: Composer,
        p19: Int,
        p20: Int,
    ): Any?

    public operator fun invoke(
        p1: Any?,
        p2: Any?,
        p3: Any?,
        p4: Any?,
        p5: Any?,
        p6: Any?,
        p7: Any?,
        p8: Any?,
        p9: Any?,
        p10: Any?,
        p11: Any?,
        p12: Any?,
        p13: Any?,
        p14: Any?,
        p15: Any?,
        p16: Any?,
        p17: Any?,
        p18: Any?,
        p19: Composer,
        p20: Int,
        p21: Int,
    ): Any?
}

@Suppress("unused")
@ComposeCompilerApi
public fun composableLambda(
    composer: Composer,
    key: Int,
    tracked: Boolean,
    block: Any,
): ComposableLambda {
    // Use a rolled version of the key to avoid the key being a duplicate of the function's
    // key. This is particularly important for live edit scenarios where the groups will be
    // invalidated by the key number. This ensures that invalidating the function will not
    // also invalidate its lambda.
    composer.startMovableGroup(key.rol(1), lambdaKey)
    val slot = composer.rememberedValue()
    val result =
        if (slot === Composer.Empty) {
            val value = ComposableLambdaImpl(key, tracked, block)
            composer.updateRememberedValue(value)
            value
        } else {
            slot as ComposableLambdaImpl
            slot.update(block)
            slot
        }
    composer.endMovableGroup()
    return result
}

private val lambdaKey = Any()

@Suppress("unused")
@ComposeCompilerApi
public fun composableLambdaInstance(key: Int, tracked: Boolean, block: Any): ComposableLambda =
    ComposableLambdaImpl(key, tracked, block)

@Suppress("unused")
@Composable
@ComposeCompilerApi
public fun rememberComposableLambda(key: Int, tracked: Boolean, block: Any): ComposableLambda =
    remember { ComposableLambdaImpl(key, tracked, block) }.also { it.update(block) }
