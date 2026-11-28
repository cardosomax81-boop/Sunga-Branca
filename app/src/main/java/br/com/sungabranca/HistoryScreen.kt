package br.com.sungabranca

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import br.com.sungabranca.data.SessionDao
import br.com.sungabranca.data.SessionEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
private fun formatKg(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()

private fun describeSession(s: SessionEntity): String {
    val date = historyDateFormat.format(Date(s.startedAt))
    val end = s.finishedAt
    return if (end == null) {
        "$date · Em aberto"
    } else {
        val minutes = ((end - s.startedAt) / 60000).coerceAtLeast(1)
        "$date · $minutes min"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    sessionDao: SessionDao,
    exerciseDao: ExerciseDao,
    onResume: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by remember { sessionDao.observeSessions() }
        .collectAsState(initial = emptyList())
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var checkedIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val selected = sessions.firstOrNull { it.id == selectedId }
    if (selected != null) {
        SessionDetailScreen(
            session = selected,
            sessionDao = sessionDao,
            exerciseDao = exerciseDao,
            onBack = { selectedId = null },
            onResume = onResume,
            modifier = modifier
        )
        return
    }

    // Só conta o que ainda existe na lista.
    val chosen = sessions.filter { it.id in checkedIds }
    val selectionMode = chosen.isNotEmpty()

    BackHandler(enabled = selectionMode) { checkedIds = emptySet() }

    Column(modifier = modifier) {
        if (selectionMode) {
            Text(
                text = if (chosen.size == 1) "1 treino selecionado" else "${chosen.size} treinos selecionados",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { checkedIds = emptySet() }) { Text("Cancelar") }
                TextButton(onClick = {
                    checkedIds = sessions.filter { it.finishedAt != null }.map { it.id }.toSet()
                }) { Text("Selecionar tudo") }
                TextButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Excluir (${chosen.size})") }
            }
        } else {
            Text(
                text = "${sessions.size} treinos registrados",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (sessions.isEmpty()) {
            Text(
                text = "Nada por aqui ainda. Inicie um treino na aba Treinos.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(sessions, key = { it.id }) { session ->
                val isChecked = session.id in checkedIds
                val isOpen = session.finishedAt == null
                val toggle = {
                    checkedIds = if (isChecked) checkedIds - session.id else checkedIds + session.id
                }
                ListItem(
                    headlineContent = { Text(session.workoutName) },
                    supportingContent = { Text(describeSession(session)) },
                    leadingContent = if (selectionMode) {
                        {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                                enabled = !isOpen
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (selectionMode) {
                                if (!isOpen) toggle()
                            } else {
                                selectedId = session.id
                            }
                        },
                        onLongClick = { if (!isOpen) toggle() }
                    )
                )
                HorizontalDivider()
            }
        }
    }

    if (showDeleteDialog && selectionMode) {
        val count = chosen.size
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(if (count == 1) "Excluir 1 treino?" else "Excluir $count treinos?")
            },
            text = {
                Text("Isso apaga os treinos selecionados e todas as séries registradas neles. Não dá para desfazer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = chosen
                        showDeleteDialog = false
                        checkedIds = emptySet()
                        scope.launch { sessionDao.deleteSessions(toDelete) }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SessionDetailScreen(
    session: SessionEntity,
    sessionDao: SessionDao,
    exerciseDao: ExerciseDao,
    onBack: () -> Unit,
    onResume: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val logs by remember(session.id) { sessionDao.observeSetLogs(session.id) }
        .collectAsState(initial = emptyList())
    val exercises by remember { exerciseDao.observeAll() }
        .collectAsState(initial = emptyList())
    val names = exercises.associate { it.id to it.name }
    val grouped = logs.groupBy { it.exerciseId }
    val scope = rememberCoroutineScope()
    var showDiscard by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Voltar") }
            Text(text = session.workoutName, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = describeSession(session),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (session.finishedAt == null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Button(
                    onClick = { session.workoutId?.let(onResume) },
                    enabled = session.workoutId != null
                ) { Text("Continuar treino") }
                Row {
                    TextButton(onClick = {
                        scope.launch {
                            val end = (logs.maxOfOrNull { it.completedAt }
                                ?: System.currentTimeMillis())
                                .coerceAtLeast(session.startedAt)
                            sessionDao.updateSession(session.copy(finishedAt = end))
                        }
                    }) { Text("Finalizar") }
                    TextButton(onClick = { showDiscard = true }) { Text("Descartar") }
                }
            }
        }
        if (logs.isEmpty()) {
            Text(
                text = "Nenhuma série registrada neste treino.",
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            grouped.forEach { (exerciseId, exerciseLogs) ->
                item(key = exerciseId) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = names[exerciseId] ?: "Exercício",
                            style = MaterialTheme.typography.titleSmall
                        )
                        exerciseLogs.forEach { log ->
                            Text("Série ${log.setNumber}: ${formatKg(log.weightKg)} kg × ${log.reps}")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Descartar treino?") },
            text = { Text("Isso apaga o treino e todas as séries registradas nele. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscard = false
                    scope.launch { sessionDao.deleteSession(session) }
                }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text("Cancelar") }
            }
        )
    }
}