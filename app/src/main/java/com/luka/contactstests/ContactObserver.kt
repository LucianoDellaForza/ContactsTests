package com.luka.contactstests

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.provider.ContactsContract
import android.util.Log

class ContactObserver(
    private val context: Context,
    handler: Handler
): ContentObserver(handler) {


    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)

        val deletedContacts = checkIfAnyContactsWereDeleted()
        deletedContacts.forEach {
            // delete user from DB
            Log.d("ContactObserver", "Delete contact with id: $it from DB")
        }

        val updatedContact = getLatestEditedContact()
        updatedContact?.let { contactId ->
            val contactInfo = getContactInfo(contactId)
            println("ContactObserver - contactInfo: $contactInfo")
        }

//        val contactChanges = getChangedContactsFromRaw()
//        contactChanges.deletedContactIds.forEach { contactId ->
//            // delete user from DB
//            Log.d("ContactObserver", "Delete contact with id: $contactId from DB")
//        }
//
//        // contactLastUpdatedTimestamp mechanism is because  onChange() gets called unpredictable amount of times
//        val contactLastUpdatedTimestamp = getTimestampOfLastUpdatedContact()
//        val contactLastUpdatedTimestampSp = readLongFromSharedPreferences(context, "contactLastUpdatedTimestamp")
//        Log.d("ContactObserver", "contactLastUpdatedTimestamp:   $contactLastUpdatedTimestamp")
//        Log.d("ContactObserver", "contactLastUpdatedTimestampSp: $contactLastUpdatedTimestampSp")
//        if (contactLastUpdatedTimestampSp < contactLastUpdatedTimestamp) {
//            Log.d("ContactObserver", "Updates required")
//            contactChanges.insertedOrUpdatedContactIds.forEach { contactId ->
//                // add/update users in DB
//                Log.d("ContactObserver", "Add/update contact with id: $contactId in DB")
//
//            }
//            saveLongToSharedPreferences(context, "contactLastUpdatedTimestamp", contactLastUpdatedTimestamp)
//        } else {
//            Log.d("ContactObserver", "No updates required")
//        }

//        val contactInfo = getTimestampOfLastlyUpdatedContact()
//        Log.d("ContactObserver", "last changed contact info: name = ${contactInfo.first}, timestamp = ${contactInfo.second}")
    }


    @SuppressLint("Range")
    private fun getChangedContactsFromRaw(): ContactChanges {
        val insertedOrUpdatedContactIds = mutableListOf<String>()
        val deletedContactIds = mutableListOf<String>()

        // Query the Contacts Provider for inserted/updated/deleted contacts
        val projection = arrayOf(
            ContactsContract.RawContacts._ID,
            ContactsContract.RawContacts.DELETED,
        )
        val selection = "${ContactsContract.RawContacts.DIRTY} = 1 OR ${ContactsContract.RawContacts.DELETED} = 1"
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            projection,
            selection,
            null,
            null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val contactId = it.getString(it.getColumnIndex(ContactsContract.RawContacts._ID))
                val isDeleted = it.getInt(it.getColumnIndex(ContactsContract.RawContacts.DELETED)) > 0

                if (isDeleted)
                    deletedContactIds.add(contactId)
                else
                    insertedOrUpdatedContactIds.add(contactId)


                Log.d("ContactObserver", "contact that was changed/inserted/deleted: id: $contactId, isDeleted: $isDeleted")
                getContactInfo(contactId = contactId)
            }
        }

        return ContactChanges(
            insertedOrUpdatedContactIds = insertedOrUpdatedContactIds,
            deletedContactIds = deletedContactIds
        )
    }


    @SuppressLint("Range")
    private fun getContactInfo2(
        contactId: String
    ) {
        Log.d("ContactObserver", "should search $contactId")

        var contactName: String? = ""
//        val contactPhones = mutableListOf<PhoneInfo>()
//        val contactEmails = mutableListOf<EmailInfo>()
        var contactAvatarPhotoUri: String? = ""

        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME, //DISPLAY_NAME_PRIMARY
            ContactsContract.Contacts.PHOTO_URI,
        )
        // Query to retrieve the contact by id
        val userSelection = "${ContactsContract.Contacts._ID} == $contactId"
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            userSelection,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                contactName = name
                val avatarPhotoUri = it.getString(it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI))
                contactAvatarPhotoUri = avatarPhotoUri

                Log.d("ContactObserver", "contact id $contactId from raw found in Contacts, name: $name")

//
//                // Query to retrieve phone numbers associated with the contact
//                val phoneProjection = arrayOf(
//                    ContactsContract.CommonDataKinds.Phone._ID,
//                    ContactsContract.CommonDataKinds.Phone.NUMBER
//                )
//                val phoneCursor = contentResolver.query(
//                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                    phoneProjection,
//                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
//                    arrayOf(contactId),
//                    null
//                )
//                phoneCursor?.use { phoneCrs ->
//                    while (phoneCrs.moveToNext()) {
//                        val phoneId = phoneCrs.getString(phoneCrs.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID))
//                        val phoneNumber = phoneCrs.getString(phoneCrs.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
//                        contactPhones.add(PhoneInfo(phoneId = phoneId, phoneNumber = phoneNumber)) // should phone number be normalized here?
//                    }
//                }
//
//                // Query to retrieve email addresses associated with the contact
//                val emailProjection = arrayOf(
//                    ContactsContract.CommonDataKinds.Email._ID,
//                    ContactsContract.CommonDataKinds.Email.DATA
//                )
//                val emailCursor = contentResolver.query(
//                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
//                    emailProjection,
//                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
//                    arrayOf(contactId),
//                    null
//                )
//                emailCursor?.use { emailCrs ->
//                    while (emailCrs.moveToNext()) {
//                        val emailId = emailCrs.getString(emailCrs.getColumnIndex(ContactsContract.CommonDataKinds.Email._ID))
//                        val emailAddress = emailCrs.getString(emailCrs.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA))
//                        contactEmails.add(EmailInfo(emailId = emailId, emailAddress = emailAddress))
//                    }
//                }
            } else {
                Log.d("ContactObserver", "contact id $contactId from raw NOT found in Contacts")
            }
        }
    }


    //validno samo za insert/update contacta. Kada se contact obrise, vraca timestamp promenjenog poslednjeg postojeceg kontakta
    @SuppressLint("Range")
    fun getTimestampOfLastUpdatedContact(): Long {
        var timestamp: Long = 0

        val contentResolver: ContentResolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,
        )
        val sortOrder = ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " DESC LIMIT 1"
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
        cursor?.use {
            if (it.moveToFirst()) {
                timestamp = it.getLong(it.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP))
            }
        }

        return timestamp
    }


    // Function to save a Long value to shared preferences
    private fun saveLongToSharedPreferences(context: Context, key: String, value: Long) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun readLongFromSharedPreferences(context: Context, key: String): Long {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getLong(key, 0L) // 0L is the default value if the key is not found
    }


//    @SuppressLint("Range")
//    override fun onChange(selfChange: Boolean) {
//        super.onChange(selfChange)
//
//        // Define the fields you want to retrieve from the contacts database
//        val projection = arrayOf(
//            ContactsContract.Contacts._ID,
//            ContactsContract.Contacts.DISPLAY_NAME,
//            ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
//        )
//
//        // Query the contacts database to get the updated or edited contacts
//        val cursor: Cursor? = context.contentResolver.query(
//            ContactsContract.Contacts.CONTENT_URI,
//            projection,
//            null,
//            null,
//            ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " DESC LIMIT 1"
//        )
//
//        cursor?.use { it ->
//            while (it.moveToNext()) {
//                // Retrieve the contact details
//                val contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
//                val displayName = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
//                val lastUpdatedTimestamp = it.getLong(it.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP))
//
//                // Do something with the contact details
//                // For example, you can log or display the updated contact information
//                println("ContactObserver - Contact ID: $contactId, Display Name: $displayName, Last Updated Timestamp: $lastUpdatedTimestamp")
//
//            }
//        }
//        println("ContactObserver - END")
//
//    }


    @SuppressLint("Range")
    private fun checkIfAnyContactsWereDeleted(): List<String> {
        val deletedContactIds = mutableListOf<String>()

        // Query the Contacts Provider for deleted contacts
        val projection = arrayOf(
            ContactsContract.RawContacts._ID,
            //            ContactsContract.RawContacts.DELETED,
            ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
        )
        val selection = "${ContactsContract.RawContacts.DELETED} = 1"
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            projection,
            selection,
            null,
            null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val contactId = it.getString(it.getColumnIndex(ContactsContract.RawContacts._ID))
                val displayNamePrimary = it.getString(it.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY))

                Log.d("ContactObserver", "Contact that was deleted -> id: $contactId, displayNamePrimary: $displayNamePrimary")
                deletedContactIds.add(contactId)
            }
        }
        return deletedContactIds
    }

    @SuppressLint("Range")
    fun getLatestEditedContact(): String? {
        var contactId: String? = null

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP
        )
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP + " DESC LIMIT 1"
        )
        cursor?.use {
            while (it.moveToNext()) {
                // Retrieve the contact details
                contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val displayName = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val lastUpdatedTimestamp = it.getLong(it.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP))

                println("ContactObserver - Lastly edited contact -> Contact ID: $contactId, Display Name: $displayName, Last Updated Timestamp: $lastUpdatedTimestamp")
            }
        }
        println("ContactObserver - END")
        return contactId
    }

    @SuppressLint("Range")
    private fun getContactInfo(
        contactId: String
    ): ContactInfo {
        var contactName: String? = null
        var contactPhotoUri: String? = null
        val contactPhoneNumbers = mutableListOf<PhoneInfo>()
        val contactEmails = mutableListOf<EmailInfo>()

        val projection = arrayOf(
            ContactsContract.Data._ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Photo.PHOTO_URI,
//            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME
            ContactsContract.Data.DISPLAY_NAME,
        )
        val selection = ContactsContract.Data.CONTACT_ID + " = ? AND (" +
                ContactsContract.Data.MIMETYPE + " = ? OR " +
                ContactsContract.Data.MIMETYPE + " = ?" + ")"
        val selectionArgs = arrayOf(
            contactId,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
        )
        val sortOrder = ContactsContract.Data.MIMETYPE + " ASC"
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(ContactsContract.Data._ID))
                val lastUpdatedTimestamp = it.getString(it.getColumnIndex(ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP)) // Timestamp je isti za sve (na nivou je celog kontakta)
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME))
                contactPhotoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_URI))
                val mimeType = it.getString(it.getColumnIndex(ContactsContract.Data.MIMETYPE))

                // Retrieve phone number
                if (mimeType == ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE) {
                    val phoneNumber = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    contactPhoneNumbers.add(PhoneInfo(phoneId = id, phoneNumber = phoneNumber)) // raw phone number (needs to be processed)
                }
                // Retrieve email address
                if (mimeType == ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE) {
                    val emailAddress = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))
                    contactEmails.add(EmailInfo(emailId = id, emailAddress = emailAddress))
                }

            }
        }

        return ContactInfo(
            contactId = contactId,
            name = contactName ?: "",
            avatarUri = contactPhotoUri ?: "",
            phoneNumbers = contactPhoneNumbers,
            emails = contactEmails
        )
    }

}

data class ContactChanges(
    val insertedOrUpdatedContactIds: List<String> = listOf(),
    val deletedContactIds: List<String> = listOf(),
)

data class ContactInfo(
    val contactId: String,
    val name: String,
    val avatarUri: String,
    val phoneNumbers: List<PhoneInfo>,
    val emails: List<EmailInfo>,
)
data class PhoneInfo(
    val phoneId: String,
    val phoneNumber: String
)
data class EmailInfo(
    val emailId: String,
    val emailAddress: String
)