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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class AktifSefer(
    val guzergah: String,
    val cikisKm: Int,
    val cikisSaati: String,
    val baslangicMs: Long
)

private enum class Tema { ACIK, MAVI, GECE }

private val Mor = Color(0xFF6424E8)
private val MorKoyu = Color(0xFF32116F)
private val Mavi = Color(0xFF138BE8)
private val Yesil = Color(0xFF08B878)
private val Turuncu = Color(0xFFFF8B13)
private val Pembe = Color(0xFFE92772)
private val Kirmizi = Color(0xFFE53935)
private val Lacivert = Color(0xFF18294F)
private val ArkaPlan = Color(0xFFF3F5FA)
private val Kart = Color(0xFFFFFFFF)
private val Metin = Color(0xFF14213D)
private val Ikincil = Color(0xFF68758A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SoforTakipApp() }
    }
}

@Composable
private fun SoforTakipApp() {
    val context = LocalContext.current

    var seferler by remember { mutableStateOf(yukleSeferler(context)) }
    var araclar by remember { mutableStateOf(yukleAraclar(context)) }
    var notlar by remember { mutableStateOf(yukleNotlar(context)) }
    var aktif by remember { mutableStateOf(yukleAktifSefer(context)) }
    var tema by remember { mutableStateOf(yukleTema(context)) }
    var sayfa by remember { mutableIntStateOf(0) }

    var guzergah by remember { mutableStateOf("") }
    var cikisKm by remember { mutableStateOf("") }
    var donusKm by remember { mutableStateOf("") }
    var seferNotu by remember { mutableStateOf("") }

    var bilgi by remember { mutableStateOf<String?>(null) }
    var notDialog by remember { mutableStateOf(false) }
    var aracDialog by remember { mutableStateOf(false) }
    var duzenlenenArac by remember { mutableStateOf<Arac?>(null) }
    var bitirDialog by remember { mutableStateOf(false) }
    var silinecek by remember { mutableStateOf<Sefer?>(null) }
    var temaMenu by remember { mutableStateOf(false) }

    val seciliArac = araclar.firstOrNull { it.secili } ?: araclar.firstOrNull()
    val toplamKm = seferler.sumOf { it.toplamKm }
    val toplamSure = seferler.sumOf { sureDakika(it.toplamSure) }
    val ortalamaKm = if (seferler.isEmpty()) 0 else toplamKm / seferler.size
    val bugun = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
    val gun = SimpleDateFormat("dd", Locale.getDefault()).format(Date())
    val ayYil = SimpleDateFormat("MMMM yyyy", Locale("tr", "TR")).format(Date())
        .replaceFirstChar { it.uppercase() }
    val hafta = SimpleDateFormat("EEEE", Locale("tr", "TR")).format(Date())
        .replaceFirstChar { it.uppercase() }

    val colors = when (tema) {
        Tema.ACIK -> lightColorScheme(
            primary = Mor,
            onPrimary = Color.White,
            secondary = Mavi,
            background = ArkaPlan,
            surface = Kart,
            surfaceVariant = Color(0xFFEAF0FA),
            onSurface = Metin,
            onSurfaceVariant = Ikincil
        )
        Tema.MAVI -> lightColorScheme(
            primary = Color(0xFF1565C0),
            onPrimary = Color.White,
            secondary = Color(0xFF00A6C7),
            background = Color(0xFFF1F7FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFE5F0FA),
            onSurface = Metin,
            onSurfaceVariant = Ikincil
        )
        Tema.GECE -> darkColorScheme(
            primary = Color(0xFFB994FF),
            onPrimary = Color(0xFF24103F),
            secondary = Color(0xFF70C8FF),
            background = Color(0xFF0C1220),
            surface = Color(0xFF151E2E),
            surfaceVariant = Color(0xFF202C40),
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFB9C3D2)
        )
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BottomBar(
                    selected = sayfa,
                    onSelected = { sayfa = it },
                    onAdd = {
                        sayfa = 0
                        guzergah = ""
                        cikisKm = seciliArac?.sonKm?.takeIf { it > 0 }?.toString() ?: ""
                    }
                )
            }
        ) { padding ->
            when (sayfa) {
                0 -> AnaSayfa(
                    modifier = Modifier.padding(padding),
                    seferler = seferler,
                    arac = seciliArac,
                    aktif = aktif,
                    guzergah = if (aktif != null) aktif!!.guzergah else guzergah,
                    cikisKm = if (aktif != null) aktif!!.cikisKm.toString() else cikisKm,
                    toplamKm = toplamKm,
                    toplamSure = toplamSure,
                    ortalamaKm = ortalamaKm,
                    gun = gun,
                    ayYil = ayYil,
                    hafta = hafta,
                    onGuzergah = { guzergah = it },
                    onCikisKm = { cikisKm = it.filter(Char::isDigit) },
                    onStart = {
                        val km = cikisKm.toIntOrNull()
                        when {
                            seciliArac == null -> bilgi = "脰nce Ara莽 Bilgisi b枚l眉m眉nden bir ara莽 ekleyin."
                            guzergah.isBlank() -> bilgi = "L眉tfen g眉zergah girin."
                            km == null -> bilgi = "Ge莽erli bir 莽谋k谋艧 KM girin."
                            seciliArac.sonKm > 0 && km < seciliArac.sonKm ->
                                bilgi = "脟谋k谋艧 KM, arac谋n son KM bilgisinden k眉莽眉k olamaz."
                            aktif != null -> bilgi = "Zaten devam eden bir sefer var."
                            else -> {
                                val yeni = AktifSefer(
                                    guzergah.trim(), km, saat(), System.currentTimeMillis()
                                )
                                aktif = yeni
                                kaydetAktifSefer(context, yeni)
                            }
                        }
                    },
                    onFinish = { if (aktif != null) bitirDialog = true else bilgi = "Aktif bir sefer bulunmuyor." },
                    onNavigation = {
                        navigasyonAc(context, guzergah.ifBlank { seciliArac?.ad ?: "Akseki Antalya" })
                    },
                    onNotes = { notDialog = true },
                    onVehicles = { aracDialog = true },
                    onHistory = { sayfa = 1 },
                    onReports = { sayfa = 2 },
                    onSettings = { sayfa = 3 },
                    onTheme = { temaMenu = true },
                    themeMenu = temaMenu,
                    onDismissTheme = { temaMenu = false },
                    onThemeSelect = {
                        tema = it
                        temaMenu = false
                        kaydetTema(context, it)
                    }
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
                    toplamSure = toplamSure,
                    ortalamaKm = ortalamaKm
                )
                else -> AyarlarSayfasi(
                    modifier = Modifier.padding(padding),
                    tema = tema,
                    aracSayisi = araclar.size,
                    onTema = {
                        tema = it
                        kaydetTema(context, it)
                    },
                    onVehicles = { aracDialog = true },
                    onNotes = { notDialog = true }
                )
            }
        }
    }

    if (notDialog) {
        NotesDialog(
            notlar = notlar,
            onClose = { notDialog = false },
            onSave = {
                notlar = it
                kaydetNotlar(context, it)
            }
        )
    }

    if (aracDialog) {
        AraclarDialog(
            araclar = araclar,
            onClose = { aracDialog = false },
            onSave = { yeni ->
                araclar = if (araclar.isEmpty()) listOf(yeni.copy(secili = true))
                else araclar + yeni
                kaydetAraclar(context, araclar)
            },
            onDelete = { sil ->
                araclar = araclar.filterNot { it.id == sil.id }
                if (araclar.isNotEmpty() && araclar.none { it.secili }) {
                    araclar = araclar.mapIndexed { i, a -> if (i == 0) a.copy(secili = true) else a }
                }
                kaydetAraclar(context, araclar)
            },
            onSelect = { sec ->
                araclar = araclar.map { it.copy(secili = it.id == sec.id) }
                kaydetAraclar(context, araclar)
            },
            onEdit = { duzenlenenArac = it }
        )
    }

    duzenlenenArac?.let { arac ->
        AracDuzenleDialog(
            arac = arac,
            onClose = { duzenlenenArac = null },
            onSave = { guncel ->
                araclar = araclar.map { if (it.id == guncel.id) guncel else it }
                kaydetAraclar(context, araclar)
                duzenlenenArac = null
            }
        )
    }

    if (bitirDialog && aktif != null) {
        BitirDialog(
            aktif = aktif!!,
            donusKm = donusKm,
            notMetni = seferNotu,
            onDonusKm = { donusKm = it.filter(Char::isDigit) },
            onNot = { seferNotu = it },
            onClose = {
                bitirDialog = false
                donusKm = ""
                seferNotu = ""
            },
            onSave = {
                val d = donusKm.toIntOrNull()
                val c = aktif!!.cikisKm
                when {
                    d == null -> bilgi = "Ge莽erli bir d枚n眉艧 KM girin."
                    d < c -> bilgi = "D枚n眉艧 KM, 莽谋k谋艧 KM'den k眉莽眉k olamaz."
                    else -> {
                        val bitis = System.currentTimeMillis()
                        val yeni = Sefer(
                            id = bitis,
                            guzergah = aktif!!.guzergah,
                            cikisKm = c,
                            donusKm = d,
                            cikisSaati = aktif!!.cikisSaati,
                            donusSaati = saat(),
                            toplamKm = d - c,
                            toplamSure = sureHesapla(aktif!!.baslangicMs, bitis),
                            notMetni = seferNotu.trim(),
                            tarih = bugun
                        )
                        seferler = listOf(yeni) + seferler
                        kaydetSeferler(context, seferler)

                        seciliArac?.let { sec ->
                            araclar = araclar.map { a ->
                                if (a.id == sec.id) a.copy(sonKm = d) else a
                            }
                            kaydetAraclar(context, araclar)
                        }

                        aktif = null
                        kaydetAktifSefer(context, null)
                        bitirDialog = false
                        guzergah = ""
                        cikisKm = ""
                        donusKm = ""
                        seferNotu = ""
                    }
                }
            }
        )
    }

    bilgi?.let { mesaj ->
        AlertDialog(
            onDismissRequest = { bilgi = null },
            icon = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Bilgi", fontWeight = FontWeight.ExtraBold) },
            text = { Text(mesaj) },
            confirmButton = { Button(onClick = { bilgi = null }) { Text("Tamam") } }
        )
    }

    silinecek?.let { sefer ->
        AlertDialog(
            onDismissRequest = { silinecek = null },
            icon = { Icon(Icons.Default.Delete, null, tint = Kirmizi) },
            title = { Text("Seferi sil", fontWeight = FontWeight.ExtraBold) },
            text = { Text("\"${sefer.guzergah}\" seferi ge莽mi艧ten silinecek.") },
            confirmButton = {
                Button(
                    onClick = {
                        seferler = seferler.filterNot { it.id == sefer.id }
                        kaydetSeferler(context, seferler)
                        silinecek = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Kirmizi)
                ) { Text("S陌L") }
            },
            dismissButton = { TextButton(onClick = { silinecek = null }) { Text("VAZGE脟") } }
        )
    }
}

@Composable
private fun AnaSayfa(
    modifier: Modifier,
    seferler: List<Sefer>,
    arac: Arac?,
    aktif: AktifSefer?,
    guzergah: String,
    cikisKm: String,
    toplamKm: Int,
    toplamSure: Int,
    ortalamaKm: Int,
    gun: String,
    ayYil: String,
    hafta: String,
    onGuzergah: (String) -> Unit,
    onCikisKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onNavigation: () -> Unit,
    onNotes: () -> Unit,
    onVehicles: () -> Unit,
    onHistory: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onTheme: () -> Unit,
    themeMenu: Boolean,
    onDismissTheme: () -> Unit,
    onThemeSelect: (Tema) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TopHeader(
                onNotes = onNotes,
                onTheme = onTheme,
                themeMenu = themeMenu,
                onDismissTheme = onDismissTheme,
                onThemeSelect = onThemeSelect
            )
        }

        item {
            WelcomeCard(gun, ayYil, hafta)
        }

        item {
            StatsRow(
                sefer = seferler.size,
                km = toplamKm,
                sure = toplamSure,
                ortalama = ortalamaKm,
                yakit = arac?.yakit ?: 0
            )
        }

        item {
            Text(
                "Yeni Sefer",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            NewTripCard(
                arac = arac,
                aktif = aktif,
                guzergah = guzergah,
                cikisKm = cikisKm,
                onGuzergah = onGuzergah,
                onCikisKm = onCikisKm,
                onStart = onStart,
                onFinish = onFinish
            )
        }

        item {
            Text("H谋zl谋 陌艧lemler", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
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
            VehicleCard(
                arac = arac,
                onClick = onVehicles
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sefer Ge莽mi艧i",
                    modifier = Modifier.weight(1f),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                TextButton(onClick = onHistory) {
                    Text("T眉m眉n眉 G枚r", fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        if (seferler.isEmpty()) {
            item { EmptyHistoryCard() }
        } else {
            items(seferler.take(4), key = { it.id }) { sefer ->
                HistoryCard(sefer)
            }
        }
    }
}

@Composable
private fun TopHeader(
    onNotes: () -> Unit,
    onTheme: () -> Unit,
    themeMenu: Boolean,
    onDismissTheme: () -> Unit,
    onThemeSelect: (Tema) -> Unit
) {
    Box {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = Mor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(31.dp))
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text("艦of枚r Takip", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text("G眉nl眉k sefer paneli", fontSize = 14.sp, color = Ikincil)
            }

            HeaderPill(Icons.Default.Note, "Notlar", onNotes)
            Spacer(Modifier.width(6.dp))
            HeaderPill(Icons.Default.Palette, "Tema", onTheme)
        }

        DropdownMenu(expanded = themeMenu, onDismissRequest = onDismissTheme) {
            DropdownMenuItem(
                text = { Text("A莽谋k tema") },
                leadingIcon = { Icon(Icons.Default.LightMode, null) },
                onClick = { onThemeSelect(Tema.ACIK) }
            )
            DropdownMenuItem(
                text = { Text("Mavi tema") },
                leadingIcon = { Icon(Icons.Default.WaterDrop, null) },
                onClick = { onThemeSelect(Tema.MAVI) }
            )
            DropdownMenuItem(
                text = { Text("Gece tema") },
                leadingIcon = { Icon(Icons.Default.DarkMode, null) },
                onClick = { onThemeSelect(Tema.GECE) }
            )
        }
    }
}

@Composable
private fun HeaderPill(icon: ImageVector, text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(23.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun WelcomeCard(gun: String, ayYil: String, hafta: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF172B58), Color(0xFF3B6EB5), Color(0xFF1A3C68))
                    ),
                    RoundedCornerShape(25.dp)
                )
                .padding(17.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("鈽€  Merhaba Y谋lmaz 馃憢", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Yola 莽谋kmaya haz谋r m谋s谋n?", color = Color.White.copy(alpha = .95f), fontSize = 14.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("G眉venli s眉r眉艧ler dilerim...", color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                    Spacer(Modifier.height(13.dp))
                    Text("$gun", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("$ayYil 鈥� $hafta", color = Color.White.copy(alpha = .86f), fontSize = 12.sp)
                }
                Surface(
                    modifier = Modifier.size(82.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = .13f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(49.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(sefer: Int, km: Int, sure: Int, ortalama: Int, yakit: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        StatCard(126.dp, Mor, Icons.Default.DirectionsCar, sefer.toString(), "Toplam Sefer")
        StatCard(126.dp, Mavi, Icons.Default.Speed, formatKm(km), "Toplam KM")
        StatCard(136.dp, Yesil, Icons.Default.Timer, "${sure / 60}s ${sure % 60}dk", "Toplam S眉re")
        StatCard(136.dp, Turuncu, Icons.Default.Speed, formatKm(ortalama), "Ort. Sefer KM")
        StatCard(126.dp, Pembe, Icons.Default.LocalGasStation, "%$yakit", "Yak谋t Durumu")
    }
}

@Composable
private fun StatCard(
    width: androidx.compose.ui.unit.Dp,
    color: Color,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = Modifier.width(width).height(116.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.padding(13.dp)) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(5.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = Color.White.copy(alpha = .9f), fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun NewTripCard(
    arac: Arac?,
    aktif: AktifSefer?,
    guzergah: String,
    cikisKm: String,
    onGuzergah: (String) -> Unit,
    onCikisKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(27.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (aktif == null) "Sefer bilgilerinizi girin" else "Sefer devam ediyor",
                        fontSize = 15.sp,
                        color = Ikincil
                    )
                    if (arac != null) {
                        Text("${arac.ad} 鈥� ${arac.plaka}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(
                    if (aktif == null) Icons.Default.Place else Icons.Default.AccessTime,
                    null,
                    tint = if (aktif == null) Mor else Yesil,
                    modifier = Modifier.size(34.dp)
                )
            }

            AppField(
                value = guzergah,
                onValueChange = onGuzergah,
                label = "G眉zerg芒h",
                placeholder = "脰rn. Akseki - Antalya",
                icon = Icons.Default.Place,
                enabled = aktif == null
            )

            AppField(
                value = cikisKm,
                onValueChange = onCikisKm,
                label = "脟谋k谋艧 KM",
                placeholder = "脰rn. 125430",
                icon = Icons.Default.Speed,
                keyboardType = KeyboardType.Number,
                enabled = aktif == null
            )

            if (aktif != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFFE9F8F0)
                ) {
                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = Yesil)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Sefer aktif", fontWeight = FontWeight.Bold, color = Color(0xFF177A48))
                            Text("脟谋k谋艧: ${aktif.cikisSaati}", fontSize = 12.sp, color = Color(0xFF177A48))
                        }
                    }
                }
            }

            Button(
                onClick = if (aktif == null) onStart else onFinish,
                modifier = Modifier.fillMaxWidth().height(57.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (aktif == null) Mor else Kirmizi
                )
            ) {
                Icon(if (aktif == null) Icons.Default.PlayArrow else Icons.Default.Stop, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (aktif == null) "SEFER陌 BA艦LAT" else "SEFER陌 B陌T陌R",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun AppField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(17.dp)
    )
}

@Composable
private fun QuickActions(
    onNavigation: () -> Unit,
    onNotes: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ActionCard(Modifier.weight(1f), Mavi, Icons.Default.Navigation, "Navigasyon", "Haritada A莽", onNavigation)
            ActionCard(Modifier.weight(1f), Turuncu, Icons.Default.NoteAdd, "Not Ekle", "H谋zl谋 Not", onNotes)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ActionCard(Modifier.weight(1f), Yesil, Icons.Default.BarChart, "Raporlar", "Detayl谋 G枚r", onReports)
            ActionCard(Modifier.weight(1f), Lacivert, Icons.Default.Settings, "Ayarlar", "Uygulama", onSettings)
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier,
    color: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(27.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun VehicleCard(arac: Arac?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(23.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Mor)
                    Spacer(Modifier.width(7.dp))
                    Text("Ara莽 Bilgisi", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(9.dp))
                if (arac == null) {
                    Text("Hen眉z ara莽 eklenmedi.", fontWeight = FontWeight.Bold)
                    Text("Dokunarak ara莽 ekleyin.", fontSize = 12.sp, color = Ikincil)
                } else {
                    Text(arac.ad, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = Color(0xFFEAF0FA)
                    ) {
                        Text(arac.plaka, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text("Yak谋t Durumu  鈥�  %${arac.yakit}", fontSize = 12.sp, color = Ikincil)
                    LinearProgressIndicator(
                        progress = arac.yakit / 100f,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)),
                        color = Yesil,
                        trackColor = Color(0xFFDCE3ED)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text("Son KM: ${formatKm(arac.sonKm)}", fontSize = 11.sp, color = Ikincil)
                }
            }
            Spacer(Modifier.width(10.dp))
            if (arac?.fotoUri?.isNotBlank() == true) {
                UriImage(
                    Uri.parse(arac.fotoUri),
                    Modifier.size(105.dp).clip(RoundedCornerShape(18.dp))
                )
            } else {
                Surface(
                    Modifier.size(105.dp),
                    RoundedCornerShape(18.dp),
                    color = Color(0xFFEAF0FA)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DirectionsCar, null, tint = Mor, modifier = Modifier.size(55.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(sefer: Sefer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                Modifier.size(72.dp),
                RoundedCornerShape(15.dp),
                color = Color(0xFFEAF0FA)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Mor, modifier = Modifier.size(38.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(sefer.guzergah, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${sefer.tarih} 鈥� ${sefer.cikisSaati} 鈫� ${sefer.donusSaati}", fontSize = 11.sp, color = Ikincil)
                Text("${formatKm(sefer.cikisKm)} 鈫� ${formatKm(sefer.donusKm)} KM", fontSize = 11.sp, color = Ikincil)
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFDDF7E8)) {
                    Text("Tamamland谋 鉁�", Modifier.padding(horizontal = 7.dp, vertical = 4.dp), fontSize = 9.sp, color = Color(0xFF187A48), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Text("${formatKm(sefer.toplamKm)} KM", fontWeight = FontWeight.Black, color = Yesil)
                Text(sefer.toplamSure, fontSize = 9.sp, color = Ikincil)
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.History, null, tint = Mor, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(8.dp))
            Text("Hen眉z tamamlanm谋艧 sefer yok.", fontWeight = FontWeight.Bold)
            Text("陌lk seferiniz burada g枚r眉necek.", fontSize = 12.sp, color = Ikincil)
        }
    }
}

@Composable
private fun BottomBar(
    selected: Int,
    onSelected: (Int) -> Unit,
    onAdd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .background(Lacivert)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem(
                modifier = Modifier.weight(1f),
                index = 0,
                selected = selected,
                icon = Icons.Default.Home,
                label = "Ana Sayfa",
                onSelected = onSelected
            )

            BottomItem(
                modifier = Modifier.weight(1f),
                index = 1,
                selected = selected,
                icon = Icons.Default.History,
                label = "Ge莽mi艧",
                onSelected = onSelected
            )

            Spacer(Modifier.width(68.dp))

            BottomItem(
                modifier = Modifier.weight(1f),
                index = 2,
                selected = selected,
                icon = Icons.Default.BarChart,
                label = "Raporlar",
                onSelected = onSelected
            )

            BottomItem(
                modifier = Modifier.weight(1f),
                index = 3,
                selected = selected,
                icon = Icons.Default.Settings,
                label = "Ayarlar",
                onSelected = onSelected
            )
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-18).dp)
                .size(62.dp),
            containerColor = Mor,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Yeni sefer",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun BottomItem(
    modifier: Modifier,
    index: Int,
    selected: Int,
    icon: ImageVector,
    label: String,
    onSelected: (Int) -> Unit
) {
    val isSelected = selected == index

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onSelected(index) }
            .padding(top = 7.dp, bottom = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = if (isSelected) Mor else Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 34.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(25.dp),
                    tint = if (isSelected) Color.White else Color(0xFFAEB9CC)
                )
            }
        )

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFFAEB9CC),
            maxLines = 1
        )
    }
}

@Composable
private fun GecmisSayfasi(modifier: Modifier, seferler: List<Sefer>, onDelete: (Sefer) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageTitle("Sefer Ge莽mi艧i", "Tamamlanan t眉m seferler") }
        if (seferler.isEmpty()) {
            item { EmptyHistoryCard() }
        } else {
            items(seferler, key = { it.id }) { sefer ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(sefer.guzergah, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                Text(sefer.tarih, color = Ikincil, fontSize = 12.sp)
                            }
                            Text("${formatKm(sefer.toplamKm)} KM", color = Mor, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(7.dp))
                        Text("${formatKm(sefer.cikisKm)} 鈫� ${formatKm(sefer.donusKm)} KM 鈥� ${sefer.cikisSaati} 鈫� ${sefer.donusSaati}", fontSize = 12.sp)
                        Text(sefer.toplamSure, fontSize = 11.sp, color = Ikincil)
                        if (sefer.notMetni.isNotBlank()) Text("Not: ${sefer.notMetni}", fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onDelete(sefer) }) {
                                Icon(Icons.Default.Delete, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Sil")
                            }
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
    toplamSure: Int,
    ortalamaKm: Int
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageTitle("Raporlar", "S眉r眉艧 performans 枚zeti") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ReportBox(Modifier.weight(1f), "Sefer", seferler.size.toString())
                ReportBox(Modifier.weight(1f), "KM", formatKm(toplamKm))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                ReportBox(Modifier.weight(1f), "S眉re", "${toplamSure / 60}s ${toplamSure % 60}dk")
                ReportBox(Modifier.weight(1f), "Ort. KM", formatKm(ortalamaKm))
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp)) {
                Column(Modifier.padding(17.dp)) {
                    Text("Son seferler", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(5.dp))
                    seferler.take(10).forEach {
                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                            Text(it.guzergah, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${formatKm(it.toplamKm)} KM", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportBox(modifier: Modifier, title: String, value: String) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(17.dp)) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(title, color = Ikincil)
        }
    }
}

@Composable
private fun AyarlarSayfasi(
    modifier: Modifier,
    tema: Tema,
    aracSayisi: Int,
    onTema: (Tema) -> Unit,
    onVehicles: () -> Unit,
    onNotes: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageTitle("Ayarlar", "Uygulama tercihleri") }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp)) {
                Column(Modifier.padding(17.dp)) {
                    Text("Tema", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(tema == Tema.ACIK, { onTema(Tema.ACIK) }, label = { Text("A莽谋k") })
                        FilterChip(tema == Tema.MAVI, { onTema(Tema.MAVI) }, label = { Text("Mavi") })
                        FilterChip(tema == Tema.GECE, { onTema(Tema.GECE) }, label = { Text("Gece") })
                    }
                }
            }
        }
        item {
            SettingRow(Icons.Default.DirectionsCar, "Ara莽lar谋m", "$aracSayisi kay谋tl谋 ara莽", onVehicles)
        }
        item {
            SettingRow(Icons.Default.Note, "Genel Notlar", "Notlar谋n谋z谋 y枚netin", onNotes)
        }
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = Ikincil)
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(46.dp), RoundedCornerShape(14.dp), color = Mor.copy(alpha = .1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Mor)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 12.sp, color = Ikincil)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
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
    val context = LocalContext.current

    var ad by remember { mutableStateOf("") }
    var plaka by remember { mutableStateOf("") }
    var yakit by remember { mutableStateOf("100") }
    var sonKm by remember { mutableStateOf("") }
    var sonDolum by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            kaliciUriIzniAl(context, uri)
            fotoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Ara莽lar谋m", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                araclar.forEach { arac ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (arac.secili) Mor.copy(alpha = .09f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (arac.fotoUri.isNotBlank()) {
                                UriImage(Uri.parse(arac.fotoUri), Modifier.size(48.dp).clip(RoundedCornerShape(11.dp)))
                            } else {
                                Surface(Modifier.size(48.dp), RoundedCornerShape(11.dp), color = Mor.copy(alpha = .1f)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DirectionsCar, null, tint = Mor)
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(arac.ad, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(arac.plaka, fontSize = 11.sp)
                            }
                            TextButton(onClick = { onSelect(arac) }) { Text(if (arac.secili) "Se莽ili" else "Se莽") }
                            IconButton(onClick = { onEdit(arac) }) { Icon(Icons.Default.Edit, null) }
                            IconButton(onClick = { onDelete(arac) }) { Icon(Icons.Default.Delete, null) }
                        }
                    }
                }

                OutlinedTextField(ad, { ad = it }, Modifier.fillMaxWidth(), label = { Text("Ara莽 ad谋") }, singleLine = true)
                OutlinedTextField(
                    plaka,
                    { plaka = it.uppercase(Locale.getDefault()) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Plaka") },
                    singleLine = true
                )
                OutlinedTextField(
                    yakit,
                    { yakit = it.filter(Char::isDigit).take(3) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Yak谋t %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    sonKm,
                    { sonKm = it.filter(Char::isDigit) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Son KM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    sonDolum,
                    { sonDolum = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("Son dolum tarihi") },
                    singleLine = true
                )
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Photo, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (fotoUri.isBlank()) "Ara莽 foto臒raf谋 se莽" else "Foto臒raf se莽ildi")
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
                        yakit = "100"
                        sonKm = ""
                        sonDolum = ""
                        fotoUri = ""
                    }
                }
            ) { Text("Ara莽 Ekle") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Kapat") } }
    )
}

@Composable
private fun AracDuzenleDialog(arac: Arac, onClose: () -> Unit, onSave: (Arac) -> Unit) {
    val context = LocalContext.current

    var ad by remember(arac.id) { mutableStateOf(arac.ad) }
    var plaka by remember(arac.id) { mutableStateOf(arac.plaka) }
    var yakit by remember(arac.id) { mutableStateOf(arac.yakit.toString()) }
    var sonKm by remember(arac.id) { mutableStateOf(arac.sonKm.toString()) }
    var sonDolum by remember(arac.id) { mutableStateOf(arac.sonDolum) }
    var fotoUri by remember(arac.id) { mutableStateOf(arac.fotoUri) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            kaliciUriIzniAl(context, uri)
            fotoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Ara莽 Bilgilerini D眉zenle", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(ad, { ad = it }, Modifier.fillMaxWidth(), label = { Text("Ara莽 ad谋") }, singleLine = true)
                OutlinedTextField(plaka, { plaka = it.uppercase(Locale.getDefault()) }, Modifier.fillMaxWidth(), label = { Text("Plaka") }, singleLine = true)
                OutlinedTextField(
                    yakit,
                    { yakit = it.filter(Char::isDigit).take(3) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Yak谋t %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    sonKm,
                    { sonKm = it.filter(Char::isDigit) },
                    Modifier.fillMaxWidth(),
                    label = { Text("Son KM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(sonDolum, { sonDolum = it }, Modifier.fillMaxWidth(), label = { Text("Son dolum") }, singleLine = true)
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Photo, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Foto臒raf de臒i艧tir")
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
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Vazge莽") } }
    )
}

@Composable
private fun BitirDialog(
    aktif: AktifSefer,
    donusKm: String,
    notMetni: String,
    onDonusKm: (String) -> Unit,
    onNot: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        icon = { Icon(Icons.Default.Flag, null, tint = Mor) },
        title = { Text("Seferi Tamamla", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(aktif.guzergah, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("脟谋k谋艧 KM: ${formatKm(aktif.cikisKm)}")
                Text("脟谋k谋艧 saati: ${aktif.cikisSaati}", color = Ikincil)
                OutlinedTextField(
                    donusKm,
                    onDonusKm,
                    Modifier.fillMaxWidth(),
                    label = { Text("D枚n眉艧 KM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    notMetni,
                    onNot,
                    Modifier.fillMaxWidth(),
                    label = { Text("Sefer Notu") },
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(4.dp))
                Text("KAYDET")
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("VAZGE脟") } }
    )
}

@Composable
private fun NotesDialog(notlar: String, onClose: () -> Unit, onSave: (String) -> Unit) {
    var metin by remember(notlar) { mutableStateOf(notlar) }
    AlertDialog(
        onDismissRequest = onClose,
        icon = { Icon(Icons.Default.Note, null, tint = Mor) },
        title = { Text("Notlar", fontWeight = FontWeight.ExtraBold) },
        text = {
            OutlinedTextField(
                metin,
                { metin = it },
                Modifier.fillMaxWidth(),
                label = { Text("Not") },
                minLines = 6,
                maxLines = 10
            )
        },
        confirmButton = {
            Button(onClick = { onSave(metin); onClose() }) { Text("KAYDET") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("VAZGE脟") } }
    )
}

@Composable
private fun UriImage(uri: Uri, modifier: Modifier) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap!!.asImageBitmap(),
            contentDescription = "Ara莽 foto臒raf谋",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFEAF0FA)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DirectionsCar, null, tint = Mor, modifier = Modifier.size(38.dp))
            }
        }
    }
}

private fun prefs(context: Context) =
    context.getSharedPreferences("sofor_takip", Context.MODE_PRIVATE)

private fun kaydetSeferler(context: Context, list: List<Sefer>) {
    val veri = list.joinToString("\u001E") {
        listOf(
            it.id, it.guzergah, it.cikisKm, it.donusKm, it.cikisSaati,
            it.donusSaati, it.toplamKm, it.toplamSure, it.notMetni, it.tarih
        ).joinToString("\u001F")
    }
    prefs(context).edit().putString("seferler", veri).apply()
}

private fun yukleSeferler(context: Context): List<Sefer> {
    val veri = prefs(context).getString("seferler", "") ?: ""
    if (veri.isBlank()) return emptyList()

    return veri.split("\u001E").mapNotNull { satir ->
        val p = satir.split("\u001F")
        try {
            when {
                p.size >= 10 -> Sefer(
                    p[0].toLong(), p[1], p[2].toInt(), p[3].toInt(),
                    p[4], p[5], p[6].toInt(), p[7], p[8], p[9]
                )
                p.size == 9 -> Sefer(
                    p[0].toLong(), p[1], p[2].toInt(), p[3].toInt(),
                    p[4], p[5], p[6].toInt(), p[7], p[8], ""
                )
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

private fun kaydetAraclar(context: Context, list: List<Arac>) {
    val veri = list.joinToString("\u001E") {
        listOf(
            it.id, it.ad, it.plaka, it.fotoUri, it.yakit,
            it.sonDolum, it.sonKm, it.secili
        ).joinToString("\u001F")
    }
    prefs(context).edit().putString("araclar", veri).apply()
}

private fun yukleAraclar(context: Context): List<Arac> {
    val veri = prefs(context).getString("araclar", "") ?: ""
    if (veri.isBlank()) return emptyList()

    return veri.split("\u001E").mapNotNull { satir ->
        val p = satir.split("\u001F")
        try {
            when {
                p.size >= 8 -> Arac(
                    p[0].toLong(), p[1], p[2], p[3],
                    p[4].toInt().coerceIn(0, 100), p[5],
                    p[6].toInt(), p[7].toBoolean()
                )
                p.size == 5 -> Arac(
                    p[0].toLong(), p[1], p[2], p[3],
                    100, "", 0, p[4].toBoolean()
                )
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

private fun kaydetNotlar(context: Context, notlar: String) {
    prefs(context).edit().putString("genel_notlar", notlar).apply()
}

private fun yukleNotlar(context: Context): String =
    prefs(context).getString("genel_notlar", "") ?: ""

private fun kaydetAktifSefer(context: Context, aktif: AktifSefer?) {
    val edit = prefs(context).edit()
    if (aktif == null) {
        edit.remove("aktif_guzergah")
            .remove("aktif_cikis_km")
            .remove("aktif_cikis_saati")
            .remove("aktif_baslangic")
    } else {
        edit.putString("aktif_guzergah", aktif.guzergah)
            .putInt("aktif_cikis_km", aktif.cikisKm)
            .putString("aktif_cikis_saati", aktif.cikisSaati)
            .putLong("aktif_baslangic", aktif.baslangicMs)
    }
    edit.apply()
}

private fun yukleAktifSefer(context: Context): AktifSefer? {
    val p = prefs(context)
    val guzergah = p.getString("aktif_guzergah", null) ?: return null
    val km = p.getInt("aktif_cikis_km", -1)
    val saat = p.getString("aktif_cikis_saati", null) ?: return null
    val baslangic = p.getLong("aktif_baslangic", 0L)
    if (km < 0 || baslangic <= 0L) return null

    return AktifSefer(guzergah, km, saat, baslangic)
}

private fun kaydetTema(context: Context, tema: Tema) {
    prefs(context).edit().putString("tema", tema.name).apply()
}

private fun yukleTema(context: Context): Tema =
    runCatching {
        Tema.valueOf(
            prefs(context).getString("tema", Tema.ACIK.name) ?: Tema.ACIK.name
        )
    }.getOrDefault(Tema.ACIK)

private fun kaliciUriIzniAl(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

private fun navigasyonAc(context: Context, guzergah: String) {
    val hedef = guzergah.trim().ifBlank { "Akseki Antalya" }
    val geo = Uri.parse("geo:0,0?q=${Uri.encode(hedef)}")

    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
    }.onFailure {
        val web = Uri.parse(
            "https://www.google.com/maps/search/?api=1&query=${Uri.encode(hedef)}"
        )
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, web))
        }
    }
}

private fun saat(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun sureHesapla(baslangic: Long, bitis: Long): String {
    val dakika = ((bitis - baslangic) / 60000L).coerceAtLeast(0L)
    return "${dakika / 60} saat ${dakika % 60} dakika"
}

private fun sureDakika(sure: String): Int {
    val match = Regex("""(\d+)\s*saat\s*(\d+)\s*dakika""").find(sure) ?: return 0
    return (match.groupValues[1].toIntOrNull() ?: 0) * 60 +
            (match.groupValues[2].toIntOrNull() ?: 0)
}

private fun formatKm(km: Int): String =
    String.format(Locale.getDefault(), "%,d", km)

private fun formatKm(km: Long): String =
    String.format(Locale.getDefault(), "%,d", km)
