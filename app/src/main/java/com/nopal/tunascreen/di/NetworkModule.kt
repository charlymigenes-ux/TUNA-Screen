package com.nopal.tunascreen.di

import com.nopal.tunascreen.core.auth.TokenStore
import com.nopal.tunascreen.core.network.NopalApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Reescribe scheme/host/port de cada request al del NOPAL pareado
 * actualmente -- Retrofit exige una baseUrl fija en build time, pero el
 * host real solo se conoce después del pairing (y puede cambiar si el
 * usuario separa y parea con otro NOPAL). El baseUrl del Retrofit.Builder
 * de abajo es un placeholder que nunca se usa de verdad. */
private class DynamicBaseUrlInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val configured = tokenStore.currentBaseUrlBlocking()?.toHttpUrlOrNull()
            ?: return chain.proceed(original)
        val rewritten = original.url.newBuilder()
            .scheme(configured.scheme)
            .host(configured.host)
            .port(configured.port)
            .build()
        return chain.proceed(original.newBuilder().url(rewritten).build())
    }
}

private class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.currentTokenBlocking()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            // `header` reemplaza un valor existente. `addHeader` duplicaba
            // Authorization en el handshake WS, porque TunaWebSocketClient
            // ya lo agrega antes de pasar por este interceptor.
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        // Permite que un valor de enum que NOPAL agregue a futuro (y que
        // esta versión de la app no conozca todavía) caiga en Capability.UNKNOWN
        // en vez de romper la deserialización de toda la lista de máquinas.
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenStore: TokenStore): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(DynamicBaseUrlInterceptor(tokenStore))
        .addInterceptor(AuthInterceptor(tokenStore))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // Keepalive del WS -- una tablet de taller queda horas con la
        // conexión abierta y sin tráfico propio del cliente (canal de
        // solo push), sin ping periódico routers/proxies la cortarían.
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("http://localhost/") // sobreescrito por DynamicBaseUrlInterceptor
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideNopalApiService(retrofit: Retrofit): NopalApiService =
        retrofit.create(NopalApiService::class.java)
}
