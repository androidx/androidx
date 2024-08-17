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
package androidx.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScatterSetTest {
    @Test
    fun emptyScatterSetConstructor() {
        val set = MutableScatterSet<String>()
        assertEquals(7, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun immutableEmptyScatterSet() {
        val set: ScatterSet<String> = emptyScatterSet()
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun zeroCapacityScatterSet() {
        val set = MutableScatterSet<String>(0)
        assertEquals(0, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun emptyScatterSetWithCapacity() {
        // When unloading the suggested capacity, we'll fall outside of the
        // expected bucket of 2047 entries, and we'll get 4095 instead
        val set = MutableScatterSet<String>(1800)
        assertEquals(4095, set.capacity)
        assertEquals(0, set.size)
    }

    @Test
    fun mutableScatterSetBuilder() {
        val empty = mutableScatterSetOf<String>()
        assertEquals(0, empty.size)

        val withElements = mutableScatterSetOf("Hello", "World")
        assertEquals(2, withElements.size)
        assertTrue("Hello" in withElements)
        assertTrue("World" in withElements)
    }

    @Test
    fun addToScatterSet() {
        val set = MutableScatterSet<String>()
        set += "Hello"
        assertTrue(set.add("World"))

        assertEquals(2, set.size)
        val elements = Array(2) { "" }
        var index = 0
        set.forEach { element -> elements[index++] = element }
        elements.sort()
        assertEquals("Hello", elements[0])
        assertEquals("World", elements[1])
    }

    @Test
    fun addToSizedScatterSet() {
        val set = MutableScatterSet<String>(12)
        set += "Hello"

        assertEquals(1, set.size)
        assertEquals("Hello", set.first())
    }

    @Test
    fun addExistingElement() {
        val set = MutableScatterSet<String>(12)
        set += "Hello"
        assertFalse(set.add("Hello"))
        set += "Hello"

        assertEquals(1, set.size)
        assertEquals("Hello", set.first())
    }

    @Test
    fun addAllArray() {
        val set = mutableScatterSetOf("Hello")
        assertFalse(set.addAll(arrayOf("Hello")))
        assertEquals(1, set.size)
        assertTrue(set.addAll(arrayOf("Hello", "World")))
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun addAllIterable() {
        val set = mutableScatterSetOf("Hello")
        assertFalse(set.addAll(listOf("Hello")))
        assertEquals(1, set.size)
        assertTrue(set.addAll(listOf("Hello", "World")))
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun addAllSequence() {
        val set = mutableScatterSetOf("Hello")
        assertFalse(set.addAll(listOf("Hello").asSequence()))
        assertEquals(1, set.size)
        assertTrue(set.addAll(listOf("Hello", "World").asSequence()))
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun addAllScatterSet() {
        val set = mutableScatterSetOf("Hello")
        assertFalse(set.addAll(mutableScatterSetOf("Hello")))
        assertEquals(1, set.size)
        assertTrue(set.addAll(mutableScatterSetOf("Hello", "World")))
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun addAllObjectList() {
        val set = mutableScatterSetOf("Hello")
        assertFalse(set.addAll(objectListOf("Hello")))
        assertEquals(1, set.size)
        assertTrue(set.addAll(objectListOf("Hello", "World")))
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun plusAssignArray() {
        val set = mutableScatterSetOf("Hello")
        set += arrayOf("Hello")
        assertEquals(1, set.size)
        set += arrayOf("Hello", "World")
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun plusAssignIterable() {
        val set = mutableScatterSetOf("Hello")
        set += listOf("Hello")
        assertEquals(1, set.size)
        set += listOf("Hello", "World")
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun plusAssignSequence() {
        val set = mutableScatterSetOf("Hello")
        set += listOf("Hello").asSequence()
        assertEquals(1, set.size)
        set += listOf("Hello", "World").asSequence()
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun plusAssignScatterSet() {
        val set = mutableScatterSetOf("Hello")
        set += mutableScatterSetOf("Hello")
        assertEquals(1, set.size)
        set += mutableScatterSetOf("Hello", "World")
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun plusAssignObjectList() {
        val set = mutableScatterSetOf("Hello")
        set += objectListOf("Hello")
        assertEquals(1, set.size)
        set += objectListOf("Hello", "World")
        assertEquals(2, set.size)
        assertTrue("World" in set)
    }

    @Test
    fun nullElement() {
        val set = MutableScatterSet<String?>()
        set += null

        assertEquals(1, set.size)
        assertNull(set.first())
    }

    @Test
    fun firstWithValue() {
        val set = MutableScatterSet<String>()
        set += "Hello"
        set += "World"
        var element: String? = null
        var otherElement: String? = null
        set.forEach { if (element == null) element = it else otherElement = it }
        assertEquals(element, set.first())
        set -= element!!
        assertEquals(otherElement, set.first())
    }

    @Test
    fun firstEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableScatterSet<String>()
            set.first()
        }
    }

    @Test
    fun firstMatching() {
        val set = MutableScatterSet<String>()
        set += "Hello"
        set += "World"
        assertEquals("Hello", set.first { it.contains('H') })
        assertEquals("World", set.first { it.contains('W') })
    }

    @Test
    fun firstMatchingEmpty() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableScatterSet<String>()
            set.first { it.contains('H') }
        }
    }

    @Test
    fun firstMatchingNoMatch() {
        assertFailsWith(NoSuchElementException::class) {
            val set = MutableScatterSet<String>()
            set += "Hello"
            set += "World"
            set.first { it.startsWith("Q") }
        }
    }

    @Test
    fun firstOrNull() {
        val set = MutableScatterSet<String>()
        assertNull(set.firstOrNull { it.startsWith('H') })
        set += "Hello"
        set += "World"
        var element: String? = null
        set.forEach { if (element == null) element = it }
        assertEquals(element, set.firstOrNull { it.contains('l') })
        assertEquals("Hello", set.firstOrNull { it.contains('H') })
        assertEquals("World", set.firstOrNull { it.contains('W') })
        assertNull(set.firstOrNull { it.startsWith('Q') })
    }

    @Test
    fun remove() {
        val set = MutableScatterSet<String?>()
        assertFalse(set.remove("Hello"))

        set += "Hello"
        assertTrue(set.remove("Hello"))
        assertEquals(0, set.size)

        set += "Hello"
        set -= "Hello"
        assertEquals(0, set.size)

        set += null
        assertTrue(set.remove(null))
        assertEquals(0, set.size)

        set += null
        set -= null
        assertEquals(0, set.size)
    }

    @Test
    fun removeThenAdd() {
        // Use a size of 6 to fit in a single entry in the metadata table
        val set = MutableScatterSet<String?>(6)
        set += "Hello"
        set += "Bonjour"
        set += "Hallo"
        set += "Konnichiwa"
        set += "Ciao"
        set += "Annyeong"

        // Removing all the entries will mark the medata as deleted
        set.remove("Hello")
        set.remove("Bonjour")
        set.remove("Hallo")
        set.remove("Konnichiwa")
        set.remove("Ciao")
        set.remove("Annyeong")

        assertEquals(0, set.size)

        val capacity = set.capacity

        // Make sure reinserting an entry after filling the table
        // with "Deleted" markers works
        set += "Hello"

        assertEquals(1, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun removeAllArray() {
        val set = mutableScatterSetOf("Hello", "World")
        assertFalse(set.removeAll(arrayOf("Hola", "Bonjour")))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(arrayOf("Hola", "Hello", "Bonjour")))
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun removeAllIterable() {
        val set = mutableScatterSetOf("Hello", "World")
        assertFalse(set.removeAll(listOf("Hola", "Bonjour")))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(listOf("Hola", "Hello", "Bonjour")))
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun removeAllSequence() {
        val set = mutableScatterSetOf("Hello", "World")
        assertFalse(set.removeAll(sequenceOf("Hola", "Bonjour")))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(sequenceOf("Hola", "Hello", "Bonjour")))
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun removeAllScatterSet() {
        val set = mutableScatterSetOf("Hello", "World")
        assertFalse(set.removeAll(mutableScatterSetOf("Hola", "Bonjour")))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(mutableScatterSetOf("Hola", "Hello", "Bonjour")))
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun removeAllObjectList() {
        val set = mutableScatterSetOf("Hello", "World")
        assertFalse(set.removeAll(objectListOf("Hola", "Bonjour")))
        assertEquals(2, set.size)
        assertTrue(set.removeAll(objectListOf("Hola", "Hello", "Bonjour")))
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun removeDoesNotCauseGrowthOnInsert() {
        val set = MutableScatterSet<String>(10) // Must be > GroupWidth (8)
        assertEquals(15, set.capacity)

        set += "Hello"
        set += "Bonjour"
        set += "Hallo"
        set += "Konnichiwa"
        set += "Ciao"
        set += "Annyeong"

        // Reach the upper limit of what we can store without increasing the map size
        for (i in 0..7) {
            set += i.toString()
        }

        // Delete a few items
        for (i in 0..5) {
            set.remove(i.toString())
        }

        // Inserting a new item shouldn't cause growth, but the deleted markers to be purged
        set += "Foo"
        assertEquals(15, set.capacity)

        assertTrue(set.contains("Foo"))
    }

    @Test
    fun minusAssignArray() {
        val set = mutableScatterSetOf("Hello", "World")
        set -= arrayOf("Hola", "Bonjour")
        assertEquals(2, set.size)
        set -= arrayOf("Hola", "Hello", "Bonjour")
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun minusAssignIterable() {
        val set = mutableScatterSetOf("Hello", "World")
        set -= listOf("Hola", "Bonjour")
        assertEquals(2, set.size)
        set -= listOf("Hola", "Hello", "Bonjour")
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun minusAssignSequence() {
        val set = mutableScatterSetOf("Hello", "World")
        set -= sequenceOf("Hola", "Bonjour")
        assertEquals(2, set.size)
        set -= sequenceOf("Hola", "Hello", "Bonjour")
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun minusAssignScatterSet() {
        val set = mutableScatterSetOf("Hello", "World")
        set -= mutableScatterSetOf("Hola", "Bonjour")
        assertEquals(2, set.size)
        set -= mutableScatterSetOf("Hola", "Hello", "Bonjour")
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun minusAssignObjectList() {
        val set = mutableScatterSetOf("Hello", "World")
        set -= objectListOf("Hola", "Bonjour")
        assertEquals(2, set.size)
        set -= objectListOf("Hola", "Hello", "Bonjour")
        assertEquals(1, set.size)
        assertFalse("Hello" in set)
    }

    @Test
    fun insertManyEntries() {
        val set = MutableScatterSet<String>()

        for (i in 0 until 1700) {
            set += i.toString()
        }

        assertEquals(1700, set.size)
    }

    @Test
    fun forEach() {
        for (i in 0..48) {
            val set = MutableScatterSet<Int>()

            for (j in 0 until i) {
                set += j
            }

            val elements = Array(i) { -1 }
            var index = 0
            set.forEach { element -> elements[index++] = element }
            elements.sort()

            index = 0
            elements.forEach { element ->
                assertEquals(element, index)
                index++
            }
        }
    }

    @Test
    fun clear() {
        val set = MutableScatterSet<String>()

        for (i in 0 until 32) {
            set += i.toString()
        }

        val capacity = set.capacity
        set.clear()

        assertEquals(0, set.size)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun string() {
        val set = MutableScatterSet<String?>()
        assertEquals("[]", set.toString())

        set += "Hello"
        set += "Bonjour"
        assertTrue("[Hello, Bonjour]" == set.toString() || "[Bonjour, Hello]" == set.toString())

        set.clear()
        set += null
        assertEquals("[null]", set.toString())

        set.clear()

        val selfAsElement = MutableScatterSet<Any>()
        selfAsElement.add(selfAsElement)
        assertEquals("[(this)]", selfAsElement.toString())
    }

    @Test
    fun joinToString() {
        val set = scatterSetOf(1, 2, 3, 4, 5)
        val order = IntArray(5)
        var index = 0
        set.forEach { element -> order[index++] = element }
        assertEquals(
            "${order[0]}, ${order[1]}, ${order[2]}, ${order[3]}, ${order[4]}",
            set.joinToString()
        )
        assertEquals(
            "x${order[0]}, ${order[1]}, ${order[2]}...",
            set.joinToString(prefix = "x", postfix = "y", limit = 3)
        )
        assertEquals(
            ">${order[0]}-${order[1]}-${order[2]}-${order[3]}-${order[4]}<",
            set.joinToString(separator = "-", prefix = ">", postfix = "<")
        )
        val names = arrayOf("one", "two", "three", "four", "five")
        assertEquals(
            "${names[order[0]]}, ${names[order[1]]}, ${names[order[2]]}...",
            set.joinToString(limit = 3) { names[it] }
        )
    }

    @Test
    fun hashCodeAddValues() {
        val set = mutableScatterSetOf<String?>()
        assertEquals(217, set.hashCode())
        set += null
        assertEquals(218, set.hashCode())
        set += "Hello"
        val h1 = set.hashCode()
        set += "World"
        assertNotEquals(h1, set.hashCode())
    }

    @Test
    fun equalsTest() {
        val set = MutableScatterSet<String?>()
        set += "Hello"
        set += null
        set += "Bonjour"

        assertFalse(set.equals(null))
        assertEquals(set, set)

        val set2 = MutableScatterSet<String?>()
        set2 += "Bonjour"
        set2 += null

        assertNotEquals(set, set2)

        set2 += "Hello"
        assertEquals(set, set2)
    }

    @Test
    fun contains() {
        val set = MutableScatterSet<String?>()
        set += "Hello"
        set += null
        set += "Bonjour"

        assertTrue(set.contains("Hello"))
        assertTrue(set.contains(null))
        assertFalse(set.contains("World"))
    }

    @Test
    fun empty() {
        val set = MutableScatterSet<String?>()
        assertTrue(set.isEmpty())
        assertFalse(set.isNotEmpty())
        assertTrue(set.none())
        assertFalse(set.any())

        set += "Hello"

        assertFalse(set.isEmpty())
        assertTrue(set.isNotEmpty())
        assertTrue(set.any())
        assertFalse(set.none())
    }

    @Test
    fun count() {
        val set = MutableScatterSet<String>()
        assertEquals(0, set.count())

        set += "Hello"
        assertEquals(1, set.count())

        set += "Bonjour"
        set += "Hallo"
        set += "Konnichiwa"
        set += "Ciao"
        set += "Annyeong"

        assertEquals(2, set.count { it.startsWith("H") })
        assertEquals(0, set.count { it.startsWith("W") })
    }

    @Test
    fun any() {
        val set = MutableScatterSet<String>()
        set += "Hello"
        set += "Bonjour"
        set += "Hallo"
        set += "Konnichiwa"
        set += "Ciao"
        set += "Annyeong"

        assertTrue(set.any { it.startsWith("K") })
        assertFalse(set.any { it.startsWith("W") })
    }

    @Test
    fun all() {
        val set = MutableScatterSet<String>()
        set += "Hello"
        set += "Bonjour"
        set += "Hallo"
        set += "Konnichiwa"
        set += "Ciao"
        set += "Annyeong"

        assertTrue(set.all { it.length >= 4 })
        assertFalse(set.all { it.length >= 5 })
    }

    @Test
    fun asSet() {
        val scatterSet = mutableScatterSetOf("Hello", "World")
        val set = scatterSet.asSet()
        assertEquals(2, set.size)
        assertTrue(set.containsAll(listOf("Hello", "World")))
        assertFalse(set.containsAll(listOf("Hola", "World")))
        assertTrue(set.contains("Hello"))
        assertTrue(set.contains("World"))
        assertFalse(set.contains("Hola"))
        assertFalse(set.isEmpty())
        val elements = Array(2) { "" }
        set.forEachIndexed { index, element -> elements[index] = element }
        elements.sort()
        assertEquals("Hello", elements[0])
        assertEquals("World", elements[1])
    }

    @Test
    fun asMutableSet() {
        val scatterSet = mutableScatterSetOf("Hello", "World")
        val set = scatterSet.asMutableSet()
        assertTrue("Hello" in set)
        assertTrue("World" in set)
        assertFalse("Bonjour" in set)

        assertFalse(set.add("Hello"))
        assertEquals(2, set.size)

        assertTrue(set.add("Hola"))
        assertEquals(3, set.size)
        assertTrue("Hola" in set)

        assertFalse(set.addAll(listOf("World", "Hello")))
        assertEquals(3, set.size)

        assertTrue(set.addAll(listOf("Hello", "Mundo")))
        assertEquals(4, set.size)
        assertTrue("Mundo" in set)

        assertFalse(set.remove("Bonjour"))
        assertEquals(4, set.size)

        assertTrue(set.remove("World"))
        assertEquals(3, set.size)
        assertFalse("World" in set)

        assertFalse(set.retainAll(listOf("Hola", "Hello", "Mundo")))
        assertEquals(3, set.size)

        assertTrue(set.retainAll(listOf("Hola", "Hello")))
        assertEquals(2, set.size)
        assertFalse("Mundo" in set)

        assertFalse(set.removeAll(listOf("Bonjour", "Mundo")))
        assertEquals(2, set.size)

        assertTrue(set.removeAll(listOf("Hello", "Mundo")))
        assertEquals(1, set.size)
        assertFalse("Hello" in set)

        set.clear()
        assertEquals(0, set.size)
        assertFalse("Hola" in set)
    }

    @Test
    fun trim() {
        val set = mutableScatterSetOf("Hello", "World", "Hola", "Mundo", "Bonjour", "Monde")
        val capacity = set.capacity
        assertEquals(0, set.trim())
        set.clear()
        assertEquals(capacity, set.trim())
        assertEquals(0, set.capacity)
        set.addAll(
            arrayOf(
                "Hello",
                "World",
                "Hola",
                "Mundo",
                "Bonjour",
                "Monde",
                "Hallo",
                "Welt",
                "Konnichiwa",
                "Sekai",
                "Ciao",
                "Mondo",
                "Annyeong",
                "Sesang"
            )
        )
        set.removeAll(
            arrayOf("Hallo", "Welt", "Konnichiwa", "Sekai", "Ciao", "Mondo", "Annyeong", "Sesang")
        )
        assertTrue(set.trim() > 0)
        assertEquals(capacity, set.capacity)
    }

    @Test
    fun scatterSetOfEmpty() {
        assertSame(emptyScatterSet<String>(), scatterSetOf<String>())
        assertEquals(0, scatterSetOf<String>().size)
    }

    @Test
    fun scatterSetOfOne() {
        val set = scatterSetOf("Hello")
        assertEquals(1, set.size)
        assertEquals("Hello", set.first())
    }

    @Test
    fun scatterSetOfTwo() {
        val set = scatterSetOf("Hello", "World")
        assertEquals(2, set.size)
        assertTrue("Hello" in set)
        assertTrue("World" in set)
        assertFalse("Bonjour" in set)
    }

    @Test
    fun scatterSetOfThree() {
        val set = scatterSetOf("Hello", "World", "Hola")
        assertEquals(3, set.size)
        assertTrue("Hello" in set)
        assertTrue("World" in set)
        assertTrue("Hola" in set)
        assertFalse("Bonjour" in set)
    }

    @Test
    fun scatterSetOfFour() {
        val set = scatterSetOf("Hello", "World", "Hola", "Mundo")
        assertEquals(4, set.size)
        assertTrue("Hello" in set)
        assertTrue("World" in set)
        assertTrue("Hola" in set)
        assertTrue("Mundo" in set)
        assertFalse("Bonjour" in set)
    }

    @Test
    fun mutableScatterSetOfOne() {
        val set = mutableScatterSetOf("Hello")
        assertEquals(1, set.size)
        assertEquals("Hello", set.first())
    }

    @Test
    fun mutableScatterSetOfTwo() {
        val set = mutableScatterSetOf("Hello", "World")
        assertEquals(2, set.size)
        assertTrue("Hello" in set)
        assertTrue("World" in set)
        assertFalse("Bonjour" in set)
    }

    @Test
    fun mutableScatterSetOfThree() {
        val set = mutableScatterSetOf("Hello", "World", "Hola")
        assertEquals(3, set.size)
        assertTrue("Hello" in set)
        assertTrue("World" in set)
        assertTrue("Hola" in set)
        assertFalse("Bonjour" in set)
    }

    @Test
    fun mutableScatterSetOfFour() {
        val set = mutableScatterSetOf("Hello", "World", "Hola", "Mundo")
        assertEquals(4, set.size)
        assertTrue("Hello" in set)
        assertTrue("World" in set)
        assertTrue("Hola" in set)
        assertTrue("Mundo" in set)
        assertFalse("Bonjour" in set)
    }

    @Test
    fun removeIf() {
        val set = MutableScatterSet<String>()
        set.add("Hello")
        set.add("Bonjour")
        set.add("Hallo")
        set.add("Konnichiwa")
        set.add("Ciao")
        set.add("Annyeong")

        set.removeIf { value -> value.startsWith('H') }

        assertEquals(4, set.size)
        assertTrue(set.contains("Bonjour"))
        assertTrue(set.contains("Konnichiwa"))
        assertTrue(set.contains("Ciao"))
        assertTrue(set.contains("Annyeong"))
    }

    @Test
    fun insertOneRemoveOne() {
        val set = MutableScatterSet<Int>()

        for (i in 0..1000000) {
            set.add(i)
            set.remove(i)
            assertTrue(set.capacity < 16, "Set grew larger than 16 after step $i")
        }
    }

    @Test
    fun insertManyRemoveMany() {
        val map = MutableScatterMap<Int, String>()

        for (i in 0..100) {
            map[i] = i.toString()
        }

        for (i in 0..100) {
            if (i % 2 == 0) {
                map.remove(i)
            }
        }

        for (i in 0..100) {
            if (i % 2 == 0) {
                map[i] = i.toString()
            }
        }

        for (i in 0..100) {
            if (i % 2 != 0) {
                map.remove(i)
            }
        }

        for (i in 0..100) {
            if (i % 2 != 0) {
                map[i] = i.toString()
            }
        }

        assertEquals(127, map.capacity)
        for (i in 0..100) {
            assertTrue(map.contains(i), "Map should contain element $i")
        }
    }

    @Test
    fun removeWhenIterating() {
        val set = MutableScatterSet<String>()
        set.add("Hello")
        set.add("Bonjour")
        set.add("Hallo")
        set.add("Konnichiwa")
        set.add("Ciao")
        set.add("Annyeong")

        val iterator = set.asMutableSet().iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }

        assertEquals(0, set.size)
    }

    @Test
    fun removeWhenForEach() {
        val set = MutableScatterSet<String>()
        set.add("Hello")
        set.add("Bonjour")
        set.add("Hallo")
        set.add("Konnichiwa")
        set.add("Ciao")
        set.add("Annyeong")

        set.forEach { element -> set.remove(element) }

        assertEquals(0, set.size)
    }
}
