package com.ivanovsky.passnotes.presentation.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ivanovsky.passnotes.R

class ExpandableFloatingActionButton @JvmOverloads constructor(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    lateinit var onItemClickListener: (position: Int) -> Unit
    private val mainFab: FloatingActionButton
    private val fabContainer: ViewGroup
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var isCollapsed = true

    init {
        val view = inflater.inflate(R.layout.core_expandable_fab, this, true)

        mainFab = view.findViewById(R.id.fab_main)
        fabContainer = view.findViewById(R.id.fab_container)

        mainFab.setImageResource(R.drawable.ic_add_white_24dp)
        mainFab.setOnClickListener { onMainFabClicked() }

        fabContainer.isVisible = false
    }

    fun inflate(entries: List<String>) {
        for ((idx, entry) in entries.withIndex()) {
            val innerFab = createFabItem(entry)

            innerFab.setOnClickListener {
                onItemClickListener.invoke(idx)
                collapse()
            }

            fabContainer.addView(innerFab)
        }
    }

    private fun createFabItem(text: String): ExtendedFloatingActionButton {
        val fab = inflater.inflate(R.layout.core_expandable_fab_item, fabContainer, false) as ExtendedFloatingActionButton
        fab.text = text
        return fab
    }

    @VisibleForTesting
    fun onMainFabClicked() {
        if (isCollapsed) {
            expand()
        } else {
            collapse()
        }
    }

    @VisibleForTesting
    fun expand() {
        mainFab.setImageResource(R.drawable.ic_expand_more_white_24dp)
        fabContainer.isVisible = true

        isCollapsed = false
    }

    @VisibleForTesting
    fun collapse() {
        mainFab.setImageResource(R.drawable.ic_add_white_24dp)
        fabContainer.isVisible = false

        isCollapsed = true
    }
}