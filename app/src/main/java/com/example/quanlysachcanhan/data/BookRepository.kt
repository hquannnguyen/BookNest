package com.example.quanlysachcanhan.data

import android.content.ContentValues
import android.content.Context
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.utils.Constants
import java.util.Calendar

class BookRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    fun insert(book: Book): Long {
        val db = dbHelper.writableDatabase
        return db.insert(
            DatabaseHelper.TABLE_BOOKS,
            null,
            book.toContentValues()
        )
    }

    fun update(book: Book): Int {
        val db = dbHelper.writableDatabase

        val values = book.copy(
            updatedAt = System.currentTimeMillis()
        ).toContentValues()

        return db.update(
            DatabaseHelper.TABLE_BOOKS,
            values,
            "${DatabaseHelper.COL_BOOK_ID} = ?",
            arrayOf(book.id.toString())
        )
    }

    fun delete(bookId: Long): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            DatabaseHelper.TABLE_BOOKS,
            "${DatabaseHelper.COL_BOOK_ID} = ?",
            arrayOf(bookId.toString())
        )
    }

    fun getById(bookId: Long): Book? {
        val db = dbHelper.readableDatabase

        db.query(
            DatabaseHelper.TABLE_BOOKS,
            null,
            "${DatabaseHelper.COL_BOOK_ID} = ?",
            arrayOf(bookId.toString()),
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.toBook()
            } else {
                null
            }
        }
    }

    /**
     * Query dùng chung cho danh sách:
     * - search theo title hoặc author
     * - filter category
     * - filter status
     * - sort
     */
    fun getBooks(
        keyword: String? = null,
        category: String? = null,
        status: String? = null,
        sortBy: String = Constants.Sort.TITLE
    ): List<Book> {
        val db = dbHelper.readableDatabase

        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (!keyword.isNullOrBlank()) {
            selectionParts +=
                "(${DatabaseHelper.COL_BOOK_TITLE} LIKE ? OR ${DatabaseHelper.COL_BOOK_AUTHOR} LIKE ?)"
            val pattern = "%${keyword.trim()}%"
            selectionArgs += pattern
            selectionArgs += pattern
        }

        if (!category.isNullOrBlank()) {
            selectionParts += "${DatabaseHelper.COL_BOOK_CATEGORY} = ?"
            selectionArgs += category
        }

        if (!status.isNullOrBlank()) {
            selectionParts += "${DatabaseHelper.COL_BOOK_STATUS} = ?"
            selectionArgs += status
        }

        val selection = selectionParts
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" AND ")

        val orderBy = when (sortBy) {
            Constants.Sort.AUTHOR ->
                "${DatabaseHelper.COL_BOOK_AUTHOR} COLLATE NOCASE ASC"
            Constants.Sort.RATING ->
                "${DatabaseHelper.COL_BOOK_RATING} DESC, ${DatabaseHelper.COL_BOOK_TITLE} COLLATE NOCASE ASC"
            Constants.Sort.STATUS ->
                "${DatabaseHelper.COL_BOOK_STATUS} ASC, ${DatabaseHelper.COL_BOOK_TITLE} COLLATE NOCASE ASC"
            else ->
                "${DatabaseHelper.COL_BOOK_TITLE} COLLATE NOCASE ASC"
        }

        val result = mutableListOf<Book>()

        db.query(
            DatabaseHelper.TABLE_BOOKS,
            null,
            selection,
            selectionArgs.takeIf { it.isNotEmpty() }?.toTypedArray(),
            null,
            null,
            orderBy
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += cursor.toBook()
            }
        }

        return result
    }

    fun countAll(): Int {
        return countByWhere(null, null)
    }

    fun countByCategory(category: String): Int {
        return countByWhere(
            "${DatabaseHelper.COL_BOOK_CATEGORY} = ?",
            arrayOf(category)
        )
    }

    fun countByStatus(status: String): Int {
        return countByWhere(
            "${DatabaseHelper.COL_BOOK_STATUS} = ?",
            arrayOf(status)
        )
    }

    fun countReadBooksInYear(year: Int): Int {
        val start = Calendar.getInstance().apply {
            set(year, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val end = Calendar.getInstance().apply {
            set(year + 1, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return countByWhere(
            "${DatabaseHelper.COL_BOOK_STATUS} = ? " +
                "AND ${DatabaseHelper.COL_BOOK_FINISHED_AT} >= ? " +
                "AND ${DatabaseHelper.COL_BOOK_FINISHED_AT} < ?",
            arrayOf(
                Constants.ReadingStatus.READ,
                start.toString(),
                end.toString()
            )
        )
    }

    private fun countByWhere(
        selection: String?,
        args: Array<String>?
    ): Int {
        val db = dbHelper.readableDatabase

        db.query(
            DatabaseHelper.TABLE_BOOKS,
            arrayOf("COUNT(*) AS total"),
            selection,
            args,
            null,
            null,
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.getInt(cursor.getColumnIndexOrThrow("total"))
            } else {
                0
            }
        }
    }

    private fun Book.toContentValues() = ContentValues().apply {
        put(DatabaseHelper.COL_BOOK_TITLE, title)
        put(DatabaseHelper.COL_BOOK_AUTHOR, author)
        put(DatabaseHelper.COL_BOOK_CATEGORY, category)
        put(DatabaseHelper.COL_BOOK_RATING, rating)
        put(DatabaseHelper.COL_BOOK_NOTE, note)
        put(DatabaseHelper.COL_BOOK_STATUS, readingStatus)
        put(DatabaseHelper.COL_BOOK_COVER, coverImagePath)
        put(DatabaseHelper.COL_BOOK_CREATED_AT, createdAt)

        if (updatedAt != null) {
            put(DatabaseHelper.COL_BOOK_UPDATED_AT, updatedAt)
        } else {
            putNull(DatabaseHelper.COL_BOOK_UPDATED_AT)
        }

        if (finishedAt != null) {
            put(DatabaseHelper.COL_BOOK_FINISHED_AT, finishedAt)
        } else {
            putNull(DatabaseHelper.COL_BOOK_FINISHED_AT)
        }
    }

    private fun android.database.Cursor.toBook(): Book {
        val updatedIndex =
            getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_UPDATED_AT)
        val finishedIndex =
            getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_FINISHED_AT)

        return Book(
            id = getLong(getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_ID)),
            title = getString(getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_TITLE)),
            author = getString(getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_AUTHOR)),
            category = getString(getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_CATEGORY)),
            rating = getFloat(getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_RATING)),
            note = getString(getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_NOTE)),
            readingStatus = getString(getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_STATUS)),
            coverImagePath = getString(getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_COVER)),
            createdAt = getLong(getColumnIndexOrThrow(DatabaseHelper.COL_BOOK_CREATED_AT)),
            updatedAt = if (isNull(updatedIndex)) null else getLong(updatedIndex),
            finishedAt = if (isNull(finishedIndex)) null else getLong(finishedIndex)
        )
    }
}
