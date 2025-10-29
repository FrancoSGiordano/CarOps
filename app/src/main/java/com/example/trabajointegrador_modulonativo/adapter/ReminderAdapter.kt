package com.example.trabajointegrador_modulonativo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trabajointegrador_modulonativo.R
import com.example.trabajointegrador_modulonativo.model.Reminder
import java.text.SimpleDateFormat
import java.util.*

class ReminderAdapter(
    private val onDelete: (Reminder) -> Unit = {},
    private val onEdit: (Reminder) -> Unit = {}
) : ListAdapter<Reminder, ReminderAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Reminder>() {
            override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
                return oldItem == newItem
            }
        }

        private val TIME_FMT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.reminder_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reminder = getItem(position)
        holder.bind(reminder)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTv: TextView = itemView.findViewById(R.id.reminderTitle)
        private val dateTv: TextView = itemView.findViewById(R.id.reminderDate)
        private val editBtn: ImageButton? = itemView.findViewById(R.id.editButton)


        fun bind(r: Reminder) {
            titleTv.text = r.title
            val ts = r.notifyAt?.toDate()
            dateTv.text = if (ts != null) TIME_FMT.format(ts) else ""

            editBtn?.setOnClickListener { onEdit(r) }

        }
    }
}
