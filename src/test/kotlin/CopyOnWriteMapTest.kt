package com.moshy.containers


import com.moshy.containers.util.toCollection
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.test.assertContentEquals

/* Test that we forward Map behavior. CoW behavior already tested in CopyOnWriteContainerTest.
*  LHM is used because entries.iterator().last() will have predictable behavior.
* */
class CopyOnWriteMapTest {
    private lateinit var c0: CopyOnWriteMap<String, Int>
    private val s = "a"

    @Test
    fun `test MutableMap`() {
        val r1 = c0.write { put(s, 1) }
        val r2 = c0.write { put(s, 2) }
        assertAll(
            { assertNull(r1) },
            { assertNotNull(r2) },
            { assertEquals(1, c0.size) },
            { assertEquals(2, c0[s]) }
        )
    }

    @Test
    fun `test iterator snapshot`() {
        // Map gives us an Iterable through .entries or alternately through .keys and .values
        val iter = c0.iterator()
        c0.write { put(s, 1) }
        assertAll(
            { assertContentEquals(emptySet(), iter.toCollection()) }
        )
        val kIter = c0.keys
        val vIter = c0.values
        c0.write { put(s + s, 2) }
        assertAll(
            { assertContentEquals(setOf(s) as Iterable<*>, kIter) },
            { assertContentEquals(setOf(1) as Iterable<*>, vIter) }
        )
    }

    @Test
    fun map() {
        c0 = CopyOnWriteLinkedHashMap(mapOf(s to 1))
        val inC0k = s in c0
        val inC0v = c0.containsValue(1)
        assertAll(
            { assertTrue(inC0k) },
            { assertTrue(inC0v) },
            { assertFalse(c0.isEmpty()) }
        )
    }

    @BeforeEach
    fun initContainer() {
        c0 = CopyOnWriteLinkedHashMap()
    }
}