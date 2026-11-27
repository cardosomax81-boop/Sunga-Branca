package br.com.sungabranca

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.sungabranca.data.AppDatabase
import br.com.sungabranca.data.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(db: AppDatabase, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var pendingRestore by rememberSaveable { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                status = try {
                    withContext(Dispatchers.IO) {
                        val text = BackupManager.export(db)
                        val out = context.contentResolver.openOutputStream(uri, "wt")
                            ?: throw IOException("Não foi possível abrir o arquivo.")
                        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    }
                    "Backup salvo com sucesso."
                } catch (e: Exception) {
                    "Falha ao salvar o backup: ${e.message}"
                }
                busy = false
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) pendingRestore = uri
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Backup dos dados", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Salva perfil, exercícios, treinos e histórico em um arquivo JSON. " +
                    "Guarde esse arquivo fora do celular (e-mail, nuvem, computador): " +
                    "se o app for desinstalado, os dados internos são apagados.",
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Button(
            enabled = !busy,
            onClick = {
                val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                exportLauncher.launch("sunga-branca-backup-$stamp.json")
            }
        ) { Text("Exportar backup") }
        OutlinedButton(
            enabled = !busy,
            onClick = { restoreLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Restaurar backup") }
        if (status.isNotEmpty()) {
            Text(text = status, modifier = Modifier.padding(top = 16.dp))
        }
    }

    val uri = pendingRestore
    if (uri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restaurar backup?") },
            text = {
                Text("Isso substitui TODOS os dados atuais pelos do arquivo escolhido. Não dá para desfazer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    busy = true
                    scope.launch {
                        status = try {
                            val total = withContext(Dispatchers.IO) {
                                val text = context.contentResolver.openInputStream(uri)
                                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                                    ?: throw IOException("Não foi possível abrir o arquivo.")
                                BackupManager.restore(db, text)
                            }
                            "Backup restaurado: $total registros."
                        } catch (e: Exception) {
                            "Falha ao restaurar (nada foi alterado): ${e.message}"
                        }
                        busy = false
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text("Cancelar") }
            }
        )
    }
}