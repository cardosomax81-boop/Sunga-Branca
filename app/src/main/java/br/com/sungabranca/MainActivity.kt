package br.com.sungabranca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.com.sungabranca.ui.theme.SungaBrancaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = (application as SungaBrancaApp).database
        setContent {
            SungaBrancaTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                var resumeWorkoutId by rememberSaveable { mutableStateOf<Long?>(null) }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0; resumeWorkoutId = null },
                                icon = {},
                                label = { Text("Exercícios") }
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1; resumeWorkoutId = null },
                                icon = {},
                                label = { Text("Treinos") }
                            )
                            NavigationBarItem(
                                selected = tab == 2,
                                onClick = { tab = 2; resumeWorkoutId = null },
                                icon = {},
                                label = { Text("Histórico") }
                            )
                            NavigationBarItem(
                                selected = tab == 3,
                                onClick = { tab = 3; resumeWorkoutId = null },
                                icon = {},
                                label = { Text("Evolução") }
                            )
                            NavigationBarItem(
                                selected = tab == 4,
                                onClick = { tab = 4; resumeWorkoutId = null },
                                icon = {},
                                label = { Text("Backup") }
                            )
                        }
                    }
                ) { innerPadding ->
                    val screenModifier = Modifier.padding(innerPadding)
                    when (tab) {
                        0 -> ExercisesScreen(db.exerciseDao(), screenModifier)
                        1 -> WorkoutsScreen(
                            workoutDao = db.workoutDao(),
                            exerciseDao = db.exerciseDao(),
                            sessionDao = db.sessionDao(),
                            initialWorkoutId = resumeWorkoutId,
                            modifier = screenModifier
                        )
                        2 -> HistoryScreen(
                            sessionDao = db.sessionDao(),
                            exerciseDao = db.exerciseDao(),
                            onResume = { workoutId ->
                                resumeWorkoutId = workoutId
                                tab = 1
                            },
                            modifier = screenModifier
                        )
                        3 -> ProgressScreen(
                            sessionDao = db.sessionDao(),
                            exerciseDao = db.exerciseDao(),
                            modifier = screenModifier
                        )
                        else -> BackupScreen(db = db, modifier = screenModifier)
                    }
                }
            }
        }
    }
}