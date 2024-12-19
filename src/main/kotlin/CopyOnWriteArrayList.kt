package com.moshy.containers

/**
 * Copy on write [ArrayList].
 * @see [CopyOnWriteList]
 */
class CopyOnWriteArrayList<E>(initialData: List<E> = emptyList())
    : CopyOnWriteList<E>(initialData, ::ArrayList)
