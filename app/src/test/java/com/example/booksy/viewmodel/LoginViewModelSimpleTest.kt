package com.example.booksy.viewmodel

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class LoginViewModelSimpleTest {

    @Test
    fun initialState_isCorrect() {
        val vm = LoginViewModel()
        val state = vm.ui.value

        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.loading)
        assertNull(state.error)
        assertNull(state.successUserId)
    }

    @Test
    fun setEmail_updatesEmailOnly() {
        val vm = LoginViewModel()

        vm.setEmail("test@ejemplo.com")

        val state = vm.ui.value
        assertEquals("test@ejemplo.com", state.email)
        assertEquals("", state.password) // No debe cambiar
    }

    @Test
    fun setPassword_updatesPasswordOnly() {
        val vm = LoginViewModel()

        vm.setPassword("password123")

        val state = vm.ui.value
        assertEquals("password123", state.password)
        assertEquals("", state.email) // No debe cambiar
    }

    @Test
    fun setEmail_andSetPassword_bothUpdate() {
        val vm = LoginViewModel()

        vm.setEmail("usuario@test.com")
        vm.setPassword("miPassword")

        val state = vm.ui.value
        assertEquals("usuario@test.com", state.email)
        assertEquals("miPassword", state.password)
    }

    @Test
    fun stateFields_areIndependent() {
        val vm = LoginViewModel()

        // Cambiar email
        vm.setEmail("correo@ejemplo.com")
        assertEquals("correo@ejemplo.com", vm.ui.value.email)
        assertEquals("", vm.ui.value.password)

        // Cambiar password
        vm.setPassword("clave123")
        assertEquals("correo@ejemplo.com", vm.ui.value.email)
        assertEquals("clave123", vm.ui.value.password)
    }

    @Test
    fun setEmail_withEmptyString_works() {
        val vm = LoginViewModel()

        vm.setEmail("algo@mail.com")
        vm.setEmail("")

        assertEquals("", vm.ui.value.email)
    }

    @Test
    fun setPassword_withSpecialCharacters_works() {
        val vm = LoginViewModel()

        vm.setPassword("P@ssw0rd!#$")

        assertEquals("P@ssw0rd!#$", vm.ui.value.password)
    }

    @Test
    fun multipleUpdates_lastValuePersists() {
        val vm = LoginViewModel()

        vm.setEmail("primero@mail.com")
        vm.setEmail("segundo@mail.com")
        vm.setEmail("tercero@mail.com")

        assertEquals("tercero@mail.com", vm.ui.value.email)
    }
}