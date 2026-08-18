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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
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
            secondary = Color(0xFF1976D2),
            background = Color(0xFFF7F5FC),
            surfaceVariant = Color(0xFFEDE9F5)
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
                NavigationBar {
                    BottomItem("Ana Sayfa", Icons.Default.Home, sayfa == 0) { sayfa = 0 }
                    BottomItem("Geçmiş", Icons.Default.History, sayfa == 1) { sayfa = 1 }
                    BottomItem("Raporlar", Icons.Default.BarChart, sayfa == 2) { sayfa = 2 }
                    BottomItem("Ayarlar", Icons.Default.Settings, sayfa == 3) { sayfa = 3 }
                }
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
                    cikisSaati = cikisSaati,
                    onGuzergah = { guzergah = it },
                    onCikisKm = { cikisKm = it },
                    onStart = {
                        when {
                            seciliArac == null ->
                                bilgi = "Önce Araç Bilgisi bölümünden bir araç ekleyin."
                            guzergah.isBlank() ->
                                bilgi = "Lütfen güzergah girin."
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
                    onTheme = { },
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
            text = {
                Text("\"${sefer.guzergah}\" seferi geçmişten silinecek.")
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
private fun RowScope.BottomItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null) },
        label = { Text(label) }
    )
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
    onCikisKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onNavigation: () -> Unit,
    onNotes: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
    onVehicles: () -> Unit,
    onHistory: () -> Unit,
    onTheme: () -> Unit,
    onThemeSelect: (Tema) -> Unit
) {
    var temaMenu by remember { mutableStateOf(false) }
    var anaMenu by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Header(
    onNotes = onNotes,
    onMenu = { anaMenu = true },
    anaMenu = anaMenu,
    onDismissMenu = { anaMenu = false },
                onTheme = {
                    temaMenu = true
                    onTheme()
                },
                themeMenu = temaMenu,
                onDismissTheme = { temaMenu = false },
                onThemeSelect = {
                    temaMenu = false
                    onThemeSelect(it)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Yeni Sefer",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                TextButton(onClick = onVehicles) {
                    Icon(Icons.Default.DirectionsCar, null)
                    Spacer(Modifier.width(5.dp))
                    Text("Araç Bilgisi", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            NewTripProfessional(
                seciliArac = seciliArac,
                aktif = aktif,
                guzergah = guzergah,
                cikisKm = cikisKm,
                cikisSaati = cikisSaati,
                onGuzergah = onGuzergah,
                onCikisKm = onCikisKm,
                onStart = onStart,
                onFinish = onFinish
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
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onHistory) {
                    Text("Tümünü Gör")
                }
            }
        }

        if (seferler.isEmpty()) {
            item {
                EmptyCard()
            }
        } else {
            items(seferler.take(4), key = { it.id }) { sefer ->
                HomeTripCard(sefer)
            }
        }
    }
}

@Composable
private fun Header(
    onNotes: () -> Unit,
    onTheme: () -> Unit,
    onMenu: () -> Unit,
onDismissMenu: () -> Unit,
    anaMenu: Boolean,
    themeMenu: Boolean,
    onDismissTheme: () -> Unit,
    onThemeSelect: (Tema) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Menü
            IconButton(
                onClick = onMenu,
                modifier = Modifier.size(44.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menü",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(5.dp))

            // Logo
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Şoför Takip",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.width(7.dp))

            // Başlık
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Şoför Takip",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )

                Text(
                    text = "Günlük sefer paneli",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Notlar
            IconButton(
                onClick = onNotes,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Note,
                    contentDescription = "Notlar",
                    modifier = Modifier.size(25.dp)
                )
            }

            // Tema
            IconButton(
                onClick = onTheme,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Tema",
                    modifier = Modifier.size(25.dp)
                )
            }
        }
DropdownMenu(
    expanded = anaMenu,
    onDismissRequest = onDismissMenu
) {
    DropdownMenuItem(
        text = { Text("Ana Sayfa") },
        onClick = onDismissMenu
    )

    DropdownMenuItem(
        text = { Text("Geçmiş") },
        onClick = onDismissMenu
    )

    DropdownMenuItem(
        text = { Text("Raporlar") },
        onClick = onDismissMenu
    )

    DropdownMenuItem(
        text = { Text("Araçlar") },
        onClick = onDismissMenu
    )

    DropdownMenuItem(
        text = { Text("Ayarlar") },
        onClick = onDismissMenu
    )
}
        DropdownMenu(
            expanded = themeMenu,
            onDismissRequest = onDismissTheme
        ) {
            DropdownMenuItem(
                text = { Text("Açık") },
                onClick = {
                    onThemeSelect(Tema.ACIK)
                }
            )

            DropdownMenuItem(
                text = { Text("Mavi") },
                onClick = {
                    onThemeSelect(Tema.MAVİ)
                }
            )

            DropdownMenuItem(
                text = { Text("Gece") },
                onClick = {
                    onThemeSelect(Tema.GECE)
                }
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
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (arac?.fotoUri?.isNotBlank() == true) {
                UriImage(
                    Uri.parse(arac.fotoUri),
                    Modifier
                        .size(106.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
            } else {
                Surface(
                    Modifier.size(106.dp),
                    RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = .13f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(55.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    "Bugün hazır mısınız?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    if (arac == null) {
                        "Aracınızı ekleyerek başlayın."
                    } else {
                        "${arac.ad} • ${arac.plaka}"
                    },
                    color = Color.White.copy(alpha = .88f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    "$seferSayisi sefer • $toplamKm KM",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
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
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(120.dp, Icons.Default.DirectionsCar, sefer.toString(), "Toplam Sefer")
        StatCard(120.dp, Icons.Default.Speed, km.toString(), "Toplam KM")
        StatCard(
            130.dp,
            Icons.Default.Timer,
            "${sure / 60}s ${sure % 60}dk",
            "Toplam Süre"
        )
        StatCard(
            125.dp,
            Icons.Default.Speed,
            ortalama.toString(),
            "Ort. Sefer KM"
        )
        StatCard(
            120.dp,
            Icons.Default.LocalGasStation,
            "%$yakit",
            "Yakıt"
        )
    }
}

@Composable
private fun StatCard(
    width: Dp,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        Modifier
            .width(width)
            .height(126.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Surface(
                Modifier.size(40.dp),
                RoundedCornerShape(13.dp),
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

            Spacer(Modifier.height(8.dp))

            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NewTripProfessional(
    seciliArac: Arac?,
    aktif: Boolean,
    guzergah: String,
    cikisKm: String,
    cikisSaati: String,
    onGuzergah: (String) -> Unit,
    onCikisKm: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (seciliArac?.fotoUri?.isNotBlank() == true) {
                    UriImage(
                        Uri.parse(seciliArac.fotoUri),
                        Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        if (aktif) "Sefer Devam Ediyor" else "Yeni Sefer",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        if (seciliArac == null) {
                            "Araç seçin"
                        } else {
                            "${seciliArac.ad} • ${seciliArac.plaka}"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (seciliArac != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Yakıt",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "%${seciliArac.yakit}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = guzergah,
                onValueChange = onGuzergah,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Güzergah") },
                placeholder = { Text("Örn. Akseki - Antalya") },
                leadingIcon = { Icon(Icons.Default.Place, null) },
                singleLine = true,
                shape = RoundedCornerShape(17.dp)
            )

            OutlinedTextField(
                value = cikisKm,
                onValueChange = {
                    onCikisKm(it.filter(Char::isDigit))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Çıkış KM") },
                placeholder = { Text("Örn. 125430") },
                leadingIcon = { Icon(Icons.Default.Speed, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(17.dp)
            )

            if (aktif) {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFFE9F7EE)
                ) {
                    Row(
                        Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            null,
                            tint = Color(0xFF238636)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Çıkış saati: $cikisSaati",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = if (aktif) onFinish else onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(57.dp),
                shape = RoundedCornerShape(17.dp),
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
                Spacer(Modifier.width(8.dp))
                Text(
                    if (aktif) "SEFERİ BİTİR" else "SEFERİ BAŞLAT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun QuickActions(
    onNavigation: () -> Unit,
    onNotes: () -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            ActionCard(
                Modifier.weight(1f),
                Icons.Default.Navigation,
                "Navigasyon",
                "Harita aç",
                onNavigation
            )
            ActionCard(
                Modifier.weight(1f),
                Icons.Default.Note,
                "Not Ekle",
                "Hızlı not",
                onNotes
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            ActionCard(
                Modifier.weight(1f),
                Icons.Default.BarChart,
                "Raporlar",
                "Detaylı gör",
                onReports
            )
            ActionCard(
                Modifier.weight(1f),
                Icons.Default.Settings,
                "Ayarlar",
                "Uygulama",
                onSettings
            )
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Surface(
                Modifier.size(43.dp),
                RoundedCornerShape(13.dp),
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

            Spacer(Modifier.height(10.dp))

            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeTripCard(sefer: Sefer) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                Modifier.size(58.dp),
                RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .09f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    sefer.guzergah,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    sefer.tarih,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${sefer.cikisSaati} → ${sefer.donusSaati}",
                    fontSize = 12.sp
                )
                Text(
                    "${sefer.cikisKm} → ${sefer.donusKm} KM",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${sefer.toplamKm}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("KM", fontSize = 11.sp)
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
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(Modifier.padding(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    sefer.guzergah,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    sefer.tarih,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                "${sefer.toplamKm} KM",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "${sefer.cikisKm} → ${sefer.donusKm} KM   •   ${sefer.cikisSaati} → ${sefer.donusSaati}"
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
            PageTitle(
                "Raporlar",
                "Sürüş performans özeti"
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
                    toplamKm.toString()
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
                    ortalamaKm.toString()
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
                            Text(
                                it.guzergah,
                                Modifier.weight(1f)
                            )
                            Text(
                                "${it.toplamKm} KM",
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
        modifier,
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
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
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
private fun PageTitle(title: String, subtitle: String) {
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

            Icon(
                Icons.Default.ChevronRight,
                null
            )
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
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fotoUri = uri.toString()
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
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                if (araclar.isNotEmpty()) {
                    araclar.forEach { arac ->
                        Card(
                            Modifier.fillMaxWidth(),
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
                                            Icon(
                                                Icons.Default.DirectionsCar,
                                                null
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(9.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        arac.ad,
                                        fontWeight = FontWeight.Bold
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
                    label = { Text("Son dolum tarihi") },
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
        if (uri != null) {
            fotoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Araç Bilgilerini Düzenle") },
        text = {
            Column(
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
                    onValueChange = {
                        yakit = it.filter(Char::isDigit)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Yakıt %") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = sonKm,
                    onValueChange = {
                        sonKm = it.filter(Char::isDigit)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Son KM") },
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
                            yakit = yakit.toIntOrNull()?.coerceIn(0, 100)
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
            Text(
                "Seferi Tamamla",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
    var metin by remember(notlar) {
        mutableStateOf(notlar)
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                "Notlar",
                fontWeight = FontWeight.Bold
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
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier,
            RoundedCornerShape(16.dp),
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
            Intent(
                Intent.ACTION_VIEW,
                uri
            )
        )
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

private fun sureDakika(sure: String): Int {
    val regex = Regex("""(\d+)\s*saat\s*(\d+)\s*dakika""")
    val match = regex.find(sure) ?: return 0

    return (match.groupValues[1].toIntOrNull() ?: 0) * 60 +
            (match.groupValues[2].toIntOrNull() ?: 0)
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

    return veri
        .split("\u001E")
        .mapNotNull { satir ->
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

    return veri
        .split("\u001E")
        .mapNotNull { satir ->
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
