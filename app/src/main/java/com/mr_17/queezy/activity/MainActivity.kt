package com.mr_17.queezy.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.mr_17.queezy.R
import com.mr_17.queezy.api.Api
import com.mr_17.queezy.api.ApiCount
import com.mr_17.queezy.api.QuizQuestions
import com.mr_17.queezy.api.Result
import com.mr_17.queezy.model.FirebaseModel
import com.mr_17.queezy.model.Question
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.*

class MainActivity : AppCompatActivity() {

    private var myAuth: FirebaseAuth? = null

    private var startQuizButton: Button? = null
    private var categorySpinner: Spinner? = null
    private var difficultySpinner: Spinner? = null
    private var wishingMsg: TextView? = null
    private var fullName: TextView? = null
    private var logoutButton: ImageView? = null

    private var loading: LinearLayout? = null

    private var category: String? = "science_computer"
    private var difficulty: String? = "easy"

    private var q: Question? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        InitializeFields()

        startQuizButton!!.setOnClickListener {
            startQuizButton!!.visibility = View.GONE
            loading!!.visibility = View.VISIBLE
            fetchQuestionCount()
        }

        logoutButton!!.setOnClickListener {
            // setting up the alert dialog

            // setting up the alert dialog
            val confirmLogoutDialog = AlertDialog.Builder(this@MainActivity)
            confirmLogoutDialog.setTitle("Confirm Logout")
            confirmLogoutDialog.setMessage("Are sure you want to logout?")

            // creating functionality of the "yes" button

            // creating functionality of the "yes" button
            confirmLogoutDialog.setPositiveButton(
                "Yes"
            ) { dialog, which ->
                myAuth!!.signOut()
                SendToActivity(WelcomeActivity::class.java, false)
            }

            // creating the "no" button

            // creating the "no" button
            confirmLogoutDialog.setNegativeButton(
                "No"
            ) { dialog, which -> }

            confirmLogoutDialog.create().show()
        }
    }

    private fun InitializeFields()
    {
        myAuth = FirebaseAuth.getInstance()
        wishingMsg = findViewById(R.id.wishing_msg)
        wishingMsg!!.setText("Good " + GetWishing())
        fullName = findViewById(R.id.full_name)
        startQuizButton = findViewById(R.id.start_quiz_button)
        categorySpinner = findViewById(R.id.choose_category_spinner)
        difficultySpinner = findViewById(R.id.choose_difficulty_spinner)
        logoutButton = findViewById(R.id.logout_button)

        loading = findViewById(R.id.loading)

        if(myAuth!!.currentUser != null) {
            FirebaseModel.databaseRef_users.child(myAuth!!.currentUser!!.uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val retrieveFirstName =
                                snapshot.child(FirebaseModel.node_firstName).value.toString()
                            val retrieveLastName =
                                snapshot.child(FirebaseModel.node_lastName).value.toString()
                            fullName!!.setText(retrieveFirstName + " " + retrieveLastName)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
        else
            fullName!!.setText("Guest User")
    }

    private fun fetchQuestionCount()
    {
        difficulty = (difficultySpinner!!.selectedItem as String?)?.lowercase()
        val category_value = resources.getStringArray(R.array.values2)[categorySpinner!!.selectedItemId.toInt()].toInt()

        if (category_value == 0)
        {
            Toast.makeText(applicationContext, "Select a Category.", Toast.LENGTH_SHORT).show()
            loading!!.visibility = View.GONE
            startQuizButton!!.visibility = View.VISIBLE
            return
        }

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(Api.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api: Api = retrofit.create(Api::class.java)
        val call: Call<ApiCount> = api.getQuizQuestions(category_value)
        call.enqueue(object : Callback<ApiCount> {
            override fun onResponse(call: Call<ApiCount>, response: Response<ApiCount>) {
                when (difficulty) {
                    "easy" -> response.body()?.categoryQuestionCount?.totalEasyQuestionCount?.let {
                        fetchQuestionAPI(
                            it
                        )
                    }
                    "medium" -> response.body()?.categoryQuestionCount?.totalMediumQuestionCount?.let {
                        fetchQuestionAPI(
                            it
                        )
                    }
                    "hard" -> response.body()?.categoryQuestionCount?.totalHardQuestionCount?.let {
                        fetchQuestionAPI(
                            it
                        )
                    }
                }
            }

            override fun onFailure(call: Call<ApiCount>, t: Throwable) {
                Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_SHORT)
                    .show()
                loading!!.visibility = View.GONE
                startQuizButton!!.visibility = View.VISIBLE
            }
        })
    }

    fun fetchQuestionAPI(categoryCount: Int)
    {
        q = Question(applicationContext)
        val category_value = resources.getStringArray(R.array.values2)[categorySpinner!!.selectedItemId.toInt()].toInt()
        val retrofit = Retrofit.Builder()
            .baseUrl(Api.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(Api::class.java)
        val call: Call<QuizQuestions> = api.getQuizQuestions(
            "url3986",
            if (categoryCount >= 10) 10 else categoryCount - 1,
            difficulty,
            category_value,
            "multiple"
        )
        call.enqueue(object : Callback<QuizQuestions?> {
            override fun onResponse(
                call: Call<QuizQuestions?>,
                response: Response<QuizQuestions?>
            ) {
                Log.v("url-----", call.request().url().toString())
                val quizQuestions: QuizQuestions? = response.body()
                if (quizQuestions?.responseCode === 0) {
                    q!!.results = quizQuestions.getResults() as ArrayList<Result>?
                    if (q!!.results != null) {
                        for (r in q!!.results!!) {
                            try {
                                q!!.question?.add(URLDecoder.decode(r.question, "UTF-8"))
                            } catch (e: UnsupportedEncodingException) {
                                e.printStackTrace()
                            }
                            val random = Random()
                            // For Boolean Type Questions, only 2 possible options (True/False)
                            // For multiple choices, 4 options are required.
                            val ran = if (r.type
                                    .equals("boolean")
                            ) random.nextInt(2) else random.nextInt(4)
                            setOptions(r, ran)
                            q!!.Answer?.add(ran + 1)
                        }
                        Log.v("answers", q!!.Answer.toString())
                    }
                    q!!.question!!.get(0)!!.let { Log.v("question1", it) }

                    loading!!.visibility = View.GONE
                    startQuizButton!!.visibility = View.VISIBLE

                    val intent = Intent(this@MainActivity, QuizActivity::class.java)
                    intent.putExtra("question", q)
                    intent.putExtra("category", categorySpinner!!.selectedItem.toString())
                    intent.putExtra("difficulty", difficultySpinner!!.selectedItem.toString())
                    startActivity(intent)
                }
                else {
                    Toast.makeText(
                        applicationContext,
                        "No questions available in this particular Category & Difficulty",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    loading!!.visibility = View.GONE
                    startQuizButton!!.visibility = View.VISIBLE
                }

            }

            override fun onFailure(call: Call<QuizQuestions?>, t: Throwable) {
                Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_SHORT)
                    .show()
                loading!!.visibility = View.GONE
                startQuizButton!!.visibility = View.VISIBLE
            }
        })
    }

    fun setOptions(r: Result, ran: Int)
    {
        val wrong: List<String>
        when (ran) {
            0 -> {
                try {
                    q!!.optA!!.add(URLDecoder.decode(r.correctAnswer, "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                wrong = r.incorrectAnswers!!
                try {
                    q!!.optB!!.add(URLDecoder.decode(wrong[0], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                // Options C, D are not applicable for Boolean Type Questions.
                if (r.type.equals("boolean")) {
                    q!!.optC!!.add("false")
                    q!!.optD!!.add("false")
                    return
                }
                try {
                    q!!.optC!!.add(URLDecoder.decode(wrong[1], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                try {
                    q!!.optD!!.add(URLDecoder.decode(wrong[2], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
            }
            1 -> {
                try {
                    q!!.optB!!.add(URLDecoder.decode(r.correctAnswer, "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                wrong = r.incorrectAnswers!!
                try {
                    q!!.optA!!.add(URLDecoder.decode(wrong[0], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                // Options C, D are not applicable for Boolean Type Questions.
                if (r.type.equals("boolean")) {
                    q!!.optC!!.add("false")
                    q!!.optD!!.add("false")
                    return
                }
                try {
                    q!!.optC!!.add(URLDecoder.decode(wrong[1], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                try {
                    q!!.optD!!.add(URLDecoder.decode(wrong[2], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
            }
            2 -> {
                try {
                    q!!.optC!!.add(URLDecoder.decode(r.correctAnswer, "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                wrong = r.incorrectAnswers!!
                try {
                    q!!.optA!!.add(URLDecoder.decode(wrong[0], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                try {
                    q!!.optB!!.add(URLDecoder.decode(wrong[1], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                try {
                    q!!.optD!!.add(URLDecoder.decode(wrong[2], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
            }
            3 -> {
                try {
                    q!!.optD!!.add(URLDecoder.decode(r.correctAnswer, "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                wrong = r.incorrectAnswers!!
                try {
                    q!!.optA!!.add(URLDecoder.decode(wrong[0], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                try {
                    q!!.optB!!.add(URLDecoder.decode(wrong[1], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                try {
                    q!!.optC!!.add(URLDecoder.decode(wrong[2], "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun GetWishing(): String? {
        val c = Calendar.getInstance()
        val timeOfDay = c[Calendar.HOUR_OF_DAY]
        return if (timeOfDay < 12) "Morning" else if (timeOfDay < 16) "Afternoon" else if (timeOfDay < 21) "Evening" else "Night"
    }

    private fun SendToActivity(activityClass: Class<out Activity?>, backEnabled: Boolean) {
        val intent = Intent(this, activityClass)
        if (!backEnabled) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        if (!backEnabled) finish()
    }
}