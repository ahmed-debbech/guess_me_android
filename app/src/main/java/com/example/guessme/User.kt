package com.example.guessme

data class User(var email : String, var first_name : String, var last_name : String,  var picture : String) {
    override fun toString(): String {
        return "User {email: ${this.email}, first_name: ${this.first_name}, last_name: ${this.last_name}, picture: ${this.picture}}"
    }

}