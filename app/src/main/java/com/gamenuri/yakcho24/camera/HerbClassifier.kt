package com.gamenuri.yakcho24.camera

import android.content.Context
import android.graphics.RectF
import android.os.Build
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,   // 정규화된 좌표 (0f..1f), left/top/right/bottom
)

/**
 * YOLOv8 TFLite 기반 객체 탐지기 (best_float16.tflite).
 *
 * 출력 텐서 형식: [1, 4+nc, num_det]  (YOLOv8 기본)
 *   - 앞 4개 행: cx, cy, w, h (정규화 0~1)
 *   - 나머지 nc 행: 각 클래스 confidence
 */
class HerbClassifier(
    private val context: Context,
    private val modelFileName: String = "best_float16.tflite",
    private val labelFileName: String = "herb_labels.txt",
    private val confThreshold: Float = 0.25f,
    private val iouThreshold: Float = 0.45f,
    private val topK: Int = 10,
) {
    companion object {
        private const val TAG = "HerbClassifier"
        private const val NUM_THREADS = 4
        private const val INPUT_SIZE = 640
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var nnApiDelegate: NnApiDelegate? = null

    /** 실제 적용된 delegate ("GPU" / "NNAPI" / "CPU") */
    var delegateInfo: String = "CPU"
        private set
    private var labels: List<String> = emptyList()

    // 출력 텐서 형식 ([1, attrs, dets] vs [1, dets, attrs])
    private var numAttributes = 0
    private var numDetections = 0
    private var attrsFirst = true   // true → [1, attrs, dets]

    private val inputBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        .apply { order(ByteOrder.nativeOrder()) }

    init {
        setupInterpreter()
        labels = loadLabels()
    }

    private fun setupInterpreter() {
        val options = Interpreter.Options().apply { numThreads = NUM_THREADS }

        // 1순위: GPU Delegate
        var pending = "CPU"
        runCatching {
            gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate!!)
            pending = "GPU"
            Log.d(TAG, "GPU Delegate 적용")
        }.onFailure { e ->
            Log.w(TAG, "GPU 미지원, 폴백: ${e.message}")
            gpuDelegate?.close()
            gpuDelegate = null
        }

        // 2순위: NNAPI (GPU 실패 + API 28+)
        if (pending == "CPU" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                nnApiDelegate = NnApiDelegate()
                options.addDelegate(nnApiDelegate!!)
                pending = "NNAPI"
                Log.d(TAG, "NNAPI Delegate 적용")
            }.onFailure { e ->
                Log.w(TAG, "NNAPI 실패, CPU 폴백: ${e.message}")
                nnApiDelegate?.close()
                nnApiDelegate = null
            }
        }

        runCatching {
            interpreter = Interpreter(loadModelFile(), options)
            inspectOutputTensor()
            delegateInfo = pending   // Interpreter 초기화 성공 후 확정
            Log.d(TAG, "Interpreter 초기화 완료 — delegate=$delegateInfo attrs=$numAttributes dets=$numDetections attrsFirst=$attrsFirst")
        }.onFailure { e ->
            Log.e(TAG, "Interpreter 초기화 실패: ${e.message}")
        }
    }

    /** 출력 텐서의 shape을 읽어 파싱 방향을 결정 */
    private fun inspectOutputTensor() {
        val interp = interpreter ?: return
        val shape = interp.getOutputTensor(0).shape()   // e.g. [1, 84, 8400]
        if (shape.size < 3) {
            Log.e(TAG, "출력 텐서 shape 이상: ${shape.contentToString()}")
            return
        }
        val dim1 = shape[1]
        val dim2 = shape[2]
        // YOLOv8: attrs(4+nc) < num_det 이 일반적
        if (dim1 < dim2) {
            attrsFirst = true
            numAttributes = dim1
            numDetections = dim2
        } else {
            attrsFirst = false
            numAttributes = dim2
            numDetections = dim1
        }
        Log.d(TAG, "출력 shape: ${shape.contentToString()}, attrsFirst=$attrsFirst")
    }

    fun detect(bitmap: android.graphics.Bitmap): List<DetectionResult> {
        val interp = interpreter ?: return emptyList()
        if (numAttributes == 0 || numDetections == 0) return emptyList()

        preprocessBitmap(bitmap)

        // 출력 버퍼: [1, dim1, dim2]
        val (d1, d2) = if (attrsFirst) numAttributes to numDetections
                       else numDetections to numAttributes
        val output = Array(1) { Array(d1) { FloatArray(d2) } }

        runCatching {
            interp.run(inputBuffer, output)
        }.onFailure { e ->
            Log.e(TAG, "추론 실패: ${e.message}")
            return emptyList()
        }

        val results = mutableListOf<DetectionResult>()
        val numClasses = numAttributes - 4

        for (i in 0 until numDetections) {
            val cx: Float
            val cy: Float
            val w: Float
            val h: Float
            val classScores: FloatArray

            if (attrsFirst) {
                // output[0][attr][det]
                cx = output[0][0][i]
                cy = output[0][1][i]
                w  = output[0][2][i]
                h  = output[0][3][i]
                classScores = FloatArray(numClasses) { c -> output[0][4 + c][i] }
            } else {
                // output[0][det][attr]
                cx = output[0][i][0]
                cy = output[0][i][1]
                w  = output[0][i][2]
                h  = output[0][i][3]
                classScores = FloatArray(numClasses) { c -> output[0][i][4 + c] }
            }

            val maxScore = classScores.maxOrNull() ?: continue
            if (maxScore < confThreshold) continue

            val classId = classScores.indexOfFirst { it == maxScore }
            val label = labels.getOrElse(classId) { "class_$classId" }

            // cx,cy,w,h → left,top,right,bottom (0..1 정규화)
            val left   = (cx - w / 2f).coerceIn(0f, 1f)
            val top    = (cy - h / 2f).coerceIn(0f, 1f)
            val right  = (cx + w / 2f).coerceIn(0f, 1f)
            val bottom = (cy + h / 2f).coerceIn(0f, 1f)

            results.add(
                DetectionResult(
                    label = label,
                    confidence = maxScore,
                    boundingBox = RectF(left, top, right, bottom),
                )
            )
        }

        return nms(results).take(topK)
    }

    /** IoU 기반 Non-Maximum Suppression */
    private fun nms(detections: List<DetectionResult>): List<DetectionResult> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<DetectionResult>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept.add(best)
            sorted.removeAll { iou(best.boundingBox, it.boundingBox) >= iouThreshold }
        }
        return kept
    }

    private fun iou(a: RectF, b: RectF): Float {
        val interLeft   = maxOf(a.left, b.left)
        val interTop    = maxOf(a.top, b.top)
        val interRight  = minOf(a.right, b.right)
        val interBottom = minOf(a.bottom, b.bottom)
        val interArea = maxOf(0f, interRight - interLeft) * maxOf(0f, interBottom - interTop)
        val unionArea = a.width() * a.height() + b.width() * b.height() - interArea
        return if (unionArea <= 0f) 0f else interArea / unionArea
    }

    private fun preprocessBitmap(bitmap: android.graphics.Bitmap) {
        // Center crop to square first (aspect ratio 유지), then resize to INPUT_SIZE
        val crop = minOf(bitmap.width, bitmap.height)
        val cropLeft = (bitmap.width - crop) / 2
        val cropTop  = (bitmap.height - crop) / 2
        val cropped  = android.graphics.Bitmap.createBitmap(bitmap, cropLeft, cropTop, crop, crop)
        val scaled   = android.graphics.Bitmap.createScaledBitmap(cropped, INPUT_SIZE, INPUT_SIZE, true)
        if (cropped !== bitmap) cropped.recycle()

        inputBuffer.rewind()
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }
        if (scaled !== cropped) scaled.recycle()
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fd = context.assets.openFd(modelFileName)
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength,
        )
    }

    private fun loadLabels(): List<String> =
        runCatching {
            context.assets.open(labelFileName)
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
        }.getOrElse {
            Log.w(TAG, "라벨 파일 없음, class_N 으로 대체: ${it.message}")
            emptyList()
        }

    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        nnApiDelegate?.close()
        interpreter = null
        gpuDelegate = null
        nnApiDelegate = null
    }
}
