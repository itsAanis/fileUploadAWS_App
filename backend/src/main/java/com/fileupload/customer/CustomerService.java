package com.fileupload.customer;

import com.fileupload.exception.DuplicateResourceException;
import com.fileupload.exception.RequestValidationException;
import com.fileupload.exception.ResourceNotFoundException;
import com.fileupload.s3.S3Buckets;
import com.fileupload.s3.S3Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerDao customerDao;
    private final CustomerDTOMapper customerDTOMapper;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final S3Buckets s3Buckets;

    public CustomerService(@Qualifier("jdbc") CustomerDao customerDao,
                           CustomerDTOMapper customerDTOMapper,
                           PasswordEncoder passwordEncoder, S3Service s3Service, S3Buckets s3Buckets) {
        this.customerDao = customerDao;
        this.customerDTOMapper = customerDTOMapper;
        this.passwordEncoder = passwordEncoder;
        this.s3Service = s3Service;
        this.s3Buckets = s3Buckets;
    }

    public List<CustomerDTO> getAllCustomers() {
        return customerDao.selectAllCustomers()
                .stream()
                .map(customerDTOMapper)
                .collect(Collectors.toList());
    }

    public CustomerDTO getCustomer(Integer id) {
        return customerDao.selectCustomerById(id)
                .map(customerDTOMapper)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(id)
                ));
    }

    public void addCustomer(CustomerRegistrationRequest customerRegistrationRequest) {
        // check if email exists
        String email = customerRegistrationRequest.email();
        if (customerDao.existsCustomerWithEmail(email)) {
            throw new DuplicateResourceException(
                    "email already taken"
            );
        }

        // add
        Customer customer = new Customer(
                customerRegistrationRequest.name(),
                customerRegistrationRequest.email(),
                passwordEncoder.encode(customerRegistrationRequest.password()),
                customerRegistrationRequest.age(),
                customerRegistrationRequest.gender());

        customerDao.insertCustomer(customer);
    }

    public void deleteCustomerById(Integer customerId) {
        if (!customerDao.existsCustomerById(customerId)) {
            throw new ResourceNotFoundException(
                    "customer with id [%s] not found".formatted(customerId)
            );
        }

        customerDao.deleteCustomerById(customerId);
    }

    public void updateCustomer(Integer customerId,
                               CustomerUpdateRequest updateRequest) {
        // TODO: for JPA use .getReferenceById(customerId) as it does does not bring object into memory and instead a reference
        Customer customer = customerDao.selectCustomerById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(customerId)
                ));

        boolean changes = false;

        if (updateRequest.name() != null && !updateRequest.name().equals(customer.getName())) {
            customer.setName(updateRequest.name());
            changes = true;
        }

        if (updateRequest.age() != null && !updateRequest.age().equals(customer.getAge())) {
            customer.setAge(updateRequest.age());
            changes = true;
        }

        if (updateRequest.email() != null && !updateRequest.email().equals(customer.getEmail())) {
            if (customerDao.existsCustomerWithEmail(updateRequest.email())) {
                throw new DuplicateResourceException(
                        "email already taken"
                );
            }
            customer.setEmail(updateRequest.email());
            changes = true;
        }

        if (!changes) {
           throw new RequestValidationException("no data changes found");
        }

        customerDao.updateCustomer(customer);
    }

    public void uploadCustomerImage(Integer customerId, MultipartFile multipartFile) {
        if (!customerDao.existsCustomerById(customerId)) {
            throw new ResourceNotFoundException(
                    "customer with id [%s] not found".formatted(customerId)
            );
        }
        String profileImageId = UUID.randomUUID().toString();
        String key = "profile-images/%s/%s".formatted(customerId, profileImageId);
        try {
            Path tempFile = Files.createTempFile("upload-", ".tmp");
            Files.write(tempFile, multipartFile.getBytes());

        s3Service.uploadFile(
                s3Buckets.getCustomer(),
                key,
                tempFile.toString()
             //   multipartFile.toString()
        );
            // Clean up the temp file
            Files.deleteIfExists(tempFile);
            // store image to db
            customerDao.updateCustomerProfileImageId(profileImageId, customerId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }


    }

    public byte[] getCustomerImage(Integer customerId) throws IOException {

        CustomerDTO customer = customerDao.selectCustomerById(customerId)
                .map(customerDTOMapper)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(customerId)
                ));
        if (customer.profileImageId() == null || customer.profileImageId().isBlank()) {
            throw new ResourceNotFoundException(
                    "customer with id [%s] profile image not found".formatted(customerId));
        }
       String profileImageId = customer.profileImageId();
       String key = "profile-images/%s/%s".formatted(customerId,profileImageId);

        // Create a temporary file path to save the downloaded file
        Path tempFile = Files.createTempFile("download-", ".tmp");

        try {
            String localFilePath = tempFile.toString();

            s3Service.downloadFile(
                    s3Buckets.getCustomer(),
                    key,
                    localFilePath
            );
            // Read the downloaded file's bytes and return them
            return Files.readAllBytes(tempFile);
        }
        finally {
            // Optionally, delete the temp file if you no longer need it:
            Files.delete(tempFile);
        }

    }
}



