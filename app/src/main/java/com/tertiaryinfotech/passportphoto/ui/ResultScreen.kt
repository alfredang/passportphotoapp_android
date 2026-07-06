package com.tertiaryinfotech.passportphoto.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.passportphoto.ImageIO
import com.tertiaryinfotech.passportphoto.PhotoSpec
import kotlinx.coroutines.launch

/** Preview, save & share the finished passport photo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    image: Bitmap,
    spec: PhotoSpec,
    onChangeSpec: (PhotoSpec) -> Unit,
    onRetake: () -> Unit,
    isProcessing: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var saveState by remember { mutableStateOf<SaveState>(SaveState.Idle) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Photo", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Card(
                    shape = Brand.CardShape,
                    colors = CardDefaults.cardColors(containerColor = Brand.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = "Your passport photo",
                            modifier = Modifier.widthIn(max = 300.dp),
                        )
                        Text(
                            "${spec.flag} ${spec.country} · ${spec.detail} · " +
                                "${spec.pixelWidth} × ${spec.pixelHeight} px @ 300 DPI",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Column(Modifier.fillMaxWidth()) {
                    CountryPicker(spec = spec, onSelect = onChangeSpec, compact = true)
                }

                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            saveState = SaveState.Saving
                            scope.launch {
                                val error = ImageIO.saveToGallery(context, image)
                                saveState = if (error == null) SaveState.Saved else SaveState.Failed(error)
                            }
                        },
                        enabled = saveState != SaveState.Saving,
                        shape = Brand.ButtonShape,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        Icon(
                            if (saveState == SaveState.Saved) Icons.Filled.CheckCircle else Icons.Filled.SaveAlt,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (saveState) {
                                SaveState.Saving -> "Saving…"
                                SaveState.Saved -> "Saved!"
                                else -> "Save to Gallery"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch { context.startActivity(ImageIO.shareIntent(context, image)) }
                        },
                        shape = Brand.ButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Brand.Surface,
                            contentColor = Brand.Accent,
                        ),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onRetake,
                        shape = Brand.ButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Brand.Surface,
                            contentColor = Brand.Accent,
                        ),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Over", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    (saveState as? SaveState.Failed)?.let {
                        Text(
                            it.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (isProcessing) {
                ProcessingOverlay()
            }
        }
    }
}

private sealed class SaveState {
    data object Idle : SaveState()
    data object Saving : SaveState()
    data object Saved : SaveState()
    data class Failed(val message: String) : SaveState()
}
