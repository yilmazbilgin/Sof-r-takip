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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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

private enum class Tema {
    ACIK, MAVI, GECE
}

private val Mor = Color(0xFF5B2BC5)
private val MorAcik = Color(0xFFE9E0FF)
private val ArkaPlan = Color(0xFFF7F5FC)
private val Kart = Color(0xFFFFFBFF)
private val Metin = Color(0xFF202024)
private val IkincilMetin = Color(0xFF5F5C66)
private val Yesil = Color(0xFF16A05D)
private val Kirmizi = Color(0xFFE53935)
private val Turuncu = Color(0xFFFF8A00)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoforTakipApp()
        }
    }
}

@Composable
private fun SoforTakipApp() {
    val context = LocalContext.current

    var seferler by remember { mutableStateOf(yukleSeferler(context)) }
    var araclar by remember { mutableStateOf(yukleAraclar(context)) }
    var notlar by remember { mutableStateOf(yukleNotlar(context)) }
    var aktifSefer by remember { mutableStateOf(yukleAktifSefer(context)) }

    var tema by remember { mutableStateOf(yukleTema(context)) }
    var sayfa by remember { mutableIntStateOf(0) }

    var notlarAcik by remember { mutableStateOf(false) }
    var araclarAcik by remember { mutableStateOf(false) }
    var aracDuzenle by remember { mutableStateOf<Arac?>(null) }
    var bitirDialog by remember { mutableStateOf(false) }
    var silinecek by remember { mutableStateOf<Sefer?>(null) }
    var bilgi by remember { mutableStateOf("") }
    var temaMenu by remember { mutableStateOf(false) }

    var guzergah by remember { mutableStateOf("") }
    var cikisKm by remember { mutableStateOf("") }
    var donusKm by remember { mutableStateOf("") }
    var notMetni by remember { mutableStateOf("") }

    val seciliArac = araclar.firstOrNull { it.secili } ?: araclar.firstOrNull()
    val toplamKm = seferler.sumOf { it.toplamKm }
    val toplamSureDakika = seferler.sumOf { sureDakika(it.toplamSure) }
    val ortalamaKm = if (seferler.isEmpty()) 0 else toplamKm / seferler.size
    val bugun = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

    val scheme = when (tema) {
        Tema.ACIK -> lightColorScheme(
            primary = Mor,
            onPrimary = Color.White,
            secondary = Color(0xFF7351C8),
            background = ArkaPlan,
            surface = Kart,
            surfaceVariant = Color(0xFFF0ECF7),
            onSurface = Metin,
            onSurfaceVariant = IkincilMetin
        )
        Tema.MAVI -> lightColorScheme(
            primary = Color(0xFF1565C0),
            onPrimary = Color.White,
            secondary = Color(0xFF00A6C7),
            background = Color(0xFFF3F8FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFE7F0F8)
        )
        Tema.GECE -> darkColorScheme(
            primary = Color(0xFFB79AFF),
            onPrimary = Color(0xFF25104F),
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
                ProfessionalBottomBar(
                    selected = sayfa,
                    onSelected = { sayfa = it }
                )
            }
        ) { padding ->
            when (sayfa) {
                0 -> AnaSayfaProfesyonel(
                    modifier = Modifier.padding(padding),
                    seferler = seferler,
                    seciliArac = seciliArac,
                    toplamKm = toplamKm,
                    toplamSureDakika = toplamSureDakika,
                    ortalamaKm = ortalamaKm,
                    aktifSefer = aktifSefer,
                    guzergah = guzergah,
                    cikisKm = cikisKm,
                    temaMenu = temaMenu,
                    onTemaMenu = { temaMenu = true },
                    onTemaDismiss = { temaMenu = false },
                    onTemaSec = {
                        tema = it
                        temaMenu = false
                        kaydetTema(context, it)
                    },
                    onGuzergah = { guzergah = it },
                    onCikisKm = { cikisKm = it },
                    onStart = {
                        val km = cikisKm.toIntOrNull()
                        when {
                            seciliArac == null ->
                                bilgi = "脰nce Ara莽 Bilgisi b枚l眉m眉nden bir ara莽 ekleyin."
                            guzergah.isBlank() ->
                                bilgi = "L眉tfen g眉zergah girin."
                            km == null ->
                                bilgi = "Ge莽erli bir 莽谋k谋艧 KM girin."
                            seciliArac.sonKm > 0 && km < seciliArac.sonKm ->
                                bilgi = "脟谋k谋艧 KM, arac谋n son KM bilgisinden k眉莽眉k olamaz."
                            else -> {
                                val yeniAktif = AktifSefer(
                                    guzergah = guzergah.trim(),
                                    cikisKm = km,
                                    cikisSaati = saat(),
                                    baslangicMs = System.currentTimeMillis()
                                )
                                aktifSefer = yeniAktif
                                kaydetAktifSefer(context, yeniAktif)
                            }
                        }
                    },
                    onFinish = { bitirDialog = true },
                    onNavigation = {
                        navigasyonAc(context, guzergah.ifBlank {
                            seciliArac?.ad.orEmpty()
                        })
                    },
                    onNotes = { notlarAcik = true },
                    onReports = { sayfa = 2 },
                    onSettings = { sayfa = 3 },
                    onVehicles = { araclarAcik = true },
                    onHistory = { sayfa = 1 },
                    onNotesHeader = { notlarAcik = true }
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
                    onTema = {
                        tema = it
                        kaydetTema(context, it)
                    },
                    aracSayisi = araclar.size,
                    onVehicles = { araclarAcik = true },
                    onNotes = { notlarAcik = true }
                )
            }
        }
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
                    araclar = araclar.mapIndexed { index, arac ->
                        if (index == 0) arac.copy(secili = true) else arac
                    }
                }
                kaydetAraclar(context, araclar)
            },
            onSelect = { sec ->
                araclar = araclar.map { it.copy(secili = it.id == sec.id) }
                kaydetAraclar(context, araclar)
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

    if (bitirDialog && aktifSefer != null) {
        BitirDialogProfesyonel(
            aktif = aktifSefer!!,
            donusKm = donusKm,
            notMetni = notMetni,
            onDonusKm = { donusKm = it },
            onNot = { notMetni = it },
            onClose = {
                bitirDialog = false
                donusKm = ""
                notMetni = ""
            },
            onSave = {
                val c = aktifSefer!!.cikisKm
                val d = donusKm.toIntOrNull()

                when {
                    d == null ->
                        bilgi = "Ge莽erli bir d枚n眉艧 KM girin."
                    d < c ->
                        bilgi = "D枚n眉艧 KM, 莽谋k谋艧 KM'den k眉莽眉k olamaz."
                    else -> {
                        val bitis = System.currentTimeMillis()
                        val yeni = Sefer(
                            id = bitis,
                            guzergah = aktifSefer!!.guzergah,
                            cikisKm = c,
                            donusKm = d,
                            cikisSaati = aktifSefer!!.cikisSaati,
                            donusSaati = saat(),
                            toplamKm = d - c,
                            toplamSure = sureHesapla(
                                aktifSefer!!.baslangicMs,
                                bitis
                            ),
                            notMetni = notMetni.trim(),
                            tarih = bugun
                        )

                        seferler = listOf(yeni) + seferler
                        kaydetSeferler(context, seferler)

                        if (seciliArac != null) {
                            araclar = araclar.map { arac ->
                                if (arac.id == seciliArac.id) {
                                    arac.copy(sonKm = d)
                                } else {
                                    arac
                                }
                            }
                            kaydetAraclar(context, araclar)
                        }

                        aktifSefer = null
                        kaydetAktifSefer(context, null)
                        bitirDialog = false
                        guzergah = ""
                        cikisKm = ""
                        donusKm = ""
                        notMetni = ""
                    }
                }
            }
        )
    }

    if (bilgi.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { bilgi = "" },
            icon = {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Bilgi", fontWeight = FontWeight.ExtraBold) },
            text = { Text(bilgi) },
            confirmButton = {
                Button(onClick = { bilgi = "" }) {
                    Text("Tamam")
                }
            }
        )
    }

    silinecek?.let { sefer ->
        AlertDialog(
            onDismissRequest = { silinecek = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Seferi sil", fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Text("\"${sefer.guzergah}\" seferi ge莽mi艧ten silinecek.")
            },
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
                    Text("S陌L")
                }
            },
            dismissButton = {
                TextButton(onClick = { silinecek = null }) {
                    Text("VAZGE脟")
                }
            }
        )
    }
}

@Composable
private fun AnaSayfaProfesyonel(
    modifier: Modifier,
    seferler: List<Sefer>,
    seciliArac: Arac?,
    toplamKm: Int,
    toplamSureDakika: Int,
    ortalamaKm: Int,
    aktifSefer: AktifSefer?,
    guzergah: String,
    cikisKm: String,
    temaMenu: Boolean,
    onTemaMenu: () -> Unit,
    onTemaDismiss: () -> Unit,
    onTemaSec: (Tema) -> Unit,
    onGuzergah: (String) -> Unit,
    onCikisKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onNavigation: () -> Unit,
    onNotes: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onVehicles: () -> Unit,
    onHistory: () -> Unit,
    onNotesHeader: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 30.dp
        ),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            ProfessionalHeader(
                onNotes = onNotesHeader,
                onTheme = onTemaMenu,
                themeMenu = temaMenu,
                onDismissTheme = onTemaDismiss,
                onThemeSelect = onTemaSec
            )
        }

        item {
            ProfessionalWelcome(
                arac = seciliArac,
                seferSayisi = seferler.size,
                toplamKm = toplamKm
            )
        }

        item {
            ProfessionalStats(
                sefer = seferler.size,
                km = toplamKm,
                sure = toplamSureDakika,
                ortalama = ortalamaKm,
                yakit = seciliArac?.yakit ?: 0
            )
        }

        item {
            SectionHeader(
                title = "Yeni Sefer",
                trailing = "Ara莽 Bilgisi",
                icon = Icons.Default.DirectionsCar,
                onTrailing = onVehicles
            )
        }

        item {
            ProfessionalNewTripCard(
                arac = seciliArac,
                aktif = aktifSefer != null,
                aktifSefer = aktifSefer,
                guzergah = if (aktifSefer != null) aktifSefer.guzergah else guzergah,
                cikisKm = if (aktifSefer != null) aktifSefer.cikisKm.toString() else cikisKm,
                onGuzergah = onGuzergah,
                onCikisKm = onCikisKm,
                onStart = onStart,
                onFinish = onFinish
            )
        }

        item {
            Text(
                "H谋zl谋 陌艧lemler",
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        item {
            QuickActionsProfessional(
                onNavigation = onNavigation,
                onNotes = onNotes,
                onReports = onReports,
                onSettings = onSettings
            )
        }

        item {
            SectionHeader(
                title = "Sefer Ge莽mi艧i",
                trailing = "T眉m眉n眉 G枚r",
                icon = Icons.Default.History,
                onTrailing = onHistory
            )
        }

        if (seferler.isEmpty()) {
            item {
                EmptyProfessionalCard()
            }
        } else {
            items(
                seferler.take(4),
                key = { it.id }
            ) { sefer ->
                ProfessionalTripHistoryCard(sefer)
            }
        }
    }
}

@Composable
private fun ProfessionalHeader(
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
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "艦of枚r Takip",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "G眉nl眉k sefer paneli",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNotes,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Note, "Notlar")
            }

            IconButton(
                onClick = onTheme,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Palette, "Tema")
            }
        }

        DropdownMenu(
            expanded = themeMenu,
            onDismissRequest = onDismissTheme
        ) {
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
private fun ProfessionalWelcome(
    arac: Arac?,
    seferSayisi: Int,
    toplamKm: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (arac?.fotoUri?.isNotBlank() == true) {
                UriImage(
                    uri = Uri.parse(arac.fotoUri),
                    modifier = Modifier
                        .size(108.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(108.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(58.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Bug眉n haz谋r m谋s谋n谋z?",
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    if (arac == null) {
                        "Arac谋n谋z谋 ekleyerek ba艧lay谋n."
                    } else {
                        "${arac.ad} 鈥� ${arac.plaka}"
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "$seferSayisi sefer 鈥� ${formatKm(toplamKm)} KM",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ProfessionalStats(
    sefer: Int,
    km: Int,
    sure: Int,
    ortalama: Int,
    yakit: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProfessionalStatCard(
            width = 126.dp,
            icon = Icons.Default.DirectionsCar,
            value = sefer.toString(),
            label = "Toplam Sefer"
        )
        ProfessionalStatCard(
            width = 126.dp,
            icon = Icons.Default.Speed,
            value = formatKm(km),
            label = "Toplam KM"
        )
        ProfessionalStatCard(
            width = 136.dp,
            icon = Icons.Default.Timer,
            value = "${sure / 60}s ${sure % 60}dk",
            label = "Toplam S眉re"
        )
        ProfessionalStatCard(
            width = 136.dp,
            icon = Icons.Default.Speed,
            value = formatKm(ortalama),
            label = "Ort. Sefer KM"
        )
        ProfessionalStatCard(
            width = 126.dp,
            icon = Icons.Default.LocalGasStation,
            value = "%$yakit",
            label = "Yak谋t"
        )
    }
}

@Composable
private fun ProfessionalStatCard(
    width: Dp,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = Modifier
            .width(width)
            .height(126.dp),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailing: String,
    icon: ImageVector,
    onTrailing: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        TextButton(onClick = onTrailing) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                trailing,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProfessionalNewTripCard(
    arac: Arac?,
    aktif: Boolean,
    aktifSefer: AktifSefer?,
    guzergah: String,
    cikisKm: String,
    onGuzergah: (String) -> Unit,
    onCikisKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(29.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (aktif) "Sefer Devam Ediyor" else "Yeni Sefer",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        if (arac == null) {
                            "Ara莽 se莽in"
                        } else {
                            "${arac.ad} 鈥� ${arac.plaka}"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (arac != null) {
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 7.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "YAKIT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "%${arac.yakit}",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            ProfessionalField(
                value = guzergah,
                onValueChange = onGuzergah,
                label = "G眉zergah",
                placeholder = "脰rn. Akseki - Antalya",
                icon = Icons.Default.Place,
                enabled = !aktif
            )

            ProfessionalField(
                value = cikisKm,
                onValueChange = {
                    onCikisKm(it.filter(Char::isDigit))
                },
                label = "脟谋k谋艧 KM",
                placeholder = "脰rn. 125430",
                icon = Icons.Default.Speed,
                keyboardType = KeyboardType.Number,
                enabled = !aktif
            )

            if (aktif && aktifSefer != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEAF8F0)
                ) {
                    Row(
                        modifier = Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            null,
                            tint = Yesil
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Sefer aktif",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E6F45)
                            )
                            Text(
                                "脟谋k谋艧 saati: ${aktifSefer.cikisSaati}",
                                fontSize = 12.sp,
                                color = Color(0xFF1E6F45)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = if (aktif) onFinish else onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(59.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (aktif) Kirmizi else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (aktif) Icons.Default.Stop else Icons.Default.PlayArrow,
                    null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (aktif) "SEFER陌 B陌T陌R" else "SEFER陌 BA艦LAT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ProfessionalField(
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
        leadingIcon = {
            Icon(icon, null)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun QuickActionsProfessional(
    onNavigation: () -> Unit,
    onNotes: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            ProfessionalActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Navigation,
                title = "Navigasyon",
                subtitle = "Harita a莽",
                onClick = onNavigation
            )
            ProfessionalActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.NoteAdd,
                title = "Not Ekle",
                subtitle = "H谋zl谋 not",
                onClick = onNotes
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            ProfessionalActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.BarChart,
                title = "Raporlar",
                subtitle = "Detayl谋 g枚r",
                onClick = onReports
            )
            ProfessionalActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Settings,
                title = "Ayarlar",
                subtitle = "Uygulama",
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun ProfessionalActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(142.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Surface(
                modifier = Modifier.size(47.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfessionalTripHistoryCard(sefer: Sefer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    sefer.guzergah,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    sefer.tarih,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${sefer.cikisSaati} 鈫� ${sefer.donusSaati}",
                    fontSize = 12.sp
                )
                Text(
                    "${formatKm(sefer.cikisKm)} 鈫� ${formatKm(sefer.donusKm)} KM",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFDDF7E8)
                ) {
                    Text(
                        "Tamamland谋 鉁�",
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF187A48)
                    )
                }

                Spacer(Modifier.height(7.dp))

                Text(
                    formatKm(sefer.toplamKm),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "KM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyProfessionalCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.History,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "Hen眉z tamamlanm谋艧 sefer yok.",
                fontWeight = FontWeight.Bold
            )
            Text(
                "陌lk seferinizi ba艧latt谋臒谋n谋zda burada g枚r眉necek.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfessionalBottomBar(
    selected: Int,
    onSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 5.dp
    ) {
        BottomNavItem(
            selected = selected == 0,
            icon = Icons.Default.Home,
            label = "Ana Sayfa",
            onClick = { onSelected(0) }
        )
        BottomNavItem(
            selected = selected == 1,
            icon = Icons.Default.History,
            label = "Ge莽mi艧",
            onClick = { onSelected(1) }
        )
        BottomNavItem(
            selected = selected == 2,
            icon = Icons.Default.BarChart,
            label = "Raporlar",
            onClick = { onSelected(2) }
        )
        BottomNavItem(
            selected = selected == 3,
            icon = Icons.Default.Settings,
            label = "Ayarlar",
            onClick = { onSelected(3) }
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                icon,
                null,
                modifier = Modifier.size(26.dp)
            )
        },
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        alwaysShowLabel = true
    )
}

@Composable
private fun GecmisSayfasi(
    modifier: Modifier,
    seferler: List<Sefer>,
    onDelete: (Sefer) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageTitle(
                "Sefer Ge莽mi艧i",
                "Tamamlanan t眉m seferler"
            )
        }

        if (seferler.isEmpty()) {
            item { EmptyProfessionalCard() }
        } else {
            items(seferler, key = { it.id }) { sefer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(Modifier.padding(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    sefer.guzergah,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
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

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "${formatKm(sefer.cikisKm)} 鈫� ${formatKm(sefer.donusKm)} KM  鈥�  ${sefer.cikisSaati} 鈫� ${sefer.donusSaati}"
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
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
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageTitle(
                "Raporlar",
                "S眉r眉艧 performans 枚zeti"
            )
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
                    "S眉re",
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
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(8.dp))

                    seferler.take(10).forEach { sefer ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp)
                        ) {
                            Text(
                                sefer.guzergah,
                                Modifier.weight(1f)
                            )
                            Text(
                                "${formatKm(sefer.toplamKm)} KM",
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
        shape = RoundedCornerShape(21.dp),
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
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageTitle(
                "Ayarlar",
                "Uygulama tercihleri"
            )
        }

        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Tema",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        FilterChip(
                            selected = tema == Tema.ACIK,
                            onClick = { onTema(Tema.ACIK) },
                            label = { Text("A莽谋k") }
                        )
                        FilterChip(
                            selected = tema == Tema.MAVI,
                            onClick = { onTema(Tema.MAVI) },
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
                icon = Icons.Default.DirectionsCar,
                title = "Ara莽lar谋m",
                subtitle = "$aracSayisi kay谋tl谋 ara莽",
                onClick = onVehicles
            )
        }

        item {
            SettingRow(
                icon = Icons.Default.Note,
                title = "Genel Notlar",
                subtitle = "H谋zl谋 notlar谋n谋z谋 y枚netin",
                onClick = onNotes
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(45.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )
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
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            kaliciUriIzniAl(context, uri)
            fotoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                "Ara莽lar谋m",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
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
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.DirectionsCar, null)
                                        }
                                    }
                                }

                                Spacer(Modifier.width(9.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        arac.ad,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        arac.plaka,
                                        fontSize = 12.sp
                                    )
                                }

                                TextButton(onClick = { onSelect(arac) }) {
                                    Text(if (arac.secili) "Se莽ili" else "Se莽")
                                }

                                IconButton(onClick = { onEdit(arac) }) {
                                    Icon(Icons.Default.Edit, null)
                                }

                                IconButton(onClick = { onDelete(arac) }) {
                                    Icon(Icons.Default.Delete, null)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ara莽 ad谋") },
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
                    label = { Text("Yak谋t %") },
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
                    label = { Text("Son dolum tarihi") },
                    singleLine = true
                )

                OutlinedButton(
                    onClick = {
                        picker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Photo, null)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (fotoUri.isBlank()) {
                            "Ara莽 foto臒raf谋 se莽"
                        } else {
                            "Foto臒raf se莽ildi"
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val yakitInt = yakit.toIntOrNull()
                        ?.coerceIn(0, 100)
                        ?: 100

                    if (ad.isNotBlank() && plaka.isNotBlank()) {
                        onSave(
                            Arac(
                                id = System.currentTimeMillis(),
                                ad = ad.trim(),
                                plaka = plaka.trim(),
                                fotoUri = fotoUri,
                                yakit = yakitInt,
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
                Text("Ara莽 Ekle")
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
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            kaliciUriIzniAl(context, uri)
            fotoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                "Ara莽 Bilgilerini D眉zenle",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(
                    value = ad,
                    onValueChange = { ad = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ara莽 ad谋") },
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
                    label = { Text("Yak谋t %") },
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
                    label = { Text("Son dolum") },
                    singleLine = true
                )

                OutlinedButton(
                    onClick = {
                        picker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Photo, null)
                    Spacer(Modifier.width(7.dp))
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
                            yakit = yakit.toIntOrNull()
                                ?.coerceIn(0, 100)
                                ?: arac.yakit,
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
                Text("Vazge莽")
            }
        }
    )
}

@Composable
private fun BitirDialogProfesyonel(
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
        icon = {
            Icon(
                Icons.Default.Flag,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Seferi Tamamla",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    aktif.guzergah,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("脟谋k谋艧 KM")
                        Text(
                            formatKm(aktif.cikisKm),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    "脟谋k谋艧 saati: ${aktif.cikisSaati}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = donusKm,
                    onValueChange = {
                        onDonusKm(it.filter(Char::isDigit))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("D枚n眉艧 KM") },
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
                Text("VAZGE脟")
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
    var metin by remember(notlar) {
        mutableStateOf(notlar)
    }

    AlertDialog(
        onDismissRequest = onClose,
        icon = {
            Icon(
                Icons.Default.Note,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Notlar",
                fontWeight = FontWeight.ExtraBold
            )
        },
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
                Text("VAZGE脟")
            }
        }
    )
}

@Composable
private fun UriImage(
    uri: Uri,
    modifier: Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(uri) {
        mutableStateOf<Bitmap?>(null)
    }

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
            contentDescription = "Ara莽 foto臒raf谋",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
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

/* -------------------- VER陌 / KALICILIK -------------------- */

private fun prefs(context: Context) =
    context.getSharedPreferences(
        "sofor_takip",
        Context.MODE_PRIVATE
    )

private fun kaydetSeferler(
    context: Context,
    list: List<Sefer>
) {
    val veri = list.joinToString("\u001E") { sefer ->
        listOf(
            sefer.id,
            sefer.guzergah,
            sefer.cikisKm,
            sefer.donusKm,
            sefer.cikisSaati,
            sefer.donusSaati,
            sefer.toplamKm,
            sefer.toplamSure,
            sefer.notMetni,
            sefer.tarih
        ).joinToString("\u001F")
    }

    prefs(context)
        .edit()
        .putString("seferler", veri)
        .apply()
}

private fun yukleSeferler(
    context: Context
): List<Sefer> {
    val veri = prefs(context)
        .getString("seferler", "")
        ?: ""

    if (veri.isBlank()) return emptyList()

    return veri
        .split("\u001E")
        .mapNotNull { satir ->
            val p = satir.split("\u001F")

            try {
                when {
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

                    p.size == 9 -> Sefer(
                        id = p[0].toLong(),
                        guzergah = p[1],
                        cikisKm = p[2].toInt(),
                        donusKm = p[3].toInt(),
                        cikisSaati = p[4],
                        donusSaati = p[5],
                        toplamKm = p[6].toInt(),
                        toplamSure = p[7],
                        notMetni = p[8],
                        tarih = ""
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
    val veri = list.joinToString("\u001E") { arac ->
        listOf(
            arac.id,
            arac.ad,
            arac.plaka,
            arac.fotoUri,
            arac.yakit,
            arac.sonDolum,
            arac.sonKm,
            arac.secili
        ).joinToString("\u001F")
    }

    prefs(context)
        .edit()
        .putString("araclar", veri)
        .apply()
}

private fun yukleAraclar(
    context: Context
): List<Arac> {
    val veri = prefs(context)
        .getString("araclar", "")
        ?: ""

    if (veri.isBlank()) return emptyList()

    return veri
        .split("\u001E")
        .mapNotNull { satir ->
            val p = satir.split("\u001F")

            try {
                when {
                    p.size >= 8 -> Arac(
                        id = p[0].toLong(),
                        ad = p[1],
                        plaka = p[2],
                        fotoUri = p[3],
                        yakit = p[4].toInt().coerceIn(0, 100),
                        sonDolum = p[5],
                        sonKm = p[6].toInt(),
                        secili = p[7].toBoolean()
                    )

                    p.size == 5 -> Arac(
                        id = p[0].toLong(),
                        ad = p[1],
                        plaka = p[2],
                        fotoUri = p[3],
                        yakit = 100,
                        sonDolum = "",
                        sonKm = 0,
                        secili = p[4].toBoolean()
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
    prefs(context)
        .edit()
        .putString("genel_notlar", notlar)
        .apply()
}

private fun yukleNotlar(
    context: Context
): String =
    prefs(context)
        .getString("genel_notlar", "")
        ?: ""

private fun kaydetAktifSefer(
    context: Context,
    aktif: AktifSefer?
) {
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

private fun yukleAktifSefer(
    context: Context
): AktifSefer? {
    val p = prefs(context)

    val guzergah = p.getString("aktif_guzergah", null)
        ?: return null

    val km = p.getInt("aktif_cikis_km", -1)
    val saat = p.getString("aktif_cikis_saati", null)
        ?: return null
    val baslangic = p.getLong("aktif_baslangic", 0L)

    if (km < 0 || baslangic <= 0L) return null

    return AktifSefer(
        guzergah = guzergah,
        cikisKm = km,
        cikisSaati = saat,
        baslangicMs = baslangic
    )
}

private fun kaydetTema(
    context: Context,
    tema: Tema
) {
    prefs(context)
        .edit()
        .putString("tema", tema.name)
        .apply()
}

private fun yukleTema(
    context: Context
): Tema {
    return runCatching {
        Tema.valueOf(
            prefs(context).getString(
                "tema",
                Tema.ACIK.name
            ) ?: Tema.ACIK.name
        )
    }.getOrDefault(Tema.ACIK)
}

/* -------------------- YARDIMCI -------------------- */

private fun kaliciUriIzniAl(
    context: Context,
    uri: Uri
) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }
}

private fun navigasyonAc(
    context: Context,
    guzergah: String
) {
    val hedef = guzergah.trim().ifBlank {
        "Akseki Antalya"
    }

    val uri = Uri.parse(
        "geo:0,0?q=${Uri.encode(hedef)}"
    )

    val intent = Intent(
        Intent.ACTION_VIEW,
        uri
    )

    runCatching {
        context.startActivity(intent)
    }.onFailure {
        val webUri = Uri.parse(
            "https://www.google.com/maps/search/?api=1&query=${Uri.encode(hedef)}"
        )

        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    webUri
                )
            )
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
    val dakika = ((bitis - baslangic) / 60000L)
        .coerceAtLeast(0L)

    return "${dakika / 60} saat ${dakika % 60} dakika"
}

private fun sureDakika(
    sure: String
): Int {
    val regex = Regex(
        """(\d+)\s*saat\s*(\d+)\s*dakika"""
    )

    val match = regex.find(sure)
        ?: return 0

    return (match.groupValues[1].toIntOrNull() ?: 0) * 60 +
            (match.groupValues[2].toIntOrNull() ?: 0)
}

private fun formatKm(km: Int): String =
    String.format(
        Locale.getDefault(),
        "%,d",
        km
    )

private fun formatKm(km: Long): String =
    String.format(
        Locale.getDefault(),
        "%,d",
        km
    )
