package com.bignerdranch.android.geoquiz

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.bignerdranch.android.geoquiz.databinding.ActivityMainBinding

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val quizViewModel: QuizViewModel by viewModels()

    private val cheatLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle the result
        if (result.resultCode == Activity.RESULT_OK) {
            quizViewModel.isCheater =
                result.data?.getBooleanExtra(EXTRA_ANSWER_SHOWN, false) ?: false
        }

    }

    private var score = 0
    private lateinit var answeredQuestions: MutableList<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate(Bundle?) called")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            score = savedInstanceState.getInt(KEY_SCORE, 0)
            answeredQuestions = savedInstanceState.getBooleanArray(KEY_ANSWERED_QUESTIONS)?.toMutableList()
                ?: MutableList(quizViewModel.questionBank.size) { false }
        } else {
            answeredQuestions = MutableList(quizViewModel.questionBank.size) { false }
        }

        binding.scoreTextView.text = getString(R.string.score_format, score)

        answeredQuestions = MutableList(quizViewModel.questionBank.size) { false } // Initialize here

        binding.trueButton.setOnClickListener {
            checkAnswer(true)
        }

        binding.falseButton.setOnClickListener {
            checkAnswer(false)
        }

        binding.nextButton.setOnClickListener {
            quizViewModel.moveToNext()
            updateQuestion() // loads next question
        }

        binding.previousButton.setOnClickListener {
            quizViewModel.moveToPrev()
            updateQuestion() // loads previous question
        }

        binding.cheatButton.setOnClickListener {
            // Start CheatActivity
            val answerIsTrue = quizViewModel.currentQuestionAnswer
            val intent = CheatActivity.newIntent(this@MainActivity, answerIsTrue)
            cheatLauncher.launch(intent)

        }

        updateQuestion() // loads question #1 on app startup
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SCORE, score)
        outState.putBooleanArray(KEY_ANSWERED_QUESTIONS, answeredQuestions.toBooleanArray())
    }


    private fun updateQuestion() {
        binding.questionTextView.setText(quizViewModel.currentQuestionText)
        // Set the answered status for the new question to false initially
        answeredQuestions[quizViewModel.currentIndex] = false
    }

    private fun checkAnswer(userAnswer: Boolean) {
        if (!answeredQuestions[quizViewModel.currentIndex]) {
            val correctAnswer = quizViewModel.currentQuestionAnswer
            val messageResId = when {
                quizViewModel.isCheater -> R.string.judgment_toast
                userAnswer == correctAnswer -> R.string.correct_toast
                else -> R.string.incorrect_toast
            }
            Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()

            if (userAnswer == correctAnswer) {
                score++
            }

            answeredQuestions[quizViewModel.currentIndex] = true // Mark the question as answered
            binding.scoreTextView.text = getString(R.string.score_format, score)

            if (answeredQuestions.all { it }) {
                showFinalScore()
            }
        } else {
            Toast.makeText(this, R.string.already_answered, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFinalScore() {
        val totalQuestions = quizViewModel.questionBank.size
        val percentageCorrect = (score.toFloat() / totalQuestions.toFloat()) * 100

        val formattedPercentage = String.format("%.2f", percentageCorrect)

        val toastMessage = getString(R.string.final_score_message, score, formattedPercentage)
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val KEY_SCORE = "score"
        private const val KEY_ANSWERED_QUESTIONS = "answeredQuestions"
    }
}
