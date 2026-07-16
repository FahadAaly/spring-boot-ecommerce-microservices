package com.ecommerce.user.services;

import com.ecommerce.user.dto.AddressDTO;
import com.ecommerce.user.dto.UserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.models.Address;
import com.ecommerce.user.models.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;


    public Optional<UserResponse> fetchUser(String id) {
        return userRepository.findById(id).map(this::mapToUserResponse);
    }

    public List<UserResponse> fetchAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public void addUser(UserRequest userRequest) {
        User user = new User();
        updateUserFromRequest(user, userRequest);
        userRepository.save(user);
    }

    public Optional<User> updateUser(String id, UserRequest updatedUser) {
        return userRepository.findById(id).map(existingUser -> {
            updateUserFromRequest(existingUser, updatedUser);
            userRepository.save(existingUser);
            return existingUser;
        });
    }

    private void updateUserFromRequest(User user, UserRequest userRequest) {
        if (user == null || userRequest == null) {
            return;
        }

        // Map simple scalar fields
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setEmail(userRequest.getEmail());
        user.setPhone(userRequest.getPhone());

        // Map address if provided
        AddressDTO addressDTO = userRequest.getAddress();
        if (addressDTO != null) {
            Address address = user.getAddress();
            if (address == null) {
                address = new Address();
            }
            address.setStreet(addressDTO.getStreet());
            address.setCity(addressDTO.getCity());
            address.setState(addressDTO.getState());
            address.setCountry(addressDTO.getCountry());
            address.setZipcode(addressDTO.getZipcode());
            user.setAddress(address);
        } else {
            // If no address provided in request, clear any existing address for new users
            // and keep consistent behavior for updates via this mapper
            user.setAddress(null);
        }
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(String.valueOf(user.getId()));
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());

        if (user.getAddress() != null) {
            AddressDTO addressResponse = new AddressDTO();
            addressResponse.setStreet(user.getAddress().getStreet());
            addressResponse.setCity(user.getAddress().getCity());
            addressResponse.setState(user.getAddress().getState());
            addressResponse.setCountry(user.getAddress().getCountry());
            addressResponse.setZipcode(user.getAddress().getZipcode());
            response.setAddress(addressResponse);
        }
        return response;
    }
}
