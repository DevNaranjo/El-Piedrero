package com.app.rondacanaria.ui.qr

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.app.rondacanaria.domain.model.ConnectionInfo
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class QrCodeAnalyzer(
    private val onQrDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val map = mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
        setHints(map)
    }
    private val isScanned = AtomicBoolean(false)
    private var reusableBuffer: ByteArray? = null

    override fun analyze(imageProxy: ImageProxy) {
        if (isScanned.get()) {
            imageProxy.close()
            return
        }

        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val remaining = buffer.remaining()

        // Reutilización eficiente de búfer de memoria para evitar presión continua sobre el Garbage Collector
        val data = if (reusableBuffer != null && reusableBuffer?.size == remaining) {
            reusableBuffer!!
        } else {
            ByteArray(remaining).also { reusableBuffer = it }
        }
        buffer.get(data)

        val width = imageProxy.width
        val height = imageProxy.height

        val source = PlanarYUVLuminanceSource(
            data,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decodeWithState(bitmap)
            if (isScanned.compareAndSet(false, true)) {
                onQrDetected(result.text)
            }
        } catch (_: NotFoundException) {
            // Frame normal sin código QR detectado
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }
}

@Composable
fun QrCameraScanner(
    modifier: Modifier = Modifier,
    onConnectionInfoScanned: (ConnectionInfo) -> Unit,
    onScanError: (String) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                cameraExecutor,
                                QrCodeAnalyzer { rawText ->
                                    val connectionInfo = ConnectionInfo.fromJson(rawText)
                                    if (connectionInfo != null) {
                                        onConnectionInfoScanned(connectionInfo)
                                    } else {
                                        onScanError("Código QR no válido para El Piedrero")
                                    }
                                }
                            )
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        onScanError("Error al iniciar la cámara: ${e.localizedMessage}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )
    }
}
