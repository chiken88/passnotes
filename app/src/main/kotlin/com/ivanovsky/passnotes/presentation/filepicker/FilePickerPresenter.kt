package com.ivanovsky.passnotes.presentation.filepicker

import android.Manifest
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.support.annotation.DrawableRes
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.ResourceHelper
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveAction
import com.ivanovsky.passnotes.util.formatAccordingSystemLocale
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import javax.inject.Inject

class FilePickerPresenter(private val mode: Mode,
                          rootFile: FileDescriptor,
                          private val context: Context) : FilePickerContract.Presenter {

	@Inject
	lateinit var interactor: FilePickerInteractor

	@Inject
	lateinit var errorInteractor: ErrorInteractor

	@Inject
	lateinit var permissionHelper: PermissionHelper

	@Inject
	lateinit var resourceHelper: ResourceHelper

	override val items = MutableLiveData<List<FilePickerAdapter.Item>>()
	override val screenState = MutableLiveData<ScreenState>()
	override val requestPermissionAction = SingleLiveAction<String>()
	override val doneButtonVisibility = SingleLiveAction<Boolean>()
	override val fileSelectedAction = SingleLiveAction<FileDescriptor>()
	override val snackbarMessageAction = SingleLiveAction<String>()

	private lateinit var files: List<FileDescriptor>
	private val disposables = CompositeDisposable()
	private var currentDir = rootFile
	private var isPermissionRejected  = false

	companion object {
		private const val SDCARD_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
	}

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun start() {
		if (!isPermissionRejected) {
			loadData()
		}
	}

	override fun stop() {
		disposables.clear()
	}

	override fun loadData() {
		screenState.value = ScreenState.loading()
		doneButtonVisibility.value = false

		if (permissionHelper.isPermissionGranted(SDCARD_PERMISSION)) {
			val disposable = interactor.getFileList(currentDir)
					.subscribe { result -> onFilesLoaded(currentDir, result) }

			disposables.add(disposable)
		} else {
			requestPermissionAction.call(SDCARD_PERMISSION)
		}
	}

	private fun onFilesLoaded(dir: FileDescriptor, result: OperationResult<List<FileDescriptor>>) {
		if (result.result != null) {
			val unsortedFiles = result.result

			if (!dir.isRoot) {
				val disposable = interactor.getParent(currentDir)
						.subscribe { parentResult -> onParentLoaded(unsortedFiles, parentResult)}

				disposables.add(disposable)
			} else {

				files = sortFiles(unsortedFiles)

				items.value = createAdapterItems(files, null)
				screenState.value = ScreenState.data()
				doneButtonVisibility.value = true
			}
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.value = ScreenState.error(message)
			doneButtonVisibility.value = false
		}
	}

	private fun onParentLoaded(unsortedFiles: List<FileDescriptor>, result: OperationResult<FileDescriptor>) {
		if (result.result != null) {
			val parent = result.result

			val sortedFiles = sortFiles(unsortedFiles).toMutableList()
			sortedFiles.add(0, parent)

			files = sortedFiles

			items.value = createAdapterItems(sortedFiles, parent)
			screenState.value = ScreenState.data()
			doneButtonVisibility.value = true
		} else {
			val message = errorInteractor.processAndGetMessage(result.error)
			screenState.value = ScreenState.error(message)
			doneButtonVisibility.value = false
		}
	}

	private fun sortFiles(files: List<FileDescriptor>): List<FileDescriptor> {
		return files.sortedWith(Comparator { lhs, rhs ->
			val result: Int

			if ((lhs.isDirectory && !rhs.isDirectory) || (!lhs.isDirectory && rhs.isDirectory)) {
				result = if (lhs.isDirectory) -1 else 1
			} else {//if files have same type
				result = lhs.name.compareTo(rhs.name)
			}

			result
		})
	}

	private fun createAdapterItems(files: List<FileDescriptor>, parent: FileDescriptor?): List<FilePickerAdapter.Item> {
		val items = mutableListOf<FilePickerAdapter.Item>()

		for (file in files) {
			val iconResId = getIconResId(file.isDirectory)
			val title = if (file == parent) ".." else formatItemTitle(file)
			val description = formatModifiedDate(file.modified)

			items.add(FilePickerAdapter.Item(iconResId, title, description, false))
		}

		return items
	}

	private fun formatModifiedDate(modified: Date?): String {
		return if (modified != null) modified.formatAccordingSystemLocale(context) else ""
	}

	@DrawableRes
	private fun getIconResId(isDirectory: Boolean): Int {
		return if (isDirectory) R.drawable.ic_folder_white_24dp else R.drawable.ic_file_white_24dp
	}

	private fun formatItemTitle(file: FileDescriptor): String {
		return if (file.isDirectory) file.name + "/" else file.name
	}

	override fun onPermissionResult(granted: Boolean) {
		if (granted) {
			loadData()
		} else {
			//TODO: somehow user should see retry button
			isPermissionRejected = true
			screenState.value = ScreenState.error(
					resourceHelper.getString(R.string.application_requires_external_storage_permission))
			doneButtonVisibility.value = false
		}
	}

	override fun onItemClicked(position: Int) {
		val selectedFile = files[position]

		if (selectedFile.isDirectory) {
			currentDir = files[position]

			loadData()
		} else if (mode == Mode.PICK_FILE) {
			val items = this.items.value!!

			if (items[position].selected) {
				items[position].selected = false
			} else {
				items.forEach { item -> item.selected = false }
				items[position].selected = true
			}

			this.items.value = items
		}
	}

	override fun onDoneButtonClicked() {
		if (mode == Mode.PICK_DIRECTORY) {
			fileSelectedAction.call(currentDir)

		} else if (mode == Mode.PICK_FILE) {
			if (isAnyFileSelected()) {
				fileSelectedAction.call(getSelectedFile())
			} else {
				snackbarMessageAction.call(context.getString(R.string.please_select_any_file))
			}
		}
	}

	private fun isAnyFileSelected(): Boolean {
		return items.value!!.any { item -> item.selected }
	}

	private fun getSelectedFile(): FileDescriptor {
		val position = items.value!!.indexOfFirst { item -> item.selected }
		return files[position]
	}
}