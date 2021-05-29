package com.ivanovsky.passnotes.data.repository.file.regular

import android.content.Context
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.ServerCredentials
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator
import com.ivanovsky.passnotes.data.repository.file.exception.IncorrectUseException

class RegularFileSystemAuthenticator : FileSystemAuthenticator {

    override fun getAuthType() = AuthType.NO_AUTH

    override fun getFsAuthority() = FSAuthority.REGULAR_FS_AUTHORITY

    override fun isAuthenticationRequired() = false

    override fun startAuthActivity(context: Context) {
        throw IncorrectUseException()
    }

    override fun setCredentials(credentials: ServerCredentials?) {
        throw IncorrectUseException()
    }
}