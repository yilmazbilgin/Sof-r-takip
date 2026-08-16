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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F7FA)
                ) {
                    SoforTakip()
                }
            }
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun SoforTakip() {
    val context = LocalContext.current

    var seferler by remember {
        mutableStateOf(yukleSeferler(context))
    }

    var aktif by remember {
        mutableStateOf(false)
    }

    var bitirAcik by remember {
        mutableStateOf(false)
    }

    var silinecekSefer by remember {
        mutableStateOf<Sefer?>(null)
    }

    var guzergah by remember {
        mutableStateOf("")
    }

    var cikisKm by remember {
        mutableStateOf("")
    }

    var donusKm by remember {
        mutableStateOf("")
    }

    var notMetni by remember {
        mutableStateOf("")
    }

    var cikisSaati by remember {
        mutableStateOf("")
    }

    var baslangicZamani by remember {
        mutableStateOf(0L)
    }

    var hataMesaji by remember {
    mutableStateOf("")
}

var notlarAcik by remember {
    mutableStateOf(false)
}

val toplamKm = seferler.sumOf { it.toplamKm }

    Scaffold(
        topBar = {
            TopAppBar(
    title = {
        Column {
            Text(
                text = "Şoför Takip",
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Sefer yönetimi",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    },
    actions = {
        TextButton(
            onClick = {
                notlarAcik = true
            }
        ) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (aktif)
                            Color(0xFFE8F5E9)
                        else
                            Color(0xFFEDE9EF)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(48.dp)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {

                                Text(
                                    text = if (aktif)
                                        "Aktif Sefer"
                                    else
                                        "Yeni Sefer",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (aktif)
                                        "$guzergah • $cikisKm KM"
                                    else
                                        "Yeni bir sefer başlatın",
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
                                        hataMesaji = "Güzergâh girin."
                                        return@Button
                                    }

                                    if (cikisKm.toIntOrNull() == null) {
                                        hataMesaji = "Geçerli bir çıkış KM girin."
                                        return@Button
                                    }

                                    aktif = true
                                    cikisSaati = saat()
                                    baslangicZamani =
                                        System.currentTimeMillis()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {

                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    "SEFERİ BAŞLAT",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                        } else {

                            Text(
                                text = "Sefer devam ediyor",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Çıkış: $cikisKm KM  •  $cikisSaati",
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    bitirAcik = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {

                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    "SEFERİ BİTİR",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (!aktif) {

                item {

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
                            Text("Örn. Akseki - Antalya")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Route,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                item {

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
                        placeholder = {
                            Text("Örn. 125430")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = null
                            )
                        },
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

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.History,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Sefer Geçmişi",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (seferler.isEmpty()) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                Icons.Default.History,
                                contentDescription = null
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                "Henüz tamamlanmış sefer yok.",
                                color = Color.Gray
                            )
                        }
                    }
                }

            } else {

                items(
                    items = seferler,
                    key = { it.id }
                ) { sefer ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    silinecekSefer = sefer
                                }
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEDE9EF)
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        sefer.guzergah,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(
                                        modifier = Modifier.height(6.dp)
                                    )

                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Icon(
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .width(18.dp)
                                                .height(18.dp)
                                        )

                                        Spacer(
                                            modifier = Modifier.width(5.dp)
                                        )

                                        Text(
                                            "${sefer.cikisSaati} → ${sefer.donusSaati}",
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Text(
                                    "${sefer.toplamKm} KM",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1769AA)
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Divider()

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Text(
                                "${sefer.cikisKm} KM → ${sefer.donusKm} KM",
                                color = Color.Gray
                            )

                            Spacer(
                                modifier = Modifier.height(5.dp)
                            )

                            Text(
                                "${sefer.toplamKm} KM • ${sefer.toplamSure}",
                                color = Color(0xFF1769AA),
                                fontWeight = FontWeight.Bold
                            )

                            if (sefer.notMetni.isNotBlank()) {

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                Text(
                                    "Not: ${sefer.notMetni}",
                                    color = Color.Gray
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                "Silmek için uzun basın",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (hataMesaji.isNotBlank()) {

        AlertDialog(
            onDismissRequest = {
                hataMesaji = ""
            },
            title = {
                Text("Bilgi")
            },
            text = {
                Text(hataMesaji)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hataMesaji = ""
                    }
                ) {
                    Text("Tamam")
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
                    "Seferi Tamamla",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {

                Column {

                    Text(
                        "Güzergâh: $guzergah",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

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

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

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
                            Text("Örn. Evrak teslim edildi")
                        },
                        minLines = 3
                    )
                }
            },
            confirmButton = {

                Button(
                    onClick = {

                        val cikis =
                            cikisKm.toIntOrNull()

                        val donus =
                            donusKm.toIntOrNull()

                        if (cikis == null ||
                            donus == null
                        ) {

                            hataMesaji =
                                "Çıkış ve dönüş KM bilgilerini girin."
                            return@Button
                        }

                        if (donus < cikis) {

                            hataMesaji =
                                "Dönüş KM, çıkış KM'den küçük olamaz."
                            return@Button
                        }

                        val bitis =
                            System.currentTimeMillis()

                        val yeniSefer = Sefer(
                            id = bitis,
                            guzergah = guzergah,
                            cikisKm = cikis,
                            donusKm = donus,
                            cikisSaati = cikisSaati,
                            donusSaati = saat(),
                            toplamKm = donus - cikis,
                            toplamSure = sureHesapla(
                                baslangicZamani,
                                bitis
                            ),
                            notMetni = notMetni
                        )

                        seferler =
                            listOf(yeniSefer) + seferler

                        kaydetSeferler(
                            context,
                            seferler
                        )

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

    if (silinecekSefer != null) {

        AlertDialog(
            onDismissRequest = {
                silinecekSefer = null
            },
            title = {
                Text("Sefer silinsin mi?")
            },
            text = {
                Text(
                    "Bu sefer geçmişten kalıcı olarak silinecek."
                )
            },
            confirmButton = {

                Button(
                    onClick = {

                        val silinecek =
                            silinecekSefer

                        if (silinecek != null) {

                            seferler =
                                seferler.filter {
                                    it.id != silinecek.id
                                }

                            kaydetSeferler(
                                context,
                                seferler
                            )
                        }

                        silinecekSefer = null
                    }
                ) {

                    Text("SİL")
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        silinecekSefer = null
                    }
                ) {

                    Text("VAZGEÇ")
                }
            }
        )
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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEDE9EF)
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Icon(
                imageVector = ikon,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                baslik,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                altBaslik,
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

    val dakika =
        ((bitis - baslangic) / 60000L)
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

    val veri =
        context
            .getSharedPreferences(
                "sofor_takip",
                Context.MODE_PRIVATE
            )
            .getString(
                "seferler",
                ""
            )
            ?: ""

    if (veri.isBlank()) {
        return emptyList()
    }

    return veri
        .split("\u001E")
        .mapNotNull { satir ->

            val p =
                satir.split("\u001F")

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

            } catch (
                e: Exception
            ) {

                null
            }
        }
}
