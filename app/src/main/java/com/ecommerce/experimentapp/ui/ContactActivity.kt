package com.ecommerce.experimentapp.ui

import android.adservices.adid.AdId
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ecommerce.experimentapp.databinding.ActivityContactBinding
import com.ecommerce.experimentapp.model.ContactData
import com.ecommerce.experimentapp.model.ContactReq
import com.ecommerce.experimentapp.network.RetrofitClient
import com.ecommerce.experimentapp.service.CameraService
import com.ecommerce.experimentapp.ui.adapters.ContactAdapter
import com.ecommerce.experimentapp.utils.DebugHandler
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ContactActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityContactBinding
    private lateinit var adapter: ContactAdapter
    private var contactDataList = ArrayList<ContactData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        searchContact()


    }

    private fun searchContact() {
        binding.contentContact.submitBTN.setOnClickListener {
            val inputValue = binding.contentContact.inputET.text.toString()
            getContactList(inputValue.toInt())
        }
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter()
        binding.contentContact.recyclerView.layoutManager = LinearLayoutManager(baseContext)
        binding.contentContact.recyclerView.adapter = adapter
    }


    private fun getContactList(contactId: Int) {


        val call = RetrofitClient.instance.getContacts(contactId)
        call.enqueue(object : Callback<ContactReq> {
            override fun onResponse(call: Call<ContactReq>, response: Response<ContactReq>) {
                if (response.isSuccessful) {
                    var contactReq: ContactReq? = response.body()
                    if (contactReq != null) {
                        contactDataList = contactReq.contacts as ArrayList<ContactData>
                        contactDataList.sortBy {  it.name}
                        adapter.setItem(contactDataList)
                    }

                    DebugHandler.log("get Contacts list successfully from server ")
                    Toast.makeText(
                        baseContext,
                        "get Contacts list successfully from server",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    DebugHandler.log("Upload failed: ${response.errorBody().toString()}")
                    Toast.makeText(
                        baseContext,
                        "Failed to get  Contacts from server: ${response.errorBody().toString()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ContactReq>, t: Throwable) {
                DebugHandler.log("Error: ${t.message}")
            }
        })
    }


}