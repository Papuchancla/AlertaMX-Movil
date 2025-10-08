package com.example.alertamx;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {
    private AuthRepository authRepository;
    private MutableLiveData<FirebaseUser> userLiveData;
    private MutableLiveData<String> errorLiveData;
    private MutableLiveData<Boolean> loadingLiveData;
    private MutableLiveData<User> currentUserData;

    public AuthViewModel() {
        authRepository = new AuthRepository();
        userLiveData = authRepository.getUserLiveData();
        errorLiveData = authRepository.getErrorLiveData();
        loadingLiveData = authRepository.getLoadingLiveData();
        currentUserData = new MutableLiveData<>();

        // Observar cambios en el usuario de Firebase y convertirlo a nuestro modelo User
        userLiveData.observeForever(firebaseUser -> {
            if (firebaseUser != null) {
                User user = new User(
                        firebaseUser.getUid(),
                        firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Usuario",
                        firebaseUser.getEmail()
                );
                currentUserData.setValue(user);
            } else {
                currentUserData.setValue(null);
            }
        });
    }

    public void register(String name, String email, String password) {
        authRepository.register(name, email, password);
    }

    // Nuevo método para login
    public void login(String email, String password) {
        authRepository.login(email, password);
    }

    // Verificar usuario actual
    public void checkCurrentUser() {
        authRepository.checkCurrentUser();
    }

    // Cerrar sesión
    public void logout() {
        authRepository.logout();
    }

    // Métodos originales (mantener compatibilidad)
    public MutableLiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public MutableLiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public MutableLiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    // Nuevos métodos para la funcionalidad de reportes
    public MutableLiveData<User> getCurrentUser() {
        return currentUserData;
    }

    public boolean isUserLoggedIn() {
        return userLiveData.getValue() != null;
    }

    public String getCurrentUserId() {
        User user = currentUserData.getValue();
        return user != null ? user.getUid() : null;
    }

    public User getCurrentUserData() {
        return currentUserData.getValue();
    }

    public String getCurrentUserName() {
        User user = currentUserData.getValue();
        return user != null ? user.getName() : null;
    }

    public String getCurrentUserEmail() {
        User user = currentUserData.getValue();
        return user != null ? user.getEmail() : null;
    }
}