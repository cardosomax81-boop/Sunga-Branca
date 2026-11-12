package br.com.sungabranca

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import br.com.sungabranca.data.ExerciseDao
import br.com.sungabranca.data.WorkoutDao
import br.com.sungabranca.data.WorkoutItemEntity
import kotlinx.coroutines.launch

@Composable
fun WorkoutDetailScreen(
    workoutDao: WorkoutDao,
    exerciseDao: ExerciseDao,
    workoutId: Long,
    workoutName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val workoutItems by remember(workoutId) { workoutDao.observeItems(workoutId) }
        .collectAsState(initial = emptyList())
    val exercises by remember { exerciseDao.observeAll() }
        .collectAsState(initial = emptyList())
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Voltar") }
                Text(text = workoutName, style = MaterialTheme.typography.titleMedium)
            }
            if (workoutItems.isEmpty()) {
                Text(
                    text = "Nenhum exercício neste treino. Toque em \"Adicionar exercício\".",
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
                        }
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