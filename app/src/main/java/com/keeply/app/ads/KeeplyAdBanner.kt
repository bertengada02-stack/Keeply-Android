package com.keeply.app.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.keeply.app.KeeplyApplication
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Google's official Android banner test ad unit. Never replace with an invented/live ID.
private const val GOOGLE_TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

private enum class BannerLoadState { LOADING, LOADED, FAILED }

@Composable
fun KeeplyAdBanner(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as? KeeplyApplication ?: return
    val adsReady by app.consentManager.adsReady.collectAsStateWithLifecycle()
    if (!adsReady) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val adSize = remember(widthDp) {
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        var loadState by remember(adSize) { mutableStateOf(BannerLoadState.LOADING) }
        val adView = remember(context, adSize) {
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = GOOGLE_TEST_BANNER_AD_UNIT_ID
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        loadState = BannerLoadState.LOADED
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        loadState = BannerLoadState.FAILED
                    }
                }
            }
        }

        if (loadState != BannerLoadState.FAILED) {
            DisposableEffect(adView) {
                onDispose { adView.destroy() }
            }
            LaunchedEffect(adView) {
                if (app.consentManager.adsReady.value) {
                    adView.loadAd(AdRequest.Builder().build())
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adSize.height.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { adView },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
