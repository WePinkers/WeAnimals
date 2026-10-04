package com.example.weanimals.core.formatting

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

object BrazilianDocumentMask {

    fun attachCpf(editText: EditText) {
        attach(editText, CPF_DIGITS, ::formatCpf)
    }

    fun attachCnpj(editText: EditText) {
        attach(editText, CNPJ_DIGITS, ::formatCnpj)
    }

    private fun attach(
        editText: EditText,
        maxDigits: Int,
        formatter: (String) -> String
    ) {
        var formatting = false
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                text: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = Unit

            override fun afterTextChanged(editable: Editable?) {
                if (formatting || editable == null) return

                val digits = editable.toString()
                    .filter(Char::isDigit)
                    .take(maxDigits)
                val formatted = formatter(digits)
                if (editable.toString() == formatted) return

                formatting = true
                editText.setText(formatted)
                editText.setSelection(formatted.length)
                formatting = false
            }
        })
    }

    private fun formatCpf(digits: String): String = buildString {
        digits.forEachIndexed { index, digit ->
            if (index == 3 || index == 6) append('.')
            if (index == 9) append('-')
            append(digit)
        }
    }

    private fun formatCnpj(digits: String): String = buildString {
        digits.forEachIndexed { index, digit ->
            if (index == 2 || index == 5) append('.')
            if (index == 8) append('/')
            if (index == 12) append('-')
            append(digit)
        }
    }

    private const val CPF_DIGITS = 11
    private const val CNPJ_DIGITS = 14
}
