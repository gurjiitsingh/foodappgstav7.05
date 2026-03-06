package com.it10x.foodappgstav7_05.ui.settings

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.it10x.foodappgstav7_05.viewmodel.ProductSyncViewModel
import com.it10x.foodappgstav7_05.viewmodel.OutletSyncViewModel
import com.it10x.foodappgstav7_05.viewmodel.TableSyncViewModel
import com.it10x.foodappgstav7_05.core.FirstSyncManager
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

@Composable
fun FirstAutoSyncScreen(
    onFinished: () -> Unit
) {

    val context = LocalContext.current

    val outletVm: OutletSyncViewModel = viewModel()
    val productVm: ProductSyncViewModel = viewModel()
    val tableVm: TableSyncViewModel = viewModel()

    var stage by remember { mutableStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {

        // OUTLET
        stage = 1
        outletVm.syncOutlet()
        kotlinx.coroutines.delay(2000)

        // MENU
        stage = 2
        productVm.syncAll()
        kotlinx.coroutines.delay(3000)

        // TABLES
        stage = 3
        tableVm.syncTables()
        kotlinx.coroutines.delay(2500)

        // FINISH
        stage = 4
        FirstSyncManager.setFirstSyncDone(context)

        finished = true
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (!finished) {

            CircularProgressIndicator()

            Spacer(Modifier.height(16.dp))

            Text(
                when(stage) {
                    0 -> "Preparing POS..."
                    1 -> "Downloading 1 ..."
                    2 -> "Downloading 2 ..."
                    3 -> "Downloading 3 ..."
                    else -> "Finishing Setup..."
                }
            )

        } else {

            Text(
                "Setup Completed ✅",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onFinished() }
            ) {
                Text("Start POS")
            }

        }
    }
}


