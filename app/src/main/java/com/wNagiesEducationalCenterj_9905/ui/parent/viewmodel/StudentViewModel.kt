package com.wNagiesEducationalCenterj_9905.ui.parent.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.wNagiesEducationalCenterj_9905.api.request.ParentComplaintRequest
import com.wNagiesEducationalCenterj_9905.common.DBEntities
import com.wNagiesEducationalCenterj_9905.common.extension.getCurrentDateTime
import com.wNagiesEducationalCenterj_9905.common.extension.toString
import com.wNagiesEducationalCenterj_9905.common.utils.PreferenceProvider
import com.wNagiesEducationalCenterj_9905.common.utils.ProfileLabel
import com.wNagiesEducationalCenterj_9905.data.db.Entities.*
import com.wNagiesEducationalCenterj_9905.data.repository.StudentRepository
import com.wNagiesEducationalCenterj_9905.viewmodel.BaseViewModel
import com.wNagiesEducationalCenterj_9905.vo.DownloadRequest
import com.wNagiesEducationalCenterj_9905.vo.Profile
import com.wNagiesEducationalCenterj_9905.vo.Resource
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class StudentViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val preferenceProvider: PreferenceProvider
) : BaseViewModel() {
    val cachedToken: MutableLiveData<String> = MutableLiveData()
    val cachedMessage: MutableLiveData<MessageEntity> = MutableLiveData()
    val cachedLabels: MutableLiveData<MutableList<Pair<Profile, String?>>> = MutableLiveData()
    var cachedSavedComplaint: MutableLiveData<Resource<List<ComplaintEntity>>> = MutableLiveData()
    val isSuccess: MutableLiveData<Boolean> = MutableLiveData()
    val cachedSavedComplaintById: MutableLiveData<ComplaintEntity> = MutableLiveData()
    val errorMessage: MutableLiveData<Int> = MutableLiveData()
    val cachedFileName: MutableLiveData<String> = MutableLiveData()

    fun getStudentMessages(token: String, shouldFetch: Boolean = false): LiveData<Resource<List<MessageEntity>>> {
        return studentRepository.fetchStudentMessages(token, shouldFetch)
    }

    fun getUserToken() {
        disposable.addAll(
            Flowable.just(preferenceProvider.getUserToken())
                .map {
                    return@map it
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    cachedToken.value = it
                    Timber.i("token: $it")
                }, {})
        )
    }

    fun getMessageById(messageId: Int) {
        disposable.addAll(
            studentRepository.getMessageById(messageId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cachedMessage.value = it }, {})
        )

    }

    fun getStudentProfile(token: String): LiveData<Resource<StudentProfileEntity>> {
        return studentRepository.fetchStudentProfile(token)
    }

    fun setProfileLabels(data: StudentProfileEntity?) {
        disposable.addAll(
            Observable.create<MutableList<Pair<Profile, String?>>> {
                val myList = mutableListOf<Pair<Profile, String?>>()
                val profileData = arrayListOf(
                    data?.studentNo, data?.studentName, data?.dob,
                    data?.gender, data?.admissionDate, data?.section,
                    data?.semester, data?.level, data?.guardian,
                    data?.contact, data?.faculty, data?.index
                )
                val label = ArrayList<String>()
                val drawable = ArrayList<Int>()
                for (student in ProfileLabel.getMultiple()) {
                    label.add(student.first)
                    drawable.add(student.second)
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

    fun sendComplaint(parentComplaintRequest: ParentComplaintRequest) {
        disposable.addAll(
            Single.just(preferenceProvider.getUserToken())
                .map {
                    Timber.i("single token $it")
                    return@map it
                }
                .flatMap {
                    studentRepository.sendParentComplaint(it, parentComplaintRequest)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.status == 200) {
                        savingComplaintToDb(parentComplaintRequest)
                        isSuccess.value = true
                        return@subscribe
                    }
                    isSuccess.value = false

                }, {
                    Timber.i("send error: $it")
                    errorMessage.value = com.wNagiesEducationalCenterj_9905.R.string.errorConnection
                })

        )
    }

    private fun savingComplaintToDb(parentComplaintRequest: ParentComplaintRequest) {
        disposable.addAll(
            Single.just(preferenceProvider.getUserToken())
                .map {
                    return@map it
                }
                .flatMap {
                    val date = getCurrentDateTime().toString("yyyy/MM/dd")
                    val complaint =
                        ComplaintEntity(parentComplaintRequest.content, date, it)
                    return@flatMap studentRepository.saveComplaintMessage(complaint)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.i("saved data id: $it")
                }, {})
        )
    }

    fun getSavedParentComplaint() {
        disposable.addAll(
            Flowable.just(preferenceProvider.getUserToken()).map {
                return@map it
            }
                .flatMap {
                    return@flatMap studentRepository.getSavedParentComplaints(it)
                }
                .map {
                    if (it.isEmpty()) return@map Resource.error("No content available", null)
                    return@map Resource.success(it)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    cachedSavedComplaint.value = Resource.loading(null)
                    Timber.i("parent complaint messages: ${it.data?.size}")
                    cachedSavedComplaint.value = it
                }, {})
        )
    }

    fun getSavedParentComplaintById(complaint_id: Int) {
        disposable.addAll(
            studentRepository.getSavedParentComplaintsById(complaint_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    cachedSavedComplaintById.value = it
                }, {})
        )
    }

    fun downloadFilesFromServer(filePath: DownloadRequest) {
        disposable.addAll(
            Observable.just(preferenceProvider.getUserToken())
                .map {
                    return@map it
                }
                .flatMap {
                    return@flatMap studentRepository.fetchFileFromServer(it, filePath)
                }


                .doOnSubscribe { isSuccess.postValue(false) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val headerFileName = it.headers().get("Content-Disposition")
                    val filename = headerFileName?.replace("attachment; filename=", "")
                    cachedFileName.value = filename
                    Timber.i("row $it updated")
                }, { Timber.i(it, "get assignment err") })
        )
    }

    fun getStudentAssignmentPDF(token: String): LiveData<Resource<List<AssignmentEntity>>> {
        return studentRepository.fetchStudentAssignmentPDF(token)
    }

    fun getStudentAssignmentImage(
        token: String,
        shouldFetch: Boolean = false
    ): LiveData<Resource<List<AssignmentEntity>>> {
        return studentRepository.fetchStudentAssignmentImage(token, shouldFetch)
    }

    fun deleteFileById(id: Int?, path: String?, entity: DBEntities) {
        disposable.addAll(
            Observable.create<String> {
                when (entity) {
                    DBEntities.ASSIGNMENT -> {
                        id?.let { it1 -> studentRepository.deleteAssignmentById(it1) }
                    }
                    DBEntities.REPORT -> {
                        id?.let { it1 -> studentRepository.deleteReportById(it1) }
                    }
                    DBEntities.CIRCULAR -> {}
                }
                path?.let { p ->
                    val file = File(p)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                it.onNext("finish")
                it.onComplete()
            }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.i("delete $it")
                }, { Timber.i(it) })
        )
    }

    fun getStudentReportPDF(token: String): LiveData<Resource<List<ReportEntity>>> {
        return studentRepository.fetchStudentReportPDF(token)
    }

    fun getStudentReportImage(token: String): LiveData<Resource<List<ReportEntity>>> {
        return studentRepository.fetchStudentReportImage(token)
    }

    fun saveDownloadFilePathToDb(id: Int?, path: String?, entity: DBEntities) {
        disposable.addAll(
            Observable.fromCallable {
                when (entity) {
                    DBEntities.ASSIGNMENT -> {
                        id?.let { path?.let { it1 -> studentRepository.updateStudentAssignmentFilePath(it, it1) } }
                    }
                    DBEntities.REPORT -> {
                        id?.let { path?.let { it1 -> studentRepository.updateStudentReportFilePath(it, it1) } }
                    }
                    DBEntities.CIRCULAR -> {
                        Timber.i("here $path")
                        id?.let { path?.let { it1 -> studentRepository.updateCircularFilePath(it, it1) } }
                    }
                }
            }
                .doOnComplete { isSuccess.postValue(true) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.i("data is saved to db $it")
                }, {})
        )

    }

    fun getClassTeacher(token: String): LiveData<Resource<List<StudentTeacherEntity>>> {
        return studentRepository.getClassTeacher(token)
    }

    fun getCircularInformation(token: String, shouldFetch: Boolean = false): LiveData<Resource<List<CircularEntity>>> {
        return studentRepository.fetchCircular(token, shouldFetch)
    }

}