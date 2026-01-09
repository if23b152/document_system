package at.technikum_wien.worker_service.service;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MinioServiceTest {

    // This is the MinIO client which will be mocked
    private MinioClient minioClient;

    // This is the class under test
    private MinioService minioService;

    @BeforeEach
    void setUp() {
        // Create a mock MinIO client
        minioClient = mock(MinioClient.class);

        // Create the service with the mocked client
        minioService = new MinioService(minioClient);

        // Manually inject the bucket name (because @Value is not active in unit tests)
        ReflectionTestUtils.setField(minioService, "bucketName", "test-bucket");
    }

    // When MinIO works → GetObjectResponse is returned
    @Test
    void downloadFile_success_returnsStream() throws Exception {
        // Create a fake object key
        String objectKey = "documents/test.pdf";

        // Create a fake MinIO response stream
        GetObjectResponse fakeResponse = mock(GetObjectResponse.class);

        // Mock MinIO to return our fake response
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(fakeResponse);

        // Call the method under test
        var result = minioService.downloadFile(objectKey);

        // Verify that we got back exactly the same stream
        assertSame(fakeResponse, result);

        // Verify that MinIO was called exactly once
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));

        // Verify no other MinIO calls happened
        verifyNoMoreInteractions(minioClient);
    }

    // When MinIO throws → RuntimeException is thrown
    @Test
    void downloadFile_minioThrows_exceptionIsWrapped() throws Exception {
        // Create a fake object key
        String objectKey = "documents/missing.pdf";

        // Mock MinIO to throw an exception
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("MinIO is down"));

        // Verify that our service throws a RuntimeException
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> minioService.downloadFile(objectKey)
        );

        // Verify the error message is meaningful
        assertTrue(ex.getMessage().contains("Failed to download file from MinIO"));

        // Verify that MinIO was called exactly once
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));

        // Verify no other MinIO calls happened
        verifyNoMoreInteractions(minioClient);
    }
}
