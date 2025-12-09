# AWS Lambda S3 Upload Function

This AWS Lambda function receives base64-encoded content via API Gateway and uploads it to an S3 bucket.

## Features

- Accepts POST requests through API Gateway
- Decodes base64-encoded content
- Uploads files to S3 bucket `cloud26-mgc` in the `ap-southeast-1` region
- Returns success message with base64 encoding

## Project Structure

```
lambda-upload-object/
├── src/
│   └── main/
│       └── java/
│           └── LambdaUploadObject.java
├── pom.xml
└── README.md
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- AWS CLI configured with appropriate credentials
- S3 bucket named `cloud26-mgc` (or modify the bucket name in the code)

## Building

To build the project and create a deployment package:

```bash
mvn clean package
```

This will create a JAR file in the `target` directory: `lambda-upload-object-1.0.0.jar`

## Deployment

1. Upload the JAR file to AWS Lambda
2. Set the handler to: `LambdaUploadObject::handleRequest`
3. Configure the Lambda execution role with permissions:
   - `s3:PutObject` on the target bucket
   - Basic Lambda execution role permissions

## API Request Format

The function expects a JSON body with the following structure:

```json
{
  "content": "base64-encoded-file-content",
  "key": "filename-or-path-in-s3"
}
```

## Example Request

```bash
curl -X POST https://your-api-gateway-url/upload \
  -H "Content-Type: application/json" \
  -d '{
    "content": "SGVsbG8gV29ybGQh",
    "key": "test-file.txt"
  }'
```

## IAM Permissions Required

The Lambda execution role needs the following policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::cloud26-mgc/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

## Configuration

To change the S3 bucket or region, modify the following lines in `LambdaUploadObject.java`:

```java
String bucketName = "cloud26-mgc";  // Change bucket name here
// ...
.region(Region.AP_SOUTHEAST_1)  // Change region here
```
