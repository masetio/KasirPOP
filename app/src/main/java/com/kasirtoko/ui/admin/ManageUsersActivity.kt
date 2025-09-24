class ManageUsersActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityManageUsersBinding
    private lateinit var userAdapter: UserAdapter
    private lateinit var userDao: UserDao
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userDao = AppDatabase.getDatabase(this).userDao()
        
        setupUI()
        setupRecyclerView()
        loadUsers()
    }
    
    private fun setupUI() {
        binding.toolbar.title = "Kelola Pengguna"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.fabAddUser.setOnClickListener {
            showAddUserDialog()
        }
    }
    
    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            onEditClick = { user ->
                showEditUserDialog(user)
            },
            onToggleActiveClick = { user ->
                toggleUserActive(user)
            },
            onResetPasswordClick = { user ->
                showResetPasswordDialog(user)
            }
        )
        
        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(this@ManageUsersActivity)
            addItemDecoration(DividerItemDecoration(this@ManageUsersActivity, DividerItemDecoration.VERTICAL))
        }
    }
    
    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                val users = userDao.getAllActiveUsers()
                withContext(Dispatchers.Main) {
                    userAdapter.submitList(users)
                    binding.tvUserCount.text = "${users.size} pengguna"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageUsersActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showAddUserDialog() {
        val dialog = AddEditUserDialogFragment.newInstance(
            user = null,
            onUserSaved = { user ->
                saveUser(user)
            }
        )
        dialog.show(supportFragmentManager, "add_user")
    }
    
    private fun showEditUserDialog(user: User) {
        val dialog = AddEditUserDialogFragment.newInstance(
            user = user,
            onUserSaved = { updatedUser ->
                updateUser(updatedUser)
            }
        )
        dialog.show(supportFragmentManager, "edit_user")
    }
    
    private fun saveUser(user: User) {
        lifecycleScope.launch {
            try {
                userDao.insertUser(user)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageUsersActivity, "Pengguna berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    loadUsers()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageUsersActivity, "Gagal menambahkan pengguna: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateUser(user: User) {
        lifecycleScope.launch {
            try {
                userDao.updateUser(user)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageUsersActivity, "Pengguna berhasil diupdate", Toast.LENGTH_SHORT).show()
                    loadUsers()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageUsersActivity, "Gagal mengupdate pengguna: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun toggleUserActive(user: User) {
        val action = if (user.isActive) "menonaktifkan" else "mengaktifkan"
        
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi")
            .setMessage("Apakah Anda yakin ingin $action pengguna ${user.fullName}?")
            .setPositiveButton("Ya") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val updatedUser = user.copy(isActive = !user.isActive)
                        userDao.updateUser(updatedUser)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ManageUsersActivity,
                                "Status pengguna berhasil diubah",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadUsers()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ManageUsersActivity,
                                "Gagal mengubah status: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    
    private fun showResetPasswordDialog(user: User) {
        val dialog = ResetPasswordDialogFragment.newInstance(
            user = user,
            onPasswordReset = { newPassword ->
                resetPassword(user, newPassword)
            }
        )
        dialog.show(supportFragmentManager, "reset_password")
    }
    
    private fun resetPassword(user: User, newPassword: String) {
        lifecycleScope.launch {
            try {
                val updatedUser = user.copy(password = newPassword)
                userDao.updateUser(updatedUser)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ManageUsersActivity,
                        "Password berhasil direset",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ManageUsersActivity,
                        "Gagal mereset password: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
