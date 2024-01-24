package com.example.shppprojects.presentation.ui.fragments.addcontacts.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shppprojects.data.model.Contact
import com.example.shppprojects.databinding.ItemAddUserBinding
import com.example.shppprojects.domain.state.ApiStateUser
import com.example.shppprojects.presentation.ui.fragments.addcontacts.adapter.interfaces.UserItemClickListener
import com.example.shppprojects.presentation.ui.fragments.contacts.adapter.utils.ContactDiffUtil
import com.example.shppprojects.presentation.utils.Constants
import com.example.shppprojects.presentation.utils.ext.gone
import com.example.shppprojects.presentation.utils.ext.invisible
import com.example.shppprojects.presentation.utils.ext.loadImage
import com.example.shppprojects.presentation.utils.ext.visible

class AddContactsAdapter(
    private val listener: UserItemClickListener,
) : androidx.recyclerview.widget.ListAdapter<Contact, AddContactsAdapter.UsersViewHolder>(
    ContactDiffUtil()
) {

    private var states: ArrayList<Pair<Long, ApiStateUser>> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAddUserBinding.inflate(inflater, parent, false)
        return UsersViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        holder.bind(
            currentList[position],
            states.find { it.first == currentList[position].id }?.second ?: ApiStateUser.Initial

        )
    }

    inner class UsersViewHolder(private val binding: ItemAddUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact, state: ApiStateUser) {
            with(binding) {
                textViewName.text = contact.name
                textViewCareer.text = contact.career
                imageViewUserPhoto.loadImage(contact.photo)
            }
            setState(state)
            setListeners(contact)
        }

        private fun setState(state: ApiStateUser) {
            with(binding) {
                when (state) {
                    is ApiStateUser.Success<*> -> {
                        textViewAdd.gone()
                        progressBar.gone()
                        imageViewDoneAddContact.visible()
                    }

                    is ApiStateUser.Loading -> {
                        textViewAdd.gone()
                        progressBar.visible()
                        imageViewDoneAddContact.invisible()
                    }

                    is ApiStateUser.Initial -> {
                        textViewAdd.visible()
                        progressBar.gone()
                        imageViewDoneAddContact.invisible()
                    }

                    is ApiStateUser.Error -> Unit
                }
            }
        }

        private fun setListeners(contact: Contact) {
            addContact(contact)
            detailView(contact)
        }

        private fun addContact(contact: Contact) {
            binding.textViewAdd.setOnClickListener {
                listener.onClickAdd(contact)
            }
        }

        private fun detailView(contact: Contact) {
            with(binding) {
                root.setOnClickListener {
                    listener.onClickContact(
                        contact,
                        arrayOf(
                            setTransitionName(
                                imageViewUserPhoto,
                                Constants.TRANSITION_NAME_IMAGE + contact.id
                            ), setTransitionName(
                                textViewName,
                                Constants.TRANSITION_NAME_NAME + contact.id
                            ), setTransitionName(
                                textViewCareer,
                                Constants.TRANSITION_NAME_CAREER + contact.id
                            )
                        )
                    )
                }
            }
        }

        private fun setTransitionName(view: View, name: String): Pair<View, String> {
            view.transitionName = name
            return view to name
        }
    }

    fun setStates(states: ArrayList<Pair<Long, ApiStateUser>>) {
        if (this.states.size != states.size) {
            this.states = states
            val lastIndex = currentList.indexOfLast { it.id == states.lastOrNull()?.first }
            if (lastIndex == -1) {
                notifyItemChanged(lastIndex)
            }
            return
        }
        states.forEachIndexed { index, state ->
            if (this.states[index] != states[index]) {
                this.states[index] = state
                notifyItemChanged(currentList.indexOfFirst { it.id == state.first })
            }
        }
    }

}