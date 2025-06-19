package com.example.taskhero.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.taskhero.R;
import com.example.taskhero.data.model.User;
import com.example.taskhero.databinding.FragmentEditProfileBinding;
import com.example.taskhero.ui.base.BasePhotoFragment;
import com.example.taskhero.util.UIUtils;

import java.util.Objects;

public class EditProfileFragment extends BasePhotoFragment {

    private FragmentEditProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private User currentUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        if (getArguments() != null) {
            int userId = getArguments().getInt("USER_ID", -1);
            if (userId != -1) {
                profileViewModel.getUser(userId).observe(getViewLifecycleOwner(), user -> {
                    if (user != null) {
                        currentUser = user;
                        populateUI();
                    }
                });
            }
        }

        setupClickListeners();
        setupObservers();
    }

    private void populateUI() {
        binding.editTextNameEdit.setText(currentUser.getName());
        binding.editTextEmailEdit.setText(currentUser.getEmail());
        if (currentUser.getPhotoUri() != null && !currentUser.getPhotoUri().isEmpty()) {
            binding.imageViewEditProfile.setImageTintList(null);
            Glide.with(this).load(Uri.parse(currentUser.getPhotoUri())).into(binding.imageViewEditProfile);
        }
    }

    @Override
    protected void onPhotoSelected(Uri uri) {
        binding.imageViewEditProfile.setImageTintList(null);
        binding.imageViewEditProfile.setImageURI(uri);
    }

    private void setupClickListeners() {
        binding.imageViewEditProfile.setOnClickListener(v -> showPhotoSourceDialog());
        binding.buttonSaveProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void saveProfileChanges() {
        if (currentUser == null || !isFormValid()) {
            return;
        }

        String finalPhotoUriString = getFinalPhotoUriString(selectedImageUri, currentUser.getPhotoUri());

        if (finalPhotoUriString == null && selectedImageUri != null) {
            return;
        }

        String newName = Objects.requireNonNull(binding.editTextNameEdit.getText()).toString().trim();
        String newEmail = Objects.requireNonNull(binding.editTextEmailEdit.getText()).toString().trim();

        profileViewModel.updateUserProfile(currentUser, newName, newEmail, finalPhotoUriString);

    }

    private boolean isFormValid() {
        boolean nameIsValid = validateName();
        boolean emailIsValid = validateEmail();
        return nameIsValid & emailIsValid;
    }

    private boolean validateName() {
        String newName = Objects.requireNonNull(binding.editTextNameEdit.getText()).toString().trim();
        if (newName.isEmpty()) {
            binding.textInputLayoutNameEdit.setError(getString(R.string.edit_profile_error_name_required));
            return false;
        } else {
            binding.textInputLayoutNameEdit.setError(null);
            return true;
        }
    }

    private boolean validateEmail() {
        String email = Objects.requireNonNull(binding.editTextEmailEdit.getText()).toString().trim();
        if (email.isEmpty()) {
            binding.textInputLayoutEmailEdit.setError(getString(R.string.edit_profile_error_email_required));
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayoutEmailEdit.setError(getString(R.string.edit_profile_error_invalid_email));
            return false;
        } else {
            binding.textInputLayoutEmailEdit.setError(null);
            return true;
        }
    }

    private void setupObservers() {
        profileViewModel.getUpdateSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                UIUtils.showSuccessSnackbar(requireView(), getString(R.string.edit_profile_snackbar_success));
                requireActivity().getSupportFragmentManager().popBackStack();
                profileViewModel.onUpdateHandled();
            }
        });

        profileViewModel.getUpdateError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                UIUtils.showErrorSnackbar(requireView(), error);
                profileViewModel.onUpdateHandled();
            }
        });
    }

    private String getFinalPhotoUriString(Uri newUri, String currentUri) {
        if (newUri != null) {
            Uri internalUri = saveImageToInternalStorage(newUri);
            if (internalUri != null) {
                return internalUri.toString();
            } else {
                UIUtils.showErrorSnackbar(requireView(), getString(R.string.edit_profile_error_saving_image));
                return null;
            }
        }
        return currentUri;
    }
}