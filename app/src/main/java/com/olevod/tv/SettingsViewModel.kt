package com.olevod.tv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.olevod.tv.data.AppRelease
import com.olevod.tv.data.GitHubUpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

internal sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Current : UpdateState
    data class Available(val release: AppRelease) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data class Ready(val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}
internal class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GitHubUpdateRepository()
    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    fun check() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            mutableState.value = UpdateState.Checking
            try {
                mutableState.value = repository.latest(BuildConfig.VERSION_NAME)?.let { UpdateState.Available(it) } ?: UpdateState.Current
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(e) }
        }
    }
    fun download(release: AppRelease) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            mutableState.value = UpdateState.Downloading(0)
            try {
                val file = repository.download(getApplication(), release) { mutableState.value = UpdateState.Downloading(it) }
                mutableState.value = UpdateState.Ready(file)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(e) }
        }
    }
    private fun fail(error: Exception) {
        mutableState.value = UpdateState.Error(if (error is IOException) "网络连接或文件读写失败，请检查网络和剩余空间后重试" else error.message ?: "更新失败，请重试")
    }
}
