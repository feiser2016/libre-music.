package com.damsky.danny.libremusic.utils

import android.app.Activity
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import com.damsky.danny.libremusic.R

class Display {
    private val context: Activity
    private val view: View

    constructor(context: Activity) {
        this.context = context
        this.view = this.context.findViewById(android.R.id.content)
    }

    constructor(context: Activity, view: View) {
        this.context = context
        this.view = view
    }

    fun showToast(resourceId: Int, length: Int) {
        Toast.makeText(context.applicationContext, resourceId, length).show()
    }

    fun showToast(text: String, length: Int) {
        Toast.makeText(context.applicationContext, text, length).show()
    }

    fun showSnack(resourceId: Int, length: Int) {
        Snackbar.make(view, resourceId, length).show()
    }

    fun showSnack(text: String, length: Int) {
        Snackbar.make(view, text, length).show()
    }

    fun getDialogBuilder(title: Int): AlertDialog.Builder =
            AlertDialog.Builder(context)
                    .setTitle(title)
                    .setIcon(R.mipmap.ic_launcher)

    fun getDialogBuilder(title: Int, message: Int): AlertDialog.Builder =
            AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setIcon(R.mipmap.ic_launcher)

    fun showDialog(title: Int, message: Int, positiveAction: () -> Unit) {
        val builder = getDialogBuilder(title, message)
        builder.setPositiveButton(R.string.yes, { dialog, _ ->
            positiveAction()
            dialog.dismiss()
        })
        builder.setNegativeButton(R.string.no, { dialog, _ -> dialog.dismiss() })
        builder.create().show()
    }

    fun showDialog(editText: EditText, hint: Int, title: Int, positiveAction: () -> Unit) {
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        editText.setHint(hint)

        val builder = getDialogBuilder(title)
        builder.setView(editText)
        builder.setPositiveButton(R.string.ok, { dialog, _ ->
            positiveAction()
            dialog.dismiss()
        })
        builder.setNegativeButton(R.string.cancel, { dialog, _ -> dialog.dismiss() })
        val dialog = builder.create()
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }


}
