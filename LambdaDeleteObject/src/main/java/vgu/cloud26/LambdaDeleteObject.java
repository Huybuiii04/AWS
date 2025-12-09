package vgu.cloud26;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class LambdaDeleteObject implements RequestHandler<S3Event, String> {

    private final S3Client s3Client = S3Client.builder().build();
    private static final String RESIZED_BUCKET = "resized-public-huy04";

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        context.getLogger().log("Received S3 delete event");

        // Validate that event contains records
        if (s3event.getRecords() == null || s3event.getRecords().isEmpty()) {
            context.getLogger().log("No S3 event records to process");
            return "No S3 event records to process";
        }

        try {
            // Get the deleted object information from the source bucket
            S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().get(0);
            String objectKey = record.getS3().getObject().getKey();
            
            context.getLogger().log("Processing deletion for object: " + objectKey);

            // Delete the corresponding resized image from the resized bucket
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(RESIZED_BUCKET)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
            
            String successMessage = String.format(
                "Successfully deleted resized image: %s from bucket: %s", 
                objectKey, RESIZED_BUCKET
            );
            context.getLogger().log(successMessage);
            
            return successMessage;

        } catch (S3Exception e) {
            String errorMessage = String.format(
                "Error deleting resized image: %s", 
                e.awsErrorDetails().errorMessage()
            );
            context.getLogger().log(errorMessage);
            return errorMessage;
        } catch (RuntimeException e) {
            String errorMessage = "Unexpected error: " + e.getMessage();
            context.getLogger().log(errorMessage);
            context.getLogger().log("Stack trace: " + e.toString());
            return errorMessage;
        }
    }
}
