package com.moshy.containers

/**
 * Copy on write [Set] that resembles [java.util.concurrent.CopyOnWriteArrayList] in that reads are unlocked but writes
 * are locked. All writes shall go through [write].
 */
open class CopyOnWriteSet<E>
protected constructor(
    initialData: Set<E>,
    copier: (Set<E>) -> MutableSet<E>
): CopyOnWriteContainer<Set<E>, MutableSet<E>>(initialData, copier), Set<E>
{
    override val size: Int
        get() = data.size
    override fun contains(element: E) = data.contains(element)
    override fun containsAll(elements: Collection<E>) = data.containsAll(elements)
    override fun isEmpty() = data.isEmpty()
    override fun iterator() = data.iterator()
}
