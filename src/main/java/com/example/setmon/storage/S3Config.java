package com.example.setmon.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(StorageProperties props) {
        if (props.hasStaticCredentials()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.accessKeyId(), props.secretAccessKey()));
        }
        // 키가 없으면 EC2 IAM 역할 등 기본 체인 사용
        return DefaultCredentialsProvider.builder().build();
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client(StorageProperties props, AwsCredentialsProvider credentials) {
        return S3Client.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials)
                .build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(StorageProperties props, AwsCredentialsProvider credentials) {
        return S3Presigner.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentials)
                .build();
    }
}
