package com.example.guessme

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity

class Preferences(act : AppCompatActivity) {
    var email : String = "";
    var token : String = "";
    private var prefFile : String = "prefs";
    var activ = act;
    var sharedPreferences: SharedPreferences? = null;

    init {
        activ = act;
        sharedPreferences = activ.getSharedPreferences(prefFile, Context.MODE_PRIVATE)
    }

    fun edit(name: String, data : String?){
        val editor: SharedPreferences.Editor? =  sharedPreferences?.edit();
        editor?.putString(name,data)
        editor?.apply()
    }
    fun get(name : String) : String? {
        val sharedNameValue = sharedPreferences?.getString(name,"/");
        return sharedNameValue;
    }
}