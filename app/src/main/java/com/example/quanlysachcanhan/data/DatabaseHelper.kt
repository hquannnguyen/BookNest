package com.example.quanlysachcanhan.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_BOOKS_TABLE)
        db.execSQL(CREATE_QUOTES_TABLE)
        createIndexes(db)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Base project: hiện mới có version 1.
        // Khi thay đổi schema, KHÔNG drop table nếu muốn giữ dữ liệu.
        // Hãy viết ALTER TABLE / migration theo từng version.
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_books_title ON $TABLE_BOOKS($COL_BOOK_TITLE)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_books_author ON $TABLE_BOOKS($COL_BOOK_AUTHOR)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_books_category ON $TABLE_BOOKS($COL_BOOK_CATEGORY)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_books_status ON $TABLE_BOOKS($COL_BOOK_STATUS)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_quotes_book_id ON $TABLE_QUOTES($COL_QUOTE_BOOK_ID)"
        )
    }

    companion object {
        const val DATABASE_NAME = "personal_books.db"
        const val DATABASE_VERSION = 1

        const val TABLE_BOOKS = "books"
        const val COL_BOOK_ID = "id"
        const val COL_BOOK_TITLE = "title"
        const val COL_BOOK_AUTHOR = "author"
        const val COL_BOOK_CATEGORY = "category"
        const val COL_BOOK_RATING = "rating"
        const val COL_BOOK_NOTE = "note"
        const val COL_BOOK_STATUS = "reading_status"
        const val COL_BOOK_COVER = "cover_image_path"
        const val COL_BOOK_CREATED_AT = "created_at"
        const val COL_BOOK_UPDATED_AT = "updated_at"
        const val COL_BOOK_FINISHED_AT = "finished_at"

        const val TABLE_QUOTES = "quotes"
        const val COL_QUOTE_ID = "id"
        const val COL_QUOTE_BOOK_ID = "book_id"
        const val COL_QUOTE_CONTENT = "content"
        const val COL_QUOTE_CREATED_AT = "created_at"

        private const val CREATE_BOOKS_TABLE = """
            CREATE TABLE $TABLE_BOOKS (
                $COL_BOOK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BOOK_TITLE TEXT NOT NULL,
                $COL_BOOK_AUTHOR TEXT NOT NULL,
                $COL_BOOK_CATEGORY TEXT NOT NULL,
                $COL_BOOK_RATING REAL NOT NULL DEFAULT 0,
                $COL_BOOK_NOTE TEXT NOT NULL DEFAULT '',
                $COL_BOOK_STATUS TEXT NOT NULL,
                $COL_BOOK_COVER TEXT,
                $COL_BOOK_CREATED_AT INTEGER NOT NULL,
                $COL_BOOK_UPDATED_AT INTEGER,
                $COL_BOOK_FINISHED_AT INTEGER
            )
        """

        private const val CREATE_QUOTES_TABLE = """
            CREATE TABLE $TABLE_QUOTES (
                $COL_QUOTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_QUOTE_BOOK_ID INTEGER NOT NULL,
                $COL_QUOTE_CONTENT TEXT NOT NULL,
                $COL_QUOTE_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_QUOTE_BOOK_ID)
                    REFERENCES $TABLE_BOOKS($COL_BOOK_ID)
                    ON DELETE CASCADE
            )
        """
    }
}
