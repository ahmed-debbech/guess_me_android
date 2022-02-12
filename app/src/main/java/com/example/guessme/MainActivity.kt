package com.example.guessme

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.*
import com.facebook.*
import com.facebook.login.LoginManager
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import com.squareup.picasso.Picasso
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var actualWord : TextView;
    private lateinit var submit : Button;
    private lateinit var login_button : Button;
    private lateinit var progress : ProgressBar;
    private lateinit var name : TextView;
    private lateinit var pic : ImageView;
    private lateinit var points : TextView;
    private lateinit var leaderboard : TextView;
    private lateinit var yourword : EditText
    private lateinit var rules : TextView;
    private lateinit var counter : TextView;
    private lateinit var msg : TextView;
    private lateinit var replay : TextView;
    private lateinit var dialog : FrameLayout
    private val mediaType = "application/json".toMediaType()
    var handler: Handler = Handler()
    var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        actualWord = findViewById(R.id.actualWord)
        progress = findViewById(R.id.progress)
        leaderboard = findViewById(R.id.leaderboard)
        points = findViewById(R.id.points)
        submit = findViewById(R.id.submit)
        name = findViewById(R.id.name)
        yourword = findViewById(R.id.yourword)
        pic = findViewById(R.id.pic)
        counter = findViewById(R.id.counter)
        rules = findViewById(R.id.rules)
        login_button = findViewById(R.id.login_button)
        dialog = findViewById(R.id.frameDialog)
        replay = findViewById(R.id.replay)
        msg = findViewById(R.id.msg)

        getActualWord("https://guessme2022.herokuapp.com/api/v1/word")

        val Token = AccessToken.getCurrentAccessToken()
        if(Token == null || Token.isExpired){
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }else {
            getUserProfile(
                AccessToken.getCurrentAccessToken(),
                AccessToken.getCurrentAccessToken()?.userId
            )
        }

        progress.visibility = View.VISIBLE
        dialog.visibility = View.GONE

        replay.setOnClickListener({
            handler.postDelayed(Runnable {
                handler.postDelayed(runnable!!, 2000.toLong())
                //Toast.makeText(this@MainActivity, "This method will run every 10 seconds", Toast.LENGTH_SHORT).show()
                solved()
            }.also { runnable = it }, 2000.toLong())
            dialog.visibility = View.GONE;
            yourword.isEnabled = true;
            submit.isEnabled = true
            getUserProfile(
                AccessToken.getCurrentAccessToken(),
                AccessToken.getCurrentAccessToken()?.userId
            )
            getActualWord("https://guessme2022.herokuapp.com/api/v1/word")
        })
        rules.setOnClickListener({
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://guessme2022.herokuapp.com"))
            startActivity(i)
        })
        yourword.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
                if(s.length == (actualWord.text.length/2)){
                    submit.isEnabled = true;
                }else{
                    submit.isEnabled = false;
                }
                if(s.length > (actualWord.text.length/2)){
                    counter.setTextColor(Color.parseColor("#ff0000"))
                }else{
                    counter.setTextColor(Color.parseColor("#ffffff"))
                }
                counter.setText(s.length.toString() + "/" + actualWord.text.length/2)
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
            }
        })
        leaderboard.setOnClickListener({
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://guessme2022.herokuapp.com/leaderboard"))
            startActivity(i)
        })
        submit.setOnClickListener(View.OnClickListener { view ->
            progress.visibility = View.VISIBLE;
            process(yourword.text.toString());
        })
        login_button.setOnClickListener(View.OnClickListener { view ->
            LoginManager.getInstance().logOut();
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        })
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(Runnable {
            handler.postDelayed(runnable!!, 2000.toLong())
            //Toast.makeText(this@MainActivity, "This method will run every 10 seconds", Toast.LENGTH_SHORT).show()
            solved()
        }.also { runnable = it }, 2000.toLong())
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable!!)
    }
    override fun onRestart() {
        super.onRestart()
        val Token = AccessToken.getCurrentAccessToken()
        if(Token == null || Token.isExpired){
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }else {
            //getActualWord("https://guessme2022.herokuapp.com/api/v1/word")
            var pr = Preferences(this);
            getUser(pr.get("email").toString())
        }
    }

    fun getUserProfile(token: AccessToken?, userId: String?) {
        val parameters = Bundle()
        parameters.putString(
            "fields",
            "id, first_name, middle_name, last_name, name, picture, email"
        )
        GraphRequest(token,
            "/$userId/",
            parameters,
            HttpMethod.GET,
            GraphRequest.Callback { response ->
                val jsonObject = response.jsonObject
                if (BuildConfig.DEBUG) {
                    FacebookSdk.setIsDebugEnabled(true)
                    FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS)
                }
                var fullname: String = jsonObject?.getString("first_name") + " " + jsonObject?.getString("last_name")
                runOnUiThread(Runnable {
                    name.setText(fullname);
                })
                var facebookProfilePicURL = "";
                if (jsonObject?.has("picture") == true) {
                    val facebookPictureObject = jsonObject.getJSONObject("picture")
                    if (facebookPictureObject.has("data")) {
                        val facebookDataObject = facebookPictureObject.getJSONObject("data")
                        if (facebookDataObject.has("url")) {
                            facebookProfilePicURL = facebookDataObject.getString("url")
                            runOnUiThread(Runnable {
                                Picasso.with(this).load(facebookProfilePicURL).into(pic);
                            })
                        }
                    }
                } else {
                    println("no photo found")
                }

                // Facebook Email
                if (jsonObject?.has("email") == true) {
                    val facebookEmail = jsonObject.getString("email")
                    println("The email: " + facebookEmail);
                    val gson = Gson()
                    checkUser(gson.toJson(User(facebookEmail, jsonObject?.getString("first_name"), jsonObject?.getString("last_name"), facebookProfilePicURL)), facebookEmail)
                }
            }).executeAsync()
    }

    fun solved(){
        println("CHeckig word...")
        var pr = Preferences(this)
        var id = pr.get("word_id")
        val request = Request.Builder()
            .url("https://guessme2022.herokuapp.com/api/v1/done/" + id)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if(response.body?.string() == "1"){
                    runOnUiThread({
                        yourword.isEnabled = false;
                        dialog.visibility = View.VISIBLE;
                        submit.isEnabled = false
                        msg.setTextColor(getResources().getColor(R.color.red))
                        msg.setText("Aw! The word was just solved by someone else :( Try again! ")
                    })
                }else{
                    println("the word is not solved yet")
                }
            }
        })
    }
    fun process(yourword1 : String){
        var pr = Preferences(this);
        var l = ProcessObject(pr.get("token"), pr.get("email"), yourword1)
        val gson = Gson()
        println("token : " + gson.toJson(l).toString())
        val request = Request.Builder()
            .url("https://guessme2022.herokuapp.com/api/v1/process/")
            .post(gson.toJson(l).toRequestBody(mediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { progress.visibility = View.GONE; }
                var result : ResultProcess? = null;
                val gsonn = Gson()
                result = gsonn.fromJson(response.body?.string(), ResultProcess::class.java)
                println("array " + result.toString())
                if(result.won == 1){
                    handler.removeCallbacks(runnable!!)
                    //Show how much score they gain
                    runOnUiThread {
                        yourword.isEnabled = false;
                        dialog.visibility = View.VISIBLE;
                        submit.isEnabled = false
                        msg.setTextColor(getResources().getColor(R.color.bgreen))
                        msg.setText("Congratulations you solved the word!! and you earned new "+ result.score +" points")
                    }
                }else{
                    loadWord(yourword1, result.score)
                }
            }
        })
    }
    fun getUser(email : String){
        val request = Request.Builder()
            .url("https://guessme2022.herokuapp.com/api/v1/user/" + email)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                var gson = Gson()
                var userWrapperc = gson.fromJson(response.body?.string(), UserWrapper::class.java)
                println(userWrapperc.toString());
                var ss : String = "(${userWrapperc?.user?.points})"
                var pr = Preferences(this@MainActivity);
                pr.edit("token", userWrapperc?.token.toString())
                pr.edit("email", userWrapperc?.user?.email)
                runOnUiThread { ->
                    points.text = ss
                }
            }
        })
    }
    fun checkUser(user : String, email : String){
        println("THE USER: " + user);
        val request = Request.Builder()
            .url("https://guessme2022.herokuapp.com/api/v1/auth")
            .post(user.toRequestBody(mediaType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                println("AUTHESD");
                getUser(email)
            }
        })
    }
    fun loadWord(worda : String?, colors : String){
        if (worda != null) {
            /*var st : String = "";
            */
            runOnUiThread(Runnable{
                var stt : String = "";
                for(i in 0..worda.length-1) {
                    if(i != worda.length){
                        stt = stt + worda[i] + " "
                    }else{
                        stt = stt + worda[i];
                    }
                }
                var span = SpannableString(stt);
                var y = 0;
                for(i in 0..stt.length-1){
                    if(i % 2 == 0){
                        if(colors[y] == '1') {
                            span.setSpan(
                                ForegroundColorSpan(getResources().getColor(R.color.bgrey)),
                                i,
                                i+1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                        }
                        if(colors[y] == '2') {
                            span.setSpan(
                                ForegroundColorSpan(getResources().getColor(R.color.byellow)),
                                i,
                                i+1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                        }
                        if(colors[y] == '3') {
                            span.setSpan(
                                ForegroundColorSpan(getResources().getColor(R.color.bgreen)),
                                i,
                                i+1,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                        }
                        y++;
                    }
                }
                actualWord.setText(span, TextView.BufferType.SPANNABLE);
                counter.setText("0/" + worda.length)
                yourword.setText("");
            })
        }
    }
    fun loadWordLength(worda : String?){
        if (worda != null) {
            var st : String = "";
            for(i in 0..worda.length-1) {
                st += "_ ";
            }
            runOnUiThread(Runnable{
                progress.visibility = View.GONE;
                actualWord.setText(st);
                counter.setText("0/" + worda.length)
            })
        }
    }
    fun getActualWord(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                var gson = Gson()
                var wordObj = gson.fromJson(response.body?.string(), WordObject::class.java)
                this@MainActivity.loadWordLength(wordObj.name);
                var pr = Preferences(this@MainActivity)
                pr.edit("word_id", wordObj.id.toString())
            }
        })
    }
}