package vgu.cloud26;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


public class LambdaEntryPoint implements
	RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final LambdaClient lambdaClient;
       
    public LambdaEntryPoint() {
	this.lambdaClient = LambdaClient.builder()
		.region(Region.of("ap-southeast-1"))
		.build();
    }
    
    public String callLambda(String functionName, String payload,  LambdaLogger logger) {
	String message;
	InvokeRequest invokeRequest = InvokeRequest.builder()
		.functionName(functionName)
		//.invocationType("Event") // Asynchronous invocation
		.payload(SdkBytes.fromUtf8String(payload))
		.invocationType("RequestResponse")
		.build();

	try {
	    InvokeResponse invokeResult = 
		    lambdaClient.invoke(invokeRequest);
      
	    ByteBuffer responsePayload = 
		    invokeResult.payload().asByteBuffer();
	    String responseString = StandardCharsets.UTF_8.decode(responsePayload).toString();

	    JSONObject responseObject = new JSONObject(responseString);
	    message = responseObject.getString("body");
	    logger.log("Response: " + message);
                          
	    return message;
	} catch (AwsServiceException | SdkClientException e) {
	    message = "Error " + functionName + 
		    ": " + e.getMessage();
	    logger.log(message);
	}
	return message;
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context cntxt) {
        
	LambdaLogger logger = cntxt.getLogger();
	logger.log("Invoking");
	
	// Get HTTP method
	String httpMethod = event.getHttpMethod();
	
	// Handle POST requests for upload
	if ("POST".equalsIgnoreCase(httpMethod)) {
	    return handleUpload(event, logger);
	}
	
	// Handle GET requests (default behavior)
	LambdaEntryPoint caller = new LambdaEntryPoint();
	JSONObject body = new JSONObject();
	body.put("key", "a.html");
	JSONObject json = new JSONObject();
	json.put("body", body.toString());
	String payload = json.toString();
	String message = caller.callLambda("LambdaGetObjects", 
		payload, logger);

       
	Map<String, String> headersMap;
	    headersMap = Map.of(
		    "content-type", "text/html");

	    return new APIGatewayProxyResponseEvent()
		    .withStatusCode(200)
		    .withHeaders(headersMap)
		    .withBody(message)
		    .withIsBase64Encoded(true);

    }
    
    private APIGatewayProxyResponseEvent handleUpload(APIGatewayProxyRequestEvent event, LambdaLogger logger) {
	try {
	    String bucketName = "public-huy04";
	    String requestBody = event.getBody();
	    
	    JSONObject bodyJSON = new JSONObject(requestBody);
	    String content = bodyJSON.getString("content");
	    String objName = bodyJSON.getString("key");
	    
	    logger.log("Uploading object: " + objName + " to bucket: " + bucketName);
	    
	    // Decode base64 content
	    byte[] objBytes = Base64.getDecoder().decode(content.getBytes());
	    
	    // Create S3 client
	    S3Client s3Client = S3Client.builder()
		    .region(Region.AP_SOUTHEAST_1)
		    .build();
	    
	    // Create put object request
	    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
		    .bucket(bucketName)
		    .key(objName)
		    .build();
	    
	    // Upload object
	    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(objBytes));
	    
	    logger.log("Object uploaded successfully");
	    
	    String message = "Object uploaded successfully";
	    String encodedString = Base64.getEncoder().encodeToString(message.getBytes());
	    
	    return new APIGatewayProxyResponseEvent()
		    .withStatusCode(200)
		    .withBody(encodedString)
		    .withIsBase64Encoded(true)
		    .withHeaders(Map.of("Content-Type", "text/plain"));
		    
	} catch (Exception e) {
	    logger.log("Error uploading object: " + e.getMessage());
	    return new APIGatewayProxyResponseEvent()
		    .withStatusCode(500)
		    .withBody("Error: " + e.getMessage())
		    .withHeaders(Map.of("Content-Type", "text/plain"));
	}
    }

}
