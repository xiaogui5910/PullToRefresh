package com.example.ccg.pulltorefresh

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var refreshLayout: PullToRefreshLayout
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        refreshLayout=findViewById(R.id.mRefresh)
         recyclerView=findViewById(R.id.mList)

        refreshLayout.reset()
        refreshLayout.setOnRefreshListener {
            //TODO 左拉刷新回调
            Toast.makeText(this,"刷新或跳转页面",Toast.LENGTH_SHORT).show()
        }
        val layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        recyclerView.layoutManager = layoutManager

        val dataList = ArrayList<String>()
        for (c in 'A'..'D') {
            dataList.add(c.toString())
        }
        val testAdapter = RvAdapter(this, dataList)
        recyclerView.adapter = testAdapter
    }
    class RvAdapter(var context: Context, var dataList: List<String>) : RecyclerView.Adapter<RvAdapter
    .RvViewHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): RvViewHolder {
            val view: View = LayoutInflater.from(context).inflate(R.layout.item_pulltorefresh, p0, false)
            return RvViewHolder(view)
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun onBindViewHolder(p0: RvViewHolder, p1: Int) {
            p0.name.text = dataList[p1]
        }


        class RvViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var name: TextView = itemView.findViewById(R.id.tv_title)
            var image: ImageView = itemView.findViewById(R.id.iv_img)
        }
    }
}
