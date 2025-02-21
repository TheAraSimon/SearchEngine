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
Spring Framework // MySQL // Jackson // Lombok / Jsoup / Lucene.morphology

## How to Run
Recommended to have JDK 22 and MySQL installed on your PC. Search_engine schema must be created on DataBase. Run the program in the IDE, navigate to the browser path localhost:8080
Check the settings (application.yaml) if required.