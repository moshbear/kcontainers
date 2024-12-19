package com.moshy.containers

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlin.concurrent.Volatile

/**
 * Copy-on-write Container that takes care of common lock-copy-build-save pattern.
 * @param ContainerT Container type (must be a Collection<E> or Map<K, V>)
 * @param MutableContainerT Mutable container type (must be a subclass of ContainerT)
 */
open class CopyOnWriteContainer<ContainerT: Any, MutableContainerT: ContainerT>
protected constructor(
    initialData: ContainerT,
    private val copyConstructor: (ContainerT) -> MutableContainerT
) {
    /* We use the Java approach that CopyOnWriteArrayList takes -
     * writes use a reentrant lock (which was converted to synchronized object in JDK 11)
     * to serialize writes and reads are unlocked.
     */
    @Volatile protected var data = initialData
        private set
    protected var immutable = false
        private set
    // an implementing class may need to acquire the lock for some operation
    protected val lock = ReentrantLock()

    init {
        /* We can't constrain ContainerT to be a subclass of either Map or Collection inside
         * the generic specification, so constrain it here and have it try to enforce that at
         * run-time.
         */
        require(
            when (initialData) {
                is Collection<*>, is Map<*, *> -> true
                else -> false
            }
        ) {
            "This container is not designed to be used with objects that are neither " +
                    "Map<>s nor Collection<>s"
        }
        // unwrap frozen CoWContainer
        (data as? CopyOnWriteContainer<*, *>)?.let {
            if (it.immutable)
                @Suppress("UNCHECKED_CAST")
                data = it.data as ContainerT
        }
    }

    /** Subclasses may call this to get a copy of the data at any time. No locking is performed. */
    protected fun copy() = copyConstructor(data)

    /** Perform a write on this container by applying [block] on a mutable fresh copy of the data. */
    fun <R> write(block: MutableContainerT.() -> R) =
        lock.withLock {
            writeInternal(block)
        }

    /** Mark a container as immutable. This presents an optimization opportunity by finalizing the reference
     * of the internal container. */
    fun freeze() =
        lock.withLock {
            immutable = true
        }

    /** Perform a write on this container by applying [block] on a mutable fresh copy of the data,
     *  then set the container as immutable.
     */
    fun <R> writeOnce(block: MutableContainerT.() -> R) =
        lock.withLock {
            val ret = writeInternal(block)
            freeze()
            ret
        }

    private fun <R> writeInternal(block: MutableContainerT.() -> R): R {
        if (immutable)
            throw UnsupportedOperationException("already immutable")
        val newData = copy()
        val ret = newData.block()
        data = newData
        return ret
    }

    // This is a container, so we must defer equals, hashCode, toString to the data itself
    override fun equals(other: Any?): Boolean =
        (this === other) ||
        (other is CopyOnWriteContainer<*, *> &&
            this.data == other.data
        )
    override fun hashCode(): Int = data.hashCode()
    override fun toString() = data.toString()
}
