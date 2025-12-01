package com.example.booksy.viewmodel

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class HomeViewModelSimpleTest {

    @Test
    fun setTitleAndSetAuthor_updateState() {
        val vm = HomeViewModel()

        vm.setTitle("Harry")
        vm.setAuthor("Rowling")

        val state = vm.ui.value
        assertEquals("Harry", state.title)
        assertEquals("Rowling", state.author)
    }

    @Test
    fun initialState_isCorrect() {
        val vm = HomeViewModel()
        val state = vm.ui.value

        assertEquals("", state.title)
        assertEquals("", state.author)
        assertTrue(state.books.isEmpty())
    }

    @Test
    fun setTitle_changesOnlyTitle() {
        val vm = HomeViewModel()
        vm.setTitle("Nuevo")

        assertEquals("Nuevo", vm.ui.value.title)
    }

}
