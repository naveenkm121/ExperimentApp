package com.ecommerce.experimentapp.model

data class ContactReq(
    var deviceId: String = "",
    var contacts: List<ContactData> = emptyList()
)