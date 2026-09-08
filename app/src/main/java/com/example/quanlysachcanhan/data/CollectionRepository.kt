package com.example.quanlysachcanhan.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.quanlysachcanhan.data.BookRepository.Companion.toBook
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.model.BookCollection
import com.example.quanlysachcanhan.model.CollectionWithStats
import com.example.quanlysachcanhan.utils.Constants

class CollectionRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun insert(collection: BookCollection, bookIds: List<Long> = emptyList()): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_COLLECTION_NAME, collection.name)
                put(DatabaseHelper.COL_COLLECTION_DESC, collection.description)
                put(DatabaseHelper.COL_COLLECTION_COLOR, collection.colorHex)
                put(DatabaseHelper.COL_COLLECTION_ICON, collection.iconName)
                put(DatabaseHelper.COL_COLLECTION_CREATED_AT, collection.createdAt)
                put(DatabaseHelper.COL_COLLECTION_UPDATED_AT, collection.updatedAt)
            }
            val collectionId = db.insert(DatabaseHelper.TABLE_COLLECTIONS, null, values)
            if (collectionId != -1L) {
                val now = System.currentTimeMillis()
                for (bId in bookIds.distinct()) {
                    val linkValues = ContentValues().apply {
                        put(DatabaseHelper.COL_CB_COLLECTION_ID, collectionId)
                        put(DatabaseHelper.COL_CB_BOOK_ID, bId)
                        put(DatabaseHelper.COL_CB_CREATED_AT, now)
                    }
                    db.insert(DatabaseHelper.TABLE_COLLECTION_BOOKS, null, linkValues)
                }
            }
            db.setTransactionSuccessful()
            return collectionId
        } finally {
            db.endTransaction()
        }
    }

    fun update(collection: BookCollection, bookIds: List<Long>? = null): Boolean {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_COLLECTION_NAME, collection.name)
                put(DatabaseHelper.COL_COLLECTION_DESC, collection.description)
                put(DatabaseHelper.COL_COLLECTION_COLOR, collection.colorHex)
                put(DatabaseHelper.COL_COLLECTION_ICON, collection.iconName)
                put(DatabaseHelper.COL_COLLECTION_UPDATED_AT, System.currentTimeMillis())
            }
            val rows = db.update(
                DatabaseHelper.TABLE_COLLECTIONS,
                values,
                "${DatabaseHelper.COL_COLLECTION_ID} = ?",
                arrayOf(collection.id.toString())
            )

            if (bookIds != null) {
                // Delete old links and re-insert new ones
                db.delete(
                    DatabaseHelper.TABLE_COLLECTION_BOOKS,
                    "${DatabaseHelper.COL_CB_COLLECTION_ID} = ?",
                    arrayOf(collection.id.toString())
                )
                val now = System.currentTimeMillis()
                for (bId in bookIds.distinct()) {
                    val linkValues = ContentValues().apply {
                        put(DatabaseHelper.COL_CB_COLLECTION_ID, collection.id)
                        put(DatabaseHelper.COL_CB_BOOK_ID, bId)
                        put(DatabaseHelper.COL_CB_CREATED_AT, now)
                    }
                    db.insert(DatabaseHelper.TABLE_COLLECTION_BOOKS, null, linkValues)
                }
            }

            db.setTransactionSuccessful()
            return rows > 0
        } finally {
            db.endTransaction()
        }
    }

    fun delete(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            DatabaseHelper.TABLE_COLLECTIONS,
            "${DatabaseHelper.COL_COLLECTION_ID} = ?",
            arrayOf(id.toString())
        )
        return rows > 0
    }

    fun getById(id: Long): BookCollection? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_COLLECTIONS,
            null,
            "${DatabaseHelper.COL_COLLECTION_ID} = ?",
            arrayOf(id.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return cursorToCollection(it)
            }
        }
        return null
    }

    fun getBookIdsInCollection(collectionId: Long): List<Long> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Long>()
        val cursor = db.query(
            DatabaseHelper.TABLE_COLLECTION_BOOKS,
            arrayOf(DatabaseHelper.COL_CB_BOOK_ID),
            "${DatabaseHelper.COL_CB_COLLECTION_ID} = ?",
            arrayOf(collectionId.toString()),
            null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.getLong(0))
            }
        }
        return list
    }

    fun getBooksInCollection(collectionId: Long): List<Book> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Book>()
        val query = """
            SELECT b.*
            FROM ${DatabaseHelper.TABLE_COLLECTION_BOOKS} cb
            INNER JOIN ${DatabaseHelper.TABLE_BOOKS} b
                ON cb.${DatabaseHelper.COL_CB_BOOK_ID} = b.${DatabaseHelper.COL_BOOK_ID}
            WHERE cb.${DatabaseHelper.COL_CB_COLLECTION_ID} = ?
            ORDER BY cb.${DatabaseHelper.COL_CB_CREATED_AT} DESC
        """
        val cursor = db.rawQuery(query, arrayOf(collectionId.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.toBook())
            }
        }
        return list
    }

    fun removeBookFromCollection(collectionId: Long, bookId: Long): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete(
            DatabaseHelper.TABLE_COLLECTION_BOOKS,
            "${DatabaseHelper.COL_CB_COLLECTION_ID} = ? AND ${DatabaseHelper.COL_CB_BOOK_ID} = ?",
            arrayOf(collectionId.toString(), bookId.toString())
        )
        if (rows > 0) {
            val remaining = getBookIdsInCollection(collectionId)
            if (remaining.isEmpty()) {
                delete(collectionId)
            } else {
                val values = ContentValues().apply {
                    put(DatabaseHelper.COL_COLLECTION_UPDATED_AT, System.currentTimeMillis())
                }
                db.update(
                    DatabaseHelper.TABLE_COLLECTIONS,
                    values,
                    "${DatabaseHelper.COL_COLLECTION_ID} = ?",
                    arrayOf(collectionId.toString())
                )
            }
        }
        return rows > 0
    }

    fun cleanupEmptyCollections(): Int {
        val db = dbHelper.writableDatabase
        return db.delete(
            DatabaseHelper.TABLE_COLLECTIONS,
            "${DatabaseHelper.COL_COLLECTION_ID} NOT IN (SELECT DISTINCT ${DatabaseHelper.COL_CB_COLLECTION_ID} FROM ${DatabaseHelper.TABLE_COLLECTION_BOOKS})",
            null
        )
    }

    fun getCollectionWithStats(collectionId: Long): CollectionWithStats? {
        val col = getById(collectionId) ?: return null
        val db = dbHelper.readableDatabase
        return getStatsForCollection(db, col)
    }

    fun getAllWithStats(): List<CollectionWithStats> {
        cleanupEmptyCollections()
        val db = dbHelper.readableDatabase
        val collections = mutableListOf<BookCollection>()

        val cursor = db.query(
            DatabaseHelper.TABLE_COLLECTIONS,
            null,
            null, null, null, null,
            "${DatabaseHelper.COL_COLLECTION_UPDATED_AT} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                collections.add(cursorToCollection(it))
            }
        }

        return collections.map { col ->
            getStatsForCollection(db, col)
        }
    }

    private fun getStatsForCollection(db: SQLiteDatabase, col: BookCollection): CollectionWithStats {
        val query = """
            SELECT b.${DatabaseHelper.COL_BOOK_ID},
                   b.${DatabaseHelper.COL_BOOK_STATUS},
                   b.${DatabaseHelper.COL_BOOK_COVER}
            FROM ${DatabaseHelper.TABLE_COLLECTION_BOOKS} cb
            INNER JOIN ${DatabaseHelper.TABLE_BOOKS} b
                ON cb.${DatabaseHelper.COL_CB_BOOK_ID} = b.${DatabaseHelper.COL_BOOK_ID}
            WHERE cb.${DatabaseHelper.COL_CB_COLLECTION_ID} = ?
            ORDER BY cb.${DatabaseHelper.COL_CB_CREATED_AT} DESC
        """

        var total = 0
        var read = 0
        var reading = 0
        var unread = 0
        val covers = mutableListOf<String?>()
        val bookIds = mutableListOf<Long>()

        val c = db.rawQuery(query, arrayOf(col.id.toString()))
        c.use {
            while (it.moveToNext()) {
                total++
                val bId = it.getLong(0)
                val status = it.getString(1)
                val cover = it.getString(2)

                bookIds.add(bId)
                covers.add(cover)

                when (status) {
                    Constants.ReadingStatus.READ -> read++
                    Constants.ReadingStatus.READING -> reading++
                    else -> unread++
                }
            }
        }

        return CollectionWithStats(
            collection = col,
            totalBooks = total,
            readBooks = read,
            readingBooks = reading,
            unreadBooks = unread,
            bookCovers = covers,
            bookIds = bookIds
        )
    }

    data class OverviewStats(
        val totalCollections: Int,
        val totalBooksInCollections: Int,
        val readBooksInCollections: Int
    ) {
        val percent: Int
            get() = if (totalBooksInCollections > 0) (readBooksInCollections * 100 / totalBooksInCollections) else 0
    }

    fun getOverviewStats(): OverviewStats {
        val db = dbHelper.readableDatabase

        var totalCollections = 0
        val cursorCol = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_COLLECTIONS}", null)
        cursorCol.use {
            if (it.moveToFirst()) totalCollections = it.getInt(0)
        }

        var totalBooks = 0
        var readBooks = 0
        val query = """
            SELECT b.${DatabaseHelper.COL_BOOK_STATUS}
            FROM ${DatabaseHelper.TABLE_COLLECTION_BOOKS} cb
            INNER JOIN ${DatabaseHelper.TABLE_BOOKS} b
                ON cb.${DatabaseHelper.COL_CB_BOOK_ID} = b.${DatabaseHelper.COL_BOOK_ID}
        """
        val cursorBooks = db.rawQuery(query, null)
        cursorBooks.use {
            while (it.moveToNext()) {
                totalBooks++
                val status = it.getString(0)
                if (status == Constants.ReadingStatus.READ) {
                    readBooks++
                }
            }
        }

        return OverviewStats(
            totalCollections = totalCollections,
            totalBooksInCollections = totalBooks,
            readBooksInCollections = readBooks
        )
    }

    fun seedSampleCollectionsIfEmpty(allBooks: List<Book>) {
        if (allBooks.isEmpty()) return
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_COLLECTIONS}", null)
        val count = cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
        if (count > 0) return

        val bookIdList = allBooks.map { it.id }

        // Sample 1: Triết Học & Nhân Sinh
        insert(
            BookCollection(
                name = "Triết Học & Nhân Sinh",
                description = "Những tác phẩm khai sáng tâm trí, soi rọi lẽ sống và chiêm nghiệm chiều sâu nội tâm.",
                colorHex = "#7D562D",
                iconName = "psychology"
            ),
            bookIds = bookIdList.take(3)
        )

        // Sample 2: Phát Triển Bản Thân
        insert(
            BookCollection(
                name = "Phát Triển Bản Thân",
                description = "Xây dựng thói quen tích cực, rèn luyện tư duy kỷ luật và nâng tầm hiệu suất.",
                colorHex = "#FFCA98",
                iconName = "trending_up"
            ),
            bookIds = bookIdList.drop(1).take(3)
        )

        // Sample 3: Kinh Điển Phải Đọc Lại
        insert(
            BookCollection(
                name = "Kinh Điển Phải Đọc Lại",
                description = "Những áng văn bất hủ vượt thời gian lưu giữ giá trị nhân văn sâu sắc.",
                colorHex = "#031632",
                iconName = "auto_stories"
            ),
            bookIds = bookIdList.drop(2).take(4)
        )
    }

    private fun cursorToCollection(cursor: Cursor): BookCollection {
        return BookCollection(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COLLECTION_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COLLECTION_NAME)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COLLECTION_DESC)) ?: "",
            colorHex = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COLLECTION_COLOR)) ?: "#7D562D",
            iconName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COLLECTION_ICON)) ?: "auto_stories",
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COLLECTION_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COLLECTION_UPDATED_AT))
        )
    }
}
