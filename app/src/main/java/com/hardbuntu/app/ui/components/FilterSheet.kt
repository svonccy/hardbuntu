package com.hardbuntu.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hardbuntu.app.ui.viewmodel.ProductSort
import com.hardbuntu.app.ui.viewmodel.StoreUiState
import java.util.Locale
import kotlin.math.roundToInt

/** Panel inferior para ordenar y filtrar el catálogo. Los cambios se aplican al instante. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    state: StoreUiState,
    onSortChange: (ProductSort) -> Unit,
    onToggleBrand: (String) -> Unit,
    onPriceRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onOnlyInStockChange: (Boolean) -> Unit,
    onOnlyFavoritesChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bounds = state.priceBounds
    var sliderValue by remember(state.priceRange, bounds) { mutableStateOf(state.priceRange ?: bounds) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Filtros y orden",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onClear,
                    enabled = state.activeFilterCount > 0 || state.sort != ProductSort.RELEVANCE
                ) {
                    Text("Limpiar", fontWeight = FontWeight.SemiBold)
                }
            }

            SectionTitle("Ordenar por")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProductSort.entries.forEach { option ->
                    FilterChip(
                        selected = state.sort == option,
                        onClick = { onSortChange(option) },
                        label = { Text(option.label) }
                    )
                }
            }

            if (state.availableBrands.isNotEmpty()) {
                SectionTitle("Marca")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.availableBrands.forEach { brand ->
                        FilterChip(
                            selected = brand in state.selectedBrands,
                            onClick = { onToggleBrand(brand) },
                            label = { Text(brand) }
                        )
                    }
                }
            }

            if (bounds.start < bounds.endInclusive) {
                SectionTitle("Precio")
                Text(
                    text = String.format(
                        Locale.US,
                        "S/. %d  -  S/. %d",
                        sliderValue.start.roundToInt(),
                        sliderValue.endInclusive.roundToInt()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                RangeSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onPriceRangeChange(sliderValue) },
                    valueRange = bounds
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SwitchRow(
                title = "Solo con stock",
                checked = state.onlyInStock,
                onCheckedChange = onOnlyInStockChange
            )
            SwitchRow(
                title = "Solo mis favoritos",
                checked = state.onlyFavorites,
                onCheckedChange = onOnlyFavoritesChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                val count = state.filteredProducts.size
                Text(
                    text = if (count == 1) "Ver 1 producto" else "Ver $count productos",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
