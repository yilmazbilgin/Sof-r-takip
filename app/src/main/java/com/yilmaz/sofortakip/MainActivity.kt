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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
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
    GECE_MORU,
    OKYANUS,
    ZUMRUT,
    GRAFIT,
    ACIK
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

    var seciliTema by remember { mutableStateOf(AppTheme.GECE_MORU) }
    var temaMenusuAcik by remember { mutableStateOf(false) }
    var seferler by remember { mutableStateOf(yukleSeferler(context)) }
    var aktif by remember { mutableStateOf(false) }
    var bitirAcik by remember { mutableStateOf(false) }
    var silinecekSefer by remember { mutableStateOf<Sefer?>(null) }
    var guzergah by remember { mutableStateOf("") }
    var cikisKm by remember { mutableStateOf("") }
    var donusKm by remember { mutableStateOf("") }
    var notMetni by remember { mutableStateOf("") }
    var cikisSaati by remember { mutableStateOf("") }
    var baslangicZamani by remember { mutableStateOf(0L) }
    var hataMesaji by remember { mutableStateOf("") }
    var notlarAcik by remember { mutableStateOf(false) }
    var notlar by remember { mutableStateOf(listOf<NotKaydi>()) }
    var yeniNot by remember { mutableStateOf("") }

    val toplamKm = seferler.sumOf { it.toplamKm }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("艦of枚r Takip", fontWeight = FontWeight.Bold)
                            Text("Sefer y枚netimi", fontSize = 12.sp, color = Color.Gray)
                        }
                    },
                    actions = {
                        TextButton(onClick = { temaMenusuAcik = true }) { Text("Tema") }
                        DropdownMenu(
                            expanded = temaMenusuAcik,
                            onDismissRequest = { temaMenusuAcik = false }
                        ) {
                            DropdownMenuItem(text = { Text("馃寵 Gece Moru") }, onClick = { seciliTema = AppTheme.GECE_MORU; temaMenusuAcik = false })
                            DropdownMenuItem(text = { Text("馃寠 Okyanus") }, onClick = { seciliTema = AppTheme.OKYANUS; temaMenusuAcik = false })
                            DropdownMenuItem(text = { Text("馃挌 Z眉mr眉t") }, onClick = { seciliTema = AppTheme.ZUMRUT; temaMenusuAcik = false })
                            DropdownMenuItem(text = { Text("馃┒ Grafit") }, onClick = { seciliTema = AppTheme.GRAFIT; temaMenusuAcik = false })
                            DropdownMenuItem(text = { Text("鈽€锔� A莽谋k") }, onClick = { seciliTema = AppTheme.ACIK; temaMenusuAcik = false })
                        }
                        TextButton(onClick = { notlarAcik = true }) { Text("Notlar") }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (aktif) Color(0xFFE8F5E9) else Color(0xFFEDE9EF)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, null, Modifier.width(48.dp).height(48.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(if (aktif) "Aktif Sefer" else "Yeni Sefer", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (aktif) "$guzergah 鈥� $cikisKm KM" else "Yeni bir sefer ba艧lat谋n",
                                        fontSize = 16.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))

                            if (!aktif) {
                                Button(
                                    onClick = {
                                        hataMesaji = ""
                                        if (guzergah.isBlank()) {
                                            hataMesaji = "G眉zerg芒h girin."
                                            return@Button
                                        }
                                        if (cikisKm.toIntOrNull() == null) {
                                            hataMesaji = "Ge莽erli bir 莽谋k谋艧 KM girin."
                                            return@Button
                                        }
                                        aktif = true
                                        cikisSaati = saat()
                                        baslangicZamani = System.currentTimeMillis()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SEFER陌 BA艦LAT", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Sefer devam ediyor", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("脟谋k谋艧: $cikisKm KM 鈥� $cikisSaati", color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { bitirAcik = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Icon(Icons.Default.Stop, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SEFER陌 B陌T陌R", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (!aktif) {
                    item {
                        OutlinedTextField(
                            value = guzergah,
                            onValueChange = { guzergah = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("G眉zerg芒h") },
                            placeholder = { Text("脰rn. Akseki - Antalya") },
                            leadingIcon = { Icon(Icons.Default.Route, null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = cikisKm,
                            onValueChange = { cikisKm = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("脟谋k谋艧 KM") },
                            placeholder = { Text("脰rn. 125430") },
                            leadingIcon = { Icon(Icons.Default.Speed, null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BilgiKarti(
                            modifier = Modifier.weight(1f),
                            ikon = Icons.Default.DirectionsCar,
                            baslik = seferler.size.toString(),
                            altBaslik = "Toplam Sefer"
                        )
                        BilgiKarti(
                            modifier = Modifier.weight(1f),
                            ikon = Icons.Default.Speed,
                            baslik = "$toplamKm KM",
                            altBaslik = "Toplam KM"
                        )
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sefer Ge莽mi艧i", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (seferler.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.History, null)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Hen眉z tamamlanm谋艧 sefer yok.", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(items = seferler, key = { it.id }) { sefer ->
                        Card(
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = {},
                                onLongClick = { silinecekSefer = sefer }
                            ),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9EF))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sefer.guzergah, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccessTime, null, Modifier.width(18.dp).height(18.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("${sefer.cikisSaati} 鈫� ${sefer.donusSaati}", color = Color.Gray)
                                        }
                                    }
                                    Text("${sefer.toplamKm} KM", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1769AA))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("${sefer.cikisKm} KM 鈫� ${sefer.donusKm} KM", color = Color.Gray)
                                Spacer(modifier = Modifier.height(5.dp))
                                Text("${sefer.toplamKm} KM 鈥� ${sefer.toplamSure}", color = Color(0xFF1769AA), fontWeight = FontWeight.Bold)
                                if (sefer.notMetni.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Not: ${sefer.notMetni}", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Silmek i莽in uzun bas谋n", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }

        if (notlarAcik) {
            AlertDialog(
                onDismissRequest = { notlarAcik = false },
                title = { Text("Notlar", fontWeight = FontWeight.Bold) },
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
                            Text("鈥� ${not.metin}", modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (yeniNot.isNotBlank()) {
                            notlar = notlar + NotKaydi(System.currentTimeMillis(), yeniNot.trim())
                            yeniNot = ""
                        }
                    }) { Text("Ekle") }
                },
                dismissButton = {
                    TextButton(onClick = { notlarAcik = false }) { Text("Kapat") }
                }
            )
        }

        if (hataMesaji.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { hataMesaji = "" },
                title = { Text("Bilgi") },
                text = { Text(hataMesaji) },
                confirmButton = {
                    TextButton(onClick = { hataMesaji = "" }) { Text("Tamam") }
                }
            )
        }

        if (bitirAcik) {
            AlertDialog(
                onDismissRequest = { bitirAcik = false },
                title = { Text("Seferi Tamamla", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("G眉zerg芒h: $guzergah", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = donusKm,
                            onValueChange = { donusKm = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("D枚n眉艧 KM") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = notMetni,
                            onValueChange = { notMetni = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Not") },
                            placeholder = { Text("脰rn. Evrak teslim edildi") },
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val cikis = cikisKm.toIntOrNull()
                        val donus = donusKm.toIntOrNull()
                        if (cikis == null || donus == null) {
                            hataMesaji = "脟谋k谋艧 ve d枚n眉艧 KM bilgilerini girin."
                            return@Button
                        }
                        if (donus < cikis) {
                            hataMesaji = "D枚n眉艧 KM, 莽谋k谋艧 KM'den k眉莽眉k olamaz."
                            return@Button
                        }
                        val bitis = System.currentTimeMillis()
                        val yeniSefer = Sefer(
                            id = bitis,
                            guzergah = guzergah,
                            cikisKm = cikis,
                            donusKm = donus,
                            cikisSaati = cikisSaati,
                            donusSaati = saat(),
                            toplamKm = donus - cikis,
                            toplamSure = sureHesapla(baslangicZamani, bitis),
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
                    }) { Text("SEFER陌 TAMAMLA") }
                },
                dismissButton = {
                    TextButton(onClick = { bitirAcik = false }) { Text("Vazge莽") }
                }
            )
        }

        if (silinecekSefer != null) {
            AlertDialog(
                onDismissRequest = { silinecekSefer = null },
                title = { Text("Sefer silinsin mi?") },
                text = { Text("Bu sefer ge莽mi艧ten kal谋c谋 olarak silinecek.") },
                confirmButton = {
                    Button(onClick = {
                        val silinecek = silinecekSefer
                        if (silinecek != null) {
                            seferler = seferler.filter { it.id != silinecek.id }
                            kaydetSeferler(context, seferler)
                        }
                        silinecekSefer = null
                    }) { Text("S陌L") }
                },
                dismissButton = {
                    TextButton(onClick = { silinecekSefer = null }) { Text("VAZGE脟") }
                }
            )
        }
    }
}

@Composable
fun BilgiKarti(
    modifier: Modifier,
    ikon: androidx.compose.ui.graphics.vector.ImageVector,
    baslik: String,
    altBaslik: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE9EF))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Icon(imageVector = ikon, contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(baslik, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(altBaslik, color = Color.Gray)
        }
    }
}

fun saat(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

fun sureHesapla(baslangic: Long, bitis: Long): String {
    if (baslangic <= 0L) return "0 saat 0 dakika"
    val dakika = ((bitis - baslangic) / 60000L).coerceAtLeast(0L)
    val saat = dakika / 60
    val kalan = dakika % 60
    return "$saat saat $kalan dakika"
}

fun kaydetSeferler(context: Context, seferler: List<Sefer>) {
    val veri = seferler.joinToString(separator = "\u001E") {
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

fun yukleSeferler(context: Context): List<Sefer> {
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
