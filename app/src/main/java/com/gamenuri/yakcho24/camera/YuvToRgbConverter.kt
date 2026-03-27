package com.gamenuri.yakcho24.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * CameraX ImageProxy(YUV_420_888)를 RGB Bitmap으로 변환한다.
 *
 * RenderScript는 API 31에서 deprecated 되었으나 minSdk 24 지원을 위해 사용.
 * API 31+ 환경에서는 추후 HardwareBufferRenderer 또는 ML Kit 내장 변환으로 교체 권장.
 *
 * 사용법:
 *   val converter = YuvToRgbConverter(context)
 *   val bitmap = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
 *   converter.yuvToRgb(imageProxy, bitmap)
 */
@Suppress("DEPRECATION")
class YuvToRgbConverter(context: Context) {

    private val rs: RenderScript = RenderScript.create(context)
    private val scriptYuvToRgb: ScriptIntrinsicYuvToRGB =
        ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    private var yuvBuffer: ByteBuffer? = null
    private var inputAllocation: Allocation? = null
    private var outputAllocation: Allocation? = null

    /**
     * [imageProxy]의 YUV 데이터를 [output] Bitmap에 RGB로 변환해 저장한다.
     * [output]은 반드시 imageProxy와 동일한 width/height, ARGB_8888 포맷이어야 한다.
     */
    @Synchronized
    fun yuvToRgb(imageProxy: ImageProxy, output: Bitmap) {
        require(output.config == Bitmap.Config.ARGB_8888) {
            "output Bitmap must use ARGB_8888 config"
        }
        require(imageProxy.format == ImageFormat.YUV_420_888) {
            "imageProxy format must be YUV_420_888 (actual: ${imageProxy.format})"
        }

        val yuvBytes = imageProxyToByteArray(imageProxy)

        // Allocation 재사용: 크기가 바뀌었을 때만 재생성
        if (inputAllocation == null || yuvBuffer?.capacity() != yuvBytes.size) {
            val yuvType = Type.Builder(rs, Element.U8(rs))
                .setX(yuvBytes.size)
                .create()
            inputAllocation = Allocation.createTyped(rs, yuvType, Allocation.USAGE_SCRIPT)

            val rgbType = Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(imageProxy.width)
                .setY(imageProxy.height)
                .create()
            outputAllocation = Allocation.createTyped(rs, rgbType, Allocation.USAGE_SCRIPT)
        }

        inputAllocation!!.copyFrom(yuvBytes)
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation!!.copyTo(output)
    }

    /**
     * ImageProxy의 YUV_420_888 플레인(Y, U, V)을 단일 ByteArray(NV21 레이아웃)로 변환한다.
     *
     * CameraX는 플레인별 pixelStride/rowStride가 다를 수 있으므로
     * 각 플레인을 직접 순회해 올바르게 패킹한다.
     */
    private fun imageProxyToByteArray(imageProxy: ImageProxy): ByteArray {
        val width = imageProxy.width
        val height = imageProxy.height

        val planeY = imageProxy.planes[0]
        val planeU = imageProxy.planes[1]
        val planeV = imageProxy.planes[2]

        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        // Y 플레인 복사
        copyPlane(
            src = planeY.buffer,
            dst = nv21,
            dstOffset = 0,
            width = width,
            height = height,
            rowStride = planeY.rowStride,
            pixelStride = planeY.pixelStride,
        )

        // VU 인터리브 복사 (NV21: V 먼저, U 나중)
        val uvRowStride = planeU.rowStride
        val uvPixelStride = planeU.pixelStride
        var dstIdx = ySize
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val uvOffset = row * uvRowStride + col * uvPixelStride
                nv21[dstIdx++] = planeV.buffer[uvOffset] // V
                nv21[dstIdx++] = planeU.buffer[uvOffset] // U
            }
        }

        return nv21
    }

    private fun copyPlane(
        src: ByteBuffer,
        dst: ByteArray,
        dstOffset: Int,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
    ) {
        var dstIdx = dstOffset
        for (row in 0 until height) {
            val rowOffset = row * rowStride
            for (col in 0 until width) {
                dst[dstIdx++] = src[rowOffset + col * pixelStride]
            }
        }
    }

    fun release() {
        inputAllocation?.destroy()
        outputAllocation?.destroy()
        scriptYuvToRgb.destroy()
        rs.destroy()
        inputAllocation = null
        outputAllocation = null
    }
}