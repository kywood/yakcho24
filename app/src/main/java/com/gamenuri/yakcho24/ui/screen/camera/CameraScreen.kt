package com.gamenuri.yakcho24.ui.screen.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import java.util.Locale
import java.util.concurrent.Executors
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.gamenuri.yakcho24.camera.DetectionResult
import com.gamenuri.yakcho24.camera.HerbClassifier
import com.gamenuri.yakcho24.camera.YuvToRgbConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices

import androidx.camera.core.Camera
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged

private const val TAG = "BBoxDiag"

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableStateOf(1f) }

    var locationText by remember { mutableStateOf("위치 정보 로딩 중...") }
    val recognitionResults = remember { mutableStateListOf<DetectionResult>() }
    // 회전 적용 후 똑바른 이미지 크기 (오버레이 좌표 변환 기준)
    var uprightW by remember { mutableIntStateOf(0) }
    var uprightH by remember { mutableIntStateOf(0) }
    val classifier = remember { HerbClassifier(context) }
    val yuvConverter = remember { YuvToRgbConverter(context) }

    DisposableEffect(Unit) {
        onDispose {
            classifier.close()
            yuvConverter.release()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        val toRequest = buildList {
            if (!hasCameraPermission) add(Manifest.permission.CAMERA)
            if (!hasLocationPermission) add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (toRequest.isNotEmpty()) permissionLauncher.launch(toRequest.toTypedArray())
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            locationText = "위치 권한 없음"
            return@LaunchedEffect
        }
        @SuppressLint("MissingPermission")
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    locationText = "위치 정보 없음"
                    return@addOnSuccessListener
                }
                val geocoder = Geocoder(context, Locale.KOREA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        locationText = addresses.firstOrNull()?.toDisplayAddress() ?: "주소 변환 실패"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = runCatching {
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    }.getOrNull()
                    locationText = addresses?.firstOrNull()?.toDisplayAddress() ?: "주소 변환 실패"
                }
            }
            .addOnFailureListener { locationText = "위치 불러오기 실패" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(camera) {
                detectTransformGestures { _, _, zoom, _ ->
                    val cam = camera ?: return@detectTransformGestures
                    val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures

                    val newZoom = (zoomState.zoomRatio * zoom)
                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)

                    cam.cameraControl.setZoomRatio(newZoom)
                    zoomRatio = newZoom
                }
            }
    ) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner,
                onCameraReady = { cam ->
                    camera = cam
                    zoomRatio = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                },
                onFrameAnalyzed = { imageProxy ->
                    val rotation = imageProxy.imageInfo.rotationDegrees

                    // 1) YUV → RGB
                    val raw = Bitmap.createBitmap(
                        imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
                    )
                    yuvConverter.yuvToRgb(imageProxy, raw)

                    // 2) 추론 전에 회전 적용 → 모델이 항상 올바른 방향 이미지를 봄
                    val upright = rotateBitmap(raw, rotation)
                    if (upright !== raw) raw.recycle()

                    val uw = upright.width
                    val uh = upright.height

                    // 3) 추론 (HerbClassifier 내부에서 center crop → 640×640)
                    val results = classifier.detect(upright)
                    upright.recycle()

                    Log.d(TAG, "센서: ${imageProxy.width}x${imageProxy.height} rot=${rotation}° → upright: ${uw}x${uh}")
                    if (results.isNotEmpty()) {
                        val r = results.first()
                        Log.d(TAG, "모델 출력(0~1): L=${r.boundingBox.left} T=${r.boundingBox.top} R=${r.boundingBox.right} B=${r.boundingBox.bottom}")
                    }

                    uprightW = uw
                    uprightH = uh
                    recognitionResults.clear()
                    recognitionResults.addAll(results)
                },
            )

            BoundingBoxOverlay(
                results = recognitionResults,
                uprightW = uprightW,
                uprightH = uprightH,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "카메라 권한이 필요합니다", color = Color.White, fontSize = 16.sp)
            }
        }

        LocationOverlay(
            locationText = locationText,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        DelegateInfoCard(
            delegateInfo = classifier.delegateInfo,
            modifier = Modifier.align(Alignment.TopStart),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 12.dp)
                .background(Color.Black.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = String.format("%.1fx", zoomRatio),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private const val INFERENCE_INTERVAL_MS = 100L

/** 센서 원본 bitmap을 rotationDegrees만큼 회전해 똑바른 이미지를 반환 */
private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return src
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCameraReady: (Camera) -> Unit = {},
    onFrameAnalyzed: (ImageProxy) -> Unit,
) {
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                // FILL_CENTER: 비율 유지하며 뷰를 꽉 채움 (넘치는 부분은 중앙 기준 crop)
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener(
                    {
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            var lastAnalysisTime = 0L
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                        val now = System.currentTimeMillis()
                                        if (now - lastAnalysisTime >= INFERENCE_INTERVAL_MS) {
                                            lastAnalysisTime = now
                                            onFrameAnalyzed(imageProxy)
                                        }
                                        imageProxy.close()
                                    }
                                }
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis,
                            )
                            onCameraReady(camera)

//                            cameraProvider.bindToLifecycle(
//                                lifecycleOwner,
//                                CameraSelector.DEFAULT_BACK_CAMERA,
//                                preview,
//                                imageAnalysis,
//                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "카메라 바인딩 실패", e)
                        }
                    },
                    ContextCompat.getMainExecutor(ctx),
                )
            }
        },
    )
}

/**
 * 바운딩 박스 오버레이.
 *
 * 좌표 변환 파이프라인:
 *
 * 모델 입력 생성:  upright(uprightW×uprightH) → center crop(square) → resize 640×640
 * 모델 출력:       normalized 0~1 (640×640 = center crop 기준)
 *
 * 역변환:
 *  [1] norm(0~1) × cropSize + cropOffset  → upright 픽셀
 *  [2] upright 픽셀 × FILL_CENTER scale − crop offset  → 화면 픽셀
 */
@Composable
private fun BoundingBoxOverlay(
    results: List<DetectionResult>,
    uprightW: Int,
    uprightH: Int,
    modifier: Modifier = Modifier,
) {
    val boxColor = Color(0xFF76C442)
    val labelPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#76C442")
            textSize = 42f
            isAntiAlias = true
            isFakeBoldText = true
        }
    }
    val labelBgPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(160, 0, 0, 0)
        }
    }
    val logThrottle = remember { LongArray(1) }

    Canvas(modifier = modifier) {
        if (uprightW <= 0 || uprightH <= 0 || results.isEmpty()) return@Canvas

        val viewW = size.width
        val viewH = size.height

        // [1] center crop 영역 계산 (HerbClassifier.preprocessBitmap과 동일 로직)
        val cropSize  = minOf(uprightW, uprightH).toFloat()
        val cropLeft  = (uprightW - cropSize) / 2f
        val cropTop   = (uprightH - cropSize) / 2f

        // [2] FILL_CENTER: 비율 유지하며 뷰를 꽉 채움, 넘치는 부분은 중앙 기준 crop
        val scale   = maxOf(viewW / uprightW, viewH / uprightH)
        val offsetX = (uprightW * scale - viewW) / 2f  // 좌우 각각 잘린 픽셀
        val offsetY = (uprightH * scale - viewH) / 2f  // 상하 각각 잘린 픽셀

        val now = System.currentTimeMillis()
        val shouldLog = now - logThrottle[0] > 2000
        if (shouldLog) {
            logThrottle[0] = now
            Log.d(TAG, "=== 오버레이 변환 ===")
            Log.d(TAG, "upright: ${uprightW}x${uprightH}, crop: ${cropSize.toInt()} cropL=${cropLeft.toInt()} cropT=${cropTop.toInt()}")
            Log.d(TAG, "view: ${viewW.toInt()}x${viewH.toInt()}, scale=${"%.4f".format(scale)} offsetX=${"%.1f".format(offsetX)} offsetY=${"%.1f".format(offsetY)}")
        }

        results.forEachIndexed { idx, result ->
            val box = result.boundingBox  // 0~1, center crop 기준

            // norm → upright 픽셀 → 화면 픽셀 (FILL_CENTER crop 오프셋 차감)
            val left   = (cropLeft + box.left   * cropSize) * scale - offsetX
            val top    = (cropTop  + box.top    * cropSize) * scale - offsetY
            val right  = (cropLeft + box.right  * cropSize) * scale - offsetX
            val bottom = (cropTop  + box.bottom * cropSize) * scale - offsetY

            if (shouldLog && idx == 0) {
                Log.d(TAG, "box norm: L=${box.left} T=${box.top} R=${box.right} B=${box.bottom}")
                Log.d(TAG, "box px:   L=${left.toInt()} T=${top.toInt()} R=${right.toInt()} B=${bottom.toInt()}")
            }

            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 3.dp.toPx()),
            )

            val label = "${result.label}  ${"%.0f".format(result.confidence * 100)}%"
            val textBounds = android.graphics.Rect()
            labelPaint.getTextBounds(label, 0, label.length, textBounds)

            val textX = left
            val textY = if (top - textBounds.height() - 8f >= 0f) {
                top - 8f
            } else {
                bottom + textBounds.height() + 8f
            }

            drawContext.canvas.nativeCanvas.drawRect(
                textX, textY - textBounds.height() - 4f,
                textX + textBounds.width() + 8f, textY + 4f,
                labelBgPaint,
            )
            drawContext.canvas.nativeCanvas.drawText(label, textX + 4f, textY, labelPaint)
        }
    }
}

@Composable
private fun LocationOverlay(locationText: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(0xFF76C442),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = locationText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RecognitionResultCard(results: List<DetectionResult>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.70f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (results.isEmpty()) {
                CircularProgressIndicator(
                    color = Color(0xFF76C442),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "카메라를 식물에 가까이 대주세요", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            } else {
                results.forEachIndexed { index, result ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = result.label,
                            color = if (index == 0) Color(0xFF76C442) else Color.White,
                            fontSize = if (index == 0) 15.sp else 13.sp,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            text = "%.1f%%".format(result.confidence * 100),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                    }
                    if (index < results.lastIndex) Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun DelegateInfoCard(delegateInfo: String, modifier: Modifier = Modifier) {
    val (label, color) = when (delegateInfo) {
        "GPU"   -> "⚡ GPU"  to Color(0xFF76C442)
        "NNAPI" -> "⚡ NNAPI" to Color(0xFFFFD700)
        else    -> "🖥 CPU"  to Color(0xFFAAAAAA)
    }
    Box(
        modifier = modifier
            .padding(start = 12.dp, top = 52.dp)
            .background(Color.Black.copy(alpha = 0.60f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// "시/군 구 동" 형태로 조합 — 예: "용인시 기흥구 동백동"
private fun Address.toDisplayAddress(): String {
    val parts = listOfNotNull(subAdminArea, locality, subLocality ?: thoroughfare).distinct()
    return parts.joinToString(" ").ifBlank { getAddressLine(0) ?: "주소 없음" }
}