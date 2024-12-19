package com.moshy.containers

import com.moshy.containers.util.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.concurrent.thread

/*
 * Tests copy-on-write behavior, basic container operation forwarding (equals, hashCode, toString), helper functions.
 */

class CopyOnWriteContainerTest {
    private lateinit var c0: CoWTracer

    @Test
    fun `test mutation works on copy`() {
        val c0Ref0 = c0.containerRef()
        c0.write { }
        val c0Ref1 = c0.containerRef()
        assertRefsUnequal(c0Ref0, c0Ref1)
    }

    @Test
    fun `test snapshot iterator`() {
        val iter1 = c0.iterator()
        c0.write { clear() }
        val iter2 = c0.iterator()
        assertAll(
            { assertFalse(iter1.hasNext()) },
            { assertTrue(iter2.hasNext()) }
        )
    }
    @Test
    fun `verify writes are serialized`() {
        // Hacky because JVM-dependent, but CoWContainer uses locks instead of coroutine mutexes so use threads
        Assumptions.assumeTrue(Runtime.getRuntime().availableProcessors() > 1,
            "multiple cores are necessary for this test"
        )
        val sleepTimeMillis = 100L // tunable
        val preTime = System.currentTimeMillis()
        val threads = (1..2).map { thread {
            c0.write {
                clear()
                Thread.sleep(sleepTimeMillis, 0)
            }
        } }
        for (t in threads)
            t.join()
        val postTime = System.currentTimeMillis()
        assertTrue((postTime - preTime) >= 2 * sleepTimeMillis)
    }

    @Test
    fun `verify writeOnce freezes the container`() {
        c0.writeOnce { }
        assertThrows<UnsupportedOperationException> {
            c0.write { }
        }
    }

    @Test
    fun `verify equals compares underlying container`() {
        val c1 = CoWTracer()
        assertEquals(c0, c1)
    }

    @Test
    fun `verify frozen container gets propagated`() {
        c0.freeze()
        val c0Ref = c0.containerRef()
        val c1 = CoWTracer(c0)
        val c1Ref = c1.containerRef()
        assertRefsEqual(c0Ref, c1Ref)
    }

    @Test
    fun `verify hashCode`() {
        val c0Ref = c0.containerRef() as TracingMutableCollectionOfObjects
        assertEquals(c0Ref.hashCode(), c0.hashCode())
    }

    @Test
    fun `verify toString`() {
        assertEquals(TracingMutableCollectionOfObjects.STR, c0.toString())
    }

    @Test
    fun `verify require container`() {
        open class C
        class C2(unused: C = C()): C()
        assertThrows<IllegalArgumentException> {
            object : CopyOnWriteContainer<C, C2>(C(), ::C2) { }
        }
    }

    @Test
    fun `verify downstream propagation`() {
        val c1 = CoWTracer(c0)
        c0.write { clear() }
        // we have double wrapping because c0 was never frozen
        val c1Ref = (c1.containerRef() as CoWTracer).containerRef() as TracingMutableCollectionOfObjects
        assertEquals(1, c1Ref.modCount)
    }

    @BeforeEach
    fun initContainer() {
        c0 = CoWTracer()
    }
}

/* Dummy container for tracing writes. We implement MutableCollection<> enough to satisfy the type
 * contract, but absolutely not enough to be general-purpose usable.
 */
private class TracingMutableCollectionOfObjects(): MutableCollection<Any> {
    var modCount: Int = 0
        private set

    /* is copy-constructible */
    constructor(unused: Collection<Any>): this()

    /* implements Collection<> */
    override val size: Int = 0
    override fun contains(element: Any) = unsupported()
    override fun containsAll(elements: Collection<Any>) = unsupported()
    override fun isEmpty() = unsupported()

    /* is container */
    override fun equals(other: Any?) = this === other || other is TracingMutableCollectionOfObjects
    // no data is stored, so hashCode will never change
    override fun hashCode() = 0
    override fun toString(): String = STR

    /* To simulate the iterator-over-immutable-snapshot property, we use a dummy Iterator
     * that produces an infinite sequence of a singleton prior to modification.
     */
    override fun iterator(): MutableIterator<Any> = object : MutableIterator<Any> {
        val modified = modCount == 0
        override fun hasNext(): Boolean = !modified
        override fun next(): Any = object {}
        override fun remove() = unsupported()
    }
    /* implements MutableCollection<> */
    override fun add(element: Any) = unsupported()
    override fun addAll(elements: Collection<Any>) = unsupported()
    override fun clear() { ++modCount }
    override fun remove(element: Any) = unsupported()
    override fun removeAll(elements: Collection<Any>) = unsupported()
    override fun retainAll(elements: Collection<Any>) = unsupported()

    companion object {
        private fun unsupported(vararg unused: Any?): Nothing = throw UnsupportedOperationException()
        const val STR = "TRACER"
    }

}

private typealias TracerContainer = CopyOnWriteContainer<Collection<Any>, TracingMutableCollectionOfObjects>
// Honestly, the only use case I see for implementing CopyOnWriteCollection
private class CoWTracer(init: Collection<Any> = TracingMutableCollectionOfObjects())
    : TracerContainer(init, ::TracingMutableCollectionOfObjects), Collection<Any>
{
    // skeletal Collection<>
    override val size: Int
        get() = data.size
    override fun contains(element: Any) = data.contains(element)
    override fun containsAll(elements: Collection<Any>) = data.containsAll(elements)
    override fun isEmpty() = data.isEmpty()
    override fun iterator() = data.iterator()
}


