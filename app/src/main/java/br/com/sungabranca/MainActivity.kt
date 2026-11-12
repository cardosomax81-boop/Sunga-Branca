package br.com.sungabranca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.sungabranca.data.ExerciseEntity
import br.com.sungabranca.ui.theme.SungaBrancaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = (application as SungaBrancaApp).database.exerciseDao()
        setContent {
            SungaBrancaTheme {
                val exercises by remember { dao.observeAll() }
                    .collectAsState(initial = emptyList())
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ExerciseList(exercises, Modifier.padding(innerPadding))
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
        LazyColumn {
            items(exercises, key = { it.id }) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    supportingContent = {
                        Text("${exercise.muscleGroup} · ${exercise.equipment}")
                    }
                )
                HorizontalDivider()
            }
        }
    }
}