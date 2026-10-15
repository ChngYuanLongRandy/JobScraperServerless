# App

This project contains an AWS Lambda maven application with [AWS Java SDK 2.x](https://github.com/aws/aws-sdk-java-v2) dependencies.
The purpose of this app is to be run in a lambda function with accompanying S3 buckets 

## Why
- Run job search scans regardless of AI tool

## Prerequisites
- Java 25
- Apache Maven
- s3 buckets
- SAM cli installed
- AWS cli installed
- AWS User Access Key

## How to Use
- if there are any adjustments
  - configure in template.yaml
  - sam validate
  - mvn clean package
  - sam build
  - sam deploy --guided
- If testing is needed, to test it in the lambda function itself

## Local Env Var
PROFILE=dev;LOG4J_CONFIGURATION_FILE=log4j2-dev.xml

## Proposed Flow
1. Read from website using FetchResponse
2. Parse into DTO using ResponseMapper
3. (optional) remove stale but not expired records
3. Read records (seen) from S3 
4. Dedupe fresh results against seen
5. Fetch JDs
6. Sync data
5. new records are scored and ranked
6. Top N (per param) sent back to app to be sent as email

### Dedupe process
Records need to be checked against source and UUID.

## Todos
- Write scorer -> left comparator in order to get the Top N
- Need to understand how to return the message in SNS when the results are being returned
- Write test cases
- Complete mcf scraper (optional)

## Observations 
### Career@Gov
- URL from the browser returns the entire catalogue and it is the actually the response from Algolia (which is what the service calls when a search is made) contains the relevant job posting IDs
- Based on the samples from the career@gov, it shows that the years of experience are tied to the position and not to the framework or language which is easier for parsing
- Also on the dedupe effort for career@gov, only the ID is required.

But based on personal experience it is not the case for mcf


Complete the GovScraper
- Filtering for experience and employment term not done for the merging

## Full-listings mode (for The Sausage)
Set via environment variables (see `template.yaml`); the original settings stay in place and are simply unused while it is on.
- `FULL_LISTINGS_MODE=true`: skip the keyword search and the experience filter; return every open posting.
- `PUBLISH_TOP_RESULTS=false`: skip scoring and the SNS email. Set `FULL_LISTINGS_MODE=false` and `PUBLISH_TOP_RESULTS=true` to restore the old behaviour.
- Each run writes its new (deduped, with job descriptions) listings to `S3_RUN_PREFIX` (default `data/runs/`) as one JSON file, BEFORE they are marked as seen.
- `JD_FETCH_BUDGET_SECONDS`: stop fetching descriptions after this long. Listings not fetched, or whose fetch failed, are not marked as seen and are retried next run. The first run over a large catalog may need several manual invocations to catch up.
- Fixed: the seen file could be written but not read back (`Pair` fields), so dedupe saw nothing as seen. A read failure now fails the run instead of silently resetting the seen list.
- Fixed: `ListingDTO` lacked `score`/`hits`/`Comparable`, so the committed code did not compile.
