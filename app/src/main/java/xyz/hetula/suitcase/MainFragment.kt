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

import android.arch.lifecycle.Observer
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class MainFragment : Fragment() {
    private val mConnection: SuitcaseConnection = SuitcaseConnection(this)
    private lateinit var mService: ServiceWrapper
    private lateinit var mAdapter: WorkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity.applicationContext.bindService(Intent(activity, SuitcaseService::class.java),
                mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_main, container, false)
        val workBtn = view.findViewById<View>(R.id.workBtn)
        val workList = view.findViewById<RecyclerView>(R.id.workList)

        mAdapter = WorkAdapter()

        workList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        workList.adapter = mAdapter
        workBtn.setOnClickListener {
            mService.service { it.workdayDb { it.insertAll(WorkDay()) } }
        }

        return view
    }

    private fun attachService(service: SuitcaseService) {
        mService = ServiceWrapper(service)
        service.workdayDbSync { it.getAllSync().observe(activity, AdapterObserving()) }
    }

    private fun detachService() {
        mService = ServiceWrapper()
    }

    inner class AdapterObserving : Observer<List<WorkDay>> {
        override fun onChanged(t: List<WorkDay>?) = mAdapter.observe(t!!)
    }

    inner class SuitcaseConnection(private val fragment: MainFragment) : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service == null) return
            fragment.attachService((service as SuitcaseService.LocalBinder).service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            detachService()
        }
    }

    inner class ServiceWrapper(val service: SuitcaseService? = null) {
        inline fun service(serviceCallback: (SuitcaseService) -> Unit) {
            if (service != null) {
                serviceCallback(service)
            }
        }
    }
}
