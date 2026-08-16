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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private enum class Tema {
    ACIK, GECE, MAVİ
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoforTakip()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SoforTakip() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var seferler by remember { mutableStateOf(yukleSeferler(context)) }
    var tema by remember { mutableStateOf(Tema.ACIK) }
    var temaMenu by remember { mutableStateOf(false) }
    var notlar by remember { mutableStateOf(yukleNotlar(context)) }
    var notlarAcik by remember { mutableStateOf(false) }

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
    val sonSefer = seferler.firstOrNull()

    val colorScheme = when (tema) {
        Tema.GECE -> darkColorScheme(
            primary = Color(0xFF8AB4F8),
            secondary = Color(0xFF9AA9FF),
            background = Color(0xFF0B1016),
            surface = Color(0xFF121A23),
            surfaceVariant = Color(0xFF1B2632)
        )
        Tema.MAVİ -> lightColorScheme(
            primary = Color(0xFF087EA4),
            secondary = Color(0xFF0E7490),
            background = Color(0xFFF4F8FA),
            surface = Color.White,
            surfaceVariant = Color(0xFFE7EFF3)
        )
        Tema.ACIK -> lightColorScheme(
            primary = Color(0xFF5B43B5),
            secondary = Color(0xFF7559C7),
            background = Color(0xFFF8F7FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFEEEAF5)
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Şoför Takip",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Günlük sefer paneli",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { notlarAcik = true }) {
                            Icon(Icons.Default.Note, contentDescription = "Notlar")
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
                                    onClick = {
                                        tema = Tema.ACIK
                                        temaMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mavi") },
                                    onClick = {
                                        tema = Tema.MAVİ
                                        temaMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Gece") },
                                    onClick = {
                                        tema = Tema.GECE
                                        temaMenu = false
                                    }
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
                    Spacer(Modifier.height(4.dp))
                    DashboardHeader(
                        aktif = aktif,
                        seferSayisi = seferler.size,
                        toplamKm = toplamKm
                    )
                }

                if (!aktif) {
                    item {
                        NewTripCard(
                            guzergah = guzergah,
                            cikisKm = cikisKm,
                            onGuzergah = { guzergah = it },
                            onKm = { cikisKm = it },
                            onStart = {
                                when {
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
                            }
                        )
                    }
                } else {
                    item {
                        ActiveTripCard(
                            guzergah = guzergah,
                            cikisKm = cikisKm,
                            cikisSaati = cikisSaati
                        )
                    }

                    item {
                        Button(
                            onClick = { bitirDialog = true },
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
                            Text("SEFERİ BİTİR", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Özet",
                        subtitle = "Kayıtlı seferleriniz"
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardStat(
                            modifier = Modifier.weight(1f),
                            value = seferler.size.toString(),
                            label = "Toplam Sefer",
                            icon = Icons.Default.DirectionsCar
                        )
                        DashboardStat(
                            modifier = Modifier.weight(1f),
                            value = "$toplamKm KM",
                            label = "Toplam KM",
                            icon = Icons.Default.Speed
                        )
                    }
                }

                if (sonSefer != null) {
                    item {
                        LastTripCard(sonSefer)
                    }
                }

                item {
                    SectionHeader(
                        title = "Sefer Geçmişi",
                        subtitle = if (seferler.isEmpty())
                            "Henüz kayıt yok"
                        else
                            "${seferler.size} tamamlanmış sefer"
                    )
                }

                if (seferler.isEmpty()) {
                    item { EmptyCard() }
                } else {
                    items(
                        items = seferler,
                        key = { it.id }
                    ) { sefer ->
                        TripCard(
                            sefer = sefer,
                            onLongClick = { silinecek = sefer }
                        )
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
                    TextButton(onClick = { bilgi = "" }) {
                        Text("Tamam")
                    }
                }
            )
        }

        if (bitirDialog) {
            AlertDialog(
                onDismissRequest = { bitirDialog = false },
                title = {
                    Text("Seferi Tamamla", fontWeight = FontWeight.Bold)
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

                        Text(
                            "Çıkış: $cikisKm KM  •  $cikisSaati",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = donusKm,
                            onValueChange = {
                                donusKm = it.filter(Char::isDigit)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Dönüş KM") },
                            placeholder = { Text("Örn. 125520") },
                            leadingIcon = {
                                Icon(Icons.Default.Speed, null)
                            },
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
                            placeholder = {
                                Text("Örn. Evrak teslim edildi")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Description, null)
                            },
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
                    TextButton(onClick = { bitirDialog = false }) {
                        Text("VAZGEÇ")
                    }
                }
            )
        }

        if (notlarAcik) {
            NotesDialog(
                notlar = notlar,
                onClose = { notlarAcik = false },
                onSave = { yeniNotlar ->
                    notlar = yeniNotlar
                    kaydetNotlar(context, yeniNotlar)
                }
            )
        }

        silinecek?.let { sefer ->
            AlertDialog(
                onDismissRequest = { silinecek = null },
                title = {
                    Text("Seferi sil", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("\"${sefer.guzergah}\" seferi geçmişten silinecek.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            seferler =
                                seferler.filter { it.id != sefer.id }
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
private fun DashboardHeader(
    aktif: Boolean,
    seferSayisi: Int,
    toplamKm: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        null,
                        Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    if (aktif) "Sefer devam ediyor" else "Bugün hazır mısınız?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (aktif)
                        "Aktif seferinizi tamamlayın."
                    else
                        "Yeni seferinizi hızlıca kaydedin.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 18.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniMetric(
                modifier = Modifier.weight(1f),
                value = seferSayisi.toString(),
                label = "Sefer"
            )
            MiniMetric(
                modifier = Modifier.weight(1f),
                value = "$toplamKm",
                label = "KM"
            )
        }
    }
}

@Composable
private fun MiniMetric(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 9.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold
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
private fun NewTripCard(
    guzergah: String,
    cikisKm: String,
    onGuzergah: (String) -> Unit,
    onKm: (String) -> Unit,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Yeni Sefer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Çıkış bilgilerini girin",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = guzergah,
                onValueChange = onGuzergah,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Güzergah") },
                placeholder = { Text("Akseki - Antalya") },
                leadingIcon = {
                    Icon(Icons.Default.DirectionsCar, null)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = cikisKm,
                onValueChange = {
                    onKm(it.filter(Char::isDigit))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Çıkış KM") },
                placeholder = { Text("125430") },
                leadingIcon = {
                    Icon(Icons.Default.Speed, null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "SEFERİ BAŞLAT",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DashboardStat(
    modifier: Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        Modifier.size(21.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                value,
                fontSize = 24.sp,
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
private fun LastTripCard(sefer: Sefer) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.History,
                null,
                Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(11.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Son sefer",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    sefer.guzergah,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "${sefer.toplamKm} KM",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HeroCard(
    aktif: Boolean,
    guzergah: String,
    cikisKm: String,
    cikisSaati: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (aktif) {
                Color(0xFFE5F5EA)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(
                start = 22.dp,
                end = 22.dp,
                top = 22.dp,
                bottom = 20.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(62.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            null,
                            Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(15.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        if (aktif) "Aktif Sefer" else "Yeni Sefer",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        if (aktif) {
                            "$guzergah • $cikisKm KM • $cikisSaati"
                        } else {
                            "Seferinizi hızlı ve düzenli kaydedin"
                        },
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InputPanel(
    guzergah: String,
    cikisKm: String,
    onGuzergah: (String) -> Unit,
    onKm: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = guzergah,
            onValueChange = onGuzergah,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Güzergah") },
            placeholder = { Text("Örn. Akseki - Antalya") },
            leadingIcon = {
                Icon(Icons.Default.DirectionsCar, null)
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )

        OutlinedTextField(
            value = cikisKm,
            onValueChange = {
                onKm(it.filter(Char::isDigit))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Çıkış KM") },
            placeholder = { Text("Örn. 125430") },
            leadingIcon = {
                Icon(Icons.Default.Speed, null)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun ActiveTripCard(
    guzergah: String,
    cikisKm: String,
    cikisSaati: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE5F5EA)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PlayArrow,
                null,
                Modifier.size(34.dp),
                tint = Color(0xFF238636)
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    "SEFER DEVAM EDİYOR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF238636)
                )
                Text(
                    guzergah,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Çıkış $cikisKm KM • $cikisSaati",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    title: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Icon(
                icon,
                null,
                Modifier.size(25.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripCard(
    sefer: Sefer,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        sefer.guzergah,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(5.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "${sefer.cikisSaati}  →  ${sefer.donusSaati}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    "${sefer.toplamKm} KM",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(13.dp))
            Divider()
            Spacer(Modifier.height(11.dp))

            Text(
                "${sefer.cikisKm} KM  →  ${sefer.donusKm} KM",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(7.dp))

            Text(
                "${sefer.toplamKm} KM  •  ${sefer.toplamSure}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (sefer.notMetni.isNotBlank()) {
                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(11.dp),
                        verticalAlignment = Alignment.Top
                    ) {
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.History,
                null,
                Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(9.dp))
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
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Note,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text("Notlar", fontWeight = FontWeight.Bold)
                    Text(
                        "Kendi notlarınızı saklayın",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            OutlinedTextField(
                value = metin,
                onValueChange = { metin = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Not") },
                placeholder = {
                    Text("Örn. araç kontrolü, evrak, özel bilgi...")
                },
                minLines = 6,
                maxLines = 10,
                shape = RoundedCornerShape(16.dp)
            )
        },
        confirmButton = {
            Button(onClick = {
                onSave(metin)
                onClose()
            }) {
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

private fun saat(): String =
    SimpleDateFormat(
        "HH:mm",
        Locale.getDefault()
    ).format(Date())

private fun sureHesapla(
    baslangic: Long,
    bitis: Long
): String {
    if (baslangic <= 0L) {
        return "0 saat 0 dakika"
    }

    val dakika =
        ((bitis - baslangic) / 60000L).coerceAtLeast(0L)

    return "${dakika / 60} saat ${dakika % 60} dakika"
}

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
            it.notMetni
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

    if (veri.isBlank()) {
        return emptyList()
    }

    return veri
        .split("\u001E")
        .mapNotNull { satir ->
            val p = satir.split("\u001F")

            if (p.size != 9) {
                return@mapNotNull null
            }

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
