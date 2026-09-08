package com.example.quanlysachcanhan.data

import android.content.ContentValues
import android.content.Context
import com.example.quanlysachcanhan.model.Quote

class QuoteRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    fun insert(quote: Quote): Long {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_QUOTE_BOOK_ID, quote.bookId)
            put(DatabaseHelper.COL_QUOTE_CONTENT, quote.content)
            put(DatabaseHelper.COL_QUOTE_PAGE, quote.page)
            put(DatabaseHelper.COL_QUOTE_CHAPTER, quote.chapter)
            put(DatabaseHelper.COL_QUOTE_TAG, quote.tag)
            put(DatabaseHelper.COL_QUOTE_CREATED_AT, quote.createdAt)
        }

        return dbHelper.writableDatabase.insert(
            DatabaseHelper.TABLE_QUOTES,
            null,
            values
        )
    }

    fun update(quote: Quote): Int {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_QUOTE_CONTENT, quote.content)
            put(DatabaseHelper.COL_QUOTE_PAGE, quote.page)
            put(DatabaseHelper.COL_QUOTE_CHAPTER, quote.chapter)
            put(DatabaseHelper.COL_QUOTE_TAG, quote.tag)
        }

        return dbHelper.writableDatabase.update(
            DatabaseHelper.TABLE_QUOTES,
            values,
            "${DatabaseHelper.COL_QUOTE_ID} = ?",
            arrayOf(quote.id.toString())
        )
    }

    fun delete(quoteId: Long): Int {
        return dbHelper.writableDatabase.delete(
            DatabaseHelper.TABLE_QUOTES,
            "${DatabaseHelper.COL_QUOTE_ID} = ?",
            arrayOf(quoteId.toString())
        )
    }

    fun getByBookId(bookId: Long): List<Quote> {
        val result = mutableListOf<Quote>()

        dbHelper.readableDatabase.query(
            DatabaseHelper.TABLE_QUOTES,
            null,
            "${DatabaseHelper.COL_QUOTE_BOOK_ID} = ?",
            arrayOf(bookId.toString()),
            null,
            null,
            "${DatabaseHelper.COL_QUOTE_CREATED_AT} ASC"
        ).use { cursor ->
            val pageCol = cursor.getColumnIndex(DatabaseHelper.COL_QUOTE_PAGE)
            val chapterCol = cursor.getColumnIndex(DatabaseHelper.COL_QUOTE_CHAPTER)
            val tagCol = cursor.getColumnIndex(DatabaseHelper.COL_QUOTE_TAG)

            while (cursor.moveToNext()) {
                result += Quote(
                    id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_ID)
                    ),
                    bookId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_BOOK_ID)
                    ),
                    content = cursor.getString(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_CONTENT)
                    ),
                    page = if (pageCol >= 0) cursor.getString(pageCol).orEmpty() else "",
                    chapter = if (chapterCol >= 0) cursor.getString(chapterCol).orEmpty() else "",
                    tag = if (tagCol >= 0) cursor.getString(tagCol).orEmpty() else "",
                    createdAt = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_CREATED_AT)
                    )
                )
            }
        }

        return result
    }

    fun countAll(): Int {
        val db = dbHelper.readableDatabase
        db.rawQuery("SELECT COUNT(*) AS total FROM ${DatabaseHelper.TABLE_QUOTES}", null).use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.getInt(cursor.getColumnIndexOrThrow("total"))
            } else {
                0
            }
        }
    }

    fun getRandomQuote(): Quote? {
        val db = dbHelper.readableDatabase
        val pageCol = DatabaseHelper.COL_QUOTE_PAGE
        val chapterCol = DatabaseHelper.COL_QUOTE_CHAPTER
        val tagCol = DatabaseHelper.COL_QUOTE_TAG

        db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_QUOTES} ORDER BY RANDOM() LIMIT 1", null).use { cursor ->
            if (cursor.moveToFirst()) {
                val pIdx = cursor.getColumnIndex(pageCol)
                val cIdx = cursor.getColumnIndex(chapterCol)
                val tIdx = cursor.getColumnIndex(tagCol)
                return Quote(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_ID)),
                    bookId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_BOOK_ID)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_CONTENT)),
                    page = if (pIdx >= 0) cursor.getString(pIdx).orEmpty() else "",
                    chapter = if (cIdx >= 0) cursor.getString(cIdx).orEmpty() else "",
                    tag = if (tIdx >= 0) cursor.getString(tIdx).orEmpty() else "",
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_CREATED_AT))
                )
            }
        }
        return null
    }

    fun getQuotesForBookIds(bookIds: List<Long>): List<Quote> {
        if (bookIds.isEmpty()) return emptyList()
        val db = dbHelper.readableDatabase
        val placeholders = bookIds.joinToString(",") { "?" }
        val args = bookIds.map { it.toString() }.toTypedArray()
        val list = mutableListOf<Quote>()
        db.query(
            DatabaseHelper.TABLE_QUOTES,
            null,
            "${DatabaseHelper.COL_QUOTE_BOOK_ID} IN ($placeholders)",
            args,
            null, null,
            "${DatabaseHelper.COL_QUOTE_CREATED_AT} DESC"
        ).use { cursor ->
            val pageCol = cursor.getColumnIndex(DatabaseHelper.COL_QUOTE_PAGE)
            val chapterCol = cursor.getColumnIndex(DatabaseHelper.COL_QUOTE_CHAPTER)
            val tagCol = cursor.getColumnIndex(DatabaseHelper.COL_QUOTE_TAG)
            while (cursor.moveToNext()) {
                list.add(
                    Quote(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_ID)),
                        bookId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_BOOK_ID)),
                        content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_CONTENT)),
                        page = if (pageCol >= 0) cursor.getString(pageCol).orEmpty() else "",
                        chapter = if (chapterCol >= 0) cursor.getString(chapterCol).orEmpty() else "",
                        tag = if (tagCol >= 0) cursor.getString(tagCol).orEmpty() else "",
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_CREATED_AT))
                    )
                )
            }
        }
        return list
    }

    fun getRandomQuoteForBookIds(bookIds: List<Long>): Quote? {
        if (bookIds.isEmpty()) return null
        val db = dbHelper.readableDatabase
        val placeholders = bookIds.joinToString(",") { "?" }
        val args = bookIds.map { it.toString() }.toTypedArray()
        val pageCol = DatabaseHelper.COL_QUOTE_PAGE
        val chapterCol = DatabaseHelper.COL_QUOTE_CHAPTER
        val tagCol = DatabaseHelper.COL_QUOTE_TAG

        db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_QUOTES} WHERE ${DatabaseHelper.COL_QUOTE_BOOK_ID} IN ($placeholders) ORDER BY RANDOM() LIMIT 1",
            args
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val pIdx = cursor.getColumnIndex(pageCol)
                val cIdx = cursor.getColumnIndex(chapterCol)
                val tIdx = cursor.getColumnIndex(tagCol)
                return Quote(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_ID)),
                    bookId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_BOOK_ID)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_CONTENT)),
                    page = if (pIdx >= 0) cursor.getString(pIdx).orEmpty() else "",
                    chapter = if (cIdx >= 0) cursor.getString(cIdx).orEmpty() else "",
                    tag = if (tIdx >= 0) cursor.getString(tIdx).orEmpty() else "",
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_CREATED_AT))
                )
            }
        }
        return null
    }

    fun getQuoteCountPerBook(): Map<Long, Int> {
        val db = dbHelper.readableDatabase
        val map = mutableMapOf<Long, Int>()
        db.rawQuery(
            "SELECT ${DatabaseHelper.COL_QUOTE_BOOK_ID}, COUNT(*) AS total FROM ${DatabaseHelper.TABLE_QUOTES} GROUP BY ${DatabaseHelper.COL_QUOTE_BOOK_ID}",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                map[cursor.getLong(0)] = cursor.getInt(1)
            }
        }
        return map
    }
}
