package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.AspectRatioOption
import com.example.model.TextScaleOption
import com.example.model.ThemeMode
import com.example.model.VisualStyle
import com.example.ui.components.AccordionSection

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PengaturanTab(
    settings: AppSettings,
    cacheInfoBytes: Pair<Long, Long>,
    isRealAiEnabled: Boolean,
    listState: LazyListState,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetTextScale: (TextScaleOption) -> Unit,
    onSetDefaultStyle: (VisualStyle) -> Unit,
    onSetDefaultRatio: (AspectRatioOption) -> Unit,
    onSetDefaultDuration: (Int) -> Unit,
    onClearCache: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandAppearance by rememberSaveable { mutableStateOf(true) }
    var expandDefaults by rememberSaveable { mutableStateOf(false) }
    var expandStoragePrivacy by rememberSaveable { mutableStateOf(true) }

    val totalCacheKb = ((cacheInfoBytes.first + cacheInfoBytes.second) / 1024L).coerceAtLeast(0L)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AccordionSection(
                title = "Tampilan & Ukuran Teks",
                subtitle = "${settings.themeMode.label} • Teks ${settings.textScale.label}",
                icon = Icons.Default.DarkMode,
                expanded = expandAppearance,
                onToggle = { expandAppearance = !expandAppearance }
            ) {
                Text("Mode Tema", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Text("Ukuran Teks", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextScaleOption.entries.forEach { scale ->
                        FilterChip(
                            selected = settings.textScale == scale,
                            onClick = { onSetTextScale(scale) },
                            label = { Text(scale.label) },
                            leadingIcon = {
                                Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            AccordionSection(
                title = "Nilai Default Proyek Baru",
                subtitle = "${settings.defaultStyle.label} • ${settings.defaultRatio.label} • ${settings.defaultSceneDurationSec} dtk",
                icon = Icons.Default.Palette,
                expanded = expandDefaults,
                onToggle = { expandDefaults = !expandDefaults }
            ) {
                Text("Gaya Visual Default", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VisualStyle.entries.forEach { style ->
                        FilterChip(
                            selected = settings.defaultStyle == style,
                            onClick = { onSetDefaultStyle(style) },
                            label = { Text(style.label) }
                        )
                    }
                }

                Text("Rasio Video Default", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AspectRatioOption.entries.forEach { ratio ->
                        FilterChip(
                            selected = settings.defaultRatio == ratio,
                            onClick = { onSetDefaultRatio(ratio) },
                            label = { Text(ratio.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Durasi Default per Scene", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onSetDefaultDuration((settings.defaultSceneDurationSec - 1).coerceAtLeast(2)) }) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                        }
                        Text("${settings.defaultSceneDurationSec} detik", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onSetDefaultDuration((settings.defaultSceneDurationSec + 1).coerceAtMost(120)) }) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                        }
                    }
                }
            }
        }

        item {
            AccordionSection(
                title = "Performa, Cache & Privasi",
                subtitle = "Cache sementara: $totalCacheKb KB • Batas 5 MB/gambar",
                icon = Icons.Default.Security,
                expanded = expandStoragePrivacy,
                onToggle = { expandStoragePrivacy = !expandStoragePrivacy }
            ) {
                Text(
                    text = "Semua gambar yang diunggah otomatis dikompresi (maksimal 5 MB) agar aplikasi tetap ringan di HP Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.fillMaxWidth().testTag("clear_cache_btn")
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bersihkan Cache ($totalCacheKb KB)")
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Privasi Penyimpanan Lokal", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Tanpa login dan tanpa akun. Semua naskah, karakter, dan gambar proyek tersimpan pribadi di perangkat HP Anda.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Button(
                    onClick = onOpenGuide,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Panduan 3 Langkah")
                }
            }
        }

        // Honest Platform / Version Status Disclosure (Section 10)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Status Platform & Generator AI", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "• Aplikasi Android Asli: StoryboardKu berjalan sebagai aplikasi Android terpasang (bukan halaman web browser), sehingga tidak memerlukan tombol 'Install Aplikasi (PWA)' tambahan.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (isRealAiEnabled) {
                            "• Generator Gambar AI Bawaan: Aktif melalui Firebase AI Logic."
                        } else {
                            "• Generator Gambar AI Bawaan: Belum aktif karena pengaturan Firebase AI Logic belum diaktifkan. Pakai cara alternatif ini: klik 'Salin Prompt' -> 'Buka Generator' (Gemini / AI generator pilihan Anda) -> lalu klik 'Impor' pada kartu scene."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}
