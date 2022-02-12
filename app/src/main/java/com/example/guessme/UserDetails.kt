package com.example.guessme

data class UserDetails (
    var id : Int,
    var name :String,
    var photoLink : String,
    var solvedWords: Int,
    var points : Int,
    var email : String,
    var hidden : Int,
    var limited : Int
)