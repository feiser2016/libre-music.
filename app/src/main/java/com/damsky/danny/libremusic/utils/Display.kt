package com.damsky.danny.libremusic.utils

import android.app.Activity
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.Toast

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


}
