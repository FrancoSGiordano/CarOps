package com.example.trabajointegrador_modulonativo.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class CurrencyTextWatcher (ediText: EditText) : TextWatcher {
    private val editTextWeakReference: WeakReference<EditText> = WeakReference(ediText)
    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int){

    }

    override fun afterTextChanged(editable: Editable?) {
        val editText = editTextWeakReference.get() ?: return
        val text = editable.toString()

        if (text.isEmpty() || isAlreadyFormatted(text)) {
            return
        }

        val cleanString = text.replace("[$,.\\s]".toRegex(), "")

        if(cleanString.isEmpty()){
            return
        }

        try {
            val parsed = BigDecimal(cleanString)
                .setScale(2, RoundingMode.FLOOR)
                .divide(BigDecimal(100), RoundingMode.FLOOR)

            val formatted = currencyFormat.format(parsed)

            editText.removeTextChangedListener(this)
            editText.setText(formatted)
            editText.setSelection(formatted.length)
            editText.addTextChangedListener(this)

        } catch (e: NumberFormatException) {

        }

    }

    private fun isAlreadyFormatted(text: String): Boolean {
        return text.startsWith(currencyFormat.currency?.symbol ?: "$")
    }


}