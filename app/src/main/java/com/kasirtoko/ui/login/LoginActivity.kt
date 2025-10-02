class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var userDao: UserDao
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        userDao = AppDatabase.getDatabase(this).userDao()
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            login()
        }
    }
    
    private fun login() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username dan password harus diisi", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val user = userDao.login(username, password)
                
                if (user != null) {
                    // Save user session
                    val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("user_id", user.id)
                        putString("username", user.username)
                        putString("role", user.role)
                        putString("full_name", user.fullName)
                        apply()
                    }
                    
                    // Navigate to appropriate activity based on role
                    val intent = if (user.role == "admin") {
                        Intent(this@LoginActivity, AdminMainActivity::class.java)
                    } else {
                        Intent(this@LoginActivity, KasirMainActivity::class.java)
                    }
                    
                    startActivity(intent)
                    finish()
                    
                } else {
                    Toast.makeText(this@LoginActivity, "Username atau password salah", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
