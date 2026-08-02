package com.nopal.tunascreen.core.network

import com.nopal.tunascreen.core.model.ActionRequest
import com.nopal.tunascreen.core.model.ActionResponse
import com.nopal.tunascreen.core.model.Machine
import com.nopal.tunascreen.core.model.MachinesResponse
import com.nopal.tunascreen.core.model.MacrosResponse
import com.nopal.tunascreen.core.model.ConsoleResponse
import com.nopal.tunascreen.core.model.PairConfirmRequest
import com.nopal.tunascreen.core.model.PairConfirmResponse
import com.nopal.tunascreen.core.model.ServerInfo
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/** Espejo 1:1 de backend/api/tunascreen.py -- no agregar acá nada que ese
 * router no exponga. `getInfo` toma una URL absoluta (@Url) porque se llama
 * ANTES de tener sesión/host confirmado (paso de Discovery, para validar
 * que la IP tipeada a mano es realmente un NOPAL), el resto usa la baseUrl
 * ya fijada por NetworkModule una vez pareado. */
interface NopalApiService {

    // Nota: si el dispositivo YA está pareado con un NOPAL, DynamicBaseUrlInterceptor
    // (ver NetworkModule) reescribe igual el host/puerto de esta request al
    // del NOPAL pareado, aunque acá se pase una URL absoluta a otro candidato --
    // por eso Discovery/Pairing solo deben usar este método ANTES de parear
    // (cuando no hay sesión guardada, el interceptor no reescribe nada).
    @GET
    suspend fun getInfo(@Url url: String): ServerInfo

    @POST("api/tunascreen/pair/confirm")
    suspend fun confirmPairing(@Body body: PairConfirmRequest): PairConfirmResponse

    @GET("api/tunascreen/machines")
    suspend fun getMachines(): MachinesResponse

    @GET("api/tunascreen/machine/{id}")
    suspend fun getMachine(@Path("id") id: String): Machine

    @GET("api/tunascreen/machine/{id}/macros")
    suspend fun getMacros(@Path("id") id: String): MacrosResponse

    @GET("api/tunascreen/machine/{id}/console")
    suspend fun getConsole(
        @Path("id") id: String,
        @Query("count") count: Int = 50,
    ): ConsoleResponse

    @POST("api/tunascreen/action")
    suspend fun sendAction(@Body body: ActionRequest): ActionResponse
}
