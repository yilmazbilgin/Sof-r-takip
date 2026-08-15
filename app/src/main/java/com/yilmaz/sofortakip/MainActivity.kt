package com.yilmaz.sofortakip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class Sefer(
    val guzergah: String,
    val cikisKm: Int,
    val donusKm: Int,
    val cikisSaati: String,
    val donusSaati: String,
    val toplamKm: Int,
    val toplamSure: String
)

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

    var aktifSefer by remember { mutableStateOf(false) }

    var guzergah by remember { mutableStateOf("") }
    var cikisKm by remember { mutableStateOf("") }
    var cikisSaati by remember { mutableStateOf("") }

    var seferler by remember { mutableStateOf(listOf<Sefer>()) }

    var baslatDialog by remember { mutableStateOf(false) }
    var bitirDialog by remember { mutableStateOf(false) }

    var donusKm by remember { mutableStateOf("") }

    val saat = {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1769AA),
            secondary = Color(0xFF2E7D32),
            background = Color(0xFFF4F6F8)
        )
    ) {

        Scaffold(
            containerColor = Color(0xFFF4F6F8),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Şoför Takip",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Sefer yönetim paneli",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                )
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                item {

                    if (!aktifSefer) {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
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
                                        tint = Color(0xFF1769AA),
                                        modifier = Modifier.size(32.dp)
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            "Hazır",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            "Yeni bir sefer başlatabilirsiniz",
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        baslatDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
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
                            }
                        }

                    } else {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {

                                Text(
                                    "● AKTİF SEFER",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    guzergah,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {

                                    Bilgi(
                                        "Çıkış KM",
                                        "$cikisKm KM"
                                    )

                                    Bilgi(
                                        "Çıkış Saati",
                                        cikisSaati
                                    )
                                }

                                Spacer(Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        bitirDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFC62828)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {

                                    Text(
                                        "🔴  SEFERİ BİTİR",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Istatistik(
                            Modifier.weight(1f),
                            Icons.Default.DirectionsCar,
                            seferler.size.toString(),
                            "Toplam Sefer"
                        )

                        Istatistik(
                            Modifier.weight(1f),
                            Icons.Default.Speed,
                            "${seferler.sumOf { it.toplamKm }} KM",
                            "Toplam KM"
                        )
                    }
                }

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(10.dp))

                                Text(
                                    "Sefer Geçmişi",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            if (seferler.isEmpty()) {

                                Text(
                                    "Henüz kayıtlı sefer bulunmuyor.",
                                    color = Color.Gray
                                )

                            } else {

                                seferler.forEach { sefer ->

                                    HorizontalDivider()

                                    Spacer(Modifier.height(12.dp))

                                    Text(
                                        sefer.guzergah,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )

                                    Text(
                                        "${sefer.cikisSaati} → ${sefer.donusSaati}",
                                        color = Color.Gray
                                    )

                                    Spacer(Modifier.height(5.dp))

                                    Text(
                                        "${sefer.toplamKm} KM • ${sefer.toplamSure}",
                                        color = Color(0xFF1769AA),
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // SEFER BAŞLAT
        if (baslatDialog) {

            AlertDialog(
                onDismissRequest = {
                    baslatDialog = false
                },
                title = {
                    Text(
                        "Yeni Sefer",
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
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = cikisKm,
                            onValueChange = {
                                cikisKm = it.filter { karakter ->
                                    karakter.isDigit()
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
                                aktifSefer = true
                                baslatDialog = false
                            }
                        }
                    ) {
                        Text("SEFERİ BAŞLAT")
                    }
                },
                dismissButton = {

                    TextButton(
                        onClick = {
                            baslatDialog = false
                        }
                    ) {
                        Text("Vazgeç")
                    }
                }
            )
        }

        // SEFER BİTİR
        if (bitirDialog) {

            AlertDialog(
                onDismissRequest = {
                    bitirDialog = false
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            "Çıkış: $cikisKm KM • $cikisSaati",
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = donusKm,
                            onValueChange = {
                                donusKm = it.filter { karakter ->
                                    karakter.isDigit()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Dönüş KM")
                            },
                            singleLine = true
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

                                val donusSaati = saat()
                                val toplamKm = donus - cikis

                                val yeniSefer = Sefer(
                                    guzergah = guzergah,
                                    cikisKm = cikis,
                                    donusKm = donus,
                                    cikisSaati = cikisSaati,
                                    donusSaati = donusSaati,
                                    toplamKm = toplamKm,
                                    toplamSure = "Tamamlandı"
                                )

                                seferler = listOf(
                                    yeniSefer
                                ) + seferler

                                aktifSefer = false
                                bitirDialog = false

                                donusKm = ""
                                guzergah = ""
                                cikisKm = ""
                                cikisSaati = ""
                            }
                        }
                    ) {
                        Text("SEFERİ TAMAMLA")
                    }
                },
                dismissButton = {

                    TextButton(
                        onClick = {
                            bitirDialog = false
                        }
                    ) {
                        Text("Vazgeç")
                    }
                }
            )
        }
    }
}

@Composable
fun Bilgi(
    baslik: String,
    deger: String
) {

    Column {

        Text(
            baslik,
            color = Color.Gray,
            fontSize = 12.sp
        )

        Text(
            deger,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun Istatistik(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    deger: String,
    baslik: String
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF1769AA)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                deger,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                baslik,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
