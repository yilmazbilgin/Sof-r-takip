package com.yilmaz.sofortakip

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
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

data class NotKaydi(
    val id: Long,
    val metin: String
)

enum class AppTheme {
    OKYANUS,
    GECE,
    ZUMRUT
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
    val context = LocalContext.current

    var seciliTema by remember { mutableStateOf(AppTheme.OKYANUS) }
    var temaMenusuAcik by remember { mutableStateOf(false) }

    var seferler by remember { mutableStateOf(yukleSeferler(context)) }
    var aktif by remember { mutableStateOf(false) }

    var guzergah by remember { mutableStateOf("") }
    var cikisKm by remember { mutableStateOf("") }
    var donusKm by remember { mutableStateOf("") }
    var notMetni by remember { mutableStateOf("") }
    var cikisSaati by remember { mutableStateOf("") }
    var baslangicZamani by remember { mutableStateOf(0L) }

    var bitirAcik by remember { mutableStateOf(false) }
    var silinecekSefer by remember { mutableStateOf<Sefer?>(null) }
    var hataMesaji by remember { mutableStateOf("") }

    var notlarAcik by remember { mutableStateOf(false) }
    var notlar by remember { mutableStateOf(listOf<NotKaydi>()) }
    var yeniNot by remember { mutableStateOf("") }

    val toplamKm = seferler.sumOf { it.toplamKm }

    val renkler = when (seciliTema) {
        AppTheme.OKYANUS -> androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF087EA4),
            secondary = Color(0xFF4EA8C7),
            background = Color(0xFFF5FAFC),
            surface = Color.White
        )
        AppTheme.GECE -> androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF8AB4F8),
            secondary = Color(0xFF9CC7FF),
            background = Color(0xFF101418),
            surface = Color(0xFF1A2026)
        )
        AppTheme.ZUMRUT -> androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF087F73),
            secondary = Color(0xFF42A89D),
            background = Color(0xFFF3FAF8),
            surface = Color.White
        )
    }

    MaterialTheme(colorScheme = renkler) {
        val anaRenk = MaterialTheme.colorScheme.primary
        val kartRengi = MaterialTheme.colorScheme.surface
        val softRenk = if (seciliTema == AppTheme.GECE) {
            Color(0xFF222A31)
        } else {
            Color(0xFFF0EEF3)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "\u015Eof\u00F6r Takip",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sefer y\u00F6netimi",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { temaMenusuAcik = true }) {
                            Text("Tema")
                        }

                        DropdownMenu(
                            expanded = temaMenusuAcik,
                            onDismissRequest = { temaMenusuAcik = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Okyanus") },
                                onClick = {
                                    seciliTema = AppTheme.OKYANUS
                                    temaMenusuAcik = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Gece") },
                                onClick = {
                                    seciliTema = AppTheme.GECE
                                    temaMenusuAcik = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Z\u00FCmr\u00FCt") },
                                onClick = {
                                    seciliTema = AppTheme.ZUMRUT
                                    temaMenusuAcik = false
                                }
                            )
                        }

                        TextButton(onClick = { notlarAcik = true }) {
                            Text("Notlar")
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
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    HeroCard(
                        aktif = aktif,
                        guzergah = guzergah,
                        cikisKm = cikisKm,
                        anaRenk = anaRenk,
                        kartRengi = kartRengi,
                        onStart = {
                            hataMesaji = when {
                                guzergah.isBlank() ->
                                    "G\u00FCzergah girin."
                                cikisKm.toIntOrNull() == null ->
                                    "Ge\u00E7erli bir \u00E7\u0131k\u0131\u015F KM girin."
                                else -> ""
                            }

                            if (hataMesaji.isBlank()) {
                                aktif = true
                                cikisSaati = saat()
                                baslangicZamani = System.currentTimeMillis()
                            }
                        },
                        onFinish = {
                            bitirAcik = true
                        }
                    )
                }

                if (!aktif) {
                    item {
                        FormCard(
                            guzergah = guzergah,
                            cikisKm = cikisKm,
                            onGuzergahChange = { guzergah = it },
                            onCikisKmChange = { cikisKm = it }
                        )
                    }
                } else {
                    item {
                        ActiveTripCard(
                            guzergah = guzergah,
                            cikisKm = cikisKm,
                            cikisSaati = cikisSaati,
                            anaRenk = anaRenk
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IstatistikKarti(
                            modifier = Modifier.weight(1f),
                            ikon = Icons.Default.DirectionsCar,
                            deger = seferler.size.toString(),
                            baslik = "Toplam Sefer",
                            kartRengi = softRenk
                        )

                        IstatistikKarti(
                            modifier = Modifier.weight(1f),
                            ikon = Icons.Default.Speed,
                            deger = "$toplamKm KM",
                            baslik = "Toplam KM",
                            kartRengi = softRenk
                        )
                    }
                }

                item {
                    SectionTitle(
                        ikon = Icons.Default.History,
                        baslik = "Sefer Ge\u00E7mi\u015Fi"
                    )
                }

                if (seferler.isEmpty()) {
                    item {
                        EmptyHistoryCard(kartRengi = softRenk)
                    }
                } else {
                    items(
                        items = seferler,
                        key = { it.id }
                    ) { sefer ->
                        SeferKarti(
                            sefer = sefer,
                            kartRengi = softRenk,
                            anaRenk = anaRenk,
                            onLongClick = {
                                silinecekSefer = sefer
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (hataMesaji.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { hataMesaji = "" },
                title = { Text("Bilgi", fontWeight = FontWeight.Bold) },
                text = { Text(hataMesaji) },
                confirmButton = {
                    TextButton(onClick = { hataMesaji = "" }) {
                        Text("Tamam")
                    }
                }
            )
        }

        if (bitirAcik) {
            AlertDialog(
                onDismissRequest = { bitirAcik = false },
                title = {
                    Text(
                        "Seferi Tamamla",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "G\u00FCzergah: $guzergah",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = donusKm,
                            onValueChange = {
                                donusKm = it.filter { karakter ->
                                    karakter.isDigit()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("D\u00F6n\u00FC\u015F KM") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = notMetni,
                            onValueChange = { notMetni = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Not") },
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cikis = cikisKm.toIntOrNull()
                            val donus = donusKm.toIntOrNull()

                            if (cikis == null || donus == null) {
                                hataMesaji =
                                    "\u00C7\u0131k\u0131\u015F ve d\u00F6n\u00FC\u015F KM bilgilerini girin."
                                return@Button
                            }

                            if (donus < cikis) {
                                hataMesaji =
                                    "D\u00F6n\u00FC\u015F KM, \u00E7\u0131k\u0131\u015F KM'den k\u00FC\u00E7\u00FCk olamaz."
                                return@Button
                            }

                            val bitis = System.currentTimeMillis()

                            val yeniSefer = Sefer(
                                id = bitis,
                                guzergah = guzergah.trim(),
                                cikisKm = cikis,
                                donusKm = donus,
                                cikisSaati = cikisSaati,
                                donusSaati = saat(),
                                toplamKm = donus - cikis,
                                toplamSure = sureHesapla(
                                    baslangicZamani,
                                    bitis
                                ),
                                notMetni = notMetni.trim()
                            )

                            seferler = listOf(yeniSefer) + seferler
                            kaydetSeferler(context, seferler)

                            aktif = false
                            bitirAcik = false
                            guzergah = ""
                            cikisKm = ""
                            donusKm = ""
                            notMetni = ""
                            cikisSaati = ""
                            baslangicZamani = 0L
                        }
                    ) {
                        Text("SEFER\u0130 TAMAMLA")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bitirAcik = false }) {
                        Text("VAZGE\u00C7")
                    }
                }
            )
        }

        if (silinecekSefer != null) {
            AlertDialog(
                onDismissRequest = { silinecekSefer = null },
                title = {
                    Text("Sefer silinsin mi?")
                },
                text = {
                    Text(
                        "Bu sefer ge\u00E7mi\u015Ften kal\u0131c\u0131 olarak silinecek."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val silinecek = silinecekSefer
                            if (silinecek != null) {
                                seferler = seferler.filter {
                                    it.id != silinecek.id
                                }
                                kaydetSeferler(context, seferler)
                            }
                            silinecekSefer = null
                        }
                    ) {
                        Text("S\u0130L")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { silinecekSefer = null }) {
                        Text("VAZGE\u00C7")
                    }
                }
            )
        }

        if (notlarAcik) {
            AlertDialog(
                onDismissRequest = { notlarAcik = false },
                title = {
                    Text("Notlar", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = yeniNot,
                            onValueChange = { yeniNot = it },
                            label = { Text("Yeni not") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        notlar.forEach { not ->
                            Text(
                                text = "\u2022 ${not.metin}",
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (yeniNot.isNotBlank()) {
                                notlar = notlar + NotKaydi(
                                    id = System.currentTimeMillis(),
                                    metin = yeniNot.trim()
                                )
                                yeniNot = ""
                            }
                        }
                    ) {
                        Text("Ekle")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { notlarAcik = false }) {
                        Text("Kapat")
                    }
                }
            )
        }
    }
}

@Composable
fun HeroCard(
    aktif: Boolean,
    guzergah: String,
    cikisKm: String,
    anaRenk: Color,
    kartRengi: Color,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (aktif) Color(0xFFE8F5E9) else kartRengi
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Hen\u00FCz tamamlanm\u0131\u015F sefer yok.",
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeferKarti(
    sefer: Sefer,
    kartRengi: Color,
    anaRenk: Color,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = kartRengi
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        sefer.guzergah,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier
                                .width(18.dp)
                                .height(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${sefer.cikisSaati} - ${sefer.donusSaati}",
                            color = Color.Gray
                        )
                    }
                }

                Text(
                    "${sefer.toplamKm} KM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = anaRenk
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "${sefer.cikisKm} KM - ${sefer.donusKm} KM",
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "${sefer.toplamKm} KM  \u2022  ${sefer.toplamSure}",
                color = anaRenk,
                fontWeight = FontWeight.Bold
            )

            if (sefer.notMetni.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Not: ${sefer.notMetni}",
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Silmek i\u00E7in uzun bas\u0131n",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

fun saat(): String {
    return SimpleDateFormat(
        "HH:mm",
        Locale.getDefault()
    ).format(Date())
}

fun sureHesapla(
    baslangic: Long,
    bitis: Long
): String {
    if (baslangic <= 0L) {
        return "0 saat 0 dakika"
    }

    val dakika = ((bitis - baslangic) / 60000L)
        .coerceAtLeast(0L)

    val saat = dakika / 60
    val kalan = dakika % 60

    return "$saat saat $kalan dakika"
}

fun kaydetSeferler(
    context: Context,
    seferler: List<Sefer>
) {
    val veri = seferler.joinToString(
        separator = "\u001E"
    ) {
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

    context
        .getSharedPreferences(
            "sofor_takip",
            Context.MODE_PRIVATE
        )
        .edit()
        .putString("seferler", veri)
        .apply()
}

fun yukleSeferler(
    context: Context
): List<Sefer> {
    val veri = context
        .getSharedPreferences(
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
