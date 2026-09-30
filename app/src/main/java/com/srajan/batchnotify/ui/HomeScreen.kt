package com.srajan.batchnotify.ui

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.srajan.batchnotify.HeldStore
import com.srajan.batchnotify.Prefs
import com.srajan.batchnotify.R
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
    var dailyTimeSet by remember { mutableStateOf(Prefs.hasDailyTime(ctx)) }
    var dailyHour by remember { mutableIntStateOf(Prefs.dailyHour(ctx)) }
    var dailyMinute by remember { mutableIntStateOf(Prefs.dailyMinute(ctx)) }
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
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

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
                    pluralStringResource(R.plurals.notifications_waiting, held),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = when {
                !enabled -> stringResource(R.string.status_paused)
                remaining <= 60_000L -> stringResource(R.string.status_any_minute)
                else -> stringResource(R.string.status_next_batch_in, formatDuration(ctx, remaining))
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
            ) { Text(stringResource(R.string.release_now)) }
            OutlinedButton(onClick = onOpenWaiting, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.see_waiting)) }
        }

        Spacer(Modifier.height(36.dp))
        SectionLabel(stringResource(R.string.deliver_every))
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
                ) { Text(stringResource(R.string.interval_hours, h)) }
            }
        }

//        Spacer(Modifier.height(24.dp))
//        SettingRow(
//            stringResource(R.string.batching),
//            stringResource(if (enabled) R.string.batching_on else R.string.batching_off),
//        ) {
//            Switch(checked = enabled, onCheckedChange = { on ->
//                enabled = on
//                Prefs.setEnabled(ctx, on)
//                if (on) Scheduler.schedule(ctx, true) else Scheduler.deliverNow(ctx)
//            })
//        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Additional daily delivery")
        SettingRow(
            title = if (dailyTimeSet) "Every day at ${formatTime(ctx, dailyHour, dailyMinute)}" else "No additional time set",
            subtitle = "This is added to your ${interval}h schedule; it does not replace it.",
            onClick = if (enabled) {
                {
                    val initial = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, dailyHour)
                        set(Calendar.MINUTE, dailyMinute)
                    }
                    TimePickerDialog(
                        ctx,
                        { _, hour, minute ->
                            dailyHour = hour
                            dailyMinute = minute
                            dailyTimeSet = true
                            Prefs.setDailyTime(ctx, hour, minute)
                            Scheduler.scheduleDailyTime(ctx)
                        },
                        initial.get(Calendar.HOUR_OF_DAY),
                        initial.get(Calendar.MINUTE),
                        android.text.format.DateFormat.is24HourFormat(ctx)
                    ).show()
                }
            } else null,
        ) {
            if (dailyTimeSet) {
                OutlinedButton(onClick = {
                    dailyTimeSet = false
                    Prefs.clearDailyTime(ctx)
                    Scheduler.cancelDailyTime(ctx)
                }) { Text("Remove") }
            } else {
                Button(onClick = {
                    val now = Calendar.getInstance()
                    TimePickerDialog(
                        ctx,
                        { _, hour, minute ->
                            dailyHour = hour
                            dailyMinute = minute
                            dailyTimeSet = true
                            Prefs.setDailyTime(ctx, hour, minute)
                            if (enabled) Scheduler.scheduleDailyTime(ctx)
                        },
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        android.text.format.DateFormat.is24HourFormat(ctx)
                    ).show()
                }, enabled = enabled) { Text("Add time") }
            }
        }



        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        SettingRow(
            stringResource(R.string.batching),
            stringResource(if (enabled) R.string.batching_on else R.string.batching_off),
        ) {
            Switch(checked = enabled, onCheckedChange = { on ->
                enabled = on
                Prefs.setEnabled(ctx, on)
                if (on) {
                    Scheduler.schedule(ctx, true)
                    if (dailyTimeSet) Scheduler.scheduleDailyTime(ctx)
                } else {
                    Scheduler.cancelDailyTime(ctx)
                    Scheduler.deliverNow(ctx)
                }
            })
        }


        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        SettingRow(
            title = stringResource(R.string.important_apps),
            subtitle = pluralStringResource(R.plurals.apps_skip_batching, importantCount, importantCount),
            onClick = onOpenApps,
        ) {
            Text(stringResource(R.string.edit), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
            Text(stringResource(R.string.access_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.access_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onGrant) { Text(stringResource(R.string.open_settings)) }
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

private fun formatTime(ctx: Context, hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return android.text.format.DateFormat.getTimeFormat(ctx).format(cal.time)
}

private fun formatDuration(ctx: Context, ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) ctx.getString(R.string.duration_hours_minutes, h, m) else ctx.getString(R.string.duration_minutes, m)
}
