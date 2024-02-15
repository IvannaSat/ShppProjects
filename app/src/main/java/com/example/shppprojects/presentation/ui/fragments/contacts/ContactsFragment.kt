package com.example.shppprojects.presentation.ui.fragments.contacts

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shppprojects.R
import com.example.shppprojects.data.model.Contact
import com.example.shppprojects.data.model.ResponseOfUser
import com.example.shppprojects.databinding.FragmentContactsBinding
import com.example.shppprojects.domain.state.ApiStateUser
import com.example.shppprojects.presentation.ui.base.BaseFragment
import com.example.shppprojects.presentation.ui.fragments.contacts.adapter.RecyclerViewAdapter
import com.example.shppprojects.presentation.ui.fragments.contacts.adapter.interfaces.ContactItemClickListener
import com.example.shppprojects.presentation.ui.fragments.viewpager.ViewPagerFragment
import com.example.shppprojects.presentation.ui.fragments.viewpager.ViewPagerFragmentDirections
import com.example.shppprojects.presentation.utils.Constants
import com.example.shppprojects.presentation.utils.ext.checkForInternet
import com.example.shppprojects.presentation.utils.ext.invisible
import com.example.shppprojects.presentation.utils.ext.setupSwipeToDelete
import com.example.shppprojects.presentation.utils.ext.showErrorSnackBar
import com.example.shppprojects.presentation.utils.ext.visible
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContactsFragment : BaseFragment<FragmentContactsBinding>(FragmentContactsBinding::inflate) {

    private val viewModel: ContactsViewModel by viewModels()
    private val userData: ResponseOfUser.Data by lazy {
        viewModel.getUser()
    }

    private val adapter: RecyclerViewAdapter by lazy {
        RecyclerViewAdapter(listener = object : ContactItemClickListener {

            override fun onClickContact(
                contact: Contact,
                transitionPairs: Array<Pair<View, String>>,
            ) {
                if (viewModel.isMultiselect.value) {
                    viewModel.changeListInMultiselectMode(contact)
                } else {
                    if (!viewModel.contactList.value.contains(contact)) return
                    val extras = FragmentNavigatorExtras(*transitionPairs)
                    val direction =
                        ViewPagerFragmentDirections.actionViewPagerFragmentToContactProfile(
                            false,
                            contact
                        )
                    navController.navigate(direction, extras)
                }
            }

            override fun onLongClick(contact: Contact) {
                viewModel.changeMultiselectMode()
                if (viewModel.isMultiselect.value) viewModel.addSelectContact(contact)
            }

            override fun onClickDelete(contact: Contact) {
                deleteUserWithRestore(contact)
            }

        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialRecyclerView()
        setListeners()
        setObservers()
    }

    private fun initialRecyclerView() {
        viewModel.deleteStates()
        viewModel.initialContactList(
            userData.user.id,
            userData.accessToken,
            requireContext().checkForInternet()
        )
        with(binding) {
            recyclerViewContacts.layoutManager = LinearLayoutManager(context)
            recyclerViewContacts.adapter = adapter
            recyclerViewContacts.setupSwipeToDelete(
                deleteFunction = {
                    deleteUserWithRestore(viewModel.contactList.value.getOrNull(it)!!)
                },
                isSwipeEnabled = {
                    !viewModel.isMultiselect.value && requireContext().checkForInternet()
                }
            )
        }
    }

    private fun setListeners() {
        addContacts()
        navigationBack()
        deleteSelectedContacts()
        searchView()
    }

    private fun addContacts() {
        binding.textViewAddContacts.setOnClickListener {
            val direction =
                ViewPagerFragmentDirections.actionViewPagerFragmentToAddContactsFragment()
            navController.navigate(direction)
        }
    }

    private fun navigationBack() {
        binding.imageViewNavigationBack.setOnClickListener {
            (parentFragment as? ViewPagerFragment)?.openFragment(Constants.PROFILE_FRAGMENT)
        }
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (requireParentFragment() as? ViewPagerFragment)?.openFragment(Constants.PROFILE_FRAGMENT)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun deleteSelectedContacts() {
        with(binding) {
            imageViewDeleteSelectMode.setOnClickListener {
                val size = viewModel.selectContacts.value.size
                if (viewModel.deleteSelectList(
                        userData.user.id,
                        userData.accessToken,
                        requireContext().checkForInternet()
                    )
                ) {
                    root.showErrorSnackBar(
                        requireContext(), if (size > 1) R.string.contacts_removed
                        else R.string.contact_removed
                    )
                    viewModel.changeMultiselectMode()
                }
            }
        }
    }

    private fun searchView() {
        binding.imageSearchView.setOnClickListener {
            viewModel.showNotification(requireContext())
        }
    }

    private fun setObservers() {
        with(binding) {
            lifecycleScope.launch {
                viewModel.contactList.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
                    adapter.submitList(it)
                }
            }

            lifecycleScope.launch {
                viewModel.isMultiselect.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
                    recyclerViewContacts.adapter = adapter
                    textViewAddContacts.visibility = if (it) View.GONE else View.VISIBLE
                    imageViewDeleteSelectMode.visibility = if (it) View.VISIBLE else View.GONE
                }
            }

            lifecycleScope.launch {
                viewModel.isSelectItem.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
                    adapter.setMultiselectData(it)
                }
            }

            lifecycleScope.launch {
                viewModel.usersState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
                    when (it) {
                        is ApiStateUser.Success<*> -> {
                            binding.progressBar.invisible()
                        }

                        is ApiStateUser.Initial -> Unit

                        is ApiStateUser.Loading -> {
                            binding.progressBar.visible()
                        }

                        is ApiStateUser.Error -> {
                            binding.root.showErrorSnackBar(requireContext(), it.error)
                            viewModel.changeStates()
                        }
                    }
                }
            }
        }
    }

    fun deleteUserWithRestore(contact: Contact) {
        val position = viewModel.contactList.value.indexOfFirst { it == contact }
        if (viewModel.deleteContactFromList(
                userData.user.id,
                userData.accessToken,
                contact,
                requireContext().checkForInternet()
            )
        ) {
            Snackbar.make(
                binding.recyclerViewContacts,
                getString(R.string.s_has_been_removed).format(contact.name),
                Snackbar.LENGTH_LONG
            ).setAction(getString(R.string.restore)) {
                viewModel.addContactToList(
                    userData.user.id,
                    contact,
                    userData.accessToken,
                    position
                )
            }.show()
        }
    }

}