package th.co.opendream.vbs_recorder.adapters

import th.co.opendream.vbs_recorder.models.Record

interface OnRecordClickListener {
    fun onRecordClick(record: Record)
    fun onRecordLongClick(record: Record): Boolean

    fun onRecordDeleteClick(isActive: Boolean)

}