package com.example.visual
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalcActivity : AppCompatActivity() {

    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calc)

        resultTextView = findViewById(R.id.resultTextView)

        setupButtons()
    }

    private fun setupButtons() {
        val digits = listOf(
            R.id.btn0 to "0",
            R.id.btn1 to "1",
            R.id.btn2 to "2",
            R.id.btn3 to "3",
            R.id.btn4 to "4",
            R.id.btn5 to "5",
            R.id.btn6 to "6",
            R.id.btn7 to "7",
            R.id.btn8 to "8",
            R.id.btn9 to "9",
            R.id.btnDot to "."
        )

        for ((id, text) in digits) {
            findViewById<Button>(id).setOnClickListener {
                appendToExpression(text)
            }
        }

        val ops = listOf(
            R.id.btnPlus to "+",
            R.id.btnMinus to "-",
            R.id.btnYmnz to "*",
            R.id.btnDelenie to "/"
        )

        for ((id, op) in ops) {
            findViewById<Button>(id).setOnClickListener {
                appendToExpression(op)
            }
        }

        findViewById<Button>(R.id.btnRavno).setOnClickListener {
            calculate()
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            resultTextView.text = "0"
        }
    }

    private fun appendToExpression(value: String) {
        var current = resultTextView.text.toString()
        if (current == "0" || current == "Error") {
            current = ""
        }
        resultTextView.text = current + value
    }

    private fun calculate() {
        val expression = resultTextView.text.toString()
        var operatorIndex = -1
        var operator = ' '
        for (i in expression.indices) {
            when (expression[i]) {
                '+', '-', '*', '/' -> {
                    operatorIndex = i
                    operator = expression[i]
                    break
                }
            }
        }

        if (operatorIndex == -1) {
            return
        }

        val leftStr = expression.substring(0, operatorIndex)
        val rightStr = expression.substring(operatorIndex + 1)

        if (leftStr.isEmpty() || rightStr.isEmpty()) {
            resultTextView.text = "Error"
            return
        }

        try {
            val left = leftStr.toDouble()
            val right = rightStr.toDouble()
            val result = when (operator) {
                '+' -> left + right
                '-' -> left - right
                '*' -> left * right
                '/' -> {
                    if (right == 0.0) {
                        resultTextView.text = "Error"
                        return
                    }
                    left / right
                }
                else -> {
                    resultTextView.text = "Error"
                    return
                }
            }

            resultTextView.text = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                result.toString()
            }

        } catch (e: NumberFormatException) {
            resultTextView.text = "Error"
        }
    }
}