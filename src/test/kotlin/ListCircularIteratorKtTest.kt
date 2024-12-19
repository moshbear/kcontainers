package com.moshy.containers

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.function.Executable

class ListCircularIteratorTest {

    @Test
    fun `test basic`() {
        val list = listOf(1, 2, 3)
        val cIter = list.circularIterator()
        val expected = listOf(1, 2, 3, 1, 2, 3, 1)

        assertTrue(cIter.hasNext())
        assertAll(
            expected.map {
                Executable {
                    assertEquals(it, cIter.next())
                }
            }
        )

        /* this is a weak verification of infinite, but as long as we went over [list] values more than once,
         * it's good enough
         */
        assertTrue(cIter.hasNext())
    }

}