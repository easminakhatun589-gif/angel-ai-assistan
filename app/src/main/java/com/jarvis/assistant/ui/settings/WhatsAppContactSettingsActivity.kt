package com.jarvis.assistant.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jarvis.assistant.R
import com.jarvis.assistant.databinding.ActivityWhatsappContactSettingsBinding
import com.jarvis.assistant.util.ContactCaller
import com.jarvis.assistant.util.WhatsAppMessenger

class WhatsAppContactSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWhatsappContactSettingsBinding
    private val contactList = mutableListOf<ContactItem>()
    private val filteredList = mutableListOf<ContactItem>()
    private lateinit var contactAdapter: ContactAdapter

    data class ContactItem(
        val id: String,
        val name: String,
        val number: String,
        var isFavorite: Boolean = false
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            binding.tvContactsPermissionNotice.visibility = View.GONE
            loadContacts()
        } else {
            binding.tvContactsPermissionNotice.visibility = View.VISIBLE
            Toast.makeText(this, "Contacts permission is required to link WhatsApp numbers.", Toast.LENGTH_LONG).show()
        }
    }

    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spoken = matches[0]
                binding.etVoiceCommandInput.setText(spoken)
                executeVoiceCommand(spoken)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(window.decorView)
        binding = ActivityWhatsappContactSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarWhatsAppContacts)
        binding.toolbarWhatsAppContacts.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupListeners()
        checkPermissionAndLoad()
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(
            filteredList,
            onMessageClick = { contact ->
                val result = WhatsAppMessenger.sendMessage(this, contact.name, "Hello from JARVIS Assistant!", confirmed = true)
                handleSendResult(result)
            },
            onVoiceCallClick = { contact ->
                val result = WhatsAppMessenger.placeCall(this, contact.name, "voice", confirmed = true)
                handleCallResult(result)
            },
            onVideoCallClick = { contact ->
                val result = WhatsAppMessenger.placeCall(this, contact.name, "video", confirmed = true)
                handleCallResult(result)
            },
            onLocalCallClick = { contact ->
                val result = ContactCaller.callContact(this, contact.number)
                if (result is ContactCaller.CallResult.Success) {
                    Toast.makeText(this, "Calling ${contact.name} (${contact.number})...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to place call to ${contact.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onFavoriteClick = { contact ->
                contact.isFavorite = !contact.isFavorite
                contactAdapter.notifyDataSetChanged()
                Toast.makeText(this, "${contact.name} favorite status updated", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvWhatsAppContacts.layoutManager = LinearLayoutManager(this)
        binding.rvWhatsAppContacts.adapter = contactAdapter
    }

    private fun setupListeners() {
        binding.tvContactsPermissionNotice.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

        binding.etSearchContact.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnTestVoiceCommand.setOnClickListener {
            val text = binding.etVoiceCommandInput.text.toString().trim()
            if (text.isNotBlank()) {
                executeVoiceCommand(text)
            } else {
                startVoiceRecognition()
            }
        }

        binding.chipFilterAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.chipFilterWhatsApp.isChecked = false
                binding.chipFilterFavorites.isChecked = false
                filterContacts(binding.etSearchContact.text.toString())
            }
        }

        binding.chipFilterWhatsApp.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.chipFilterAll.isChecked = false
                binding.chipFilterFavorites.isChecked = false
                filterWhatsAppContacts()
            }
        }

        binding.chipFilterFavorites.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.chipFilterAll.isChecked = false
                binding.chipFilterWhatsApp.isChecked = false
                filterFavoriteContacts()
            }
        }
    }

    private fun checkPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            binding.tvContactsPermissionNotice.visibility = View.GONE
            loadContacts()
        } else {
            binding.tvContactsPermissionNotice.visibility = View.VISIBLE
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun loadContacts() {
        contactList.clear()
        val resolver = contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        try {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: ""
                    val name = cursor.getString(nameIdx) ?: ""
                    val number = cursor.getString(numberIdx) ?: ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        contactList.add(ContactItem(id, name.trim(), number.trim()))
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading contacts: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        filterContacts("")
    }

    private fun filterContacts(query: String) {
        filteredList.clear()
        val q = query.lowercase().trim()
        for (item in contactList) {
            if (q.isEmpty() || item.name.lowercase().contains(q) || item.number.contains(q)) {
                filteredList.add(item)
            }
        }
        contactAdapter.notifyDataSetChanged()
    }

    private fun filterWhatsAppContacts() {
        filteredList.clear()
        for (item in contactList) {
            // All contacts with numbers can be opened in WhatsApp
            filteredList.add(item)
        }
        contactAdapter.notifyDataSetChanged()
    }

    private fun filterFavoriteContacts() {
        filteredList.clear()
        for (item in contactList) {
            if (item.isFavorite) {
                filteredList.add(item)
            }
        }
        contactAdapter.notifyDataSetChanged()
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command: e.g. WhatsApp call Mom, WhatsApp message Rahul")
        }
        try {
            speechRecognitionLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun executeVoiceCommand(command: String) {
        val lower = command.lowercase()
        if (lower.contains("whatsapp call") || lower.contains("call on whatsapp")) {
            val contactName = lower.replace("whatsapp call", "").replace("call on whatsapp", "").replace("whatsapp", "").replace("call", "").trim()
            val result = WhatsAppMessenger.placeCall(this, contactName, "voice", confirmed = true)
            handleCallResult(result)
        } else if (lower.contains("whatsapp video") || lower.contains("video call")) {
            val contactName = lower.replace("whatsapp video call", "").replace("video call", "").replace("whatsapp", "").replace("call", "").trim()
            val result = WhatsAppMessenger.placeCall(this, contactName, "video", confirmed = true)
            handleCallResult(result)
        } else if (lower.contains("whatsapp message") || lower.contains("message") || lower.contains("send to")) {
            val contactName = lower.replace("whatsapp message", "").replace("message", "").replace("send to", "").trim()
            val result = WhatsAppMessenger.sendMessage(this, contactName, "Hello! Sent via JARVIS voice command.", confirmed = true)
            handleSendResult(result)
        } else if (lower.contains("call") || lower.contains("phone") || lower.contains("dial")) {
            val contactName = lower.replace("call", "").replace("phone", "").replace("dial", "").trim()
            val callResult = ContactCaller.callContact(this, contactName)
            when (callResult) {
                is ContactCaller.CallResult.Success -> {
                    Toast.makeText(this, "Calling ${callResult.contact.name} (${callResult.contact.number})", Toast.LENGTH_SHORT).show()
                }
                is ContactCaller.CallResult.NoMatch -> {
                    Toast.makeText(this, "Contact '$contactName' not found for call.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Call action executed for $contactName", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Voice Command Parsed")
                .setMessage("Command: '$command'\n\nJARVIS understood your voice input. Try saying 'WhatsApp call [Name]', 'WhatsApp message [Name]', or 'Call [Name]'.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun handleSendResult(result: WhatsAppMessenger.SendResult) {
        when (result) {
            is WhatsAppMessenger.SendResult.Success -> {
                Toast.makeText(this, "WhatsApp opened for ${result.contactName} (${result.appName})", Toast.LENGTH_SHORT).show()
            }
            is WhatsAppMessenger.SendResult.ContactNotFound -> {
                Toast.makeText(this, "Contact not found: ${result.name}", Toast.LENGTH_SHORT).show()
            }
            is WhatsAppMessenger.SendResult.MultipleContactsFound -> {
                Toast.makeText(this, "Multiple contacts found for ${result.name}", Toast.LENGTH_SHORT).show()
            }
            is WhatsAppMessenger.SendResult.MissingPermission -> {
                Toast.makeText(this, "Contacts permission missing.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "WhatsApp message action completed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleCallResult(result: WhatsAppMessenger.CallResult) {
        when (result) {
            is WhatsAppMessenger.CallResult.Success -> {
                Toast.makeText(this, "WhatsApp ${result.callType} call initiated to ${result.contactName}", Toast.LENGTH_SHORT).show()
            }
            is WhatsAppMessenger.CallResult.ContactNotFound -> {
                Toast.makeText(this, "Contact not found: ${result.name}", Toast.LENGTH_SHORT).show()
            }
            is WhatsAppMessenger.CallResult.MultipleContactsFound -> {
                Toast.makeText(this, "Multiple contacts found for ${result.name}", Toast.LENGTH_SHORT).show()
            }
            is WhatsAppMessenger.CallResult.MissingPermission -> {
                Toast.makeText(this, "Contacts permission missing.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "WhatsApp call action completed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class ContactAdapter(
        private val items: List<ContactItem>,
        private val onMessageClick: (ContactItem) -> Unit,
        private val onVoiceCallClick: (ContactItem) -> Unit,
        private val onVideoCallClick: (ContactItem) -> Unit,
        private val onLocalCallClick: (ContactItem) -> Unit,
        private val onFavoriteClick: (ContactItem) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvInitials: TextView = view.findViewById(R.id.tvContactInitials)
            val tvName: TextView = view.findViewById(R.id.tvContactName)
            val tvNumber: TextView = view.findViewById(R.id.tvContactNumber)
            val ivFavorite: ImageView = view.findViewById(R.id.ivFavoriteStar)
            val btnMessage: MaterialButton = view.findViewById(R.id.btnWaMessage)
            val btnVoiceCall: MaterialButton = view.findViewById(R.id.btnWaVoiceCall)
            val btnVideoCall: MaterialButton = view.findViewById(R.id.btnWaVideoCall)
            val btnLocalCall: MaterialButton = view.findViewById(R.id.btnLocalCall)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_whatsapp_contact, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvNumber.text = item.number
            holder.tvInitials.text = if (item.name.isNotEmpty()) item.name.first().uppercase() else "?"

            if (item.isFavorite) {
                holder.ivFavorite.setImageResource(R.drawable.ic_sparkle)
                holder.ivFavorite.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00E5FF"))
            } else {
                holder.ivFavorite.setImageResource(R.drawable.ic_sparkle)
                holder.ivFavorite.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#94A3B8"))
            }


            holder.ivFavorite.setOnClickListener { onFavoriteClick(item) }
            holder.btnMessage.setOnClickListener { onMessageClick(item) }
            holder.btnVoiceCall.setOnClickListener { onVoiceCallClick(item) }
            holder.btnVideoCall.setOnClickListener { onVideoCallClick(item) }
            holder.btnLocalCall.setOnClickListener { onLocalCallClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
