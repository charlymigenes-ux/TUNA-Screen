package com.nopal.tunascreen.feature.workshop.components

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.nopal.tunascreen.ui.theme.NopalGreen
import com.nopal.tunascreen.ui.theme.NopalSurfaceElevated
import com.nopal.tunascreen.ui.theme.NopalTextSecondary
import kotlinx.coroutines.delay
import java.net.URI

internal data class CameraStreamRequest(
    val url: String,
    val authorization: String?,
)

/**
 * Resuelve el contrato de cámara de NOPAL sin filtrar el token hacia cámaras
 * externas. Una URL relativa pertenece al servidor pareado; una absoluta solo
 * recibe Authorization cuando conserva exactamente el mismo origen.
 */
internal fun resolveCameraStream(
    streamUrl: String?,
    baseUrl: String?,
    token: String?,
): CameraStreamRequest? {
    val raw = streamUrl?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    val base = baseUrl?.trim()?.trimEnd('/')
    val resolved = runCatching {
        val candidate = URI(raw)
        if (candidate.isAbsolute) candidate else URI("$base/").resolve(candidate)
    }.getOrNull() ?: return null
    if (resolved.scheme !in setOf("http", "https") || resolved.host.isNullOrBlank()) return null

    val sameOrigin = runCatching {
        val server = URI(base)
        server.scheme.equals(resolved.scheme, ignoreCase = true) &&
            server.host.equals(resolved.host, ignoreCase = true) &&
            effectivePort(server) == effectivePort(resolved)
    }.getOrDefault(false)

    return CameraStreamRequest(
        url = resolved.toASCIIString(),
        authorization = token?.takeIf { it.isNotBlank() && sameOrigin }?.let { "Bearer $it" },
    )
}

private fun effectivePort(uri: URI): Int = when {
    uri.port >= 0 -> uri.port
    uri.scheme.equals("https", ignoreCase = true) -> 443
    else -> 80
}

@Composable
fun CameraStreamView(
    streamUrl: String?,
    baseUrl: String?,
    token: String?,
    modifier: Modifier = Modifier,
) {
    val request = remember(streamUrl, baseUrl, token) {
        resolveCameraStream(streamUrl, baseUrl, token)
    }
    var failed by remember(request?.url) { mutableStateOf(false) }
    var loading by remember(request?.url) { mutableStateOf(request != null) }

    LaunchedEffect(request?.url) {
        if (request != null) {
            delay(1_500)
            loading = false
        }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (request == null || failed) {
            CameraUnavailable(
                message = if (streamUrl.isNullOrBlank()) {
                    "Sin transmisión vinculada"
                } else {
                    "Transmisión no disponible"
                },
            )
            return@Box
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.BLACK)
                    settings.javaScriptEnabled = false
                    settings.loadsImagesAutomatically = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            loading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame != false) {
                                failed = true
                                loading = false
                            }
                        }
                    }
                    loadUrl(
                        request.url,
                        request.authorization?.let { mapOf("Authorization" to it) }.orEmpty(),
                    )
                }
            },
            update = { webView ->
                if (webView.url != request.url) {
                    failed = false
                    loading = true
                    webView.loadUrl(
                        request.url,
                        request.authorization?.let { mapOf("Authorization" to it) }.orEmpty(),
                    )
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
        )

        if (loading) {
            CircularProgressIndicator(color = NopalGreen)
        }
    }
}

@Composable
private fun CameraUnavailable(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(NopalSurfaceElevated),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.VideocamOff, null, tint = NopalTextSecondary)
            Text(message, color = NopalTextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
