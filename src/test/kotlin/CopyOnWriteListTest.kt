package com.moshy.containers

import com.moshy.containers.util.toCollection
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.test.assertContentEquals

/* Test that we forward List behavior. CoW behavior already tested in CopyOnWriteContainerTest. */
class CopyOnWriteListTest {
    private lateinit var c0: CopyOnWriteList<Int>

    @Test
    fun `test MutableList + get + index`() {
        c0.write {
            add(1)
            add(2)
            add(1)
        }
        assertAll(
            { assertEquals(1, c0[0]) },
            { assertEquals(2, c0[c0.indexOf(2)]) },
            { assertNotEquals(c0.indexOf(1), c0.lastIndexOf(1)) }
        )
    }

    @Test
    fun `test contains + isEmpty`() {
        c0.write {
            add(1)
            add(2)
        }
        assertAll(
            { assertFalse(c0.isEmpty()) },
            { assertTrue(1 in c0) },
            { assertTrue(c0.containsAll(listOf(1, 2))) }
        )
    }

    @Test
    fun `test iterator snapshot`() {
        var iter = c0.iterator()
        c0.write { add(1) }
        assertAll(
            { assertContentEquals(emptySet(), iter.toCollection()) }
        )
        iter = c0.iterator()
        c0.write { add(2) }
        assertAll(
            { assertContentEquals(listOf(1), iter.toCollection()) }
        )
    }

    @Test
    fun `test subList`() {
        c0.write {
            add(1)
            add(2)
            add(3)
        }
        val sl = c0.subList(1, 2)
        c0.write { this[1] = 4 }
        val slIter = sl.listIterator()
        assertAll(
            { assertEquals(4, sl[0]) },
            { assertDoesNotThrow { slIter.next() } }, // c0[2] sl[1]
            { assertTrue(slIter.toCollection().isEmpty()) },
            { assertDoesNotThrow { slIter.previous() } }, // c0[1] sl[0]
            { assertThrows<NoSuchElementException> { slIter.previous() } }, // c0[0] sl[-1]
        )
    }

    @Test
    fun `test range`() {
        c0.write {
            add(1)
            add(2)
            add(3)
        }
        val sl = c0.subList(1, 2)
        assertAll(
            { assertThrows<IndexOutOfBoundsException> { c0[-1] } },
            { assertThrows<IndexOutOfBoundsException> { c0[3] } },
            { assertThrows<IndexOutOfBoundsException> { sl[-1] } },
            { assertThrows<IndexOutOfBoundsException> { sl[1] } },
        )
    }

    @Test
    fun `test recursive subList`() {
        c0.write {
            for (i in 1..5)
                add(i)
        }
        val sl0 = c0.subList(1, 5)
        val sl1 = sl0.subList(1, 2)
        c0.write {
            this[2] = 10
        }
        assertAll(
            { assertEquals(10, sl0[1]) },
            { assertEquals(10, sl1[0]) }
        )
    }

    @BeforeEach
    fun initContainer() {
        c0 = CopyOnWriteArrayList()
    }


}