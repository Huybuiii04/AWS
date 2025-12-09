# Lambda Delete Object Function

This Lambda function automatically deletes resized images from the `resized-public-huy04` bucket when the original image is deleted from the `public-huy04` bucket.

## Deployment Steps

### 1. Build the Lambda Function

```bash
cd d:\AWS\LambdaDeleteObject
mvn clean package
```

This will create a JAR file in `target/lambda-delete-object-1.0-SNAPSHOT.jar`

### 2. Create the Lambda Function in AWS Console

1. Go to AWS Lambda Console
2. Click "Create function"
3. Choose "Author from scratch"
4. Function name: `LambdaDeleteObject`
5. Runtime: `Java 11 (Corretto)`
6. Architecture: `x86_64`
7. Click "Create function"

### 3. Upload the JAR File

1. In the Lambda function page, go to "Code" tab
2. Click "Upload from" → ".zip or .jar file"
3. Upload `target/lambda-delete-object-1.0-SNAPSHOT.jar`
4. Click "Save"

### 4. Configure the Handler

1. In "Runtime settings", click "Edit"
2. Set Handler to: `vgu.cloud26.LambdaDeleteObject::handleRequest`
3. Click "Save"

### 5. Configure IAM Role Permissions

The Lambda execution role needs permission to:
- Read from `public-huy04` bucket (S3 event)
- Delete from `resized-public-huy04` bucket

Add this policy to the Lambda execution role:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:DeleteObject"
            ],
            "Resource": [
                "arn:aws:s3:::public-huy04/*",
                "arn:aws:s3:::resized-public-huy04/*"
            ]
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

### 6. Configure S3 Trigger

1. In Lambda function page, click "Add trigger"
2. Select "S3"
3. Bucket: `public-huy04`
4. Event type: Select "All object delete events" or specifically:
   - `s3:ObjectRemoved:Delete`
   - `s3:ObjectRemoved:DeleteMarkerCreated`
5. Click "Add"

## How It Works

1. When an image is deleted from `public-huy04` bucket
2. S3 triggers the `LambdaDeleteObject` function
3. The function extracts the object key from the S3 event
4. It deletes the corresponding file with the same key from `resized-public-huy04` bucket
5. Success/error messages are logged to CloudWatch

## Testing

You can test by:
1. Uploading an image to `public-huy04` (it will be resized to `resized-public-huy04`)
2. Deleting the original image from `public-huy04`
3. Verify the resized image is also deleted from `resized-public-huy04`
