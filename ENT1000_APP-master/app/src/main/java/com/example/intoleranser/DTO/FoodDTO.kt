package com.example.intoleranser.DTO

data class FoodDTO(
    var id: String,
    var name: String,
    var fodmap: String,
    var category: String,
    var details: Details
)

data class Details(
    var oligos: Int,
    var fructose: Int,
    var polyols: Int,
    var lactose: Int
)