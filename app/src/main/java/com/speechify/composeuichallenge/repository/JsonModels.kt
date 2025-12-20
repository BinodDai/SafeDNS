package com.speechify.composeuichallenge.repository

import kotlinx.serialization.Serializable

///////////////////////////////////////////////////////
//                                                   //
//              DO NOT MODIFY THIS FILE              //
//                                                   //
///////////////////////////////////////////////////////
@Serializable
data class Product(
    val id: String,
    val name: String,
    val status: String,
    val type: String,
    val supplier: String,
    val details: Details,
    val thumbnails: Thumbnails,
    val description: String?
)

@Serializable
data class Details(
    val authors: List<String>,
    val goodreadsRating: GoodreadsRating,
    val chapters: List<Chapter>,
    val publicationDate: String?
)

@Serializable
data class GoodreadsRating(
    val rating: Double,
    val count: Int
)

@Serializable
data class Chapter(
    val title: String
)

@Serializable
data class Thumbnails(
    val small: String,
    val large: String
)
