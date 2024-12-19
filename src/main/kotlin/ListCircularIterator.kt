package com.moshy.containers

/**
 * Provides an infinite circular view over a list.
 *
 * `listOf(A, B, C).circularIterator()` will yield A B C A B C A ...
 *
 * Use [List.subList] to generate a circular view over a portion of the list.
 */
fun <T> List<T>.circularIterator(): Iterator<T> = ListCircularIterator(this)

private class ListCircularIterator<E>(private val list: List<E>): Iterator<E> {
    private val size = list.size
    private var i = 0

    override fun hasNext() = true

    override fun next(): E {
        if (i == size)
            i = 0
        return list[i++ % size]
    }
}

