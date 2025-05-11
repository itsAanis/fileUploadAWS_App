package com.fileupload.s3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;


@Configuration
public class S3Config {

    @Value("${aws.region}")
    private String awsRegion;
    @Bean
    public S3AsyncClient s3Cleint () {
        S3AsyncClient s3AsyncClient = S3AsyncClient.builder()
                .region(Region.of(awsRegion))
                .build();
        return s3AsyncClient;
    }

    @Bean
    public S3TransferManager s3TransferManager(S3AsyncClient s3AsyncClient) {
        S3TransferManager transferManager = S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
        return transferManager;
    }

}
