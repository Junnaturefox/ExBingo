package me.jfenn.bingo.common.utils

class LruHashMap<K, V>(
    private val capacity: Int,
    // accessOrder = true makes the map evict the least *recently accessed* entry, not just the
    // oldest inserted one - without it this is a FIFO cache and hot early entries get dropped.
): LinkedHashMap<K, V>(capacity.coerceAtLeast(1), 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > capacity
    }
}
