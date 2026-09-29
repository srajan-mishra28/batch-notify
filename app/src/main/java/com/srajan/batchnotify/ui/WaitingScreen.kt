package com.srajan.batchnotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srajan.batchnotify.HeldStore
import com.srajan.batchnotify.R
import com.srajan.batchnotify.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private data class HeldItem(val pkg: String, val title: String, val text: String, val time: Long)

/** Peek at what's being held, without releasing it. */
@Composable
fun WaitingScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var groups by remember { mutableStateOf<Map<String, List<HeldItem>>?>(null) }
    val timeFmt = remember { android.text.format.DateFormat.getTimeFormat(ctx) }

    LaunchedEffect(Unit) {
        groups = withContext(Dispatchers.IO) {
            val arr = HeldStore.peek(ctx)
            (0 until arr.length())
                .mapNotNull { arr.optJSONObject(it) }
                .map { HeldItem(it.optString("pkg"), it.optString("title"), it.optString("text"), it.optLong("time")) }
                .sortedByDescending { it.time }
                .groupBy { it.pkg }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        TopBar(stringResource(R.string.waiting_title), onBack)
        val g = groups ?: return@Column

        if (g.isEmpty()) {
            Spacer(Modifier.height(48.dp))
            Text(stringResource(R.string.nothing_waiting), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.nothing_waiting_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            g.forEach { (pkg, items) ->
                item(key = pkg) { HeldGroup(appLabel(ctx.packageManager, pkg), items, timeFmt) }
            }
        }
        Button(
            onClick = { Scheduler.deliverNow(ctx); groups = emptyMap() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        ) { Text(stringResource(R.string.release_all_now)) }
    }
}

@Composable
private fun HeldGroup(label: String, items: List<HeldItem>, timeFmt: DateFormat) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${items.size}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            items.take(20).forEach { item ->
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (item.text.isNotBlank()) {
                            Text(item.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(timeFmt.format(Date(item.time)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
