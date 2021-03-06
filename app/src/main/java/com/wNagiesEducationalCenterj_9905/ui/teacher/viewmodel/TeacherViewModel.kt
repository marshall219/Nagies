package com.wNagiesEducationalCenterj_9905.ui.teacher.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.wNagiesEducationalCenterj_9905.api.request.ExplorerRequest
import com.wNagiesEducationalCenterj_9905.api.request.TeacherMessageRequest
import com.wNagiesEducationalCenterj_9905.api.response.ExplorerDeleteResponse
import com.wNagiesEducationalCenterj_9905.common.utils.PreferenceProvider
import com.wNagiesEducationalCenterj_9905.common.utils.ProfileLabel
import com.wNagiesEducationalCenterj_9905.data.db.Entities.*
import com.wNagiesEducationalCenterj_9905.data.repository.TeacherRepository
import com.wNagiesEducationalCenterj_9905.viewmodel.BaseViewModel
import com.wNagiesEducationalCenterj_9905.vo.IFileModel
import com.wNagiesEducationalCenterj_9905.vo.Profile
import com.wNagiesEducationalCenterj_9905.vo.Resource
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class TeacherViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository,
    private val preferenceProvider: PreferenceProvider
) : BaseViewModel() {
    var userToken: MutableLiveData<String> = MutableLiveData()
    var cachedComplaint: MutableLiveData<ComplaintEntity> = MutableLiveData()
    val cachedMessage: MutableLiveData<MessageEntity> = MutableLiveData()
    var cachedAnnouncement: MutableLiveData<AnnouncementEntity> = MutableLiveData()
    val cachedLabels: MutableLiveData<MutableList<Pair<Profile, String?>>> = MutableLiveData()
    val isSuccess: MutableLiveData<Boolean> = MutableLiveData()
    val errorMessage: MutableLiveData<Int> = MutableLiveData()
    val searchString: MutableLiveData<String> = MutableLiveData()
    var cachedUploadData: MutableLiveData<List<IFileModel>> = MutableLiveData()
    var deleteUploadResponse: MutableLiveData<ExplorerDeleteResponse> = MutableLiveData()

    fun getTeacherProfile(token: String): LiveData<Resource<TeacherProfileEntity>> {
        return teacherRepository.fetchTeacherProfile(token)
    }

    fun getUserToken() {
        disposable.addAll(
            Single.just(preferenceProvider.getUserToken())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    userToken.value = it
                    Timber.i("token: $it")
                }, { err -> Timber.i(err) })
        )
    }

    fun getComplaintMessage(
        token: String,
        shouldFetch: Boolean,
        searchContent: String = ""
    ): LiveData<Resource<List<ComplaintEntity>>> {
        return teacherRepository.fetchComplaint(token, shouldFetch, searchContent)
    }

    fun getAnnouncementMessage(
        token: String,
        shouldFetch: Boolean = false,
        searchContent: String = ""
    ): LiveData<Resource<List<AnnouncementEntity>>> {
        return teacherRepository.fetchAnnouncement(token, shouldFetch, searchContent)
    }

    fun getComplaintMessageById(id: Int) {
        disposable.addAll(
            teacherRepository.getComplaintMessageById(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    cachedComplaint.value = it
                }, { err -> Timber.i(err) })
        )
    }

    fun getAnnouncementById(id: Int) {
        disposable.addAll(
            teacherRepository.getAnnouncementById(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    cachedAnnouncement.value = it
                }, { err -> Timber.i(err) })
        )

    }

    fun setProfileLabels(data: TeacherProfileEntity?) {
        disposable.addAll(
            Observable.create<MutableList<Pair<Profile, String?>>> {
                val myList = mutableListOf<Pair<Profile, String?>>()
                val profileData = arrayListOf(
                    data?.ref, data?.name, data?.dob,
                    data?.gender, data?.admissionDate, data?.facultyName,
                    data?.level, data?.contact, data?.username
                )
                val label = ArrayList<String>()
                val drawable = ArrayList<Int>()
                for (teacher in ProfileLabel.getLabelTeacher()) {
                    label.add(teacher.first)
                    drawable.add(teacher.second)
                }

                for (i in profileData.indices) {
                    myList.add(Pair(Profile(label[i], drawable[i]), profileData[i]))
                }
                it.onNext(myList)
                it.onComplete()

            }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    cachedLabels.value = it
                }, {})
        )
    }

    fun sendTeacherMessage(teacherMessageRequest: TeacherMessageRequest) {
        disposable.addAll(Single.just(preferenceProvider.getUserToken())
            .map { token ->
                return@map token
            }
            .flatMap { token ->
                teacherRepository.sendTeacherMessage(token, teacherMessageRequest)
            }
            .doOnError { isSuccess.postValue(false) }
            .doOnSuccess { isSuccess.postValue(true) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.i("comp $it")
                if (it.status == 200) {
                    Timber.i("message sent $it")
                }
            }, { err ->
                Timber.i(err)
                errorMessage.value = com.wNagiesEducationalCenterj_9905.R.string.errorConnection
            })
        )
    }

    fun getSentMessages(
        token: String,
        shouldFetch: Boolean = false,
        search: String = ""
    ): LiveData<Resource<List<MessageEntity>>> {
        return teacherRepository.fetchSentMessages(token, shouldFetch, search)
    }

    fun getClassStudent(
        token: String,
        search: String = ""
    ): LiveData<Resource<List<ClassStudentEntity>>> {
        return teacherRepository.fetchClassStudent(token, searchName = search)
    }

    fun getUploadedFiles(request: ExplorerRequest) {
        disposable.addAll(Observable.just(preferenceProvider.getUserToken())
            .map { return@map it }
            .flatMap { token ->
                teacherRepository.fetchUploadData(token, request)
            }

            .doOnError {
                isSuccess.postValue(false)
                cachedUploadData.postValue(null)
            }
            .doOnComplete { isSuccess.postValue(false) }
            .doOnSubscribe { isSuccess.postValue(true) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                Timber.i("resp $it")
                cachedUploadData.value = it.dataUpload
                Timber.i("status: ${it.status} data size: ${it.count}")
            }, { err -> Timber.i(err, "get upload file err") })
        )
    }

    fun deleteUploadedFiles(request: ExplorerRequest) {
        disposable.addAll(Single.just(preferenceProvider.getUserToken())
            .map { return@map it }
            .flatMap { token ->
                teacherRepository.deleteUploadData(token, request)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                deleteUploadResponse.value = it
                if (it.status == 200) {
                    getUploadedFiles(request)
                }
                Timber.i("status: ${it.status} message: ${it.message}")

            }, { err -> Timber.i(err, "delete uploaded file err") })
        )
    }

    fun getMessageById(messageId: Int) {
        disposable.addAll(
            teacherRepository.getMessageById(messageId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cachedMessage.value = it }, {})
        )

    }

    fun deleteMessage(id: Int?) {
        disposable.addAll(Single.just(preferenceProvider.getUserToken())
            .map { return@map it }
            .flatMap { teacherRepository.deleteSentMessage(it, id) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it.status == 200) {
                    Timber.i("message deleted success")
                }
            }, { err -> Timber.i(err, "deleteMessage") })
        )
    }

}