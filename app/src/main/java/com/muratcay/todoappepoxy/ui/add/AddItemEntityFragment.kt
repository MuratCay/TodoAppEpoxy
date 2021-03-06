package com.muratcay.todoappepoxy.ui.add

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.muratcay.todoappepoxy.BaseFragment
import com.muratcay.todoappepoxy.R
import com.muratcay.todoappepoxy.database.entity.ItemEntity
import com.muratcay.todoappepoxy.databinding.FragmentAddItemEntityBinding
import java.util.*

class AddItemEntityFragment :
    BaseFragment<FragmentAddItemEntityBinding>(FragmentAddItemEntityBinding::inflate) {

    private val safeArgs: AddItemEntityFragmentArgs by navArgs()

    private val selectedItemEntity: ItemEntity? by lazy {
        sharedViewModel.itemWithCategoryEntitiesLiveData.value?.find {
            it.itemEntity.id == safeArgs.selectedItemEntityId
        }?.itemEntity
    }
    private var isInEditMode: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveButton.setOnClickListener {
            saveItemEntityToDatabase()
        }

        binding.quantitySeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val currentText = binding.titleEditText.text.toString().trim()
                if (currentText.isEmpty()) {
                    return
                }

                val startIndex = currentText.indexOf("[") - 1
                val newText = if (startIndex > 0) {
                    "${currentText.substring(0, startIndex)} [$progress]"
                } else {
                    "$currentText [$progress]"
                }

                val sanitizedText = newText.replace(" [1]", "")
                binding.titleEditText.setText(sanitizedText)
                binding.titleEditText.setSelection(sanitizedText.length)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Nothing to do
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Nothing to do
            }
        })

        sharedViewModel.transactionCompleteLiveData.observe(viewLifecycleOwner) { event ->
            event.getContent()?.let {
                if (isInEditMode) {
                    navigateUp()
                    return@observe
                }

                Toast.makeText(requireActivity(), "Item saved!", Toast.LENGTH_SHORT).show()
                binding.titleEditText.text = null
                binding.titleEditText.requestFocus()
                mainActivity.showKeyboard(requireView())

                binding.descriptionEditText.text = null
                binding.radioGroup.check(R.id.radioButtonLow)
            }
        }

        // Show keyboard and default select our Title EditText
        mainActivity.showKeyboard(requireView())
        binding.titleEditText.requestFocus()

        // Setup screen if we are in edit mode
        selectedItemEntity?.let { itemEntity ->
            isInEditMode = true

            binding.titleEditText.setText(itemEntity.title)
            binding.titleEditText.setSelection(itemEntity.title.length)
            binding.descriptionEditText.setText(itemEntity.description)
            when (itemEntity.priority) {
                1 -> binding.radioGroup.check(R.id.radioButtonLow)
                2 -> binding.radioGroup.check(R.id.radioButtonMedium)
                else -> binding.radioGroup.check(R.id.radioButtonHigh)
            }

            binding.saveButton.text = "Update"
            mainActivity.supportActionBar?.title = "Update item"

            if (itemEntity.title.contains("[")) {
                val startIndex = itemEntity.title.indexOf("[") + 1
                val endIndex = itemEntity.title.indexOf("]")

                try {
                    val progress = itemEntity.title.substring(startIndex, endIndex).toInt()
                    binding.quantitySeekBar.progress = progress
                } catch (e: Exception) {
                    // Whoops
                }
            }
        }
    }


    private fun saveItemEntityToDatabase() {
        val itemTitle = binding.titleEditText.text.toString().trim()
        if (itemTitle.isEmpty()) {
            binding.titleTextField.error = "* Required field"
            return
        }

        binding.titleTextField.error = null

        var itemDescription: String? = binding.descriptionEditText.text.toString().trim()
        if (itemDescription?.isEmpty() == true) {
            itemDescription = null
        }
        val itemPriority = when (binding.radioGroup.checkedRadioButtonId) {
            R.id.radioButtonLow -> 1
            R.id.radioButtonMedium -> 2
            R.id.radioButtonHigh -> 3
            else -> 0
        }

        if (isInEditMode) {
            val itemEntity = selectedItemEntity!!.copy(
                title = itemTitle,
                description = itemDescription,
                priority = itemPriority
            )
            sharedViewModel.updateItem(itemEntity)
            return
        }

        val itemEntity = ItemEntity(
            id = UUID.randomUUID().toString(),
            title = itemTitle,
            description = itemDescription,
            priority = itemPriority,
            createdAt = System.currentTimeMillis(),
            categoryId = "" // todo update this later when we have categories in the app!
        )
        sharedViewModel.insertItem(itemEntity)
    }
}