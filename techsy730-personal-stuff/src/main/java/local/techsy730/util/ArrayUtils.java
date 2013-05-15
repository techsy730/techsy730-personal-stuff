package local.techsy730.util;

import local.techsy730.math.IntUtils;

import com.google.common.cache.LoadingCache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

public class ArrayUtils
{
	public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
	public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
	public static final String[] EMPTY_STRING_ARRAY = new String[0];
	
	public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
	public static final Boolean[] EMPTY_BOOLEAN_OBJECT_ARRAY = new Boolean[0];
	
	public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	public static final Byte[] EMPTY_BYTE_OBJECT_ARRAY = new Byte[0];
	public static final short[] EMPTY_SHORT_ARRAY = new short[0];
	public static final Short[] EMPTY_SHORT_OBJECT_ARRAY = new Short[0];
	public static final int[] EMPTY_INTEGER_ARRAY = new int[0];
	public static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = new Integer[0];
	public static final long[] EMPTY_LONG_ARRAY = new long[0];
	public static final Long[] EMPTY_LONG_OBJECT_ARRAY = new Long[0];
	
	public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
	public static final Float[] EMPTY_FLOAT_OBJECT_ARRAY = new Float[0];
	public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
	public static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = new Double[0];
	
	public static final char[] EMPTY_CHARACTER_ARRAY = new char[0];
	public static final Character[] EMPTY_CHARACTER_OBJECT_ARRAY = new Character[0];
	
	//Handy for when you have to return a non-null array of an arbitrary type, but you want to signal that there is no data represented
	//Note that there is no paired primitive array for this. That is because using void as a type for anything but method returns is invalid.
	public static final Void[] EMPTY_VOID_OBJECT_ARRAY = new Void[0];
	
	/**
	 * An array with only one element, {@code null}.
	 * Despite it being declared an array of {@link Object}, its actual component type will be something different.
	 * It's exact component type is undefined, but it is guaranteed to be a type that is uninstantiable and final.
	 * This makes this array effectively immutable, as no non-null value can be assigned to its only element
	 * and not have some sort of exception (typically, an {@link ArrayStoreException}) be raised.
	 */
	public static final Object[] NULL_ONLY_ARRAY =
	    java.security.AccessController.doPrivileged(
	        new java.security.PrivilegedAction<Object[]>()
	        {
                @Override
                public Object[] run()
                {
                    return new NoInstance[]{null};
                }
	        });
	            
	
	//TODO Add this class in the list of "restricted access" stuff of the package/jar descriptor
	private static enum NoInstance
	{
	    //Note, no constants listed; this is an enum with no values.
	    //This is declared as an enum just to let the JVM know it should try to 
	    //"protect" the constructor of this class closely.
	    ;
	    
	    {
	        //This if statement is to "fool" the compiler into thinking this initializer can complete normally
	        //It is not allowed for an initializer to unconditionally throw a Throwable
	        if(true)
	        {
	            throw new NoSuchMethodError("Cannot instantiate"); //Paranoia in the face of reflection madness
	        }
	    }
	    
	    //TODO perhaps use the reflection filtering thing in sun.reflect.Reflection if it is available?
	    //This way, even getDeclaredConstructors() will claim there are no constructors.
		private NoInstance()
		{
			throw new NoSuchMethodError("Cannot instantiate"); //Paranoia in the face of reflection madness
		}
	}
	
	//Some aliases
	public static final int[] EMPTY_INT_ARRAY = EMPTY_INTEGER_ARRAY;
	public static final char[] EMPTY_CHAR_ARRAY = EMPTY_CHARACTER_ARRAY;
	
	private static final int CONCURRENCY_LEVEL = 
        IntUtils.clamp((long)Runtime.getRuntime().availableProcessors() * 2, 4, 32);
	
	//Weak keys to allow classes to be unloaded, soft values to allow the empty arrays to be unloaded but avoided if it can afford to
	private static final LoadingCache<Class<?>, Object> emptyArrayMap = CacheBuilder.newBuilder().
		initialCapacity(32).weakKeys().softValues().concurrencyLevel(CONCURRENCY_LEVEL).build(new CacheLoader<Class<?>, Object>()
		{
			@Override
			public Object load(Class<?> c)
			{
				return java.lang.reflect.Array.newInstance(c, 0);
			}
		});
	
    private static final LoadingCache<Integer, Object[]> nullOnlyMap = CacheBuilder.newBuilder().
        initialCapacity(4).weakValues().concurrencyLevel(CONCURRENCY_LEVEL).build(new CacheLoader<Integer, Object[]>()
        {
            @Override
            public Object[] load(final Integer count)
            {
                if(count < 0)
                    throw new ArrayIndexOutOfBoundsException(count);
                return java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Object[]>()
                    {
                        @Override
                        public Object[] run()
                        {
                            return new NoInstance[count];
                        }
                    });
            }
        });
   
    private static final Object[] EMPTY_NO_INSTANCE_ARRAY = new NoInstance[0];
	
	static
	{ 
		//The following cases are very common, common enough to keep around
		//These should never be freed thanks to the public strong references this class exports
		emptyArrayMap.put(Object.class, EMPTY_OBJECT_ARRAY);
		emptyArrayMap.put(Class.class, EMPTY_CLASS_ARRAY);
		emptyArrayMap.put(String.class, EMPTY_STRING_ARRAY);
		emptyArrayMap.put(Boolean.class, EMPTY_BOOLEAN_OBJECT_ARRAY);
		emptyArrayMap.put(boolean.class, EMPTY_BOOLEAN_ARRAY);
		emptyArrayMap.put(Byte.class, EMPTY_BYTE_OBJECT_ARRAY);
		emptyArrayMap.put(byte.class, EMPTY_BYTE_ARRAY);
		emptyArrayMap.put(Short.class, EMPTY_SHORT_OBJECT_ARRAY);
		emptyArrayMap.put(short.class, EMPTY_SHORT_ARRAY);
		emptyArrayMap.put(Integer.class, EMPTY_INTEGER_OBJECT_ARRAY);
		emptyArrayMap.put(int.class, EMPTY_INTEGER_ARRAY);
		emptyArrayMap.put(Long.class, EMPTY_LONG_OBJECT_ARRAY);
		emptyArrayMap.put(long.class, EMPTY_LONG_ARRAY);
		emptyArrayMap.put(Float.class, EMPTY_FLOAT_OBJECT_ARRAY);
		emptyArrayMap.put(float.class, EMPTY_FLOAT_ARRAY);
		emptyArrayMap.put(Double.class, EMPTY_DOUBLE_OBJECT_ARRAY);
		emptyArrayMap.put(double.class, EMPTY_DOUBLE_ARRAY);
		emptyArrayMap.put(Character.class, EMPTY_CHARACTER_OBJECT_ARRAY);
		emptyArrayMap.put(char.class, EMPTY_CHARACTER_ARRAY);
		emptyArrayMap.put(Void.class, EMPTY_VOID_OBJECT_ARRAY);
		emptyArrayMap.put(NoInstance.class, EMPTY_NO_INSTANCE_ARRAY);
		
		nullOnlyMap.put(0, EMPTY_NO_INSTANCE_ARRAY);
		nullOnlyMap.put(1, NULL_ONLY_ARRAY);
	}
	
	//Mostly a toy. Making a new array of size 0 would probably be cheaper than the ConcurrentHashMap lookup
	/**
	 * Returns a possibly cached array of 0 size with the component type being the class type given.
	 * As this method returns a T[], primitive arrays cannot be accessed through this method.
	 * Use {@link #getEmptyArray(Class)} for cases where arrays of primitives may be needed.<p>
	 * 
	 * As a lookup in a cache (if the implementation uses one) is likely to introduce some overhead.
	 * Thus, the intended use pattern is to store this into a static constant.
	 * The following code snippet would be considered a typical usage pattern.
	 * <code>
	 * private static final Foo[] EMPTY_FOO_ARRAY = ArrayUtils.getEmptyObjectArray(Foo.class);
	 * 
	 * public static Foo[] dumpToFooArray(java.util.Collection&lt;? extends Foo&gt; coll)
	 * {
	 *     if(coll == null)
	 *         return EMPTY_FOO_ARRAY;
	 *     return coll.toArray(EMPTY_FOO_ARRAY);
	 * }
	 * </code>
	 * @param <T> The compile time type of the array to be returned
	 * @param type the runtime type of the array to be returned
	 * @return a possibly cached array of 0 size with the component type being the class type given
	 * @throws IllegalArgumentException if the given type is a {@link Class#isPrimitive() primitive type}
	 *                                  or is {@link Void#TYPE}
	 * @throws NullPointerException if the given type was null
	 */
	@SuppressWarnings("unchecked")
	public static final <T> T[] getEmptyObjectArray(Class<T> type)
	{
		if(type.isPrimitive())
			throw new IllegalArgumentException("Cannot create an object array of primitive types");
		return (T[])emptyArrayMap.getUnchecked(type);
	}
	
	/**
     * Returns a possibly cached array of 0 size with the component type being the class type given.<br>
     * If you know that the component type given is not a primitive type, then it may be more convenient
     * to use {@link #getEmptyObjectArray(Class) getEmptyObjectArray(Class<T>)} as that method has
     * a return compile time type of the proper array type, removing the need for a cast.
     * <p>
     * 
     * As a lookup in a cache  (if the implementation uses one) is likely to introduce some overhead,
     * the intended usage pattern is to store this into a static constant.
     * The following code snippet would be considered a typical usage pattern.
     * <code><pre>
     * private static final Foo[] EMPTY_FOO_ARRAY = (Foo[])ArrayUtils.getEmptyArray(Foo.class);
     * 
     * public static Foo[] dumpToFooArray(java.util.Collection&lt;? extends Foo&gt; coll)
     * {
     *     if(coll == null)
     *         return EMPTY_FOO_ARRAY;
     *     return coll.toArray(EMPTY_FOO_ARRAY);
     * }
     * </pre></code>
     * @param type the runtime type of the array to be returned
     * @return a possibly cached array of 0 size with the component type being the class type given
     * @throws IllegalArgumentException if the given type is {@link Void#TYPE}
     * @throws NullPointerException if the given type was null
     * @see #getEmptyObjectArray(Class)
     */
	public static final Object getEmptyArray(Class<?> type)
	{
	    return emptyArrayMap.getUnchecked(type);
	}
	
	private static boolean isWithin(int testIfIsInRange, int startRange, int rangeLength)
	{
	    return testIfIsInRange >= startRange && testIfIsInRange < (startRange + rangeLength);
	}
	
	/**
     * Returns an effectively immutable array with the given number of elements, all null. 
     * Despite it the return type being declared an array of {@link Object}, its actual component type will be something different.
     * It's exact component type is undefined, but it is guaranteed to be a type that is uninstantiable and final.
     * This makes this array effectively immutable, as no non-null value can be assigned to its only element
     * and not have some sort of exception (typically, an {@link ArrayStoreException}) be raised.
     * @param count the size of the returned array
     * @return an effectively immutable array with the given number of elements, all null
     * @throws ArrayIndexOutOfBoundsException if count < 0
     */
	public static final Object[] getNullOnlyArray(int count)
	{
	    if(count < 0)
	        throw new ArrayIndexOutOfBoundsException(count);
	    return nullOnlyMap.getUnchecked(count);
	}
	
	public static String[] toArrayString(Object[] array)
    {
        if(array instanceof String[])
            return (String[])array.clone();
        String[] toReturn = new String[array.length];
        for(int i = 0; i < array.length; ++i)
        {
            toReturn[i] = array[i].toString();
        }
        return toReturn;
    }
	
	/**
	 * 
	 * @param src the source array.
	 * @param srcPos the destination array.
	 * @param dest the destination array.
	 * @param destPos starting position in the destination data.
	 * @param length the number of array elements to be copied.
	 * @see System#arraycopy(Object, int, Object, int, int)
	 * @deprecated Only a proof of concept, showing that no native code short of {@link Object#getClass()} and the native methods on 
	 * {@link java.lang.Class Class} are strictly needed for this.
	 * Performance is most certain to be inferior to {@link System#arraycopy(Object, int, Object, int, int)}
	 */
	@Deprecated
	static void arraycopy(final Object src, int srcPos, Object dest, int destPos, int length)
	{
		if(src == null)
			throw new NullPointerException("source array is null");
		if(dest == null)
			throw new NullPointerException("destination array is null");
		if(srcPos < 0)
			throw new IndexOutOfBoundsException("source position: " + srcPos + " < 0");
		if(destPos < 0)
			throw new IndexOutOfBoundsException("destination position: " + destPos + " < 0");
		if(length < 0)
			throw new IndexOutOfBoundsException("copy length: " + length + " < 0");
		final Class<?> srcClass = src.getClass();
		if(!srcClass.isArray())
			throw new ArrayStoreException("source is not an array");
	    //If the src and dest are the same
        if(src == dest)
        {
            if(srcPos == destPos)
            {
                //From and to are exactly the same, this means the net effect is nothing
                return;
            }
            if(isWithin(destPos, srcPos, length))
            {
                assert srcPos < destPos;
                /*
                 * Diagram of this case
                 *  aaaaaaaaaa...aaabbbbbbb...bbcccccccccccc...ccdddddddddddddd
                 *  ^               ^           ^                ^
                 *  srcPos          destPos     srcPos+length    destPos+length
                 *  Region b is the region that could potentially be overwritten too early,
                 *  so that is the region to copy out.
                 * 
                 */
                int regionALen = destPos - srcPos;
                int regionBLen = srcPos + length - destPos;
                //Copy over region b first
                arraycopy(src, destPos, dest, destPos+regionALen, regionBLen);
                //Now we can safely copy over region a
                arraycopy(src, srcPos, dest, destPos, regionALen);
                return;
            }
            /*
            if(isWithin(srcPos, destPos, length)
            {
                assert destPos > srcPos; */
                /*
                 * Diagram of this case
                 *  aaaaaaaaaa...aaabbbbbbb...bbcccccccccccc...ccdddddddddddddd
                 *  ^               ^           ^                ^
                 *  destPos         srcPos      destPos+length   srcPos+length
                 *  No region can be written out too early,
                 *  as by the time something in region b gets overwritten,
                 *  the parts that are overwritten have already been copied over,
                 *  and region c isn't going to get touched anyways.
                 *  So as such, in this case, just use the general purpose logic.
                 */
            /*
            } */
            //If not overlapping, no special logic needed
            //Continue with the general purpose logic
        }
		final Class<?> destClass = dest.getClass();
		if(!destClass.isArray())
			throw new ArrayStoreException("destination is not an array");
		
		//Cannot test array length here, as that would require casting to an array type, but we don't know if it is an array of primitives or an array of references 
		//We now know that both of them are actually arrays, now we gotta test for compatibility
		final Class<?> srcCompClass = srcClass.getComponentType(), destCompClass = destClass.getComponentType();
		if(srcCompClass.isPrimitive())
		{
			//Primitive copy case
			if(destCompClass.isPrimitive())
			{
				if(!srcCompClass.equals(destCompClass))
				{
					//System.arraycopy does not allow copying among different primitive array types
					throw new ArrayStoreException("Incompatible primitive types: from " + srcCompClass.getName() + " to " + destCompClass.getName());
				}
				//OK, we now know they are the same, so hand off to the delegate function 
				arraycopyPrimitive(src, srcPos, dest, destPos, length);
				return;
			}
			throw new ArrayStoreException("Source is an array of primitives, and destination is not");
		}
		if(destCompClass.isPrimitive())
		{
			//Src also being a primitive array already covered, so this is an invalid case
			throw new ArrayStoreException("Destination is an array of primitives, and source is not");
		}
		//Reference copy case			
		Object[] srcArr = (Object[])src; //Safe due to our checks ahead of time
		Object[] destArr = (Object[])dest;
		if(srcPos + length > srcArr.length)
		{
			throw new IndexOutOfBoundsException("source array: " + (srcPos + length) + " > " + srcArr.length);
		}
		if(destPos + length > destArr.length)
		{
			throw new IndexOutOfBoundsException("destination array: " + (destPos + length) + " > " + destArr.length);
		}
		//Loop through the elements
		for(int i = 0; i < length; ++i)
		{
			destArr[i + destPos] = srcArr[i + srcPos]; //Implies an ArrayStoreException if the types are not compatible
		}
	}
	
	/**
	 * Assumes that src are non-null and hold the same primitive types
	 * @param src
	 * @param srcPos
	 * @param dest
	 * @param destPos
	 * @param length
	 */
	private static void arraycopyPrimitive(Object src, int srcPos, Object dest, int destPos, int length)
	{
	    assert src.getClass() == dest.getClass();
		if(src instanceof int[]) //Common cases first
			arraycopyInt((int[])src, srcPos, (int[])dest, destPos, length);
		else if(src instanceof char[])
			arraycopyChar((char[])src, srcPos, (char[])dest, destPos, length);
		else if(src instanceof byte[])
			arraycopyByte((byte[])src, srcPos, (byte[])dest, destPos, length);
        else if(src instanceof double[])
            arraycopyDouble((double[])src, srcPos, (double[])dest, destPos, length);
		else if(src instanceof boolean[])
			arraycopyBoolean((boolean[])src, srcPos, (boolean[])dest, destPos, length);
		else if(src instanceof short[])
			arraycopyShort((short[])src, srcPos, (short[])dest, destPos, length);
		else if(src instanceof long[])
			arraycopyLong((long[])src, srcPos, (long[])dest, destPos, length);
		else if (src instanceof float[])
			arraycopyFloat((float[])src, srcPos, (float[])dest, destPos, length);
		else
			throw new AssertionError("Given primitive array type that is not recognized: " + src.getClass().getName());
	}
	
	private static void arraycopyBoolean(boolean[] srcArr, int srcPos, boolean[] destArr, int destPos, int length)
	{
		if(srcPos + length > srcArr.length)
		{
			throw new IndexOutOfBoundsException("source array: " + (srcPos + length) + " > " + srcArr.length);
		}
		if(destPos + length > destArr.length)
		{
			throw new IndexOutOfBoundsException("destination array: " + (destPos + length) + " > " + destArr.length);
		}
		//Loop through the elements
		for(int i = 0; i < length; ++i)
		{
			destArr[i + destPos] = srcArr[i + srcPos];
		}
	}
	
	private static void arraycopyByte(byte[] srcArr, int srcPos, byte[] destArr, int destPos, int length)
	{
		if(srcPos + length > srcArr.length)
		{
			throw new IndexOutOfBoundsException("source array: " + (srcPos + length) + " > " + srcArr.length);
		}
		if(destPos + length > destArr.length)
		{
			throw new IndexOutOfBoundsException("destination array: " + (destPos + length) + " > " + destArr.length);
		}
		//Loop through the elements
		for(int i = 0; i < length; ++i)
		{
			destArr[i + destPos] = srcArr[i + srcPos];
		}
	}
	
	private static void arraycopyShort(short[] srcArr, int srcPos, short[] destArr, int destPos, int length)
	{
		if(srcPos + length > srcArr.length)
		{
			throw new IndexOutOfBoundsException("source array: " + (srcPos + length) + " > " + srcArr.length);
		}
		if(destPos + length > destArr.length)
		{
			throw new IndexOutOfBoundsException("destination array: " + (destPos + length) + " > " + destArr.length);
		}
		//Loop through the elements
		for(int i = 0; i < length; ++i)
		{
			destArr[i + destPos] = srcArr[i + srcPos];
		}
	}
	
	private static void arraycopyInt(int[] srcArr, int srcPos, int[] destArr, int destPos, int length)
	{
		if(srcPos + length > srcArr.length)
		{
			throw new IndexOutOfBoundsException("source array: " + (srcPos + length) + " > " + srcArr.length);
		}
		if(destPos + length > destArr.length)
		{
			throw new IndexOutOfBoundsException("destination array: " + (destPos + length) + " > " + destArr.length);
		}
		//Loop through the elements
		for(int i = 0; i < length; ++i)
		{
			destArr[i + destPos] = srcArr[i + srcPos];
		}
	}
	
	private static void arraycopyLong(long[] srcArr, int srcPos, long[] destArr, int destPos, int length)
	{
		if(srcPos + length > srcArr.length)
		{
			throw new IndexOutOfBoundsException("source array: " + (srcPos + length) + " > " + srcArr.length);
		}
		if(destPos + length > destArr.length)
		{
			throw new IndexOutOfBoundsException("destination array: " + (destPos + length) + " > " + destArr.length);
		}
		//Loop through the elements
		for(int i = 0; i < length; ++i)
		{
			destArr[i + destPos] = srcArr[i + srcPos];
		}
	}
	
	private static void arraycopyFloat(float[] srcArr, int srcPos, float[] destArr, int destPos, int length)
	{
		if(srcPos + length > srcArr.length)
		{
			throw new IndexOutOfBoundsException("source array: " + (srcPos + length) + " > " + srcArr.length);
		}
		if(destPos + length > destArr.length)
		{
			throw new IndexOutOfBoundsException("destination array: " + (destPos + length) + " > " + destArr.length);
		}
		//Loop through the elements
		for(int i = 0; i < length; ++i)
		{
			destArr[i + destPos] = srcArr[i + srcPos];
		}
	}
	
	private static void arraycopyDouble(double[] srcArr, int srcPos, double[] destArr, int destPos, int length)
	{
		if(srcPos + length > srcArr.length)
		{
			throw new IndexOutOfBoundsException("source array: " + (srcPos + length) + " > " + srcArr.length);
		}
		if(destPos + length > destArr.length)
		{
			throw new IndexOutOfBoundsException("destination array: " + (destPos + length) + " > " + destArr.length);
		}
		//Loop through the elements
		for(int i = 0; i < length; ++i)
		{
			destArr[i + destPos] = srcArr[i + srcPos];
		}
	}
	
	private static void arraycopyChar(char[] srcArr, int srcPos, char[] destArr, int destPos, int length)
	{
		if(srcPos + length > srcArr.length)
		{
			throw new IndexOutOfBoundsException("source array: " + (srcPos + length) + " > " + srcArr.length);
		}
		if(destPos + length > destArr.length)
		{
			throw new IndexOutOfBoundsException("destination array: " + (destPos + length) + " > " + destArr.length);
		}
		//Loop through the elements
		for(int i = 0; i < length; ++i)
		{
			destArr[i + destPos] = srcArr[i + srcPos];
		}
	}
	
	//Boilerplate
	
	//For all of the "base" methods, _P_ is the primitive type, _T_ is its corrisponding wrapper type, and _PO_ is another primitive type
	/* Base methods for numeric types
	public static _P_[] toPrim_T_Array(_T_[] array) //Special case for performance
	{
		if(array.length == 0)
		{
			return EMPTY__T__ARRAY;
		}
		_P_ result = new _P_[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = array[i];
		}
		return result;
	}
	
	public static _P_[] toPrim_T_Array(Number[] array)
	{
		if(array.length == 0)
		{
			return EMPTY__T__ARRAY;
		}
		_P_ result = new _P_[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = array[i]._P_value();
		}
		return result;
	}
	
	public static _T_[] toObject_T_Array(_P_[] array) //Special case for performance
	{
		if(array.length == 0)
		{
			return EMPTY__T__OBJECT_ARRAY;
		}
		_T_[] result = new _T_[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = array[i];
		}
		return result;
	}
	
	public static _T_[] toObject_T_Array(_PO_[] array)
	{
		if(array.length == 0)
		{
			return EMPTY__T__OBJECT_ARRAY;
		}
		_T_[] result = new _T_[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = _T_.valueOf((_P_)array[i]);
		}
		return result;
	}
	
	public static _T_[] toObject_T_Array(Number[] array)
	{
		if(array.length == 0)
		{
			return EMPTY__T__OBJECT_ARRAY;
		}
		_T_[] result = new _T_[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = array[i]._P_Value();
		}
		return result;
	}*/

	public static void main(String... args)
	{
		int[] i = {2, 3, 4, 5};
		int[] i2 = new int[4];
		arraycopy(i, 0, i2, 0, 4);
		long[] i3 = {2, 5, 7, 8};
		arraycopy(i3, 1, i2, 0, 5);
		
	}
	
	
}
