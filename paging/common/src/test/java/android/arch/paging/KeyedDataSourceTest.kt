package android.arch.paging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KeyedDataSourceTest {

    // ----- STANDARD -----

    @Test
    fun loadInitial() {
        val dataSource = ItemDataSource()

        // all
        assertEquals(ITEMS_BY_NAME_ID, dataSource.loadInitial(ITEMS_BY_NAME_ID.size))

        // 10
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 10), dataSource.loadInitial(10))

        // too many
        assertEquals(ITEMS_BY_NAME_ID, dataSource.loadInitial(ITEMS_BY_NAME_ID.size + 10))
    }

    @Test
    fun initializeList_null() {
        val dataSource = ItemDataSource()

        // loadInitial(null, count) == loadInitial(count)
        val initialLoad = dataSource.loadInitial(null, 10)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 10), initialLoad.mList)
        assertEquals(90, initialLoad.trailingNullCount)
    }

    @Test
    fun initializeList_keyed_simple() {
        val dataSource = ItemDataSource()

        // loadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val initialLoad = dataSource.loadInitial(key, 10)!!

        assertEquals(50, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(50, 60), initialLoad.mList)
        assertEquals(40, initialLoad.trailingNullCount)
    }

    @Test
    fun initializeList_keyed_outOfBounds() {
        val dataSource = ItemDataSource()

        // if key is past entire data set, should return last items in data set
        val key = Key("fz", 0)
        val initialLoad = dataSource.loadInitial(key, 10)!!

        assertEquals(90, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(90, 100), initialLoad.mList)
        assertEquals(0, initialLoad.trailingNullCount)
    }

    // ----- UNCOUNTED -----

    @Test
    fun initializeList_null_uncounted() {
        val dataSource = ItemDataSource(counted = false)

        // loadInitial(null, count) == loadInitial(count)
        val initialLoad = dataSource.loadInitial(null, 10)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 10), initialLoad.mList)
        assertEquals(0, initialLoad.trailingNullCount)
    }

    @Test
    fun initializeList_keyed_uncounted() {
        val dataSource = ItemDataSource(counted = false)

        // loadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val initialLoad = dataSource.loadInitial(key, 10)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(50, 60), initialLoad.mList)
        assertEquals(0, initialLoad.trailingNullCount)
    }

    // ----- EMPTY -----

    @Test
    fun initializeList_null_empty() {
        val dataSource = ItemDataSource(empty = true)
        val initialLoad = dataSource.loadInitial(null, 10)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertTrue(initialLoad.mList.isEmpty())
        assertEquals(0, initialLoad.trailingNullCount)
    }

    @Test
    fun initializeList_keyed_empty() {
        val dataSource = ItemDataSource(empty = true)

        // loadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val initialLoad = dataSource.loadInitial(key, 10)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertTrue(initialLoad.mList.isEmpty())
        assertEquals(0, initialLoad.trailingNullCount)
    }

    internal data class Key(val name: String, val id: Int)

    internal data class Item(
            val name: String, val id: Int, val balance: Double, val address: String)

    internal class ItemDataSource(val counted: Boolean = true, val empty: Boolean = false)
            : KeyedDataSource<Key, Item>() {
        override fun getKey(item: Item): Key {
            return Key(item.name, item.id)
        }

        override fun loadInitial(pageSize: Int): List<Item>? {
            // call loadAfter with a default key
            return loadAfter(Key("", Integer.MAX_VALUE), pageSize)
        }

        fun findFirstIndexAfter(key: Key): Int {
            return ITEMS_BY_NAME_ID.indices.firstOrNull {
                KEY_COMPARATOR.compare(key, getKey(ITEMS_BY_NAME_ID[it])) < 0
            } ?: ITEMS_BY_NAME_ID.size
        }

        fun findFirstIndexBefore(key: Key): Int {
            return ITEMS_BY_NAME_ID.indices.reversed().firstOrNull {
                KEY_COMPARATOR.compare(key, getKey(ITEMS_BY_NAME_ID[it])) > 0
            } ?: -1
        }

        override fun countItemsBefore(key: Key): Int {
            if (!counted) {
                return DataSource.COUNT_UNDEFINED
            }

            return findFirstIndexBefore(key) + 1
        }

        override fun countItemsAfter(key: Key): Int {
            if (!counted) {
                return DataSource.COUNT_UNDEFINED
            }

            return ITEMS_BY_NAME_ID.size - findFirstIndexAfter(key)
        }

        override fun loadAfter(key: Key, pageSize: Int): List<Item>? {
            if (empty) {
                return ArrayList()
            }

            val start = findFirstIndexAfter(key)
            val endExclusive = Math.min(start + pageSize, ITEMS_BY_NAME_ID.size)

            return ITEMS_BY_NAME_ID.subList(start, endExclusive)
        }

        override fun loadBefore(key: Key, pageSize: Int): List<Item>? {
            if (empty) {
                return ArrayList()
            }

            val firstIndexBefore = findFirstIndexBefore(key)
            val endExclusive = Math.max(0, firstIndexBefore + 1)
            val start = firstIndexBefore - pageSize + 1

            val list = ITEMS_BY_NAME_ID.subList(start, endExclusive)
            return list.reversed()
        }
    }

    companion object {
        private val ITEM_COMPARATOR = compareBy<Item>( {it.name} ).thenByDescending( {it.id} )
        private val KEY_COMPARATOR = compareBy<Key>( {it.name} ).thenByDescending( {it.id} )

        private val ITEMS_BY_NAME_ID = List(size = 100, init = {
            val names = Array(size = 10, init = {
                "f" + ('a' + it)
            })
            Item(names[it % 10],
                 it,
                 Math.random() * 1000,
                 (Math.random() * 200).toInt().toString() + " fake st.")
        }).sortedWith(ITEM_COMPARATOR)
    }
}
