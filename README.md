# Crypto Client POC Application
This application is tightly coupled to [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc)

This Client application does the following: 
- fetches choosen cryptocurrency information from Coingecko.com
- Parses the data to [Coin](https://github.com/src-dbgr/crypto-monitor-api-poc/blob/main/src/main/java/com/sam/coin/model/Coin.java) format
- Posts the Coin object to crypto-monitor-api-poc service which stores the data in a PostgreSQL database and serves as the datasource for Grafana Monitoring Dashboards

## So far it offers the following operations
- updateCryptos()
  - This updates all choosen and in the backend enabled cryptos with the current price and meta data information
- fetchAllHistoricalData(String[] cryptos, int historicalDays)
  - This updates all crypto provided in the array
  - histrocal days start updating for each day starting from today and go back the number of days passed
- Since this is part of a POC the goal is to have an end to end communication channel which is hereby established
- Feel free to play around with the sources

## Running the application
- Make sure [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) is cloned to you local machine and the project is properly build
  - meaning `mvn build` & `mvn install` have been performed successfully otherwise you might run into dependency errors on compilation of this client
- Make sure [crypto-monitor-api-poc](https://github.com/src-dbgr/crypto-monitor-api-poc) is up and running, either in a Docker container or as a pure Java application
- It's recommended to import this maven project into your IDE of choice
- You may trim the String[] cryptoIds content to your desires
- Set the method you want to exececute at the bottom of the CryptoClient within the Main method
- Run as a Java application in order to perform the desired updates