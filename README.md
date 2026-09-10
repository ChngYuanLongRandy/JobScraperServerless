# App

This project contains an AWS Lambda maven application with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2) dependencies.
The purpose of this app is to be run in a lambda function with accompanying S3 buckets 

## Why
- Run job search scans regardless of AI tool

## Prerequisites
- Java 25
- Apache Maven
- s3 buckets

## How to Use
- Setup scheduler to run this function

## Local Env Var
PROFILE=dev;LOG4J_CONFIGURATION_FILE=log4j2-dev.xml

## Proposed Flow
1. Read from website using FetchResponse
2. Parse into DTO using ResponseMapper
3. Read records (seen) from S3
4. Dedupe fresh results against seen
5. new records are scored and ranked
6. Top N (per param) sent back to app to be sent as email
7. Write fresh results into S3 

## Todos
