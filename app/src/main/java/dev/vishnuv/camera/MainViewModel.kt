package dev.vishnuv.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _recordingStatus = MutableLiveData<Boolean>()
    val recordingStatus: LiveData<Boolean>
        get() = _recordingStatus

    fun setRecordingStatus(status: Boolean) {
        _recordingStatus.value = status
    }

}