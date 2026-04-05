package com.bongbee.iptv.util

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {
    private val AD_DOMAINS = hashSetOf(
        "chatmate.tv","cherrylive.chat", "doubleclick.net", "googleadservices.com", "googlesyndication.com", "moatads.com",
        "adservice.google.com", "popads.net", "onclickads.net", "propellerads.com",
        "adsystem.com", "adtarget.me", "adthrive.com", "adunit.com", "adzerk.net",
        "amazon-adsystem.com", "adnxs.com", "appnexus.com", "bidswitch.net",
        "casalemedia.com", "criteo.com", "openx.net", "pubmatic.com", "rubiconproject.com",
        "smartadserver.com", "taboola.com", "outbrain.com", "yieldmo.com", "bet365.com",
        "1xbet.com", "melbet.com", "parimatch.com", "mostbet.com", "histats.com",
        "popcash.net", "adsterra.com", "exo-click.com", "exoclick.com", "juicyads.com",
        "ero-advertising.com", "adskeeper.co.uk", "mgid.com", "revcontent.com",
        "popmyads.com", "wigetmedia.com", "vdo.ai", "anyclip.com", "brid.tv",
        "connatix.com", "spotxchange.com", "teads.tv", "tremorhub.com", "unruly.co",
        "facebook.net", "fbcdn.net", "connect.facebook.net", "static.doubleclick.net",
        "securepubads.g.doubleclick.net", "pagead2.googlesyndication.com", "ad.doubleclick.net",
        "google-analytics.com", "ssl.google-analytics.com", "www.google-analytics.com",
        "api.segment.io", "cdn.segment.com", "hotjar.com", "mixpanel.com", "amplitude.com",
        "crazyegg.com", "yandex.ru", "mail.ru", "vk.com", "zapic.top", "jads.co",
        "clonex.xyz", "monetized.com", "a.bestcontentfood.top", "a.shukriya90.com",
        "r.mradx.net", "a.p6m.xyz", "onclick.com", "bestcontentfood", "shukriya",
        "interyield.com", "onclickads", "popunder", "disable-devtool", "msh.vidsrc.me",
        "monetag.com", "clickadu.com", "hilltopads.com", "ad-maven.com",
        "activerevenu.com", "infolinks.com", "yllix.com", "bidvertiser.com",
        "revenuehits.com", "adcash.com", "pactom.com", "starmark.com",
        "media.net", "buyads.com", "adkernel.com", "adreactor.com", "admedia.com",
        "adsupply.com", "ad-center.com", "ad-score.com", "judy.com", "carbonads.net",
        "buysellads.com", "adrecover.com", "adpushup.com", "monetizemore.com",
        "adthrive.com", "mediavine.com", "ezoic.com", "shemedia.com", "playwire.com",
        "antiblock.org", "blockadblock.com", "adblockanalytics.com"
    )

    private val AD_KEYWORDS = listOf(
        "/ads/", "/ad-", "/advert", "popunder", "popup", "banner", "tracking", "pixel", "analytics",
        "ads.js", "prebid", "pro-ads", "dismiss", "close_ad", "onclick", "shukriya", "native-ad",
        "sponsored", "promo", "detection", "debugger", "devtools", "overlay", "modal-open",
        "telemetry", "log-event", "collect-data", "marketing", "promotion", "antiblock",
        "adblock-detection", "detect-adblock", "ad-check", "is-blocked"
    )

    fun isAd(url: String): Boolean {
        val lowUrl = url.lowercase()

        if (lowUrl.contains("cloudflare.com/challenges") || lowUrl.contains("challenges.cloudflare.com")) {
            return false
        }

        if (lowUrl.contains(".m3u8") || lowUrl.contains(".mp4") || lowUrl.contains(".ts") ||
            lowUrl.contains("playlist") || lowUrl.contains("chunk") || lowUrl.contains("manifest") ||
            lowUrl.contains("key") || lowUrl.contains("license") || lowUrl.contains("init-") ||
            lowUrl.contains("fragment") || lowUrl.contains("segment")) {
            return false
        }

        val videoProviders = listOf(
            "vidsrc", "2embed", "embed.su", "multiembed", "autoembed", "moviesapi",
            "vidzee", "vidrock", "vidnest", "riveembed", "smashystream", "111movies",
            "videasy", "vidlink", "vidfast", "primewire", "warezcdn", "superflix",
            "vidup", "vidplay", "filemoon", "rabbitstream", "megacloud", "cloudemb",
            "vizcloud", "cloudnestra", "playm4u", "player", "video"
        )

        if (videoProviders.any { lowUrl.contains(it) }) {
            if (lowUrl.contains("/ads/") || lowUrl.contains("ads.js") ||
                lowUrl.contains("pop.js") || lowUrl.contains("popunder") ||
                lowUrl.contains("onclick") || lowUrl.contains("track")) {
                return true
            }
            return false
        }

        if (AD_DOMAINS.any { lowUrl.contains(it) }) return true
        if (AD_KEYWORDS.any { lowUrl.contains(it) }) return true

        return false
    }

    fun createEmptyResource(): WebResourceResponse {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
    }
}
