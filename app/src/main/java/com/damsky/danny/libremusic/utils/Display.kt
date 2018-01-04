package com.damsky.danny.libremusic.utils

import android.app.Activity
import android.content.DialogInterface
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import com.damsky.danny.libremusic.R

/**
 * This class is used to display UI elements that are not part of the layout such as
 * Toasts, Snackbars and AlertDialogs.
 *
 * @author Danny Damsky
 * @since 2018-01-04
 */

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

    private fun getDialogBuilder(): AlertDialog.Builder =
            AlertDialog.Builder(context).setIcon(R.mipmap.ic_launcher)

    private fun getDialogBuilder(title: Int): AlertDialog.Builder =
            getDialogBuilder().setTitle(title)

    private fun getDialogBuilder(title: String): AlertDialog.Builder =
            getDialogBuilder().setTitle(title)

    private fun getDialogBuilder(title: Int, message: Int): AlertDialog.Builder =
            getDialogBuilder(title).setMessage(message)

    private fun getDialogBuilder(title: String, message: Int): AlertDialog.Builder =
            getDialogBuilder(title).setMessage(message)

    private fun getDialogBuilder(title: Int, message: String): AlertDialog.Builder =
            getDialogBuilder(title).setMessage(message)

    private fun getDialogBuilder(title: String, message: String): AlertDialog.Builder =
            getDialogBuilder(title).setMessage(message)

    private fun setDialogActions(builder: AlertDialog.Builder, positiveText: Int, negativeText: Int, positiveAction: () -> Unit) {
        builder.setPositiveButton(positiveText, { dialog, _ ->
            positiveAction()
            dialog.dismiss()
        })

        builder.setNegativeButton(negativeText, { dialog, _ -> dialog.dismiss() })
    }

    private fun showDialogWithEditText(builder: AlertDialog.Builder, editText: EditText, positiveAction: () -> Unit) {
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

        builder.setView(editText)
        setDialogActions(builder, R.string.ok, R.string.cancel, positiveAction)
        val dialog = builder.create()
        dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }

    private fun showBasicDialog(builder: AlertDialog.Builder, positiveAction: () -> Unit) {
        setDialogActions(builder, R.string.yes, R.string.no, positiveAction)
        builder.create().show()
    }

    private fun showMultiChoiceDialog(builder: AlertDialog.Builder, itemList: Array<String>, boolList: BooleanArray, positiveAction: () -> Unit) {
        builder.setMultiChoiceItems(itemList, boolList, { _: DialogInterface, which: Int, isChecked: Boolean ->
            boolList[which] = isChecked
        })
        setDialogActions(builder, R.string.ok, R.string.cancel, positiveAction)
        builder.create().show()
    }

    fun showDialog(title: Int, message: Int, positiveAction: () -> Unit) =
            showBasicDialog(getDialogBuilder(title, message), positiveAction)

    fun showDialog(title: String, message: Int, positiveAction: () -> Unit) =
            showBasicDialog(getDialogBuilder(title, message), positiveAction)

    fun showDialog(title: Int, message: String, positiveAction: () -> Unit) =
            showBasicDialog(getDialogBuilder(title, message), positiveAction)

    fun showDialog(title: String, message: String, positiveAction: () -> Unit) =
            showBasicDialog(getDialogBuilder(title, message), positiveAction)

    fun showDialog(title: Int, hint: Int, editText: EditText, positiveAction: () -> Unit) {
        editText.setHint(hint)
        showDialogWithEditText(getDialogBuilder(title), editText, positiveAction)
    }

    fun showDialog(title: Int, hint: String, editText: EditText, positiveAction: () -> Unit) {
        editText.hint = hint
        showDialogWithEditText(getDialogBuilder(title), editText, positiveAction)
    }

    fun showDialog(title: String, hint: Int, editText: EditText, positiveAction: () -> Unit) {
        editText.setHint(hint)
        showDialogWithEditText(getDialogBuilder(title), editText, positiveAction)
    }

    fun showDialog(title: String, hint: String, editText: EditText, positiveAction: () -> Unit) {
        editText.hint = hint
        showDialogWithEditText(getDialogBuilder(title), editText, positiveAction)
    }

    fun showDialog(title: Int, itemList: Array<String>, boolList: BooleanArray, positiveAction: () -> Unit) =
            showMultiChoiceDialog(getDialogBuilder(title), itemList, boolList, positiveAction)

    fun showDialog(title: String, itemList: Array<String>, boolList: BooleanArray, positiveAction: () -> Unit) =
            showMultiChoiceDialog(getDialogBuilder(title), itemList, boolList, positiveAction)
}
