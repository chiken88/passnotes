package com.ivanovsky.passnotes.presentation.storagelist

import android.arch.lifecycle.MutableLiveData
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.domain.entity.StorageOptionType
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class StorageListPresenter(private val mode: Mode) :
		StorageListContract.Presenter {

	@Inject
	lateinit var interactor: StorageListInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var fileSystemResolver: FileSystemResolver

	@Inject
	lateinit var resourceHelper: ResourceHelper

	override val storageOptions = MutableLiveData<List<StorageOption>>()
	override val screenState = MutableLiveData<ScreenState>()
	override val showFilePickerScreenAction = SingleLiveAction<Pair<FileDescriptor, Mode>>()
	override val fileSelectedAction = SingleLiveAction<FileDescriptor>()
	override val authActivityStartedAction = SingleLiveAction<FSType>()

	private var isDropboxAuthDisplayed = false
	private val disposables = CompositeDisposable()

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		if (isDropboxAuthDisplayed) {
			// case when user returns to the application after dropbox login
			isDropboxAuthDisplayed = false

			val provider = fileSystemResolver.resolveProvider(FSType.DROPBOX)
			if (provider.authenticator.isAuthenticationRequired) {
				screenState.value = ScreenState.dataWithError(
						resourceHelper.getString(R.string.authentication_failed))
			} else {
				val disposable = interactor.getDropboxRoot()
						.subscribe { result -> onDropboxRootLoaded(result)}

				disposables.add(disposable)
			}

		} else {
			storageOptions.value = interactor.getAvailableStorageOptions()
			screenState.value = ScreenState.data()
		}
	}

	override fun stop() {
		disposables.clear()
	}

	override fun onStorageOptionClicked(option: StorageOption) {
		when (option.type) {
			StorageOptionType.PRIVATE_STORAGE -> onPrivateStorageSelected(option.root!!)
			StorageOptionType.EXTERNAL_STORAGE -> onExternalStorageSelected(option.root!!)
			StorageOptionType.DROPBOX -> onDropboxStorageSelected()
		}
	}

	private fun onPrivateStorageSelected(root: FileDescriptor) {
		fileSelectedAction.call(root)
	}

	private fun onExternalStorageSelected(root: FileDescriptor) {
		showFilePickerScreenAction.call(Pair(root, mode))
	}

	private fun onDropboxStorageSelected() {
		val provider = fileSystemResolver.resolveProvider(FSType.DROPBOX)

		screenState.value = ScreenState.loading()

		if (provider.authenticator.isAuthenticationRequired) {
			isDropboxAuthDisplayed = true
			authActivityStartedAction.call(FSType.DROPBOX)

		} else {
			val disposable = interactor.getDropboxRoot()
					.subscribe { result -> onDropboxRootLoaded(result)}

			disposables.add(disposable)
		}
	}

	override fun onFilePicked(file: FileDescriptor) {
		fileSelectedAction.call(file)
	}

	private fun onDropboxRootLoaded(result: OperationResult<FileDescriptor>) {
		if (result.result != null) {
			val rootFile = result.result
			showFilePickerScreenAction.call(Pair(rootFile, mode))

		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.value = ScreenState.error(message)
		}
	}
}