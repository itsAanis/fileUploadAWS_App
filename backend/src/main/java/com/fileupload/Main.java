package com.fileupload;

import com.fileupload.customer.Customer;
import com.fileupload.customer.CustomerRepository;
import com.fileupload.customer.Gender;
import com.fileupload.s3.S3Buckets;
import com.fileupload.s3.S3Service;
import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    CommandLineRunner runner(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            S3Service s3Service,
            S3Buckets s3Buckets,
            S3TransferManager s3TransferManager) {
        return args -> {
         //   testBucketUploadAndDownload(  s3Service, s3Buckets, s3TransferManager);
            createRandomCustomer(customerRepository,passwordEncoder );
        };
    }

   private static void testBucketUploadAndDownload( S3Service s3Service,
                                                    S3Buckets s3Buckets,
                                                    S3TransferManager s3TransferManager) throws IOException {

       s3Service.uploadFile( s3Buckets.getCustomer(), "test" ,
               "Hello World");
       s3Service.downloadFile("fileupload-customer",
               "test" ,"downloaded.txt" );

   }
    private static void createRandomCustomer(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        var faker = new Faker();
        Random random = new Random();
        Name name = faker.name();
        String firstName = name.firstName();
        String lastName = name.lastName();
        int age = random.nextInt(16, 99);
        Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@fileupload.com";
        Customer customer = new Customer(
                firstName + " " + lastName,
                email,
                passwordEncoder.encode("password"),
                age,
                gender);
        customerRepository.save(customer);
        System.out.println(email);
    }

}
