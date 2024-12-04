package th.co.opendream.vbs_recorder.services

interface RecordServiceListener {
    fun onRecordFrameCreated(isFinished: Boolean = false)

    fun onFinished()
}


