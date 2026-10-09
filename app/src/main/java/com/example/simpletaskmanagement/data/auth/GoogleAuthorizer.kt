package com.example.simpletaskmanagement.data.auth

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface AuthorizationOutcome {
    data object Success : AuthorizationOutcome
    data class NeedsResolution(val pendingIntent: PendingIntent) : AuthorizationOutcome
    data class Failure(val error: Throwable) : AuthorizationOutcome
}

/**
 * Obtains OAuth access tokens for the Google Tasks scope using the Identity
 * AuthorizationClient and stores them in [AuthManager].
 */
@Singleton
class GoogleAuthorizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) {
    private val client = Identity.getAuthorizationClient(context)

    private val request: AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(TASKS_SCOPE)))
        .build()

    suspend fun authorize(): AuthorizationOutcome {
        return try {
            val result = suspendCancellableCoroutine { cont ->
                client.authorize(request)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
            handleResult(result)
        } catch (e: Exception) {
            AuthorizationOutcome.Failure(e)
        }
    }

    fun handleResultIntent(data: Intent?): AuthorizationOutcome {
        return try {
            handleResult(client.getAuthorizationResultFromIntent(data))
        } catch (e: Exception) {
            AuthorizationOutcome.Failure(e)
        }
    }

    /** Blocking silent refresh; must not be called from the main thread. */
    fun refreshTokenBlocking(): String? {
        return try {
            val result = Tasks.await(client.authorize(request))
            if (result.hasResolution()) return null
            saveResult(result)
        } catch (e: Exception) {
            null
        }
    }

    private fun handleResult(result: AuthorizationResult): AuthorizationOutcome {
        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent
                ?: return AuthorizationOutcome.Failure(IllegalStateException("Missing pending intent"))
            return AuthorizationOutcome.NeedsResolution(pendingIntent)
        }
        return if (saveResult(result) != null) {
            AuthorizationOutcome.Success
        } else {
            AuthorizationOutcome.Failure(IllegalStateException("No access token received"))
        }
    }

    private fun saveResult(result: AuthorizationResult): String? {
        val token = result.accessToken
        Log.d("SyncWorker", "Auth: accessToken ${if (token == null) "NULL" else "recibido"}, scopes=${result.grantedScopes}")
        if (token == null) return null
        val email = result.toGoogleSignInAccount()?.email ?: authManager.getUserEmail()
        authManager.saveAuthData(token, email)
        return token
    }

    companion object {
        const val TASKS_SCOPE = "https://www.googleapis.com/auth/tasks"
    }
}
