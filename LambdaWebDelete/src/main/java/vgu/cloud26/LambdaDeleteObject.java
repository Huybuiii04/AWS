package vgu.cloud26;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.json.JSONObject;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.HashMap;
import java.util.Map;

public class LambdaDeleteObject implements RequestHandler<Object, Map<String, Object>> {

    private final S3Client s3Client = S3Client.builder().build();
    private static final String SOURCE_BUCKET = "public-huy04";
    private static final String RESIZED_BUCKET = "resized-public-huy04";

    @Override
    public Map<String, Object> handleRequest(Object input, Context context) {
        context.getLogger().log("Received request: " + input);
        
        // Check if this is an S3 Event (triggered by S3 deletion)
        if (input instanceof S3Event) {
            return handleS3Event((S3Event) input, context);
        }
        
        // Otherwise, treat as web request (Function URL)
        if (input instanceof Map) {
            return handleWebRequest((Map<String, Object>) input, context);
        }
        
        return createErrorResponse(400, "Unknown event type");
    }
    
    // Handle S3 event notification (when file deleted from S3 Console)
    private Map<String, Object> handleS3Event(S3Event s3event, Context context) {
        context.getLogger().log("Processing S3 event");
        
        if (s3event.getRecords() == null || s3event.getRecords().isEmpty()) {
            context.getLogger().log("No S3 event records");
            return createSuccessResponse("No records to process");
        }
        
        try {
            S3EventNotification.S3EventNotificationRecord record = s3event.getRecords().get(0);
            String objectKey = record.getS3().getObject().getKey();
            
            context.getLogger().log("S3 delete event for: " + objectKey);
            
            // Only delete from resized bucket (original already deleted)
            deleteFromBucket(RESIZED_BUCKET, objectKey, context);
            
            return createSuccessResponse("Deleted resized image: " + objectKey);
            
        } catch (Exception e) {
            context.getLogger().log("Error processing S3 event: " + e.getMessage());
            return createErrorResponse(500, e.getMessage());
        }
    }
    
    // Handle web request (from HTML delete button)
    private Map<String, Object> handleWebRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Processing web request");

        try {
            // Parse the request body
            String body = (String) event.get("body");
            if (body == null || body.isEmpty()) {
                return createErrorResponse(400, "Request body is required");
            }

            // Handle base64 encoded body if necessary
            Boolean isBase64Encoded = (Boolean) event.get("isBase64Encoded");
            if (isBase64Encoded != null && isBase64Encoded) {
                body = new String(java.util.Base64.getDecoder().decode(body));
            }

            JSONObject jsonBody = new JSONObject(body);
            String key = jsonBody.optString("key", null);

            if (key == null || key.isEmpty()) {
                return createErrorResponse(400, "Key is required");
            }

            context.getLogger().log("Deleting object: " + key + " from both buckets");

            // Delete from both buckets
            deleteFromBucket(SOURCE_BUCKET, key, context);
            // Add "resized-" prefix for resized bucket
            deleteFromBucket(RESIZED_BUCKET, "resized-" + key, context);

            String successMessage = String.format("Successfully deleted: %s from both buckets", key);
            context.getLogger().log(successMessage);

            return createSuccessResponse(successMessage);

        } catch (S3Exception e) {
            String errorMessage = "S3 Error: " + e.awsErrorDetails().errorMessage();
            context.getLogger().log(errorMessage);
            return createErrorResponse(500, errorMessage);
        } catch (Exception e) {
            String errorMessage = "Error: " + e.getMessage();
            context.getLogger().log(errorMessage);
            e.printStackTrace();
            return createErrorResponse(500, errorMessage);
        }
    }
    
    // Helper method to delete from a bucket
    private void deleteFromBucket(String bucket, String key, Context context) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            
            s3Client.deleteObject(deleteRequest);
            context.getLogger().log("Deleted from " + bucket + ": " + key);
        } catch (S3Exception e) {
            context.getLogger().log("Error deleting from " + bucket + ": " + e.getMessage());
            throw e;
        }
    }

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        
        JSONObject body = new JSONObject();
        body.put("message", message);
        response.put("body", body.toString());
        
        return response;
    }

    private Map<String, Object> createErrorResponse(int statusCode, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        
        JSONObject body = new JSONObject();
        body.put("error", errorMessage);
        response.put("body", body.toString());
        
        return response;
    }
}
