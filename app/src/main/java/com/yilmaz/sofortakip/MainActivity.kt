package com.yilmaz.sofortakip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

data class Sefer(
    val guzergah: String,
    val cikisKm: Int,
    val donusKm: Int,
    val cikisSaati: String,
    val donusSaati: String,
    val toplamKm: Int,
    val toplamSure: String
    val notMetni: String
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoforTakip()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoforTakip() {

    var aktif by remember { mutableStateOf(false) }
    val context = LocalContext.current
val prefs = remember {
    context.getSharedPreferences("sofor_takip", 0)
}
    var guzergah by remember { mutableStateOf("") }
    var cikisKm by remember { mutableStateOf("") }
    var donusKm by remember { mutableStateOf("") }
    var cikisSaati by remember { mutableStateOf("") }
    var baslangic by remember { mutableStateOf(0L) }
    var notMetni by remember { mutableStateOf("") }

    var seferler by remember { mutableStateOf(listOf<Sefer>()) }
    LaunchedEffect(Unit) {
    val kayitlar = prefs.getStringSet("seferler", emptySet())
        ?.mapNotNull { kayit ->
            val parcalar = kayit.split("|")

            if (parcalar.size == 7 || parcalar.size == 8) {
                Sefer(
                    guzergah = parcalar[0],
                    cikisKm = parcalar[1].toIntOrNull() ?: return@mapNotNull null,
                    donusKm = parcalar[2].toIntOrNull() ?: return@mapNotNull null,
                    cikisSaati = parcalar[3],
                    donusSaati = parcalar[4],
                    toplamKm = parcalar[5].toIntOrNull() ?: return@mapNotNull null,
                    toplamSure = parcalar[6]
                    not = if (parcalar.size == 8) parcalar[7] else ""
                )
            } else {
                null
            }
        }
        ?: emptyList()

    seferler = kayitlar
    }
    var baslatAcik by remember { mutableStateOf(false) }
    var bitirAcik by remember { mutableStateOf(false) }

    fun saat(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    fun sureHesapla(basla: Long, bitir: Long): String {
        val dakika = ((bitir - basla) / 60000).coerceAtLeast(0)
        return "${dakika / 60} saat ${dakika % 60} dakika"
    }
    fun seferleriKaydet(liste: List<Sefer>) {
    val kayitlar = liste.map {
        "${it.guzergah}|${it.cikisKm}|${it.donusKm}|${it.cikisSaati}|${it.donusSaati}|${it.toplamKm}|${it.toplamSure}"
    }.toSet()

    prefs.edit()
        .putStringSet("seferler", kayitlar)
        .apply()
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1769AA)
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Şoför Takip",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Sefer yönetimi",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    if (aktif) "Aktif Sefer"
                                    else "Yeni Sefer",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    if (aktif) guzergah
                                    else "Sefer başlatmak için aşağıdaki butona basın",
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        if (!aktif) {

                            Button(
                                onClick = {
                                    baslatAcik = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(55.dp),
                                shape = RoundedCornerShape(15.dp)
                            ) {
                                Icon(
                                    Icons.Default.Route,
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    "SEFERİ BAŞLAT",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                        } else {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Çıkış KM",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "$cikisKm KM",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column {
                                    Text(
                                        "Çıkış Saati",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        cikisSaati,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    bitirAcik = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(55.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFC62828)
                                ),
                                shape = RoundedCornerShape(15.dp)
                            ) {
                                Text(
                                    "🔴 SEFERİ BİTİR",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                seferler.size.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Toplam Sefer",
                                color = Color.Gray
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${seferler.sumOf { it.toplamKm }} KM",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Toplam KM",
                                color = Color.Gray
                            )
                        }
                    }
                }

                Text(
                    "Sefer Geçmişi",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                if (seferler.isEmpty()) {
                    Text(
                        "Henüz tamamlanmış sefer yok.",
                        color = Color.Gray
                    )
                } else {
                    seferler.forEach { sefer ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    sefer.guzergah,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    "${sefer.cikisSaati} → ${sefer.donusSaati}",
                                    color = Color.Gray
                                )
                                Text(
    "${sefer.cikisKm} KM → ${sefer.donusKm} KM",
    color = Color.Gray,
    fontWeight = FontWeight.Medium
)
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    "${sefer.toplamKm} KM • ${sefer.toplamSure}",
                                    color = Color(0xFF1769AA),
                                    fontWeight = FontWeight.Bold
                                )
                                if (sefer.notMetni.isNotBlank()) {
    Spacer(Modifier.height(8.dp))
    Text(
        "Not: ${sefer.notMetni}",
        color = Color.Gray,
        fontWeight = FontWeight.Medium
    )
                                }
                            }
                        }
                    }
                }
            }
        }    }

    if (baslatAcik) {
        AlertDialog(
            onDismissRequest = {
                baslatAcik = false
            },
            title = {
                Text(
                    "Seferi Başlat",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = guzergah,
                        onValueChange = {
                            guzergah = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Güzergâh")
                        },
                        placeholder = {
                            Text("Örn. Akseki → Antalya")
                        },
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cikisKm,
                        onValueChange = {
                            cikisKm = it.filter { c ->
                                c.isDigit()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Çıkış KM")
                        },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (
                            guzergah.isNotBlank() &&
                            cikisKm.isNotBlank()
                        ) {
                            cikisSaati = saat()
                            baslangic = System.currentTimeMillis()
                            aktif = true
                            baslatAcik = false
                        }
                    }
                ) {
                    Text("BAŞLAT")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        baslatAcik = false
                    }
                ) {
                    Text("Vazgeç")
                }
            }
        )
    }

    if (bitirAcik) {
        AlertDialog(
            onDismissRequest = {
                bitirAcik = false
            },
            title = {
                Text(
                    "Seferi Bitir",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        guzergah,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Çıkış: $cikisKm KM • $cikisSaati",
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = donusKm,
                        onValueChange = {
                            donusKm = it.filter { c ->
                                c.isDigit()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Dönüş KM")
                        },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))

OutlinedTextField(
    value = notMetni,
onValueChange = {
    notMetni = it
},
    modifier = Modifier.fillMaxWidth(),
    label = {
        Text("Not")
    },
    placeholder = {
        Text("Örn. Evrak teslim edilecek")
    },
    minLines = 3
)
                }
            },
            confirmButton = {
                Button(
                    onClick = {

                        val cikis = cikisKm.toIntOrNull()
                        val donus = donusKm.toIntOrNull()

                        if (
                            cikis != null &&
                            donus != null &&
                            donus >= cikis
                        ) {

                            val bitis =
                                System.currentTimeMillis()

                            val yeniSefer = Sefer(
                                guzergah = guzergah,
                                cikisKm = cikis,
                                donusKm = donus,
                                cikisSaati = cikisSaati,
                                donusSaati = saat(),
                                toplamKm = donus - cikis,
                                toplamSure = sureHesapla(
                                    baslangic,
                                    bitis
                                )
                                notMetni = notMetni
                            )

                            seferler =
                                listOf(yeniSefer) + seferler
                            seferleriKaydet(seferler)

                            aktif = false
                            bitirAcik = false

                            guzergah = ""
                            cikisKm = ""
                            donusKm = ""
                            cikisSaati = ""
                            baslangic = 0L
                        }
                    }
                ) {
                    Text("SEFERİ TAMAMLA")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        bitirAcik = false
                    }
                ) {
                    Text("Vazgeç")
                }
            }
        )
    }
}
