Ingestion takes place by consuming one or more boutiqaat_v2 table(s), in pages (paginated data consumption), each 
page is an iteration. The read data is assembled into CRS-corresponding models, a rest client (Feign) is used to 
communicate the models to CRS upsertion APIs.

All APIs are POST.
All CRS APIs stream their response as failures/success model upserts are captured
To observe streamed output, use curl command

All CDC APIs return no response body.

Every streamed output follows the structure:
[
    "success_1": {}, 
    "success_2": {}, 
    "failure_1": {},
    "failure_2": {},
    ...
    "success_n": {},
    "failure_m": {},
    "done": "true"
]
where n is the total number of successfully ingested models, and m is the total number of ingestion failures.
"done" identifies the response end.

All CRS APIs have 4 common request parameters:
[startPage]     - default = 0       - From which page in corresponding boutiqaat_v2 table reading/consuming data starts
[pageSize]      - default = 100     - The page size of consuming the corresponding boutiqaat_v2 table, greater value 
means the ingestion iterations count is less but memory consumption is higher.
[showSuccess]   - default = false   - API output stream to include/exclude successfully ingested models.
[showFailure]   - default = true    - API output stream to include/exclude failed-models.


Feed [sku] table of multi_stocks DB
curl --location --request POST "http://host:8099/crs?startPage=838&pageSize=200" --output -

Feed [configurable] table of multi_stocks DB
curl --location --request POST "http://host:8099/crs/config" --output -

Feed [bundle] table of multi_stocks DB
curl --location --request POST "http://host:8099/crs/bundle?startPage=50&pageSize=200" --output -

Feed [stock] table of multi_stocks DB, with real data (KW)
curl --location --request POST "http://host:8099/crs/inventory?pageSize=100" --output -
Feed [stock] table of multi_stocks DB, with dummy data (SA)
curl --location --request POST "http://host:8099/crs/inventory?pageSize=100&dummy=true" --output -

Feed [price] table of multi_stocks DB, with real data (KW)
curl --location --request POST "http://host:8099/crs/price" --output -
Feed [price] table of multi_stocks DB, with dummy data (SA)
curl --location --request POST "http://host:8099/crs/price?dummy=true" --output -

All CDC APIs have 2 common request parameters:
[startId]     - default = 0       - From which entity id in corresponding boutiqaat_v2 table reading/consuming data starts
[pageSize]    - default = 100     - The page size of consuming the corresponding boutiqaat_v2 table, greater value
means the ingestion iterations count is less but memory consumption is higher. 500 or less is recommended to avoid
exceeding http header limit

Feed [ES products index]
curl --location --request POST "http://host:8099/cdc/products?startId=0&pageSize=200" --output -

Feed [ES celebrities index]
curl --location --request POST "http://host:8099/cdc/celebrities?startId=0&pageSize=200" --output -
