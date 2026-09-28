package com.srajan.batchnotify.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.srajan.batchnotify.HeldStore
import com.srajan.batchnotify.Prefs
import com.srajan.batchnotify.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

private val INTERVALS = listOf(3, 4, 6, 12)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenApps: () -> Unit, onOpenWaiting: () -> Unit) {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var nextAt by remember { mutableLongStateOf(Prefs.nextBatchAt(ctx)) }
    var hasAccess by remember { mutableStateOf(true) }
    var held by remember { mutableIntStateOf(0) }
    var interval by remember { mutableIntStateOf(Prefs.intervalHours(ctx)) }
    var enabled by remember { mutableStateOf(Prefs.isEnabled(ctx)) }
    val importantCount = remember { Prefs.allowlist(ctx).size }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Ask for post-notification permission once, then refresh the screen every second.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        while (true) {
            now = System.currentTimeMillis()
            nextAt = Prefs.nextBatchAt(ctx)
            hasAccess = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
            held = withContext(Dispatchers.IO) { HeldStore.count(ctx) }
            delay(1000)
        }
    }

    val totalMs = interval * 3_600_000L
    val remaining = (nextAt - now).coerceIn(0L, totalMs)
    val progress = if (enabled) 1f - remaining.toFloat() / totalMs else 0f

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text("Batch", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        if (!hasAccess) {
            Spacer(Modifier.height(16.dp))
            AccessCard { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }

        // The hero: a ring that fills up as the next batch gets closer.
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BatchRing(progress = progress, hours = interval, active = enabled, modifier = Modifier.size(260.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$held", style = MaterialTheme.typography.displayLarge)
                Text(
                    if (held == 1) "notification waiting" else "notifications waiting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                !enabled -> "Paused. Notifications arrive as usual."
                remaining <= 60_000L -> "Next batch any minute now"
                else -> "Next batch in ${formatDuration(remaining)}"
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { Scheduler.deliverNow(ctx); held = 0 },
                enabled = held > 0,
                modifier = Modifier.weight(1f),
            ) { Text("Release now") }
            OutlinedButton(onClick = onOpenWaiting, modifier = Modifier.weight(1f)) { Text("See waiting") }
        }

        Spacer(Modifier.height(36.dp))
        SectionLabel("Deliver every")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            INTERVALS.forEachIndexed { i, h ->
                SegmentedButton(
                    selected = interval == h,
                    onClick = {
                        interval = h
                        Prefs.setIntervalHours(ctx, h)
                        Scheduler.schedule(ctx, true)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = INTERVALS.size),
                ) { Text("$h h") }
            }
        }

        Spacer(Modifier.height(24.dp))
        SettingRow("Batching", if (enabled) "On" else "Off, notifications arrive right away") {
            Switch(checked = enabled, onCheckedChange = { on ->
                enabled = on
                Prefs.setEnabled(ctx, on)
                if (on) Scheduler.schedule(ctx, true) else Scheduler.deliverNow(ctx)
            })
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        SettingRow(
            title = "Important apps",
            subtitle = if (importantCount == 1) "1 app skips batching" else "$importantCount apps skip batching",
            onClick = onOpenApps,
        ) {
            Text("Edit", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun BatchRing(progress: Float, hours: Int, active: Boolean, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val tick = MaterialTheme.colorScheme.outline
    val animated by animateFloatAsState(progress, label = "ring")

    Canvas(modifier) {
        val stroke = 14.dp.toPx()
        val inset = stroke / 2 + 10.dp.toPx()
        val topLeft = Offset(inset, inset)
        val arc = Size(size.width - inset * 2, size.height - inset * 2)

        drawArc(track, -90f, 360f, false, topLeft, arc, style = Stroke(stroke))
        if (animated > 0.001f) {
            drawArc(fill, -90f, 360f * animated, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }

        // One small tick per hour of the interval, just outside the ring.
        val r = size.minDimension / 2
        val len = 6.dp.toPx()
        for (i in 0 until hours) {
            val a = Math.toRadians(-90.0 + 360.0 * i / hours)
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(
                color = tick,
                start = Offset(center.x + (r - len) * dx, center.y + (r - len) * dy),
                end = Offset(center.x + r * dx, center.y + r * dy),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun AccessCard(onGrant: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Turn on notification access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Batch needs this to hold notifications and release them on your schedule.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGrant) { Text("Open settings") }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

private fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
