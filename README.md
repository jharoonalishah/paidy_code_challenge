## Paidy code challenge

### Supported currencies:
 - USD
 - JPY
 - AUD
 - CAD
 - CHF
 - EUR
 - GBP
 - NZD
 - SGD

### How to Request example:
- `curl --location --request GET 'localhost:8090/rates?from=USD&to=JPY'`

### Pre-requisites
Data source is following docker image:
`docker pull paidyinc/one-frame`

run it using `docker run -p <port>:<port> paidyinc/one-frame`
default `docker run -p 8080:8080 paidyinc/one-frame`

### Language
- Requirements Scala 2.13, sbt 1.3.10
