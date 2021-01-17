package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.IOException;
import java.util.Arrays;

public class BTreeImpl<Key extends Comparable<Key>,Value> implements BTree<Key,Value> {

    private final int MAX;
    private Node root; //root of the B-tree
    private Node leftMostExternalNode;
    private int height; //height of the B-tree
    private int n; //number of key-value pairs in the B-tree
    private PersistenceManager<Key, Value> pm;

    public BTreeImpl() {
        this.MAX = 6;
        this.root = new Node(0, MAX);
        this.leftMostExternalNode = this.root;
        this.height = 0;
        this.n = 0;
        this.pm = null;
    }

    /**
     * if the value was stored on disk, this method *WILL REMOVE* it from disk during deserialization
     */
    @Override
    public Value get(Key key) {
        if (key == null) {
            throw new IllegalArgumentException("argument to get() is null");
        }
        Entry entry = this.get(this.root, key, this.height);
        if (entry != null) {
            if (entry.val != null) {
                return (Value) entry.val;
            } else {
                Value val = null;
                try {
                    val = this.pm.deserialize(key);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                entry.val = val;
                entry.storedOnDisk = false;
                return val;
            }
        }
        return null;
    }

//  this method DOES NOT DEAL WITH (DE)SERIALIZATION and therefore doesn't interact with the disk
    protected Entry<Key,Value> get(Node currentNode, Key key, int height) {
        Entry[] entries = currentNode.entries;

        //current node is external (i.e. height == 0)
        if (height == 0) {
            for (int j = 0; j < currentNode.entryCount; j++) {
                if (isEqual(key, entries[j].key)) {
                    //found desired key. Return its value
                    return entries[j];
                }
            }
            //didn't find the key
            return null;
        }
        //current node is internal (height > 0)
        else {
            for (int j = 0; j < currentNode.entryCount; j++) {
                //if (we are at the last key in this node OR the key we
                //are looking for is less than the next key, i.e. the
                //desired key must be in the subtree below the current entry),
                //then recurse into the current entry’s child
                if (j + 1 == currentNode.entryCount || isLess(key, entries[j + 1].key)) {
                    return this.get(entries[j].child, key, height - 1);
                }
            }
            //didn't find the key
            return null;
        }
    }

    /**
     * @param key - the key for the value we are storing
     * @param value - the value that should be stored under the key
     * @return - the value that used to be stored under the key (whether on disk or memory) OR
     * null if no previous value was stored
     */
    @Override
    public Value put(Key key, Value value) {
        if (key == null) {
            throw new IllegalArgumentException("key argument to put() is null");
        }
        //if the key already exists in the b-tree, simply replace the value
        @SuppressWarnings("unchecked") Entry<Key,Value> alreadyThere = (Entry<Key,Value>) this.get(this.root, key, this.height);
        if(alreadyThere != null) {
            try {
                return this.replaceEntryValue(alreadyThere, value);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Node newNode = this.put(this.root, key, value, this.height);
        this.n++;

        //adding if statement just for testing purposes; this shouldn't be needed as we just
        //added the value to the BTree, so it definitely should not be null
        alreadyThere = this.get(this.root, key, this.height);
//        if (alreadyThere == null) {
//            throw new NullPointerException();
//        }

        if (newNode == null) {
            return null;
        }

        //split the root:
        //Create a new node to be the root.
        //Set the old root to be new root's first entry.
        //Set the node returned from the call to put to be new root's second entry
        Node newRoot = new Node(2, this.MAX);
        newRoot.entries[0] = new Entry(this.root.entries[0].key, null, this.root);
        newRoot.entries[1] = new Entry(newNode.entries[0].key, null, newNode);
        this.root = newRoot;
        //a split at the root always increases the tree height by 1
        this.height++;
        return null;
    }

    private Node put(Node currentNode, Key key, Value val, int height) {
        int j;
        Entry newEntry = new Entry(key, val, null);

        //external node
        if (height == 0) {
            //find index in currentNode’s entry[] to insert new entry
            //we look for key < entry.key since we want to leave j
            //pointing to the slot to insert the new entry, hence we want to find
            //the first entry in the current node that key is LESS THAN
            for (j = 0; j < currentNode.entryCount; j++) {
                if (isLess(key, currentNode.entries[j].key)) {
                    break;
                }
            }
        }

        // internal node
        else {
            //find index in node entry array to insert the new entry
            for (j = 0; j < currentNode.entryCount; j++) {
                //if (we are at the last key in this node OR the key we
                //are looking for is less than the next key, i.e. the
                //desired key must be added to the subtree below the current entry),
                //then do a recursive call to put on the current entry’s child
                if ((j + 1 == currentNode.entryCount) || isLess(key, currentNode.entries[j + 1].key)) {
                    //increment j (j++) after the call so that a new entry created by a split
                    //will be inserted in the next slot
                    Node newNode = this.put(currentNode.entries[j++].child, key, val, height - 1);
                    if (newNode == null) {
                        return null;
                    }
                    //if the call to put returned a node, it means I need to add a new entry to
                    //the current node
                    newEntry.key = newNode.entries[0].key;
                    newEntry.val = null;
                    newEntry.child = newNode;
                    break;
                }
            }
        }
        //shift entries over one place to make room for new entry
        for (int i = currentNode.entryCount; i > j; i--) {
            currentNode.entries[i] = currentNode.entries[i - 1];
        }
        //add new entry
        currentNode.entries[j] = newEntry;
        currentNode.entryCount++;
        if (currentNode.entryCount < this.MAX) {
            //no structural changes needed in the tree
            //so just return null
            return null;
        }
        else {
            //will have to create new entry in the parent due
            //to the split, so return the new node, which is
            //the node for which the new entry will be created
            return this.split(currentNode, height);
        }
    }

    private Value replaceEntryValue(Entry<Key,Value> entry, Value newValue) throws IOException {
        if (entry == null) {
            throw new NullPointerException();
        }
        if (entry.val != null) {
            Value oldValue = entry.val;
            entry.val = newValue;
            entry.storedOnDisk = false;
            return oldValue;
        }
        entry.val = newValue;
        entry.storedOnDisk = false;
        return this.pm.deserialize(entry.key);
    }

    private Node split(Node currentNode, int height) {
        Node newNode = new Node(this.MAX / 2, this.MAX);
        //by changing currentNode.entryCount, we will treat any value
        //at index higher than the new currentNode.entryCount as if
        //it doesn't exist
        currentNode.entryCount = this.MAX / 2;
        //copy top half of h into t
        for (int j = 0; j < this.MAX / 2; j++) {
            newNode.entries[j] = currentNode.entries[MAX / 2 + j];
        }
        //external node
        if (height == 0) {
            newNode.setNext(currentNode.getNext());
            newNode.setPrevious(currentNode);
            currentNode.setNext(newNode);
        }
        return newNode;
    }

    @Override
    public void moveToDisk(Key key) throws Exception {
        @SuppressWarnings("unchecked") Entry<Key,Value> entry = (Entry<Key, Value>) this.get(this.root, key, this.height);
        if (entry == null) {
            return;
        }
        if (entry.val == null) {
            return;
        }
        this.pm.serialize(key, entry.val);
        entry.setVal(null);
        entry.setStoredOnDisk(true);
    }

    @Override
    public void setPersistenceManager(PersistenceManager<Key, Value> pm) {
        this.pm = pm;
    }

    private boolean isLess(Comparable k1, Comparable k2) {
        return k1.compareTo(k2) < 0;
    }

    private boolean isEqual(Comparable k1, Comparable k2) {
        return k1.compareTo(k2) == 0;
    }

    protected Node getRoot() {
        return this.root;
    }

    protected int getHeight() {
        return this.height;
    }

    protected PersistenceManager<Key,Value> getPM() {
        return this.pm;
    }

    protected boolean getStoredOnDisk(Entry entry) {
        return entry.getStoredOnDisk();
    }

    //B-tree node data type
    protected class Node {
        int entryCount; // number of entries
        Entry[] entries; // the array of children
        Node next;
        Node previous;

        // create a node with k entries
        Node(int k, int max) {
            this.entryCount = k;
            this.entries = new Entry[max];
        }

        void setNext(Node next) {
            this.next = next;
        }

        Node getNext() {
            return this.next;
        }

        void setPrevious(Node previous) {
            this.previous = previous;
        }

        Node getPrevious() {
            return this.previous;
        }

        Entry[] getEntries() {
            return Arrays.copyOf(this.entries, this.entryCount);
        }
    }

    //internal nodes: only use key and child
//external nodes: only use key and value
    protected class Entry<Key extends Comparable, Value> {
        Key key;
        Value val;
        boolean storedOnDisk;
        Node child;

        Entry(Key key, Value val, Node child) {
            this.key = key;
            this.val = val;
            this.storedOnDisk = false;
            this.child = child;
        }

        protected void setVal(Value val) {
            this.val = val;
        }

        protected void setStoredOnDisk(boolean b) {
            this.storedOnDisk = b;
        }

        protected boolean getStoredOnDisk() {
            return this.storedOnDisk;
        }
    }
}
