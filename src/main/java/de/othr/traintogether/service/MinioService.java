package de.othr.traintogether.service;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioService(MinioClient minioClient,
                        @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );

                String policy = """
                        {
                            "Version": "2012-10-17",
                            "Statement": [
                                {
                                    "Effect": "Allow",
                                    "Principal": {"AWS": "*"},
                                    "Action": ["s3:GetObject"],
                                    "Resource": ["arn:aws:s3:::%s/*"]
                                }
                            ]
                        }
                        """.formatted(bucketName);
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policy)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket exists: " + e.getMessage(), e);
        }
    }

    public String uploadProfilePicture(MultipartFile file, Long userId) {
        validateFile(file);
        return saveFile(file, "profile-pictures", String.valueOf(userId));
    }

    public void deleteProfilePicture(String urlOrObjectName) {
        deleteObjectByUrlOrName(urlOrObjectName);
    }

    public String uploadGroupPicture(MultipartFile file, Long groupId) {
        validateFile(file);
        return saveFile(file, "group-pictures", String.valueOf(groupId));
    }

    public void deleteGroupPicture(String urlOrObjectName) {
        deleteObjectByUrlOrName(urlOrObjectName);
    }

    public String uploadGymBanner(MultipartFile file, Long gymId) {
        validateFile(file);
        return saveFile(file, "gym-banners", String.valueOf(gymId));
    }

    public void deleteGymBanner(String urlOrObjectName) {
        deleteObjectByUrlOrName(urlOrObjectName);
    }

    public String getPresignedUrl(String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    private String getPublicUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get public URL: " + e.getMessage(), e);
        }
    }

    // Reusable save/upload helper
    private String saveFile(MultipartFile file, String folder, String id) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";

        String objectName = folder + "/" + id + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Return the public URL
            return getPublicUrl(objectName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    private void deleteObjectByUrlOrName(String urlOrObjectName) {
        if (urlOrObjectName == null || urlOrObjectName.isEmpty()) {
            return;
        }

        try {
            String objectName = urlOrObjectName;
            if (urlOrObjectName.startsWith("http")) {
                int bucketIndex = urlOrObjectName.indexOf("/" + bucketName + "/");
                if (bucketIndex != -1) {
                    objectName = urlOrObjectName.substring(bucketIndex + bucketName.length() + 2);
                    int queryIndex = objectName.indexOf("?");
                    if (queryIndex != -1) {
                        objectName = objectName.substring(0, queryIndex);
                    }
                }
            }

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            // Log but don't throw - deletion failures shouldn't stop updates
            System.err.println("Failed to delete object: " + e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Max 5MB (configured in application.properties)
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (5MB)");
        }
    }
}
