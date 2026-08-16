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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

private enum class Tema { ACIK, GECE }

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
    var tema by remember { mutableStateOf(Tema.ACIK) }
    var temaMenu by remember { mutableStateOf(false) }

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

    val dark = tema == Tema.GECE
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF63B3ED),
            secondary = Color(0xFF90CDF4),
            background = Color(0xFF0F1419),
            surface = Color(0xFF171D23)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF087EA4),
            secondary = Color(0xFF4EA8C7),
            background = Color(0xFFF4F8FA),
            surface = Color.White
        )
    }

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Şoför Takip", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Sefer yönetimi", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        TextButton(onClick = { temaMenu = true }) { Text("Tema") }
                        DropdownMenu(
                            expanded = temaMenu,
                            onDismissRequest = { temaMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Açık") },
                                onClick = { tema = Tema.ACIK; temaMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Gece") },
                                onClick = { tema = Tema.GECE; temaMenu = false }
                            )
                        }
                    }
                )
            }
        ) { pad ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { DashboardHeader(aktif, guzergah, cikisKm, cikisSaati) }

                if (!aktif) {
                    item {
                        InputPanel(
                            guzergah = guzergah,
                            cikisKm = cikisKm,
                            onGuzergah = { guzergah = it },
                            onKm = { cikisKm = it }
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                bilgi = when {
                                    guzergah.isBlank() -> "Güzergah girin."
                                    cikisKm.toIntOrNull() == null -> "Geçerli bir çıkış KM girin."
                                    else -> ""
                                }
                                if (bilgi.isBlank()) {
                                    aktif = true
                                    cikisSaati = saat()
                                    baslangic = System.currentTimeMillis()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("SEFERİ BAŞLAT", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    item {
                        ActivePanel(guzergah, cikisKm, cikisSaati)
                    }
                    item {
                        Button(
                            onClick = { bitirDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(18.dp)
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
                        StatCard(
                            Modifier.weight(1f),
                            Icons.Default.DirectionsCar,
                            seferler.size.toString(),
                            "Toplam Sefer"
                        )
                        StatCard(
                            Modifier.weight(1f),
                            Icons.Default.Speed,
                            "${seferler.sumOf { it.toplamKm }} KM",
                            "Toplam KM"
                        )
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sefer Geçmişi", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (seferler.isEmpty()) {
                    item { EmptyCard() }
                } else {
                    items(seferler, key = { it.id }) { sefer ->
                        TripCard(sefer) { silinecek = sefer }
                    }
                }

                item { Spacer(Modifier.height(20.dp)) }
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

        if (bitirDialog) {
            AlertDialog(
                onDismissRequest = { bitirDialog = false },
                title = { Text("Seferi Tamamla", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(guzergah, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = donusKm,
                            onValueChange = { donusKm = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Dönüş KM") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.height(10.dp))
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
                    Button(onClick = {
                        val c = cikisKm.toIntOrNull()
                        val d = donusKm.toIntOrNull()
                        when {
                            c == null || d == null -> bilgi = "Çıkış ve dönüş KM bilgilerini girin."
                            d < c -> bilgi = "Dönüş KM, çıkış KM'den küçük olamaz."
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
                    }) { Text("SEFERİ TAMAMLA") }
                },
                dismissButton = {
                    TextButton(onClick = { bitirDialog = false }) { Text("VAZGEÇ") }
                }
            )
        }

        silinecek?.let { sefer ->
            AlertDialog(
                onDismissRequest = { silinecek = null },
                title = { Text("Sefer silinsin mi?") },
                text = { Text("Bu sefer geçmişten silinecek.") },
                confirmButton = {
                    Button(onClick = {
                        seferler = seferler.filter { it.id != sefer.id }
                        kaydetSeferler(context, seferler)
                        silinecek = null
                    }) { Text("SİL") }
                },
                dismissButton = {
                    TextButton(onClick = { silinecek = null }) { Text("VAZGEÇ") }
                }
            )
        }
    }
}

@Composable
private fun DashboardHeader(aktif: Boolean, guzergah: String, km: String, saat: String) {
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (aktif) Color(0xFFE6F4EA)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (aktif) Icons.Default.DirectionsCar else Icons.Default.Route,
                    null,
                    Modifier.size(48.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        if (aktif) "Aktif Sefer" else "Yeni Sefer",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (aktif) "$guzergah • $km KM • $saat"
                        else "Günün seferini hızlıca kaydedin",
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = guzergah,
            onValueChange = onGuzergah,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Güzergah") },
            placeholder = { Text("Örn. Akseki - Antalya") },
            leadingIcon = { Icon(Icons.Default.Route, null) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        OutlinedTextField(
            value = cikisKm,
            onValueChange = { onKm(it.filter(Char::isDigit)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Çıkış KM") },
            placeholder = { Text("Örn. 125430") },
            leadingIcon = { Icon(Icons.Default.Speed, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun ActivePanel(guzergah: String, km: String, saat: String) {
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA))
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("SEFER DEVAM EDİYOR", fontWeight = FontWeight.Bold, color = Color(0xFF238636))
            Spacer(Modifier.height(6.dp))
            Text(guzergah, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Çıkış: $km KM  •  $saat", color = Color.Gray)
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
        modifier,
        RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Icon(icon, null)
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripCard(sefer: Sefer, onLongClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().combinedClickable(
            onClick = {},
            onLongClick = onLongClick
        ),
        RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(sefer.guzergah, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${sefer.cikisSaati} - ${sefer.donusSaati}", color = Color.Gray)
                    }
                }
                Text(
                    "${sefer.toplamKm} KM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(10.dp))
            Text("${sefer.cikisKm} KM - ${sefer.donusKm} KM", color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            Text(
                "${sefer.toplamKm} KM  •  ${sefer.toplamSure}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (sefer.notMetni.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Not: ${sefer.notMetni}", color = Color.Gray)
            }
            Spacer(Modifier.height(9.dp))
            Text("Silmek için uzun basın", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun EmptyCard() {
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.History, null)
            Spacer(Modifier.height(8.dp))
            Text("Henüz tamamlanmış sefer yok.", color = Color.Gray)
        }
    }
}

private fun saat(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun sureHesapla(baslangic: Long, bitis: Long): String {
    if (baslangic <= 0L) return "0 saat 0 dakika"
    val dakika = ((bitis - baslangic) / 60000L).coerceAtLeast(0L)
    return "${dakika / 60} saat ${dakika % 60} dakika"
}

private fun kaydetSeferler(context: Context, seferler: List<Sefer>) {
    val veri = seferler.joinToString("") {
        listOf(
            it.id, it.guzergah, it.cikisKm, it.donusKm,
            it.cikisSaati, it.donusSaati, it.toplamKm,
            it.toplamSure, it.notMetni
        ).joinToString("")
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

    return veri.split("").mapNotNull { satir ->
        val p = satir.split("")
        if (p.size != 9) return@mapNotNull null
        try {
            Sefer(
                p[0].toLong(), p[1], p[2].toInt(), p[3].toInt(),
                p[4], p[5], p[6].toInt(), p[7], p[8]
            )
        } catch (_: Exception) {
            null
        }
    }
}
