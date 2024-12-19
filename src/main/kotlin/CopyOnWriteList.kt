package com.moshy.containers

import kotlinx.atomicfu.locks.withLock

/**
 * Copy on write [List] that resembles [java.util.concurrent.CopyOnWriteArrayList] in that reads are unlocked but writes
 * are locked. All writes shall go through [write].
 */
open class CopyOnWriteList<E>
protected constructor(
    initialData: List<E>,
    copier: (List<E>) -> MutableList<E>
): CopyOnWriteContainer<List<E>, MutableList<E>>(initialData, copier), List<E> {
    override val size: Int
        get() = data.size

    override fun contains(element: E) = data.contains(element)
    override fun containsAll(elements: Collection<E>) = data.containsAll(elements)
    override fun get(index: Int): E = data[index]
    override fun indexOf(element: E) = data.indexOf(element)
    override fun lastIndexOf(element: E) = data.lastIndexOf(element)
    override fun isEmpty() = data.isEmpty()
    override fun iterator() = data.iterator()
    override fun listIterator() = data.listIterator()
    override fun listIterator(index: Int) = data.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<E> = SubList(fromIndex, toIndex)

    private inner class SubList(
        private val offset: Int,
        private val toIndex: Int
    ): AbstractList<E>() {
        /* The Kotlin contract for List<E>.subList doesn't require synchronization for structural changes so
         * this is a simple property without a synchronized getter
         */
        override val size = toIndex - offset

        init {
            require(offset >= 0) { "fromIndex < 0" }
            /* A zero-length sublist is useful if we allow structural modifications via add(), addAll(),
             * but we don't, so require a positive size.
             */
            require(size > 0) { "toIndex <= fromIndex" }
            require (offset + size <= this@CopyOnWriteList.size) { "toIndex out of bounds" }
        }
        // CopyOnWriteArrayList.COWSubList does this, thus so do we
        override fun get(index: Int) = lock.withLock {
            checkRange(index)
            this@CopyOnWriteList[offset + index]
        }

        /* Iterator is a snapshot, not a live view;
         * acquire modification lock on iterator() call not per Iterator().next().
         * AbstractList<E> contains(), containsAll() will use this because that's how
         * CopyOnWriteArrayList.COWSubList behaves, anyway.
         */
        override fun iterator() = lock.withLock { this@CopyOnWriteList.data.subList(offset, toIndex).iterator() }
        override fun listIterator() = listIterator(0)
        override fun listIterator(index: Int) = lock.withLock {
            this@CopyOnWriteList.data.subList(offset, toIndex).listIterator(index)
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<E> = lock.withLock {
            checkRange(fromIndex)
            checkRange(toIndex)
            SubList(offset + fromIndex, offset + toIndex)
        }

        private fun checkRange(index: Int) {
            if (index < 0 || index >= size)
                throw IndexOutOfBoundsException("index $index, size $size")
        }

    }
}
