/*
 * MIT License
 *
 * Copyright (c) 2017 Tuomo Heino
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package xyz.hetula.suitcase

import android.support.v7.util.SortedList
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.util.SortedListAdapterCallback
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class WorkAdapter : RecyclerView.Adapter<WorkAdapter.WorkHolder>() {
    private val mData = SortedList<WorkDay>(WorkDay::class.java, WorkdaySorterCallback(this))

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): WorkHolder {
        val inflater = LayoutInflater.from(parent!!.context)
        return WorkHolder(inflater.inflate(R.layout.item_workday, parent, false))
    }

    override fun onBindViewHolder(holder: WorkHolder?, position: Int) {
        if (holder == null) {
            return
        }
        val workday = mData[position]
        if (workday.end == 0L) {
            holder.txtTitle.setText(R.string.work_day_ongoing)
            holder.txtTime.text = DateUtils.getRelativeTimeSpanString(workday.start)
        } else {
            holder.txtTitle.setText(R.string.work_day_over)
            val dur = (workday.end - workday.start) / 1000L
            holder.txtTime.text = DateUtils.formatElapsedTime(dur)
        }
    }

    override fun getItemCount() = mData.size()

    fun observe(changed: List<WorkDay>) {
        for (i in 0 until changed.size - 1) {
            changed[i].end = changed[i + 1].start
        }
        mData.beginBatchedUpdates()
        mData.clear()
        mData.addAll(changed)
        mData.endBatchedUpdates()
    }

    class WorkHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.work_item_title)
        val txtTime: TextView = itemView.findViewById(R.id.work_item_time)
    }

    class WorkdaySorterCallback(adapter: WorkAdapter) : SortedListAdapterCallback<WorkDay>(adapter) {
        override fun areContentsTheSame(oldItem: WorkDay?, newItem: WorkDay?): Boolean {
            if (oldItem == null || newItem == null) {
                return false
            }
            return oldItem == newItem
        }

        override fun areItemsTheSame(item1: WorkDay?, item2: WorkDay?): Boolean {
            if (item1 == null || item2 == null) {
                return false
            }
            return item1.id == item2.id
        }

        override fun compare(o1: WorkDay?, o2: WorkDay?): Int {
            if (o1 == null && o2 == null) {
                return 0
            }
            if (o1 == null) {
                return 1
            }
            if (o2 == null) {
                return -1
            }
            return o2.start.compareTo(o1.start)
        }
    }

}
