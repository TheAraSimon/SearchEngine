<h1 align="center">Search Engine</h1>  
<p align="center">Search system for indexing and searching websites</p>

## Description
**INDEX SITES AND FIND RELEVANT PAGES!**

<p align="center">
<img src="https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExNjl6ZWxleTBmOXR1OHl0cHFlZzV3b2VqZXZmYmE3aHV4Mmc4cTA3OSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/Lj7ZCos3TGQ73XnonQ/giphy.gif" width="80%"></p>

Search Engine is a high-performance web indexing and search system designed for efficient crawling, indexing, and retrieving relevant information from websites. It supports multi-threaded processing, lemma-based search optimization, and real-time data updates.

## How to Use
- **Specify the sites** which are required to be indexed in Setting (application.yaml file).
- **Start indexing** in Management tab. And stop if required.
- **Add extra page** option is also available.
- **Check the statistics** in Dashboard tab.
- **Search relevant pages** in Search tab using keywords. If no site is chosen, the search will be done throughout all indexed sites.

## Stack of Technologies and Libraries
Spring Framework // PostgreSQL // Jackson // Lombok // Jsoup // Lucene.morphology // Liquibase

## How to Run
Run PostgreSQL container in Docker. Use the command below in Terminal:
docker run -p 5432:5432 --name search_engine -e POSTGRES_PASSWORD=testtest -e POSTGRES_USER=AraSimon -e POSTGRES_DB=search_engine -d postgres:14
Recommended to have JDK 22 installed on your PC. Run the program in the IDE, navigate to the browser path localhost:8080
Check the settings (application.yaml) if required.