package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.IntervalProgramEntity
import com.example.data.model.IntervalStage
import com.example.ui.WalkingPadViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.OrangeWarning
import com.example.ui.theme.VoltGreen
import com.example.ui.theme.VoltGreenBright
import org.json.JSONArray

@Composable
fun IntervalsScreen(
    viewModel: WalkingPadViewModel,
    modifier: Modifier = Modifier
) {
    val intervalPrograms by viewModel.intervalPrograms.collectAsStateWithLifecycle()
    val isIntervalActive by viewModel.isIntervalActive.collectAsStateWithLifecycle()
    val currentStages by viewModel.currentIntervalStages.collectAsStateWithLifecycle()
    val currentStageIndex by viewModel.currentStageIndex.collectAsStateWithLifecycle()
    val stageRemainingSec by viewModel.stageRemainingSec.collectAsStateWithLifecycle()
    val activeProgram by viewModel.activeIntervalProgram.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("intervals_list"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "INTERVAL PROGRAMS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Automated speed routines for cardiovascular fitness and fat burn",
                        fontSize = 12.sp,
                        color = Color(0xFF8B949E)
                    )
                }
            }

            // Active Runner Cockpit Banner (if running)
            if (isIntervalActive && currentStages.isNotEmpty()) {
                item {
                    val currentStage = currentStages.getOrNull(currentStageIndex)
                    val nextStage = currentStages.getOrNull(currentStageIndex + 1)
                    val totalStageSec = currentStage?.durationSeconds ?: 1
                    val progress = (1.0f - (stageRemainingSec.toFloat() / totalStageSec)).coerceIn(0f, 1f)

                    ActiveIntervalCard(
                        programTitle = activeProgram?.title ?: "Interval Routine",
                        stageName = currentStage?.name ?: "Stage",
                        speedKmh = currentStage?.speedKmh ?: 3.0f,
                        remainingSec = stageRemainingSec,
                        stageProgress = progress,
                        stageNumber = currentStageIndex + 1,
                        totalStages = currentStages.size,
                        nextStageName = nextStage?.name,
                        nextStageSpeed = nextStage?.speedKmh,
                        onStop = { viewModel.stopIntervalProgram() }
                    )
                }
            }

            // Programs List
            items(intervalPrograms, key = { it.id }) { program ->
                IntervalProgramCard(
                    program = program,
                    isRunning = isIntervalActive && activeProgram?.id == program.id,
                    onStart = { viewModel.startIntervalProgram(program) },
                    onDelete = if (!program.isBuiltIn) { { viewModel.deleteIntervalProgram(program.id) } } else null
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Action Button to create custom program
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("btn_create_interval"),
            containerColor = VoltGreen,
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Custom Routine")
        }
    }

    if (showCreateDialog) {
        CreateIntervalDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, desc, stages ->
                viewModel.saveCustomIntervalProgram(title, desc, stages)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun ActiveIntervalCard(
    programTitle: String,
    stageName: String,
    speedKmh: Float,
    remainingSec: Int,
    stageProgress: Float,
    stageNumber: Int,
    totalStages: Int,
    nextStageName: String?,
    nextStageSpeed: Float?,
    onStop: () -> Unit
) {
    val mins = remainingSec / 60
    val secs = remainingSec % 60

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.5.dp, VoltGreen, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("active_interval_runner"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(VoltGreen, RoundedCornerShape(5.dp))
                )
                Text(
                    text = programTitle.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltGreenBright
                )
            }

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("End Routine", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Stage $stageNumber of $totalStages",
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E)
                )
                Text(
                    text = stageName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = VoltGreen
                )
                Text(
                    text = "${String.format("%.1f", speedKmh)} km/h",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
            }
        }

        LinearProgressIndicator(
            progress = { stageProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = VoltGreen,
            trackColor = Color(0xFF1E2633)
        )

        if (nextStageName != null && nextStageSpeed != null) {
            Text(
                text = "Next: $nextStageName @ ${String.format("%.1f", nextStageSpeed)} km/h",
                fontSize = 11.sp,
                color = Color(0xFF8B949E)
            )
        }
    }
}

@Composable
fun IntervalProgramCard(
    program: IntervalProgramEntity,
    isRunning: Boolean,
    onStart: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val stages = remember(program.stagesJson) {
        parseStagesJson(program.stagesJson)
    }
    val totalSeconds = stages.sumOf { it.durationSeconds }
    val totalMinutes = totalSeconds / 60

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, if (isRunning) VoltGreen else DarkSurfaceVariant, RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("interval_card_${program.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = program.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (program.isBuiltIn) {
                        Box(
                            modifier = Modifier
                                .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "BUILT-IN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }
                }

                Text(
                    text = program.description,
                    fontSize = 12.sp,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Routine",
                        tint = Color(0xFF8B949E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Stages Chips / Timeline Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for ((index, stage) in stages.take(5).withIndex()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceElevated)
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${String.format("%.1f", stage.speedKmh)}k",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            if (stages.size > 5) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceElevated)
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${stages.size - 5}",
                        fontSize = 10.sp,
                        color = Color(0xFF8B949E)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFF8B949E),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$totalMinutes mins • ${stages.size} stages",
                    fontSize = 12.sp,
                    color = Color(0xFF8B949E)
                )
            }

            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) OrangeWarning else VoltGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("btn_start_interval_${program.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isRunning) "RESTART" else "START",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun CreateIntervalDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, List<IntervalStage>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val stages = remember {
        mutableStateListOf(
            IntervalStage("Warmup", 2.5f, 120),
            IntervalStage("Fast Walk", 4.5f, 180),
            IntervalStage("Cooldown", 2.5f, 120)
        )
    }

    var newStageName by remember { mutableStateOf("") }
    var newStageSpeed by remember { mutableStateOf("4.0") }
    var newStageDurationSec by remember { mutableStateOf("120") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Interval Routine", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Title") },
                    placeholder = { Text("e.g. Afternoon Stride") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VoltGreen,
                        unfocusedBorderColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. 15-minute conditioning") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VoltGreen,
                        unfocusedBorderColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Stages (${stages.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((idx, stage) in stages.withIndex()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stage.name}: ${stage.speedKmh} km/h (${stage.durationSeconds}s)",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { stages.removeAt(idx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = EmergencyRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Add stage row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newStageName,
                        onValueChange = { newStageName = it },
                        placeholder = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = newStageSpeed,
                        onValueChange = { newStageSpeed = it },
                        placeholder = { Text("km/h") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Button(
                        onClick = {
                            val speed = newStageSpeed.toFloatOrNull() ?: 3.0f
                            val duration = newStageDurationSec.toIntOrNull() ?: 120
                            val name = if (newStageName.isBlank()) "Stage ${stages.size + 1}" else newStageName
                            stages.add(IntervalStage(name, speed, duration))
                            newStageName = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = if (title.isBlank()) "Custom Interval" else title
                    onSave(finalTitle, description, stages.toList())
                },
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = Color.Black),
                enabled = stages.isNotEmpty()
            ) {
                Text("Save Program", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8B949E))
            }
        },
        containerColor = DarkSurface
    )
}

private fun parseStagesJson(json: String): List<IntervalStage> {
    return try {
        val list = mutableListOf<IntervalStage>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                IntervalStage(
                    name = obj.getString("name"),
                    speedKmh = obj.getDouble("speedKmh").toFloat(),
                    durationSeconds = obj.getInt("durationSeconds")
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}
