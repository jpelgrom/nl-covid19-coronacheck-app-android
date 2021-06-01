/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.ctr.appconfig

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.ctr.appconfig.models.AppStatus
import nl.rijksoverheid.ctr.appconfig.models.ConfigResult
import nl.rijksoverheid.ctr.appconfig.persistence.StorageResult
import nl.rijksoverheid.ctr.appconfig.usecases.AppConfigUseCase
import nl.rijksoverheid.ctr.appconfig.usecases.AppStatusUseCase
import nl.rijksoverheid.ctr.appconfig.usecases.LoadPublicKeysUseCase
import nl.rijksoverheid.ctr.appconfig.usecases.PersistConfigUseCase
import nl.rijksoverheid.ctr.shared.MobileCoreWrapper

abstract class AppConfigViewModel : ViewModel() {
    val appStatusLiveData = MutableLiveData<AppStatus>()

    abstract fun refresh(mobileCoreWrapper: MobileCoreWrapper)
}

class AppConfigViewModelImpl(
    private val appConfigUseCase: AppConfigUseCase,
    private val appStatusUseCase: AppStatusUseCase,
    private val persistConfigUseCase: PersistConfigUseCase,
    private val loadPublicKeysUseCase: LoadPublicKeysUseCase,
    private val cacheDirPath: String,
    private val versionCode: Int
) : AppConfigViewModel() {

    override fun refresh(mobileCoreWrapper: MobileCoreWrapper) {
        viewModelScope.launch {
            val configResult = appConfigUseCase.get()
            val appStatus = appStatusUseCase.get(configResult, versionCode)
            if (configResult is ConfigResult.Success) {
                persistConfigUseCase.persist(
                    appConfig = configResult.appConfig,
                    publicKeys = configResult.publicKeys
                )
                loadPublicKeysUseCase.load(
                    publicKeys = configResult.publicKeys
                )
                mobileCoreWrapper.initializeVerifier(cacheDirPath)
            }
            appStatusLiveData.postValue(appStatus)
        }
    }
}
