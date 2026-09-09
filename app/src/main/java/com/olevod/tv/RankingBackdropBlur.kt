package com.olevod.tv

import android.graphics.Bitmap
import androidx.core.graphics.scale
import coil.size.Size
import coil.transform.Transformation

/** Small, cached decorative image; works on API 26+ without per-frame GPU blur. */
internal object RankingBackdropBlur : Transformation {
    private const val WIDTH = 160
    private const val HEIGHT = 80
    private const val RADIUS = 8
    private const val PASSES = 2
    override val cacheKey = "ranking-backdrop:$WIDTH:$HEIGHT:$RADIUS:$PASSES:v1"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Stretch only this background copy. The foreground uses the untouched Fit poster.
        val scaled = input.scale(WIDTH, HEIGHT)
        var pixels = IntArray(WIDTH * HEIGHT)
        scaled.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
        repeat(PASSES) {
            pixels = blur(pixels, horizontal = true)
            pixels = blur(pixels, horizontal = false)
        }
        // Never mutate or recycle a bitmap owned by Coil / another image request.
        return Bitmap.createBitmap(pixels, WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
    }

    private fun blur(source: IntArray, horizontal: Boolean): IntArray {
        val target = IntArray(source.size)
        val window = RADIUS * 2 + 1
        val lines = if (horizontal) HEIGHT else WIDTH
        val length = if (horizontal) WIDTH else HEIGHT
        fun offset(line: Int, position: Int): Int =
            if (horizontal) line * WIDTH + position.coerceIn(0, WIDTH - 1)
            else position.coerceIn(0, HEIGHT - 1) * WIDTH + line
        for (line in 0 until lines) {
            var a = 0; var r = 0; var g = 0; var b = 0
            fun accumulate(pixel: Int, sign: Int) {
                a += (pixel ushr 24) * sign
                r += ((pixel ushr 16) and 255) * sign
                g += ((pixel ushr 8) and 255) * sign
                b += (pixel and 255) * sign
            }
            for (position in -RADIUS..RADIUS) accumulate(source[offset(line, position)], 1)
            for (position in 0 until length) {
                target[offset(line, position)] = ((a / window) shl 24) or
                    ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
                accumulate(source[offset(line, position - RADIUS)], -1)
                accumulate(source[offset(line, position + RADIUS + 1)], 1)
            }
        }
        return target
    }
}
