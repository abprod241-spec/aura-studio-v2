package com.laparole.aurastudio

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) { AuraScreen() }
            }
        }
    }
}

@Composable
fun AuraScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var media by remember { mutableStateOf<List<MediaRef>>(emptyList()) }
    var music by remember { mutableStateOf<File?>(null) }
    var preset by remember { mutableStateOf(Preset.CINEMA) }
    var status by remember { mutableStateOf("Choisis des medias.") }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            busy = true
            status = "Importation..."
            scope.launch {
                val list = withContext(Dispatchers.IO) { AuraEngine.importMedia(context, uris) }
                media = list
                busy = false
                status = list.size.toString() + " / " + uris.size.toString() + " media(s) prets."
            }
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val f = withContext(Dispatchers.IO) { AuraEngine.importAudio(context, uri) }
                music = f
                busy = false
                status = if (f != null) "Musique prete." else "Musique illisible."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "AURA Studio", style = MaterialTheme.typography.headlineMedium)
        Text(text = status)
        if (busy) CircularProgressIndicator()

        Button(enabled = !busy, onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }) { Text(text = "1 - Choisir des medias") }

        Button(enabled = !busy, onClick = {
            audioPicker.launch("audio/*")
        }) { Text(text = if (music == null) "2 - Musique (optionnel)" else "2 - Musique OK") }

        if (music != null) {
            TextButton(enabled = !busy, onClick = {
                music = null
                status = "Musique retiree."
            }) { Text(text = "Retirer la musique") }
        }

        Text(text = "3 - Ambiance")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Preset.values().forEach { p ->
                if (p == preset) {
                    Button(enabled = !busy, onClick = { preset = p }) { Text(text = p.label) }
                } else {
                    OutlinedButton(enabled = !busy, onClick = { preset = p }) { Text(text = p.label) }
                }
            }
        }

        Button(enabled = !busy && media.isNotEmpty(), onClick = {
            busy = true
            status = "Creation du film..."
            AuraEngine.export(context, media, music, preset) { msg ->
                busy = false
                status = msg
            }
        }) { Text(text = "4 - Creer le film") }
    }
}
