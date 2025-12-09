# Lambda Get List Of Objects

AWS Lambda function để lấy danh sách objects từ S3 bucket.

## Yêu cầu

- Java 21
- Maven 3.x
- AWS CLI (optional - để deploy)

## Cấu trúc Project

```
LambdaGetListOfObjects/
├── src/
│   └── main/
│       └── java/
│           └── vgu/
│               └── cloud26/
│                   └── LambdaGetListOfObjects.java
├── pom.xml
└── README.md
```

## Các Lệnh Chính

### 1. Build Project

```bash
mvn clean package
```

File JAR sẽ được tạo tại: `target/LambdaGetListOfObjects-1.0-SNAPSHOT.jar`

### 2. Test Local (nếu có test)

```bash
mvn test
```

### 3. Clean Build Files

```bash
mvn clean
```

## Cấu Hình Lambda Function

### Handler Configuration
```
vgu.cloud26.LambdaGetListOfObjects::handleRequest
```

### Runtime
- Java 21

### Environment Variables
Không cần thiết (sử dụng IAM Role)

### IAM Role Permissions
Lambda function cần quyền:
- `s3:ListBucket` trên bucket `public-huy04`
- Hoặc attach policy: `AmazonS3ReadOnlyAccess`

## Deploy lên AWS Lambda

### Cách 1: Upload qua AWS Console

1. Mở AWS Lambda Console
2. Tạo function mới:
   - Function name: `GetListOfObjects` (hoặc tên bất kỳ)
   - Runtime: Java 21
   - Architecture: x86_64
3. Upload file `target/LambdaGetListOfObjects-1.0-SNAPSHOT.jar`
4. Configure Handler: `vgu.cloud26.LambdaGetListOfObjects::handleRequest`
5. Gắn IAM Role có quyền truy cập S3

### Cách 2: Deploy qua AWS CLI

```bash
# Tạo Lambda function
aws lambda create-function \
  --function-name GetListOfObjects \
  --runtime java21 \
  --role arn:aws:iam::YOUR_ACCOUNT_ID:role/YOUR_LAMBDA_ROLE \
  --handler vgu.cloud26.LambdaGetListOfObjects::handleRequest \
  --zip-file fileb://target/LambdaGetListOfObjects-1.0-SNAPSHOT.jar \
  --timeout 30 \
  --memory-size 512

# Update Lambda function (nếu đã tồn tại)
aws lambda update-function-code \
  --function-name GetListOfObjects \
  --zip-file fileb://target/LambdaGetListOfObjects-1.0-SNAPSHOT.jar
```

## Test Lambda Function

### Test Event (JSON)

```json
{
  "test": "event"
}
```

### Expected Response

```json
{
  "statusCode": 200,
  "headers": {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type"
  },
  "body": "[\"file1.jpg\",\"file2.png\",\"folder/file3.pdf\"]"
}
```

## Kết Nối với API Gateway

1. Tạo REST API trong API Gateway
2. Tạo Resource và Method (GET)
3. Integrate với Lambda function
4. Enable CORS
5. Deploy API

**API Endpoint Example:**
```
https://abc123.execute-api.ap-southeast-1.amazonaws.com/prod/list-objects
```

## Thay Đổi S3 Bucket

Để sử dụng bucket khác, sửa file `LambdaGetListOfObjects.java`:

```java
private static final String BUCKET_NAME = "your-bucket-name";
private static final Region REGION = Region.AP_SOUTHEAST_1;
```

Sau đó rebuild:
```bash
mvn clean package
```

## Troubleshooting

### Lỗi: Unable to load credentials
- Kiểm tra IAM Role của Lambda có quyền S3
- Đảm bảo Lambda function đã được gắn đúng Role

### Lỗi: Access Denied
- Kiểm tra S3 bucket policy
- Kiểm tra IAM Role permissions

### Lỗi: Timeout
- Tăng timeout configuration trong Lambda (mặc định 3s, nên set 30s)
- Kiểm tra kết nối mạng đến S3

## Dependencies

Xem chi tiết trong `pom.xml`:
- AWS Lambda Java Core (1.4.0)
- AWS Lambda Java Events (3.16.1)
- AWS SDK for Java v2 - S3 (2.35.4)
- JSON (20250517)

## License

MIT License

## Author

Cloud26 Project - VGU
