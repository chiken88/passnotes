package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.sharing.FileMemberActionError;
import com.ivanovsky.passnotes.data.entity.DropboxFile;
import com.ivanovsky.passnotes.data.repository.file.RemoteFileOutputStream;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxException;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxNetworkException;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class DropboxFileOutputStream extends RemoteFileOutputStream {

	private static final String TAG = DropboxFileOutputStream.class.getSimpleName();

	private final DropboxFileSystemProvider provider;
	private final DropboxClient client;
	private final OutputStream out;
	private final DropboxFile file;
	private final File outFile;
	private boolean failed;

	DropboxFileOutputStream(DropboxFileSystemProvider provider,
							DropboxClient client,
							DropboxFile file) throws FileNotFoundException {
		this.provider = provider;
		this.client = client;
		this.file = file;
		this.outFile = new File(file.getLocalPath());
		this.out = new BufferedOutputStream(new FileOutputStream(outFile));
	}

	@Override
	public void write(int b) throws IOException {
		try {
			out.write(b);
		} catch (IOException e) {
			Logger.printStackTrace(e);

			failed = true;

			provider.onFileUploadFailed(file);

			throw new IOException(e);
		}
	}

	@Override
	public void flush() throws IOException {
		try {
			out.flush();
		} catch (IOException e) {
			Logger.printStackTrace(e);

			failed = true;

			provider.onFileUploadFailed(file);

			throw new IOException(e);
		}
	}

	@Override
	public void close() throws IOException {
		if (failed) {
			return;
		}

		FileMetadata metadata;

		try {
			metadata = client.uploadFileOrThrow(file.getRemotePath(), file.getLocalPath());
		} catch (DropboxNetworkException e) {
			Logger.printStackTrace(e);

			provider.onOfflineWriteFinished(file);

			throw new IOException(e);

		} catch (DropboxException e) {
			Logger.printStackTrace(e);

			provider.onFileUploadFailed(file);

			throw new IOException(e);
		}

		provider.onFileUploadFinished(file, metadata);
	}

	@Override
	public File getOutputFile() {
		return outFile;
	}
}
