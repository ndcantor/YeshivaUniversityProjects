# Search Engine Project Overview
I completed this project as part of a semester-long project for a Data Structures\
course at Yeshiva University. The search engine has several functinalities:
## Adding, retreiveing, and deleting a document
Documents are stored in the engine as key-value pairs, where the key is a URI and the\
value is the document associated with that URI. This URI is supplied when first adding\
a document to the system, and can be used later on to retreive and/or delete the document.\
This functionality is implemented using a hashtable data structure.
## Getting a document as a PDF
Clients have the option to retreive documents as either strings of text, or bytes of a pdf\
whose contets are the same as the contents of the document which was originally stored\
in the system. This pdf functionality is implemented using [Apache PDFBox](https://pdfbox.apache.org/).
## Undoing previous actions
After either putting or deleting a document into/from the system, you can undo that action,\
thereby removing/putting back the document from/into the system. This functionality is\
implemented using a stack and a Java Lambda funcion with a reference to the relevant document.
## Memory Management
Users can set memory limits to limit the amount of data stored in main memory. Once the\
limit is reached, documents get removed from memory and written to disk. If a document\
on disk is then called upon by the user, it is brought back to memory.
### Main Memory Storage
Main memory storage is managed by a heap data structure. Documents are compared based on\
their time of last use, where the least recently used documents are high in the heap.\
Once a document is removed from the top of the heap, it is written to memory as a\
JSON string. This process is implemented using a heap data structure in conjuction\
with the GSON library.
### Disk Storage
All documents stored on disk are recorded in a B-Tree, where the key is the URI of the\
document. When a user requests a document that is no longer stored in memory, the\
B-Tree will read the document from disk and return it back to main memory.
## Searchability
Users can querry the system for all documents that contain a given sequence of letters\
or words. A collection of all such documents will then be returned. Thisfunctionality\
is implemented using a trie data structure.
## JUnit Tests
The project has the structure of a Maven project, and there are several classes of\
junit tests in the Maven test code directory.
# Thank you
Thank you for checking out my project!
