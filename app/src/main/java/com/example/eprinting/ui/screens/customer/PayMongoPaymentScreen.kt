package com.example.eprinting.ui.screens.customer

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.*
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import java.net.URLDecoder

@Composable
fun PayMongoPaymentScreen(
    navController: NavController,
    redirectUrl: String
) {
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
    var paymentDetected by remember { mutableStateOf(false) }

    val decodedUrl = remember(redirectUrl) {
        try {
            URLDecoder.decode(redirectUrl, "UTF-8")
        } catch (e: Exception) {
            Log.e("PayMongo", "URL decode error", e)
            redirectUrl
        }
    }

    LaunchedEffect(decodedUrl) {
        Log.d("PayMongo", "Initializing with URL: $decodedUrl")
    }

    // Handle back button
    DisposableEffect(Unit) {
        val callback = object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView?.let {
                    if (it.canGoBack()) {
                        it.goBack()
                    } else {
                        if (!paymentDetected) {
                            navController.popBackStack()
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "payment_cancelled", true
                            )
                        }
                    }
                } ?: run {
                    if (!paymentDetected) {
                        navController.popBackStack()
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            "payment_cancelled", true
                        )
                    }
                }
            }
        }

        backDispatcher?.addCallback(callback)

        onDispose {
            callback.remove()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        defaultTextEncodingName = "utf-8"

                        // Cache settings
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // setAppCacheEnabled is deprecated, use cacheMode instead

                        // Allow redirects
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            Log.d("PayMongo", "Page started: $url")

                            // Log the URL for debugging
                            url?.let { currentUrl ->
                                Log.d("PayMongo", "Current URL on start: $currentUrl")
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false

                            url?.let { currentUrl ->
                                Log.d("PayMongo", "Page finished: $currentUrl")

                                // Get the page title for debugging
                                view?.title?.let { title ->
                                    Log.d("PayMongo", "Page title: $title")
                                }

                                // Inject JavaScript to detect PayMongo's success state
                                view?.evaluateJavascript("""
                                    (function() {
                                        try {
                                            // Check for success text in the page
                                            var bodyText = document.body.innerText.toLowerCase();
                                            var htmlContent = document.documentElement.innerHTML.toLowerCase();
                                            
                                            console.log('Body text contains:', bodyText);
                                            console.log('HTML contains:', htmlContent.substring(0, 500));
                                            
                                            // Check for various success indicators
                                            var successIndicators = [
                                                'payment successful',
                                                'payment completed',
                                                'thank you for your payment',
                                                'you paid',
                                                'successful payment',
                                                'transaction successful',
                                                'payment was successful',
                                                '✓',
                                                'payment succeeded',
                                                'paid successfully'
                                            ];
                                            
                                            var failedIndicators = [
                                                'payment failed',
                                                'transaction failed',
                                                'payment declined',
                                                'card declined',
                                                'unsuccessful',
                                                'error',
                                                'failed to process',
                                                '×',
                                                'try again'
                                            ];
                                            
                                            for (var i = 0; i < successIndicators.length; i++) {
                                                if (bodyText.includes(successIndicators[i]) || 
                                                    htmlContent.includes(successIndicators[i])) {
                                                    return 'success';
                                                }
                                            }
                                            
                                            for (var i = 0; i < failedIndicators.length; i++) {
                                                if (bodyText.includes(failedIndicators[i]) || 
                                                    htmlContent.includes(failedIndicators[i])) {
                                                    return 'failed';
                                                }
                                            }
                                            
                                            return 'unknown';
                                        } catch(e) {
                                            console.error('Error in detection:', e);
                                            return 'error';
                                        }
                                    })();
                                """.trimIndent()) { result ->
                                    Log.d("PayMongo", "JavaScript detection result: $result")

                                    if (result != null && result.contains("success")) {
                                        Log.d("PayMongo", "✅ Payment SUCCESS detected via JavaScript")
                                        paymentDetected = true
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            navController.popBackStack()
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "payment_success", true
                                            )
                                        }, 1500)
                                    } else if (result != null && result.contains("failed")) {
                                        Log.d("PayMongo", "❌ Payment FAILED detected via JavaScript")
                                        paymentDetected = true
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            navController.popBackStack()
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "payment_failed", true
                                            )
                                        }, 1500)
                                    }
                                }

                                // Also check URL patterns (as before)
                                val lowerUrl = currentUrl.lowercase()
                                Log.d("PayMongo", "Checking URL patterns: $lowerUrl")

                                when {
                                    lowerUrl.contains("/success") ||
                                            lowerUrl.contains("success=true") ||
                                            lowerUrl.contains("checkout/success") ||
                                            lowerUrl.contains("thank-you") ||
                                            lowerUrl.contains("payment-success") ||
                                            lowerUrl.contains("status=success") ||
                                            lowerUrl.contains("success/") -> {
                                        Log.d("PayMongo", "✅ Payment SUCCESS detected via URL")
                                        paymentDetected = true
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            navController.popBackStack()
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "payment_success", true
                                            )
                                        }, 1500)
                                    }

                                    lowerUrl.contains("/failed") ||
                                            lowerUrl.contains("success=false") ||
                                            lowerUrl.contains("checkout/failed") ||
                                            lowerUrl.contains("payment-failed") ||
                                            lowerUrl.contains("status=failed") ||
                                            lowerUrl.contains("failed/") -> {
                                        Log.d("PayMongo", "❌ Payment FAILED detected via URL")
                                        paymentDetected = true
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            navController.popBackStack()
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "payment_failed", true
                                            )
                                        }, 1500)
                                    }

                                    lowerUrl.contains("/cancel") ||
                                            lowerUrl.contains("cancelled") ||
                                            lowerUrl.contains("checkout/cancel") -> {
                                        Log.d("PayMongo", "⚠️ Payment CANCELLED detected")
                                        paymentDetected = true
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            navController.popBackStack()
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                "payment_cancelled", true
                                            )
                                        }, 1000)
                                    }
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url.toString()
                            Log.d("PayMongo", "Loading URL: $url")

                            // Handle return URLs from PayMongo
                            if (url.startsWith("http://localhost") ||
                                url.startsWith("https://localhost") ||
                                url.startsWith("myapp://") ||
                                url.startsWith("eprinting://")) {

                                Log.d("PayMongo", "Handling custom URL scheme: $url")

                                when {
                                    url.contains("success", ignoreCase = true) -> {
                                        paymentDetected = true
                                        navController.popBackStack()
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "payment_success", true
                                        )
                                        return true
                                    }
                                    url.contains("failed", ignoreCase = true) -> {
                                        paymentDetected = true
                                        navController.popBackStack()
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "payment_failed", true
                                        )
                                        return true
                                    }
                                    url.contains("cancel", ignoreCase = true) -> {
                                        paymentDetected = true
                                        navController.popBackStack()
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "payment_cancelled", true
                                        )
                                        return true
                                    }
                                }
                            }
                            return false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            Log.e("PayMongo", "WebView Error: ${error?.description}")
                            isLoading = false
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            Log.e("PayMongo", "HTTP Error: ${errorResponse?.statusCode} - ${errorResponse?.reasonPhrase}")
                            isLoading = false
                        }

                        override fun onLoadResource(view: WebView?, url: String?) {
                            super.onLoadResource(view, url)
                            Log.d("PayMongo", "Loading resource: $url")
                        }
                    }

                    // Create a concrete class for JavaScript interface
                    class JavaScriptInterface {
                        @android.webkit.JavascriptInterface
                        fun showToast(message: String) {
                            Log.d("PayMongoJS", "JavaScript message: $message")
                        }

                        @android.webkit.JavascriptInterface
                        fun paymentComplete(status: String) {
                            Log.d("PayMongoJS", "Payment status from JS: $status")
                            Handler(Looper.getMainLooper()).post {
                                paymentDetected = true
                                when (status.lowercase()) {
                                    "success", "paid" -> {
                                        navController.popBackStack()
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "payment_success", true
                                        )
                                    }
                                    "failed", "declined" -> {
                                        navController.popBackStack()
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "payment_failed", true
                                        )
                                    }
                                    else -> {
                                        navController.popBackStack()
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "payment_cancelled", true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add JavaScript interface for better communication
                    addJavascriptInterface(JavaScriptInterface(), "Android")

                    webView = this

                    // Load the URL
                    post {
                        Log.d("PayMongo", "Loading WebView with URL: $decodedUrl")
                        loadUrl(decodedUrl)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // Update if URL changes
                if (view.url != decodedUrl && !paymentDetected) {
                    Log.d("PayMongo", "Reloading WebView with new URL")
                    view.loadUrl(decodedUrl)
                }
            }
        )

        // Add a debug floating action button
        FloatingActionButton(
            onClick = {
                webView?.let { view ->
                    val currentUrl = view.url
                    val title = view.title

                    Log.d("PayMongoDebug", "Current URL: $currentUrl")
                    Log.d("PayMongoDebug", "Page Title: $title")
                    Log.d("PayMongoDebug", "Can go back: ${view.canGoBack()}")

                    // Reload the page
                    view.reload()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                contentDescription = "Refresh"
            )
        }
    }
}