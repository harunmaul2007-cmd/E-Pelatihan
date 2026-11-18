package com.harunmaul.epelatihan.feature.participant.common

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

data class PickedContactInfo(
    val name: String,
    val phone: String,
)

object ContactPickerHelper {
    fun extractContactInfo(context: Context, contactUri: Uri): PickedContactInfo? {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        return try {
            context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val number = if (numberIdx >= 0) cursor.getString(numberIdx).orEmpty() else ""
                    PickedContactInfo(name = name.trim(), phone = number.trim())
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
