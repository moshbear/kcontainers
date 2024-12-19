package com.moshy.containers

import com.moshy.containers.util.toCollection
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.test.assertContentEquals

/* Test that we forward Set behavior. CoW behavior already tested in CopyOnWriteContainerTest. */
class CopyOnWriteSetTest {
    private lateinit var c0: CopyOnWriteSet<String>
    private val s = "a"

    @Test
    fun `test MutableSet`() {
        val r1 = c0.write { add(s) }
        val r2 = c0.write { add(s) }
        assertAll(
            { assertTrue(r1) },
            { assertFalse(r2) }
        )
    }

    @Test
    fun `test iterator snapshot`() {
        var iter = c0.iterator()
        c0.write { add(s) }
        assertAll(
            { assertContentEquals(emptySet(), iter.toCollection()) }
        )
        iter = c0.iterator()
        assertAll(
            { assertContentEquals(setOf(s), iter.toCollection()) }
        )
    }

    @Test
    fun set() {
        c0 = CopyOnWriteHashSet(setOf(s))
        val inC0 = s in c0
        val inC0_2 = c0.containsAll(setOf(s))
        assertAll(
            { assertTrue(inC0) },
            { assertTrue(inC0_2) }
        )
    }

    @BeforeEach
    fun initContainer() {
        c0 = CopyOnWriteHashSet()
    }
}