/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.arena

import org.junit.Assert.assertThat
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ArenaTest {

    private lateinit var arena: GestureArenaManager
    private lateinit var first: TestGestureArenaMember
    private lateinit var second: TestGestureArenaMember

    @Before
    fun setup() {
        arena = GestureArenaManager()
        first = TestGestureArenaMember()
        second = TestGestureArenaMember()
    }

    @Test
    fun `Should win by accepting`() {
        val firstEntry = addFirst()
        addSecond()
        arena.close(primaryKey)
        expectNothing()
        firstEntry.resolve(GestureDisposition.accepted)
        expectFirstWin()
    }

    @Test
    fun `Should win by sweep`() {
        addFirst()
        addSecond()
        arena.close(primaryKey)
        expectNothing()
        arena.sweep(primaryKey)
        expectFirstWin()
    }

    @Test
    fun `Should win on release after hold sweep release`() {
        addFirst()
        addSecond()
        arena.close(primaryKey)
        expectNothing()
        arena.hold(primaryKey)
        expectNothing()
        arena.sweep(primaryKey)
        expectNothing()
        arena.release(primaryKey)
        expectFirstWin()
    }

    @Test
    fun `Should win on sweep after hold release sweep`() {
        addFirst()
        addSecond()
        arena.close(primaryKey)
        expectNothing()
        arena.hold(primaryKey)
        expectNothing()
        arena.release(primaryKey)
        expectNothing()
        arena.sweep(primaryKey)
        expectFirstWin()
    }

    @Test
    fun `Only first winner should win`() {
        val firstEntry = addFirst()
        val secondEntry = addSecond()
        arena.close(primaryKey)
        expectNothing()
        firstEntry.resolve(GestureDisposition.accepted)
        secondEntry.resolve(GestureDisposition.accepted)
        expectFirstWin()
    }

    @Test
    fun `Only first winner should win, regardless of order`() {
        val firstEntry = addFirst()
        val secondEntry = addSecond()
        arena.close(primaryKey)
        expectNothing()
        secondEntry.resolve(GestureDisposition.accepted)
        firstEntry.resolve(GestureDisposition.accepted)
        expectSecondWin()
    }

    @Test
    fun `Win before close is delayed to close`() {
        val firstEntry = addFirst()
        addSecond()
        expectNothing()
        firstEntry.resolve(GestureDisposition.accepted)
        expectNothing()
        arena.close(primaryKey)
        expectFirstWin()
    }

    @Test
    fun `Win before close is delayed to close, and only first winner should win`() {
        val firstEntry = addFirst()
        val secondEntry = addSecond()
        expectNothing()
        firstEntry.resolve(GestureDisposition.accepted)
        secondEntry.resolve(GestureDisposition.accepted)
        expectNothing()
        arena.close(primaryKey)
        expectFirstWin()
    }

    @Test
    fun `Win before close is delayed to close, only first winner wins, regardless of order`() {
        val firstEntry = addFirst()
        val secondEntry = addSecond()
        expectNothing()
        secondEntry.resolve(GestureDisposition.accepted)
        firstEntry.resolve(GestureDisposition.accepted)
        expectNothing()
        arena.close(primaryKey)
        expectSecondWin()
    }

    private fun addFirst(): GestureArenaEntry = arena.add(primaryKey, first)

    private fun addSecond(): GestureArenaEntry = arena.add(primaryKey, second)

    private fun expectNothing() {
        assertThat(first.acceptRan, `is`(false))
        assertThat(first.rejectRan, `is`(false))
        assertThat(second.acceptRan, `is`(false))
        assertThat(second.rejectRan, `is`(false))
    }

    private fun expectFirstWin() {
        assertThat(first.acceptRan, `is`(true))
        assertThat(first.rejectRan, `is`(false))
        assertThat(second.acceptRan, `is`(false))
        assertThat(second.rejectRan, `is`(true))
    }

    private fun expectSecondWin() {
        assertThat(first.acceptRan, `is`(false))
        assertThat(first.rejectRan, `is`(true))
        assertThat(second.acceptRan, `is`(true))
        assertThat(second.rejectRan, `is`(false))
    }

    companion object {
        const val primaryKey: Int = 4
    }

    private inner class TestGestureArenaMember : GestureArenaMember {
        var acceptRan: Boolean = false
        var rejectRan: Boolean = false

        override fun acceptGesture(pointer: Int) {
            assertThat(pointer, `is`(equalTo(primaryKey)))
            acceptRan = true
        }

        override fun rejectGesture(pointer: Int) {
            assertThat(pointer, `is`(equalTo(primaryKey)))
            rejectRan = true
        }
    }
}