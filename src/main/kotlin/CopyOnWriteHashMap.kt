package com.moshy.containers

/**
 * Copy on write [HashMap].
 * @see CopyOnWriteMap
 */
class CopyOnWriteHashMap<K, V>(initialData: Map<K, V> = emptyMap())
    : CopyOnWriteMap<K, V>(initialData, ::HashMap)
