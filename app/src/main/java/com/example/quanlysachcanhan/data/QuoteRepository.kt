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
            "${DatabaseHelper.COL_QUOTE_CREATED_AT} DESC"
        ).use { cursor ->
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
                    createdAt = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUOTE_CREATED_AT)
                    )
                )
            }
        }

        return result
    }
}
