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
    fun loadInitial_validateTestDataSource() {
        val dataSource = ItemDataSource()

        // all
        assertEquals(ITEMS_BY_NAME_ID, dataSource.loadInitial(ITEMS_BY_NAME_ID.size))

        // 10
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 10), dataSource.loadInitial(10))

        // too many
        assertEquals(ITEMS_BY_NAME_ID, dataSource.loadInitial(ITEMS_BY_NAME_ID.size + 10))
    }

    @Test
    fun loadInitial() {
        val dataSource = ItemDataSource()

        // loadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val initialLoad = dataSource.loadInitial(key, 10, true)!!

        assertEquals(45, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(45, 55), initialLoad.mList)
        assertEquals(45, initialLoad.trailingNullCount)
    }

    @Test
    fun loadInitial_keyMatchesSingleItem() {
        val dataSource = ItemDataSource(items = ITEMS_BY_NAME_ID.subList(0, 1))

        // this is tricky, since load after and load before with the passed key will fail
        val initialLoad = dataSource.loadInitial(dataSource.getKey(ITEMS_BY_NAME_ID[0]), 20, true)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 1), initialLoad.mList)
        assertEquals(0, initialLoad.trailingNullCount)
    }

    @Test
    fun loadInitial_keyMatchesLastItem() {
        val dataSource = ItemDataSource()

        // tricky, because load after key is empty, so another load before and load after required
        val key = dataSource.getKey(ITEMS_BY_NAME_ID.last())
        val initialLoad = dataSource.loadInitial(key, 20, true)!!

        assertEquals(89, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(89, 100), initialLoad.mList)
        assertEquals(0, initialLoad.trailingNullCount)
    }

    @Test
    fun loadInitial_nullKey() {
        val dataSource = ItemDataSource()

        // loadInitial(null, count) == loadInitial(count)
        val initialLoad = dataSource.loadInitial(null, 10, true)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 10), initialLoad.mList)
        assertEquals(90, initialLoad.trailingNullCount)
    }

    @Test
    fun loadInitial_keyPastEndOfList() {
        val dataSource = ItemDataSource()

        // if key is past entire data set, should return last items in data set
        val key = Key("fz", 0)
        val initialLoad = dataSource.loadInitial(key, 10, true)!!

        // NOTE: ideally we'd load 10 items here, but it adds complexity and unpredictability to
        // do: load after was empty, so pass full size to load before, since this can incur larger
        // loads than requested (see keyMatchesLastItem test)
        assertEquals(95, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(95, 100), initialLoad.mList)
        assertEquals(0, initialLoad.trailingNullCount)
    }

    // ----- UNCOUNTED -----

    @Test
    fun loadInitial_disablePlaceholders() {
        val dataSource = ItemDataSource()

        // loadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val initialLoad = dataSource.loadInitial(key, 10, false)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(45, 55), initialLoad.mList)
        assertEquals(0, initialLoad.trailingNullCount)
    }

    @Test
    fun loadInitial_uncounted() {
        val dataSource = ItemDataSource(counted = false)

        // loadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val initialLoad = dataSource.loadInitial(key, 10, true)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(45, 55), initialLoad.mList)
        assertEquals(0, initialLoad.trailingNullCount)
    }

    @Test
    fun loadInitial_nullKey_uncounted() {
        val dataSource = ItemDataSource(counted = false)

        // loadInitial(null, count) == loadInitial(count)
        val initialLoad = dataSource.loadInitial(null, 10, true)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 10), initialLoad.mList)
        assertEquals(0, initialLoad.trailingNullCount)
    }

    // ----- EMPTY -----

    @Test
    fun loadInitial_empty() {
        val dataSource = ItemDataSource(items = ArrayList())

        // loadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val initialLoad = dataSource.loadInitial(key, 10, true)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertTrue(initialLoad.mList.isEmpty())
        assertEquals(0, initialLoad.trailingNullCount)
    }

    @Test
    fun loadInitial_nullKey_empty() {
        val dataSource = ItemDataSource(items = ArrayList())
        val initialLoad = dataSource.loadInitial(null, 10, true)!!

        assertEquals(0, initialLoad.leadingNullCount)
        assertTrue(initialLoad.mList.isEmpty())
        assertEquals(0, initialLoad.trailingNullCount)
    }

    internal data class Key(val name: String, val id: Int)

    internal data class Item(
            val name: String, val id: Int, val balance: Double, val address: String)

    internal class ItemDataSource(val counted: Boolean = true,
                                  val items: List<Item> = ITEMS_BY_NAME_ID)
            : KeyedDataSource<Key, Item>() {

        override fun getKey(item: Item): Key {
            return Key(item.name, item.id)
        }

        override fun loadInitial(pageSize: Int): List<Item>? {
            // call loadAfter with a default key
            return loadAfter(Key("", Integer.MAX_VALUE), pageSize)
        }

        fun findFirstIndexAfter(key: Key): Int {
            return items.indices.firstOrNull {
                KEY_COMPARATOR.compare(key, getKey(items[it])) < 0
            } ?: items.size
        }

        fun findFirstIndexBefore(key: Key): Int {
            return items.indices.reversed().firstOrNull {
                KEY_COMPARATOR.compare(key, getKey(items[it])) > 0
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

            return items.size - findFirstIndexAfter(key)
        }

        override fun loadAfter(key: Key, pageSize: Int): List<Item>? {
            val start = findFirstIndexAfter(key)
            val endExclusive = Math.min(start + pageSize, items.size)

            return items.subList(start, endExclusive)
        }

        override fun loadBefore(key: Key, pageSize: Int): List<Item>? {
            val firstIndexBefore = findFirstIndexBefore(key)
            val endExclusive = Math.max(0, firstIndexBefore + 1)
            val start = Math.max(0, firstIndexBefore - pageSize + 1)

            val list = items.subList(start, endExclusive)
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
