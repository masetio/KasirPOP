package com.kasirtoko.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kasirtoko.R
import com.kasirtoko.data.entities.User
import com.kasirtoko.databinding.ItemUserBinding
import com.kasirtoko.utils.formatDateTime

class UserAdapter(
    private val onEditClick: (User) -> Unit,
    private val onToggleActiveClick: (User) -> Unit,
    private val onResetPasswordClick: (User) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvUsername.text = user.username
                tvFullName.text = user.fullName
                tvRole.text = user.role.uppercase()
                tvCreatedAt.text = "Dibuat: ${user.createdAt.formatDateTime()}"
                
                // Set role chip color
                val roleColor = if (user.role == "admin") {
                    R.color.primary
                } else {
                    R.color.secondary
                }
                chipRole.setChipBackgroundColorResource(roleColor)
                chipRole.text = user.role.uppercase()
                
                // Set status indicator
                if (user.isActive) {
                    indicatorStatus.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                    )
                    tvStatus.text = "Aktif"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                    )
                    btnToggleActive.text = "Nonaktifkan"
                } else {
                    indicatorStatus.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.error)
                    )
                    tvStatus.text = "Nonaktif"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.error)
                    )
                    btnToggleActive.text = "Aktifkan"
                }
                
                // Set user icon based on role
                if (user.role == "admin") {
                    ivUserIcon.setImageResource(R.drawable.ic_admin)
                    ivUserIcon.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.primary)
                    )
                } else {
                    ivUserIcon.setImageResource(R.drawable.ic_user)
                    ivUserIcon.setColorFilter(
                        ContextCompat.getColor(itemView.context, R.color.secondary)
                    )
                }
                
                btnEdit.setOnClickListener {
                    onEditClick(user)
                }
                
                btnToggleActive.setOnClickListener {
                    onToggleActiveClick(user)
                }
                
                btnResetPassword.setOnClickListener {
                    onResetPasswordClick(user)
                }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
