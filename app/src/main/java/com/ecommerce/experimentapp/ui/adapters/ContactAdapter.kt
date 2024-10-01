package com.ecommerce.experimentapp.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecommerce.experimentapp.databinding.ItemContactBinding
import com.ecommerce.experimentapp.model.ContactData

class ContactAdapter( ) : RecyclerView.Adapter<ContactAdapter.ContactVH>() {

    private var itemList: List<ContactData> = listOf()

    fun setItem(list: List<ContactData>) {
        this.itemList = list
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int= itemList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactVH {
        val binding: ItemContactBinding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactVH(parent.context, binding)
    }
    override fun onBindViewHolder(holder: ContactVH, position: Int) {
        holder.bind(itemList[position])

    }



    class ContactVH(
        private val context: Context,
        private val itemBinding: ItemContactBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root),
        View.OnClickListener  {

        init {
            itemBinding.root.setOnClickListener(this)
        }

        @SuppressLint("SetTextI18n")
        fun bind(item: ContactData) {


            itemBinding.nameTV.text=item.name
            itemBinding.mobileTV.text=item.mobile


        }



        override fun onClick(p0: View?) {

            //listener.onClickedCard(item)
        }
    }

}