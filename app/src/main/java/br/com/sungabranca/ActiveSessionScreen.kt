package br.com.sungabranca

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.sungabranca.data.SessionDao
import br.com.sungabranca.data.SetLogEntity
import br.com.sungabranca.data.WorkoutDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun formatWeight(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()

@Composable
fun ActiveSessionScreen(
    sessionDao: SessionDao,
    workoutDao: WorkoutDao,
    sessionId: Long,
    workoutId: Long,
    workoutName: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items by remember(workoutId) { workoutDao.observeItems(workoutId) }
        .collectAsState(initial = emptyList())
    val logs by remember(sessionId) { sessionDao.observeSetLogs(sessionId) }
        .collectAsState(initial = emptyList())
    var logExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showExit by rememberSaveable { mutableStateOf(false) }
    var restEndAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(restEndAt) {
        val end = restEndAt
        if (end != null) {
            while (System.currentTimeMillis() < end) {
                now = System.currentTimeMillis()
                delay(250)
            }
            restEndAt = null
        }
    }
    val restLeft: Long? = restEndAt?.let { ((it - now + 999) / 1000).coerceAtLeast(0) }

    BackHandler(onBack = { showExit = true })

    val finish: () -> Unit = {
        scope.launch {
            val s = sessionDao.getSession(sessionId)
            if (s != null) {
                sessionDao.updateSession(s.copy(finishedAt = System.currentTimeMillis()))
            }
            onClose()
        }
    }
    val discard: () -> Unit = {
        scope.launch {
            val s = sessionDao.getSession(sessionId)
            if (s != null) sessionDao.deleteSession(s)
            onClose()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            Text(
                text = "Em andamento: $workoutName",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            if (restLeft != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Descanso: %d:%02d".format(restLeft / 60, restLeft % 60),
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = { restEndAt = null }) { Text("Pular") }
                }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                items(items, key = { it.item.id }) { row ->
                    val exerciseId = row.item.exerciseId
                    val exLogs = logs.filter { it.exerciseId == exerciseId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(row.exercise.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Plano: ${row.item.plannedSets} x " +
                                        "${row.item.plannedRepsMin}-${row.item.plannedRepsMax}" +
                                        " · descanso ${row.item.restSeconds}s"
                            )
                            exLogs.forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Série ${log.setNumber}: " +
                                                "${formatWeight(log.weightKg)} kg × ${log.reps}"
                                    )
                                    TextButton(onClick = {
                                        scope.launch { sessionDao.deleteSetLog(log) }
                                    }) { Text("Remover") }
                                }
                            }
                            TextButton(onClick = { logExerciseId = exerciseId }) {
                                Text("Registrar série")
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = finish,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Finalizar treino")
        }
    }

    val dialogRow = items.firstOrNull { it.item.exerciseId == logExerciseId }
    if (dialogRow != null) {
        val exerciseId = dialogRow.item.exerciseId
        val exLogs = logs.filter { it.exerciseId == exerciseId }
        val last = exLogs.lastOrNull()
        SetLogDialog(
            exerciseName = dialogRow.exercise.name,
            setNumber = exLogs.size + 1,
            initialWeight = last?.let { formatWeight(it.weightKg) } ?: "",
            initialReps = last?.reps?.toString() ?: "",
            onDismiss = { logExerciseId = null },
            onSave = { weight, reps ->
                scope.launch {
                    sessionDao.insertSetLog(
                        SetLogEntity(
                            sessionId = sessionId,
                            exerciseId = exerciseId,
                            setNumber = exLogs.size + 1,
                            weightKg = weight,
                            reps = reps
                        )
                    )
                }
                now = System.currentTimeMillis()
                restEndAt = now + dialogRow.item.restSeconds * 1000L
                logExerciseId = null
            }
        )
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text("Sair do treino?") },
            text = { Text("Finalizar guarda o treino no histórico. Descartar apaga o que foi registrado.") },
            confirmButton = {
                Column {
                    TextButton(onClick = finish) { Text("Finalizar treino") }
                    TextButton(onClick = discard) { Text("Descartar treino") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExit = false }) { Text("Continuar treinando") }
            }
        )
    }
}

@Composable
fun SetLogDialog(
    exerciseName: String,
    setNumber: Int,
    initialWeight: String,
    initialReps: String,
    onDismiss: () -> Unit,
    onSave: (weight: Double, reps: Int) -> Unit
) {
    var weightText by rememberSaveable { mutableStateOf(initialWeight) }
    var repsText by rememberSaveable { mutableStateOf(initialReps) }
    val weight = weightText.replace(',', '.').toDoubleOrNull()
    val reps = repsText.toIntOrNull()
    val valid = weight != null && weight >= 0 && reps != null && reps > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$exerciseName · série $setNumber") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Carga (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("Repetições") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (valid) onSave(weight!!, reps!!) },
                enabled = valid
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}