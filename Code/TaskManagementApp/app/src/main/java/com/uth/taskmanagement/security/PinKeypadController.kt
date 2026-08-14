package com.uth.taskmanagement.security

import android.view.View
import android.widget.TextView

class PinKeypadController(
    private val dots: List<View>,
    private val onComplete: (String) -> Unit
) {
    private val currentPin = StringBuilder()
    private val maxLength = 4

    fun bindKeys(numberKeys: Map<TextView, String>, backspaceKey: View) {
        numberKeys.forEach { (view, digit) ->
            view.setOnClickListener { onDigitPressed(digit) }
        }
        backspaceKey.setOnClickListener { onBackspacePressed() }
    }

    private fun onDigitPressed(digit: String) {
        if (currentPin.length >= maxLength) return
        currentPin.append(digit)
        updateDots()

        if (currentPin.length == maxLength) {
            onComplete(currentPin.toString())
        }
    }

    private fun onBackspacePressed() {
        if (currentPin.isNotEmpty()) {
            currentPin.deleteCharAt(currentPin.length - 1)
            updateDots()
        }
    }

    private fun updateDots() {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < currentPin.length)
                    com.uth.taskmanagement.R.drawable.dot_filled
                else
                    com.uth.taskmanagement.R.drawable.dot_empty
            )
        }
    }

    fun reset() {
        currentPin.clear()
        updateDots()
    }
}