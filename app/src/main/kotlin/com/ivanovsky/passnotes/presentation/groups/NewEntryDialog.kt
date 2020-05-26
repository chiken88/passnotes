package com.ivanovsky.passnotes.presentation.groups

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.injection.Injector
import javax.inject.Inject

class NewEntryDialog : DialogFragment(), DialogInterface.OnClickListener {

    @Inject
    lateinit var resourceHelper: ResourceHelper

    lateinit var onItemClickListener: (itemIndex: Int) -> Unit

    init {
        Injector.getInstance().appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arrayOf(
            resourceHelper.getString(R.string.new_item_entry_new_group),
            resourceHelper.getString(R.string.new_item_entry_new_note)
        )

        return AlertDialog.Builder(context)
            .setTitle(resourceHelper.getString(R.string.create))
            .setItems(items, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        onItemClickListener.invoke(which)
    }

    companion object {
        val TAG = NewEntryDialog::class.java.simpleName
    }
}