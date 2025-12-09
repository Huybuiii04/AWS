# Lambda Image Resizer

AWS Lambda function that automatically resizes images when they are uploaded to S3.

## Features

- **Automatic Trigger**: Triggered by S3 PUT events
- **Supported Formats**: JPG and PNG images
- **Resize**: Scales images down to 100x100 pixels (thumbnail size)
- **Preserves Aspect Ratio**: Maintains original image proportions
- **Separate Bucket**: Stores resized images in `resized-{source-bucket}` bucket

## How It Works

1. Image is uploaded to source bucket (`public-huy04`)
2. Lambda is triggered by S3 event
3. Lambda downloads the image
4. Image is resized to max dimension of 100 pixels
5. Resized image is saved to destination bucket (`resized-public-huy04`)
6. File is prefixed with `resized-` (e.g., `resized-photo.jpg`)

## Project Structure

```
lambda-resizer/
├── src/
│   └── main/
│       └── java/
│           └── LambdaResizer.java
├── pom.xml
└── README.md
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- AWS CLI configured with appropriate credentials
- Two S3 buckets:
  - Source: `public-huy04`
  - Destination: `resized-public-huy04` (create this if it doesn't exist)

## Building

```bash
mvn clean package
```

This creates: `target/lambda-resizer-1.0.0.jar`

## Deployment Steps

### 1. Create Destination Bucket

```bash
aws s3 mb s3://resized-public-huy04 --region ap-southeast-1
```

### 2. Create Lambda Function

1. Upload JAR to AWS Lambda
2. Set handler: `LambdaResizer::handleRequest`
3. Set runtime: Java 11
4. Set memory: 512 MB (recommended for image processing)
5. Set timeout: 30 seconds

### 3. Configure IAM Permissions

Lambda execution role needs:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::public-huy04/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::resized-public-huy04/*"
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

### 4. Add S3 Trigger

1. Go to Lambda function configuration
2. Add trigger → S3
3. Select bucket: `public-huy04`
4. Event type: `PUT`
5. Suffix (optional): `.jpg` or `.png`
6. Enable trigger

## Testing

Upload an image to test:

```bash
aws s3 cp test-image.jpg s3://public-huy04/test-image.jpg
```

Check the resized image:

```bash
aws s3 ls s3://resized-public-huy04/
```

Download resized image:

```bash
aws s3 cp s3://resized-public-huy04/resized-test-image.jpg ./resized-test-image.jpg
```

## Configuration

To change resize dimensions, modify `MAX_DIMENSION` in `LambdaResizer.java`:

```java
private static final float MAX_DIMENSION = 100; // Change this value
```

To change source bucket, modify:

```java
String srcBucket = "public-huy04"; // Change this
```

## Supported Image Types

- **JPG/JPEG**: `image/jpeg`
- **PNG**: `image/png`

Other file types are skipped automatically.

## Logging

View logs in CloudWatch Logs:
- Log group: `/aws/lambda/{your-function-name}`
- Logs include: source file, destination file, resize operations, and errors

## Troubleshooting

### Lambda times out
- Increase timeout setting (default: 30 seconds)
- Increase memory (more memory = more CPU power)

### Image quality issues
- Adjust `RenderingHints` in `resizeImage()` method
- Change interpolation method for better/faster results

### Access denied errors
- Check IAM role permissions
- Ensure both buckets exist
- Verify bucket policies allow Lambda access

## Notes

- Function only processes new uploads (PUT events)
- Existing images won't be processed unless re-uploaded
- Destination bucket must exist before deployment
- Image processing uses Java AWT (works in Lambda environment)
