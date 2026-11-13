package br.com.sungabranca

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.sungabranca.data.ExerciseDao
import br.com.sungabranca.data.SessionDao
import br.com.sungabranca.data.SessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

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

@Composable
fun HistoryScreen(
    sessionDao: SessionDao,
    exerciseDao: ExerciseDao,
    modifier: Modifier = Modifier
) {
    val sessions by remember { sessionDao.observeSessions() }
        .collectAsState(initial = emptyList())
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }

    val selected = sessions.firstOrNull { it.id == selectedId }
    if (selected != null) {
        SessionDetailScreen(
            session = selected,
            sessionDao = sessionDao,
            exerciseDao = exerciseDao,
            onBack = { selectedId = null },
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier) {
        Text(
            text = "${sessions.size} treinos registrados",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        if (sessions.isEmpty()) {
            Text(
                text = "Nada por aqui ainda. Inicie um treino na aba Treinos.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(sessions, key = { it.id }) { session ->
                ListItem(
                    headlineContent = { Text(session.workoutName) },
                    supportingContent = { Text(describeSession(session)) },
                    modifier = Modifier.clickable { selectedId = session.id }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SessionDetailScreen(
    session: SessionEntity,
    sessionDao: SessionDao,
    exerciseDao: ExerciseDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val logs by remember(session.id) { sessionDao.observeSetLogs(session.id) }
        .collectAsState(initial = emptyList())
    val exercises by remember { exerciseDao.observeAll() }
        .collectAsState(initial = emptyList())
    val names = exercises.associate { it.id to it.name }
    val grouped = logs.groupBy { it.exerciseId }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Voltar") }
            Text(text = session.workoutName, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = describeSession(session),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
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
}