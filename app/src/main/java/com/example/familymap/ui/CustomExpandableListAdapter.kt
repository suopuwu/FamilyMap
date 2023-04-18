package com.example.familymap.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import com.example.familymap.databinding.ExpandableChildBinding
import com.example.familymap.databinding.ExpandableHeaderBinding
import com.example.familymap.utils.SuopConstants

class CustomExpandableListAdapter(
    context: Context,
    private var groups: List<String>,
    private var items: Map<String, List<String>>,
) : BaseExpandableListAdapter() {
    private var layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getGroupCount(): Int {
        return groups.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return items[groups[groupPosition]]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): Any {
        return groups[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return items[groups[groupPosition]]?.get(childPosition) ?: ""
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(
        position: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val binding = ExpandableHeaderBinding.inflate(layoutInflater)
        //Set text on header, with a + or - sign depending on if it's opened
        binding.expandableHeader.text =
            (if (isExpanded) "- " else "+ ") + getGroup(position).toString()
        return binding.root
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val binding = ExpandableChildBinding.inflate(layoutInflater)
        //Split the string into its 2 parts, the text and the icon
        val content =
            getChild(groupPosition, childPosition).toString().split(SuopConstants.STRING_SEPARATOR)
        binding.expandableChild.text = content[0]
        binding.expandableChild.setCompoundDrawablesWithIntrinsicBounds(content[1].toInt(), 0, 0, 0)
        return binding.root
    }

    override fun isChildSelectable(p0: Int, p1: Int): Boolean {
        return true
    }
}