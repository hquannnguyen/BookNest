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
        db.execSQL(CREATE_COLLECTIONS_TABLE)
        db.execSQL(CREATE_COLLECTION_BOOKS_TABLE)
        createIndexes(db)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE $TABLE_BOOKS ADD COLUMN $COL_BOOK_CURRENT_PAGE INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE $TABLE_BOOKS ADD COLUMN $COL_BOOK_TOTAL_PAGES INTEGER NOT NULL DEFAULT 0"
            )
        }
        if (oldVersion < 3) {
            db.execSQL(
                "ALTER TABLE $TABLE_QUOTES ADD COLUMN $COL_QUOTE_PAGE TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE $TABLE_QUOTES ADD COLUMN $COL_QUOTE_CHAPTER TEXT NOT NULL DEFAULT ''"
            )
            db.execSQL(
                "ALTER TABLE $TABLE_QUOTES ADD COLUMN $COL_QUOTE_TAG TEXT NOT NULL DEFAULT ''"
            )
        }
        if (oldVersion < 4) {
            db.execSQL(CREATE_COLLECTIONS_TABLE)
            db.execSQL(CREATE_COLLECTION_BOOKS_TABLE)
            createCollectionIndexes(db)
        }
        if (oldVersion < 5) {
            db.execSQL(
                "ALTER TABLE $TABLE_BOOKS ADD COLUMN $COL_BOOK_PUBLISHER TEXT NOT NULL DEFAULT ''"
            )
        }
    }

    private fun createIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_books_title ON $TABLE_BOOKS($COL_BOOK_TITLE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_books_author ON $TABLE_BOOKS($COL_BOOK_AUTHOR)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_books_category ON $TABLE_BOOKS($COL_BOOK_CATEGORY)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_books_status ON $TABLE_BOOKS($COL_BOOK_STATUS)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_quotes_book_id ON $TABLE_QUOTES($COL_QUOTE_BOOK_ID)")
        createCollectionIndexes(db)
    }

    private fun createCollectionIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cb_collection_id ON $TABLE_COLLECTION_BOOKS($COL_CB_COLLECTION_ID)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cb_book_id ON $TABLE_COLLECTION_BOOKS($COL_CB_BOOK_ID)")
    }

    companion object {
        const val DATABASE_NAME = "personal_books.db"
        const val DATABASE_VERSION = 5

        const val TABLE_BOOKS = "books"
        const val COL_BOOK_ID = "id"
        const val COL_BOOK_TITLE = "title"
        const val COL_BOOK_AUTHOR = "author"
        const val COL_BOOK_CATEGORY = "category"
        const val COL_BOOK_RATING = "rating"
        const val COL_BOOK_NOTE = "note"
        const val COL_BOOK_STATUS = "reading_status"
        const val COL_BOOK_COVER = "cover_image_path"
        const val COL_BOOK_CURRENT_PAGE = "current_page"
        const val COL_BOOK_TOTAL_PAGES = "total_pages"
        const val COL_BOOK_PUBLISHER = "publisher"
        const val COL_BOOK_CREATED_AT = "created_at"
        const val COL_BOOK_UPDATED_AT = "updated_at"
        const val COL_BOOK_FINISHED_AT = "finished_at"

        const val TABLE_QUOTES = "quotes"
        const val COL_QUOTE_ID = "id"
        const val COL_QUOTE_BOOK_ID = "book_id"
        const val COL_QUOTE_CONTENT = "content"
        const val COL_QUOTE_PAGE = "page"
        const val COL_QUOTE_CHAPTER = "chapter"
        const val COL_QUOTE_TAG = "tag"
        const val COL_QUOTE_CREATED_AT = "created_at"

        const val TABLE_COLLECTIONS = "collections"
        const val COL_COLLECTION_ID = "id"
        const val COL_COLLECTION_NAME = "name"
        const val COL_COLLECTION_DESC = "description"
        const val COL_COLLECTION_COLOR = "color_hex"
        const val COL_COLLECTION_ICON = "icon_name"
        const val COL_COLLECTION_CREATED_AT = "created_at"
        const val COL_COLLECTION_UPDATED_AT = "updated_at"

        const val TABLE_COLLECTION_BOOKS = "collection_books"
        const val COL_CB_COLLECTION_ID = "collection_id"
        const val COL_CB_BOOK_ID = "book_id"
        const val COL_CB_CREATED_AT = "created_at"

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
                $COL_BOOK_CURRENT_PAGE INTEGER NOT NULL DEFAULT 0,
                $COL_BOOK_TOTAL_PAGES INTEGER NOT NULL DEFAULT 0,
                $COL_BOOK_PUBLISHER TEXT NOT NULL DEFAULT '',
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
                $COL_QUOTE_PAGE TEXT NOT NULL DEFAULT '',
                $COL_QUOTE_CHAPTER TEXT NOT NULL DEFAULT '',
                $COL_QUOTE_TAG TEXT NOT NULL DEFAULT '',
                $COL_QUOTE_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_QUOTE_BOOK_ID)
                    REFERENCES $TABLE_BOOKS($COL_BOOK_ID)
                    ON DELETE CASCADE
            )
        """

        private const val CREATE_COLLECTIONS_TABLE = """
            CREATE TABLE $TABLE_COLLECTIONS (
                $COL_COLLECTION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_COLLECTION_NAME TEXT NOT NULL,
                $COL_COLLECTION_DESC TEXT NOT NULL DEFAULT '',
                $COL_COLLECTION_COLOR TEXT NOT NULL DEFAULT '#7D562D',
                $COL_COLLECTION_ICON TEXT NOT NULL DEFAULT 'auto_stories',
                $COL_COLLECTION_CREATED_AT INTEGER NOT NULL,
                $COL_COLLECTION_UPDATED_AT INTEGER NOT NULL
            )
        """

        private const val CREATE_COLLECTION_BOOKS_TABLE = """
            CREATE TABLE $TABLE_COLLECTION_BOOKS (
                $COL_CB_COLLECTION_ID INTEGER NOT NULL,
                $COL_CB_BOOK_ID INTEGER NOT NULL,
                $COL_CB_CREATED_AT INTEGER NOT NULL,
                PRIMARY KEY ($COL_CB_COLLECTION_ID, $COL_CB_BOOK_ID),
                FOREIGN KEY($COL_CB_COLLECTION_ID)
                    REFERENCES $TABLE_COLLECTIONS($COL_COLLECTION_ID)
                    ON DELETE CASCADE,
                FOREIGN KEY($COL_CB_BOOK_ID)
                    REFERENCES $TABLE_BOOKS($COL_BOOK_ID)
                    ON DELETE CASCADE
            )
        """
    }
}
