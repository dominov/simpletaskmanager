package com.example.simpletaskmanagement.data.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow(hasToken())
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun saveAuthData(token: String, email: String? = null) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_USER_EMAIL, email)
            .apply()
        _authState.value = true
    }

    fun clearAuth() {
        prefs.edit().clear().apply()
        _authState.value = false
    }

    fun hasToken(): Boolean {
        return !prefs.getString(KEY_ACCESS_TOKEN, null).isNullOrBlank()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_EMAIL = "user_email"
    }
}
