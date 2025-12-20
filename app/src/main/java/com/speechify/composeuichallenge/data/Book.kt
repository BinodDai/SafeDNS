package com.speechify.composeuichallenge.data

data class Book(
    val id: String,
    val name: String,
    val author: String,
    val imageUrl: String,
    val description: String,
    val rating: Float,
    val reviewCount: Int
)
