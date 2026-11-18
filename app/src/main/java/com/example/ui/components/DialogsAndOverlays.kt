package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import java.io.File

@Composable
fun OnboardingGuideDialog(
    onSkip: () -> Unit,
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Selamat Datang di StoryboardKu") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_onboarding_banner),
                    contentDescription = "Ilustrasi panduan StoryboardKu",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "Contoh proyek 10 scene \"Sireum Boy — Mencari Makanan\" sudah siap dicoba dan bisa langsung Anda edit. Cukup 3 langkah:",
                    style = MaterialTheme.typography.bodySmall
                )
                GuideStepRow("1", "Isi Karakter (Character Lock)", "Masukkan deskripsi & foto referensi karakter di tab Karakter agar bentuk tokoh tidak berubah antar scene.")
                GuideStepRow("2", "Tulis & Susun Scene", "Atur visual, dialog, narasi, kamera, durasi, dan urutan scene di tab Storyboard.")
                GuideStepRow("3", "Buat Gambar & Export", "Buat pratinjau/impor gambar, lalu export PDF, ZIP gambar, atau daftar shot untuk CapCut.")
            }
        },
        confirmButton = {
            Button(onClick = onStart) { Text("Mulai Sekarang") }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Lewati") }
        }
    )
}

@Composable
private fun GuideStepRow(number: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FullscreenImageDialog(
    imagePath: String,
    onSaveToGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Tampilan penuh gambar scene",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSaveToGallery) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Simpan ke Galeri")
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color(0x88000000), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ExternalGeneratorDialog(
    promptText: String,
    onCopyPrompt: (String) -> Unit,
    onLaunchUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buka Generator Gambar AI") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Karena generator bawaan belum diaktifkan dengan Firebase AI Logic, Anda bisa membuat gambar gratis lewat generator AI eksternal lalu mengimpor hasilnya kembali ke scene:",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("1. Salin prompt Bahasa Inggris di bawah ini.", style = MaterialTheme.typography.labelLarge)
                if (promptText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = promptText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            maxLines = 6
                        )
                    }
                    OutlinedButton(
                        onClick = { onCopyPrompt(promptText) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Salin Prompt Ini")
                    }
                }
                Text("2. Buka salah satu generator gambar AI:", style = MaterialTheme.typography.labelLarge)
                Button(
                    onClick = { onLaunchUrl("https://gemini.google.com/app") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Buka Gemini (Web)")
                }
                OutlinedButton(
                    onClick = { onLaunchUrl("https://www.bing.com/images/create") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Buka Bing Image Creator")
                }
                Text(
                    text = "3. Simpan gambar hasilnya ke HP, lalu tekan tombol 'Impor' di kartu scene untuk memasangnya.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun VideoPromptsAndCapCutDialog(
    allVideoPromptsText: String,
    onCopyAll: () -> Unit,
    onExportCsv: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prompt Video AI & Daftar Shot CapCut") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Setiap scene otomatis dibuatkan prompt klip video (lengkap dengan gerakan kamera, transisi, dan durasi).",
                    style = MaterialTheme.typography.bodySmall
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = allVideoPromptsText.ifBlank { "Belum ada scene." },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onCopyAll, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Salin Semua Prompt Video")
                }
                OutlinedButton(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export Daftar Shot (CSV CapCut)")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Tutup")
                }
            }
        }
    )
}

@Composable
fun PdfExportChoiceDialog(
    onSavePdfFile: () -> Unit,
    onSharePdf: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Storyboard PDF") },
        text = {
            Text(
                "Pilih cara export PDF lengkap (berisi nomor scene, gambar, durasi, deskripsi, dialog, dan narasi):",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onDismiss()
                        onSavePdfFile()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Simpan File PDF ke HP")
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSharePdf()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Bagikan PDF (WhatsApp / Drive)")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Batal")
                }
            }
        }
    )
}
