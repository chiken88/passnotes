package com.ivanovsky.passnotes.presentation.unlock.cells.factory

import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.factory.CellViewModelFactory
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.unlock.cells.model.DatabaseCellModel
import com.ivanovsky.passnotes.presentation.unlock.cells.viewmodel.DatabaseCellViewModel

class UnlockCellViewModelFactory : CellViewModelFactory {

    override fun createCellViewModel(
        model: BaseCellModel,
        eventProvider: EventProvider
    ): BaseCellViewModel {
        return when (model) {
            is DatabaseCellModel -> DatabaseCellViewModel(model)
            else -> throwUnsupportedModelException(model)
        }
    }
}