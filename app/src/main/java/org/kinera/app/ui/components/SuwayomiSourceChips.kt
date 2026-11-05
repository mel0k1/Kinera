package org.kinera.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kinera.app.R
import org.kinera.app.data.suwayomi.SuwayomiSourceOption

/**
 * Second chip row shown while a Suwayomi catalog is selected: which source
 * installed on the server the feed or the search should read from. "Auto"
 * stands for the automatic pick made in Settings (preferred source, then
 * first English, then first listed).
 */
@Composable
fun SuwayomiSourceChips(
    options: List<SuwayomiSourceOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.suwayomi_server_sources),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedId == null,
                    onClick = { onSelect(null) },
                    label = { Text(stringResource(R.string.suwayomi_source_auto)) },
                )
            }
            items(options.size) { index ->
                val option = options[index]
                FilterChip(
                    selected = option.id == selectedId,
                    onClick = { onSelect(option.id) },
                    label = {
                        val lang = option.lang.trim().uppercase()
                        Text(if (lang.isBlank()) option.name else "${option.name} · $lang")
                    },
                )
            }
        }
    }
}
