package dev.by1337.core.command.bcmd.argument;

import dev.by1337.cmd.*;
import org.bukkit.Keyed;
import org.bukkit.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ArgumentRegistry<C, E extends Keyed> extends Argument<C, E> {

    protected final NamespacedKeyTrie<E> lookup = new NamespacedKeyTrie<>();

    protected ArgumentRegistry(String name) {
        super(name);
    }

    public ArgumentRegistry(String name, Registry<E> registry) {
        this(name, registry, false);
    }

    public ArgumentRegistry(String name, Registry<E> registry, boolean noNamespace) {
        super(name);
        build(registry, noNamespace);
    }

    protected void build(Registry<E> registry, boolean noNamespace) {
        registry.iterator().forEachRemaining(key -> {
            lookup.insert(key.getKey().getKey(), key);
            if (noNamespace) return;
            lookup.insert(key.getKey().toString(), key);
        });
    }

    @Override
    public void parse(C ctx, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        String str = reader.readString();
        if (str.isEmpty()) {
            return;
        }
        String input = str.toLowerCase(Locale.ENGLISH);
        E value = lookup.search(input);
        if (value == null) {
            List<String> suggestions = lookup.getWordsWithPrefix("", 5);
            throw new CommandMsgError("Unknown " + name + ": " + str + ", did you mean: " + String.join(", ", suggestions));
        }
        out.put(name, value);
    }

    @Override
    public void suggest(C ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
        String str = reader.readString();
        //todo триграммы?
        lookup.getWordsWithPrefix(str, 15).forEach(suggestions::suggest);
    }

    @Override
    public boolean compilable() {
        return true;
    }

    @Override
    public boolean allowAsync() {
        return true;
    }

    protected static class NamespacedKeyTrie<V> {
        private static final int ALPHABET_SIZE = 40;
        private static final char[] INDEX_TO_CHAR = new char[ALPHABET_SIZE];
        private static final int[] CHAR_TO_INDEX = new int[128]; // ASCII достаточно

        static {
            Arrays.fill(CHAR_TO_INDEX, -1);

            int idx = 0;
            // a-z
            for (char c = 'a'; c <= 'z'; c++) {
                INDEX_TO_CHAR[idx] = c;
                CHAR_TO_INDEX[c] = idx++;
            }
            // 0-9
            for (char c = '0'; c <= '9'; c++) {
                INDEX_TO_CHAR[idx] = c;
                CHAR_TO_INDEX[c] = idx++;
            }
            INDEX_TO_CHAR[idx] = '.';
            CHAR_TO_INDEX['.'] = idx++;
            INDEX_TO_CHAR[idx] = '_';
            CHAR_TO_INDEX['_'] = idx++;
            INDEX_TO_CHAR[idx] = '-';
            CHAR_TO_INDEX['-'] = idx++;
            INDEX_TO_CHAR[idx] = ':';
            CHAR_TO_INDEX[':'] = idx++;
        }

        private TrieNode<V> root = new TrieNode<V>();
        private int maxWordLength = 0;

        public void clear() {
            root = new TrieNode<V>();
            maxWordLength = 0;
        }

        public void insert(String word, V value) {
            TrieNode<V> node = root;
            int length = word.length();
            for (int i = 0; i < length; i++) {
                char ch = word.charAt(i);
                int idx;
                if (ch > 128 || (idx = CHAR_TO_INDEX[ch]) < 0)
                    throw new IllegalArgumentException("Illegal character: " + ch);

                TrieNode<V> next = node.children[idx];
                if (next == null) {
                    next = new TrieNode<V>();
                    node.children[idx] = next;
                }
                node = next;
            }
            node.val = value;
            if (length > maxWordLength) {
                maxWordLength = length;
            }
        }

        public V search(String word) {
            TrieNode<V> node = root;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (c > 128) return null;
                int idx = CHAR_TO_INDEX[c];
                if (idx < 0 || (node = node.children[idx]) == null) return null;
            }
            return node.val;
        }

        public boolean startsWith(String prefix) {
            TrieNode<V> node = root;
            for (int i = 0; i < prefix.length(); i++) {
                int idx = CHAR_TO_INDEX[prefix.charAt(i)];
                if (idx < 0 || (node = node.children[idx]) == null) return false;
            }
            return true;
        }

        public V delete(String word) {
            return delete(root, word, 0);
        }

        private V delete(TrieNode<V> node, String word, int depth) {
            if (node == null) return null;

            if (depth == word.length()) {
                if (node.val == null) return null;

                V oldValue = node.val;
                node.val = null;

                if (node.isEmpty()) {
                    return oldValue;
                }
                return oldValue;
            }

            int idx = CHAR_TO_INDEX[word.charAt(depth)];
            TrieNode<V> child = node.children[idx];
            if (child == null) return null;

            V oldValue = delete(child, word, depth + 1);

            if (oldValue != null && child.val == null && child.isEmpty()) {
                node.children[idx] = null;
            }

            return oldValue;
        }

        public List<String> getWordsWithPrefix(String prefix, int limit) {
            TrieNode<V> node = root;
            for (int i = 0; i < prefix.length(); i++) {
                char c = prefix.charAt(i);
                if (c > 128) return List.of();
                int idx = CHAR_TO_INDEX[c];
                if (idx < 0 || (node = node.children[idx]) == null) return List.of();
            }

            List<String> result = new ArrayList<>(Math.min(limit, 32));

            char[] buffer = new char[Math.max(maxWordLength, prefix.length())];
            prefix.getChars(0, prefix.length(), buffer, 0);

            dfs(node, buffer, prefix.length(), result, limit);
            return result;
        }

        private void dfs(TrieNode<V> node, char[] buffer, int depth,
                         List<String> result, int limit) {
            if (result.size() >= limit) return;

            if (node.val != null) {
                result.add(new String(buffer, 0, depth));
                if (result.size() >= limit) return;
            }

            for (int i = 0; i < ALPHABET_SIZE; i++) {
                TrieNode<V> child = node.children[i];
                if (child != null) {
                    buffer[depth] = INDEX_TO_CHAR[i];
                    dfs(child, buffer, depth + 1, result, limit);
                    if (result.size() >= limit) return;
                }
            }
        }

        public List<V> getValuesWithPrefix(String prefix, int limit) {
            TrieNode<V> node = root;
            for (int i = 0; i < prefix.length(); i++) {
                char c = prefix.charAt(i);
                if (c > 128) return List.of();
                int idx = CHAR_TO_INDEX[c];
                if (idx < 0 || (node = node.children[idx]) == null) return List.of();
            }

            List<V> result = new ArrayList<>(Math.min(limit, 32));

            char[] buffer = new char[Math.max(maxWordLength, prefix.length())];
            prefix.getChars(0, prefix.length(), buffer, 0);

            dfsValues(node, buffer, prefix.length(), result, limit);
            return result;
        }

        private void dfsValues(TrieNode<V> node, char[] buffer, int depth,
                               List<V> result, int limit) {
            if (result.size() >= limit) return;

            if (node.val != null) {
                result.add(node.val);
                if (result.size() >= limit) return;
            }

            for (int i = 0; i < ALPHABET_SIZE; i++) {
                TrieNode<V> child = node.children[i];
                if (child != null) {
                    buffer[depth] = INDEX_TO_CHAR[i];
                    dfsValues(child, buffer, depth + 1, result, limit);
                    if (result.size() >= limit) return;
                }
            }
        }

        private static class TrieNode<V> {
            TrieNode<V>[] children;
            V val;

            @SuppressWarnings("unchecked")
            TrieNode() {
                this.children = (TrieNode<V>[]) new TrieNode[ALPHABET_SIZE];
            }

            boolean isEmpty() {
                for (TrieNode<V> child : children)
                    if (child != null) return false;
                return true;
            }
        }
    }
}
