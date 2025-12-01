package com.example.booksy.viewmodel

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class RegisterViewModelSimpleTest {

    @Test
    fun initialState_isCorrect() {
        val vm = RegisterViewModel()
        val state = vm.ui.value

        assertEquals("", state.email)
        assertEquals("", state.pass)
        assertEquals("", state.confirm)
        assertFalse(state.loading)
        assertNull(state.error)
        assertNull(state.successUserId)
    }

    @Test
    fun setEmail_updatesEmailOnly() {
        val vm = RegisterViewModel()

        vm.setEmail("nuevo@usuario.com")

        val state = vm.ui.value
        assertEquals("nuevo@usuario.com", state.email)
        assertEquals("", state.pass)
        assertEquals("", state.confirm)
    }

    @Test
    fun setPass_updatesPasswordOnly() {
        val vm = RegisterViewModel()

        vm.setPass("miClave123")

        val state = vm.ui.value
        assertEquals("miClave123", state.pass)
        assertEquals("", state.email)
        assertEquals("", state.confirm)
    }

    @Test
    fun setConfirm_updatesConfirmOnly() {
        val vm = RegisterViewModel()

        vm.setConfirm("claveConfirmada")

        val state = vm.ui.value
        assertEquals("claveConfirmada", state.confirm)
        assertEquals("", state.email)
        assertEquals("", state.pass)
    }

    @Test
    fun allSetters_updateIndependently() {
        val vm = RegisterViewModel()

        vm.setEmail("test@mail.com")
        vm.setPass("password123")
        vm.setConfirm("password123")

        val state = vm.ui.value
        assertEquals("test@mail.com", state.email)
        assertEquals("password123", state.pass)
        assertEquals("password123", state.confirm)
    }

    @Test
    fun setPass_andConfirm_canMatch() {
        val vm = RegisterViewModel()

        val password = "miClaveSegura"
        vm.setPass(password)
        vm.setConfirm(password)

        val state = vm.ui.value
        assertEquals(state.pass, state.confirm)
    }

    @Test
    fun setConfirm_canBeDifferentFromPass() {
        val vm = RegisterViewModel()

        vm.setPass("password1")
        vm.setConfirm("password2")

        val state = vm.ui.value
        assertEquals("password1", state.pass)
        assertEquals("password2", state.confirm)
        assertTrue(state.pass != state.confirm)
    }

    @Test
    fun multipleUpdates_lastValuePersists() {
        val vm = RegisterViewModel()

        vm.setEmail("primero@mail.com")
        vm.setEmail("segundo@mail.com")
        vm.setEmail("final@mail.com")

        assertEquals("final@mail.com", vm.ui.value.email)
    }

    @Test
    fun emptyValues_areAllowed() {
        val vm = RegisterViewModel()

        vm.setEmail("algo@mail.com")
        vm.setPass("clave")
        vm.setConfirm("clave")

        // Limpiar todo
        vm.setEmail("")
        vm.setPass("")
        vm.setConfirm("")

        val state = vm.ui.value
        assertEquals("", state.email)
        assertEquals("", state.pass)
        assertEquals("", state.confirm)
    }
}