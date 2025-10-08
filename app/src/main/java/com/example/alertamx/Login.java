package com.example.alertamx;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Vincular vistas
        editTextEmail = findViewById(R.id.editTextTextEmailAddress);
        editTextPassword = findViewById(R.id.editTextTextPassword2);
        buttonLogin = findViewById(R.id.button);
        textViewRegister = findViewById(R.id.textViewRegistrar);

        // Verificar si ya hay un usuario autenticado
        authViewModel.checkCurrentUser();

        // Configurar observadores
        setupObservers();

        // Configurar listeners
        setupListeners();
    }

    private void setupObservers() {
        // Observar éxito en login
        authViewModel.getUserLiveData().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                Toast.makeText(Login.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                redirectToMainActivity();
            }
        });

        // Observar errores
        authViewModel.getErrorLiveData().observe(this, errorMessage -> {
            if (!TextUtils.isEmpty(errorMessage)) {
                Toast.makeText(Login.this, errorMessage, Toast.LENGTH_LONG).show();
                enableLoginButton(); // Rehabilitar botón en caso de error
            }
        });

        // Observar estado de carga
        authViewModel.getLoadingLiveData().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                disableLoginButton();
            } else {
                enableLoginButton();
            }
        });
    }

    private void setupListeners() {
        // Listener del botón de login
        buttonLogin.setOnClickListener(v -> {
            attemptLogin();
        });

        // Listener para campo de contraseña (login al presionar enter)
        editTextPassword.setOnEditorActionListener((v, actionId, event) -> {
            attemptLogin();
            return true;
        });

        // Listener para ir al registro
        textViewRegister.setOnClickListener(v -> {
            redirectToRegister();
        });
    }

    private void attemptLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validaciones locales mejoradas
        if (!validateInputs(email, password)) {
            return;
        }

        // Limpieza básica de inputs
        email = sanitizeEmail(email);

        // Llamar al ViewModel para login
        authViewModel.login(email, password);
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("El correo es requerido");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Formato de email inválido");
            isValid = false;
        } else {
            editTextEmail.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("La contraseña es requerida");
            isValid = false;
        } else if (password.length() < 6) {
            editTextPassword.setError("La contraseña debe tener al menos 6 caracteres");
            isValid = false;
        } else if (password.length() > 128) {
            editTextPassword.setError("La contraseña es demasiado larga");
            isValid = false;
        } else {
            editTextPassword.setError(null);
        }

        return isValid;
    }

    private String sanitizeEmail(String email) {
        // Eliminar espacios en blanco al inicio y final
        return email.trim();
    }

    private void disableLoginButton() {
        buttonLogin.setEnabled(false);
        buttonLogin.setText("Iniciando sesión...");
    }

    private void enableLoginButton() {
        buttonLogin.setEnabled(true);
        buttonLogin.setText("Iniciar Sesión");
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(Login.this, ReportFormActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToRegister() {
        Intent intent = new Intent(Login.this, registrer.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Limpiar campos al regresar a esta actividad
        editTextPassword.setText("");
        enableLoginButton();
    }
}