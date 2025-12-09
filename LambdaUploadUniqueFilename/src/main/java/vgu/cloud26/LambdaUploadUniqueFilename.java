package vgu.cloud26;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Base64;
import java.util.UUID;

import org.json.JSONObject;

public class LambdaUploadUniqueFilename implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String BUCKET_NAME = "your-bucket-name";
    private final S3Client s3Client = S3Client.builder().build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        JSONObject response = new JSONObject();
        
        try {
            // Get the base64 encoded file content from request body
            String body = request.getBody();
            JSONObject requestBody = new JSONObject(body);
            
            String base64Content = requestBody.getString("content");
            String originalFileName = requestBody.optString("fileName", "file");
            String contentType = requestBody.optString("contentType", "application/octet-stream");
            
            // Decode base64 content
            byte[] fileContent = Base64.getDecoder().decode(base64Content);
            
            // Generate unique filename
            String extension = "";
            int lastDot = originalFileName.lastIndexOf('.');
            if (lastDot > 0) {
                extension = originalFileName.substring(lastDot);
            }
            String uniqueFileName = UUID.randomUUID().toString() + extension;
            
            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(uniqueFileName)
                    .contentType(contentType)
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));
            
            response.put("success", true);
            response.put("fileName", uniqueFileName);
            response.put("s3Key", uniqueFileName);
            response.put("bucket", BUCKET_NAME);
            
            logger.log("File uploaded successfully: " + uniqueFileName);
            
        } catch (Exception ex) {
            logger.log("Error: " + ex.toString());
            response.put("success", false);
            response.put("error", ex.getMessage());
        }
        
        APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
        apiResponse.setStatusCode(200);
        apiResponse.setBody(response.toString());
        apiResponse.setHeaders(java.util.Collections.singletonMap("Content-Type", "application/json"));
        
        return apiResponse;
    }
}
