# OCR Application Sample

Sample OCR application build on the top of Spring Boot, [Spring Cloud AWS](https://github.com/awspring/spring-cloud-aws) and [AWS Textract](https://aws.amazon.com/textract/).

**Note:** this sample uses Spring Cloud AWS 3.0 (which has not been yet released).

## How to Run

Create an S3 bucket and put your bucket name to `src/main/resources/application.properties` under `app.bucket` property.

Run application with:

```
./mvnw spring-boot:run
```
