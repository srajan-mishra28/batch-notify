package com.srajan.batchnotify.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("Back") }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

fun appLabel(pm: PackageManager, pkg: String): String = try {
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
} catch (e: PackageManager.NameNotFoundException) {
    pkg
}
