package com.uth.taskmanagement.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.uth.taskmanagement.R
import java.time.LocalDate
import java.time.YearMonth

/**
 * Ve luoi 42 o ngay (6 hang x 7 cot) cho 1 thang.
 * Moi o: hinh tron so ngay (highlight neu dang chon), + cham mau nho ben duoi
 * the hien trang thai cac task trong ngay do (do CalendarViewModel tinh san).
 */
class CalendarGridAdapter(
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarGridAdapter.DayViewHolder>() {

    private var days: List<CalendarDay> = emptyList()
    private var displayedMonth: YearMonth = YearMonth.now()

    fun submitDays(newDays: List<CalendarDay>, month: YearMonth) {
        days = newDays
        displayedMonth = month
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position], onDayClick)
    }

    override fun getItemCount(): Int = days.size

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        private val dotStatus: View = itemView.findViewById(R.id.dotStatus)

        fun bind(day: CalendarDay, onDayClick: (LocalDate) -> Unit) {
            tvDayNumber.text = day.date.dayOfMonth.toString()
            tvDayNumber.isSelected = day.isSelected

            // Ngay thuoc thang truoc/sau (o "lap day") hien mo hon de phan biet.
            tvDayNumber.alpha = if (day.isCurrentMonth) 1f else 0.35f
            dotStatus.alpha = if (day.isCurrentMonth) 1f else 0.35f

            if (day.dotColorRes != null) {
                dotStatus.visibility = View.VISIBLE
                // mutate() bat buoc de tranh RecyclerView tai su dung view lam doi mau
                // tat ca cac cham khac dang dung chung 1 drawable.
                dotStatus.background = dotStatus.background.mutate()
                dotStatus.background.setTint(
                    ContextCompat.getColor(itemView.context, day.dotColorRes)
                )
            } else {
                dotStatus.visibility = View.GONE
            }

            itemView.setOnClickListener { onDayClick(day.date) }
        }
    }
}