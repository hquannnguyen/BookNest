package com.example.quanlysachcanhan.utils

object Constants {

    object ReadingStatus {
        const val UNREAD = "UNREAD"
        const val READING = "READING"
        const val READ = "READ"

        val ALL = listOf(UNREAD, READING, READ)
    }

    object Category {
        const val SCIENCE = "SCIENCE"
        const val LITERATURE = "LITERATURE"
        const val ECONOMICS = "ECONOMICS"
        const val CHILDREN = "CHILDREN"
        const val LIFE_SKILLS = "LIFE_SKILLS"
        const val INFORMATION_TECHNOLOGY = "INFORMATION_TECHNOLOGY"
        const val PSYCHOLOGY = "PSYCHOLOGY"
        const val HISTORY = "HISTORY"
        const val OTHER = "OTHER"

        val ALL = listOf(
            SCIENCE,
            LITERATURE,
            ECONOMICS,
            CHILDREN,
            LIFE_SKILLS,
            INFORMATION_TECHNOLOGY,
            PSYCHOLOGY,
            HISTORY,
            OTHER
        )
    }

    object Sort {
        const val TITLE = "TITLE"
        const val AUTHOR = "AUTHOR"
        const val RATING = "RATING"
        const val STATUS = "STATUS"
    }

    object Extras {
        const val BOOK_ID = "BOOK_ID"
        const val EDIT_MODE = "EDIT_MODE"
    }
}
