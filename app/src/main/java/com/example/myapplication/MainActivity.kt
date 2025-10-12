package com.example.myapplication

import android.widget.TextView
import android.widget.Button
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    var firstNumber = 0.0
    var operation = ""
    var isOperationClicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val tvResult: TextView = findViewById(R.id.result_text_view)

        val buttonID = listOf(
            R.id.button, R.id.button6, R.id.button7,                   // 0 1 2
            R.id.button8, R.id.button11, R.id.button12,                // 3 4 5
            R.id.button13, R.id.button15, R.id.button16, R.id.button17 // 6 7 8 9
        )

        for (id in buttonID) {
            val button: Button = findViewById(id)
            button.setOnClickListener {
                if (isOperationClicked) {
                    tvResult.text = ""
                    isOperationClicked = false
                }
                tvResult.text = tvResult.text.toString() + button.text.toString()
            }
        }

        //С
        val buttonClear: Button = findViewById(R.id.button19)
        buttonClear.setOnClickListener {
            tvResult.text = ""
            firstNumber = 0.0
            operation = ""
        }

        //del
        val buttonDel: Button = findViewById(R.id.button5)
        buttonDel.setOnClickListener {
            if (tvResult.text.isNotEmpty()) {
                tvResult.text = tvResult.text.toString().dropLast(1)
            }
        }

        val operations = mapOf(
            R.id.button9 to "+",
            R.id.button14 to "-",
            R.id.button18 to "×",
            R.id.button22 to "÷"
        )

        for ((id, op) in operations) {
            val button: Button = findViewById(id)
            button.setOnClickListener {
                if (tvResult.text.isNotEmpty()) {
                    firstNumber = tvResult.text.toString().toDouble()
                    operation = op
                    isOperationClicked = true
                }
            }
        }

        //равно
        val buttonRavno: Button = findViewById(R.id.button3)
        buttonRavno.setOnClickListener {
            if (tvResult.text.isNotEmpty() && operation.isNotEmpty()) {
                val secondNumber = tvResult.text.toString().toDouble()
                val result = when (operation) {
                    "+" -> firstNumber + secondNumber
                    "-" -> firstNumber - secondNumber
                    "×" -> firstNumber * secondNumber
                    "÷" -> if (secondNumber != 0.0) firstNumber / secondNumber else Double.NaN
                    else -> 0.0
                }
                tvResult.text = if (result % 1 == 0.0)
                    result.toInt().toString()
                else
                    result.toString()
                operation = ""
            }
        }

        //процент
        val buttonPercent: Button = findViewById(R.id.button21)
        buttonPercent.setOnClickListener {
            if (tvResult.text.isNotEmpty()) {
                val value = tvResult.text.toString().toDouble()
                val percent = value / 100
                tvResult.text = percent.toString()
            }
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
