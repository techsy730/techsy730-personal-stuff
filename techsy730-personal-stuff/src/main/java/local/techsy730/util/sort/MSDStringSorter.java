package local.techsy730.util.sort;

import java.io.IOException;
import java.util.Comparator;
import java.util.Deque;
import java.util.Arrays;

// TODO DOCUMENTATION!
// TODO benchmark support and comparison with Arrays.sort(Object[])
// XXX See if something can be done about "midstring" null characters ('\0') without harming performance much
// TODO Refactor to an instance type thing, which will allow some of the methods to be merged together some due to not having to need so many local variables.
/**
 * 
 * @author C. Sean Young
 * 
 */
public final class MSDStringSorter
{
    private MSDStringSorter()
    {} // Utility class

    // XXX Empirically determine a good estimate for these constants, based on common
    // usage cases
    //XXX This should be 16 or so on Java 7, but 7 on Java 6
    //NOTE Keep in mind that when this triggers, *up to* all of the remaining characters will be looked at for each comparison
    //Thus, setting this too high negates the advantages of MSD sort.
    //This number was chosen based on some extremely rough "back of the envelope" theoritcal calculations and verfied loosly
    //to be decent with very non-rigourous tests.
    //The advantage is that once a chunk gets sorted by this, that chunk will never have to be looked at again.
    private static final int MAX_INSERTION_SORT = 16;
    //Set to MAX_INSERTION_SORT to effectively disable this optimization
    //NOTE Keep in mind, if the Arrays.sort only on the current index is used
    //then we have to loop through that segment AGAIN to find the next layer of "chunks".
    //Thus, this needs to be kept somewhat low.
    private static final int MAX_ARRAYS_SORT = 31;
    
    //This causes a few extra loops to be run, and makes other loops gain extra operations, thus keep it somewhat low
    //However, the savings from a correctly computed max length can be rather large, so thus you don't need to be too conservative with it.
    private static final int MAX_UPDATE_MAX_LENGTH = (int)(MAX_ARRAYS_SORT * 1.4);
    
    //Set this to the minimum that there isn't a specific n-way sort method for.
    private static final int MIN_BLOCK_LEN_TO_MERGE = 4;

    // For these two values, SMALLER numbers mean MORE hesistant to trigger the optimization
    //Merging is a rather expensive operation, just to save a few method calls. Thus, be conservative with this number. 
    private static final int MAX_REMAINING_BEFORE_MERGING_RANGES = 18;
    //Be even more conservative with this number, as not only will it trigger more aggressive merges, but also cause a non-insertion call to
    //arrays sort to look at all of the remaining characters, which could get expensive.
    private static final int MAX_REMAINING_BEFORE_FULL_ARRAYS_SORT = 8;
    
/*
    static
    {
        assert MAX_INSERTION_SORT <= MAX_ARRAYS_SORT;
        assert MAX_REMAINING_BEFORE_FULL_ARRAYS_SORT <= MAX_REMAINING_BEFORE_MERGING_RANGES;
    }
*/
    public static final void sort(String[] arr)
    {
        sort(arr, 0, arr.length);
    }
    
    public static final void sort(String[] arr, int fromIndex, int toIndex)
    {
        sortPartially(Integer.MAX_VALUE, arr, fromIndex, toIndex);
    }
    
    public static final void sortPartially(int minCharsToSort, String[] arr)
    {
        sortPartially(minCharsToSort, arr, 0, arr.length);
    }
    
    public static void sortPartially(int minCharsToSort, String[] arr, int fromIndex, int toIndex)
    {
        // TODO We can do much better if we dump to char[] first, and also be willing to
        // have a "working" copy
        if(checkBounds(arr.length, fromIndex, toIndex)) return;
        final int len = toIndex - fromIndex;
        if(len == 2)
        {
            sortTwoItems(arr, fromIndex, fromIndex + 1, 0);
            return;
        }
        if(len == 3)
        {
            sortThreeItems(arr, fromIndex, fromIndex + 1, fromIndex + 2, 0);
            return;
        }
        if(len <= MAX_ARRAYS_SORT)
        {
            Arrays.sort(arr, fromIndex, toIndex);
            return;
        }
        String[] wc = new String[arr.length];
        // Thanks to how large strings can get, we cannot use recursion. As such, we must
        // maintain our own stack to maintain current state
        // XXX Empirically determine a good estimate for preallocation, based on common
        // usage cases
        // Not using <> syntactic shortcut to maintain source compatibility with java 1.6
        Deque<SortState> stack = new java.util.ArrayDeque<SortState>(
            Math.max(16, com.google.common.math.IntMath.log2(len, java.math.RoundingMode.UP)));
        stack.addFirst(new SortState(fromIndex, toIndex, 0, minCharsToSort));
        final int[] baseMaxIndexHolder = new int[1];
        while(!stack.isEmpty())
        {
            // "Recurse"
            processPart(arr, wc, stack, stack.pollFirst(), baseMaxIndexHolder);
        }
        return; // This redundant return is only so we can set a breakpoint here.
    }

    private static void processPart(String[] arr, String[] wc, Deque<SortState> stack,
        SortState currentState, final int[] maxIndexHolder)
    {
        final int start = currentState.start;
        final int end = currentState.end;
        final int index = currentState.index;
        int maxIndex = currentState.maxIndex;
        // If there are no characters in this block, just return now
        if(maxIndex == 0) return;

        assert index <= maxIndex : index + " > max of " + maxIndex;
        boolean needMaxIndexUpdate = maxIndex == Integer.MAX_VALUE || end - start <= MAX_UPDATE_MAX_LENGTH;
        // First, sort on the current character
        int[] buckets = doSortOnPart(arr, wc, start, end, index, maxIndex, needMaxIndexUpdate, maxIndexHolder);
        if(buckets == FULLY_SORTED) return;
        if(maxIndexHolder[0] != -1)
        {
            maxIndex = Math.min(maxIndexHolder[0], maxIndex);
            needMaxIndexUpdate = end - start < MAX_UPDATE_MAX_LENGTH;
        }

        // Now, find sub-blocks of the same characters

        char curChar = charAt(arr[start], index);
        if(index < maxIndex)  // Skip the next index if we have reached the max, as that
                             // signals that all strings in this sub-block don't need to
                             // be sorted anymore, as there is no data in the next index
        {
            if(curChar == charAt(arr[end - 1], index))
            {
                if(curChar == '\0')
                {
                    //All of them were the null char, which is a sign that all of them are past the string size
                    return;
                }
                // Common case, all the characters at this level were the same, just
                // immediately move on
                int sharedPrefixLen = findSharedPrefixLen(arr, start, end, index + 1, maxIndex);
                // Shared prefix length might be zero if all of them are them are shorter
                // than the current index
                stack.addFirst(currentState.setState(start, end, index + sharedPrefixLen + 1, maxIndex));
            }
            else
            {
                assert buckets != FULLY_SORTED;
                assert buckets.length != 1;
                if(buckets == INDEX_SORTED_NO_BUCKETS)
                    splitByCharGroups(arr, stack, start, end, index, maxIndex, curChar, currentState);
                else
                    splitByCharGroups(arr, stack, start, end, index, maxIndex, buckets, currentState, needMaxIndexUpdate);
            }
        }
        else
            assert index >= maxIndex; // This is just here for a nice breakpoint
    }

    private static void splitByCharGroups(String[] arr, Deque<SortState> stack, final int start,
        final int end, final int index, final int maxIndex, char curChar, SortState currentState)
    {
        int lastNewIndex = start;
        /** Max for the current block we are processing */
        int maxIndexBlock = 0;
        SortState tempHolder = currentState;
        SortState mergeHolder = null;
        boolean mergeHasMoreThanOne = false;
        // Once we start getting near the end of the strings, go ahead and be willing to have larger merges
        // Warning: Whatever range this may be, it MUST be small enough such that merged
        // range WILL fall under a complete sort, not a current index only sort
        final int remainingCharsToProcess = maxIndex - index;
        final boolean shouldMerge = remainingCharsToProcess < MAX_REMAINING_BEFORE_MERGING_RANGES;
        final int maxMergedRangeSize = remainingCharsToProcess < MAX_REMAINING_BEFORE_FULL_ARRAYS_SORT ? MAX_ARRAYS_SORT
            : MAX_INSERTION_SORT;
        for(int i = start; i < end; ++i)
        {
            //We have to iterate over all the strings in the block anyways, so we might as well track the length 
            if(arr[i].length() > maxIndexBlock)
            {
                maxIndexBlock = arr[i].length();
            }
            if(charAt(arr[i], index) != curChar)
            {
                maxIndexBlock = Math.min(maxIndexBlock, maxIndex);
                // New character, record end of block, and track new character
                final int newSize = i - lastNewIndex;
                if(newSize > 1) // Skip sub-blocks of size 1, one of the advantages of MSD
                                // sort
                {
                    if(curChar == '\0')
                    {
                        //Block for null character block, which is a sign that we are at the end of these strings, so just skip this block
                    }
                    //Just "inline sort" blocks of size 2
                    else if(newSize == 2)
                    {
                        sortTwoItems(arr, lastNewIndex, lastNewIndex + 1, index + 1);
                    }
                    else if(newSize == 3)
                    {
                        sortThreeItems(arr, lastNewIndex, lastNewIndex + 1, lastNewIndex + 2, index + 1);
                    }
                    // If this is a tiny block, try to see if we can accumulate with the
                    // next block
                    else if(shouldMerge &&
                        newSize < MAX_INSERTION_SORT && newSize >= MIN_BLOCK_LEN_TO_MERGE)
                    {
                        // From where we first saw this character to here,
                        // where the current pos is
                        SortState newState = new SortState(lastNewIndex, i, index, maxIndexBlock);
                        SortState newMerged = tempHolder.setState(newState).mergeWith(mergeHolder);
                        if(newMerged == null || newMerged.length() >= maxMergedRangeSize)
                        {
                            // We have grown too big, or hit a non-adjacent region, dump
                            // the merged so far (minus this block)
                            stack.addFirst(advanceIndexIfNeeded(mergeHolder, !mergeHasMoreThanOne));
                            // And then start a new merge range (starting with this block)
                            mergeHasMoreThanOne = false;
                            mergeHolder = newState;
                        }
                        else
                        {
                            if(mergeHolder != null)
                            {
                                mergeHasMoreThanOne = true;
                                mergeHolder.setState(newMerged);
                            }
                            else
                            {
                                mergeHolder = new SortState(newMerged);
                            }
                        }
                    }
                    else
                    {
                        if(mergeHolder != null) // Can't merge this block, as it is too big,
                                                // so dump out what we have so far if any                        
                        {
                            stack.addFirst(advanceIndexIfNeeded(mergeHolder, !mergeHasMoreThanOne));
                            mergeHasMoreThanOne = false;
                            mergeHolder = null;
                        }
                        stack.addFirst(new SortState(lastNewIndex, i, index + 1, maxIndexBlock));
                    }
                }
                else
                {
                    assert newSize > 1; // This is just here for a nice breakpoint
                }
                lastNewIndex = i;
                maxIndexBlock = -1;
                curChar = charAt(arr[i], index);
            }

        }
        // Yes, it may be that the last block could be merged in. But that would require
        // all of the logic above.
        // It isn't really worth the code complexity
        // However, we do need to write out any pending merged ranges
        if(mergeHolder != null)
        {
            stack.addFirst(advanceIndexIfNeeded(mergeHolder, !mergeHasMoreThanOne));
            mergeHasMoreThanOne = false;
            mergeHolder = null;
        }

        // The for loop may miss one block, the final one. If so, add it now
        final int newSize = end - lastNewIndex;
        maxIndexBlock = Math.min(maxIndexBlock, maxIndex);
        if(newSize > 1)
        {
            //Just "inline sort" blocks of size 2
            if(newSize == 2)
                sortTwoItems(arr, lastNewIndex, lastNewIndex + 1, index + 1);
            else
                stack.addFirst(currentState.setState(lastNewIndex, end, index + 1, maxIndexBlock));
        }
    }

    private static void splitByCharGroups(String[] arr, Deque<SortState> stack, final int start,
        final int end, final int index, final int maxIndex, final int[] buckets, SortState currentState, final boolean updateMaxSize)
    {
        SortState mergeHolder = null;
        boolean mergeHasMoreThanOne = false;
        SortState tempHolder = currentState;
        // Once we start getting near the end of the strings, go ahead and be willing to have larger merges
        // Warning: Whatever range this may be, it MUST be small enough such that merged
        // range WILL fall under a complete sort, not a current index only sort
        final int remainingCharsToProcess = maxIndex - index;
        final boolean shouldMerge = remainingCharsToProcess < MAX_REMAINING_BEFORE_MERGING_RANGES;
        final int maxMergedRangeSize = remainingCharsToProcess < MAX_REMAINING_BEFORE_FULL_ARRAYS_SORT ? MAX_ARRAYS_SORT
            : MAX_INSERTION_SORT;
        // curChar MUST be the min char seen
        for(int i = 0; i < buckets.length - 1; ++i)
        {
            int maxIndexBlock = -1;
            final int regionStart = buckets[i] + start;
            if(charAt(arr[regionStart], 0) == '\0')
            {
                //Block for null character block, which is a sign that we are at the end of these strings, so just skip this block
                continue;
            }
            final int regionEnd = buckets[i + 1] + start;
            final int newSize = regionEnd - regionStart;
            // record block
            if(newSize > 1) // Skip sub-blocks of size 1, one of the advantages of MSD sort
            {
                //Just "inline sort" blocks of size 2
                if(newSize == 2)
                {
                    sortTwoItems(arr, regionStart, regionStart + 1, index + 1);
                    continue;
                }
                if(newSize == 3)
                {
                    sortThreeItems(arr, regionStart, regionStart + 1, regionStart + 2, index + 1);
                    continue;
                }
                //Even if requested, don't bother tracking max length unless it is long enough not to get stuffed into a full sort next round
                //The full sorts don't benefit from knowing the max length ahead of time
                if(updateMaxSize && newSize > maxMergedRangeSize)
                {
                    for(int j = regionStart; j < regionEnd; ++j)
                    {
                        if(arr[j].length() > maxIndexBlock)
                        {
                            maxIndexBlock = arr[j].length();
                        }
                    }
                    maxIndexBlock = Math.min(maxIndexBlock, maxIndex);
                }
                else
                    maxIndexBlock = maxIndex;
                // If this is a tiny block, try to see if we can accumulate with the next block
                if(shouldMerge &&
                    newSize < MAX_INSERTION_SORT && newSize >= MIN_BLOCK_LEN_TO_MERGE)
                {
                    SortState newState = new SortState(regionStart, regionEnd, index, maxIndexBlock);
                    SortState newMerged = tempHolder.setState(newState).mergeWith(mergeHolder);
                    if(newMerged == null || newMerged.length() >= maxMergedRangeSize)
                    {
                        // We have grown too big, or hit a non-adjacent region, dump the
                        // merged so far (minus this block)
                        stack.addFirst(advanceIndexIfNeeded(mergeHolder, !mergeHasMoreThanOne));
                        // And then start a new merge range (starting with this block)
                        mergeHasMoreThanOne = false;
                        mergeHolder = newState;
                    }
                    else
                    {
                        if(mergeHolder != null) 
                        {
                            mergeHolder.setState(newMerged);
                            mergeHasMoreThanOne = true;
                        }
                        else
                        {
                            mergeHolder = new SortState(newMerged);
                        }
                    }
                }
                else
                {
                    if(mergeHolder != null) // Can't merge this block, as it is too big,
                                            // so dump out what we have so far if any
                    {
                        stack.addFirst(advanceIndexIfNeeded(mergeHolder, !mergeHasMoreThanOne));
                        mergeHasMoreThanOne = false;
                        mergeHolder = null;
                    }
                    stack.addFirst(new SortState(regionStart, regionEnd, index + 1, maxIndexBlock));
                }
            }
            else
            {
                assert newSize <= 1; // This is just here for a nice breakpoint
            }
        }

        // Yes, it may be that the last block could be merged in. But that would require
        // all of the logic above.
        // It isn't really worth the code complexity
        // However, we do need to write out any pending merged ranges
        if(mergeHolder != null)
        {
            stack.addFirst(advanceIndexIfNeeded(mergeHolder, !mergeHasMoreThanOne));
            mergeHasMoreThanOne = false;
            mergeHolder = null;
        }

        // The for loop may miss one block, the final one. If so, add it now
        final int regionStart = buckets[buckets.length - 1] + start;
        final int regionEnd = end;
        final int newSize = regionEnd - regionStart;
        if(newSize > 1)
        {
            //Just "inline sort" blocks of size 2
            if(newSize == 2)
                sortTwoItems(arr, regionStart, regionStart + 1, index + 1);
            else if(newSize == 3)
                sortThreeItems(arr, regionStart, regionStart + 1, regionStart + 2, index + 1);
            else
            {
                int maxIndexBlock = -1;
                //Even if requested, don't bother tracking max length unless it is long enough not to get stuffed into a full sort next round
                //The full sorts don't benefit from knowing the max length ahead of time
                if(updateMaxSize && newSize > maxMergedRangeSize)
                {
                    for(int j = regionStart; j < regionEnd; ++j)
                    {
                        if(arr[j].length() > maxIndexBlock)
                        {
                            maxIndexBlock = arr[j].length();
                        }
                    }
                    maxIndexBlock = Math.min(maxIndexBlock, maxIndex);
                }
                else
                    maxIndexBlock = maxIndex;
                stack.addFirst(currentState.setState(regionStart, regionEnd, index + 1, maxIndexBlock));
            }
        }
    }

    private static final SortState advanceIndexIfNeeded(SortState state, boolean advanceIndex)
    {
        if(advanceIndex) ++state.index;
        return state;
    }

    /**
     * Sorts the given subpart using the character index given. If this method returns true, then the fromIndex to the
     * toIndex (starting at charIndex) has been fully sorted, and thus no need to go any further for this subsection. If
     * this method returns false, then the fromIndex to the toIndex has only been sorted using index charIndex, and thus
     * you should keep going on for the next index in this subsection.
     * 
     * @param arr
     * @param wc
     * @param fromIndex
     * @param toIndex
     * @param charIndex
     * @param maxIndex
     * @return
     */
    private static final int[] doSortOnPart(final String[] arr, final String[] wc,
        final int fromIndex, final int toIndex, final int charIndex, final int maxIndex, boolean tryTrackMaxIndex, int[] maxIndexHolder)
    {
        final int len = toIndex - fromIndex;
        maxIndexHolder[0] = -1;
        if(len <= 1) return FULLY_SORTED;
        if(len == 2)
        {
            sortTwoItems(arr, fromIndex, fromIndex + 1, charIndex);
            return FULLY_SORTED;
        }
        if(len == 3)
        {
            sortThreeItems(arr, fromIndex, fromIndex + 1, fromIndex + 2, charIndex);
            return FULLY_SORTED;
        }
        if(len <= MAX_INSERTION_SORT)
        {
            insertionSort(arr, fromIndex, toIndex, charIndex);
            return FULLY_SORTED;
        }
        else if(len <= MAX_ARRAYS_SORT)
        {
            if(maxIndex - charIndex <= MAX_REMAINING_BEFORE_FULL_ARRAYS_SORT)
            {
                Arrays.sort(arr, fromIndex, toIndex, new NaturalSubstringComparator(charIndex));
                return FULLY_SORTED;
            }
            Arrays.sort(arr, fromIndex, toIndex, new NaturalCharComparatorAtIndex(charIndex));
            return INDEX_SORTED_NO_BUCKETS;
        }
        else
        {
            return bucketSort(arr, wc, fromIndex, toIndex, charIndex, tryTrackMaxIndex, maxIndexHolder);
        }
    }

    // A couple of "conditional flag arrays" as a hack to allow a sort of
    // "true, false, or data" type return
    private static final int[] FULLY_SORTED = new int[0];
    private static final int[] INDEX_SORTED_NO_BUCKETS = new int[0];

    // XXX #2 bottleneck
    // XXX Add some way to do this in the "middle" of a sort, so we don't loop through the array potentially twice
    // Basically, this needs to be merged into the comparator function somehow.
    private static final int findSharedPrefixLen(String[] arr, int fromIndex, int toIndex,
        int charIndex, int maxIndex)
    {
        // It doesn't really matter which element we choose, it will give the same result
        final String toTestAgainst = arr[fromIndex];

        final int len = Math.min(maxIndex, toTestAgainst.length());
        // If we want the shared prefix starting past the end, clearly there is no shared
        // prefix starting here
        if(charIndex >= len) return 0;
        //Is there a faster way to do this than a O(n*m) algorithm?
        for(int i = charIndex; i < len; ++i)
        {
            char currentChar = toTestAgainst.charAt(i);
            final int lastIndex = toIndex - 1; 
            //First off, check the char of the last string of this block, as if there are mismatches, that is a very likely place to find one of them
            if(charAt(arr[lastIndex], i) != currentChar)
                return i - charIndex;
            //Then, check the rest
            for(int j = fromIndex + 1; j < lastIndex; ++j)
            {
                if(charAt(arr[j], i) != currentChar)
                    return i - charIndex;
            }
        }
        // If we made it all the way through, the whole string matched
        return len - charIndex;
    }

    private final static void sortTwoItems(final String[] arr, 
        final int index1, final int index2, final int charIndex)
    {
        assert index1 < index2;
        if(compareSubtrStartingAt(arr[index1], arr[index2], charIndex) > 0)
            exchange(arr, index1, index2);
    }
    
    //Adapted from The Art of Computer Programming
    private final static void sortThreeItems(final String[] arr, 
        final int index1, final int index2, final int index3, final int charIndex)
    {
        assert index1 < index2;
        assert index2 < index3;
        if(compareSubtrStartingAt(arr[index1], arr[index2], charIndex) > 0)
        {
            String temp = arr[index1];
            if(compareSubtrStartingAt(arr[index2], arr[index3], charIndex) > 0)
            {
                arr[index1] = arr[index3];
                arr[index3] = temp;
            }
            else
            {
                //compareSubtrStartingAt(arr[index1], arr[index3], charIndex) > 0
                if(compareSubtrStartingAt(temp, arr[index3], charIndex) > 0)
                {
                    arr[index1] = arr[index2];
                    arr[index2] = arr[index3];
                    arr[index3] = temp;
                }
                else
                {
                    arr[index1] = arr[index2];
                    arr[index2] = temp;
                }
            }
        }
        else
        {
            if(compareSubtrStartingAt(arr[index2], arr[index3], charIndex) > 0)
            {
                String temp = arr[index3];
                arr[index3] = arr[index2];
                //compareSubtrStartingAt(arr[index1], original arr[index3], charIndex) > 0
                if(compareSubtrStartingAt(arr[index1], temp, charIndex) > 0)
                {
                    arr[index2] = arr[index1];
                    arr[index1] = temp;
                }
                else
                {
                    arr[index2] = temp;
                }
            }
        }
    }

    private static final void
        insertionSort(String[] arr, int fromIndex, int toIndex, int charIndex)
    {
        // XXX Do a binary search when moving entries back into the correct place, but
        // take care to preserve stability
        // The java.util.Arrays.sort has just a much, MUCH nicer insertion sort it uses
        // under the covers
        Arrays.sort(arr, fromIndex, toIndex, new NaturalSubstringComparator(charIndex));
        /*
         * for(int i = fromIndex; i < toIndex; ++i) for(int j = i; j > fromIndex && compareSubtrStartingAt(arr[j],
         * arr[j-1], charIndex) < 0; --j) exchange(arr, j, j-1);
         */
    }

    private static final int[] bucketSort(String[] arr, String[] wc, final int fromIndex, final int toIndex,
        final int charIndex, boolean trackMaxLen, int[] maxIndexHolder)
    {
        final int len = toIndex - fromIndex;
        // This will grow as needed
        // Default size should hold the standard ANSI charset
        int[] charCountBuffer = new int[0xFF];

        int minCharSeen = Integer.MAX_VALUE;
        int maxCharSeen = Integer.MIN_VALUE;
        int maxSeenSize = 0;

        if(trackMaxLen)
        {
            // Count how often each character appears
            for(int i = fromIndex; i < toIndex; ++i)
            {
                if(arr[i].length() > maxSeenSize)
                    maxSeenSize = arr[i].length();
                final char c = charAt(arr[i], charIndex);
                final int cAsInt = c;
                if(cAsInt < minCharSeen) minCharSeen = cAsInt;
                if(cAsInt > maxCharSeen) maxCharSeen = cAsInt;
                charCountBuffer = com.google.common.primitives.Ints.ensureCapacity(charCountBuffer, cAsInt, 20);
                ++charCountBuffer[cAsInt];
            }
            maxIndexHolder[0] = maxSeenSize;
        }
        else
        {
            // Count how often each character appears
            for(int i = fromIndex; i < toIndex; ++i)
            {
                final char c = charAt(arr[i], charIndex);
                final int cAsInt = c;
                if(cAsInt < minCharSeen) minCharSeen = cAsInt;
                if(cAsInt > maxCharSeen) maxCharSeen = cAsInt;
                charCountBuffer = com.google.common.primitives.Ints.ensureCapacity(charCountBuffer, cAsInt, 20);
                ++charCountBuffer[cAsInt];
            }
            maxIndexHolder[0] = -1;
        }

        if(minCharSeen == maxCharSeen)
        {
            // Only one char seen, that is a sign to just skip this sub-block, and move on
            return INDEX_SORTED_NO_BUCKETS;
        }

        // Compute index where each "section" will start
        int[] indexes = new int[(maxCharSeen - minCharSeen) + 1];

        {
            int currentIndex = 0;
            for(int i = minCharSeen; i <= maxCharSeen; ++i)
            {
                indexes[i - minCharSeen] = currentIndex;
                currentIndex += charCountBuffer[i];
            }
        }

        int[] indexesOrig = indexes.clone();

        // Now, copy over to the "working copy" array the strings into the right places
        for(int i = fromIndex; i < toIndex; ++i)
        {
            char c = charAt(arr[i], charIndex);
            // Copy to where the section is currently at, while incrementing that
            // section's index
            wc[(indexes[c - minCharSeen]++) + fromIndex] = arr[i];
        }

        // Copy back over into the real array
        System.arraycopy(wc, fromIndex, arr, fromIndex, len);
        return indexesOrig;
    }

    private static final void exchange(Object[] arr, int index1, int index2)
    {
        Object temp = arr[index1];
        arr[index1] = arr[index2];
        arr[index2] = temp;
    }

    private static final boolean checkBounds(int length, int fromIndex, int toIndex)
    {
        if(fromIndex < 0) throw new ArrayIndexOutOfBoundsException(fromIndex + " < 0");
        if(toIndex > length) throw new ArrayIndexOutOfBoundsException(toIndex + " > " + length);
        if(fromIndex > toIndex)
            throw new IllegalArgumentException("start position: " + fromIndex +
                " > end position: " + toIndex);
        if(toIndex - fromIndex <= 1) return true; // Trivial case, flag to skip
        return false;
    }

    /**
     * Compares CharSequences using the natural ordering of the characters at a given index
     * 
     * @author C. Sean Young
     * 
     */
    private static final class NaturalCharComparatorAtIndex implements java.util.Comparator<String>
    {
        final int index;

        NaturalCharComparatorAtIndex(int index)
        {
            this.index = index;
        }

        @Override
        // FIXME #3 bottleneck
        public int
            compare(String s1, String s2)
        {
            return charAt(s1, index) - charAt(s2, index);
        }
    }

    private static final class NaturalSubstringComparator implements java.util.Comparator<String>
    {
        final int index;

        NaturalSubstringComparator(int index)
        {
            this.index = index;
        }

        @Override
        public int compare(String s1, String s2)
        {
            return compareSubtrStartingAt(s1, s2, index);
        }
    }

    static final int compareSubtrStartingAt(final String s1, final String s2, final int index)
    {
        final int len1 = s1.length();
        final int len2 = s2.length();
        if(len1 <= index)
        {
            if(len2 <= index) return 0;
            return -1;
        }
        if(len2 <= index) return 1;
        
        //Pretty much ripped from String#compareTo(String)
        int lim = Math.min(len1, len2);

        int k = index;
        while (k < lim)
        {
            char c1 = s1.charAt(k);
            char c2 = s2.charAt(k);
            if (c1 != c2)
            {
                return c1 - c2;
            }
            k++;
        }
        //Even if the count doesn't start from the beginning, it doesn't matter, as cutting off the same number of characters still preserves ordering
        return len1 - len2;
    }

    /**
     * A wrapper method, returning '\0' instead of throwing an exception for out of bounds. Cleanly handles the case of
     * mismatching string lengths.
     * 
     * @param s
     * @param index
     * @return
     * @see CharSequence#charAt(int)
     */
    // FIXME #1 bottleneck when inlining does not occur
    static final char charAt(String s, int index)
    {
        // Any self respecting JIT should be able to inline these two methods to their
        // respective array calls, and then be able to inline this method itself.
        return index < s.length() ? s.charAt(index) : '\0';
    }

    // Thanks to how large strings can get, we cannot use recursion. As such, we must
    // maintain our own stack to maintain current state
    // This is the "mini stack frame" containing the data we need for the current
    // sub-problem.
    // Mutable to help reduce the number of allocations needed.
    // XXX Yea, allocating these is not as cheap as I was hoping for
    // Some moderate speedup can be had if I am willing to track ranges without having to
    // resort to this
    private static final class SortState implements Cloneable
    {
        /**
         * start index (inclusive)
         */
        int start;
        /**
         * end index (exclusive)
         */
        int end;
        int index;
        int maxIndex;

        public SortState(int start, int end, int index, int maxIndex)
        {
            this.start = start;
            this.end = end;
            this.index = index;
            this.maxIndex = maxIndex;
        }
        
        public SortState(SortState otherState)
        {
            this.start = otherState.start;
            this.end = otherState.end;
            this.index = otherState.index;
            this.maxIndex = otherState.maxIndex;
        }
        
        public static final SortState getOrSetState(SortState existing, SortState otherState)
        {
            if(existing == null)
                return new SortState(otherState);
            return existing.setState(otherState);
        }

        public SortState setState(int start, int end, int index, int maxIndex)
        {
            this.start = start;
            this.end = end;
            this.index = index;
            this.maxIndex = maxIndex;
            return this;
        }
        
        public SortState setState(SortState otherState)
        {
            this.start = otherState.start;
            this.end = otherState.end;
            this.index = otherState.index;
            this.maxIndex = otherState.maxIndex;
            return this;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + end;
            result = prime * result + index;
            result = prime * result + start;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if(this == obj) return true;
            if(!(obj instanceof SortState)) return false; // Handles the null case
            SortState other = (SortState)obj;
            if(end != other.end) return false;
            if(index != other.index) return false;
            if(start != other.start) return false;
            return true;
        }

        public int length()
        {
            return end - start;
        }

        @Override
        public String toString()
        {
            return "[" + start + ", " + end + ") @ " + index + " (max: " + maxIndex + ')';
        }

        /**
         * Attempts to merge two adjacent ranges over the same indexes, returns null if it cannot.
         * The object will be untouched if the merged could not be done.
         * 
         * @param other
         * @return this (which is now merged with other), or {@literal null} if it cannot merge.
         */
        public SortState mergeWith(SortState other)
        {
            if(this == other) return null;
            if(other == null) return this;
            if(index != other.index) return null;
            if(start != other.end && end != other.start) return null;
            boolean isOtherSecond = start < other.start;
            return setState(isOtherSecond ? start : other.start, isOtherSecond ? other.end
                : end, index, Math.max(maxIndex, other.maxIndex));
        }

        @Override
        public SortState clone()
        {
            return new SortState(start, end, index, maxIndex);
        }

    }

    private static final int NUM_TIMES_WARMUP = 200;
    private static final int NUM_TIMES_RUN_SORT = 1000;
    private static final int MAX_NUM_TO_PRINT = 1000;

    private static final int TIME_TO_PAUSE_AFTER_WARMUP = 8;
    private static final int INDEX_TO_PART_SORT_TO = 21;

    public static void main(String... args) throws IOException, InterruptedException
    {
        // System.err.println("Pausing for 30 seconds");
        // Thread.sleep(30000);
        // As of Java 1.7.0_02, java.util.Arrays has a whopping 124 methods in it.
        // Due to the nature of methods, there are ALOT of shared prefixes, making this a
        // decent test candidate
        String[] test = org.apache.commons.io.IOUtils.readLines(
            MSDStringSorter.class.getResourceAsStream("usagov_bitly_data2012-10-29-1351522030"))
            .toArray(new String[0]);
        //String[] test = org.apache.commons.io.IOUtils.readLines(MSDStringSorter.class.getResourceAsStream("README")).toArray(new String[0]);
        //String[] test = toArrayString(Arrays.class.getDeclaredMethods());
        //java.util.Collections.shuffle(Arrays.asList(test));
        //String[] test = new String[5000];
        //Arrays.fill(test, "");
        System.err.println("Dataset is: " + test.length + " entries long");
        
        final Comparator<String> compWith = 
            new NaturalSubstringComparator(INDEX_TO_PART_SORT_TO);
        
        for(String s : test)
        {
            if(s.contains("\0"))
            {
                System.err
                    .println("Dataset contains embedded null chars, this may cause discrepencies");
                break;
            }
        }
        // Now, "warm up" the JIT

        System.err.println("Warming up JIT compiler");

        for(int i = 0; i < NUM_TIMES_WARMUP; ++i)
        {
            sort(test.clone());
            Arrays.sort(test.clone());
            String[] testClone = test.clone();
            sortPartially(INDEX_TO_PART_SORT_TO, testClone);
            Arrays.sort(testClone, compWith);
        }

        // Alright, clean up, and pause
        System.gc();
        System.err.println("Pausing for " + TIME_TO_PAUSE_AFTER_WARMUP + " seconds");
        Thread.sleep(TIME_TO_PAUSE_AFTER_WARMUP * 1000);
        System.gc();

        String[] test2 = test.clone();
        String[] test3 = test.clone();

        System.err.println("Starting MSD sort test");
        if(test.length < MAX_NUM_TO_PRINT) System.out.println(Arrays.toString(test));
        long startTime = System.nanoTime();
        for(int i = 0; i < NUM_TIMES_RUN_SORT - 1; ++i)
        {
            sort(test.clone());
        }
        sort(test);
        long endTime = System.nanoTime();

        long msdSortTime = endTime - startTime;

        System.err.println("Finished MSD sort test");
        System.gc();
        System.err.println("Starting Arrays.sort test");

        startTime = System.nanoTime();
        for(int i = 0; i < NUM_TIMES_RUN_SORT - 1; ++i)
        {
            Arrays.sort(test2.clone());
        }
        Arrays.sort(test2);
        endTime = System.nanoTime();
        
        long arraysSortTime = endTime - startTime;
        
        System.err.println("Finished Arrays.sort test");
        System.gc();
        System.err.println("Starting partial sort to Arrays.sort test");
        
        startTime = System.nanoTime();
        for(int i = 0; i < NUM_TIMES_RUN_SORT - 1; ++i)
        {
            String[] test3Clone = test3.clone();
            sortPartially(INDEX_TO_PART_SORT_TO, test3Clone);
            Arrays.sort(test3Clone, compWith);
        }
        sortPartially(INDEX_TO_PART_SORT_TO, test3);
        Arrays.sort(test3, compWith);
        endTime = System.nanoTime();
        System.err.println("Finished partial sort to Arrays.sort test");
        
        long partialSortTime = endTime - startTime;
     
        if(test.length < MAX_NUM_TO_PRINT)
        {
            System.out.println(Arrays.toString(test));
            System.out.println(Arrays.toString(test2));
            System.out.println(Arrays.toString(test3));
        }
        boolean matchedUp = Arrays.equals(test, test2);
        if(!matchedUp)
        {
            System.err.println("Sort mismatch: full MSD sort");
            int misMatchIndex = findDiffIndex(test, test2);
            System.err.println("@" + misMatchIndex + ": \"" + test[misMatchIndex] + "\" != \"" +
                test2[misMatchIndex] + '"');
        }
        
        matchedUp = Arrays.equals(test3, test2);
        if(!matchedUp)
        {
            System.err.println("Sort mismatch: partial MSD sort");
            int misMatchIndex = findDiffIndex(test3, test2);
            System.err.println("@" + misMatchIndex + ": \"" + test3[misMatchIndex] + "\" != \"" +
                test2[misMatchIndex] + '"');
        }

        System.out.println("MSD sort, total:" + msdSortTime / 1000000 + ", average: " +
            ((double)msdSortTime / NUM_TIMES_RUN_SORT) / 1000000);
        System.out.println("Arrays.sort sort, total:" + arraysSortTime / 1000000 + ", average: " +
            ((double)arraysSortTime / NUM_TIMES_RUN_SORT) / 1000000);
        System.out.println("Partial sort to Arrays.sort, total:" + partialSortTime / 1000000 + ", average: " +
            ((double)partialSortTime / NUM_TIMES_RUN_SORT) / 1000000);
    }

    private static String[] toArrayString(Object[] array)
    {
        if(array instanceof String[]) return (String[])array.clone();
        String[] toReturn = new String[array.length];
        for(int i = 0; i < array.length; ++i)
        {
            toReturn[i] = array[i].toString();
        }
        return toReturn;
    }

    private static int findDiffIndex(Object[] array1, Object[] array2)
    {
        assert array1.length == array2.length;
        for(int i = 0; i < array1.length; ++i)
        {
            if(!array1[i].equals(array2[i])) return i;
        }
        return -1;
    }

}
