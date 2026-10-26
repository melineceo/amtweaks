@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.multik.amtweaks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.multik.amtweaks.Mode
import com.multik.amtweaks.MainViewModel
import com.multik.amtweaks.RootState
import com.multik.amtweaks.UiState
import com.multik.amtweaks.core.Catalog
import com.multik.amtweaks.core.Hidden
import com.multik.amtweaks.core.PrefType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbar) {
        state.snackbar?.let {
            snack.showSnackbar(it)
            vm.consumeSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("AM Tweaks", fontWeight = FontWeight.SemiBold)
            })
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { HeaderCard(state) }
                if (!state.appInstalled) item { WarnCard("Apple Music не найден на устройстве. Настройки станут доступны, когда приложение будет установлено.") }
                item { ModeSelector(state.mode) { vm.setMode(it) } }
                item { StatusCard(state, vm) }

                if (state.mode == Mode.ROOT) {
                    item { PrimaryActionCard(state, vm) }
                    item { SectionTitle("Качество и поток", Icons.Filled.Tune) }
                    items(Catalog.playbackToggles) { SettingRow(it, state, vm) }
                    item { SectionTitle("Аудио", Icons.Filled.MusicNote) }
                    items(Catalog.audioControls) { SettingRow(it, state, vm) }
                    item { SectionTitle("Отладочные флаги приложения", Icons.Filled.Warning) }
                    item {
                        Text(
                            "Приложение может сбрасывать эти флаги при запуске. Меняй, только если знаешь зачем.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(Catalog.applicationDebug) { SettingRow(it, state, vm) }
                } else {
                    item { ShizukuActionsCard(state, vm) }
                    item { SectionTitle("Что скрыто (переключается внутри меню Apple Music)", Icons.Filled.Lock) }
                    items(Catalog.playbackToggles) { CatalogReadOnlyRow(it) }
                }
                item { FooterCard(state) }
            }
        }
    }
}

@Composable
private fun HeaderCard(state: UiState) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MusicNote, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Скрытые настройки Apple Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Найдено ${state.hiddenCount} скрытых функций в коде 6.5.3",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WarnCard(text: String) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(text, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ModeSelector(mode: Mode, onPick: (Mode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = mode == Mode.ROOT,
            onClick = { onPick(Mode.ROOT) },
            shape = SegmentedButtonDefaults.itemShape(0, 2)
        ) { Text("Root") }
        SegmentedButton(
            selected = mode == Mode.SHIZUKU,
            onClick = { onPick(Mode.SHIZUKU) },
            shape = SegmentedButtonDefaults.itemShape(1, 2)
        ) { Text("Shizuku") }
    }
}

@Composable
private fun StatusCard(state: UiState, vm: MainViewModel) {
    val (ok, title, body) = when (state.mode) {
        Mode.ROOT -> when (state.root) {
            RootState.CHECKING -> Triple(false, "Проверяю root…", "Секунду.")
            RootState.AVAILABLE -> Triple(true, "Root доступен", "Можно щёлкать переключатели ниже. Apple Music закроется на время правки.")
            RootState.UNAVAILABLE -> Triple(false, "Root не выдан", "Выдай приложению root в менеджере суперпользователя или переключись на Shizuku.")
        }
        Mode.SHIZUKU -> when {
            !state.shizukuRunning -> Triple(false, "Shizuku не запущен", "Запусти службу Shizuku (adb/root) и вернись.")
            !state.shizukuPermission -> Triple(false, "Нет разрешения Shizuku", "Дай доступ кнопкой ниже.")
            else -> Triple(true, "Shizuku готов", "Переключатели недоступны: у Shizuku нет доступа к приватным файлам. Зато он откроет скрытое меню Apple Music.")
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    null,
                    tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.size(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.mode == Mode.SHIZUKU && state.shizukuRunning && !state.shizukuPermission) {
                Spacer(Modifier.size(10.dp))
                Button(onClick = { vm.requestShizukuPermission() }) { Text("Дать доступ Shizuku") }
            }
        }
    }
}

@Composable
private fun PrimaryActionCard(state: UiState, vm: MainViewModel) {
    val enabled = state.root == RootState.AVAILABLE && !state.busy
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bolt, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Выключить авто-снижение качества", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.size(6.dp))
            Text(
                "Одной кнопкой: плеер перестанет опускать качество под скорость сети.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.disableAdaptive() }, enabled = enabled) {
                    Icon(Icons.Filled.Bolt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Выключить")
                }
                OutlinedButton(onClick = { vm.resetPlaybackDefaults() }, enabled = enabled) {
                    Icon(Icons.Filled.Restore, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Сбросить")
                }
            }
        }
    }
}

@Composable
private fun ShizukuActionsCard(state: UiState, vm: MainViewModel) {
    val enabled = state.shizukuRunning && state.shizukuPermission
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Открыть скрытое меню отладки", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(6.dp))
            Text(
                "Экспериментально. Запускает настоящий экран отладки Apple Music через Shizuku. " +
                    "Если не откроется — используй root.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.openInternalMenu() }, enabled = enabled) { Text("Полное меню") }
                OutlinedButton(onClick = { vm.openUserMenu() }, enabled = enabled) { Text("Пользовательское") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingRow(h: Hidden, state: UiState, vm: MainViewModel) {
    val enabled = state.root == RootState.AVAILABLE && !state.busy
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(h.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(h.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!h.persists) {
                    Spacer(Modifier.size(6.dp))
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("сбрасывается приложением", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            when (val t = h.type) {
                is PrefType.Bool -> {
                    val checked = vm.current(h) == "true"
                    Switch(
                        checked = checked,
                        onCheckedChange = { vm.setValue(h, if (it) "true" else "false") },
                        enabled = enabled
                    )
                }
                is PrefType.Enum -> {
                    val cur = vm.current(h)
                    val idx = t.values.indexOf(cur).let { if (it < 0) 0 else it }
                    EnumPicker(t.labels, idx, enabled) { vm.setValue(h, t.values[it]) }
                }
                is PrefType.IntEnum -> {
                    val cur = vm.current(h).toIntOrNull() ?: 0
                    EnumPicker(t.labels, cur.coerceIn(0, t.labels.lastIndex), enabled) { vm.setValue(h, it.toString()) }
                }
            }
        }
    }
}

@Composable
private fun EnumPicker(labels: List<String>, index: Int, enabled: Boolean, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { if (enabled) open = true },
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.width(160.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    labels.getOrElse(index) { labels.first() },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Icon(Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            labels.forEachIndexed { i, label ->
                DropdownMenuItem(text = { Text(label) }, onClick = { open = false; onPick(i) })
            }
        }
    }
}

@Composable
private fun CatalogReadOnlyRow(h: Hidden) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(h.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(h.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(4.dp))
            Text("ключ: ${h.key}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FooterCard(state: UiState) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Правятся только собственные настройки Apple Music на твоём устройстве. " +
                    "Приложение не пересобирается и не переподписывается — загрузки и обновления из Play остаются.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(6.dp))
            Text(
                "После обновления Apple Music может сбросить настройки — просто открой её раз и примени снова.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
