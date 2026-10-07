package index;

import java.util.Arrays;

public class SuffixArray {
    public enum Status {
        NOT_BUILT,
        BUILDING,
        BUILT
    }

    private final String text;
    private final Integer[] suffixIndices;
    private Status status = Status.NOT_BUILT;

    public SuffixArray(String text) {
        this.status = Status.BUILDING;
        // Usually we add a terminal character like '$', but for simple binary search it's okay without it
        // We use lowercase normalized text for case-insensitive matching
        this.text = text.toLowerCase();
        int n = this.text.length();
        this.suffixIndices = new Integer[n];
        for (int i = 0; i < n; i++) {
            this.suffixIndices[i] = i;
        }

        // Sort suffixes using a custom comparator
        Arrays.sort(suffixIndices, (i, j) -> {
            int len1 = n - i;
            int len2 = n - j;
            int minLen = Math.min(len1, len2);
            for (int k = 0; k < minLen; k++) {
                char c1 = this.text.charAt(i + k);
                char c2 = this.text.charAt(j + k);
                if (c1 != c2) {
                    return c1 - c2;
                }
            }
            return len1 - len2;
        });
        
        this.status = Status.BUILT;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Binary search to find the exact substring.
     * Target complexity: O(M log N)
     * where M = pattern length, N = document length
     */
    public int searchPhrase(String pattern) {
        if (pattern == null || pattern.isEmpty()) return -1;
        
        String p = pattern.toLowerCase();
        int left = 0;
        int right = suffixIndices.length - 1;
        int m = p.length();

        while (left <= right) {
            int mid = left + (right - left) / 2;
            int suffixIndex = suffixIndices[mid];
            
            // Compare suffix with pattern
            int cmp = 0;
            for (int i = 0; i < m; i++) {
                if (suffixIndex + i >= text.length()) {
                    cmp = -1; // Pattern is longer than suffix
                    break;
                }
                char tc = text.charAt(suffixIndex + i);
                char pc = p.charAt(i);
                if (tc != pc) {
                    cmp = tc - pc;
                    break;
                }
            }

            if (cmp == 0) {
                return suffixIndex; // Match found
            } else if (cmp < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return -1; // Not found
    }
}
