package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;

//    0-9 are 48-57
//    A-Z are 65-90 (a-z are 97-122)

public class TrieImpl<Value> implements Trie<Value> {


    TrieNode<Value> root;

    public TrieImpl() {
        this.root = new TrieNode<Value>();
    }

    /**
     * @param string
     * @return a string with only alpha-numeric characters and no whitespace
     */
    private String getUsableString(String string) {
        string = string.replaceAll("[^0-9a-zA-Z ]", "");
        string = string.trim();
        return string.toUpperCase();
    }

    /**
     * add the given value at the given key
     *
     * @param key
     * @param val
     */
    @Override
    public void put(String key, Value val) {
        if (val == null || key == null) {
            return;
        }
        key = this.getUsableString(key);
        this.root = this.put(this.root, key, val, 0);

    }

    private TrieNode put(TrieNode<Value> x, String key, Value val, int d) {
        if (x == null) {
            x = new TrieNode<Value>();
        }
        if (d == key.length()) {
            x.valueSet.add(val);
            return x;
        }

        char c = key.charAt(d);
//        TrieNode<Value> node = x.getNodeLink(c);
//        node = this.put(x.getNodeLink(c), key, val, d + 1);
        x.linksArray[c] = this.put(x.linksArray[c], key, val, d+1);
        return x;
    }

    /**
     * get all exact matches for the given key, sorted in descending order.
     * Search is CASE INSENSITIVE.
     *
     * @param key
     * @param comparator used to sort  values
     * @return a List of matching Values, in descending order
     */
    @Override
    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        if (comparator == null || key == null) {
            return new ArrayList<>();
        }

        key = this.getUsableString(key);

        TrieNode<Value> x = this.get(this.root, key, 0);

        if (x == null) {
            return new ArrayList<>();
        }

        HashSet<Value> values = x.getValues();
        ArrayList<Value> list = new ArrayList<>(values.size());
        list.addAll(values);
        list.sort(comparator);
        return list;
    }

    TrieNode<Value> get(TrieNode<Value> x, String key, int d) {
        if (x == null) {
            return null;
        }
        if (d == key.length()) {
            return x;
        }
        char c = key.charAt(d);
        return this.get(x.linksArray[c], key, d + 1);
    }

    /**
     * get all matches which contain a String with the given prefix, sorted in descending order.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE INSENSITIVE.
     *
     * @param prefix
     * @param comparator used to sort values
     * @return a List of all matching Values containing the given prefix, in descending order
     */
    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if (prefix == null || comparator == null) {
            return new ArrayList<>();
        }

        prefix = this.getUsableString(prefix);

        TrieNode<Value> endOfPrefix = this.get(this.root, prefix, 0);

        if (endOfPrefix == null) {
            return new ArrayList<>();
        }

        HashSet<Value> resultsSet = new HashSet<>();
        this.collect(endOfPrefix, new StringBuilder(prefix), resultsSet);
        ArrayList<Value> resultsList = new ArrayList<>(resultsSet);
        resultsList.sort(comparator);
        return resultsList;
    }

    /**
     * collects all of the values stored in x and all nodes below them and stores it in a set
     *
     * @param x
     * @param prefix
     * @param results
     */
    private void collect(TrieNode<Value> x, StringBuilder prefix, Set<Value> results) {
        results.addAll(x.getValues());

        for (char c = 0; c < 91; c++) {
            if (x.getNodeLink(c) != null) {
                prefix.append(c);
                this.collect(x.getNodeLink(c), prefix, results);
                prefix.deleteCharAt(prefix.length() - 1);
            }
        }

    }

    /**
     * Delete the subtree rooted at the last character of the prefix.
     * Search is CASE INSENSITIVE.
     *
     * @param prefix
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if (prefix == null) {
            return new HashSet<>();
        }

        prefix = this.getUsableString(prefix);

        if (prefix.equals("")) {
            return new HashSet<>();
        }

        HashSet<Value> results = new HashSet<>();
        TrieNode<Value> node = this.get(this.root, prefix, 0);
        if (node == null) {
            return new HashSet<>();
        }
        this.collect(node, new StringBuilder(prefix), results);

        //***here is where you need to null out the reference*****
        char[] chars = new char[prefix.length() - 1];
        for (int i = 0; i < prefix.length() - 1; i++) {
            chars[i] = prefix.toCharArray()[i];
        }
        String beforePrefix = new String(chars);
        char lastCharInPrefix = prefix.toCharArray()[prefix.length() - 1];
        this.get(this.root, beforePrefix, 0).linksArray[lastCharInPrefix] = null;

        this.deleteAll(this.root, prefix, 0);

        return results;
    }


    private TrieNode<Value> deleteAll(TrieNode<Value> x, String key, int d) {
        if (x == null) {
            return null;
        }
        if (d == key.length()) {
            x.deleteAllValues();
        }
        else {
            char c = key.charAt(d);
            x.linksArray[c] = this.deleteAll(x.linksArray[c], key, d + 1);
        }
        if (x.getValues().size() != 0) {
            return x;
        }
        for (int i = 0; i < 91; i++) {
            if (x.linksArray[i] != null) {
                return x;
            }
        }
        return null;
    }

    /**
     * Delete all values from the node of the given key (do not remove the values from other nodes in the Trie)
     *
     * @param key
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAll(String key) {
        if (key == null) {
            return new HashSet<>();
        }
        key = this.getUsableString(key);

        TrieNode<Value> x = this.get(this.root, key, 0);

        if (x == null) {
            return new HashSet<>();
        }
        Set<Value> deletedValues = x.deleteAllValues();

        this.deleteAll(this.root, key, 0);

        return deletedValues;
    }

    /**
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     *
     * @param key
     * @param val
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */
    @Override
    public Value delete(String key, Value val) {
        if (key == null || val == null) { //IS THIS SUPPOSED TO RETURN NULL?????
            return null;
        }

        key = this.getUsableString(key);

        TrieNode<Value> node = this.get(this.root, key, 0);

        if (node == null) {
            return null;
        }
        boolean valueWasDeleted = node.deleteValue(val);
        if (!valueWasDeleted) {
            return null;
        }
        else if (node.valueSet.size() != 0){
            return val;
        }
        else {
            this.deleteAll(key);
            return val;
        }
    }
}

/**
 * @param <Value>
 */
class TrieNode<Value> {

    HashSet<Value> valueSet;
    TrieNode[] linksArray;

    TrieNode() {
        this.valueSet = new HashSet<>();
        this.linksArray = new TrieNode[91];
    }

    HashSet<Value> getValues() {
        return this.valueSet;
    }


    /**
     * @param value to add to valueSet
     * @return true if value changed the set; false if set already contained the value
     */
    boolean addToValues(Value value) {
        return this.valueSet.add(value);
    }

    /**
     * @param value to delete from valueSet
     * @return true if deletion changed the set; false if set didn't contain the value
     */
    boolean deleteValue(Value value) {
        return this.valueSet.remove(value);
    }

    /**
     * @param arrayIndex - number of index whose node you would like to get
     * @return node stored in the index, or null if there is no node
     */
    TrieNode<Value> getNodeLink(int arrayIndex) {
        return this.linksArray[arrayIndex];
    }


    /**
     * deletes all values and by setting valuesSet to an empty HashSet
     */
    Set<Value> deleteAllValues() {
        Set<Value> oldValues = this.valueSet;
        this.valueSet = new HashSet<>();
        return oldValues;
    }
}