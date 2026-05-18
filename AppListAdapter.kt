package com.rhdevsx.adblockerlsp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private var appList: List<AppInfo>,
    private val configManager: ConfigManager
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val appPackage: TextView = view.findViewById(R.id.appPackage)
        val appSwitch: Switch = view.findViewById(R.id.appSwitch)
        val schemeLayout: LinearLayout = view.findViewById(R.id.schemeLayout)
        val cbScheme1: CheckBox = view.findViewById(R.id.cbScheme1)
        val cbScheme2: CheckBox = view.findViewById(R.id.cbScheme2)
        val cbScheme3: CheckBox = view.findViewById(R.id.cbScheme3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = appList[position]

        holder.appIcon.setImageDrawable(app.icon)
        holder.appName.text = app.name
        holder.appPackage.text = app.packageName
        holder.appSwitch.isChecked = app.isEnabled
        
        holder.cbScheme1.isChecked = app.scheme1
        holder.cbScheme2.isChecked = app.scheme2
        holder.cbScheme3.isChecked = app.scheme3

        holder.schemeLayout.visibility = if (app.isEnabled) View.VISIBLE else View.GONE

        holder.appSwitch.setOnCheckedChangeListener { _, isChecked ->
            app.isEnabled = isChecked
            configManager.setAppEnabled(app.packageName, isChecked)
            holder.schemeLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val checkListener = View.OnClickListener {
            app.scheme1 = holder.cbScheme1.isChecked
            app.scheme2 = holder.cbScheme2.isChecked
            app.scheme3 = holder.cbScheme3.isChecked
            configManager.setSchemeConfig(app.packageName, app.scheme1, app.scheme2, app.scheme3)
        }

        holder.cbScheme1.setOnClickListener(checkListener)
        holder.cbScheme2.setOnClickListener(checkListener)
        holder.cbScheme3.setOnClickListener(checkListener)
    }

    override fun getItemCount() = appList.size
    
    fun updateData(newList: List<AppInfo>) {
        appList = newList
        notifyDataSetChanged()
    }
}
