package com.moshy.containers

/**
 * Copy on write [Map] that resembles [java.util.concurrent.CopyOnWriteArrayList] in that reads are unlocked but writes
 * are locked. All writes shall go through [write].
 */
open class CopyOnWriteMap<K, V>
protected constructor(
    initialData: Map<K, V>,
    copier: (Map<K, V>) -> MutableMap<K, V>
): CopyOnWriteContainer<Map<K, V>, MutableMap<K, V>>(initialData, copier), Map<K, V>
{
    override val entries: Set<Map.Entry<K, V>>
        get() = data.entries
    override val keys: Set<K>
        get() = data.keys
    override val size: Int
        get() = data.size
    override val values: Collection<V>
        get() = data.values
    override fun containsKey(key: K) = data.containsKey(key)
    override fun containsValue(value: V) = data.containsValue(value)
    override fun get(key: K): V? = data[key]
    override fun isEmpty() = data.isEmpty()
}
