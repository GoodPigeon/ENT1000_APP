package com.example.intoleranser.DTO

import java.util.*

data class RegisteredDTO(
    var foodDTO: FoodDTO,
    var date: String,
    var symptom: String,
    var alvorlighet: String
)