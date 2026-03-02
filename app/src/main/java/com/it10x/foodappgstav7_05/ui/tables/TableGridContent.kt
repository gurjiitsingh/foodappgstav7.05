package com.it10x.foodappgstav7_05.com.it10x.foodappgstav7_05.ui.tables


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.it10x.foodappgstav7_05.viewmodel.PosTableViewModel
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.it10x.foodappgstav7_05.ui.pos.StatusBadge


@Composable
fun TableGridContent(
    tables: List<PosTableViewModel.TableUiState>,
    selectedTable: String?,
    navController: NavController,
    onTableSelected: (String) -> Unit
) {

    val groupedByArea = tables
        .groupBy { it.table.area ?: "General" }
        .toSortedMap()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 85.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        groupedByArea.forEach { (areaName, areaTables) ->

            // 🔹 AREA HEADER (Full Width)
            item(
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Text(
                    text = areaName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 🔹 TABLES
            items(
                items = areaTables.sortedBy { it.table.sortOrder ?: Int.MAX_VALUE },
                key = { it.table.id }
            ) { ui ->

                val table = ui.table
                val isSelected = selectedTable == table.id

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    tonalElevation = 2.dp,
                    border = if (isSelected)
                        BorderStroke(2.dp, Color(0xFFFF9800))
                    else null,
                    modifier = Modifier
                        .aspectRatio(.95f)
                        .clickable {
                            onTableSelected(table.id)
                        }
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {

                        // HEADER BUTTON
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            modifier = Modifier.clickable {
                                onTableSelected(table.id)
                                navController.navigate("pos") {
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = table.tableName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (ui.cartCount > 0) {
                                StatusBadge(
                                    icon = "🛒",
                                    text = ui.cartCount.toString(),
                                    bgColor = Color(0xFF1976D2).copy(alpha = 0.6f)
                                )
                            }

                            if (ui.billDoneCount > 0) {
                                StatusBadge(
                                    icon = "🧾",
                                    text = ui.billDoneCount.toString(),
                                    bgColor = Color(0xFF2E7D32).copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



//
//Surface(
//shape = CircleShape,
//tonalElevation = 4.dp,
//border = if (isSelected)
//BorderStroke(3.dp, Color(0xFFFF9800))
//else
//BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
//modifier = Modifier
//.aspectRatio(1f)
//.padding(6.dp)
//.clickable { onTableSelected(table.id) }
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.surface),
//        contentAlignment = Alignment.Center
//    ) {
//
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//
//            // 🪑 Table Name (Big & Centered)
//            Text(
//                text = table.tableName,
//                style = MaterialTheme.typography.titleLarge,
//                fontWeight = FontWeight.Bold
//            )
//
//            Spacer(modifier = Modifier.height(6.dp))
//
//            // Status Info
//            if (ui.cartCount > 0) {
//                Text(
//                    text = "🛒 ${ui.cartCount}",
//                    style = MaterialTheme.typography.bodyMedium
//                )
//            }
//
//            if (ui.billDoneCount > 0) {
//                Text(
//                    text = "🧾 ${ui.billDoneCount}",
//                    style = MaterialTheme.typography.bodyMedium
//                )
//            }
//        }
//    }
//}