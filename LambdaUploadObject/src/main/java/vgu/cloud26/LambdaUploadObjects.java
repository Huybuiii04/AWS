package vgu.cloud26;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


public class LambdaUploadObjects implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final String RDS_INSTANCE_HOSTNAME = "my-mysql-db.c36cecqkoyi1.ap-southeast-1.rds.amazonaws.com";
    private static final int RDS_INSTANCE_PORT = 3306;
    private static final String DB_USER = "cloud26";
    private static final String DB_PASSWORD = "anhhuy2004z";
    private static final String JDBC_URL = "jdbc:mysql://" + RDS_INSTANCE_HOSTNAME + ":" + RDS_INSTANCE_PORT + "/Cloud26";

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        
        try {
            context.getLogger().log("=== EVENT START ===");
            context.getLogger().log("Event keys: " + event.keySet().toString());
            
            // Log all event values to see what we're receiving
            for (Map.Entry<String, Object> entry : event.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "null";
                context.getLogger().log("Event[" + entry.getKey() + "] = " + value.substring(0, Math.min(200, value.length())));
            }
            
            String bucketName = "public-huy04";
            String requestBody = null;
            String content = null;
            String objName = null;
            String description = null;
            
            // Lambda Function URL sends body in the event
            if (event.containsKey("body")) {
                requestBody = (String) event.get("body");
                
                // Check if body is base64 encoded
                Boolean isBase64Encoded = (Boolean) event.get("isBase64Encoded");
                if (isBase64Encoded != null && isBase64Encoded) {
                    context.getLogger().log("Body is base64 encoded, decoding...");
                    requestBody = new String(Base64.getDecoder().decode(requestBody));
                }
                
                context.getLogger().log("Body from event (first 200 chars): " + requestBody.substring(0, Math.min(200, requestBody.length())));
                
                // Parse JSON body
                JSONObject bodyJSON = new JSONObject(requestBody);
                context.getLogger().log("JSON keys: " + bodyJSON.keySet().toString());
                
                // Support both field names
                if (bodyJSON.has("fileContent")) {
                    content = bodyJSON.getString("fileContent");
                } else if (bodyJSON.has("content")) {
                    content = bodyJSON.getString("content");
                }
                
                if (bodyJSON.has("fileName")) {
                    objName = bodyJSON.getString("fileName");
                } else if (bodyJSON.has("key")) {
                    objName = bodyJSON.getString("key");
                }
                
                if (bodyJSON.has("description")) {
                    description = bodyJSON.getString("description");
                }
            } else if (event.containsKey("fileContent") || event.containsKey("content")) {
                // Direct event (test event or direct invocation)
                context.getLogger().log("Processing direct event (no body wrapper)");
                
                if (event.containsKey("fileContent")) {
                    content = (String) event.get("fileContent");
                } else if (event.containsKey("content")) {
                    content = (String) event.get("content");
                }
                
                if (event.containsKey("fileName")) {
                    objName = (String) event.get("fileName");
                } else if (event.containsKey("key")) {
                    objName = (String) event.get("key");
                }
                
                if (event.containsKey("description")) {
                    description = (String) event.get("description");
                }
            } else {
                context.getLogger().log("ERROR: No body or direct content found in event");
                return createErrorResponse(400, "Both fileContent/content and fileName/key are required");
            }
            
            context.getLogger().log("content: " + (content != null ? "present (" + content.length() + " chars)" : "null"));
            context.getLogger().log("objName: " + (objName != null ? objName : "null"));
            context.getLogger().log("description: " + (description != null ? description : "null"));
            
            if (content == null || content.isEmpty() || objName == null || objName.isEmpty()) {
                return createErrorResponse(400, "Both fileContent/content and fileName/key are required");
            }
            
            byte[] objBytes = Base64.getDecoder().decode(content.getBytes());
            
            // Build PutObjectRequest with metadata for description
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objName);
            
            // Add description as metadata if provided
            if (description != null && !description.isEmpty()) {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("description", description);
                requestBuilder.metadata(metadata);
                context.getLogger().log("Added description to S3 metadata: " + description);
            }
            
            PutObjectRequest putObjectRequest = requestBuilder.build();

            S3Client s3Client = S3Client.builder()
                    .region(Region.AP_SOUTHEAST_1)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(objBytes));

            context.getLogger().log("Object uploaded to S3 successfully: " + objName);
            context.getLogger().log("Description stored in S3 metadata: " + (description != null ? description : "none"));
            
            // Insert into MySQL database
            boolean dbSuccess = false;
            String dbError = null;
            try {
                context.getLogger().log("=== Starting Database Insert ===");
                context.getLogger().log("JDBC URL: " + JDBC_URL);
                context.getLogger().log("DB User: " + DB_USER);
                context.getLogger().log("RDS Hostname: " + RDS_INSTANCE_HOSTNAME);
                
                context.getLogger().log("Loading MySQL driver...");
                Class.forName("com.mysql.cj.jdbc.Driver");
                context.getLogger().log("MySQL driver loaded successfully");
                
                context.getLogger().log("Generating RDS auth token...");
                Properties props = setMySqlConnectionProperties();
                context.getLogger().log("Auth token generated, attempting connection...");
                
                Connection mySQLClient = DriverManager.getConnection(JDBC_URL, props);
                context.getLogger().log("Database connection established successfully!");
                
                PreparedStatement st = mySQLClient.prepareStatement(
                    "INSERT INTO Photos (S3Key, Description) VALUES (?, ?)"
                );
                st.setString(1, objName);
                st.setString(2, description != null ? description : "");
                context.getLogger().log("Executing INSERT query...");
                
                int result = st.executeUpdate();
                context.getLogger().log("INSERT successful! Rows affected: " + result);
                
                st.close();
                mySQLClient.close();
                context.getLogger().log("Database connection closed");
                dbSuccess = true;
                
            } catch (ClassNotFoundException ex) {
                dbError = "MySQL driver not found: " + ex.getMessage();
                context.getLogger().log("ERROR: " + dbError);
                ex.printStackTrace();
            } catch (java.sql.SQLException ex) {
                dbError = "SQL Exception: " + ex.getClass().getName() + " - " + ex.getMessage();
                context.getLogger().log("ERROR: " + dbError);
                context.getLogger().log("SQL State: " + ex.getSQLState());
                context.getLogger().log("Error Code: " + ex.getErrorCode());
                ex.printStackTrace();
            } catch (Exception ex) {
                dbError = "General Exception: " + ex.getClass().getName() + " - " + ex.getMessage();
                context.getLogger().log("ERROR: " + dbError);
                ex.printStackTrace();
            }
            
            if (dbSuccess) {
                return createSuccessResponse("File uploaded successfully with database record: " + objName);
            } else {
                return createSuccessResponse("File uploaded to S3: " + objName + " (database insert failed: " + dbError + ")");
            }
            
        } catch (Exception e) {
            context.getLogger().log("ERROR: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("body", "{\"message\": \"" + message + "\"}");
        return response;
    }
    
    private Map<String, Object> createErrorResponse(int statusCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("body", "{\"error\": \"" + message + "\"}");
        return response;
    }
    
    private static Properties setMySqlConnectionProperties() {
        Properties mysqlConnectionProperties = new Properties();
        mysqlConnectionProperties.setProperty("useSSL", "true");
        mysqlConnectionProperties.setProperty("user", DB_USER);
        mysqlConnectionProperties.setProperty("password", DB_PASSWORD);
        return mysqlConnectionProperties;
    }

}