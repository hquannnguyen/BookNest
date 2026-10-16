package com.example.quanlysachcanhan.ui.collections

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.model.Book
import kotlin.math.*

/** Software 3D: world vertices -> camera rotation -> perspective projection.
 * Faces are depth-sorted, and the same visible polygons are used for picking.
 * No network, WebView, camera, or external rendering service is required.
 */
class Bookshelf3DView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    data class Entry(val book: Book, val cover: Bitmap)
    private data class V3(val x: Float, val y: Float, val z: Float)
    private data class Face(val vertices: List<V3>, val color: Int,
                            val cover: Bitmap? = null, val bookId: Long? = null)
    private data class Hit(val path: Path, val bookId: Long?)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()
    private val hitRegions = mutableListOf<Hit>()
    private var entries = emptyList<Entry>()
    var onBookClick: ((Long) -> Unit)? = null
    var yaw = -0.18f
        private set
    var pitch = 0.14f
        private set
    var zoom = 1f
        private set
    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private var tapId: Long? = null
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                moved = true
                changeZoom(detector.scaleFactor)
                return true
            }
        })

    fun setEntries(value: List<Entry>) {
        entries = value
        hitRegions.clear()
        invalidate()
    }

    fun restoreCamera(yaw: Float, pitch: Float, zoom: Float) {
        this.yaw = yaw.coerceIn(-0.65f, 0.65f)
        this.pitch = pitch.coerceIn(-0.18f, 0.40f)
        this.zoom = zoom.coerceIn(0.7f, 1.7f)
        invalidate()
    }

    fun resetCamera() = restoreCamera(-0.18f, 0.14f, 1f)
    fun changeZoom(factor: Float) = restoreCamera(yaw, pitch, zoom * factor)

    private fun rotate(v: V3): V3 {
        val x = v.x * cos(yaw) + v.z * sin(yaw)
        val z = -v.x * sin(yaw) + v.z * cos(yaw)
        return V3(x, v.y * cos(pitch) - z * sin(pitch),
            v.y * sin(pitch) + z * cos(pitch))
    }

    private fun project(v: V3): PointF {
        val scale = min(width / 4.9f, height / 4.0f) * zoom
        val perspective = 6f / (6f - v.z)
        return PointF(width / 2f + v.x * scale * perspective,
            height / 2f - v.y * scale * perspective)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(ContextCompat.getColor(context, R.color.surface_container_low))
        val faces = mutableListOf<Face>()
        // Back panel, side posts and three horizontal boards form a real solid shelf.
        box(faces, -1.98f, -1.65f, -0.38f, 1.98f, 1.65f, -0.28f, 0xFF765033.toInt())
        box(faces, -2.08f, -1.72f, -0.38f, -1.94f, 1.72f, 0.48f, 0xFFAD794D.toInt())
        box(faces, 1.94f, -1.72f, -0.38f, 2.08f, 1.72f, 0.48f, 0xFFAD794D.toInt())
        listOf(-1.58f, -0.08f, 1.52f).forEach { y ->
            box(faces, -1.97f, y - 0.12f, -0.38f, 1.97f, y, 0.52f, 0xFFC18C58.toInt())
        }
        entries.take(6).forEachIndexed { i, entry ->
            val x = (i % 3 - 1) * 1.22f
            val bottom = if (i < 3) -0.08f else -1.58f
            val height = 1.10f + (i % 3) * 0.07f
            box(faces, x - 0.44f, bottom, 0.02f, x + 0.44f,
                bottom + height, 0.30f, 0xFFEEE4D3.toInt(), entry.cover, entry.book.id)
        }
        hitRegions.clear()
        faces.map { it.copy(vertices = it.vertices.map(::rotate)) }
            .sortedBy { face -> face.vertices.sumOf { it.z.toDouble() } / 4 }
            .forEach { face -> drawFace(canvas, face) }
    }

    private fun box(faces: MutableList<Face>, x0: Float, y0: Float, z0: Float,
                    x1: Float, y1: Float, z1: Float, color: Int,
                    cover: Bitmap? = null, bookId: Long? = null) {
        val a = V3(x0, y0, z0); val b = V3(x1, y0, z0)
        val c = V3(x1, y1, z0); val d = V3(x0, y1, z0)
        val e = V3(x0, y0, z1); val f = V3(x1, y0, z1)
        val g = V3(x1, y1, z1); val h = V3(x0, y1, z1)
        faces += Face(listOf(c, d, a, b), shade(color, 0.6f), bookId = bookId)
        faces += Face(listOf(d, h, e, a), shade(color, 0.72f), bookId = bookId)
        faces += Face(listOf(g, c, b, f), shade(color, 0.85f), bookId = bookId)
        faces += Face(listOf(e, f, b, a), shade(color, 0.65f), bookId = bookId)
        faces += Face(listOf(d, c, g, h), shade(color, 1.08f), bookId = bookId)
        // Texture corner order: top-left, top-right, bottom-right, bottom-left.
        faces += Face(listOf(h, g, f, e), color, cover, bookId)
    }

    private fun shade(color: Int, amount: Float) = Color.rgb(
        (Color.red(color) * amount).toInt().coerceIn(0, 255),
        (Color.green(color) * amount).toInt().coerceIn(0, 255),
        (Color.blue(color) * amount).toInt().coerceIn(0, 255))

    private fun drawFace(canvas: Canvas, face: Face) {
        val points = face.vertices.map(::project)
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        paint.style = Paint.Style.FILL
        paint.color = face.color
        canvas.drawPath(path, paint)
        face.cover?.let { bitmap ->
            val src = floatArrayOf(0f, 0f, bitmap.width.toFloat(), 0f,
                bitmap.width.toFloat(), bitmap.height.toFloat(), 0f, bitmap.height.toFloat())
            val dst = points.flatMap { listOf(it.x, it.y) }.toFloatArray()
            matrix.reset()
            if (matrix.setPolyToPoly(src, 0, dst, 0, 4)) {
                canvas.save()
                canvas.clipPath(path)
                canvas.drawBitmap(bitmap, matrix, paint)
                canvas.restore()
            }
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = resources.displayMetrics.density * 0.6f
        paint.color = 0x35000000
        canvas.drawPath(path, paint)
        // Include shelf faces so a board hiding a book also blocks taps on it.
        hitRegions += Hit(path, face.bookId)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastX = event.x; downX = event.x
                lastY = event.y; downY = event.y
                moved = false
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> moved = true
            MotionEvent.ACTION_POINTER_UP -> {
                val remaining = if (event.actionIndex == 0) 1 else 0
                lastX = event.getX(remaining); lastY = event.getY(remaining)
            }
            MotionEvent.ACTION_MOVE -> {
                if (hypot(event.x - downX, event.y - downY) > slop) moved = true
                if (!scaleDetector.isInProgress && event.pointerCount == 1 && moved) {
                    restoreCamera(yaw + (event.x - lastX) / max(width, 1) * 1.8f,
                        pitch + (event.y - lastY) / max(height, 1) * 1.2f, zoom)
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (!moved) {
                    val clip = Region(0, 0, width, height)
                    tapId = hitRegions.asReversed().firstOrNull {
                        Region().apply { setPath(it.path, clip) }.contains(event.x.toInt(), event.y.toInt())
                    }?.bookId
                    performClick()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                moved = true
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        tapId?.let { onBookClick?.invoke(it) }
        tapId = null
        return true
    }
}
