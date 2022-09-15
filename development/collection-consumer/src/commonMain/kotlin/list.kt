import androidx.collection.LongSparseArray
import androidx.collection.LruCache

fun runCache(): String {
    val cache = object : LruCache<Int, String>(2) {
        override fun create(key: Int): String? {
            return "x".repeat(key)
        }
    }
    cache[1]
    cache[2]
    cache[3]
    return cache.snapshot().toString()
}

fun runSparseArray(): String {
    val array = LongSparseArray<String>()
    array.put(0L, "zero")
    array.put(1L, "one")
    return array.toString()
}