class AddEditUserDialogFragment : DialogFragment() {
    
    private var _binding: DialogAddEditUserBinding? = null
    private val binding get() = _binding!!
    
    private var user: User? = null
    private var onUserSaved: ((User) -> Unit)? = null
    
    companion object {
        fun newInstance(
            user: User? = null,
            onUserSaved: (User) -> Unit
        ): AddEditUserDialogFragment {
            return AddEditUserDialogFragment().apply {
                this.user = user
                this.onUserSaved = onUserSaved
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditUserBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }
    
    private fun setupUI() {
        val isEdit = user != null
        binding.tvTitle.text = if (isEdit) "Edit Pengguna" else "Tambah Pengguna"
        
        // Setup role spinner
        val roles = arrayOf("kasir", "admin")
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRole.adapter = roleAdapter
        
        // Fill fields if editing
        user?.let { user ->
            binding.etUsername.setText(user.username)
            binding.etFullName.setText(user.fullName)
            binding.etPassword.setText(user.password)
            
            // Set role selection
            val roleIndex = roles.indexOf(user.role)
            if (roleIndex >= 0) {
                binding.spinnerRole.setSelection(roleIndex)
            }
            
            // Disable username edit for existing users
            binding.etUsername.isEnabled = false
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveUser()
        }
        
        binding.btnGeneratePassword.setOnClickListener {
            generateRandomPassword()
        }
    }
    
    private fun saveUser() {
        val username = binding.etUsername.text.toString().trim()
        val fullName = binding.etFullName.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val role = binding.spinnerRole.selectedItem.toString()
        
        // Validation
        if (username.isEmpty()) {
            binding.etUsername.error = "Username harus diisi"
            return
        }
        
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Nama lengkap harus diisi"
            return
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "Password harus diisi"
            return
        }
        
        if (password.length < 6) {
            binding.etPassword.error = "Password minimal 6 karakter"
            return
        }
        
        val newUser = if (user != null) {
            // Update existing user
            user!!.copy(
                fullName = fullName,
                password = password,
                role = role
            )
        } else {
            // Create new user
            User(
                id = "user_${System.currentTimeMillis()}",
                username = username,
                password = password,
                role = role,
                fullName = fullName
            )
        }
        
        onUserSaved?.invoke(newUser)
        dismiss()
    }
    
    private fun generateRandomPassword() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val password = (1..8)
            .map { chars.random() }
            .joinToString("")
        
        binding.etPassword.setText(password)
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
