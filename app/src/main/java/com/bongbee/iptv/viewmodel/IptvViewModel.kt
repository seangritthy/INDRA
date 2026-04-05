package com.bongbee.iptv.viewmodel

import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bongbee.iptv.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class IptvViewModel : ViewModel() {
    var appLanguage by mutableStateOf("en")
        private set

    var isDarkMode by mutableStateOf(true)
        private set
        
    var themeSkin by mutableStateOf("Cyber")
        private set

    var backgroundSkin by mutableStateOf("Classic")
        private set

    var showPrivateCategories by mutableStateOf(false)
        private set

    var autoNext by mutableStateOf(false)
        private set

    // Picture-in-Picture State
    var isInPipMode by mutableStateOf(false)
        private set

    fun setIsInPipMode(inPip: Boolean) {
        isInPipMode = inPip
    }

    // ── Stream Status Checking ──────────────────────────────────────────
    val channelStatus = mutableStateMapOf<String, StreamStatus>()
    private val statusSemaphore = Semaphore(8) // max 8 concurrent checks
    var isCheckingAll by mutableStateOf(false)
        private set

    /** Lightweight HTTP check for a single channel — no ExoPlayer needed */
    fun checkChannelStatus(channel: Channel) {
        val key = channel.id + channel.name
        if (channelStatus[key] == StreamStatus.LIVE || channelStatus[key] == StreamStatus.CHECKING) return
        channelStatus[key] = StreamStatus.CHECKING

        viewModelScope.launch(Dispatchers.IO) {
            statusSemaphore.acquire()
            try {
                val isLive = channel.urls.any { url -> probeStreamUrl(url) }
                channelStatus[key] = if (isLive) StreamStatus.LIVE else StreamStatus.ERROR
            } catch (_: Exception) {
                channelStatus[key] = StreamStatus.ERROR
            } finally {
                statusSemaphore.release()
            }
        }
    }

    /** Check all loaded channels at once */
    fun checkAllChannelStatuses() {
        isCheckingAll = true
        val toCheck = channels.toList()
        viewModelScope.launch {
            toCheck.forEach { channel -> checkChannelStatus(channel) }
            // Wait until all finish (simplified: just mark done after launching)
            isCheckingAll = false
        }
    }

    fun clearChannelStatuses() {
        channelStatus.clear()
    }

    private fun probeStreamUrl(url: String): Boolean {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.instanceFollowRedirects = true
            val code = conn.responseCode
            if (code == 200) {
                val buffer = ByteArray(1024)
                val bytesRead = conn.inputStream.use { it.read(buffer) }
                conn.disconnect()
                if (bytesRead <= 0) return false
                // For HLS (.m3u8), verify it contains valid playlist markers
                if (url.contains(".m3u8", ignoreCase = true)) {
                    val content = String(buffer, 0, bytesRead)
                    content.contains("#EXT", ignoreCase = true)
                } else {
                    true
                }
            } else {
                conn.disconnect()
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    // ── MekongTV Fallback Resolver ──────────────────────────────────────
    // Maps known Khmer channel names to mekongtv.net slugs
    private val mekongTvSlugMap = mapOf(
        "TVK" to "tvk",
        "TV5 Cambodia" to "tv5",
        "TV5" to "tv5",
        "Bayon TV" to "bayontv",
        "BayonTV" to "bayontv",
        "BTV News" to "bayontv1",
        "BTV" to "bayontv1",
        "Apsara TV11" to "apsaratv",
        "Apsara TV" to "apsaratv",
        "ApsaraTV" to "apsaratv",
        "Fresh News" to "freshnew",
        "FreshNews" to "freshnew",
        "MekongNet" to "mekongnet",
        "WIKI TV" to "wikitv",
        "WikiTV" to "wikitv",
        "CTV8 HD" to "ctv8",
        "CTV8" to "ctv8",
        "TV3" to "tv3",
        "My TV" to "mytv",
        "MyTV" to "mytv",
        "Hang Meas HDTV" to "hangmeas",
        "Hang Meas" to "hangmeas",
        "Rasmey Hang Meas" to "rhm",
        "CTN" to "ctn",
        "CNC" to "cnc",
        "PNN" to "pnn",
        "SEATV" to "seatv",
        "SEA TV" to "seatv",
        "Town TV" to "towntv",
        "TownTV" to "towntv",
        "NTV" to "ntv",
        "TV9" to "tv9",
        "Komsan TV" to "komsantv",
        "Nice TV" to "nicetv",
        "NiceTV" to "nicetv",
        "EAC News" to "eacnews",
    )

    /**
     * Resolve a fresh MekongTV stream URL for a given channel name.
     * MekongTV uses expiring auth tokens so we must fetch at play-time.
     */
    suspend fun resolveMekongTvUrl(channelName: String): String? = withContext(Dispatchers.IO) {
        // Find matching slug (fuzzy match)
        val slug = mekongTvSlugMap.entries.firstOrNull { (key, _) ->
            channelName.contains(key, ignoreCase = true) || key.contains(channelName, ignoreCase = true)
        }?.value ?: return@withContext null

        try {
            val pageUrl = "https://mekongtv.net/channels/$slug"
            val conn = URL(pageUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            // Extract stream URL: src="https://stream.mekongtv.net/..."
            val regex = """src="(https://stream\.mekongtv\.net/[^"]+)"""".toRegex()
            val match = regex.find(html)
            val streamUrl = match?.groupValues?.get(1)
                ?.replace("&amp;", "&") // decode HTML entities

            streamUrl
        } catch (e: Exception) {
            android.util.Log.w("IptvVM", "MekongTV resolve failed for $slug: ${e.message}")
            null
        }
    }

    val categories = listOf(
        Category("General", 2371, "https://iptv-org.github.io/iptv/categories/general.m3u", videoResName = "cat_general"),
        Category("Entertainment", 642, "https://iptv-org.github.io/iptv/categories/entertainment.m3u", videoResName = "cat_entertainment"),
        Category("Movies", 402, "https://iptv-org.github.io/iptv/categories/movies.m3u", videoResName = "cat_movies"),
        Category("News", 898, "https://iptv-org.github.io/iptv/categories/news.m3u", videoResName = "cat_news"),
        Category("Sports", 299, "https://iptv-org.github.io/iptv/categories/sports.m3u", videoResName = "cat_sports"),
        Category("Music", 619, "https://iptv-org.github.io/iptv/categories/music.m3u", videoResName = "cat_music"),
        Category("Kids", 225, "https://iptv-org.github.io/iptv/categories/kids.m3u", videoResName = "cat_kids"),
        Category("Animation", 68, "https://iptv-org.github.io/iptv/categories/animation.m3u", videoResName = "cat_animation"),
        Category("Documentary", 120, "https://iptv-org.github.io/iptv/categories/documentary.m3u", videoResName = "cat_documentary"),
        Category("Education", 185, "https://iptv-org.github.io/iptv/categories/education.m3u", videoResName = "cat_education"),
        Category("Series", 181, "https://iptv-org.github.io/iptv/categories/series.m3u", videoResName = "cat_series"),
        Category("Business", 70, "https://iptv-org.github.io/iptv/categories/business.m3u"),
        Category("Classic", 49, "https://iptv-org.github.io/iptv/categories/classic.m3u"),
        Category("Comedy", 63, "https://iptv-org.github.io/iptv/categories/comedy.m3u"),
        Category("Cooking", 33, "https://iptv-org.github.io/iptv/categories/cooking.m3u"),
        Category("Culture", 165, "https://iptv-org.github.io/iptv/categories/culture.m3u"),
        Category("Family", 52, "https://iptv-org.github.io/iptv/categories/family.m3u"),
        Category("Lifestyle", 95, "https://iptv-org.github.io/iptv/categories/lifestyle.m3u"),
        Category("Outdoor", 51, "https://iptv-org.github.io/iptv/categories/outdoor.m3u"),
        Category("Public", 41, "https://iptv-org.github.io/iptv/categories/public.m3u"),
        Category("Religious", 694, "https://iptv-org.github.io/iptv/categories/religious.m3u"),
        Category("Science", 19, "https://iptv-org.github.io/iptv/categories/science.m3u"),
        Category("Travel", 44, "https://iptv-org.github.io/iptv/categories/travel.m3u"),
        Category("Weather", 16, "https://iptv-org.github.io/iptv/categories/weather.m3u"),
        Category("Auto", 18, "https://iptv-org.github.io/iptv/categories/auto.m3u"),
        Category("Shop", 79, "https://iptv-org.github.io/iptv/categories/shop.m3u"),
        Category("Relax", 6, "https://iptv-org.github.io/iptv/categories/relax.m3u"),
        Category("Interactive", 1, "https://iptv-org.github.io/iptv/categories/interactive.m3u"),
        Category("Undefined", 4715, "https://iptv-org.github.io/iptv/categories/undefined.m3u")
    )

    val privateCategories = listOf(
        Category("🔞 Private (xvideos.com)", 0, "https://xvideos.com", isWeb = true)
    )

    val countries = listOf(
        Country("🇦🇫 Afghanistan", "https://iptv-org.github.io/iptv/countries/af.m3u"),
        Country("Albania", "https://iptv-org.github.io/iptv/countries/al.m3u"),
        Country("Algeria", "https://iptv-org.github.io/iptv/countries/dz.m3u"),
        Country("Andorra", "https://iptv-org.github.io/iptv/countries/ad.m3u"),
        Country("Angola", "https://iptv-org.github.io/iptv/countries/ao.m3u"),
        Country("Argentina", "https://iptv-org.github.io/iptv/countries/ar.m3u"),
        Country("Armenia", "https://iptv-org.github.io/iptv/countries/am.m3u"),
        Country("Aruba", "https://iptv-org.github.io/iptv/countries/aw.m3u"),
        Country("Australia", "https://iptv-org.github.io/iptv/countries/au.m3u"),
        Country("Austria", "https://iptv-org.github.io/iptv/countries/at.m3u"),
        Country("Azerbaijan", "https://iptv-org.github.io/iptv/countries/az.m3u"),
        Country("Bahamas", "https://iptv-org.github.io/iptv/countries/bs.m3u"),
        Country("Bahrain", "https://iptv-org.github.io/iptv/countries/bh.m3u"),
        Country("Bangladesh", "https://iptv-org.github.io/iptv/countries/bd.m3u"),
        Country("Barbados", "https://iptv-org.github.io/iptv/countries/bb.m3u"),
        Country("Belarus", "https://iptv-org.github.io/iptv/countries/by.m3u"),
        Country("Belgium", "https://iptv-org.github.io/iptv/countries/be.m3u"),
        Country("Belize", "https://iptv-org.github.io/iptv/countries/bz.m3u"),
        Country("Benin", "https://iptv-org.github.io/iptv/countries/bj.m3u"),
        Country("Bhutan", "https://iptv-org.github.io/iptv/countries/bt.m3u"),
        Country("Bolivia", "https://iptv-org.github.io/iptv/countries/bo.m3u"),
        Country("Bonaire", "https://iptv-org.github.io/iptv/countries/bq.m3u"),
        Country("Bosnia and Herzegovina", "https://iptv-org.github.io/iptv/countries/ba.m3u"),
        Country("Brazil", "https://iptv-org.github.io/iptv/countries/br.m3u"),
        Country("British Virgin Islands", "https://iptv-org.github.io/iptv/countries/vg.m3u"),
        Country("Brunei", "https://iptv-org.github.io/iptv/countries/bn.m3u"),
        Country("Bulgaria", "https://iptv-org.github.io/iptv/countries/bg.m3u"),
        Country("Burkina Faso", "https://iptv-org.github.io/iptv/countries/bf.m3u"),
        Country("Cambodia", "https://iptv-org.github.io/iptv/countries/kh.m3u"),
        Country("Cameroon", "https://iptv-org.github.io/iptv/countries/cm.m3u"),
        Country("Canada", "https://iptv-org.github.io/iptv/countries/ca.m3u"),
        Country("Cape Verde", "https://iptv-org.github.io/iptv/countries/cv.m3u"),
        Country("Chad", "https://iptv-org.github.io/iptv/countries/td.m3u"),
        Country("Chile", "https://iptv-org.github.io/iptv/countries/cl.m3u"),
        Country("China", "https://iptv-org.github.io/iptv/countries/cn.m3u"),
        Country("Colombia", "https://iptv-org.github.io/iptv/countries/co.m3u"),
        Country("Comoros", "https://iptv-org.github.io/iptv/countries/km.m3u"),
        Country("Costa Rica", "https://iptv-org.github.io/iptv/countries/cr.m3u"),
        Country("Croatia", "https://iptv-org.github.io/iptv/countries/hr.m3u"),
        Country("Cuba", "https://iptv-org.github.io/iptv/countries/cu.m3u"),
        Country("Curacao", "https://iptv-org.github.io/iptv/countries/cw.m3u"),
        Country("Cyprus", "https://iptv-org.github.io/iptv/countries/cy.m3u"),
        Country("Czech Republic", "https://iptv-org.github.io/iptv/countries/cz.m3u"),
        Country("Democratic Republic of the Congo", "https://iptv-org.github.io/iptv/countries/cd.m3u"),
        Country("Denmark", "https://iptv-org.github.io/iptv/countries/dk.m3u"),
        Country("Djibouti", "https://iptv-org.github.io/iptv/countries/dj.m3u"),
        Country("Dominican Republic", "https://iptv-org.github.io/iptv/countries/do.m3u"),
        Country("Ecuador", "https://iptv-org.github.io/iptv/countries/ec.m3u"),
        Country("Egypt", "https://iptv-org.github.io/iptv/countries/eg.m3u"),
        Country("El Salvador", "https://iptv-org.github.io/iptv/countries/sv.m3u"),
        Country("Equatorial Guinea", "https://iptv-org.github.io/iptv/countries/gq.m3u"),
        Country("Eritrea", "https://iptv-org.github.io/iptv/countries/er.m3u"),
        Country("Estonia", "https://iptv-org.github.io/iptv/countries/ee.m3u"),
        Country("Ethiopia", "https://iptv-org.github.io/iptv/countries/et.m3u"),
        Country("Faroe Islands", "https://iptv-org.github.io/iptv/countries/fo.m3u"),
        Country("Finland", "https://iptv-org.github.io/iptv/countries/fi.m3u"),
        Country("France", "https://iptv-org.github.io/iptv/countries/fr.m3u"),
        Country("French Guiana", "https://iptv-org.github.io/iptv/countries/gf.m3u"),
        Country("French Polynesia", "https://iptv-org.github.io/iptv/countries/pf.m3u"),
        Country("Gambia", "https://iptv-org.github.io/iptv/countries/gm.m3u"),
        Country("Georgia", "https://iptv-org.github.io/iptv/countries/ge.m3u"),
        Country("Germany", "https://iptv-org.github.io/iptv/countries/de.m3u"),
        Country("Ghana", "https://iptv-org.github.io/iptv/countries/gh.m3u"),
        Country("Greece", "https://iptv-org.github.io/iptv/countries/gr.m3u"),
        Country("Guadeloupe", "https://iptv-org.github.io/iptv/countries/gp.m3u"),
        Country("Guam", "https://iptv-org.github.io/iptv/countries/gu.m3u"),
        Country("Guatemala", "https://iptv-org.github.io/iptv/countries/gt.m3u"),
        Country("Guernsey", "https://iptv-org.github.io/iptv/countries/gg.m3u"),
        Country("Guinea", "https://iptv-org.github.io/iptv/countries/gn.m3u"),
        Country("Guyana", "https://iptv-org.github.io/iptv/countries/gy.m3u"),
        Country("Haiti", "https://iptv-org.github.io/iptv/countries/ht.m3u"),
        Country("Honduras", "https://iptv-org.github.io/iptv/countries/hn.m3u"),
        Country("Hong Kong", "https://iptv-org.github.io/iptv/countries/hk.m3u"),
        Country("Hungary", "https://iptv-org.github.io/iptv/countries/hu.m3u"),
        Country("Iceland", "https://iptv-org.github.io/iptv/countries/is.m3u"),
        Country("India", "https://iptv-org.github.io/iptv/countries/in.m3u"),
        Country("Indonesia", "https://iptv-org.github.io/iptv/countries/id.m3u"),
        Country("Iran", "https://iptv-org.github.io/iptv/countries/ir.m3u"),
        Country("Iraq", "https://iptv-org.github.io/iptv/countries/iq.m3u"),
        Country("Ireland", "https://iptv-org.github.io/iptv/countries/ie.m3u"),
        Country("Israel", "https://iptv-org.github.io/iptv/countries/il.m3u"),
        Country("Italy", "https://iptv-org.github.io/iptv/countries/it.m3u"),
        Country("Ivory Coast", "https://iptv-org.github.io/iptv/countries/ci.m3u"),
        Country("Jamaica", "https://iptv-org.github.io/iptv/countries/jm.m3u"),
        Country("Japan", "https://iptv-org.github.io/iptv/countries/jp.m3u"),
        Country("Jordan", "https://iptv-org.github.io/iptv/countries/jo.m3u"),
        Country("Kazakhstan", "https://iptv-org.github.io/iptv/countries/kz.m3u"),
        Country("Kenya", "https://iptv-org.github.io/iptv/countries/ke.m3u"),
        Country("Kosovo", "https://iptv-org.github.io/iptv/countries/xk.m3u"),
        Country("Kuwait", "https://iptv-org.github.io/iptv/countries/kw.m3u"),
        Country("Kyrgyzstan", "https://iptv-org.github.io/iptv/countries/kg.m3u"),
        Country("Laos", "https://iptv-org.github.io/iptv/countries/la.m3u"),
        Country("Latvia", "https://iptv-org.github.io/iptv/countries/lv.m3u"),
        Country("Lebanon", "https://iptv-org.github.io/iptv/countries/lb.m3u"),
        Country("Libya", "https://iptv-org.github.io/iptv/countries/ly.m3u"),
        Country("Liechtenstein", "https://iptv-org.github.io/iptv/countries/li.m3u"),
        Country("Lithuania", "https://iptv-org.github.io/iptv/countries/lt.m3u"),
        Country("Luxembourg", "https://iptv-org.github.io/iptv/countries/lu.m3u"),
        Country("Macao", "https://iptv-org.github.io/iptv/countries/mo.m3u"),
        Country("Madagascar", "https://iptv-org.github.io/iptv/countries/mg.m3u"),
        Country("Malaysia", "https://iptv-org.github.io/iptv/countries/my.m3u"),
        Country("Maldives", "https://iptv-org.github.io/iptv/countries/mv.m3u"),
        Country("Mali", "https://iptv-org.github.io/iptv/countries/ml.m3u"),
        Country("Malta", "https://iptv-org.github.io/iptv/countries/mt.m3u"),
        Country("Martinique", "https://iptv-org.github.io/iptv/countries/mq.m3u"),
        Country("Mauritania", "https://iptv-org.github.io/iptv/countries/mr.m3u"),
        Country("Mauritius", "https://iptv-org.github.io/iptv/countries/mu.m3u"),
        Country("Mexico", "https://iptv-org.github.io/iptv/countries/mx.m3u"),
        Country("Moldova", "https://iptv-org.github.io/iptv/countries/md.m3u"),
        Country("Monaco", "https://iptv-org.github.io/iptv/countries/mc.m3u"),
        Country("Mongolia", "https://iptv-org.github.io/iptv/countries/mn.m3u"),
        Country("Montenegro", "https://iptv-org.github.io/iptv/countries/me.m3u"),
        Country("Morocco", "https://iptv-org.github.io/iptv/countries/ma.m3u"),
        Country("Mozambique", "https://iptv-org.github.io/iptv/countries/mz.m3u"),
        Country("Myanmar", "https://iptv-org.github.io/iptv/countries/mm.m3u"),
        Country("Namibia", "https://iptv-org.github.io/iptv/countries/na.m3u"),
        Country("Nepal", "https://iptv-org.github.io/iptv/countries/np.m3u"),
        Country("Netherlands", "https://iptv-org.github.io/iptv/countries/nl.m3u"),
        Country("New Zealand", "https://iptv-org.github.io/iptv/countries/nz.m3u"),
        Country("Nicaragua", "https://iptv-org.github.io/iptv/countries/ni.m3u"),
        Country("Niger", "https://iptv-org.github.io/iptv/countries/ne.m3u"),
        Country("Nigeria", "https://iptv-org.github.io/iptv/countries/ng.m3u"),
        Country("North Korea", "https://iptv-org.github.io/iptv/countries/kp.m3u"),
        Country("North Macedonia", "https://iptv-org.github.io/iptv/countries/mk.m3u"),
        Country("Norway", "https://iptv-org.github.io/iptv/countries/no.m3u"),
        Country("Oman", "https://iptv-org.github.io/iptv/countries/om.m3u"),
        Country("Pakistan", "https://iptv-org.github.io/iptv/countries/pk.m3u"),
        Country("Palestine", "https://iptv-org.github.io/iptv/countries/ps.m3u"),
        Country("Panama", "https://iptv-org.github.io/iptv/countries/pa.m3u"),
        Country("Papua New Guinea", "https://iptv-org.github.io/iptv/countries/pg.m3u"),
        Country("Paraguay", "https://iptv-org.github.io/iptv/countries/py.m3u"),
        Country("Peru", "https://iptv-org.github.io/iptv/countries/pe.m3u"),
        Country("Philippines", "https://iptv-org.github.io/iptv/countries/ph.m3u"),
        Country("Poland", "https://iptv-org.github.io/iptv/countries/pl.m3u"),
        Country("Portugal", "https://iptv-org.github.io/iptv/countries/pt.m3u"),
        Country("Puerto Rico", "https://iptv-org.github.io/iptv/countries/pr.m3u"),
        Country("Qatar", "https://iptv-org.github.io/iptv/countries/qa.m3u"),
        Country("Republic of the Congo", "https://iptv-org.github.io/iptv/countries/cg.m3u"),
        Country("Reunion", "https://iptv-org.github.io/iptv/countries/re.m3u"),
        Country("Romania", "https://iptv-org.github.io/iptv/countries/ro.m3u"),
        Country("Russia", "https://iptv-org.github.io/iptv/countries/ru.m3u"),
        Country("Rwanda", "https://iptv-org.github.io/iptv/countries/rw.m3u"),
        Country("Saint Kitts and Nevis", "https://iptv-org.github.io/iptv/countries/kn.m3u"),
        Country("Saint Lucia", "https://iptv-org.github.io/iptv/countries/lc.m3u"),
        Country("Samoa", "https://iptv-org.github.io/iptv/countries/ws.m3u"),
        Country("San Marino", "https://iptv-org.github.io/iptv/countries/sm.m3u"),
        Country("Saudi Arabia", "https://iptv-org.github.io/iptv/countries/sa.m3u"),
        Country("Senegal", "https://iptv-org.github.io/iptv/countries/sn.m3u"),
        Country("Serbia", "https://iptv-org.github.io/iptv/countries/rs.m3u"),
        Country("Singapore", "https://iptv-org.github.io/iptv/countries/sg.m3u"),
        Country("Sint Maarten", "https://iptv-org.github.io/iptv/countries/sx.m3u"),
        Country("Slovakia", "https://iptv-org.github.io/iptv/countries/sk.m3u"),
        Country("Slovenia", "https://iptv-org.github.io/iptv/countries/si.m3u"),
        Country("Somalia", "https://iptv-org.github.io/iptv/countries/so.m3u"),
        Country("South Africa", "https://iptv-org.github.io/iptv/countries/za.m3u"),
        Country("South Korea", "https://iptv-org.github.io/iptv/countries/kr.m3u"),
        Country("Spain", "https://iptv-org.github.io/iptv/countries/es.m3u"),
        Country("Sri Lanka", "https://iptv-org.github.io/iptv/countries/lk.m3u"),
        Country("Sudan", "https://iptv-org.github.io/iptv/countries/sd.m3u"),
        Country("Suriname", "https://iptv-org.github.io/iptv/countries/sr.m3u"),
        Country("Sweden", "https://iptv-org.github.io/iptv/countries/se.m3u"),
        Country("Switzerland", "https://iptv-org.github.io/iptv/countries/ch.m3u"),
        Country("Syria", "https://iptv-org.github.io/iptv/countries/sy.m3u"),
        Country("Taiwan", "https://iptv-org.github.io/iptv/countries/tw.m3u"),
        Country("Tajikistan", "https://iptv-org.github.io/iptv/countries/tj.m3u"),
        Country("Tanzania", "https://iptv-org.github.io/iptv/countries/tz.m3u"),
        Country("Thailand", "https://iptv-org.github.io/iptv/countries/th.m3u"),
        Country("Togo", "https://iptv-org.github.io/iptv/countries/tg.m3u"),
        Country("Trinidad and Tobago", "https://iptv-org.github.io/iptv/countries/tt.m3u"),
        Country("Tunisia", "https://iptv-org.github.io/iptv/countries/tn.m3u"),
        Country("Turkiye", "https://iptv-org.github.io/iptv/countries/tr.m3u"),
        Country("Turkmenistan", "https://iptv-org.github.io/iptv/countries/tm.m3u"),
        Country("Uganda", "https://iptv-org.github.io/iptv/countries/ug.m3u"),
        Country("Ukraine", "https://iptv-org.github.io/iptv/countries/ua.m3u"),
        Country("United Arab Emirates", "https://iptv-org.github.io/iptv/countries/ae.m3u"),
        Country("United Kingdom", "https://iptv-org.github.io/iptv/countries/uk.m3u"),
        Country("United States", "https://iptv-org.github.io/iptv/countries/us.m3u"),
        Country("Uruguay", "https://iptv-org.github.io/iptv/countries/uy.m3u"),
        Country("Uzbekistan", "https://iptv-org.github.io/iptv/countries/uz.m3u"),
        Country("Vatican City", "https://iptv-org.github.io/iptv/countries/va.m3u"),
        Country("Venezuela", "https://iptv-org.github.io/iptv/countries/ve.m3u"),
        Country("Vietnam", "https://iptv-org.github.io/iptv/countries/vn.m3u"),
        Country("Western Sahara", "https://iptv-org.github.io/iptv/countries/eh.m3u"),
        Country("Yemen", "https://iptv-org.github.io/iptv/countries/ye.m3u"),
        Country("Zimbabwe", "https://iptv-org.github.io/iptv/countries/zw.m3u"),
        Country("International", "https://iptv-org.github.io/iptv/countries/int.m3u"),
        Country("Undefined", "https://iptv-org.github.io/iptv/countries/undefined.m3u")
    )

    val regions = listOf(
        Country("Africa", "https://iptv-org.github.io/iptv/regions/afr.m3u"),
        Country("Americas", "https://iptv-org.github.io/iptv/regions/amer.m3u"),
        Country("Arab world", "https://iptv-org.github.io/iptv/regions/arab.m3u"),
        Country("Asia", "https://iptv-org.github.io/iptv/regions/asia.m3u"),
        Country("Asia-Pacific", "https://iptv-org.github.io/iptv/regions/apac.m3u"),
        Country("Association of Southeast Asian Nations", "https://iptv-org.github.io/iptv/regions/asean.m3u"),
        Country("Balkan", "https://iptv-org.github.io/iptv/regions/balkan.m3u"),
        Country("Benelux", "https://iptv-org.github.io/iptv/regions/benelux.m3u"),
        Country("Caribbean", "https://iptv-org.github.io/iptv/regions/carib.m3u"),
        Country("Central America", "https://iptv-org.github.io/iptv/regions/cenamer.m3u"),
        Country("Central and Eastern Europe", "https://iptv-org.github.io/iptv/regions/cee.m3u"),
        Country("Central Asia", "https://iptv-org.github.io/iptv/regions/cas.m3u"),
        Country("Central Europe", "https://iptv-org.github.io/iptv/regions/ceu.m3u"),
        Country("Commonwealth of Independent States", "https://iptv-org.github.io/iptv/regions/cis.m3u"),
        Country("East Africa", "https://iptv-org.github.io/iptv/regions/eaf.m3u"),
        Country("East Asia", "https://iptv-org.github.io/iptv/regions/eas.m3u"),
        Country("Europe", "https://iptv-org.github.io/iptv/regions/eur.m3u"),
        Country("Europe, the Middle East and Africa", "https://iptv-org.github.io/iptv/regions/emea.m3u"),
        Country("European Union", "https://iptv-org.github.io/iptv/regions/eu.m3u"),
        Country("Gulf Cooperation Council", "https://iptv-org.github.io/iptv/regions/gcc.m3u"),
        Country("Hispanic America", "https://iptv-org.github.io/iptv/regions/hispam.m3u"),
        Country("Latin America", "https://iptv-org.github.io/iptv/regions/latam.m3u"),
        Country("Latin America and the Caribbean", "https://iptv-org.github.io/iptv/regions/lac.m3u"),
        Country("Maghreb", "https://iptv-org.github.io/iptv/regions/maghreb.m3u"),
        Country("Middle East", "https://iptv-org.github.io/iptv/regions/mideast.m3u"),
        Country("Middle East and North Africa", "https://iptv-org.github.io/iptv/regions/mena.m3u"),
        Country("Nordics", "https://iptv-org.github.io/iptv/regions/nord.m3u"),
        Country("North America", "https://iptv-org.github.io/iptv/regions/noram.m3u"),
        Country("Northern America", "https://iptv-org.github.io/iptv/regions/nam.m3u"),
        Country("Northern Europe", "https://iptv-org.github.io/iptv/regions/neu.m3u"),
        Country("Oceania", "https://iptv-org.github.io/iptv/regions/oce.m3u"),
        Country("South America", "https://iptv-org.github.io/iptv/regions/southam.m3u"),
        Country("South Asia", "https://iptv-org.github.io/iptv/regions/sas.m3u"),
        Country("Southeast Asia", "https://iptv-org.github.io/iptv/regions/sea.m3u"),
        Country("Southern Africa", "https://iptv-org.github.io/iptv/regions/saf.m3u"),
        Country("Southern Europe", "https://iptv-org.github.io/iptv/regions/ser.m3u"),
        Country("Sub-Saharan Africa", "https://iptv-org.github.io/iptv/regions/ssa.m3u"),
        Country("United Nations", "https://iptv-org.github.io/iptv/regions/un.m3u"),
        Country("West Africa", "https://iptv-org.github.io/iptv/regions/waf.m3u"),
        Country("West Asia", "https://iptv-org.github.io/iptv/regions/was.m3u"),
        Country("Western Europe", "https://iptv-org.github.io/iptv/regions/wer.m3u"),
        Country("Worldwide", "https://iptv-org.github.io/iptv/regions/ww.m3u")
    )

    val languages = listOf(
        Country("Khmer", "https://iptv-org.github.io/iptv/languages/khm.m3u"),
        Country("English", "https://iptv-org.github.io/iptv/languages/eng.m3u"),
        Country("French", "https://iptv-org.github.io/iptv/languages/fra.m3u"),
        Country("Spanish", "https://iptv-org.github.io/iptv/languages/spa.m3u"),
        Country("German", "https://iptv-org.github.io/iptv/languages/deu.m3u"),
        Country("Italian", "https://iptv-org.github.io/iptv/languages/ita.m3u"),
        Country("Japanese", "https://iptv-org.github.io/iptv/languages/jpn.m3u"),
        Country("Korean", "https://iptv-org.github.io/iptv/languages/kor.m3u"),
        Country("Russian", "https://iptv-org.github.io/iptv/languages/rus.m3u"),
        Country("Arabic", "https://iptv-org.github.io/iptv/languages/ara.m3u"),
        Country("Hindi", "https://iptv-org.github.io/iptv/languages/hin.m3u"),
        Country("Thai", "https://iptv-org.github.io/iptv/languages/tha.m3u"),
        Country("Chinese", "https://iptv-org.github.io/iptv/languages/zho.m3u")
    )

    var channels by mutableStateOf<List<Channel>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var currentChannel by mutableStateOf<Channel?>(null)
        private set

    var currentWebUrl by mutableStateOf<String?>(null)
        private set
    var currentWebTitle by mutableStateOf<String?>(null)
        private set
    var isCurrentWebAiHub by mutableStateOf(false)
        private set

    var apiChannels by mutableStateOf<List<ApiChannel>>(emptyList())
        private set
    var apiGuides by mutableStateOf<List<ApiGuide>>(emptyList())
        private set

    val webViewCache = mutableMapOf<String, WebView>()

    // TMDB Movie Hub State
    private val tmdbApiKey = com.bongbee.iptv.BuildConfig.TMDB_API_KEY
    var movieHubMovies by mutableStateOf<List<Movie>>(emptyList())
        private set
    var isMovieHubLoading by mutableStateOf(false)
        private set
    var movieHubPage by mutableIntStateOf(1)
        private set
    var movieHubTotalPages by mutableIntStateOf(1)
        private set
    var movieHubTotalResults by mutableIntStateOf(0)
        private set
    var movieHubSearchQuery by mutableStateOf("")
        private set

    // Per-section pagination states
    var trendingMoviesPage by mutableIntStateOf(1)
    var popularMoviesPage by mutableIntStateOf(1)
    var topRatedMoviesPage by mutableIntStateOf(1)
    var upcomingMoviesPage by mutableIntStateOf(1)
    var trendingTvPage by mutableIntStateOf(1)
    var popularTvPage by mutableIntStateOf(1)
    var topRatedTvPage by mutableIntStateOf(1)
    var onTheAirPage by mutableIntStateOf(1)

    // Per-section total counts
    var trendingMoviesTotal by mutableIntStateOf(0)
    var popularMoviesTotal by mutableIntStateOf(0)
    var topRatedMoviesTotal by mutableIntStateOf(0)
    var upcomingMoviesTotal by mutableIntStateOf(0)
    var trendingTvTotal by mutableIntStateOf(0)
    var popularTvTotal by mutableIntStateOf(0)
    var topRatedTvTotal by mutableIntStateOf(0)
    var onTheAirTotal by mutableIntStateOf(0)

    // ── Watchlist ───────────────────────────────────────────────────────
    var watchlistMovies by mutableStateOf<List<Movie>>(emptyList())
        private set

    fun addToWatchlist(movie: Movie) {
        if (!isInWatchlist(movie.id, movie.mediaType)) {
            watchlistMovies = watchlistMovies + movie
        }
    }

    fun removeFromWatchlist(movieId: Int, mediaType: String) {
        watchlistMovies = watchlistMovies.filter { it.id != movieId || it.mediaType != mediaType }
    }

    fun isInWatchlist(movieId: Int, mediaType: String): Boolean =
        watchlistMovies.any { it.id == movieId && it.mediaType == mediaType }

    fun toggleWatchlist(movie: Movie) {
        if (isInWatchlist(movie.id, movie.mediaType)) {
            removeFromWatchlist(movie.id, movie.mediaType)
        } else {
            addToWatchlist(movie)
        }
    }

    // Additional Movie Categories
    var popularMovies by mutableStateOf<List<Movie>>(emptyList())
        private set
    var topRatedMovies by mutableStateOf<List<Movie>>(emptyList())
        private set
    var upcomingMovies by mutableStateOf<List<Movie>>(emptyList())
        private set
    var trendingMovies by mutableStateOf<List<Movie>>(emptyList())
        private set

    // TV Show Categories
    var popularTvShows by mutableStateOf<List<Movie>>(emptyList())
        private set
    var topRatedTvShows by mutableStateOf<List<Movie>>(emptyList())
        private set
    var trendingTvShows by mutableStateOf<List<Movie>>(emptyList())
        private set
    var onTheAirTvShows by mutableStateOf<List<Movie>>(emptyList())
        private set

    init {
        fetchApiData()
        fetchMovieHubMovies()
        fetchMovieCategories()
        fetchTvShowCategories()
    }

    private fun fetchApiData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val channelsJson = URL("https://iptv-org.github.io/api/channels.json").readText()
                val channelsArray = JSONArray(channelsJson)
                val channelsList = mutableListOf<ApiChannel>()
                for (i in 0 until channelsArray.length()) {
                    val obj = channelsArray.getJSONObject(i)
                    val categoriesArray = obj.optJSONArray("categories")
                    val cats = mutableListOf<String>()
                    if (categoriesArray != null) {
                        for (j in 0 until categoriesArray.length()) {
                            cats.add(categoriesArray.getString(j))
                        }
                    }
                    channelsList.add(ApiChannel(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        network = obj.optString("network"),
                        country = obj.optString("country"),
                        website = obj.optString("website"),
                        categories = cats
                    ))
                }
                apiChannels = channelsList

                val guidesJson = URL("https://iptv-org.github.io/api/guides.json").readText()
                val guidesArray = JSONArray(guidesJson)
                val guidesList = mutableListOf<ApiGuide>()
                for (i in 0 until guidesArray.length()) {
                    val obj = guidesArray.getJSONObject(i)
                    guidesList.add(ApiGuide(
                        channel = obj.optString("channel"),
                        site = obj.optString("site"),
                        site_name = obj.optString("site_name"),
                        lang = obj.optString("lang")
                    ))
                }
                apiGuides = guidesList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchMovieHubMovies(searchQuery: String = "", page: Int = 1) {
        viewModelScope.launch {
            isMovieHubLoading = true
            movieHubSearchQuery = searchQuery
            movieHubPage = page
            try {
                val result = withContext(Dispatchers.IO) {
                    val movies = mutableListOf<Movie>()
                    val baseUrl = if (searchQuery.isEmpty()) {
                        "https://api.themoviedb.org/3/discover/movie"
                    } else {
                        // Use MULTI SEARCH to find both movies and TV shows and persons
                        "https://api.themoviedb.org/3/search/multi"
                    }
                    
                    var totalResults = 0
                    
                    // Fetch up to 5 TMDB pages to get enough content
                    for (i in 1..5) {
                        val tmdbPage = (page - 1) * 5 + i
                        val urlString = "$baseUrl?api_key=$tmdbApiKey&page=$tmdbPage" + 
                            if (searchQuery.isNotEmpty()) "&query=${java.net.URLEncoder.encode(searchQuery, "UTF-8")}" else ""
                        
                        try {
                            val response = URL(urlString).readText()
                            val json = JSONObject(response)
                            val resultsArray = json.getJSONArray("results")
                            totalResults = json.optInt("total_results", 0)
                            
                            for (j in 0 until resultsArray.length()) {
                                val obj = resultsArray.getJSONObject(j)
                                val mediaType = obj.optString("media_type", "movie")
                                
                                if (mediaType == "person") {
                                    // If it's a person, add their known_for movies
                                    val knownFor = obj.optJSONArray("known_for")
                                    if (knownFor != null) {
                                        for (k in 0 until knownFor.length()) {
                                            val item = knownFor.getJSONObject(k)
                                            movies.add(Movie(
                                                id = item.getInt("id"),
                                                title = item.optString("title", item.optString("name")),
                                                posterPath = item.optString("poster_path").takeIf { it.isNotEmpty() && it != "null" },
                                                releaseDate = item.optString("release_date", item.optString("first_air_date")).takeIf { it.isNotEmpty() && it != "null" },
                                                mediaType = item.optString("media_type", "movie")
                                            ))
                                        }
                                    }
                                } else if (mediaType == "movie" || mediaType == "tv") {
                                    movies.add(Movie(
                                        id = obj.getInt("id"),
                                        title = obj.optString("title", obj.optString("name")),
                                        posterPath = obj.optString("poster_path").takeIf { it.isNotEmpty() && it != "null" },
                                        releaseDate = obj.optString("release_date", obj.optString("first_air_date")).takeIf { it.isNotEmpty() && it != "null" },
                                        mediaType = mediaType
                                    ))
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                        
                        if (movies.size >= 100) break
                    }
                    // Filter duplicates by ID
                    val uniqueMovies = movies.distinctBy { it.id }
                    
                    // Filter out movies that are already in categorization sections
                    val existingIds = (popularMovies + topRatedMovies + upcomingMovies + trendingMovies +
                                      popularTvShows + topRatedTvShows + trendingTvShows + onTheAirTvShows).map { it.id }.toSet()
                    
                    val filteredMovies = if (searchQuery.isEmpty()) {
                        uniqueMovies.filter { it.id !in existingIds }
                    } else {
                        uniqueMovies
                    }
                    
                    Triple(filteredMovies.take(100), totalResults, (totalResults + 99) / 100)
                }
                movieHubMovies = result.first
                movieHubTotalResults = result.second
                movieHubTotalPages = result.third
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isMovieHubLoading = false
            }
        }
    }

    private fun fetchMovieCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val trending = fetchMoviesFromUrl("https://api.themoviedb.org/3/trending/movie/day?api_key=$tmdbApiKey", page = trendingMoviesPage)
            trendingMovies = trending.first
            trendingMoviesTotal = trending.second

            val popular = fetchMoviesFromUrl("https://api.themoviedb.org/3/movie/popular?api_key=$tmdbApiKey", page = popularMoviesPage)
            popularMovies = popular.first
            popularMoviesTotal = popular.second

            val topRated = fetchMoviesFromUrl("https://api.themoviedb.org/3/movie/top_rated?api_key=$tmdbApiKey", page = topRatedMoviesPage)
            topRatedMovies = topRated.first
            topRatedMoviesTotal = topRated.second

            val upcoming = fetchMoviesFromUrl("https://api.themoviedb.org/3/movie/upcoming?api_key=$tmdbApiKey", page = upcomingMoviesPage)
            upcomingMovies = upcoming.first
            upcomingMoviesTotal = upcoming.second
        }
    }

    private fun fetchTvShowCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val trending = fetchMoviesFromUrl("https://api.themoviedb.org/3/trending/tv/day?api_key=$tmdbApiKey", page = trendingTvPage)
            trendingTvShows = trending.first
            trendingTvTotal = trending.second

            val popular = fetchMoviesFromUrl("https://api.themoviedb.org/3/tv/popular?api_key=$tmdbApiKey", page = popularTvPage)
            popularTvShows = popular.first
            popularTvTotal = popular.second

            val topRated = fetchMoviesFromUrl("https://api.themoviedb.org/3/tv/top_rated?api_key=$tmdbApiKey", page = topRatedTvPage)
            topRatedTvShows = topRated.first
            topRatedTvTotal = topRated.second

            val onTheAir = fetchMoviesFromUrl("https://api.themoviedb.org/3/tv/on_the_air?api_key=$tmdbApiKey", page = onTheAirPage)
            onTheAirTvShows = onTheAir.first
            onTheAirTotal = onTheAir.second
        }
    }

    fun fetchNextSector(category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (category) {
                "trending_movies" -> { trendingMoviesPage++; trendingMovies = fetchMoviesFromUrl("https://api.themoviedb.org/3/trending/movie/day?api_key=$tmdbApiKey", page = trendingMoviesPage).first }
                "popular_movies" -> { popularMoviesPage++; popularMovies = fetchMoviesFromUrl("https://api.themoviedb.org/3/movie/popular?api_key=$tmdbApiKey", page = popularMoviesPage).first }
                "top_rated_movies" -> { topRatedMoviesPage++; topRatedMovies = fetchMoviesFromUrl("https://api.themoviedb.org/3/movie/top_rated?api_key=$tmdbApiKey", page = topRatedMoviesPage).first }
                "upcoming_movies" -> { upcomingMoviesPage++; upcomingMovies = fetchMoviesFromUrl("https://api.themoviedb.org/3/movie/upcoming?api_key=$tmdbApiKey", page = upcomingMoviesPage).first }
                "trending_tv" -> { trendingTvPage++; trendingTvShows = fetchMoviesFromUrl("https://api.themoviedb.org/3/trending/tv/day?api_key=$tmdbApiKey", page = trendingTvPage).first }
                "popular_tv" -> { popularTvPage++; popularTvShows = fetchMoviesFromUrl("https://api.themoviedb.org/3/tv/popular?api_key=$tmdbApiKey", page = popularTvPage).first }
                "top_rated_tv" -> { topRatedTvPage++; topRatedTvShows = fetchMoviesFromUrl("https://api.themoviedb.org/3/tv/top_rated?api_key=$tmdbApiKey", page = topRatedTvPage).first }
                "on_the_air" -> { onTheAirPage++; onTheAirTvShows = fetchMoviesFromUrl("https://api.themoviedb.org/3/tv/on_the_air?api_key=$tmdbApiKey", page = onTheAirPage).first }
            }
        }
    }

    private fun fetchMoviesFromUrl(urlString: String, page: Int = 1): Pair<List<Movie>, Int> {
        val movies = mutableListOf<Movie>()
        val isTv = urlString.contains("/tv/") || urlString.contains("/trending/tv/")
        var totalResults = 0
        
        try {
            // Fetch 5 TMDB pages to get 100 items per sector
            for (i in 0 until 5) {
                val tmdbPage = (page - 1) * 5 + i + 1
                val urlWithPage = if (urlString.contains("?")) "$urlString&page=$tmdbPage" else "$urlString?page=$tmdbPage"
                val response = URL(urlWithPage).readText()
                val json = JSONObject(response)
                totalResults = json.optInt("total_results", 0)
                val results = json.getJSONArray("results")
                
                for (j in 0 until results.length()) {
                    val obj = results.getJSONObject(j)
                    movies.add(Movie(
                        id = obj.getInt("id"),
                        title = obj.optString("title", obj.optString("name")),
                        posterPath = obj.optString("poster_path").takeIf { it.isNotEmpty() && it != "null" },
                        releaseDate = obj.optString("release_date", obj.optString("first_air_date")).takeIf { it.isNotEmpty() && it != "null" },
                        mediaType = if (isTv) "tv" else "movie"
                    ))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        
        return Pair(movies.distinctBy { it.id }, totalResults)
    }

    /** Whether the current channel list is a Khmer/Cambodia source */
    private var isKhmerSource = false

    fun fetchChannels(url: String) {
        isKhmerSource = url.contains("/kh.m3u") || url.contains("/khm.m3u") ||
                url.contains("cambodia", ignoreCase = true) || url.contains("khmer", ignoreCase = true)
        viewModelScope.launch {
            isLoading = true
            try {
                val m3uText = withContext(Dispatchers.IO) {
                    URL(url).readText()
                }
                channels = parseM3u(m3uText)
                // Auto-enrich Khmer channels with fresh MekongTV URLs
                if (isKhmerSource) {
                    enrichChannelsWithMekongTv()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                channels = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * For each Khmer channel that has a MekongTV mapping, resolve a fresh
     * stream URL and put it FIRST — replacing broken iptv-org URLs.
     */
    private fun enrichChannelsWithMekongTv() {
        viewModelScope.launch {
            val enriched = channels.toMutableList()
            val resolveJobs = enriched.mapIndexed { index, channel ->
                val slug = mekongTvSlugMap.entries.firstOrNull { (key, _) ->
                    channel.name.contains(key, ignoreCase = true) ||
                    key.contains(channel.name.replace(Regex("\\s*\\(.*\\)"), "").trim(), ignoreCase = true)
                }?.value

                if (slug != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val mekongUrl = resolveMekongTvUrl(channel.name)
                        if (mekongUrl != null) {
                            // Put MekongTV URL first, keep old URLs as fallback
                            val newUrls = listOf(mekongUrl) + channel.urls.filter {
                                !it.contains("mekongtv.net")
                            }
                            enriched[index] = channel.copy(urls = newUrls)
                            android.util.Log.d("IptvVM", "Enriched ${channel.name} with MekongTV URL")
                        }
                    }
                } else null
            }.filterNotNull()

            // Wait for all resolves to complete
            resolveJobs.forEach { it.join() }
            channels = enriched.toList()
        }
    }

    private fun parseM3u(m3uText: String): List<Channel> {
        val channelMap = mutableMapOf<String, Channel>()
        val lines = m3uText.lines()
        var currentName = ""
        var currentLogo = ""
        var currentGroup = ""
        var currentId = ""

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("#EXTINF:")) {
                currentName = trimmedLine.substringAfterLast(",").trim()
                currentLogo = extractAttribute(trimmedLine, "tvg-logo")
                currentGroup = extractAttribute(trimmedLine, "group-title")
                currentId = extractAttribute(trimmedLine, "tvg-id")
            } else if (trimmedLine.startsWith("http")) {
                val url = trimmedLine
                val key = currentId.takeIf { it.isNotEmpty() } ?: currentName
                
                if (channelMap.containsKey(key)) {
                    val existing = channelMap[key]!!
                    val newUrls = existing.urls.toMutableList().apply { add(url) }
                    channelMap[key] = existing.copy(urls = newUrls)
                } else {
                    channelMap[key] = Channel(currentId, currentName, currentLogo, listOf(url), currentGroup)
                }
                
                currentName = ""
                currentLogo = ""
                currentGroup = ""
                currentId = ""
            }
        }
        return channelMap.values.toList()
    }

    private fun extractAttribute(line: String, attribute: String): String {
        val regex = "$attribute=\"([^\"]*)\"".toRegex()
        val match = regex.find(line)
        return match?.groupValues?.get(1) ?: ""
    }

    fun playChannel(channel: Channel) {
        currentChannel = channel
    }
    
    fun playNextChannel() {
        val currentIndex = channels.indexOf(currentChannel)
        if (currentIndex != -1 && currentIndex < channels.size - 1) {
            currentChannel = channels[currentIndex + 1]
        } else if (channels.isNotEmpty()) {
            currentChannel = channels[0] // Loop back to the start
        }
    }

    fun playPreviousChannel() {
        val currentIndex = channels.indexOf(currentChannel)
        if (currentIndex > 0) {
            currentChannel = channels[currentIndex - 1]
        } else if (channels.isNotEmpty()) {
            currentChannel = channels[channels.size - 1] // Loop to the end
        }
    }
    
    fun closePlayer() {
        currentChannel = null
    }

    fun setLanguage(lang: String) {
        appLanguage = lang
    }
    
    fun toggleTheme() {
        isDarkMode = !isDarkMode
    }
    
    fun changeThemeSkin(skin: String) {
        themeSkin = skin
    }

    fun setBackground(skin: String) {
        backgroundSkin = skin
    }

    fun translateCategory(name: String): String {
        if (appLanguage != "km") return name
        return when (name.lowercase()) {
            "animation" -> "តុក្កតា"
            "auto" -> "យានយន្ត"
            "business" -> "ពាណិជ្ជកម្ម"
            "classic" -> "បុរាណ"
            "comedy" -> "កំប្លែង"
            "cooking" -> "ចម្អិនអាហារ"
            "culture" -> "វប្បធម៌"
            "documentary" -> "ឯកសារ"
            "education" -> "អប់រំ"
            "entertainment" -> "កម្សាន្ត"
            "family" -> "គ្រួសារ"
            "general" -> "ទូទៅ"
            "interactive" -> "អន្តរកម្ម"
            "kids" -> "កុមារ"
            "legislative" -> "នីតិប្បញ្ញត្តិ"
            "lifestyle" -> "របៀបរស់នៅ"
            "movies" -> "ភាពយន្ត"
            "music" -> "តន្ត្រី"
            "news" -> "ព័ត៌មាន"
            "outdoor" -> "ក្រៅផ្ទះ"
            "public" -> "សាធារណៈ"
            "relax" -> "សម្រាក"
            "religious" -> "សាសនា"
            "science" -> "វិទ្យាសាស្ត្រ"
            "series" -> "រឿងភាគ"
            "shop" -> "ទិញទំនិញ"
            "sports" -> "កីឡា"
            "travel" -> "ទេសចរណ៍"
            "weather" -> "អាកាសធាតុ"
            "action" -> "សកម្មភាព"
            "adventure" -> "ផ្សងព្រេង"
            "crime" -> "បទល្មើស"
            "history" -> "ប្រវត្តិសាស្ត្រ"
            "horror" -> "រន្ធត់"
            "thriller" -> "រំភើប"
            "undefined" -> "មិនបានកំណត់"
            else -> name
        }
    }

    fun translateChannelName(name: String): String {
        if (appLanguage != "km") return name
        var translated = name
        val replacements = mapOf(
            "HD" to "ច្បាស់",
            "TV" to "ទូរទស្សន៍",
            "Movies" to "ភាពយន្ត",
            "News" to "ព័ត៌មាន",
            "Music" to "តន្ត្រី",
            "Sports" to "កីឡា",
            "Live" to "ផ្ទាល់",
            "International" to "អន្តរជាតិ",
            "World" to "ពិភពលោក"
        )
        for ((eng, km) in replacements) {
            translated = translated.replace(eng, km, ignoreCase = true)
        }
        return translated
    }

    fun togglePrivateMode() {
        showPrivateCategories = !showPrivateCategories
    }

    fun toggleAutoNext() {
        autoNext = !autoNext
    }

    fun openWebUrl(url: String, title: String, isAi: Boolean = false) {
        currentWebUrl = url
        currentWebTitle = title
        isCurrentWebAiHub = isAi
    }

    fun closeWebUrl() {
        currentWebUrl = null
        currentWebTitle = null
        isCurrentWebAiHub = false
    }

    fun refreshWebView(url: String) {
        webViewCache[url]?.reload()
    }

    override fun onCleared() {
        super.onCleared()
        webViewCache.values.forEach { it.destroy() }
        webViewCache.clear()
    }
}
