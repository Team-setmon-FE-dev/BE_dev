package com.example.setmon.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties props;

    @Override
    public String presignPut(String key, String contentType, Duration ttl) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.bucket()).key(key).contentType(contentType).build();
        return presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(ttl).putObjectRequest(put).build())
                .url().toString();
    }

    @Override
    public String presignGet(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(props.bucket()).key(key).build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(ttl).getObjectRequest(get).build())
                .url().toString();
    }

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(props.bucket()).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public void upload(String key, Path file, String contentType) {
        s3.putObject(PutObjectRequest.builder().bucket(props.bucket()).key(key).contentType(contentType).build(),
                RequestBody.fromFile(file));
    }

    @Override
    public void download(String key, Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        s3.getObject(GetObjectRequest.builder().bucket(props.bucket()).key(key).build(),
                ResponseTransformer.toFile(target));
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(props.bucket()).key(key).build());
    }

    @Override
    public void deleteByPrefix(String prefix) {
        ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(props.bucket()).prefix(prefix).build();
        for (ListObjectsV2Response page : s3.listObjectsV2Paginator(request)) {
            List<ObjectIdentifier> ids = page.contents().stream()
                    .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                    .toList();
            if (!ids.isEmpty()) {
                s3.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(props.bucket())
                        .delete(Delete.builder().objects(ids).build())
                        .build());
            }
        }
    }
}
