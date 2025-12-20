package com.speechify.composeuichallenge.repository

import android.content.Context
import com.speechify.composeuichallenge.R
import com.speechify.composeuichallenge.data.Book
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton


///////////////////////////////////////////////////////
//                                                   //
//              DO NOT MODIFY THIS FILE              //
//                                                   //
///////////////////////////////////////////////////////
interface BooksRepository {
    suspend fun getBooks(): List<Book>
    suspend fun getBook(id: String): Book?
    suspend fun searchBook(query: String): List<Book>
}

@Singleton
class BooksRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BooksRepository {

    private val books by lazy { parseBooksList() }

    override suspend fun getBooks(): List<Book> {
        delay(1500)
        return books
    }

    override suspend fun getBook(id: String): Book? {
        delay(1000)
        return books.find { it.id == id }
    }

    override suspend fun searchBook(query: String): List<Book> {
        delay(100)
        return books.filter { it.name.contains(query, ignoreCase = true) }
    }

    private fun parseBooksList(): List<Book> {
        val inputStream = context.resources.openRawResource(R.raw.books_information)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return Json.decodeFromString(
            deserializer = ListSerializer(Product.serializer()),
            string = jsonString
        ).map {
            Book(
                id = it.id,
                name = it.name,
                imageUrl = it.thumbnails.large,
                author = it.details.authors.first(),
                description = it.description.orEmpty(),
                rating = it.details.goodreadsRating.rating.toFloat(),
                reviewCount = it.details.goodreadsRating.count.toString().take(3).toInt(),
            )
        }
    }
}
