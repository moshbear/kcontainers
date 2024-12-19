package com.moshy.containers

/**
 * Copy on write [HashSet].
 * @see CopyOnWriteSet
 */
class CopyOnWriteHashSet<E>(initialData: Set<E> = emptySet())
    : CopyOnWriteSet<E>(initialData, ::HashSet)