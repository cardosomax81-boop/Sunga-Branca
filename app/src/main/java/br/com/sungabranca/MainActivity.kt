package br.com.sungabranca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.sungabranca.data.ExerciseEntity
import br.com.sungabranca.ui.theme.SungaBrancaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = (application as SungaBrancaApp).database.exerciseDao()
        setContent {
            SungaBrancaTheme {
                val exercises by remember { dao.observeAll() }
                    .collectAsState(initial = emptyList())
                var showDialog by rememberSaveable { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        ExtendedFloatingActionButton(onClick = { showDialog = true }) {
                            Text("Novo exercício")
                        }
                    }
                ) { innerPadding ->
                    ExerciseList(exercises, Modifier.padding(innerPadding))
                }

                if (showDialog) {
                    NewExerciseDialog(
                        onDismiss = { showDialog = false },
                        onSave = { name, group, equipment ->
                            scope.launch {
                                dao.insert(
                                    ExerciseEntity(
                                        name = name,
                                        muscleGroup = group.ifBlank { "Outros" },
                                        equipment = equipment.ifBlank { "Outros" },
                                        isCustom = true
                                    )
                                )
                            }
                            showDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseList(exercises: List<ExerciseEntity>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "${exercises.size} exercícios",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
            items(exercises, key = { it.id }) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    supportingContent = {
                        val tag = if (exercise.isCustom) " · Personalizado" else ""
                        Text("${exercise.muscleGroup} · ${exercise.equipment}$tag")
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun NewExerciseDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, group: String, equipment: String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("") }
    var equipment by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo exercício") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("Grupo muscular") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Equipamento") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), group.trim(), equipment.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}