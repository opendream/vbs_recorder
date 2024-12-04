package th.co.opendream.vbs_recorder.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.models.Record
import th.co.opendream.vbs_recorder.utils.DateUtil
import java.util.Date

class RecordAdapter(private val context: Context, private val records: List<Record>, private val listener: OnRecordClickListener) : BaseAdapter() {
    private var showCheckBoxes = false
    private var deleteSelectedRecords = mutableListOf<Int>()

    override fun getCount(): Int = records.size

    override fun getItem(position: Int): Any = records[position]

    override fun getItemId(position: Int): Long = records[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_record_list, parent, false)
        val record = records[position]

        val titleTextView = view.findViewById<TextView>(R.id.record_title)
        val descriptionTextView = view.findViewById<TextView>(R.id.record_description)

        titleTextView.text = record.title
        if (record.createdAt != null) {
            titleTextView.text = "${DateUtil.formatLocaleDateTitle(Date(record.createdAt))} at ${DateUtil.formatLocaleTimeTitle(Date(record.createdAt))}"
        }

        val seconds = record.duration?.toLong() ?: 0
        val hours = seconds / 3600
        val minutes = (seconds / 60) % 60
        val secs = seconds % 60
        descriptionTextView.text = "Duration: ${(String.format("%02d:%02d:%02d", hours, minutes, secs))}"


        val syncedStatus = view.findViewById<ImageView>(R.id.synced_status)
        if (record.isSynced!!) {
            syncedStatus.setImageResource(R.mipmap.tabler_icon_cloud_upload)
        } else {
            syncedStatus.setImageResource(R.drawable.baseline_cloud_off_24)
        }

        val deleteCheckSelected = view.findViewById<CheckBox>(R.id.check_selected)
        if (showCheckBoxes) {
            deleteCheckSelected.visibility = View.VISIBLE
        } else {
            deleteCheckSelected.visibility = View.GONE
        }

        if (deleteSelectedRecords.contains(record.id)) {
            deleteCheckSelected.isChecked = true
        } else {
            deleteCheckSelected.isChecked = false
        }

        deleteCheckSelected.setOnClickListener {
            if (deleteCheckSelected.isChecked) {
                deleteSelectedRecords.add(record.id)
            } else {
                deleteSelectedRecords.remove(record.id)
            }

            listener.onRecordDeleteClick(deleteSelectedRecords.size > 0)
        }

        view.setOnClickListener {
            listener.onRecordClick(record)
        }

        view.setOnLongClickListener {
            listener.onRecordLongClick(record)
        }


        return view
    }

    fun setCheckBoxesVisibility(visibility: Boolean) {
        showCheckBoxes = visibility
        notifyDataSetChanged()
    }

    fun selectAllRecords() {
        deleteSelectedRecords.clear()
        records.forEach {
            deleteSelectedRecords.add(it.id)
        }
        notifyDataSetChanged()
    }

    fun deselectAllRecords() {
        deleteSelectedRecords.clear()
        notifyDataSetChanged()
    }

    fun getDeleteSelectedRecords(): List<Int> {
        return deleteSelectedRecords
    }

    fun clearDeleteSelectedRecords() {
        deleteSelectedRecords.clear()
    }

}