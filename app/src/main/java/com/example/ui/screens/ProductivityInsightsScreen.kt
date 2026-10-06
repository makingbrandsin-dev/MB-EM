package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.TaskEntity
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.SurfaceBg
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProductivityInsightsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val modelProducer = remember { CartesianChartModelProducer.build() }

    LaunchedEffect(tasks) {
        withContext(Dispatchers.Default) {
            // Process task data for visualization
            val completedCount = tasks.count { it.isCompleted }
            val pendingCount = tasks.size - completedCount
            
            modelProducer.runTransaction {
                columnSeries {
                    series(completedCount, pendingCount)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Productivity Insights",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Task Completion vs Pending", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(),
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis()
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
