package br.com.sungabranca

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import br.com.sungabranca.data.ExerciseDao
import br.com.sungabranca.data.SessionDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val progressDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

private fun formatLoad(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString()
    else String.format(Locale.forLanguageTag("pt-BR"), "%.1f", w)

private data class SessionSummary(
    val sessionId: Long,
    val startedAt: Long,
    val topWeight: Double,
    val repsAtTop: Int,
    val sets: Int,
    val volume: Double
)

@Composable
fun ProgressScreen(
    sessionDao: SessionDao,
    exerciseDao: ExerciseDao,
    modifier: Modifier = Modifier
) {
    val exercises by remember { exerciseDao.observeAll() }
        .collectAsState(initial = emptyList())
    val loggedIds by remember { sessionDao.observeLoggedExerciseIds() }
        .collectAsState(initial = emptyList())
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }

    val logged = exercises.filter { it.id in loggedIds }.sortedBy { it.name }
    val selected = logged.firstOrNull { it.id == selectedId }

    if (selected != null) {
        ProgressDetail(
            exerciseId = selected.id,
            exerciseName = selected.name,
            sessionDao = sessionDao,
            onBack = { selectedId = null },
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier) {
        Text(
            text = "Evolução por exercício",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        if (logged.isEmpty()) {
            Text(
                text = "Ainda não há séries registradas. Faça um treino para ver a evolução.",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(logged, key = { it.id }) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    supportingContent = { Text(exercise.muscleGroup) },
                    modifier = Modifier.clickable { selectedId = exercise.id }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ProgressDetail(
    exerciseId: Long,
    exerciseName: String,
    sessionDao: SessionDao,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val points by remember(exerciseId) { sessionDao.observeSetPoints(exerciseId) }
        .collectAsState(initial = emptyList())

    val summaries = remember(points) {
        points.groupBy { it.sessionId }
            .map { (sessionId, list) ->
                val top = list.maxOf { it.weightKg }
                SessionSummary(
                    sessionId = sessionId,
                    startedAt = list.first().startedAt,
                    topWeight = top,
                    repsAtTop = list.filter { it.weightKg == top }.maxOf { it.reps },
                    sets = list.size,
                    volume = list.sumOf { it.weightKg * it.reps }
                )
            }
            .sortedByDescending { it.startedAt }
    }
    val best = summaries.maxByOrNull { it.topWeight }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Voltar") }
            Text(text = exerciseName, style = MaterialTheme.typography.titleMedium)
        }
        if (best != null) {
            Text(
                text = "Melhor carga: ${formatLoad(best.topWeight)} kg × ${best.repsAtTop} " +
                        "(${progressDateFormat.format(Date(best.startedAt))})",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Text(
                text = "${summaries.size} treinos com este exercício",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (summaries.size >= 2) {
            val chronological = summaries.reversed()
            val loads = chronological.map { it.topWeight }
            Text(
                text = "Maior carga por treino (do mais antigo ao mais recente)",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
            )
            Text(
                text = "Máx. ${formatLoad(loads.max())} kg · Mín. ${formatLoad(loads.min())} kg",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            WeightChart(
                values = loads,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = progressDateFormat.format(Date(chronological.first().startedAt)),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = progressDateFormat.format(Date(chronological.last().startedAt)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(summaries, key = { it.sessionId }) { s ->
                ListItem(
                    headlineContent = {
                        Text("${formatLoad(s.topWeight)} kg × ${s.repsAtTop}")
                    },
                    supportingContent = {
                        Text(
                            "${progressDateFormat.format(Date(s.startedAt))} · " +
                                    "${s.sets} ${if (s.sets == 1) "série" else "séries"} · " +
                                    "volume ${formatLoad(s.volume)} kg"
                        )
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun WeightChart(values: List<Double>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val baseColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val pad = 16.dp.toPx()
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad
        val minV = values.min()
        val maxV = values.max()

        fun xAt(i: Int): Float =
            if (values.size == 1) pad + w / 2 else pad + w * i / (values.size - 1)

        fun yAt(v: Double): Float =
            if (maxV == minV) pad + h / 2
            else pad + h - (h * ((v - minV) / (maxV - minV))).toFloat()

        drawLine(
            color = baseColor,
            start = Offset(pad, size.height - pad),
            end = Offset(size.width - pad, size.height - pad),
            strokeWidth = 2f
        )
        for (i in 0 until values.size - 1) {
            drawLine(
                color = lineColor,
                start = Offset(xAt(i), yAt(values[i])),
                end = Offset(xAt(i + 1), yAt(values[i + 1])),
                strokeWidth = 5f
            )
        }
        for (i in values.indices) {
            drawCircle(
                color = lineColor,
                radius = 7f,
                center = Offset(xAt(i), yAt(values[i]))
            )
        }
    }
}