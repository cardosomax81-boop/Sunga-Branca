package br.com.sungabranca

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import br.com.sungabranca.data.ExerciseDao
import br.com.sungabranca.data.WorkoutDao
import br.com.sungabranca.data.WorkoutEntity
import kotlinx.coroutines.launch

@Composable
fun WorkoutsScreen(
    workoutDao: WorkoutDao,
    exerciseDao: ExerciseDao,
    modifier: Modifier = Modifier
) {
    val workouts by remember { workoutDao.observeWorkouts() }
        .collectAsState(initial = emptyList())
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    val selected = workouts.firstOrNull { it.id == selectedId }
    if (selected != null) {
        WorkoutDetailScreen(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            workoutId = selected.id,
            workoutName = selected.name,
            onBack = { selectedId = null },
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            Text(
                text = "${workouts.size} treinos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            if (workouts.isEmpty()) {
                Text(
                    text = "Nenhum treino ainda. Toque em \"Novo treino\" para criar o primeiro.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                items(workouts, key = { it.id }) { workout ->
                    ListItem(
                        headlineContent = { Text(workout.name) },
                        modifier = Modifier.clickable { selectedId = workout.id }
                    )
                    HorizontalDivider()
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Novo treino")
        }
    }

    if (showDialog) {
        NewWorkoutDialog(
            onDismiss = { showDialog = false },
            onSave = { name ->
                scope.launch {
                    workoutDao.insertWorkout(
                        WorkoutEntity(
                            name = name,
                            notes = "",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun NewWorkoutDialog(onDismiss: () -> Unit, onSave: (name: String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo treino") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome (ex.: Treino A)") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}