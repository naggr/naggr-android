package au.com.naggr.naggr

import android.Manifest
import android.content.CursorLoader
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.preference.PreferenceManager
import android.content.SharedPreferences
import android.R.id.edit
import android.R.attr.key
import org.json.JSONArray


class AddContactsActivity : AppCompatPreferenceActivity() {
    val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    var contactRows = ArrayList<String>()
    val nocontact = arrayOf("No Contacts on the Device")
    var editor: SharedPreferences.Editor? = null
    var prefs: SharedPreferences? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contacts)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        editor = prefs!!.edit()
        loadContacts()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onBackPressed()
        return true
    }

    private fun loadContacts() {
        val mListContacts = findViewById<ListView>(R.id.contacts)
        mListContacts.setAdapter(this.getContacts())
        mListContacts.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(this, MainActivity::class.java)
            System.out.println("id: $id; position: $position")
            if (editor != null) {
                val editor = editor!!
                val chosenContacts: JSONArray = if (prefs!!.contains("chosenContacts")) {
                    val json = prefs!!.getString("chosenContacts", null)
                    JSONArray(json)
                } else {
                    JSONArray()
                }
                chosenContacts.put(position)

                editor.putString("chosenContacts", chosenContacts.toString())
                editor.apply()
                editor.commit()
            }
            intent.putExtra("contact", position)
            this.startActivity(intent)
        }
    }

    fun getContacts(): ArrayAdapter<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                        Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS),
                    PERMISSIONS_REQUEST_READ_CONTACTS)
            //callback onRequestPermissionsResult
            return ArrayAdapter(this, android.R.layout.simple_list_item_1, nocontact)
        } else {
            val contactsUri = Uri.parse("content://contacts/people")

            val projection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
            )
            val c: Cursor
            val cursorLoader = CursorLoader(this, contactsUri, projection, null, null, null)

            c = cursorLoader.loadInBackground()
            contactRows.clear()
            c.moveToFirst()

            while (c.isAfterLast === false) {
                val contactID = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID))
                val contactDisplayName = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                contactRows.add("$contactID| $contactDisplayName")
                c.moveToNext()
            }

            if (c != null && !c.isClosed()) {
                c.close()
            }

            return if (contactRows.isEmpty()) {
                ArrayAdapter(this, android.R.layout.simple_list_item_1, nocontact)
            } else {
                val contacts = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, contactRows);
                val contactsArray = JSONArray()
                for (i in 0 until contacts.count) {
                    contactsArray.put(contacts.getItem(i))
                }
                if (editor != null) {
                    val editor = editor!!
                    editor.putString("contacts", contactsArray.toString())
                    editor.apply()
                    editor.commit()
                }
                return contacts
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                //  toast("Permission must be granted in order to display contacts information")
            }
        }
    }
}
