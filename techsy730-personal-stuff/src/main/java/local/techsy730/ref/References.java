package local.techsy730.ref;

import java.lang.ref.*;

import local.techsy730.util.ArrayUtils;

public class References
{
	@SuppressWarnings("rawtypes")
	private static final Reference[] EMPTY_REFERENCE_ARRAY = ArrayUtils.getEmptyObjectArray(Reference.class);
	@SuppressWarnings("rawtypes")
	private static final SoftReference[] EMPTY_SOFT_REFERENCE_ARRAY = ArrayUtils.getEmptyObjectArray(SoftReference.class);
	@SuppressWarnings("rawtypes")
	private static final WeakReference[] EMPTY_WEAK_REFERENCE_ARRAY = ArrayUtils.getEmptyObjectArray(WeakReference.class);
	@SuppressWarnings("rawtypes")
	private static final PhantomReference[] EMPTY_PHANTOM_REFERENCE_ARRAY = ArrayUtils.getEmptyObjectArray(PhantomReference.class);
	
	public static final <T> T unwrapSafe(Reference<T> ref)
	{
	    return ref == null ? null : ref.get();
	}
	
	@SuppressWarnings("unchecked") @SafeVarargs //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
	public static <T> Reference<T>[] toReferenceArray(ReferenceType refType, T... array)
	{
		if(array.length == 0)
			return EMPTY_REFERENCE_ARRAY;
		Reference<T>[] result = new Reference[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = refType.wrapPreserveNull(array[i]);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked") @SafeVarargs //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
	public static <T> Reference<T>[] toReferenceArray(ReferenceType refType, ReferenceQueue<? super T> queue, T... array)
	{
		if(array.length == 0)
			return EMPTY_REFERENCE_ARRAY;

		Reference<T>[] result = new Reference[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = refType.wrapPreserveNull(array[i], queue);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked") @SafeVarargs //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
	public static <T> SoftReference<T>[] toSoftReferenceArray(T... array)
	{
		if(array.length == 0)
			return EMPTY_SOFT_REFERENCE_ARRAY;
		@SuppressWarnings("unchecked") //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
		SoftReference<T>[] result = new SoftReference[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = (SoftReference<T>)ReferenceType.SOFT.wrapPreserveNull(array[i]);
		}
		return result;
	}

	@SuppressWarnings("unchecked") @SafeVarargs //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
	public static <T> SoftReference<T>[] toSoftReferenceArray(ReferenceQueue<? super T> queue, T... array)
	{
		if(array.length == 0)
			return EMPTY_SOFT_REFERENCE_ARRAY;
		@SuppressWarnings("unchecked") //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
		SoftReference<T>[] result = new SoftReference[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = (SoftReference<T>)ReferenceType.SOFT.wrapPreserveNull(array[i], queue);
		}
		return result;
	}

	@SuppressWarnings("unchecked") @SafeVarargs //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
	public static <T> WeakReference<T>[] toWeakReferenceArray(T... array)
	{
		if(array.length == 0)
			return EMPTY_WEAK_REFERENCE_ARRAY;
		@SuppressWarnings("unchecked") //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
		WeakReference<T>[] result = new WeakReference[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = (WeakReference<T>)ReferenceType.WEAK.wrapPreserveNull(array[i]);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked") @SafeVarargs //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
	public static <T> WeakReference<T>[] toWeakReferenceArray(ReferenceQueue<? super T> queue, T... array)
	{
		if(array.length == 0)
			return EMPTY_WEAK_REFERENCE_ARRAY;
		@SuppressWarnings("unchecked") //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
		WeakReference<T>[] result = new WeakReference[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = (WeakReference<T>)ReferenceType.WEAK.wrapPreserveNull(array[i], queue);
		}
		return result;
	}
	

	@SuppressWarnings("unchecked") @SafeVarargs //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
	public static <T> PhantomReference<T>[] toPhantomReferenceArray(ReferenceQueue<? super T> queue, T... array)
	{
		if(array.length == 0)
			return EMPTY_PHANTOM_REFERENCE_ARRAY;
		@SuppressWarnings("unchecked") //This is safe, as long as the caller doesn't try to do stupid stuff with the resulting array
		PhantomReference<T>[] result = new PhantomReference[array.length];
		for(int i = 0; i < array.length; ++i)
		{
			result[i] = (PhantomReference<T>)ReferenceType.PHANTOM.wrapPreserveNull(array[i], queue);
		}
		return result;
	}
}
