package com.tertiaryinfotech.passportphoto.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.passportphoto.ImageIO
import com.tertiaryinfotech.passportphoto.PassportPhotoProcessor
import com.tertiaryinfotech.passportphoto.PhotoSpec
import kotlinx.coroutines.launch

/** The main passport-photo creation flow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakePhotoScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val processor = remember { PassportPhotoProcessor() }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var spec by remember { mutableStateOf(PhotoSpec.DEFAULT) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }

    fun process(bitmap: Bitmap, withSpec: PhotoSpec) {
        isProcessing = true
        errorMessage = null
        scope.launch {
            try {
                resultBitmap = processor.process(bitmap, withSpec)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Something went wrong. Try another photo."
            }
            isProcessing = false
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bitmap = ImageIO.decode(context, uri)
                if (bitmap != null) {
                    sourceBitmap = bitmap
                    process(bitmap, spec)
                } else {
                    errorMessage = "That image couldn't be read. Try another photo."
                }
            }
        }
    }

    if (showCamera) {
        CameraScreen(
            onCapture = { bitmap ->
                showCamera = false
                sourceBitmap = bitmap
                process(bitmap, spec)
            },
            onClose = { showCamera = false },
        )
        return
    }

    val result = resultBitmap
    if (result != null) {
        ResultScreen(
            image = result,
            spec = spec,
            onChangeSpec = { newSpec ->
                spec = newSpec
                sourceBitmap?.let { process(it, newSpec) }
            },
            onRetake = {
                sourceBitmap = null
                resultBitmap = null
            },
            isProcessing = isProcessing,
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passport Photo", fontWeight = FontWeight.Bold) },
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HeroCard()
                CountryPicker(spec = spec, onSelect = { spec = it })
                ActionButtons(
                    onTakeSelfie = { showCamera = true },
                    onPickLibrary = {
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )
                TipsCard()
            }

            if (isProcessing) {
                ProcessingOverlay()
            }
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Couldn't Process Photo") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun HeroCard() {
    Card(shape = Brand.CardShape, colors = CardDefaults.cardColors(containerColor = Brand.Surface)) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.Portrait,
                contentDescription = null,
                tint = Brand.Accent,
                modifier = Modifier.size(56.dp),
            )
            Text(
                "Studio-quality passport photos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Take a selfie and we'll remove the background, clean up distractions, and crop it to an official size — all on your device.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Country dropdown shared by the start screen and the result screen. */
@Composable
fun CountryPicker(spec: PhotoSpec, onSelect: (PhotoSpec) -> Unit, compact: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Country", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Box {
            Card(
                shape = Brand.ButtonShape,
                colors = CardDefaults.cardColors(containerColor = Brand.Surface),
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(spec.flag, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(spec.country, fontWeight = FontWeight.Medium)
                        if (!compact) {
                            Text(
                                "Passport photo · ${spec.detail}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        Icons.Filled.UnfoldMore,
                        contentDescription = "Change country",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PhotoSpec.ALL.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("${option.flag}  ${option.country} · ${option.detail}") },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(onTakeSelfie: () -> Unit, onPickLibrary: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onTakeSelfie,
            shape = Brand.ButtonShape,
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Take a Selfie", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onPickLibrary,
            shape = Brand.ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand.Surface,
                contentColor = Brand.Accent,
            ),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose from Library", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TipsCard() {
    Card(shape = Brand.ButtonShape, colors = CardDefaults.cardColors(containerColor = Brand.Surface)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Tip("Face the camera with a neutral expression")
            Tip("Use even lighting and avoid shadows")
            Tip("Keep your whole head and shoulders in frame")
        }
    }
}

@Composable
private fun Tip(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Filled.Verified,
            contentDescription = null,
            tint = Brand.Accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProcessingOverlay() {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(shape = Brand.CardShape) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(color = Brand.Accent)
                Text("Creating your passport photo…", fontWeight = FontWeight.Medium)
            }
        }
    }
}
