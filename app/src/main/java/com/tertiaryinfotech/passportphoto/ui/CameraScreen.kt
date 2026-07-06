package com.tertiaryinfotech.passportphoto.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Full-screen camera with an oval head-positioning guide (front camera by default). */
@Composable
fun CameraScreen(
    onCapture: (Bitmap) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var useFrontCamera by remember { mutableStateOf(true) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    val previewView = remember { PreviewView(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // (Re)bind the camera whenever permission or lens direction changes.
    LaunchedEffect(hasPermission, useFrontCamera) {
        if (!hasPermission) return@LaunchedEffect
        val provider = cameraProvider ?: run {
            val future = ProcessCameraProvider.getInstance(context)
            suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
                future.addListener(
                    { cont.resume(future.get()) },
                    ContextCompat.getMainExecutor(context)
                )
            }
        }.also { cameraProvider = it }

        val preview = CameraPreview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }
        val selector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        } catch (_: Exception) {
            // Device without the requested lens; fall back to whatever binds.
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            OvalGuide()
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    if (permissionDenied)
                        "Camera access is needed to take your passport photo. Enable it in Settings, or choose a photo from your library instead."
                    else
                        "Requesting camera access…",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Top bar: close + flip.
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close camera", tint = Color.White)
            }
            IconButton(onClick = { useFrontCamera = !useFrontCamera }) {
                Icon(Icons.Filled.Cameraswitch, contentDescription = "Switch camera", tint = Color.White)
            }
        }

        // Bottom: hint + shutter.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Position your head inside the oval",
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (hasPermission) {
                ShutterButton {
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toUprightBitmap()
                                image.close()
                                if (bitmap != null) onCapture(bitmap)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                // Keep the camera open; the user can try again.
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier
            .size(76.dp)
            .border(4.dp, Color.White, CircleShape),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        content = {},
    )
}

/** Dimmed overlay with a clear oval where the head should go. */
@Composable
private fun OvalGuide() {
    Canvas(Modifier.fillMaxSize()) {
        val ovalWidth = size.width * 0.62f
        val ovalHeight = ovalWidth * 1.3f
        val topLeft = Offset((size.width - ovalWidth) / 2, size.height * 0.18f)
        drawOval(
            color = Color.White,
            topLeft = topLeft,
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

/** Converts a captured JPEG frame to an upright bitmap (rotation applied). */
private fun ImageProxy.toUprightBitmap(): Bitmap? {
    return try {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val rotation = imageInfo.rotationDegrees
        if (rotation == 0) {
            bitmap
        } else {
            val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    } catch (_: Exception) {
        null
    }
}
