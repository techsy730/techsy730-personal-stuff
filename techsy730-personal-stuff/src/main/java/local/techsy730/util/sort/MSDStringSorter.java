package local.techsy730.util.sort;

import java.util.Deque;
import java.util.Arrays;

// TODO DOCUMENTATION!
// TODO Tuning so it can be on par with Arrays.sort
// XXX See if something can be done about "midstring" null characters ('\0') without harming performance much
// TODO Paralell versions of these methods, as this is a trivially parralizable sort
/**
 * 
 * @author C. Sean Young
 * 
 */
public final class MSDStringSorter
{
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
    //But not too low, as MSD sorting does involve a lot of overhead
    private static final int MAX_ARRAYS_INDEX_SORT = 51;
    private static final int MAX_ARRAYS_FULL_SORT = 59;
    
    @SuppressWarnings("unused") //One of these cases will always be marked as "dead"
    private static final int ARRAYS_SORT_CHECK = (MAX_ARRAYS_FULL_SORT > MAX_ARRAYS_INDEX_SORT) ? MAX_ARRAYS_FULL_SORT : MAX_ARRAYS_INDEX_SORT;
    
    //This causes a few extra loops to be run, and makes other loops gain extra operations, thus keep it somewhat low
    //However, the savings from a correctly computed max length can be rather large, so thus you don't need to be too conservative with it.
    private static final int _MAX_UPDATE_MAX_LENGTH_COMPUTE = (int)(ARRAYS_SORT_CHECK * 2.2);
    private static final int _MAX_MAX_UPDATE_MAX_LENGTH = 120;
    @SuppressWarnings("unused") //One of these cases will always be marked as "dead"
    private static final int MAX_UPDATE_MAX_LENGTH = _MAX_UPDATE_MAX_LENGTH_COMPUTE > _MAX_MAX_UPDATE_MAX_LENGTH ? _MAX_MAX_UPDATE_MAX_LENGTH : _MAX_UPDATE_MAX_LENGTH_COMPUTE;
    
    //Set this to the minimum that there isn't a specific n-way sort method for.
    private static final int MIN_BLOCK_LEN_TO_MERGE = 4;
    
    //Blocks of size less than this we don't binary search over to find the guess for the end character. It is usually not worth it in these cases.
    private static final int MIN_BINARY_SEARCH = 28;
    
    //If this is false, then bucket sort will be used even if length < MAX_ARRAYS_INDEX_SORT but not close enough to the end to trigger a full sort
    //(Except for length < MAX_INSERTION_SORT, which will always fully sort)
    private static final boolean DO_NON_FULL_ARRAYS_SORT = true;

    // For these two values, SMALLER numbers mean MORE hesistant to trigger the optimization
    //Merging is a rather expensive operation, just to save a few method calls. Thus, be conservative with this number. 
    private static final int MAX_REMAINING_BEFORE_MERGING_RANGES = 2;
    //Be conservative with this number, as not only will it trigger more aggressive merges, but also cause a non-insertion call to
    //arrays sort to look at all of the remaining characters, which could get expensive.
    private static final int MAX_REMAINING_BEFORE_FULL_ARRAYS_SORT = 4;
    
    private static final int MAX_DIFF_BETWEEN_MAX_INDEX_GUESS_AND_REAL_MAX = 30;
    
    /*
    static
    {
        assert MAX_INSERTION_SORT <= MAX_ARRAYS_SORT;
    }
*/
    
    private MSDStringSorter(Deque<SortState> stack, String[] arr, String[] wc)
    {
        this.stack = stack;
        this.arr = arr;
        this.wc = wc;
    }

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
        // TODO We can do much better if we dump to char[] first
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
        if(len <= MAX_ARRAYS_FULL_SORT)
        {
            Arrays.sort(arr, fromIndex, toIndex);
            return;
        }
        String[] wc = new String[arr.length];
        // Thanks to how large strings can get, we cannot use recursion. As such, we must
        // maintain our own stack to maintain current state
        // XXX Empirically determine a good estimate for preallocation, based on common usage cases
        // Not using <> syntactic shortcut to maintain source compatibility with java 1.6
        @SuppressWarnings("unused") //This is to keep a 1.7 compiler from complaining about the reduntant type specification
        Deque<SortState> stack = new java.util.ArrayDeque<SortState>(
            Math.max(15, (int)(1.5 * com.google.common.math.IntMath.log2(len, java.math.RoundingMode.UP))));
        stack.addFirst(new SortState(fromIndex, toIndex, 0, minCharsToSort));
        //The thing that will hold our sort progress
        new MSDStringSorter(stack, arr, wc).doSort();
    }

    private final Deque<SortState> stack;
    private final String[] arr;
    private final String[] wc;
    private SortState tempHolder;
    private SortState mergeHolder = null;
    private boolean mergeHasMoreThanOne = false;
    private int maxIndexTempTrack = -1;
    
    private int maxCharValueSeen = DEFAULT_CHAR_STORAGE_PRE_ALLOC;
    
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

    static final int compareSubtrStartingAt(final String s1, final String s2, final int index)
    {
        final int len1 = s1.length();
        final int len2 = s2.length();
        if(len1 <= index)
        {
            return len2 <= index ? 0 : -1;
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
    
    static final int compareSubtr(final String s1, final String s2, final int index, final int maxIndex)
    {
        assert maxIndex > index;
        final int len1 = s1.length();
        final int len2 = s2.length();
        if(len1 <= index)
        {
            return len2 <= index ? 0 : -1;
        }
        if(len2 <= index) return 1;
        
        //Pretty much ripped from String#compareTo(String)
        int lim = Math.min(len1, len2);
        lim = Math.min(lim, maxIndex);
    
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
        return k == maxIndex ? 0 : len1 - len2;
    }

    private final void doSort()
    {
        while(!stack.isEmpty())
        {
            // "Recurse"
            processPart(stack.pollFirst());
        }
        return; // This redundant return is only so we can set a breakpoint here.
    }

    private void processPart(SortState currentState)
    {
        final int start = currentState.start;
        final int end = currentState.end;
        final int index = currentState.index;
        int maxIndex = currentState.maxIndex;
        // If there are no characters in this block, just return now
        if(maxIndex <= 0) return;
        //Skip if we went past the "end" of the character range we want to sort 
        if(index >= maxIndex) return;
        boolean needMaxIndexUpdate = maxIndex == Integer.MAX_VALUE || end - start <= MAX_UPDATE_MAX_LENGTH;
        // First, sort on the current character
        int[] buckets = doSortOnPart(start, end, index, maxIndex, needMaxIndexUpdate);
        if(buckets == FULLY_SORTED) return;
        if(maxIndexTempTrack != -1)
        {
            maxIndex = Math.min(maxIndexTempTrack, maxIndex);
            needMaxIndexUpdate = end - start < MAX_UPDATE_MAX_LENGTH;
        }

        // Now, find sub-blocks of the same characters

        char curChar = charAt(arr[start], index);
        if(index < maxIndex)  // Skip the next index if we have reached the max, as that
                             // signals that all strings in this sub-block don't need to
                             // be sorted anymore, as there is no data in the next index
        {
            if(curChar == charAt(arr[end - 1], index))
            //Inlined from
            //processSingleCharBlock(int start, int end, int index, int maxIndex, SortState currentState)
            {
                if(curChar != '\0')
                {
                    // Common case, all the characters at this level were the same, just
                    // immediately move on
                    final int sharedPrefixLen = findSharedPrefixLen(arr, start, end, index + 1, maxIndex);
                    // Shared prefix length might be zero if all of them are them are shorter
                    // than the current index
                    stack.addFirst(currentState.setState(start, end, index + sharedPrefixLen + 1, maxIndex));
                }
            }
            else
            {
                assert buckets != FULLY_SORTED;
                assert buckets.length != 1;
                if(buckets == INDEX_SORTED_NO_BUCKETS)
                    splitByCharGroups(start, end, index, maxIndex, curChar, currentState, needMaxIndexUpdate);
                else
                    splitByCharGroups(start, end, index, maxIndex, buckets, currentState, needMaxIndexUpdate);
            }
        }
    }
    
    private void splitByCharGroups(final int start,
        final int end, final int index, final int maxIndex, final char curChar, SortState currentState, final boolean updateMaxSize)
    {
        @SuppressWarnings("hiding") //We are trying to shift from a instance member lookup to a local variable lookup for performance 
        final String[] arr = this.arr;
        tempHolder = currentState;
        int lastNewIndex = start;
        
        // Once we start getting near the end of the strings, go ahead and be willing to have larger merges
        // Warning: Whatever range this may be, it MUST be small enough such that merged
        // range WILL fall under a complete sort, not a current index only sort
        final int remainingCharsToProcess = maxIndex - index;
        final boolean shouldMerge = remainingCharsToProcess < MAX_REMAINING_BEFORE_MERGING_RANGES;
        final int maxMergedRangeSize = remainingCharsToProcess < MAX_REMAINING_BEFORE_FULL_ARRAYS_SORT ? MAX_ARRAYS_FULL_SORT : MAX_INSERTION_SORT;
        boolean atStartOfBlock = end - start >= MIN_BINARY_SEARCH;
        final java.util.Comparator<String> comp = new NaturalCharComparatorAtIndex(index);
        char curChar2 = curChar;
        if(atStartOfBlock) //Skip the longer form of the loop if we are not binary searching
        {
            int newStart = start;
            {   //Make sure to update the version of this inside the loop as well
                //Before doing a binary search, check the next string (if any) so we don't waste time on blocks of size 1
                if(start == end - 1 || (start < end - 1 && charAt(arr[start + 1], index) != curChar2))
                {
                    atStartOfBlock = false;
                    //TODO track the max index like the below loop in this case
                }
                else
                {
                    //First, find a place of the current character, and jump there. We will iterate forward from there to find the last index that has that character.
                    int charEndGuess = Arrays.binarySearch(arr, start + 1, end, arr[start], comp);
                    //Should of been found
                    assert charEndGuess > 0;
                    newStart = charEndGuess;
                    atStartOfBlock = false;
                }
            }
            for(int i = newStart; i < end; ++i)
            {
                if(charAt(arr[i], index) != curChar2)
                {
                    // New character, record end of block, and track new character
                    final int newSize = i - lastNewIndex;
                    if(newSize > 1) //Don't even bother with a method call if our size is zero or one
                        doRecurse(lastNewIndex, i, newSize, index, maxIndex, curChar2, updateMaxSize, shouldMerge, maxMergedRangeSize);
                    lastNewIndex = i;
                    curChar2 = charAt(arr[i], index);
                    atStartOfBlock = true;
                }
                if(atStartOfBlock)
                {
                    assert lastNewIndex == i;
                    //Before doing a binary search, check the next string (if any) so we don't waste time on blocks of size 1
                    if(i == end - 1 || (i < end - 1 && charAt(arr[i + 1], index) != curChar2))
                    {
                        atStartOfBlock = false;
                        //TODO track the max index like the below loop in this case
                        continue;
                    }
                    //First, find a place of the current character, and jump there. We will iterate forward from there to find the last index that has that character.
                    int charEndGuess = Arrays.binarySearch(arr, i + 1, end, arr[i], comp);
                    //Should of been found
                    assert charEndGuess > 0;
                    i = charEndGuess;
                    atStartOfBlock = false;
                }
            }   
        }
        else
        {
            int blockMaxSize = arr[start].length();
            for(int i = start + 1; i < end; ++i)
            {
                //We have to look at every character anyways, so might as well track the max size
                blockMaxSize = arr[i].length() > blockMaxSize ? arr[i].length() : blockMaxSize;
                if(charAt(arr[i], index) != curChar2)
                {
                    blockMaxSize = Math.min(blockMaxSize, maxIndex);
                    // New character, record end of block, and track new character
                    final int newSize = i - lastNewIndex;
                    if(newSize > 1) //Don't even bother with a method call if our size is zero or one
                        doRecurse(lastNewIndex, i, newSize, index, blockMaxSize, curChar2, false, shouldMerge, maxMergedRangeSize);
                    lastNewIndex = i;
                    curChar2 = charAt(arr[i], index);
                    blockMaxSize = arr[i].length();
                }
            }
        }


        // The for loop may miss one block, the final one. If so, add it now
        final int newSize = end - lastNewIndex;
        if(newSize > 1)
            doRecurseFinalIteration(lastNewIndex, end, newSize, index, maxIndex, curChar2, currentState, updateMaxSize, shouldMerge, maxMergedRangeSize);
        
        //End block, thus no more to merge
        //Thus, flush out any pending merges
        flushMergedIfNeeded();
    }

    private void splitByCharGroups(final int start, final int end,
        final int index, final int maxIndex, final int[] buckets, SortState currentState, final boolean updateMaxSize)
    {
        @SuppressWarnings("hiding") //We are trying to shift from a instance member lookup to a local variable lookup for performance 
        final String[] arr = this.arr;
        tempHolder = currentState;
        // Once we start getting near the end of the strings, go ahead and be willing to have larger merges
        // Warning: Whatever range this may be, it MUST be small enough such that merged
        // range WILL fall under a complete sort, not a current index only sort
        final int remainingCharsToProcess = maxIndex - index;
        final boolean shouldMerge = remainingCharsToProcess < MAX_REMAINING_BEFORE_MERGING_RANGES;
        final int maxMergedRangeSize = remainingCharsToProcess < MAX_REMAINING_BEFORE_FULL_ARRAYS_SORT ? MAX_ARRAYS_FULL_SORT : MAX_INSERTION_SORT;
        // curChar MUST be the min char seen
        for(int i = 0; i < buckets.length - 1; ++i)
        {
            final int regionStart = buckets[i] + start;
            final int regionEnd = buckets[i + 1] + start;
            final int newSize = regionEnd - regionStart;
            if(newSize > 1) //Don't even bother with a method call if our size is zero or one
                doRecurse(regionStart, regionEnd, newSize, index, maxIndex, charAt(arr[regionStart], index), updateMaxSize, shouldMerge, maxMergedRangeSize);
        }
    
        // The for loop may miss one block, the final one. If so, add it now
        final int regionStart = buckets[buckets.length - 1] + start;
        final int regionEnd = end;
        final int newSize = regionEnd - regionStart;
        if(newSize > 1)
            doRecurseFinalIteration(regionStart, regionEnd, newSize, index, maxIndex, charAt(arr[regionStart], index), currentState, updateMaxSize, shouldMerge, maxMergedRangeSize);
        
        //End block, thus no more to merge
        //Thus, flush out any pending merges
        flushMergedIfNeeded();
    }

    private final void doRecurse(final int regionStart, final int regionEnd, final int newSize, final int index,
        final int maxIndex, final char curChar, final boolean updateMaxSize, final boolean shouldMerge,
        final int maxMergedRangeSize)
    {
        assert newSize > 1;
        if(curChar == '\0')
        {
            //Block for null character block, which is a sign that we are at the end of these strings, so just skip this block
            return;
        }
        // From where we first saw this character to here, where the current pos is
        //Just "inline sort" blocks of size 2 or 3
        switch(newSize)
        {
            case 2:
            {
                sortTwoItems(arr, regionStart, regionStart + 1, index + 1);
                break;
            }
            case 3:
            {
                sortThreeItems(arr, regionStart, regionStart + 1, regionStart + 2, index + 1);
                break;
            }
            default:
            {
                int maxIndexBlock = conditionalFindMaxIndexBlock(regionStart, regionEnd, newSize, maxIndex, updateMaxSize, maxMergedRangeSize);
                trackMergeAndAdd(regionStart, regionEnd, newSize, index, maxIndexBlock, shouldMerge, maxMergedRangeSize);
            }
        }
    }

    private final void doRecurseFinalIteration(int regionStart, final int regionEnd, final int newSize,
        final int index, final int maxIndex, final char curChar, SortState currentState, final boolean updateMaxSize,
        final boolean shouldMerge, final int maxMergedRangeSize)
    {
        assert newSize > 1;
        if(curChar == '\0')
        {
            //Block for null character block, which is a sign that we are at the end of these strings, so just skip this block
            return;
        }
        // From where we first saw this character to here, where the current pos is
        //Just "inline sort" blocks of size 2 or 3
        switch(newSize)
        {
            case 2:
            {
                sortTwoItems(arr, regionStart, regionStart + 1, index + 1);
                break;
            }
            case 3:
            {
                sortThreeItems(arr, regionStart, regionStart + 1, regionStart + 2, index + 1);
                break;
            }
            default:
            {
                int maxIndexBlock = conditionalFindMaxIndexBlock(regionStart, regionEnd, newSize, maxIndex, updateMaxSize, maxMergedRangeSize);
                trackMergeAndAdd(regionStart, regionEnd, newSize, index, maxIndexBlock, shouldMerge, maxMergedRangeSize, currentState);
            }
        }
    }

    private final int conditionalFindMaxIndexBlock(final int regionStart, final int regionEnd,
        final int newSize, final int maxIndex, final boolean updateMaxSize,
        final int maxMergedRangeSize)
    {
        int maxIndexBlock;
        if(updateMaxSize && newSize > maxMergedRangeSize)
        //Inlined from tryFindMaxIndexBlock(int regionStart, int regionEnd)
        {
            assert regionEnd > regionStart;
            @SuppressWarnings("hiding") //We are trying to shift from a instance member lookup to a local variable lookup for performance 
            final String[] arr = this.arr;
            int maxIndexBlock1 = -1;
            for(int i = regionStart; i < regionEnd; ++i)
            {
                maxIndexBlock1 = arr[i].length() > maxIndexBlock1 ? arr[i].length() : maxIndexBlock1;
            }
            maxIndexBlock = Math.min(maxIndexBlock1, maxIndex);
        }
        else
            maxIndexBlock = maxIndex;
        return maxIndexBlock;
    }

    private final void
        trackMergeAndAdd(final int regionStart, final int regionEnd, final int newSize,
            final int index, final int maxIndexBlock, final boolean shouldMerge,
            final int maxMergedRangeSize)
    {
        trackMergeAndAdd(regionStart, regionEnd, newSize, index, maxIndexBlock, shouldMerge, maxMergedRangeSize, new SortState());
    }
    
    private final void
        trackMergeAndAdd(final int regionStart, final int regionEnd, final int newSize,
            final int index, final int maxIndexBlock, final boolean shouldMerge,
            final int maxMergedRangeSize, SortState toUseState)
    {
        // If this is a tiny block, try to see if we can accumulate with the next block
        if(shouldMerge)
        {
            if(newSize < MAX_INSERTION_SORT && newSize >= MIN_BLOCK_LEN_TO_MERGE)
            {
                assert mergeHolder != toUseState;
                //We might get a clash between the temp state holder and the state to reuse
                //Must check for that
                SortState newState =
                    toUseState == tempHolder ?
                        new SortState(regionStart, regionEnd, index, maxIndexBlock) :
                        toUseState.setState(regionStart, regionEnd, index, maxIndexBlock);
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
                        mergeHolder = toUseState.setState(newMerged);
                    }
                }
            }
            else
            {
                flushMergedIfNeeded();
                stack.addFirst(toUseState.setState(regionStart, regionEnd, index + 1, maxIndexBlock));
            }
        }
        else
        {
            stack.addFirst(toUseState.setState(regionStart, regionEnd, index + 1, maxIndexBlock));
        }
    }

    private final void flushMergedIfNeeded()
    {
        if(mergeHolder != null)
        {
            stack.addFirst(advanceIndexIfNeeded(mergeHolder, !mergeHasMoreThanOne));
            mergeHasMoreThanOne = false;
            mergeHolder = null;
        }
    }

    private static final SortState advanceIndexIfNeeded(SortState state, final boolean advanceIndex)
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
    private int[] doSortOnPart(final int fromIndex, final int toIndex,
        final int charIndex, final int maxIndex, final boolean tryTrackMaxIndex)
    {
        //Note The SortState is not given for from, to, etc, but rather for updating the max seen character
        final int len = toIndex - fromIndex;
        maxIndexTempTrack = -1;
        switch(len) 
        {
            case 0: case 1:
                return FULLY_SORTED;
            case 2:
            {
                sortTwoItems(arr, fromIndex, fromIndex + 1, charIndex);
                return FULLY_SORTED;
            }
            case 3:
            {
                sortThreeItems(arr, fromIndex, fromIndex + 1, fromIndex + 2, charIndex);
                return FULLY_SORTED;
            }
            case 4: //Even though we don't have a specific sort for 4 items, this is a common enough case to have a jump for
            {
                insertionSort(arr, fromIndex, toIndex, charIndex, maxIndex);
                return FULLY_SORTED;
            }
            default:
            {
                if(len < 0)
                    return FULLY_SORTED;
                if(len <= MAX_INSERTION_SORT)
                {
                    insertionSort(arr, fromIndex, toIndex, charIndex, maxIndex);
                    return FULLY_SORTED;
                }
                if(len <= ARRAYS_SORT_CHECK)
                {
                    if(maxIndex - charIndex <= MAX_REMAINING_BEFORE_FULL_ARRAYS_SORT && len <= MAX_ARRAYS_FULL_SORT)
                    {
                        final java.util.Comparator<String> comp = getRemainingCharSortComparator(arr, fromIndex, toIndex, charIndex, maxIndex);
                        Arrays.sort(arr, fromIndex, toIndex, comp);
                        return FULLY_SORTED;
                    }
                    if(DO_NON_FULL_ARRAYS_SORT)
                    {
                        if(len <= MAX_ARRAYS_INDEX_SORT)
                        {
                            Arrays.sort(arr, fromIndex, toIndex, new NaturalCharComparatorAtIndex(charIndex));
                            return INDEX_SORTED_NO_BUCKETS;
                        }
                    }
                }
            }
        }
        return bucketSort(fromIndex, toIndex, charIndex, tryTrackMaxIndex);
    }

    private static final java.util.Comparator<String> getRemainingCharSortComparator(final String[] arr, final int fromIndex,
        final int toIndex, final int charIndex, final int maxIndex)
    {
        final int maxIndexRealGuess = Math.max(Math.max(arr[fromIndex].length(), arr[toIndex - 1].length()), arr[(toIndex + fromIndex) >> 1].length());
        return maxIndexRealGuess - maxIndex > MAX_DIFF_BETWEEN_MAX_INDEX_GUESS_AND_REAL_MAX ?
            new NaturalSubstringComparator(charIndex, maxIndex) :
            new NaturalSuffixComparator(charIndex);
    }

    // A couple of "conditional flag arrays" as a hack to allow a sort of
    // "true, false, or data" type return
    private static final int[] FULLY_SORTED = new int[0];
    private static final int[] INDEX_SORTED_NO_BUCKETS = new int[0];

    // XXX #2 bottleneck
    // XXX Add some way to do this in the "middle" of a sort, so we don't loop through the array potentially twice
    // Basically, this needs to be merged into the comparator function somehow.
    private static final int findSharedPrefixLen(final String[] arr, final int fromIndex, final int toIndex,
        final int charIndex, final int maxIndex)
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
            final char currentChar = toTestAgainst.charAt(i);
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
        if(compareSubtrStartingAt(arr[index1], arr[index2], charIndex) > 0) {
            String temp = arr[index1];
            arr[index1] = arr[index2];
            arr[index2] = temp;
        }
    }
    
    //Adapted from The Art of Computer Programming
    //It's hard to see, but this sort is stable
    private final static void sortThreeItems(final String[] arr, 
        final int index1, final int index2, final int index3, final int charIndex)
    {
        assert index1 < index2;
        assert index2 < index3;
        if(compareSubtrStartingAt(arr[index2], arr[index1], charIndex) < 0)
        {
            String temp = arr[index1];
            if(compareSubtrStartingAt(arr[index3], arr[index2], charIndex) < 0)
            {
                arr[index1] = arr[index3];
                arr[index3] = temp;
            }
            else
            {
                //compareSubtrStartingAt(arr[index3], arr[index1], charIndex) < 0
                if(compareSubtrStartingAt(arr[index3], temp, charIndex) < 0)
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
            if(compareSubtrStartingAt(arr[index3], arr[index2], charIndex) < 0)
            {
                String temp = arr[index3];
                arr[index3] = arr[index2];
                //compareSubtrStartingAt(original arr[index3], arr[index1], charIndex) < 0
                if(compareSubtrStartingAt(temp, arr[index1], charIndex) < 0)
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
        insertionSort(final String[] arr, final int fromIndex, final int toIndex, final int charIndex, final int maxIndex)
    {
        final java.util.Comparator<String> comp = getRemainingCharSortComparator(arr, fromIndex, toIndex, charIndex, maxIndex);
        // The java.util.Arrays.sort has just a much, MUCH nicer insertion sort it uses
        // under the covers
        Arrays.sort(arr, fromIndex, toIndex, comp);
        // XXX Do a binary search when moving entries back into the correct place, but
        // take care to preserve stability
        /*
         * for(int i = fromIndex; i < toIndex; ++i) for(int j = i; j > fromIndex && compareSubtrStartingAt(arr[j],
         * arr[j-1], charIndex) < 0; --j) exchange(arr, j, j-1);
         */
    }
    
    // Default size should hold the standard ANSI charset
    private static final int DEFAULT_CHAR_STORAGE_PRE_ALLOC = 0x7F;
    
    private static final int CHAR_STORAGE_PRE_ALLOC_ANSI = 0xFF;
    
    //Big enough to hold most of the fancy "script" characters used in Chinese, Japanese, etc.
    private static final int MAX_CHAR_STORAGE_PRE_ALLOC = 0x2FA1F; 
    
    private static final int getNewUpdateSize(final int old, final int maxSeen)
    {
        int toReturn = maxSeen;
        //Never shrink below DEFAULT_CHAR_STORAGE_PRE_ALLOC
        if(old >= DEFAULT_CHAR_STORAGE_PRE_ALLOC)
        {
            if(old == toReturn)
            {
                return old;
            }
            if(toReturn < DEFAULT_CHAR_STORAGE_PRE_ALLOC)
            {
                toReturn = DEFAULT_CHAR_STORAGE_PRE_ALLOC;
                //If old was already DEFAULT_CHAR_STORAGE_PRE_ALLOC, then don't bother with a recomputation
                if(old == toReturn)
                {
                    return old;
                }
            }
            else if(old < toReturn)
            {
                //If our old size over sized, then shrink down a bit
                //Equivalent to 1 - 1/4 + 1/16 = 3/4 + 1/16 = 12/16 + 1/16 = 13/16 = 81.25%
                int newOld = old - (old >> 2) + (old >> 4);
                toReturn = toReturn > newOld ? toReturn : newOld;
            }
        }
        if(toReturn <= DEFAULT_CHAR_STORAGE_PRE_ALLOC)
            return DEFAULT_CHAR_STORAGE_PRE_ALLOC;
        if(toReturn <= CHAR_STORAGE_PRE_ALLOC_ANSI)
            return CHAR_STORAGE_PRE_ALLOC_ANSI;
        return toReturn > MAX_CHAR_STORAGE_PRE_ALLOC ? MAX_CHAR_STORAGE_PRE_ALLOC : toReturn;
    }

    //This is where the "magic" happens
    //XXX O(3n + 2m), ugly :(
    private int[] bucketSort(final int fromIndex, final int toIndex,
        final int charIndex, final boolean trackMaxLen)
    {
        //Note The SortState is not given for from, to, etc, but rather for updating the max seen character 
        final int len = toIndex - fromIndex;
        @SuppressWarnings("hiding") //We are trying to shift from a instance member lookup to a local variable lookup for performance 
        final String[] arr = this.arr;
        @SuppressWarnings("hiding") //We are trying to shift from a instance member lookup to a local variable lookup for performance
        final String[] wc = this.wc;
        final int oldMaxValueSeen = maxCharValueSeen;
        // This will grow as needed
        int[] charCountBuffer = new int[oldMaxValueSeen];

        int minCharSeen = Integer.MAX_VALUE;
        int maxCharSeen = Integer.MIN_VALUE;
        int maxSeenSize = 0;

        if(trackMaxLen)
        {
            // Count how often each character appears
            for(int i = fromIndex; i < toIndex; ++i)
            {
                final char c = charAt(arr[i], charIndex);
                minCharSeen = c < minCharSeen ? c :  minCharSeen;
                if(c > maxCharSeen)
                {
                    maxCharSeen = c;
                    charCountBuffer = (charCountBuffer.length < c) ?
                        Arrays.copyOf(charCountBuffer, c + (charCountBuffer.length >> 3) + 1) :
                        charCountBuffer;
                }
                ++charCountBuffer[c];
                if(arr[i].length() > maxSeenSize) maxSeenSize = arr[i].length();
            }
            maxIndexTempTrack = maxSeenSize;
        }
        else
        {
            // Count how often each character appears
            for(int i = fromIndex; i < toIndex; ++i)
            {
                final char c = charAt(arr[i], charIndex);
                minCharSeen = c < minCharSeen ? c :  minCharSeen;
                if(c > maxCharSeen)
                {
                    maxCharSeen = c;
                    charCountBuffer = (charCountBuffer.length < c) ?
                        Arrays.copyOf(charCountBuffer, c + (charCountBuffer.length >> 3) + 1) :
                        charCountBuffer;
                }
                ++charCountBuffer[c];
            }
            maxIndexTempTrack = -1;
        }
        
        maxCharValueSeen = getNewUpdateSize(oldMaxValueSeen, maxCharSeen);
        
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

    private static final class NaturalSuffixComparator implements java.util.Comparator<String>
    {
        final int index;

        NaturalSuffixComparator(final int index)
        {
            this.index = index;
        }

        @Override
        public int compare(final String s1, final String s2)
        {
            return compareSubtrStartingAt(s1, s2, index);
        }
    }
    
    private static final class NaturalSubstringComparator implements java.util.Comparator<String>
    {
        final int index;
        final int maxIndex;

        NaturalSubstringComparator(final int index, final int maxIndex)
        {
            this.index = index;
            this.maxIndex = maxIndex;
        }

        @Override
        public int compare(final String s1, final String s2)
        {
            return compareSubtr(s1, s2, index, maxIndex);
        }
    }
    

    // Thanks to how large strings can get, we cannot use recursion. As such, we must
    // maintain our own stack to maintain current state
    // This is the "mini stack frame" containing the data we need for the current
    // sub-problem.
    // Mutable to help reduce the number of allocations needed.
    // XXX Yea, allocating these is not as cheap as I was hoping for
    // Some moderate speedup can be had if I can manage to track ranges without having to
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
        //If this extra field turns out to be too much overhead, then this should shift to a global sort state type thing
        
        SortState(){} //Don't really care what the initial values are in this case

        SortState(final int start, final int end, final int index, final int maxIndex)
        {
            this.start = start;
            this.end = end;
            this.index = index;
            this.maxIndex = maxIndex;
        }
        
        @SuppressWarnings("unused")
        SortState(final SortState otherState)
        {
            this.start = otherState.start;
            this.end = otherState.end;
            this.index = otherState.index;
            this.maxIndex = otherState.maxIndex;
        }
        
        public SortState setState(final int start, final int end, final int index, final int maxIndex)
        {
            this.start = start;
            this.end = end;
            this.index = index;
            this.maxIndex = maxIndex;
            return this;
        }
        
        public SortState setState(final SortState otherState)
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
        public boolean equals(final Object obj)
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
         * @param other the other state to merge with
         * @return this (which is now merged with other), or {@literal null} if it cannot merge.
         */
        public SortState mergeWith(final SortState other)
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

    //Higher values mean more for higher input sizes
    private static final int NUM_TIMES_WARMUP_RATIO = 4_000_000;
    private static final int NUM_TIMES_RUN_SORT_RATIO = 10_000_000;
    private static final int MAX_NUM_TO_PRINT = 1000;

    private static final int TIME_TO_PAUSE_AFTER_WARMUP = 20;
    private static final int INDEX_TO_PART_SORT_TO = 40;
    
    private static final boolean ONLY_MSD_SORT = false;
    private static final boolean ONLY_ONE_RUN = false;
    private static final boolean GC_BETWEEN_EACH_SORT = false;
    
    private static final void conditionalGC()
    {
        if(GC_BETWEEN_EACH_SORT)
            System.gc();
    }
    
    private static final int NS_IN_MS = 1000000;

    //These command line args seem to work well
    //-Xms420m -Xmx420m -XX:NewSize=340m -XX:MaxNewSize=340m -XX:SurvivorRatio=18 -XX:+RelaxAccessControlCheck -XX:Tier0ProfilingStartPercentage=150 -XX:Tier3CompileThreshold=3000 -XX:Tier3BackEdgeThreshold=5500 -XX:MaxRecursiveInlineLevel=3
    public static void main(String... args) throws java.io.IOException, InterruptedException
    {
        // System.err.println("Pausing for 30 seconds");
        // Thread.sleep(30000);
        // As of Java 1.7.0_02, java.util.Arrays has a whopping 124 methods in it.
        // Due to the nature of methods, there are ALOT of shared prefixes, making this a
        // decent test candidate
        String[] test = org.apache.commons.io.IOUtils.readLines(
            MSDStringSorter.class.getResourceAsStream("usagov_bitly_data2012-10-29-1351522030"))
            .toArray(new String[0]);
//        String[] test = org.apache.commons.io.IOUtils.readLines(MSDStringSorter.class.getResourceAsStream("README")).toArray(new String[0]);
//        String[] test = toArrayString(Arrays.class.getDeclaredMethods());
        java.util.Collections.shuffle(Arrays.asList(test));
        //String[] test = new String[5000];
        //Arrays.fill(test, "");
        System.err.println("Dataset is: " + test.length + " entries long");
        
        final int NUM_TIMES_WARMUP = ONLY_ONE_RUN ? 1 : Math.max(20, NUM_TIMES_WARMUP_RATIO / test.length);
        final int NUM_TIMES_RUN_SORT = ONLY_ONE_RUN ? 1 : Math.max(50, NUM_TIMES_RUN_SORT_RATIO / test.length);
        
        for(String s : test)
        {
            if(s.indexOf('\0') >= 0)
            {
                System.err
                    .println("Dataset contains embedded null chars, this may cause discrepencies");
                break;
            }
        }
        // Now, "warm up" the JIT
        
        System.err.println("Will run sorts " + NUM_TIMES_WARMUP + " time(s) for warmup");
        System.err.println("Will run sorts " + NUM_TIMES_RUN_SORT + " time(s) for benchmark");

        System.err.println("Warming up JIT compiler");

        if(ONLY_MSD_SORT)
        {
            for(int i = 0; i < NUM_TIMES_WARMUP; ++i)
            {
                sort(test.clone());
            }
        }
        else
        {
            for(int i = 0; i < NUM_TIMES_WARMUP; ++i)
            {
                sort(test.clone());
                Arrays.sort(test.clone());
                String[] testClone = test.clone();
                sortPartially(INDEX_TO_PART_SORT_TO, testClone);
                Arrays.sort(testClone);
            }
        }

        // Alright, clean up, and pause
        System.gc();
        System.err.println("Pausing for " + TIME_TO_PAUSE_AFTER_WARMUP + " seconds");
        Thread.sleep(TIME_TO_PAUSE_AFTER_WARMUP * 1000);
        System.gc();

        String[] test2 = test.clone();
        String[] test3 = test.clone();
        
        long startTime;
        long endTime;
        long msdSortTime = 0; 

        System.err.println("Starting MSD sort test");
        if(test.length < MAX_NUM_TO_PRINT) System.out.println(Arrays.toString(test));
        for(int i = 0; i < NUM_TIMES_RUN_SORT - 1; ++i)
        {
            startTime = System.nanoTime();
            sort(test.clone());
            endTime = System.nanoTime();
            msdSortTime += endTime - startTime;
            conditionalGC();
        }
        startTime = System.nanoTime();
        sort(test);
        endTime = System.nanoTime();

        msdSortTime += endTime - startTime;

        System.err.println("Finished MSD sort test");
        
        long arraysSortTime = 0;
        long partialSortTime = 0;
        
        if(!ONLY_MSD_SORT)
        {
            System.gc();
            System.err.println("Starting Arrays.sort test");
    

            for(int i = 0; i < NUM_TIMES_RUN_SORT - 1; ++i)
            {
                startTime = System.nanoTime();
                Arrays.sort(test2.clone());
                endTime = System.nanoTime();
                arraysSortTime += endTime - startTime;
                conditionalGC();
            }
            startTime = System.nanoTime();
            Arrays.sort(test2);
            endTime = System.nanoTime();
            arraysSortTime += endTime - startTime;
            
            System.err.println("Finished Arrays.sort test");
            System.gc();
            System.err.println("Starting partial sort to Arrays.sort test");
            
            for(int i = 0; i < NUM_TIMES_RUN_SORT - 1; ++i)
            {
                String[] test3Clone = test3.clone();
                startTime = System.nanoTime();
                sortPartially(INDEX_TO_PART_SORT_TO, test3Clone);
                Arrays.sort(test3Clone);
                endTime = System.nanoTime();
                partialSortTime += endTime - startTime;
                conditionalGC();
            }
            startTime = System.nanoTime();
            sortPartially(INDEX_TO_PART_SORT_TO, test3);
            Arrays.sort(test3);
            endTime = System.nanoTime();
            partialSortTime += endTime - startTime;
            System.err.println("Finished partial sort to Arrays.sort test");
         
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
        }

        System.out.println("MSD sort, total:" + msdSortTime / NS_IN_MS + ", average: " +
            ((double)msdSortTime / NUM_TIMES_RUN_SORT) / NS_IN_MS);
        if(!ONLY_MSD_SORT)
        {
            System.out.println("Arrays.sort sort, total:" + arraysSortTime / NS_IN_MS + ", average: " +
                ((double)arraysSortTime / NUM_TIMES_RUN_SORT) / NS_IN_MS);
            System.out.println("Partial sort to Arrays.sort, total:" + partialSortTime / NS_IN_MS + ", average: " +
                ((double)partialSortTime / NUM_TIMES_RUN_SORT) / NS_IN_MS);
        }
        
        //Thread.sleep(TIME_TO_PAUSE_AFTER_WARMUP * 1000);
    }

    @SuppressWarnings("unused")
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
        for(int i = array1.length - 1; i >= 0; --i)
        {
            if(!array1[i].equals(array2[i])) return i;
        }
        return -1;
    }

}
