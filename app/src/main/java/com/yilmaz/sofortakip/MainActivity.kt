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
import androidx.compose.foundation.text.BasicTextField
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

data class AktifSefer(
    val guzergah: String,
    val cikisKm: Int,
    val cikisSaati: String,
    val baslangic: Long,
    val notMetni: String = ""
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

    val kayitliAktifSefer = remember { yukleAktifSefer(context) }

    var aktif by remember { mutableStateOf(kayitliAktifSefer != null) }
    var guzergah by remember { mutableStateOf(kayitliAktifSefer?.guzergah ?: "") }
    var cikisKm by remember {
        mutableStateOf(kayitliAktifSefer?.cikisKm?.toString() ?: "")
    }
    var donusKm by remember { mutableStateOf("") }
    var notMetni by remember { mutableStateOf(kayitliAktifSefer?.notMetni ?: "") }
    var cikisSaati by remember { mutableStateOf(kayitliAktifSefer?.cikisSaati ?: "") }
    var baslangic by remember { mutableStateOf(kayitliAktifSefer?.baslangic ?: 0L) }

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

                                kaydetAktifSefer(
                                    context,
                                    AktifSefer(
                                        guzergah = guzergah.trim(),
                                        cikisKm = cikisKm.toInt(),
                                        cikisSaati = cikisSaati,
                                        baslangic = baslangic,
                                        notMetni = notMetni.trim()
                                    )
                                )
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

                        silAktifSefer(context)

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                onThemeSelect = { temaMenu = false; onThemeSelect(it) },
                onHome = { anaMenu = false },
                onHistory = { anaMenu = false; onHistory() },
                onReports = { anaMenu = false; onReports() },
                onVehicles = { anaMenu = false; onVehicles() },
                onSettings = { anaMenu = false; onSettings() }
            )
        }

        item { WelcomeBanner(seciliArac, seferler.size, toplamKm) }

        item {
            StatsRow(
                seferler.size,
                toplamKm,
                toplamSureDakika,
                ortalamaKm,
                seciliArac?.yakit ?: 0
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

        item { SectionTitle(Icons.Default.Apps, "Hızlı İşlemler") }

        item {
            QuickActions(
                onNavigation,
                onNotes,
                onReports,
                onSettings
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "Sefer Geçmişi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onHistory,
                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)
                ) {
                    Text("Tümünü Gör", fontSize = 11.sp)
                }
            }
        }

        if (seferler.isEmpty()) {
            item { EmptyCard() }
        } else {
            items(seferler.take(2), key = { it.id }) { sefer ->
                HomeTripCard(sefer = sefer, arac = seciliArac)
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
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenu,
                modifier = Modifier.size(43.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Menu, "Menü", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, "Şoför Takip", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.width(7.dp))

            Column(Modifier.weight(1f)) {
                Text("Şoför Takip", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text("Günlük sefer paneli", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            IconButton(onClick = onNotes, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Note, "Notlar", modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onTheme, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Palette, "Tema", modifier = Modifier.size(24.dp))
            }
        }

        DropdownMenu(expanded = anaMenu, onDismissRequest = onDismissMenu) {
            DropdownMenuItem(text = { Text("Ana Sayfa") }, onClick = onHome)
            DropdownMenuItem(text = { Text("Geçmiş") }, onClick = onHistory)
            DropdownMenuItem(text = { Text("Raporlar") }, onClick = onReports)
            DropdownMenuItem(text = { Text("Araçlar") }, onClick = onVehicles)
            DropdownMenuItem(text = { Text("Ayarlar") }, onClick = onSettings)
        }

        DropdownMenu(expanded = themeMenu, onDismissRequest = onDismissTheme) {
            DropdownMenuItem(text = { Text("Açık") }, onClick = { onThemeSelect(Tema.ACIK) })
            DropdownMenuItem(text = { Text("Mavi") }, onClick = { onThemeSelect(Tema.MAVİ) })
            DropdownMenuItem(text = { Text("Gece") }, onClick = { onThemeSelect(Tema.GECE) })
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
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(19.dp)),
                color = Color.White.copy(alpha = 0.13f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (arac?.fotoUri?.isNotBlank() == true) {
                        UriImage(
                            Uri.parse(arac.fotoUri),
                            Modifier.fillMaxSize().clip(RoundedCornerShape(19.dp))
                        )
                    } else {
                        Icon(Icons.Default.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(58.dp))
                    }
                }
            }

            Spacer(Modifier.width(9.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Bugün hazır\nmısınız?",
                    fontSize = 21.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (arac == null) "Aracınızı ekleyerek başlayın." else "${arac.ad} • ${arac.plaka}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "$seferSayisi sefer • ${formatKm(toplamKm)} KM",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Icon(
                Icons.Default.Route,
                null,
                tint = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.size(58.dp)
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
        modifier = modifier.height(75.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                Modifier.size(32.dp),
                RoundedCornerShape(11.dp),
                color = iconColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
        modifier = modifier.height(209.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (aktif) "Sefer Devam Ediyor" else "Yeni Sefer",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                if (aktif) "Seferiniz devam ediyor" else "Seferinizi başlatın",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            SelectionField(
                Icons.Default.Place,
                guzergah.ifBlank { "Güzergah seçin" },
                guzergah.isBlank(),
                onGuzergahDialog
            )

            Surface(
                modifier = Modifier.fillMaxWidth().height(30.dp),
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.surface,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = cikisKm,
                        onValueChange = { onCikisKm(it.filter(Char::isDigit)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            if (cikisKm.isBlank()) {
                                Text("Çıkış KM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    )
                }
            }

            SelectionField(
                Icons.Default.AccessTime,
                cikisSaati,
                false,
                {}
            )

            Button(
                onClick = if (aktif) onFinish else onStart,
                modifier = Modifier.fillMaxWidth().height(34.dp),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (aktif) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (aktif) Icons.Default.Stop else Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    if (aktif) "SEFERİ BİTİR" else "SEFERİ BAŞLAT",
                    fontSize = 10.sp,
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
        modifier = Modifier.fillMaxWidth().height(30.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surface,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text,
                Modifier.weight(1f),
                fontSize = 10.sp,
                color = if (placeholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
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
        modifier = modifier.height(209.dp).fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(5.dp))
                Text("Araç Bilgisi", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.MoreHoriz, "Araç seç", modifier = Modifier.size(21.dp))
            }

            if (arac == null) {
                Box(Modifier.fillMaxWidth().height(78.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(55.dp))
                }
                Text("Araç seçin", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("Araç eklemek veya seçmek için
dokunun.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            } else {
                if (arac.fotoUri.isNotBlank()) {
                    UriImage(Uri.parse(arac.fotoUri), Modifier.fillMaxWidth().height(62.dp).clip(RoundedCornerShape(12.dp)))
                } else {
                    Surface(Modifier.fillMaxWidth().height(62.dp), RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                        }
                    }
                }
                Text(arac.ad, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                VehicleInfoLine("Plaka", arac.plaka)
                VehicleInfoLine("Yakıt Tipi", yakitTipi(arac.ad, arac.sonDolum))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Yakıt", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text("%${arac.yakit}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF25B85A))
                }
                LinearProgressIndicator(
                    progress = arac.yakit.coerceIn(0, 100) / 100f,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF25B85A),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                VehicleInfoLine("Km", "${formatKm(arac.sonKm)} km")
                VehicleInfoLine("Periyodik Bakım", "12.500 km")
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
    Row(
        modifier = Modifier.height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(5.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
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
        modifier = modifier.height(77.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.fillMaxSize().padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(Modifier.size(34.dp), RoundedCornerShape(11.dp), color = iconColor.copy(alpha = 0.12f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun HomeTripCard(
    sefer: Sefer,
    arac: Arac?
) {
    Card(
        Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxSize().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (arac?.fotoUri?.isNotBlank() == true) {
                UriImage(Uri.parse(arac.fotoUri), Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Surface(Modifier.size(42.dp), RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Route, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
                    }
                }
            }
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f)) {
                Text(sefer.guzergah, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(9.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(sefer.tarih, fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(9.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(sefer.cikisSaati, fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text("Süre: ${sefer.toplamSure} • Ort. Hız: --", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Surface(RoundedCornerShape(9.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                Text("${formatKm(sefer.toplamKm)} KM", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun EmptyCard() {
    Card(
        Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.History, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Henüz tamamlanmış sefer yok.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun kaydetAktifSefer(
    context: Context,
    aktifSefer: AktifSefer
) {
    val veri = listOf(
        aktifSefer.guzergah,
        aktifSefer.cikisKm,
        aktifSefer.cikisSaati,
        aktifSefer.baslangic,
        aktifSefer.notMetni
    ).joinToString("\u001F")

    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .edit()
        .putString("aktif_sefer", veri)
        .apply()
}

private fun yukleAktifSefer(
    context: Context
): AktifSefer? {
    val veri = context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .getString("aktif_sefer", "")
        ?: ""

    if (veri.isBlank()) return null

    val p = veri.split("\u001F")

    return try {
        if (p.size >= 4) {
            AktifSefer(
                guzergah = p[0],
                cikisKm = p[1].toInt(),
                cikisSaati = p[2],
                baslangic = p[3].toLong(),
                notMetni = p.getOrElse(4) { "" }
            )
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun silAktifSefer(
    context: Context
) {
    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .edit()
        .remove("aktif_sefer")
        .apply()
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
