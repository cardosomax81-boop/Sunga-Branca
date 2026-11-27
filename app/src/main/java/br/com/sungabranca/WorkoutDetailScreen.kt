package br.com.sungabranca

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.sungabranca.data.ExerciseDao
import br.com.sungabranca.data.SessionDao
import br.com.sungabranca.data.SessionEntity
import br.com.sungabranca.data.WorkoutDao
import br.com.sungabranca.data.WorkoutItemEntity
import kotlinx.coroutines.launch

@Composable
fun WorkoutDetailScreen(
    workoutDao: WorkoutDao,
    exerciseDao: ExerciseDao,
    sessionDao: SessionDao,
    workoutId: Long,
    workoutName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by remember { sessionDao.observeSessions() }
        .collectAsState(initial = emptyList())
    val sessionId: Long? = sessions
        .firstOrNull { it.workoutId == workoutId && it.finishedAt == null }
        ?.id
    BackHandler(enabled = sessionId == null, onBack = onBack)

    val workoutItems by remember(workoutId) { workoutDao.observeItems(workoutId) }
        .collectAsState(initial = emptyList())
    val exercises by remember { exerciseDao.observeAll() }
        .collectAsState(initial = emptyList())
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var editingItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    if (sessionId != null) {
        ActiveSessionScreen(
            sessionDao = sessionDao,
            workoutDao = workoutDao,
            sessionId = sessionId,
            workoutId = workoutId,
            workoutName = workoutName,
            onClose = { },
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Voltar") }
                Text(
                    text = workoutName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            sessionDao.insertSession(
                                SessionEntity(
                                    workoutId = workoutId,
                                    workoutName = workoutName
                                )
                            )
                        }
                    },
                    enabled = workoutItems.isNotEmpty()
                ) { Text("Iniciar treino") }
            }
            if (workoutItems.isEmpty()) {
                Text(
                    text = "Nenhum exercício neste treino. Toque em \"Adicionar exercício\".",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Text(
                    text = "Toque em um exercício para editar séries, repetições e descanso.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                items(workoutItems, key = { it.item.id }) { row ->
                    ListItem(
                        headlineContent = { Text(row.exercise.name) },
                        supportingContent = {
                            val i = row.item
                            Text(
                                "${i.plannedSets} x ${i.plannedRepsMin}-${i.plannedRepsMax}" +
                                        " · descanso ${i.restSeconds}s"
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = {
                                scope.launch { workoutDao.deleteItem(row.item) }
                            }) { Text("Remover") }
                        },
                        modifier = Modifier.clickable { editingItemId = row.item.id }
                    )
                    HorizontalDivider()
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showPicker = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Adicionar exercício")
        }
    }

    val editingRow = workoutItems.firstOrNull { it.item.id == editingItemId }
    if (editingRow != null) {
        EditItemDialog(
            exerciseName = editingRow.exercise.name,
            item = editingRow.item,
            onDismiss = { editingItemId = null },
            onSave = { updated ->
                scope.launch { workoutDao.updateItem(updated) }
                editingItemId = null
            }
        )
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Escolha o exercício") },
            text = {
                LazyColumn {
                    items(exercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = { Text(exercise.muscleGroup) },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    workoutDao.insertItem(
                                        WorkoutItemEntity(
                                            workoutId = workoutId,
                                            exerciseId = exercise.id,
                                            position = workoutItems.size,
                                            plannedSets = 3,
                                            plannedRepsMin = 8,
                                            plannedRepsMax = 12,
                                            restSeconds = 90
                                        )
                                    )
                                }
                                showPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun EditItemDialog(
    exerciseName: String,
    item: WorkoutItemEntity,
    onDismiss: () -> Unit,
    onSave: (WorkoutItemEntity) -> Unit
) {
    var sets by rememberSaveable { mutableStateOf(item.plannedSets.toString()) }
    var repsMin by rememberSaveable { mutableStateOf(item.plannedRepsMin.toString()) }
    var repsMax by rememberSaveable { mutableStateOf(item.plannedRepsMax.toString()) }
    var rest by rememberSaveable { mutableStateOf(item.restSeconds.toString()) }

    val setsV = sets.toIntOrNull()
    val minV = repsMin.toIntOrNull()
    val maxV = repsMax.toIntOrNull()
    val restV = rest.toIntOrNull()
    val updated: WorkoutItemEntity? =
        if (setsV != null && setsV >= 1 &&
            minV != null && minV >= 1 &&
            maxV != null && maxV >= minV &&
            restV != null && restV >= 0
        ) {
            item.copy(
                plannedSets = setsV,
                plannedRepsMin = minV,
                plannedRepsMax = maxV,
                restSeconds = restV
            )
        } else {
            null
        }

    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it.filter(Char::isDigit) },
                    label = { Text("Séries") },
                    singleLine = true,
                    keyboardOptions = numberKeyboard
                )
                OutlinedTextField(
                    value = repsMin,
                    onValueChange = { repsMin = it.filter(Char::isDigit) },
                    label = { Text("Repetições (mínimo)") },
                    singleLine = true,
                    keyboardOptions = numberKeyboard
                )
                OutlinedTextField(
                    value = repsMax,
                    onValueChange = { repsMax = it.filter(Char::isDigit) },
                    label = { Text("Repetições (máximo)") },
                    singleLine = true,
                    keyboardOptions = numberKeyboard
                )
                OutlinedTextField(
                    value = rest,
                    onValueChange = { rest = it.filter(Char::isDigit) },
                    label = { Text("Descanso (segundos)") },
                    singleLine = true,
                    keyboardOptions = numberKeyboard
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { updated?.let(onSave) },
                enabled = updated != null
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}