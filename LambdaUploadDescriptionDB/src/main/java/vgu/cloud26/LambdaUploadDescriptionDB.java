package vgu.cloud26;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;

import org.json.JSONObject;

public class LambdaUploadDescriptionDB implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String RDS_INSTANCE_HOSTNAME = "rds-tbt.c9bobnrufdr9.ap-southeast-1.rds.amazonaws.com";
    private static final int RDS_INSTANCE_PORT = 3306;
    private static final String DB_USER = "cloud26";
    private static final String JDBC_URL = "jdbc:mysql://" + RDS_INSTANCE_HOSTNAME + ":" + RDS_INSTANCE_PORT + "/Cloud26";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        JSONObject response = new JSONObject();
        
        try {
            // Parse request body
            String body = request.getBody();
            JSONObject requestBody = new JSONObject(body);
            
            String description = requestBody.getString("description");
            String s3Key = requestBody.getString("s3Key");
            
            // Connect to database
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection mySQLClient = DriverManager.getConnection(JDBC_URL, setMySqlConnectionProperties());
            
            // Insert into Photos table
            PreparedStatement st = mySQLClient.prepareStatement(
                    "INSERT INTO Photos (Description, S3Key) VALUES (?, ?)"
            );
            st.setString(1, description);
            st.setString(2, s3Key);
            
            int rowsInserted = st.executeUpdate();
            
            if (rowsInserted > 0) {
                response.put("success", true);
                response.put("message", "Photo description saved to database");
                response.put("description", description);
                response.put("s3Key", s3Key);
            } else {
                response.put("success", false);
                response.put("message", "Failed to insert into database");
            }
            
            st.close();
            mySQLClient.close();
            
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

    private static Properties setMySqlConnectionProperties() throws Exception {
        Properties mysqlConnectionProperties = new Properties();
        mysqlConnectionProperties.setProperty("useSSL", "true");
        mysqlConnectionProperties.setProperty("user", DB_USER);
        mysqlConnectionProperties.setProperty("password", generateAuthToken());
        return mysqlConnectionProperties;
    }

    private static String generateAuthToken() throws Exception {
        RdsUtilities rdsUtilities = RdsUtilities.builder().build();
        
        String authToken = rdsUtilities.generateAuthenticationToken(
                GenerateAuthenticationTokenRequest.builder()
                        .hostname(RDS_INSTANCE_HOSTNAME)
                        .port(RDS_INSTANCE_PORT)
                        .username(DB_USER)
                        .region(Region.AP_SOUTHEAST_1)
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build());
        return authToken;
    }
}
