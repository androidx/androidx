/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime.mock

import androidx.compose.runtime.AbstractApplier

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
class ViewApplier(root: View) : AbstractApplier<View>(root) {
    var called = false

    var onBeginChangesCalled = 0
        private set

    var onEndChangesCalled = 0
        private set

    override fun insertTopDown(index: Int, instance: View) {
        // Ignored as the tree is built bottom-up.
        called = true
    }

    override fun insertBottomUp(index: Int, instance: View) {
        current.addAt(index, instance)
        called = true
    }

    override fun remove(index: Int, count: Int) {
        current.removeAt(index, count)
        called = true
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.moveAt(from, to, count)
        called = true
    }

    override fun onClear() {
        root.removeAllChildren()
        called = true
    }

    override fun onBeginChanges() {
        onBeginChangesCalled++
        called = true
    }

    override fun onEndChanges() {
        onEndChangesCalled++
        called = true
    }

    override var current: View
        get() = super.current.also { if (it != root) called = true }
        set(value) {
            super.current = value
            called = true
        }

    override fun down(node: View) {
        super.down(node)
        called = true
    }

    override fun up() {
        super.up()
        called = true
    }
}
