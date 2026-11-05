package org.kinera.app.data.selfhost

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import org.kinera.app.data.settings.ServerConnection
import org.kinera.app.data.settings.SettingsRepository

/**
 * Injects self-hosted server credentials into requests aimed at the
 * configured Komga / Suwayomi hosts. Cover and page images are loaded by
 * Coil through the same client, so authentication works everywhere.
 *
 * Reads [SettingsRepository.snapshot], a StateFlow kept fresh by the
 * repository, so no per-request flow collection is needed.
 */
class SelfHostAuthInterceptor(private val settings: SettingsRepository) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host.lowercase()
        if (host.isEmpty()) return chain.proceed(request)

        val snapshot = settings.snapshot.value
        val auth = authFor(snapshot.komga, host, request.url.port)
            ?: authFor(snapshot.suwayomi, host, request.url.port)
            ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder().header("Authorization", auth).build(),
        )
    }

    /** Port must match too: Komga and Suwayomi can share one host. */
    private fun authFor(server: ServerConnection, host: String, port: Int): String? {
        if (!server.isConfigured || server.host != host) return null
        if (server.port != -1 && server.port != port) return null
        if (server.user.isBlank()) return null
        return Credentials.basic(server.user, server.password)
    }
}
