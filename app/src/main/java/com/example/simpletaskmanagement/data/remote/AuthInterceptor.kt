package com.example.simpletaskmanagement.data.remote

import com.example.simpletaskmanagement.data.auth.AuthManager
import com.example.simpletaskmanagement.data.auth.GoogleAuthorizer
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val authManager: AuthManager,
    private val googleAuthorizer: GoogleAuthorizer
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = authManager.getAccessToken()
        val response = chain.proceed(buildRequest(chain.request(), token))

        if (response.code != 401 || token.isNullOrBlank()) return response

        // Access token expired: try a silent refresh and retry once.
        val newToken = googleAuthorizer.refreshTokenBlocking() ?: return response
        response.close()
        return chain.proceed(buildRequest(chain.request(), newToken))
    }

    private fun buildRequest(original: Request, token: String?): Request {
        val builder = original.newBuilder().header("Accept", "application/json")
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        return builder.build()
    }
}
