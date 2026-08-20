package com.yilmaz.sofortakip

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Sefer(
    val id: Long,
    val guzergah: String,
    val cikisKm: Int,
    val donusKm: Int,
    val cikisSaati: String,
    val donusSaati: String,
    val toplamKm: Int,
    val toplamSure: String,
    val notMetni: String,
    val tarih: String
)

data class Arac(
    val id: Long,
    val ad: String,
    val plaka: String,
    val fotoUri: String = "",
    val yakit: Int = 100,
    val sonDolum: String = "",
    val sonKm: Int = 0,
    val secili: Boolean = false
)

private enum class Tema { ACIK, MAVİ, GECE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SoforTakip() }
    }
}

@Composable
fun SoforTakip() {
    val context = LocalContext.current

    var seferler by remember { mutableStateOf(yukleSeferler(context)) }
    var araclar by remember { mutableStateOf(yukleAraclar(context)) }
    var notlar by remember { mutableStateOf(yukleNotlar(context)) }

    var tema by remember { mutableStateOf(Tema.ACIK) }
    var sayfa by remember { mutableStateOf(0) }

    var notlarAcik by remember { mutableStateOf(false) }
    var araclarAcik by remember { mutableStateOf(false) }
    var aracDuzenle by remember { mutableStateOf<Arac?>(null) }
    var guzergahDialog by remember { mutableStateOf(false) }

    var aktif by remember { mutableStateOf(false) }
    var guzergah by remember { mutableStateOf("") }
    var cikisKm by remember { mutableStateOf("") }
    var donusKm by remember { mutableStateOf("") }
    var notMetni by remember { mutableStateOf("") }
    var cikisSaati by remember { mutableStateOf("") }
    var baslangic by remember { mutableStateOf(0L) }

    var bilgi by remember { mutableStateOf("") }
    var silinecek by remember { mutableStateOf<Sefer?>(null) }
    var bitirDialog by remember { mutableStateOf(false) }

    val seciliArac = araclar.firstOrNull { it.secili } ?: araclar.firstOrNull()
    val toplamKm = seferler.sumOf { it.toplamKm }
    val toplamSureDakika = seferler.sumOf { sureDakika(it.toplamSure) }
    val ortalamaKm = if (seferler.isEmpty()) 0 else toplamKm / seferler.size
    val bugun = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

    val scheme = when (tema) {
        Tema.ACIK -> lightColorScheme(
            primary = Color(0xFF5B2BC5),
            onPrimary = Color.White,
            background = Color(0xFFF8F7FC),
            onBackground = Color(0xFF202024),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF0EDF7),
            onSurfaceVariant = Color(0xFF66616D)
        )
        Tema.MAVİ -> lightColorScheme(
            primary = Color(0xFF1565C0),
            secondary = Color(0xFF00A6C7),
            background = Color(0xFFF2F8FC),
            surfaceVariant = Color(0xFFE2EEF7)
        )
        Tema.GECE -> darkColorScheme(
            primary = Color(0xFFB69CFF),
            secondary = Color(0xFF7CC9FF),
            background = Color(0xFF0D1117),
            surface = Color(0xFF151B23),
            surfaceVariant = Color(0xFF202833)
        )
    }

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BottomNavigation(
                    sayfa = sayfa,
                    onHome = { sayfa = 0 },
                    onHistory = { sayfa = 1 },
                    onReports = { sayfa = 2 },
                    onSettings = { sayfa = 3 },
                    onPlus = {
                        sayfa = 0
                        aktif = false
                    }
                )
            }
        ) { padding ->
            when (sayfa) {
                0 -> AnaSayfa(
                    modifier = Modifier.padding(padding),
                    seferler = seferler,
                    seciliArac = seciliArac,
                    toplamKm = toplamKm,
                    toplamSureDakika = toplamSureDakika,
                    ortalamaKm = ortalamaKm,
                    aktif = aktif,
                    guzergah = guzergah,
                    cikisKm = cikisKm,
                    cikisSaati = cikisSaati.ifBlank { saat() },
                    onGuzergah = { guzergah = it },
                    onGuzergahDialog = { guzergahDialog = true },
                    onCikisKm = { cikisKm = it },
                    onStart = {
                        when {
                            seciliArac == null ->
                                bilgi = "Önce Araç Bilgisi bölümünden bir araç ekleyin."
                            guzergah.isBlank() ->
                                bilgi = "Lütfen güzergah seçin."
                            cikisKm.toIntOrNull() == null ->
                                bilgi = "Geçerli bir çıkış KM girin."
                            else -> {
                                aktif = true
                                cikisSaati = saat()
                                baslangic = System.currentTimeMillis()
                            }
                        }
                    },
                    onFinish = { bitirDialog = true },
                    onNavigation = { navigasyonAc(context, guzergah) },
                    onNotes = { notlarAcik = true },
                    onReports = { sayfa = 2 },
                    onSettings = { sayfa = 3 },
                    onVehicles = { araclarAcik = true },
                    onHistory = { sayfa = 1 },
                    onThemeSelect = { tema = it }
                )

                1 -> GecmisSayfasi(
                    modifier = Modifier.padding(padding),
                    seferler = seferler,
                    onDelete = { silinecek = it }
                )

                2 -> RaporSayfasi(
                    modifier = Modifier.padding(padding),
                    seferler = seferler,
                    toplamKm = toplamKm,
                    toplamSureDakika = toplamSureDakika,
                    ortalamaKm = ortalamaKm
                )

                else -> AyarlarSayfasi(
                    modifier = Modifier.padding(padding),
                    tema = tema,
                    onTema = { tema = it },
                    aracSayisi = araclar.size,
                    onVehicles = { araclarAcik = true },
                    onNotes = { notlarAcik = true }
                )
            }
        }
    }

    if (guzergahDialog) {
        RouteDialog(
            current = guzergah,
            onClose = { guzergahDialog = false },
            onSave = {
                guzergah = it
                guzergahDialog = false
            }
        )
    }

    if (notlarAcik) {
        NotesDialog(
            notlar = notlar,
            onClose = { notlarAcik = false },
            onSave = {
                notlar = it
                kaydetNotlar(context, it)
            }
        )
    }

    if (araclarAcik) {
        AraclarDialog(
            araclar = araclar,
            onClose = { araclarAcik = false },
            onSave = { yeni ->
                araclar = if (araclar.isEmpty()) {
                    listOf(yeni.copy(secili = true))
                } else {
                    araclar + yeni
                }
                kaydetAraclar(context, araclar)
            },
            onDelete = { sil ->
                araclar = araclar.filterNot { it.id == sil.id }
                if (araclar.isNotEmpty() && araclar.none { it.secili }) {
                    araclar = araclar.mapIndexed { index, a ->
                        if (index == 0) a.copy(secili = true) else a
                    }
                }
                kaydetAraclar(context, araclar)
            },
            onSelect = { sec ->
                araclar = araclar.map { it.copy(secili = it.id == sec.id) }
                kaydetAraclar(context, araclar)
                araclarAcik = false
            },
            onEdit = { aracDuzenle = it }
        )
    }

    aracDuzenle?.let { arac ->
        AracDuzenleDialog(
            arac = arac,
            onClose = { aracDuzenle = null },
            onSave = { guncel ->
                araclar = araclar.map { if (it.id == guncel.id) guncel else it }
                kaydetAraclar(context, araclar)
                aracDuzenle = null
            }
        )
    }

    if (bitirDialog) {
        BitirDialog(
            guzergah = guzergah,
            cikisKm = cikisKm,
            cikisSaati = cikisSaati,
            donusKm = donusKm,
            notMetni = notMetni,
            onDonusKm = { donusKm = it },
            onNot = { notMetni = it },
            onClose = { bitirDialog = false },
            onSave = {
                val c = cikisKm.toIntOrNull()
                val d = donusKm.toIntOrNull()

                when {
                    c == null || d == null ->
                        bilgi = "Çıkış ve dönüş KM bilgilerini girin."
                    d < c ->
                        bilgi = "Dönüş KM, çıkış KM'den küçük olamaz."
                    else -> {
                        val bitis = System.currentTimeMillis()
                        val yeni = Sefer(
                            id = bitis,
                            guzergah = guzergah.trim(),
                            cikisKm = c,
                            donusKm = d,
                            cikisSaati = cikisSaati,
                            donusSaati = saat(),
                            toplamKm = d - c,
                            toplamSure = sureHesapla(baslangic, bitis),
                            notMetni = notMetni.trim(),
                            tarih = bugun
                        )

                        seferler = listOf(yeni) + seferler
                        kaydetSeferler(context, seferler)

                        if (seciliArac != null) {
                            val guncelArac = seciliArac.copy(sonKm = d)
                            araclar = araclar.map {
                                if (it.id == guncelArac.id) guncelArac else it
                            }
                            kaydetAraclar(context, araclar)
                        }

                        aktif = false
                        bitirDialog = false
                        guzergah = ""
                        cikisKm = ""
                        donusKm = ""
                        notMetni = ""
                        cikisSaati = ""
                        baslangic = 0L
                    }
                }
            }
        )
    }

    if (bilgi.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { bilgi = "" },
            title = { Text("Bilgi") },
            text = { Text(bilgi) },
            confirmButton = {
                TextButton(onClick = { bilgi = "" }) {
                    Text("Tamam")
                }
            }
        )
    }

    silinecek?.let { sefer ->
        AlertDialog(
            onDismissRequest = { silinecek = null },
            title = { Text("Seferi sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${sefer.guzergah}\" seferi geçmişten silinecek.") },
            confirmButton = {
                Button(
                    onClick = {
                        seferler = seferler.filterNot { it.id == sefer.id }
                        kaydetSeferler(context, seferler)
                        silinecek = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(6.dp))
                    Text("SİL")
                }
            },
            dismissButton = {
                TextButton(onClick = { silinecek = null }) {
                    Text("VAZGEÇ")
                }
            }
        )
    }
}

@Composable
private fun BottomNavigation(
    sayfa: Int,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onPlus: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NavigationBarItem(
                selected = sayfa == 0,
                onClick = onHome,
                icon = { Icon(Icons.Default.Home, null) },
                label = { Text("Ana Sayfa") }
            )
            NavigationBarItem(
                selected = sayfa == 1,
                onClick = onHistory,
                icon = { Icon(Icons.Default.History, null) },
                label = { Text("Geçmiş") }
            )

            Spacer(Modifier.width(72.dp))

            NavigationBarItem(
                selected = sayfa == 2,
                onClick = onReports,
                icon = { Icon(Icons.Default.BarChart, null) },
                label = { Text("Raporlar") }
            )
            NavigationBarItem(
                selected = sayfa == 3,
                onClick = onSettings,
                icon = { Icon(Icons.Default.Settings, null) },
                label = { Text("Ayarlar") }
            )
        }

        FloatingActionButton(
            onClick = onPlus,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Yeni sefer",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun AnaSayfa(
    modifier: Modifier,
    seferler: List<Sefer>,
    seciliArac: Arac?,
    toplamKm: Int,
    toplamSureDakika: Int,
    ortalamaKm: Int,
    aktif: Boolean,
    guzergah: String,
    cikisKm: String,
    cikisSaati: String,
    onGuzergah: (String) -> Unit,
    onGuzergahDialog: () -> Unit,
    onCikisKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onNavigation: () -> Unit,
    onNotes: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onVehicles: () -> Unit,
    onHistory: () -> Unit,
    onThemeSelect: (Tema) -> Unit
) {
    var anaMenu by remember { mutableStateOf(false) }
    var temaMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Header(
                onMenu = { anaMenu = true },
                onDismissMenu = { anaMenu = false },
                anaMenu = anaMenu,
                onNotes = onNotes,
                onTheme = { temaMenu = true },
                themeMenu = temaMenu,
                onDismissTheme = { temaMenu = false },
                onThemeSelect = {
                    temaMenu = false
                    onThemeSelect(it)
                },
                onHome = { anaMenu = false },
                onHistory = {
                    anaMenu = false
                    onHistory()
                },
                onReports = {
                    anaMenu = false
                    onReports()
                },
                onVehicles = {
                    anaMenu = false
                    onVehicles()
                },
                onSettings = {
                    anaMenu = false
                    onSettings()
                }
            )
        }

        item {
            WelcomeBanner(
                arac = seciliArac,
                seferSayisi = seferler.size,
                toplamKm = toplamKm
            )
        }

        item {
            StatsRow(
                sefer = seferler.size,
                km = toplamKm,
                sure = toplamSureDakika,
                ortalama = ortalamaKm,
                yakit = seciliArac?.yakit ?: 0
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NewTripCard(
                    modifier = Modifier.weight(1f),
                    aktif = aktif,
                    guzergah = guzergah,
                    cikisKm = cikisKm,
                    cikisSaati = cikisSaati,
                    onGuzergah = onGuzergah,
                    onGuzergahDialog = onGuzergahDialog,
                    onCikisKm = onCikisKm,
                    onStart = onStart,
                    onFinish = onFinish
                )

                VehicleInfoCard(
                    modifier = Modifier.weight(1f),
                    arac = seciliArac,
                    onSelect = onVehicles
                )
            }
        }

        item {
            SectionTitle(
                icon = Icons.Default.Apps,
                title = "Hızlı İşlemler"
            )
        }

        item {
            QuickActions(
                onNavigation = onNavigation,
                onNotes = onNotes,
                onReports = onReports,
                onSettings = onSettings
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "Sefer Geçmişi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onHistory) {
                    Text("Tümünü Gör")
                }
            }
        }

        if (seferler.isEmpty()) {
            item { EmptyCard() }
        } else {
            items(seferler.take(3), key = { it.id }) { sefer ->
                HomeTripCard(
                    sefer = sefer,
                    arac = seciliArac
                )
            }
        }
    }
}

@Composable
private fun Header(
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    anaMenu: Boolean,
    onNotes: () -> Unit,
    onTheme: () -> Unit,
    themeMenu: Boolean,
    onDismissTheme: () -> Unit,
    onThemeSelect: (Tema) -> Unit,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onReports: () -> Unit,
    onVehicles: () -> Unit,
    onSettings: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenu,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Menu,
                            "Menü",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(5.dp))

            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        "Şoför Takip",
                        tint = Color.White,
                        modifier = Modifier.size(31.dp)
                    )
                }
            }

            Spacer(Modifier.width(9.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Şoför Takip",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    "Günlük sefer paneli",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(onClick = onNotes) {
                Icon(Icons.Default.Note, "Notlar", modifier = Modifier.size(27.dp))
            }

            IconButton(onClick = onTheme) {
                Icon(Icons.Default.Palette, "Tema", modifier = Modifier.size(27.dp))
            }
        }

        DropdownMenu(
            expanded = anaMenu,
            onDismissRequest = onDismissMenu
        ) {
            DropdownMenuItem(
                text = { Text("Ana Sayfa") },
                onClick = onHome
            )
            DropdownMenuItem(
                text = { Text("Geçmiş") },
                onClick = onHistory
            )
            DropdownMenuItem(
                text = { Text("Raporlar") },
                onClick = onReports
            )
            DropdownMenuItem(
                text = { Text("Araçlar") },
                onClick = onVehicles
            )
            DropdownMenuItem(
                text = { Text("Ayarlar") },
                onClick = onSettings
            )
        }

        DropdownMenu(
            expanded = themeMenu,
            onDismissRequest = onDismissTheme
        ) {
            DropdownMenuItem(
                text = { Text("Açık") },
                onClick = { onThemeSelect(Tema.ACIK) }
            )
            DropdownMenuItem(
                text = { Text("Mavi") },
                onClick = { onThemeSelect(Tema.MAVİ) }
            )
            DropdownMenuItem(
                text = { Text("Gece") },
                onClick = { onThemeSelect(Tema.GECE) }
            )
        }
    }
}

@Composable
private fun WelcomeBanner(
    arac: Arac?,
    seferSayisi: Int,
    toplamKm: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(27.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(106.dp)
                    .clip(RoundedCornerShape(24.dp)),
                color = Color.White.copy(alpha = 0.13f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (arac?.fotoUri?.isNotBlank() == true) {
                        UriImage(
                            Uri.parse(arac.fotoUri),
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                        )
                    } else {
                        Icon(
                            Icons.Default.DirectionsCar,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(58.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Bugün hazır\nmısınız?",
                    fontSize = 25.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    if (arac == null) {
                        "Aracınızı ekleyerek başlayın."
                    } else {
                        "${arac.ad} • ${arac.plaka}"
                    },
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 1
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "${seferSayisi} sefer • ${formatKm(toplamKm)} KM",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Icon(
                Icons.Default.Route,
                null,
                tint = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.size(75.dp)
            )
        }
    }
}

@Composable
private fun StatsRow(
    sefer: Int,
    km: Int,
    sure: Int,
    ortalama: Int,
    yakit: Int
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.DirectionsCar,
            value = sefer.toString(),
            label = "Toplam Sefer",
            iconColor = Color(0xFF6C2BD9)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Speed,
            value = formatKm(km),
            label = "Toplam KM",
            iconColor = Color(0xFF2196F3)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Timer,
            value = "${sure / 60}s ${sure % 60}dk",
            label = "Toplam Süre",
            iconColor = Color(0xFF00C853)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Speed,
            value = formatKm(ortalama),
            label = "Ort. Sefer KM",
            iconColor = Color(0xFFFF9800)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.LocalGasStation,
            value = "%$yakit",
            label = "Yakıt Durumu",
            iconColor = Color(0xFFE91E63)
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color
) {
    Card(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(13.dp),
                color = iconColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(5.dp))

            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Text(
                label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NewTripCard(
    modifier: Modifier,
    aktif: Boolean,
    guzergah: String,
    cikisKm: String,
    cikisSaati: String,
    onGuzergah: (String) -> Unit,
    onGuzergahDialog: () -> Unit,
    onCikisKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                if (aktif) "Sefer Devam Ediyor" else "Yeni Sefer",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                if (aktif) "Seferiniz devam ediyor" else "Seferinizi başlatın",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SelectionField(
                icon = Icons.Default.Place,
                text = guzergah.ifBlank { "Güzergah seçin" },
                placeholder = guzergah.isBlank(),
                onClick = onGuzergahDialog
            )

            OutlinedTextField(
                value = cikisKm,
                onValueChange = {
                    onCikisKm(it.filter(Char::isDigit))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Çıkış KM") },
                leadingIcon = {
                    Icon(Icons.Default.Speed, null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(15.dp)
            )

            SelectionField(
                icon = Icons.Default.AccessTime,
                text = cikisSaati,
                placeholder = false,
                onClick = {}
            )

            Button(
                onClick = if (aktif) onFinish else onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (aktif) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Icon(
                    if (aktif) Icons.Default.Stop else Icons.Default.PlayArrow,
                    null
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    if (aktif) "SEFERİ BİTİR" else "SEFERİ BAŞLAT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SelectionField(
    icon: ImageVector,
    text: String,
    placeholder: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = if (placeholder) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VehicleInfoCard(
    modifier: Modifier,
    arac: Arac?,
    onSelect: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DirectionsCar,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "Araç Bilgisi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onSelect,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        "Araç seç",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (arac == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .clickable(onClick = onSelect),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(55.dp)
                    )
                }

                Text(
                    "Araç seçin",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelect)
                )

                Text(
                    "Araç eklemek veya seçmek için dokunun.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (arac.fotoUri.isNotBlank()) {
                    UriImage(
                        Uri.parse(arac.fotoUri),
                        Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(17.dp))
                    )
                } else {
                    Surface(
                        Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(17.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(58.dp)
                            )
                        }
                    }
                }

                Text(
                    arac.ad,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                VehicleInfoLine("Plaka", arac.plaka)
                VehicleInfoLine(
                    "Yakıt Tipi",
                    yakitTipi(arac.ad, arac.sonDolum)
                )

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Yakıt",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "%${arac.yakit}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF25B85A)
                    )
                }

                LinearProgressIndicator(
                    progress = arac.yakit.coerceIn(0, 100) / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF25B85A),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                VehicleInfoLine(
                    "Km",
                    "${formatKm(arac.sonKm)} km"
                )
                VehicleInfoLine(
                    "Periyodik Bakım",
                    "12.500 km"
                )
            }
        }
    }
}

@Composable
private fun VehicleInfoLine(
    label: String,
    value: String
) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun yakitTipi(ad: String, sonDolum: String): String {
    val metin = "$ad $sonDolum".lowercase(Locale.getDefault())
    return if (
        metin.contains("dizel") ||
        metin.contains("transit") ||
        metin.contains("kamyon") ||
        metin.contains("minibüs")
    ) {
        "Dizel"
    } else {
        "Benzin"
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(7.dp))
        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun QuickActions(
    onNavigation: () -> Unit,
    onNotes: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionCard(
            Modifier.weight(1f),
            Icons.Default.Navigation,
            "Navigasyon",
            "Harita aç",
            Color(0xFF5B2BC5),
            onNavigation
        )
        ActionCard(
            Modifier.weight(1f),
            Icons.Default.NoteAdd,
            "Not Ekle",
            "Hızlı not",
            Color(0xFF24B95A),
            onNotes
        )
        ActionCard(
            Modifier.weight(1f),
            Icons.Default.BarChart,
            "Raporlar",
            "Detaylı gör",
            Color(0xFF4B7BEC),
            onReports
        )
        ActionCard(
            Modifier.weight(1f),
            Icons.Default.Settings,
            "Ayarlar",
            "Uygulama",
            Color(0xFFF28A00),
            onSettings
        )
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                Modifier.size(45.dp),
                RoundedCornerShape(14.dp),
                color = iconColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = iconColor,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Spacer(Modifier.height(7.dp))

            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HomeTripCard(
    sefer: Sefer,
    arac: Arac?
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (arac?.fotoUri?.isNotBlank() == true) {
                UriImage(
                    Uri.parse(arac.fotoUri),
                    Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(13.dp))
                )
            } else {
                Surface(
                    Modifier.size(76.dp),
                    RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Route,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    sefer.guzergah,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        sefer.tarih,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.AccessTime,
                        null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        sefer.cikisSaati,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    "Süre: ${sefer.toplamSure} • Ort. Hız: --",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Text(
                    "${formatKm(sefer.toplamKm)} KM",
                    modifier = Modifier.padding(
                        horizontal = 9.dp,
                        vertical = 5.dp
                    ),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(4.dp))

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GecmisSayfasi(
    modifier: Modifier,
    seferler: List<Sefer>,
    onDelete: (Sefer) -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageTitle(
                "Sefer Geçmişi",
                "Tüm tamamlanmış seferler"
            )
        }

        if (seferler.isEmpty()) {
            item { EmptyCard() }
        } else {
            items(seferler, key = { it.id }) { sefer ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    sefer.guzergah,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    sefer.tarih,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                "${formatKm(sefer.toplamKm)} KM",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(7.dp))

                        Text(
                            "${formatKm(sefer.cikisKm)} → ${formatKm(sefer.donusKm)} KM   •   ${sefer.cikisSaati} → ${sefer.donusSaati}"
                        )

                        Text(
                            sefer.toplamSure,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (sefer.notMetni.isNotBlank()) {
                            Spacer(Modifier.height(7.dp))
                            Text(
                                "Not: ${sefer.notMetni}",
                                fontSize = 12.sp
                            )
                        }

                        TextButton(onClick = { onDelete(sefer) }) {
                            Icon(Icons.Default.Delete, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Sil")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RaporSayfasi(
    modifier: Modifier,
    seferler: List<Sefer>,
    toplamKm: Int,
    toplamSureDakika: Int,
    ortalamaKm: Int
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageTitle("Raporlar", "Sürüş performans özeti")
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportBox(
                    Modifier.weight(1f),
                    "Sefer",
                    seferler.size.toString()
                )
                ReportBox(
                    Modifier.weight(1f),
                    "KM",
                    formatKm(toplamKm)
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportBox(
                    Modifier.weight(1f),
                    "Süre",
                    "${toplamSureDakika / 60}s ${toplamSureDakika % 60}dk"
                )
                ReportBox(
                    Modifier.weight(1f),
                    "Ort. KM",
                    formatKm(ortalamaKm)
                )
            }
        }

        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Son seferler",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    seferler.take(10).forEach {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp)
                        ) {
                            Text(it.guzergah, Modifier.weight(1f))
                            Text(
                                "${formatKm(it.toplamKm)} KM",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportBox(
    modifier: Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                value,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AyarlarSayfasi(
    modifier: Modifier,
    tema: Tema,
    onTema: (Tema) -> Unit,
    aracSayisi: Int,
    onVehicles: () -> Unit,
    onNotes: () -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageTitle("Ayarlar", "Uygulama tercihleri")
        }

        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Tema",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(
                            selected = tema == Tema.ACIK,
                            onClick = { onTema(Tema.ACIK) },
                            label = { Text("Açık") }
                        )
                        FilterChip(
                            selected = tema == Tema.MAVİ,
                            onClick = { onTema(Tema.MAVİ) },
                            label = { Text("Mavi") }
                        )
                        FilterChip(
                            selected = tema == Tema.GECE,
                            onClick = { onTema(Tema.GECE) },
                            label = { Text("Gece") }
                        )
                    }
                }
            }
        }

        item {
            SettingRow(
                Icons.Default.DirectionsCar,
                "Araçlarım",
                "$aracSayisi kayıtlı araç",
                onVehicles
            )
        }

        item {
            SettingRow(
                Icons.Default.Note,
                "Genel Notlar",
                "Hızlı notlarınızı yönetin",
                onNotes
            )
        }
    }
}

@Composable
private fun PageTitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            title,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun RouteDialog(
    current: String,
    onClose: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(current) { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text("Güzergah", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Güzergah") },
                    placeholder = { Text("Akseki → Antalya") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotBlank()) onSave(value.trim())
                }
            ) {
                Text("SEÇ")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("VAZGEÇ")
            }
        }
    )
}

@Composable
private fun AraclarDialog(
    araclar: List<Arac>,
    onClose: () -> Unit,
    onSave: (Arac) -> Unit,
    onDelete: (Arac) -> Unit,
    onSelect: (Arac) -> Unit,
    onEdit: (Arac) -> Unit
) {
    var ad by remember { mutableStateOf("") }
    var plaka by remember { mutableStateOf("") }
    var yakit by remember { mutableStateOf("100") }
    var sonKm by remember { mutableStateOf("") }
    var sonDolum by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) fotoUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text("Araçlarım", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (araclar.isNotEmpty()) {
                    araclar.forEach { arac ->
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (arac.secili) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                Modifier.padding(9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (arac.fotoUri.isNotBlank()) {
                                    UriImage(
                                        Uri.parse(arac.fotoUri),
                                        Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                } else {
                                    Surface(
                                        Modifier.size(52.dp),
                                        RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.DirectionsCar, null)
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        arac.ad,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        arac.plaka,
                                        fontSize = 12.sp
                                    )
                                }

                                TextButton(onClick = { onSelect(arac) }) {
                                    Text(if (arac.secili) "Seçili" else "Seç")
                                }

                                IconButton(onClick = { onEdit(arac) }) {
                                    Icon(Icons.Default.Edit, "Düzenle")
                                }

                                IconButton(onClick = { onDelete(arac) }) {
                                    Icon(Icons.Default.Delete, "Sil")
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                }

                Text(
                    "Yeni araç ekle",
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Araç adı") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = plaka,
                    onValueChange = {
                        plaka = it.uppercase(Locale.getDefault())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Plaka") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = yakit,
                    onValueChange = {
                        yakit = it.filter(Char::isDigit).take(3)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Yakıt %") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sonKm,
                    onValueChange = {
                        sonKm = it.filter(Char::isDigit)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Son KM") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sonDolum,
                    onValueChange = { sonDolum = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Son dolum tarihi / yakıt tipi") },
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Photo, null)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (fotoUri.isBlank()) {
                            "Araç fotoğrafı seç"
                        } else {
                            "Fotoğraf seçildi"
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ad.isNotBlank() && plaka.isNotBlank()) {
                        onSave(
                            Arac(
                                id = System.currentTimeMillis(),
                                ad = ad.trim(),
                                plaka = plaka.trim(),
                                fotoUri = fotoUri,
                                yakit = yakit.toIntOrNull()?.coerceIn(0, 100) ?: 100,
                                sonDolum = sonDolum.trim(),
                                sonKm = sonKm.toIntOrNull() ?: 0,
                                secili = araclar.isEmpty()
                            )
                        )
                        ad = ""
                        plaka = ""
                        fotoUri = ""
                        sonKm = ""
                        sonDolum = ""
                        yakit = "100"
                    }
                }
            ) {
                Text("Araç Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Kapat")
            }
        }
    )
}

@Composable
private fun AracDuzenleDialog(
    arac: Arac,
    onClose: () -> Unit,
    onSave: (Arac) -> Unit
) {
    var ad by remember(arac.id) { mutableStateOf(arac.ad) }
    var plaka by remember(arac.id) { mutableStateOf(arac.plaka) }
    var yakit by remember(arac.id) { mutableStateOf(arac.yakit.toString()) }
    var sonKm by remember(arac.id) { mutableStateOf(arac.sonKm.toString()) }
    var sonDolum by remember(arac.id) { mutableStateOf(arac.sonDolum) }
    var fotoUri by remember(arac.id) { mutableStateOf(arac.fotoUri) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) fotoUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Araç Bilgilerini Düzenle") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Araç adı") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = plaka,
                    onValueChange = {
                        plaka = it.uppercase(Locale.getDefault())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Plaka") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = yakit,
                    onValueChange = { yakit = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Yakıt %") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = sonKm,
                    onValueChange = { sonKm = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Son KM") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = sonDolum,
                    onValueChange = { sonDolum = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Son dolum / yakıt tipi") },
                    singleLine = true
                )
                OutlinedButton(
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Photo, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Fotoğraf değiştir")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        arac.copy(
                            ad = ad.trim(),
                            plaka = plaka.trim(),
                            fotoUri = fotoUri,
                            yakit = yakit.toIntOrNull()?.coerceIn(0, 100) ?: arac.yakit,
                            sonKm = sonKm.toIntOrNull() ?: arac.sonKm,
                            sonDolum = sonDolum.trim()
                        )
                    )
                }
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Vazgeç")
            }
        }
    )
}

@Composable
private fun BitirDialog(
    guzergah: String,
    cikisKm: String,
    cikisSaati: String,
    donusKm: String,
    notMetni: String,
    onDonusKm: (String) -> Unit,
    onNot: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text("Seferi Tamamla", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    guzergah,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Çıkış: $cikisKm KM • $cikisSaati")
                OutlinedTextField(
                    value = donusKm,
                    onValueChange = {
                        onDonusKm(it.filter(Char::isDigit))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dönüş KM") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notMetni,
                    onValueChange = onNot,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sefer Notu") },
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(5.dp))
                Text("KAYDET")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("VAZGEÇ")
            }
        }
    )
}

@Composable
private fun NotesDialog(
    notlar: String,
    onClose: () -> Unit,
    onSave: (String) -> Unit
) {
    var metin by remember(notlar) { mutableStateOf(notlar) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Notlar", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = metin,
                onValueChange = { metin = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Not") },
                minLines = 6,
                maxLines = 10
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(metin)
                    onClose()
                }
            ) {
                Text("KAYDET")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("VAZGEÇ")
            }
        }
    )
}

@Composable
private fun EmptyCard() {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.History,
                null,
                Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text("Henüz tamamlanmış sefer yok.")
        }
    }
}

@Composable
private fun UriImage(
    uri: Uri,
    modifier: Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = runCatching {
            context.contentResolver
                .openInputStream(uri)
                ?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.DirectionsCar,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}

private fun navigasyonAc(
    context: Context,
    guzergah: String
) {
    val hedef = guzergah.trim()
    val uri = if (hedef.isBlank()) {
        Uri.parse("geo:0,0?q=Akseki Antalya")
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(hedef)}")
    }

    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri)
        )
    }
}

private fun saat(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun sureHesapla(
    baslangic: Long,
    bitis: Long
): String {
    val dakika = ((bitis - baslangic) / 60000L).coerceAtLeast(0L)
    return "${dakika / 60} saat ${dakika % 60} dakika"
}

private fun sureDakika(sure: String): Int {
    val regex = Regex("""(\d+)\s*saat\s*(\d+)\s*dakika""")
    val match = regex.find(sure) ?: return 0
    return (match.groupValues[1].toIntOrNull() ?: 0) * 60 +
            (match.groupValues[2].toIntOrNull() ?: 0)
}

private fun formatKm(value: Int): String {
    val symbols = DecimalFormatSymbols(Locale("tr", "TR"))
    val formatter = DecimalFormat("#,###", symbols)
    return formatter.format(value)
}

private fun kaydetSeferler(
    context: Context,
    list: List<Sefer>
) {
    val veri = list.joinToString("\u001E") {
        listOf(
            it.id,
            it.guzergah,
            it.cikisKm,
            it.donusKm,
            it.cikisSaati,
            it.donusSaati,
            it.toplamKm,
            it.toplamSure,
            it.notMetni,
            it.tarih
        ).joinToString("\u001F")
    }

    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .edit()
        .putString("seferler", veri)
        .apply()
}

private fun yukleSeferler(
    context: Context
): List<Sefer> {
    val veri = context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .getString("seferler", "")
        ?: ""

    if (veri.isBlank()) return emptyList()

    return veri.split("\u001E").mapNotNull { satir ->
        val p = satir.split("\u001F")

        try {
            when {
                p.size >= 10 -> Sefer(
                    p[0].toLong(),
                    p[1],
                    p[2].toInt(),
                    p[3].toInt(),
                    p[4],
                    p[5],
                    p[6].toInt(),
                    p[7],
                    p[8],
                    p[9]
                )

                p.size == 9 -> Sefer(
                    p[0].toLong(),
                    p[1],
                    p[2].toInt(),
                    p[3].toInt(),
                    p[4],
                    p[5],
                    p[6].toInt(),
                    p[7],
                    p[8],
                    ""
                )

                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

private fun kaydetAraclar(
    context: Context,
    list: List<Arac>
) {
    val veri = list.joinToString("\u001E") {
        listOf(
            it.id,
            it.ad,
            it.plaka,
            it.fotoUri,
            it.yakit,
            it.sonDolum,
            it.sonKm,
            it.secili
        ).joinToString("\u001F")
    }

    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .edit()
        .putString("araclar", veri)
        .apply()
}

private fun yukleAraclar(
    context: Context
): List<Arac> {
    val veri = context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .getString("araclar", "")
        ?: ""

    if (veri.isBlank()) return emptyList()

    return veri.split("\u001E").mapNotNull { satir ->
        val p = satir.split("\u001F")

        try {
            when {
                p.size >= 8 -> Arac(
                    p[0].toLong(),
                    p[1],
                    p[2],
                    p[3],
                    p[4].toInt(),
                    p[5],
                    p[6].toInt(),
                    p[7].toBoolean()
                )

                p.size == 5 -> Arac(
                    p[0].toLong(),
                    p[1],
                    p[2],
                    p[3],
                    100,
                    "",
                    0,
                    p[4].toBoolean()
                )

                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

private fun kaydetNotlar(
    context: Context,
    notlar: String
) {
    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .edit()
        .putString("genel_notlar", notlar)
        .apply()
}

private fun yukleNotlar(
    context: Context
): String {
    return context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .getString("genel_notlar", "")
        ?: ""
}
