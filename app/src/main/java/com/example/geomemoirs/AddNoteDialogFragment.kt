package com.example.geomemoirs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment

class AddNoteDialogFragment : DialogFragment() {

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.fragment_add_note_dialog, null)

        val builder = AlertDialog.Builder(activity).setView(dialogView)
        val dialog = builder.create()
        return dialog
    }
}
