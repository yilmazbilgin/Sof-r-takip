package com.yilmaz.sofortakip

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val notMetni: String
)
data class Arac(
    val id: Long,
    val ad: String,
    val plaka: String,
    val fotoUri: String = "",
    val secili: Boolean = false
)
private enum class Tema { ACIK, MAVİ, GECE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SoforTakip() }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SoforTakip() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var seferler by remember { mutableStateOf(yukleSeferler(context)) }
    var notlar by remember { mutableStateOf(yukleNotlar(context)) }
    var tema by remember { mutableStateOf(Tema.ACIK) }
    var temaMenu by remember { mutableStateOf(false) }
    var notlarAcik by remember { mutableStateOf(false) }
    var araclarAcik by remember { mutableStateOf(false) }
    var araclar by remember {
    mutableStateOf(yukleAraclar(context))
    }
    var aktif by remember { mutableStateOf(false) }

    var guzergah by remember { mutableStateOf("") }
    var cikisKm by remember { mutableStateOf("") }
    var donusKm by remember { mutableStateOf("") }
    var notMetni by remember { mutableStateOf("") }
    var cikisSaati by remember { mutableStateOf("") }
    var baslangic by remember { mutableStateOf(0L) }

    var bitirDialog by remember { mutableStateOf(false) }
    var bilgi by remember { mutableStateOf("") }
    var silinecek by remember { mutableStateOf<Sefer?>(null) }

    val toplamKm = seferler.sumOf { it.toplamKm }

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
                            Text("Şoför Takip", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Günlük sefer paneli",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { notlarAcik = true }) {
                            Icon(Icons.Default.Note, contentDescription = "Notlar")
                            Spacer(Modifier.width(5.dp))
                            Text("Notlar", fontWeight = FontWeight.Bold)
                        }
                        Box {
                            IconButton(onClick = { temaMenu = true }) {
                                Icon(Icons.Default.Palette, contentDescription = "Tema")
                            }
                            DropdownMenu(
                                expanded = temaMenu,
                                onDismissRequest = { temaMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Açık") },
                                    onClick = { tema = Tema.ACIK; temaMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mavi") },
                                    onClick = { tema = Tema.MAVİ; temaMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gece") },
                                    onClick = { tema = Tema.GECE; temaMenu = false }
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ProfessionalHeader(
                        aktif = aktif,
                        seferSayisi = seferler.size,
                        toplamKm = toplamKm
                    )
                }

                item {
                    if (!aktif) {
                        NewTripCard(
                            guzergah = guzergah,
                            cikisKm = cikisKm,
                            onGuzergah = { guzergah = it },
                            onKm = { cikisKm = it },
                            onStart = {
                                when {
                                    guzergah.isBlank() -> bilgi = "Lütfen güzergah girin."
                                    cikisKm.toIntOrNull() == null ->
                                        bilgi = "Geçerli bir çıkış KM girin."
                                    else -> {
                                        aktif = true
                                        cikisSaati = saat()
                                        baslangic = System.currentTimeMillis()
                                    }
                                }
                            }
                        )
                    } else {
                        ActiveTripCard(guzergah, cikisKm, cikisSaati)
                        Button(
                            onClick = { bitirDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(8.dp))
                            Text("SEFERİ BİTİR", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickAction(
                            Modifier.weight(1f),
                            Icons.Default.Note,
                            "Not Ekle",
                            "Hızlı not"
                        ) { notlarAcik = true }

                        QuickAction(
                            Modifier.weight(1f),
                            Icons.Default.DirectionsCar,
                            "Araç Bilgisi",
                            "Araçlarım"
                        ) {
                            {
                                araclarAcik = true
                            }
                        }
                    }
                }

                item {
                    SectionHeader("Özet", "Kayıtlı seferleriniz")
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardStat(
                            Modifier.weight(1f),
                            Icons.Default.DirectionsCar,
                            seferler.size.toString(),
                            "Toplam Sefer"
                        )
                        DashboardStat(
                            Modifier.weight(1f),
                            Icons.Default.Speed,
                            "$toplamKm KM",
                            "Toplam KM"
                        )
                    }
                }

                item {
                    SectionHeader(
                        "Sefer Geçmişi",
                        if (seferler.isEmpty()) "Henüz kayıt yok"
                        else "${seferler.size} tamamlanmış sefer"
                    )
                }

                if (seferler.isEmpty()) {
                    item { EmptyCard() }
                } else {
                    items(seferler, key = { it.id }) { sefer ->
                        TripCard(sefer) { silinecek = sefer }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
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
        onSave = { yeniArac ->
            araclar = araclar + yeniArac
            kaydetAraclar(context, araclar)
        },
        onDelete = { silinecekArac ->
            araclar = araclar.filter {
                it.id != silinecekArac.id
            }
            kaydetAraclar(context, araclar)
        }
    )
        }

        if (bitirDialog) {
            AlertDialog(
                onDismissRequest = { bitirDialog = false },
                title = { Text("Seferi Tamamla", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(guzergah, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Çıkış: $cikisKm KM • $cikisSaati",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = donusKm,
                            onValueChange = { donusKm = it.filter(Char::isDigit) },
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
                            onValueChange = { notMetni = it },
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
                        onClick = {
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
                                        notMetni = notMetni.trim()
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
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(6.dp))
                        Text("KAYDET")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bitirDialog = false }) { Text("VAZGEÇ") }
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
                text = { Text("\"${sefer.guzergah}\" seferi geçmişten silinecek.") },
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
                    TextButton(onClick = { silinecek = null }) { Text("VAZGEÇ") }
                }
            )
        }
    }
}

@Composable
private fun ProfessionalHeader(aktif: Boolean, seferSayisi: Int, toplamKm: Int) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.size(64.dp),
                    RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = .16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            null,
                            Modifier.size(38.dp),
                            tint = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (aktif) "Sefer devam ediyor" else "Bugün hazır mısınız?",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        if (aktif) "Aktif seferinizi takip edin."
                        else "Yeni seferinizi hızlıca kaydedin.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = .84f)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniMetric(Modifier.weight(1f), seferSayisi.toString(), "Sefer")
                MiniMetric(Modifier.weight(1f), toplamKm.toString(), "KM")
            }
        }
    }
}

@Composable
private fun MiniMetric(modifier: Modifier, value: String, label: String) {
    Surface(
        modifier,
        RoundedCornerShape(15.dp),
        color = Color.White.copy(alpha = .13f)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = .82f))
        }
    }
}

@Composable
private fun NewTripCard(
    guzergah: String,
    cikisKm: String,
    onGuzergah: (String) -> Unit,
    onKm: (String) -> Unit,
    onStart: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.size(44.dp),
                    RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("Yeni Sefer", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Çıkış bilgilerini girin",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = guzergah,
                onValueChange = onGuzergah,
                Modifier.fillMaxWidth(),
                label = { Text("Güzergah") },
                placeholder = { Text("Örn. Akseki - Antalya") },
                leadingIcon = { Icon(Icons.Default.DirectionsCar, null) },
                singleLine = true,
                shape = RoundedCornerShape(17.dp)
            )

            OutlinedTextField(
                value = cikisKm,
                onValueChange = { onKm(it.filter(Char::isDigit)) },
                Modifier.fillMaxWidth(),
                label = { Text("Çıkış KM") },
                placeholder = { Text("Örn. 125430") },
                leadingIcon = { Icon(Icons.Default.Speed, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(17.dp)
            )

            Button(
                onClick = onStart,
                Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("SEFERİ BAŞLAT", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Surface(
                Modifier.size(40.dp),
                RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            subtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DashboardStat(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(17.dp)) {
            Surface(
                Modifier.size(40.dp),
                RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveTripCard(guzergah: String, cikisKm: String, cikisSaati: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F6EC))
    ) {
        Row(
            Modifier.padding(19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                Modifier.size(52.dp),
                RoundedCornerShape(16.dp),
                color = Color(0xFF2E9D52)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column {
                Text(
                    "SEFER DEVAM EDİYOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF238636)
                )
                Text(guzergah, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Çıkış $cikisKm KM • $cikisSaati",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripCard(sefer: Sefer, onLongClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    Modifier.size(46.dp),
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
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(sefer.guzergah, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            null,
                            Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${sefer.cikisSaati} → ${sefer.donusSaati}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "${sefer.toplamKm} KM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(13.dp))
            HorizontalDivider()
            Spacer(Modifier.height(11.dp))

            Text(
                "${sefer.cikisKm} KM → ${sefer.donusKm} KM",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "${sefer.toplamKm} KM • ${sefer.toplamSure}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (sefer.notMetni.isNotBlank()) {
                Spacer(Modifier.height(11.dp))
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = .07f)
                ) {
                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Note,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            sefer.notMetni,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(9.dp))
            Text(
                "Silmek için uzun basın",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyCard() {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.History,
                null,
                Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Henüz tamamlanmış sefer yok.",
                fontSize = 15.sp,
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
                Modifier.fillMaxWidth(),
                label = { Text("Not") },
                placeholder = { Text("Örn. araç kontrolü, evrak, özel bilgi...") },
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

private fun saat(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun sureHesapla(baslangic: Long, bitis: Long): String {
    if (baslangic <= 0L) return "0 saat 0 dakika"
    val dakika = ((bitis - baslangic) / 60000L).coerceAtLeast(0L)
    return "${dakika / 60} saat ${dakika % 60} dakika"
}

private fun kaydetSeferler(context: Context, seferler: List<Sefer>) {
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
            it.notMetni
        ).joinToString("\u001F")
    }

    context.getSharedPreferences("sofor_takip", Context.MODE_PRIVATE)
        .edit()
        .putString("seferler", veri)
        .apply()
}

private fun yukleSeferler(context: Context): List<Sefer> {
    val veri = context.getSharedPreferences("sofor_takip", Context.MODE_PRIVATE)
        .getString("seferler", "") ?: ""

    if (veri.isBlank()) return emptyList()

    return veri.split("\u001E").mapNotNull { satir ->
        val p = satir.split("\u001F")
        if (p.size != 9) return@mapNotNull null

        try {
            Sefer(
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
        } catch (_: Exception) {
            null
        }
    }
}
@Composable
private fun AraclarDialog(
    araclar: List<Arac>,
    onClose: () -> Unit,
    onSave: (Arac) -> Unit,
    onDelete: (Arac) -> Unit
) {
    var aracAdi by remember { mutableStateOf("") }
    var plaka by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                "Araçlarım",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (araclar.isEmpty()) {
                    Text("Henüz araç eklenmedi.")
                } else {
                    araclar.forEach { arac ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        arac.ad,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(arac.plaka)
                                }

                                TextButton(
                                    onClick = {
                                        onDelete(arac)
                                    }
                                ) {
                                    Text("Sil")
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = aracAdi,
                    onValueChange = { aracAdi = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Araç adı") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = plaka,
                    onValueChange = { plaka = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Plaka") },
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
                                ad = aracAdi,
                                plaka = plaka
                            )
                        )
                        aracAdi = ""
                        plaka = ""
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
private fun kaydetNotlar(context: Context, notlar: String) {
    context.getSharedPreferences("sofor_takip", Context.MODE_PRIVATE)
        .edit()
        .putString("genel_notlar", notlar)
        .apply()
}

private fun yukleNotlar(context: Context): String {
    return context.getSharedPreferences("sofor_takip", Context.MODE_PRIVATE)
        .getString("genel_notlar", "") ?: ""
}
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
        .getString("araclar", "")
        ?: ""

    if (veri.isBlank()) {
        return emptyList()
    }

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
