package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.*;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings({"unused", "DuplicatedCode"})
public class DocumentStoreImpl implements DocumentStore {final private StackImpl<Undoable> commandStack;
    final private TrieImpl<URI> trie; //change to URI
    final private EnhancedMinHeapImpl<ComparableUriByDocTime> heap; //change to URI
    final private EnhancedBTreeImpl<URI,Document> bTree;
    private int documentCount;
    private int byteCount;
    private int maxDocumentCount;
    private int maxDocumentBytes;

    public DocumentStoreImpl() {
        this(null);
    }

    public DocumentStoreImpl(File baseDir) {
        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.heap = new EnhancedMinHeapImpl<>();
        this.bTree = new EnhancedBTreeImpl<>();
        PersistenceManager<URI,Document> pm = new DocumentPersistenceManager(baseDir);
        this.bTree.setPersistenceManager(pm);
        //add the sentinel before anything else
        try {
            this.bTree.put(new URI(null,null,null), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        this.documentCount = 0;
        this.byteCount = 0;
        this.maxDocumentCount = Integer.MAX_VALUE;
        this.maxDocumentBytes = Integer.MAX_VALUE;
    }

    protected void putIntoBtree(URI uri, Document document) {
        if (document == null) {
            this.bTree.put(uri, null);
        }
        if (this.bTree.isStoredOnDisk(uri)) {
            Document oldDoc = this.bTree.put(uri, document);
            //noinspection ConstantConditions
            this.addToHeap(document);
        }
        this.bTree.put(uri, document);
    }

    protected Document getFromBtree(URI uri) {
        if (this.bTree.isStoredOnDisk(uri)) {
            Document doc = this.bTree.get(uri);
            this.addToHeap(doc);
            return doc;
        }
        return this.bTree.get(uri);
    }

    protected Function<URI, Boolean> getUndoFunction(Document newDoc, Document oldDoc) {
        return (uri) -> {
            try {
//              if undoing a put, first you must remove all words from the trie and remove from heap/bTree
                if (newDoc != null) {
                    this.deleteAllWordsFromTrie(newDoc);
                    this.removeFromHeap(newDoc);
                    this.bTree.put(uri, null);
                }
//              from here on it doesn't matter if we are dealing with a put or a delete
//              if no previous value was stored under this URI delete from hashtable
//              no need to add new words to trie, as there was no document that used to be stored under this URI
                if (oldDoc == null) {
                    this.bTree.put(uri, null);
                }
                else {
                    this.putIntoBtree(uri, oldDoc);
                    this.addAllWordsToTrie(oldDoc);
                    oldDoc.setLastUseTime(System.nanoTime());
                    this.addToHeap(oldDoc);
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * @param toCompare - the string to compare
     * @return a comparator that compares the document by string that was passed as the parameter
     */
    protected Comparator<URI> getTrieSearchComparatorByWord(String toCompare) {
        return (o1, o2) -> Integer.compare(this.getFromBtree(o2).wordCount(toCompare), this.getFromBtree(o1).wordCount(toCompare));
    }

    @SuppressWarnings("DuplicatedCode")
    protected Comparator<URI> getTrieSearchComparatorByPrefix(String prefix) {
        return (o1, o2) -> {
            int prefixCountDoc1 = 0;
            DocumentImpl doc1 = (DocumentImpl) this.getFromBtree(o1);
            for (String word : doc1.getWordSet()) {
                if (word.length() < prefix.length()) {
                    continue;
                }
                if (word.substring(0, prefix.length()).equals(prefix)) {
                    prefixCountDoc1 += this.getFromBtree(o1).wordCount(word);
                }
            }
            int prefixCountDoc2 = 0;
            DocumentImpl doc2 = (DocumentImpl) this.getFromBtree(o2);
            for (String word : doc2.getWordSet()) {
                if (word.length() < prefix.length())
                    continue;
                if (word.substring(0, prefix.length()).equals(prefix)) {
                    prefixCountDoc2 += this.getFromBtree(o2).wordCount(word);
                }
            }
            return Integer.compare(prefixCountDoc2, prefixCountDoc1);
        };
    }

    /**
     * @param string to alphanumeritize
     * @return a string with only alpha-numeric characters and no whitespace
     */
    protected String getUsableString(String string) {
        string = string.replaceAll("[^0-9a-zA-Z ]", "");
        string = string.trim();
        return string.toUpperCase();
    }

    protected void addAllWordsToTrie(Document doc) {
        DocumentImpl document = (DocumentImpl) doc;
        for (String word : document.getWordSet()) {
            this.trie.put(word, document.getKey());
        }
    }

    protected void deleteAllWordsFromTrie(Document doc) {
        DocumentImpl document = (DocumentImpl) doc;
        Set<String> docWordSet = document.getWordSet();
        for (String word : docWordSet) {
            this.trie.delete(word, document.getKey());
        }
    }

    //adds document to heap and keeps track of docStore memory usage
    protected void addToHeap(Document document) {
        this.heap.insert(new ComparableUriByDocTime(document.getKey(), (this::getFromBtree)));
        this.documentCount++;
        this.byteCount += document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length;
        this.manageMemory();
    }

    //removes document from heap and keeps track of docStore memory usage
    protected void removeFromHeap(Document oldDocument) {
        oldDocument.setLastUseTime(Long.MIN_VALUE);
        if (!this.heap.contains(new ComparableUriByDocTime(oldDocument.getKey(), null))) {
            return;
        }
        this.heap.reHeapify(new ComparableUriByDocTime(oldDocument.getKey(), (this::getFromBtree)));
        this.heap.removeMin();
        this.documentCount--;
        this.byteCount -= (oldDocument.getDocumentAsPdf().length + oldDocument.getDocumentAsTxt().getBytes().length);
    }

    protected void manageMemory() {
        if (this.documentCount <= this.maxDocumentCount && this.byteCount <= this.maxDocumentBytes) {
            return;
        }
        while (this.documentCount > this.maxDocumentCount || this.byteCount > this.maxDocumentBytes) {
            ComparableUriByDocTime cubdt = this.heap.removeMin();
            Document removed = this.getFromBtree(cubdt.getUri());
            try {
                this.bTree.moveToDisk(removed.getKey());
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.documentCount--;
            this.byteCount -= (removed.getDocumentAsPdf().length + removed.getDocumentAsTxt().getBytes().length);
        }
    }

    /**
     * @return the Document object stored IN MEMORY at that URI, or null if there is no such
     * Document. this method DOES NOT update last use time and doesn't bring in anything
     * to memory from disk
     */
    protected Document getDocument(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (this.bTree.isStoredOnDisk(uri)) {
            return null;
        }
        return this.bTree.get(uri);
    }

    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc,
     * return the hashCode of the String version of the previous doc. If InputStream is null, this
     * is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     */
    @Override
    public int putDocument(InputStream input, URI uri, DocumentFormat format) {
        if (uri == null || format == null) {
            throw new IllegalArgumentException("null uri or null DocumentFormat");
        }
        // if input is null, delete document
        if (input == null) {
            DocumentImpl deletion = (DocumentImpl) this.getDocument(uri);
            return deleteDocument(uri) ? deletion.getDocumentTextHashCode() : 0;
        }
        DocumentImpl newDoc;
        switch (format) {
            case PDF:
                newDoc = (DocumentImpl) this.createNewPdfDoc(input, uri);
                break;
            case TXT:
                newDoc = (DocumentImpl) this.createNewTxtDoc(input, uri);
                break;
            default:
                throw new IllegalArgumentException("document format can't be null");
        }

        if (!this.bTree.isStoredOnDisk(uri) && this.getDocument(uri) == null) {
            this.addNewDocumentToSystem(newDoc, null);
            return 0;
        }
        Document oldDoc = this.bTree.get(uri);
        this.deleteAllWordsFromTrie(oldDoc);
        this.removeFromHeap(oldDoc);
        this.bTree.put(uri, null);
        this.addNewDocumentToSystem(newDoc, oldDoc);
        return oldDoc.getDocumentTextHashCode();
    }

    /**
     * @param input the InputStream to eventually be converted to text and stored in the Document
     * @param uri   the unique identifier of the Document being created
     * @return new instance of the pdf version of DocumentImpl
     */
    protected Document createNewPdfDoc(InputStream input, URI uri) {
        byte[] bytes = this.isToByteArray(input);
        PDDocument pdf;
        String text = null;
        try {
            pdf = PDDocument.load(bytes);
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(pdf).trim();
            pdf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //noinspection ConstantConditions
        Document document = new DocumentImpl(uri, text, text.hashCode(), bytes);
        document.setLastUseTime(System.nanoTime());
        return document;
    }

    /**
     * @param input the Inputstream to be converted to text and stored in the Document
     * @param uri   the unique identifier of the Document being created
     * @return a new instance of the txt version of DocumentImpl
     */
    protected Document createNewTxtDoc(InputStream input, URI uri) {
        byte[] bytes = this.isToByteArray(input);
        String string = new String(bytes);
        Document document = new DocumentImpl(uri, string, (string.hashCode()));
        document.setLastUseTime(System.nanoTime());
        return document;
    }


    /**
     * @param input the Inputstream to by converted to byte[]
     * @return the converted byte[]
     */
    protected byte[] isToByteArray(InputStream input) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int byteValue = 0;
        try {
            byteValue = input.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (byteValue != -1) {
            byteArrayOutputStream.write(byteValue);
            try {
                byteValue = input.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * @param newDoc to add
     */
    protected void addNewDocumentToSystem(Document newDoc, Document oldDoc) {
        this.commandStack.push(new GenericCommand<>(newDoc.getKey(), this.getUndoFunction(newDoc, oldDoc)));
        this.putIntoBtree(newDoc.getKey(), newDoc);
        this.addAllWordsToTrie(newDoc);
        this.addToHeap(newDoc);
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document as a PDF, or null if no document exists with that URI
     */
    @Override
    public byte[] getDocumentAsPdf(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        Document document = this.getFromBtree(uri);
        if (document == null) {
            return null;
        }
        document.setLastUseTime(System.nanoTime());
        this.heap.reHeapify(new ComparableUriByDocTime(document.getKey(), (this::getFromBtree)));
        return document.getDocumentAsPdf();
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document as TXT, i.e. a String, or null if no document exists with that URI
     */
    @Override
    public String getDocumentAsTxt(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        Document document = this.getFromBtree(uri);
        if (document == null) {
            return null;
        }
        document.setLastUseTime(System.nanoTime());
        this.heap.reHeapify(new ComparableUriByDocTime(document.getKey(), (this::getFromBtree)));
        return document.getDocumentAsTxt();
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean deleteDocument(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        Document deletion = this.bTree.get(uri);
        if (deletion != null) {
            this.deleteAllWordsFromTrie(deletion);
            this.removeFromHeap(deletion);
        }
        this.commandStack.push(new GenericCommand<>(uri, this.getUndoFunction(null, deletion)));
        return this.bTree.put(uri, null) != null;
    }

    /**
     * undo the last put or delete command
     *
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    @Override
    public void undo() throws IllegalStateException {
        if (this.commandStack.size() == 0) {
            throw new IllegalStateException("you cannot undo anything on an empty stack");
        }
        else if (this.commandStack.peek() instanceof GenericCommand) {
            @SuppressWarnings("unchecked") GenericCommand<URI> command = (GenericCommand<URI>) this.commandStack.peek();
            this.undo(command.getTarget());
        }
        else {
            @SuppressWarnings("unchecked") CommandSet<URI> commandSet = (CommandSet<URI>) this.commandStack.peek();
            int setSize = commandSet.size();
            //get set of documents now to use later for setting document last use time
            Set<GenericCommand<URI>> set = new HashSet<>();
            Iterator<GenericCommand<URI>> iterator = commandSet.iterator();
            //noinspection WhileLoopReplaceableByForEach
            while (iterator.hasNext()) {
                //noinspection UseBulkOperation
                set.add(iterator.next());
            }
            commandSet.undoAll();
            this.commandStack.pop();
            //take care of time stored for each document that was put back into system
            long currentTime = System.nanoTime();
            for (GenericCommand<URI> command : set) {
                this.getFromBtree(command.getTarget()).setLastUseTime(currentTime);
            }
        }
    }

    /**
     * undo the last put or delete that was done with the given URI as its key
     *
     * @param uri to undo action for
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    @Override
    public void undo(URI uri) throws IllegalStateException {
        StackImpl<Undoable> tempStack = new StackImpl<>();
        this.pushCommandToTop(uri, tempStack);

        if (this.commandStack.peek() == null) {
            while (tempStack.peek() != null) {
                this.commandStack.push(tempStack.pop());
            }
            throw new IllegalStateException("no command was found under the given URI");
        }

        Undoable target = this.commandStack.peek();

        if (target instanceof GenericCommand) {
            target.undo();
            this.commandStack.pop();
        }
        else {
            @SuppressWarnings("unchecked") CommandSet<URI> commandSet = (CommandSet<URI>) target;

            commandSet.undo(uri);
            if (commandSet.size() == 0) {
                this.commandStack.pop();
            }
        }

        while (tempStack.peek() != null) {
            this.commandStack.push(tempStack.pop());
        }
    }

    /**
     * @param uri to find in command stack
     * @param tempStack in which to store Undoables from command stack
     *
     * removes Undoables from command stack and puts them into tempStack until the Undoable with the given target
     * is located at the top of the command stack
     */
    protected void pushCommandToTop(URI uri, StackImpl<Undoable> tempStack) {
        Undoable current = this.commandStack.peek();

        while (current != null) {
            if (current instanceof GenericCommand) {
                @SuppressWarnings("unchecked") GenericCommand<URI> command = (GenericCommand<URI>) current;
                if (!(command.getTarget().equals(uri))){
                    tempStack.push(this.commandStack.pop());
                    current = this.commandStack.peek();
                }
                else {
                    break;
                }
            }
            else {
                @SuppressWarnings("unchecked") CommandSet<URI> commands = (CommandSet<URI>) current;
                if (!commands.containsTarget(uri)) {
                    tempStack.push(this.commandStack.pop());
                    current = this.commandStack.peek();
                }
                else {
                    break;
                }
            }
        }
    }

    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE INSENSITIVE.
     *
     * @param keyword to search for
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<String> search(String keyword) {
        if (keyword == null) {
            return new ArrayList<>();
        }
//        keyword = this.getUsableString(keyword);

        List<Document> matches = this.getAllDocsByKeywordForSearch(keyword);

        ArrayList<String> matchTxts =  new ArrayList<>(matches.size());
        long currentTime = System.nanoTime();
        for (Document doc : matches) {
            if (this.heap.contains(new ComparableUriByDocTime(doc.getKey(), null))) {
                doc.setLastUseTime(currentTime);
                this.heap.reHeapify(new ComparableUriByDocTime(doc.getKey(), (this::getFromBtree)));
            }
            matchTxts.add(doc.getDocumentAsTxt());
        }

        return matchTxts;
    }

    /**
     * same logic as search, but returns the docs as PDFs instead of as Strings
     *
     * @param keyword to search for
     */
    @Override
    public List<byte[]> searchPDFs(String keyword) {
        if (keyword == null) {
            return new ArrayList<>();
        }
//        keyword = this.getUsableString(keyword);

        List<Document> matches = this.getAllDocsByKeywordForSearch(keyword);

        ArrayList<byte[]> matchPDFs = new ArrayList<>(matches.size());
        long currentTime = System.nanoTime();
        for (Document doc : matches) {
            if (this.heap.contains(new ComparableUriByDocTime(doc.getKey(), null))) {
                doc.setLastUseTime(currentTime);
                this.heap.reHeapify(new ComparableUriByDocTime(doc.getKey(), (this::getFromBtree)));
            }
            matchPDFs.add(doc.getDocumentAsPdf());
        }

        return matchPDFs;
    }


    /**
     * @param keyword to search for
     * @return a sorted list of all docs that contain the keyword
     */
    protected List<Document> getAllDocsByKeywordForSearch(String keyword) {
        keyword = this.getUsableString(keyword);

        Comparator<URI> compareByWordCount = this.getTrieSearchComparatorByWord(keyword);
        List<URI> uriList = this.trie.getAllSorted(keyword, compareByWordCount);
        List<Document> docList = new ArrayList<>(uriList.size());
        for (URI uri : uriList) {
            docList.add(this.getFromBtree(uri));
        }
        return docList;
    }

    /**
     * Retrieve all documents whose text starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE INSENSITIVE.
     *
     * @param keywordPrefix to search for
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<String> searchByPrefix(String keywordPrefix) {
        if (keywordPrefix == null) {
            return new ArrayList<>();
        }
//        keywordPrefix = this.getUsableString(keywordPrefix);

        List<Document> matches = this.getAllDocsByPrefixForSearch(keywordPrefix);

        @SuppressWarnings("DuplicatedCode") ArrayList<String> matchTxts = new ArrayList<>(matches.size());
        long currentTime = System.nanoTime();
        for (Document doc : matches) {
            if (this.heap.contains(new ComparableUriByDocTime(doc.getKey(), null))) {
                doc.setLastUseTime(currentTime);
                this.heap.reHeapify(new ComparableUriByDocTime(doc.getKey(), (this::getFromBtree)));
            }
            matchTxts.add(doc.getDocumentAsTxt());
        }

        return matchTxts;
    }

    /**
     * same logic as searchByPrefix, but returns the docs as PDFs instead of as Strings
     *
     * @param keywordPrefix to search for
     */
    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) {
        if (keywordPrefix == null) {
            return new ArrayList<>();
        }
//        keywordPrefix = this.getUsableString(keywordPrefix);

        List<Document> matches = this.getAllDocsByPrefixForSearch(keywordPrefix);

        @SuppressWarnings("DuplicatedCode") ArrayList<byte[]> matchPDFs = new ArrayList<>(matches.size());
        long currentTime = System.nanoTime();
        for (Document doc : matches) {
            if (this.heap.contains(new ComparableUriByDocTime(doc.getKey(), null))) {
                doc.setLastUseTime(currentTime);
                this.heap.reHeapify(new ComparableUriByDocTime(doc.getKey(), (this::getFromBtree)));
            }
            matchPDFs.add(doc.getDocumentAsPdf());
        }

        return matchPDFs;
    }

    /**
     * @param prefix to search for
     * @return a sorted list of all docs that contain the prefix
     */
    protected List<Document> getAllDocsByPrefixForSearch(String prefix) {
        prefix = this.getUsableString(prefix);

        Comparator<URI> documentComparator = this.getTrieSearchComparatorByPrefix(prefix);
        List<URI> uriList = this.trie.getAllWithPrefixSorted(prefix, documentComparator);
        List<Document> docList = new ArrayList<>();
        for (URI uri : uriList) {
            docList.add(this.getFromBtree(uri));
        }
        return docList;
    }

    /**
     * Completely remove any trace of any document which contains the given keyword
     *
     * @param keyword search for
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAll(String keyword) {
        if (keyword == null) {
            return new HashSet<>();
        }
//        keyword = this.getUsableString(keyword);
        Set<Document> docSet = this.getAllDocsByKeywordForDelete(keyword);
        return this.deleteDocSetFromSystem(docSet);
    }

    /**
     * this comparator works for both deleting prefixes and deleting specific words
     * the reason is that you don't really need to compare the documents, as the return
     * values in the public methods are sets, not lists. However, the trie still needs
     * a comparator to be passed to it, so this is a comparator that makes sure not
     * to interact with the minHeap - it just compares their hashcodes (this was chosen
     * arbitrarily)
     */
    protected Comparator<URI> getGenericTrieDeleteComparator() {
        return ((o1, o2) -> Integer.compare(o2.hashCode(), o1.hashCode()));
    }

    protected Set<Document> getAllDocsByKeywordForDelete(String keyword) {
        keyword = this.getUsableString(keyword);

        Comparator<URI> comparator = this.getGenericTrieDeleteComparator();
        List<URI> uriList = this.trie.getAllSorted(keyword, comparator);
        Set<Document> docSet = new HashSet<>(uriList.size());
        for (URI uri : uriList) {
            docSet.add(this.bTree.get(uri));
        }
        return docSet;
    }

    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE INSENSITIVE.
     *
     * @param keywordPrefix to search for
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        if (keywordPrefix == null) {
            return new HashSet<>();
        }
//        keywordPrefix = this.getUsableString(keywordPrefix);
        Set<Document> docSet = this.getAllDocsByPrefixForDelete(keywordPrefix);
        return this.deleteDocSetFromSystem(docSet);
    }

    protected Set<Document> getAllDocsByPrefixForDelete(String prefix) {
        prefix = this.getUsableString(prefix);

        Comparator<URI> comparator = this.getGenericTrieDeleteComparator();
        List<URI> uriList = this.trie.getAllWithPrefixSorted(prefix, comparator);
        Set<Document> docSet = new HashSet<>(uriList.size());
        for (URI uri : uriList) {
            docSet.add(this.bTree.get(uri));
        }
        return docSet;
    }

    /**
     * @param docSet to be deleted
     * @return set of URIs made of all docs in the docSet
     *
     * this method removes all words from all docs in docSet from the trie, removes all docs in the docSet from
     * the hashTable, and stores all docs in the docSet in the recycleBin. Then it adds a CommandSet made of
     * GenericCommands for each doc in the docSet
     */
    protected Set<URI> deleteDocSetFromSystem(Set<Document> docSet) {
        if (docSet == null) {
            throw new NullPointerException();
        }
        CommandSet<URI> commandSet = new CommandSet<>();
        for (Document document : docSet) {
            this.removeFromHeap(document);
            this.deleteAllWordsFromTrie(document);
            this.bTree.put(document.getKey(), null);
            GenericCommand<URI> command = new GenericCommand<>(document.getKey(), this.getUndoFunction(null, document));
            commandSet.addCommand(command);
        }
        this.commandStack.push(commandSet);

        Set<URI> uriSet = new HashSet<>(docSet.size());
        for (Document document : docSet) {
            uriSet.add(document.getKey());
        }
        return uriSet;
    }

    /**
     * set maximum number of documents that may be stored
     *
     * @param limit the limit
     */
    @Override
    public void setMaxDocumentCount(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("memory limit cannot be negative");
        }
        this.maxDocumentCount = limit;
        this.manageMemory();
    }

    /**
     * set maximum number of bytes of memory that may be used by all the documents in memory combined
     *
     * @param limit the limit
     */
    @Override
    public void setMaxDocumentBytes(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("memory limit cannot be negative");
        }
        this.maxDocumentBytes = limit;
        this.manageMemory();
    }
}
class EnhancedBTreeImpl<Key extends  Comparable<Key>,Value> extends BTreeImpl<Key,Value> {

    protected EnhancedBTreeImpl() {
        super();
    }

    protected boolean isStoredOnDisk(Key key) {
        Entry<Key,Value> entry = this.get(this.getRoot(), key, this.getHeight());
        if (entry == null) {
            return false;
        }
        return this.getStoredOnDisk(entry);
    }
}

class ComparableUriByDocTime implements Comparable<ComparableUriByDocTime> {

    private URI uri;
    private Function<URI,Document> function;

    protected ComparableUriByDocTime(URI uri, Function<URI,Document> function) {
        this.uri = uri;
        this.function = function;
    }

    protected URI getUri() {
        return this.uri;
    }

    protected Function<URI,Document> getFunction() {
        return this.function;
    }

    @Override
    public int compareTo(ComparableUriByDocTime o) {
        Document thisDoc = function.apply(this.uri);
        Document comparisonDoc = o.getFunction().apply(o.getUri() );
        return thisDoc.compareTo(comparisonDoc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ComparableUriByDocTime that = (ComparableUriByDocTime) o;
        return getUri().equals(that.getUri());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }
}
class EnhancedMinHeapImpl<E extends Comparable> extends MinHeapImpl<E> {

    protected EnhancedMinHeapImpl() {
        super();
    }

    protected boolean contains(E e) {
        return this.getElementsToArrayIndex().containsKey(e);
    }
}