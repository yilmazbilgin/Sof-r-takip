package com.yilmaz.sofortakip

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val tarih: String = ""
)

data class Arac(
    val id: Long,
    val ad: String,
    val plaka: String,
    val fotoUri: String = "",
    val secili: Boolean = false
)

private enum class Tema { ACIK, MAVİ, GECE }

private data class AktifSefer(
    val guzergah: String,
    val cikisKm: Int,
    val cikisSaati: String,
    val baslangic: Long
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SoforTakip() }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SoforTakip() {
    val context = LocalContext.current

    var seferler by remember { mutableStateOf(yukleSeferler(context)) }
    var notlar by remember { mutableStateOf(yukleNotlar(context)) }
    var araclar by remember { mutableStateOf(yukleAraclar(context)) }
    var tema by remember { mutableStateOf(yukleTema(context)) }

    var sekme by remember { mutableStateOf(0) }
    var temaMenu by remember { mutableStateOf(false) }
    var notlarAcik by remember { mutableStateOf(false) }
    var araclarAcik by remember { mutableStateOf(false) }
    var bitirDialog by remember { mutableStateOf(false) }
    var bilgi by remember { mutableStateOf("") }
    var silinecek by remember { mutableStateOf<Sefer?>(null) }

    val kayitliAktif = remember { yukleAktif(context) }
    var aktif by remember { mutableStateOf(kayitliAktif != null) }
    var guzergah by remember { mutableStateOf(kayitliAktif?.guzergah ?: "") }
    var cikisKm by remember { mutableStateOf(kayitliAktif?.cikisKm?.toString() ?: "") }
    var donusKm by remember { mutableStateOf("") }
    var notMetni by remember { mutableStateOf("") }
    var cikisSaati by remember { mutableStateOf(kayitliAktif?.cikisSaati ?: "") }
    var baslangic by remember { mutableStateOf(kayitliAktif?.baslangic ?: 0L) }

    val seciliArac = araclar.firstOrNull { it.secili } ?: araclar.firstOrNull()
    val toplamKm = seferler.sumOf { it.toplamKm }
    val toplamSureDakika = seferler.sumOf { sureDakika(it.toplamSure) }
    val ortalamaKm = if (seferler.isEmpty()) 0 else toplamKm / seferler.size

    val scheme = when (tema) {
        Tema.ACIK -> lightColorScheme(
            primary = Color(0xFF5B35C7),
            secondary = Color(0xFF7B55E8),
            background = Color(0xFFF7F5FC),
            surfaceVariant = Color(0xFFEDE8F8)
        )
        Tema.MAVİ -> lightColorScheme(
            primary = Color(0xFF087EA4),
            secondary = Color(0xFF00A6C7),
            background = Color(0xFFF2F8FA),
            surfaceVariant = Color(0xFFE4EFF3)
        )
        Tema.GECE -> darkColorScheme(
            primary = Color(0xFFB39DFF),
            secondary = Color(0xFF80CBC4),
            background = Color(0xFF0B1016),
            surface = Color(0xFF121A23),
            surfaceVariant = Color(0xFF1C2733)
        )
    }

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Şoför Takip",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                when (sekme) {
                                    1 -> "Sefer geçmişi"
                                    2 -> "Raporlar"
                                    3 -> "Uygulama ayarları"
                                    else -> "Günlük sefer paneli"
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { notlarAcik = true }) {
                            Icon(Icons.Default.Note, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Notlar", fontWeight = FontWeight.Bold)
                        }
                        Box {
                            IconButton(onClick = { temaMenu = true }) {
                                Icon(Icons.Default.Palette, "Tema")
                            }
                            DropdownMenu(
                                expanded = temaMenu,
                                onDismissRequest = { temaMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Açık Tema") },
                                    onClick = {
                                        tema = Tema.ACIK
                                        kaydetTema(context, tema)
                                        temaMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mavi Tema") },
                                    onClick = {
                                        tema = Tema.MAVİ
                                        kaydetTema(context, tema)
                                        temaMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gece Tema") },
                                    onClick = {
                                        tema = Tema.GECE
                                        kaydetTema(context, tema)
                                        temaMenu = false
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = sekme == 0,
                        onClick = { sekme = 0 },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Ana Sayfa") }
                    )
                    NavigationBarItem(
                        selected = sekme == 1,
                        onClick = { sekme = 1 },
                        icon = { Icon(Icons.Default.History, null) },
                        label = { Text("Geçmiş") }
                    )
                    NavigationBarItem(
                        selected = sekme == 2,
                        onClick = { sekme = 2 },
                        icon = { Icon(Icons.Default.BarChart, null) },
                        label = { Text("Raporlar") }
                    )
                    NavigationBarItem(
                        selected = sekme == 3,
                        onClick = { sekme = 3 },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Ayarlar") }
                    )
                }
            }
        ) { padding ->
            when (sekme) {
                0 -> AnaSayfa(
                    modifier = Modifier.padding(padding),
                    aktif = aktif,
                    guzergah = guzergah,
                    cikisKm = cikisKm,
                    cikisSaati = cikisSaati,
                    seferler = seferler,
                    toplamKm = toplamKm,
                    ortalamaKm = ortalamaKm,
                    seciliArac = seciliArac,
                    onGuzergah = { guzergah = it },
                    onKm = { cikisKm = it },
                    onStart = {
                        when {
                            guzergah.isBlank() -> bilgi = "Lütfen güzergah girin."
                            cikisKm.toIntOrNull() == null ->
                                bilgi = "Geçerli bir çıkış KM girin."
                            else -> {
                                val saatNow = saat()
                                val bas = System.currentTimeMillis()
                                aktif = true
                                cikisSaati = saatNow
                                baslangic = bas
                                kaydetAktif(
                                    context,
                                    AktifSefer(
                                        guzergah.trim(),
                                        cikisKm.toInt(),
                                        saatNow,
                                        bas
                                    )
                                )
                            }
                        }
                    },
                    onFinish = { bitirDialog = true },
                    onNotes = { notlarAcik = true },
                    onVehicles = { araclarAcik = true },
                    onReports = { sekme = 2 },
                    onSettings = { sekme = 3 },
                    onNavigate = { rota ->
                        acNavigasyon(context, rota)
                    }
                )

                1 -> GecmisSayfasi(
                    modifier = Modifier.padding(padding),
                    seferler = seferler,
                    onDelete = { silinecek = it }
                )

                2 -> RaporlarSayfasi(
                    modifier = Modifier.padding(padding),
                    seferler = seferler,
                    toplamKm = toplamKm,
                    toplamSureDakika = toplamSureDakika,
                    ortalamaKm = ortalamaKm
                )

                else -> AyarlarSayfasi(
                    modifier = Modifier.padding(padding),
                    aracSayisi = araclar.size,
                    tema = tema,
                    onVehicles = { araclarAcik = true },
                    onTheme = { temaMenu = true },
                    onNotes = { notlarAcik = true }
                )
            }
        }

        if (bilgi.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { bilgi = "" },
                title = { Text("Bilgi") },
                text = { Text(bilgi) },
                confirmButton = {
                    TextButton(onClick = { bilgi = "" }) { Text("Tamam") }
                }
            )
        }

        if (araclarAcik) {
            AraclarDialog(
                araclar = araclar,
                onClose = { araclarAcik = false },
                onSave = { yeni ->
                    if (araclar.isEmpty()) {
                        araclar = listOf(yeni.copy(secili = true))
                    } else {
                        araclar = araclar + yeni
                    }
                    kaydetAraclar(context, araclar)
                },
                onSelect = { secilecek ->
                    araclar = araclar.map { it.copy(secili = it.id == secilecek.id) }
                    kaydetAraclar(context, araclar)
                },
                onDelete = { silinecekArac ->
                    araclar = araclar.filter { it.id != silinecekArac.id }
                    if (araclar.isNotEmpty() && araclar.none { it.secili }) {
                        araclar = araclar.mapIndexed { index, arac ->
                            arac.copy(secili = index == 0)
                        }
                    }
                    kaydetAraclar(context, araclar)
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
                                tarih = SimpleDateFormat(
                                    "dd.MM.yyyy",
                                    Locale.getDefault()
                                ).format(Date())
                            )

                            seferler = listOf(yeni) + seferler
                            kaydetSeferler(context, seferler)
                            aktif = false
                            temizleAktif(context)
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

        silinecek?.let { sefer ->
            AlertDialog(
                onDismissRequest = { silinecek = null },
                title = { Text("Seferi sil", fontWeight = FontWeight.Bold) },
                text = {
                    Text("\"${sefer.guzergah}\" seferi geçmişten silinecek.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            seferler = seferler.filter { it.id != sefer.id }
                            kaydetSeferler(context, seferler)
                            silinecek = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(5.dp))
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
}

@Composable
private fun AnaSayfa(
    modifier: Modifier,
    aktif: Boolean,
    guzergah: String,
    cikisKm: String,
    cikisSaati: String,
    seferler: List<Sefer>,
    toplamKm: Int,
    ortalamaKm: Int,
    seciliArac: Arac?,
    onGuzergah: (String) -> Unit,
    onKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onNotes: () -> Unit,
    onVehicles: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onNavigate: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 22.dp)
    ) {
        item {
            HeroHeader(
                aktif = aktif,
                seferSayisi = seferler.size,
                toplamKm = toplamKm,
                seciliArac = seciliArac
            )
        }

        item {
            StatsRow(
                seferSayisi = seferler.size,
                toplamKm = toplamKm,
                toplamSure = formatSureDakika(
                    seferler.sumOf { sureDakika(it.toplamSure) }
                ),
                ortalamaKm = ortalamaKm
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallAction(
                    Modifier.weight(1f),
                    Icons.Default.Navigation,
                    "Navigasyon",
                    "Harita aç",
                    onClick = { onNavigate(guzergah) }
                )
                SmallAction(
                    Modifier.weight(1f),
                    Icons.Default.Note,
                    "Not Ekle",
                    "Hızlı not",
                    onNotes
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallAction(
                    Modifier.weight(1f),
                    Icons.Default.BarChart,
                    "Raporlar",
                    "Detaylı gör",
                    onReports
                )
                SmallAction(
                    Modifier.weight(1f),
                    Icons.Default.Settings,
                    "Ayarlar",
                    "Uygulama",
                    onSettings
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Yeni Sefer",
                    Modifier.weight(1f),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                TextButton(onClick = onVehicles) {
                    Icon(Icons.Default.DirectionsCar, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Araç Bilgisi")
                }
            }
        }

        item {
            NewTripCard(
                guzergah = guzergah,
                cikisKm = cikisKm,
                cikisSaati = cikisSaati,
                aktif = aktif,
                seciliArac = seciliArac,
                onGuzergah = onGuzergah,
                onKm = onKm,
                onStart = onStart,
                onFinish = onFinish
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Sefer Geçmişi",
                    Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (seferler.isNotEmpty()) {
                    Text(
                        "${seferler.size} kayıt",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (seferler.isEmpty()) {
            item { EmptyCard() }
        } else {
            items(seferler.take(3), key = { it.id }) { sefer ->
                TripCard(sefer) { }
            }
        }
    }
}

@Composable
private fun HeroHeader(
    aktif: Boolean,
    seferSayisi: Int,
    toplamKm: Int,
    seciliArac: Arac?
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageOrCar(
                uri = seciliArac?.fotoUri.orEmpty(),
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(22.dp)),
                iconSize = 42
            )

            Spacer(Modifier.width(15.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    if (aktif) "Sefer devam ediyor" else "Bugün hazır mısınız?",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (seciliArac != null) {
                        "${seciliArac.ad} • ${seciliArac.plaka}"
                    } else {
                        "Aracınızı ekleyerek başlayın."
                    },
                    color = Color.White.copy(alpha = .88f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$seferSayisi sefer • $toplamKm KM",
                    color = Color.White.copy(alpha = .78f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    seferSayisi: Int,
    toplamKm: Int,
    toplamSure: String,
    ortalamaKm: Int
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        StatChip(
            Icons.Default.DirectionsCar,
            seferSayisi.toString(),
            "Sefer",
            Color(0xFF5B35C7)
        )
        StatChip(
            Icons.Default.Speed,
            toplamKm.toString(),
            "Toplam KM",
            Color(0xFF087EA4)
        )
        StatChip(
            Icons.Default.Timer,
            toplamSure,
            "Toplam Süre",
            Color(0xFF159A70)
        )
        StatChip(
            Icons.Default.Speed,
            ortalamaKm.toString(),
            "Ort. KM",
            Color(0xFFED8B18)
        )
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        Modifier.width(130.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(13.dp)) {
            Surface(
                Modifier.size(36.dp),
                RoundedCornerShape(11.dp),
                color = color.copy(alpha = .12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmallAction(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(7.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NewTripCard(
    guzergah: String,
    cikisKm: String,
    cikisSaati: String,
    aktif: Boolean,
    seciliArac: Arac?,
    onGuzergah: (String) -> Unit,
    onKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.size(46.dp),
                    RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AddLocation,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(11.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        if (aktif) "Aktif Sefer" else "Yeni Sefer",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        if (seciliArac == null)
                            "Çıkış bilgilerini girin"
                        else
                            "${seciliArac.ad} • ${seciliArac.plaka}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (seciliArac != null) {
                    ImageOrCar(
                        uri = seciliArac.fotoUri,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        iconSize = 27
                    )
                }
            }

            OutlinedTextField(
                value = guzergah,
                onValueChange = onGuzergah,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Güzergah") },
                placeholder = { Text("Örn. Akseki - Antalya") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                singleLine = true,
                enabled = !aktif,
                shape = RoundedCornerShape(17.dp)
            )

            OutlinedTextField(
                value = cikisKm,
                onValueChange = { onKm(it.filter(Char::isDigit)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Çıkış KM") },
                placeholder = { Text("Örn. 125430") },
                leadingIcon = { Icon(Icons.Default.Speed, null) },
                singleLine = true,
                enabled = !aktif,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(17.dp)
            )

            if (aktif) {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFF159A70).copy(alpha = .10f)
                ) {
                    Row(
                        Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            null,
                            tint = Color(0xFF159A70)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Sefer başladı", fontWeight = FontWeight.Bold)
                            Text(
                                "Çıkış saati: $cikisSaati",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("SEFERİ BİTİR", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "SEFERİ BAŞLAT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
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
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 22.dp)
    ) {
        item {
            Text(
                "Sefer Geçmişi",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "${seferler.size} tamamlanmış sefer",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (seferler.isEmpty()) {
            item { EmptyCard() }
        } else {
            items(seferler, key = { it.id }) { sefer ->
                TripCard(sefer) { onDelete(sefer) }
            }
        }
    }
}

@Composable
private fun RaporlarSayfasi(
    modifier: Modifier,
    seferler: List<Sefer>,
    toplamKm: Int,
    toplamSureDakika: Int,
    ortalamaKm: Int
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 22.dp)
    ) {
        item {
            Text(
                "Raporlar",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Sefer performansınız",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ReportCard(
                Icons.Default.DirectionsCar,
                seferler.size.toString(),
                "Toplam Sefer"
            )
        }

        item {
            ReportCard(
                Icons.Default.Speed,
                "$toplamKm KM",
                "Toplam Mesafe"
            )
        }

        item {
            ReportCard(
                Icons.Default.Timer,
                formatSureDakika(toplamSureDakika),
                "Toplam Süre"
            )
        }

        item {
            ReportCard(
                Icons.Default.TrendingUp,
                "$ortalamaKm KM",
                "Ortalama Sefer Mesafesi"
            )
        }

        if (seferler.isNotEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(23.dp)
                ) {
                    Column(Modifier.padding(17.dp)) {
                        Text(
                            "Son Seferler",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(10.dp))

                        seferler.take(5).forEach {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(it.guzergah, fontWeight = FontWeight.Bold)
                                    Text(
                                        it.tarih.ifBlank { "-" },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${it.toplamKm} KM",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                Modifier.size(48.dp),
                RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(13.dp))

            Column {
                Text(
                    value,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AyarlarSayfasi(
    modifier: Modifier,
    aracSayisi: Int,
    tema: Tema,
    onVehicles: () -> Unit,
    onTheme: () -> Unit,
    onNotes: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Ayarlar",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "Uygulama ve araç ayarları",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingCard(
            Icons.Default.DirectionsCar,
            "Araçlarım",
            "$aracSayisi kayıtlı araç",
            onVehicles
        )

        SettingCard(
            Icons.Default.Palette,
            "Tema",
            when (tema) {
                Tema.ACIK -> "Açık"
                Tema.MAVİ -> "Mavi"
                Tema.GECE -> "Gece"
            },
            onTheme
        )

        SettingCard(
            Icons.Default.Note,
            "Genel Notlar",
            "Notlarınızı düzenleyin",
            onNotes
        )
    }
}

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(21.dp)
    ) {
        Row(
            Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                Modifier.size(48.dp),
                RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(13.dp))

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
        title = { Text("Seferi Tamamla", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    guzergah,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Çıkış: $cikisKm KM • $cikisSaati",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = donusKm,
                    onValueChange = { onDonusKm(it.filter(Char::isDigit)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dönüş KM") },
                    leadingIcon = { Icon(Icons.Default.Speed, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedTextField(
                    value = notMetni,
                    onValueChange = onNot,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sefer Notu") },
                    leadingIcon = { Icon(Icons.Default.Description, null) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(6.dp))
                Text("KAYDET")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("VAZGEÇ") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripCard(
    sefer: Sefer,
    onLongClick: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(23.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.size(45.dp),
                    RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .09f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        sefer.guzergah,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${sefer.tarih.ifBlank { "Tarih yok" }} • " +
                                "${sefer.cikisSaati} → ${sefer.donusSaati}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF159A70).copy(alpha = .12f)
                ) {
                    Text(
                        "Tamamlandı",
                        Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),
                        fontSize = 10.sp,
                        color = Color(0xFF159A70),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth()) {
                InfoColumn(
                    Modifier.weight(1f),
                    "KM",
                    "${sefer.cikisKm} → ${sefer.donusKm}"
                )
                InfoColumn(
                    Modifier.weight(1f),
                    "Mesafe",
                    "${sefer.toplamKm} KM"
                )
                InfoColumn(
                    Modifier.weight(1f),
                    "Süre",
                    sefer.toplamSure
                )
            }

            if (sefer.notMetni.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Not: ${sefer.notMetni}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoColumn(
    modifier: Modifier,
    label: String,
    value: String
) {
    Column(modifier) {
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyCard() {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.History,
                null,
                Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "Henüz tamamlanmış sefer yok.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Note,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Notlar", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            OutlinedTextField(
                value = metin,
                onValueChange = { metin = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Not") },
                placeholder = { Text("Genel notunuzu yazın...") },
                minLines = 6,
                maxLines = 10,
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            Button(onClick = { onSave(metin); onClose() }) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(5.dp))
                Text("KAYDET")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("VAZGEÇ") }
        }
    )
}

@Composable
private fun AraclarDialog(
    araclar: List<Arac>,
    onClose: () -> Unit,
    onSave: (Arac) -> Unit,
    onSelect: (Arac) -> Unit,
    onDelete: (Arac) -> Unit
) {
    val context = LocalContext.current
    var aracAdi by remember { mutableStateOf("") }
    var plaka by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            fotoUri = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                "Araçlarım",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (araclar.isNotEmpty()) {
                    araclar.forEach { arac ->
                        Card(
                            Modifier.fillMaxWidth(),
                            onClick = { onSelect(arac) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (arac.secili) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ImageOrCar(
                                    uri = arac.fotoUri,
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(13.dp)),
                                    iconSize = 30
                                )

                                Spacer(Modifier.width(10.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        arac.ad,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        arac.plaka,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (arac.secili) {
                                        Text(
                                            "Seçili araç",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                TextButton(onClick = { onDelete(arac) }) {
                                    Text("Sil")
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                }

                Text(
                    "Yeni Araç",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImageOrCar(
                        uri = fotoUri,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(17.dp)),
                        iconSize = 34
                    )

                    Spacer(Modifier.width(10.dp))

                    OutlinedButton(
                        onClick = {
                            launcher.launch(arrayOf("image/*"))
                        }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Fotoğraf seç")
                    }
                }

                OutlinedTextField(
                    value = aracAdi,
                    onValueChange = { aracAdi = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Araç adı") },
                    placeholder = { Text("Örn. Ford Transit") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = plaka,
                    onValueChange = {
                        plaka = it.uppercase(Locale.getDefault())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Plaka") },
                    placeholder = { Text("Örn. 07 AB 123") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (aracAdi.isNotBlank() && plaka.isNotBlank()) {
                        onSave(
                            Arac(
                                id = System.currentTimeMillis(),
                                ad = aracAdi.trim(),
                                plaka = plaka.trim(),
                                fotoUri = fotoUri,
                                secili = araclar.isEmpty()
                            )
                        )
                        aracAdi = ""
                        plaka = ""
                        fotoUri = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(5.dp))
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
private fun ImageOrCar(
    uri: String,
    modifier: Modifier,
    iconSize: Int
) {
    val context = LocalContext.current
    var bitmap by remember(uri) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(uri) {
        bitmap = if (uri.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver
                        .openInputStream(Uri.parse(uri))
                        ?.use { BitmapFactory.decodeStream(it) }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Araç fotoğrafı",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.DirectionsCar,
                    null,
                    Modifier.size(iconSize.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun acNavigasyon(context: Context, guzergah: String) {
    val hedef = guzergah.trim()

    if (hedef.isBlank()) {
        android.widget.Toast
            .makeText(context, "Önce güzergah girin.", android.widget.Toast.LENGTH_SHORT)
            .show()
        return
    }

    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=" +
            Uri.encode(hedef)
    )

    val intent = Intent(Intent.ACTION_VIEW, uri)

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(hedef)}")
        )
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            android.widget.Toast
                .makeText(
                    context,
                    "Harita uygulaması bulunamadı.",
                    android.widget.Toast.LENGTH_SHORT
                )
                .show()
        }
    }
}

private fun saat(): String =
    SimpleDateFormat(
        "HH:mm",
        Locale.getDefault()
    ).format(Date())

private fun sureHesapla(
    baslangic: Long,
    bitis: Long
): String {
    if (baslangic <= 0L) return "0 saat 0 dakika"

    val dakika = ((bitis - baslangic) / 60000L)
        .coerceAtLeast(0L)

    return "${dakika / 60} saat ${dakika % 60} dakika"
}

private fun sureDakika(sure: String): Int {
    val saat = Regex("(\\d+)\\s*saat")
        .find(sure)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull() ?: 0

    val dakika = Regex("(\\d+)\\s*dakika")
        .find(sure)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull() ?: 0

    return saat * 60 + dakika
}

private fun formatSureDakika(dakika: Int): String =
    "${dakika / 60}s ${dakika % 60}dk"

private fun kaydetSeferler(
    context: Context,
    seferler: List<Sefer>
) {
    val veri = seferler.joinToString("\u001E") {
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
        .getString("seferler", "") ?: ""

    if (veri.isBlank()) return emptyList()

    return veri
        .split("\u001E")
        .mapNotNull { satir ->
            val p = satir.split("\u001F")

            try {
                when {
                    p.size == 9 -> Sefer(
                        id = p[0].toLong(),
                        guzergah = p[1],
                        cikisKm = p[2].toInt(),
                        donusKm = p[3].toInt(),
                        cikisSaati = p[4],
                        donusSaati = p[5],
                        toplamKm = p[6].toInt(),
                        toplamSure = p[7],
                        notMetni = p[8]
                    )

                    p.size >= 10 -> Sefer(
                        id = p[0].toLong(),
                        guzergah = p[1],
                        cikisKm = p[2].toInt(),
                        donusKm = p[3].toInt(),
                        cikisSaati = p[4],
                        donusSaati = p[5],
                        toplamKm = p[6].toInt(),
                        toplamSure = p[7],
                        notMetni = p[8],
                        tarih = p[9]
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
): String =
    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .getString("genel_notlar", "") ?: ""

private fun kaydetAraclar(
    context: Context,
    araclar: List<Arac>
) {
    val veri = araclar.joinToString("\u001E") {
        listOf(
            it.id,
            it.ad,
            it.plaka,
            it.fotoUri,
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
        .getString("araclar", "") ?: ""

    if (veri.isBlank()) return emptyList()

    return veri
        .split("\u001E")
        .mapNotNull { satir ->
            val p = satir.split("\u001F")

            if (p.size != 5) {
                return@mapNotNull null
            }

            try {
                Arac(
                    id = p[0].toLong(),
                    ad = p[1],
                    plaka = p[2],
                    fotoUri = p[3],
                    secili = p[4].toBoolean()
                )
            } catch (_: Exception) {
                null
            }
        }
}

private fun kaydetTema(
    context: Context,
    tema: Tema
) {
    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .edit()
        .putString("tema", tema.name)
        .apply()
}

private fun yukleTema(
    context: Context
): Tema {
    return try {
        Tema.valueOf(
            context.getSharedPreferences(
                "sofor_takip",
                Context.MODE_PRIVATE
            )
                .getString("tema", Tema.ACIK.name)
                ?: Tema.ACIK.name
        )
    } catch (_: Exception) {
        Tema.ACIK
    }
}

private fun kaydetAktif(
    context: Context,
    aktif: AktifSefer
) {
    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .edit()
        .putBoolean("aktif", true)
        .putString("aktif_guzergah", aktif.guzergah)
        .putInt("aktif_cikis_km", aktif.cikisKm)
        .putString("aktif_cikis_saati", aktif.cikisSaati)
        .putLong("aktif_baslangic", aktif.baslangic)
        .apply()
}

private fun yukleAktif(
    context: Context
): AktifSefer? {
    val p = context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )

    if (!p.getBoolean("aktif", false)) return null

    val guzergah = p.getString("aktif_guzergah", "") ?: ""
    val km = p.getInt("aktif_cikis_km", 0)
    val saat = p.getString("aktif_cikis_saati", "") ?: ""
    val bas = p.getLong("aktif_baslangic", 0L)

    if (guzergah.isBlank() || km <= 0 || bas <= 0L) {
        return null
    }

    return AktifSefer(
        guzergah,
        km,
        saat,
        bas
    )
}

private fun temizleAktif(
    context: Context
) {
    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )
        .edit()
        .remove("aktif")
        .remove("aktif_guzergah")
        .remove("aktif_cikis_km")
        .remove("aktif_cikis_saati")
        .remove("aktif_baslangic")
        .apply()
}
