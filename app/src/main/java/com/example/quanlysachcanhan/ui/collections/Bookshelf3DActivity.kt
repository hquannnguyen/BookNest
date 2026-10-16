package com.example.quanlysachcanhan.ui.collections

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.data.CollectionRepository
import com.example.quanlysachcanhan.databinding.ActivityBookshelf3dBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.ui.detail.BookDetailActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.NavigationHelper
import java.util.concurrent.Executors

class Bookshelf3DActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookshelf3dBinding
    private val worker = Executors.newSingleThreadExecutor()
    private var books = emptyList<Book>()
    private var collectionId = 0L
    private var page = 0
    private var generation = 0
    private var loading = false
    private val pageCount get() = ((books.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookshelf3dBinding.inflate(layoutInflater)
        setContentView(binding.root)
        collectionId = intent.getLongExtra(CollectionDetailActivity.EXTRA_COLLECTION_ID, 0L)
        if (collectionId <= 0L) {
            finish()
            return
        }
        page = savedInstanceState?.getInt("page") ?: 0
        binding.bookshelf.restoreCamera(savedInstanceState?.getFloat("yaw") ?: -0.18f,
            savedInstanceState?.getFloat("pitch") ?: 0.14f,
            savedInstanceState?.getFloat("zoom") ?: 1f)
        binding.btnBack.setOnClickListener { NavigationHelper.finishWithSlide(this) }
        onBackPressedDispatcher.addCallback(this) { NavigationHelper.finishWithSlide(this@Bookshelf3DActivity) }
        binding.bookshelf.onBookClick = ::openBook
        binding.btnZoomIn.setOnClickListener { binding.bookshelf.changeZoom(1.12f) }
        binding.btnZoomOut.setOnClickListener { binding.bookshelf.changeZoom(1f / 1.12f) }
        binding.btnReset.setOnClickListener { binding.bookshelf.resetCamera() }
        binding.btnPrevious.setOnClickListener { if (!loading && page > 0) { page--; showPage() } }
        binding.btnNext.setOnClickListener { if (!loading && page + 1 < pageCount) { page++; showPage() } }
        binding.btnChooseBook.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.bookshelf_choose)
                .setItems(books.map { "${it.title} — ${it.author}" }.toTypedArray()) { _, index ->
                    openBook(books[index].id)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (collectionId <= 0L) return
        val request = ++generation
        setLoading()
        worker.execute {
            val result = runCatching {
                CollectionRepository(applicationContext).use { repository ->
                    val collection = repository.getById(collectionId)
                    collection to if (collection != null) repository.getBooksInCollection(collectionId) else emptyList()
                }
            }
            runOnUiThread {
                if (isDestroyed || isFinishing || request != generation) return@runOnUiThread
                result.fold(onSuccess = { (collection, items) ->
                    books = items
                    binding.tvTitle.text = collection?.name ?: getString(R.string.bookshelf_open)
                    if (collection == null) showError(R.string.bookshelf_missing)
                    else {
                        page = page.coerceIn(0, pageCount - 1)
                        showPage()
                    }
                }, onFailure = { showError(R.string.bookshelf_error) })
            }
        }
    }

    private fun showPage() {
        val request = ++generation
        val visibleBooks = books.drop(page * PAGE_SIZE).take(PAGE_SIZE)
        setLoading()
        worker.execute {
            val result = runCatching {
                visibleBooks.map { Bookshelf3DView.Entry(it, makeCover(it)) }
            }
            runOnUiThread {
                if (isDestroyed || isFinishing || request != generation) return@runOnUiThread
                result.fold(onSuccess = { entries ->
                    loading = false
                    binding.bookshelf.setEntries(entries)
                    binding.tvState.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
                    binding.tvState.setText(R.string.bookshelf_empty)
                    updateControls()
                }, onFailure = { showError(R.string.bookshelf_error) })
            }
        }
    }

    private fun setLoading() {
        loading = true
        binding.tvState.setText(R.string.bookshelf_loading)
        binding.tvState.visibility = View.VISIBLE
        // Discard old picking regions while the replacement textures are loading.
        binding.bookshelf.setEntries(emptyList())
        updateControls()
    }

    private fun showError(message: Int) {
        loading = false
        books = emptyList()
        page = 0
        binding.bookshelf.setEntries(emptyList())
        binding.tvState.setText(message)
        binding.tvState.visibility = View.VISIBLE
        updateControls()
    }

    private fun updateControls() {
        binding.btnPrevious.isEnabled = !loading && page > 0
        binding.btnNext.isEnabled = !loading && page + 1 < pageCount
        binding.btnPrevious.alpha = if (binding.btnPrevious.isEnabled) 1f else 0.3f
        binding.btnNext.alpha = if (binding.btnNext.isEnabled) 1f else 0.3f
        binding.btnChooseBook.isEnabled = !loading && books.isNotEmpty()
        binding.tvPage.text = getString(R.string.bookshelf_page, page + 1, pageCount, books.size)
    }

    private fun openBook(id: Long) {
        if (loading || books.none { it.id == id }) return
        NavigationHelper.startActivitySlide(this, Intent(this, BookDetailActivity::class.java).apply {
            putExtra(Constants.Extras.BOOK_ID, id)
        })
    }

    /** Only six small textures exist per page. Decode off the UI thread, sampled
     * before allocation, so a high resolution camera image does not exhaust RAM. */
    private fun makeCover(book: Book): Bitmap {
        val bitmap = Bitmap.createBitmap(256, 360, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val colors = intArrayOf(0xFF25495C.toInt(), 0xFF74503D.toInt(), 0xFF52624A.toInt(), 0xFF654D70.toInt())
        canvas.drawColor(colors[(book.id % colors.size).toInt().coerceAtLeast(0)])
        book.coverImagePath?.takeIf { it.isNotBlank() }?.let { path ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 768 || bounds.outHeight / inSampleSize > 1080) {
                        inSampleSize *= 2
                    }
                }
                BitmapFactory.decodeFile(path, options)?.let { image ->
                    val scale = maxOf(256f / image.width, 360f / image.height)
                    val w = image.width * scale; val h = image.height * scale
                    canvas.drawBitmap(image, null, RectF((256 - w) / 2, (360 - h) / 2,
                        (256 + w) / 2, (360 + h) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
                    image.recycle()
                }
            }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 190f, 0f, 360f,
            intArrayOf(Color.TRANSPARENT, 0xEF000000.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 190f, 256f, 360f, paint)
        paint.shader = null
        paint.color = 0x40000000
        canvas.drawRect(0f, 0f, 10f, 360f, paint)
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 21f; typeface = Typeface.DEFAULT_BOLD
        }
        val title = StaticLayout.Builder.obtain(book.title, 0, book.title.length, text, 220)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END).setIncludePad(false).build()
        canvas.save()
        canvas.translate(18f, 275f)
        title.draw(canvas)
        canvas.restore()
        text.textSize = 14f; text.typeface = Typeface.DEFAULT
        canvas.drawText(TextUtils.ellipsize(book.author, text, 220f, TextUtils.TruncateAt.END).toString(),
            18f, 344f, text)
        return bitmap
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("page", page)
        outState.putFloat("yaw", binding.bookshelf.yaw)
        outState.putFloat("pitch", binding.bookshelf.pitch)
        outState.putFloat("zoom", binding.bookshelf.zoom)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        generation++
        worker.shutdownNow()
        super.onDestroy()
    }

    companion object { private const val PAGE_SIZE = 6 }
}
