package local.techsy730.ref;

import java.lang.ref.*;



/**
 * 
 * @author C. Sean Young
 * @see java.lang.ref.Reference
 *
 */
public enum ReferenceType
{
	/**
	 * 
	 * @see java.lang.ref.SoftReference
	 */
	SOFT(SoftReference.class, "soft", "soft reference")
	{
		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public <T> Class<SoftReference<T>> getReferenceClass()
		{
			return (Class)SoftReference.class;
		}

		@Override
		public <T> SoftReference<T> wrap(T object)
		{
			return new SoftReference<>(object);
		}

		@Override
		public <T> SoftReference<T> wrap(T object, ReferenceQueue<? super T> queue)
		{
			return new SoftReference<>(object, queue);
		}
		
		@Override
		public <T> SoftReference<T> wrapPreserveNull(T object)
		{
			return (SoftReference<T>)super.wrapPreserveNull(object);
		}
		
		@Override
		public <T> SoftReference<T> wrapPreserveNull(T object, ReferenceQueue<? super T> queue)
		{
			return (SoftReference<T>)super.wrapPreserveNull(object, queue);
		}
	},
	/**
	 * 
	 * @see java.lang.ref.WeakReference
	 */
	WEAK(WeakReference.class, "weak", "weak reference")
	{
		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public <T> Class<WeakReference<T>> getReferenceClass()
		{
			return (Class)WeakReference.class;
		}

		@Override
		public <T> WeakReference<T> wrap(T object)
		{
			return new WeakReference<>(object);
		}

		@Override
		public <T> WeakReference<T> wrap(T object, ReferenceQueue<? super T> queue)
		{
			return new WeakReference<>(object, queue);
		}
		
		@Override
		public <T> WeakReference<T> wrapPreserveNull(T object)
		{
			return (WeakReference<T>)super.wrapPreserveNull(object);
		}
		
		@Override
		public <T> WeakReference<T> wrapPreserveNull(T object, ReferenceQueue<? super T> queue)
		{
			return (WeakReference<T>)super.wrapPreserveNull(object, queue);
		}

	},
	/**
	 * 
	 * @see java.lang.ref.PhantomReference
	 */
	PHANTOM(PhantomReference.class, "phantom", "phantom reference")
	{
		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public <T> Class<PhantomReference<T>> getReferenceClass()
		{
			return (Class)PhantomReference.class;
		}

		@Override
		public <T> PhantomReference<T> wrap(T object)
		{
			throw new IllegalArgumentException("Phantom references make no sense without a reference queue");
		}

		@Override
		public <T> PhantomReference<T> wrap(T object, ReferenceQueue<? super T> queue)
		{
			return new PhantomReference<>(object, queue);
		}
		
		@Override
		public <T> PhantomReference<T> wrapPreserveNull(T object)
		{
			throw new IllegalArgumentException("Phantom references make no sense without a reference queue");
		}
		
		@Override
		public <T> PhantomReference<T> wrapPreserveNull(T object, ReferenceQueue<? super T> queue)
		{
			return (PhantomReference<T>)super.wrapPreserveNull(object, queue);
		}
	};

	private final String lowercaseName;
	private final String className;
	private final String logicalFullName;

	private ReferenceType(@SuppressWarnings("rawtypes") Class<? extends Reference> refClass, String lowercaseName, String logicalFullName)
	{
		this.className = refClass.getName();
		this.lowercaseName = lowercaseName;
		this.logicalFullName = logicalFullName;
	}

	public abstract <T> Class<? extends Reference<T>> getReferenceClass();

	public abstract <T> Reference<T> wrap(T object);
	
	public <T> Reference<T> wrapPreserveNull(T object)
	{
		return object == null ? null : wrap(object);
	}

	public abstract <T> Reference<T> wrap(T object, ReferenceQueue<? super T> queue);
	
	public <T> Reference<T> wrapPreserveNull(T object, ReferenceQueue<? super T> queue)
	{
		return object == null ? null : wrap(object, queue);
	}
	
	public final String getName()
	{
		return lowercaseName;
	}
	
	public final String getReferenceClassname()
	{
		return className;
	}
	
	public final String getFullName()
	{
		return logicalFullName;
	}

	public static final boolean isReferenceClassType(Class<?> refClass)
	{
		return forClassType0(refClass) != null;
	}
	
	private static final Class<?> FINAL_REFERNCE;
	
	static
	{
		@SuppressWarnings({"unchecked", "rawtypes"})
		Class<?> ref = null;
		try
		{
			//Needed as FinalReference is a package private class
			 ref = Class.forName("java.lang.ref.FinalReference");
		}
		catch(ClassNotFoundException | LinkageError | SecurityException err)
		{
			//This Java library implementation does not use a final reference, or does not let us get the class of it, so just ignore it
			ref = null;
		}
		FINAL_REFERNCE = ref;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final ReferenceType forClassType(Class<?> refClass)
	{
		if(refClass == null)
			throw new NullPointerException();
		if(refClass.equals(Reference.class)) throw new IllegalArgumentException("Top level java.lang.ref.Reference class, although is a reference, makes no sense on its own as a reference type");
		if(FINAL_REFERNCE != null && FINAL_REFERNCE.isAssignableFrom(refClass))
		{
			if(refClass.equals(FINAL_REFERNCE))
				throw new IllegalArgumentException("Final references (java.lang.ref.FinalReference), although a valid reference type, are implemenation specific, and as such not recognized");
			throw new IllegalArgumentException("Class " + refClass.getName() + " is a final reference. Final references (java.lang.ref.FinalReference), although a valid reference type, are implemenation private, and as such not recognized");
		}
		ReferenceType result = forClassType0(refClass);
		if(result == null)
		{
			if(Reference.class.isAssignableFrom(refClass))
			{ //Are we a reference type that wasn't covered by this enum?
				Class cl = refClass;
				//Walk up the class tree until we find the reference class, so we know what kind of reference we are dealing with
				for(; !cl.getSuperclass().equals(Reference.class); cl = cl.getSuperclass()){} //Loop parts take care of all the logic
				//Now we are one below the reference type
				if(cl.equals(refClass))
					//We are the actual kind of reference
					throw new IllegalArgumentException("Unrecognized reference type " + refClass.getName());
				throw new IllegalArgumentException("Class " + refClass.getName() + " is of an unrecognized reference type " + cl.getName());
			}
			throw new IllegalArgumentException("Class " + refClass.getName() + " is not a reference type");
		}
		return result;
	}

	private static final ReferenceType forClassType0(Class<?> refClass)
	{
		if(WeakReference.class.isAssignableFrom(refClass)) return WEAK;
		if(SoftReference.class.isAssignableFrom(refClass)) return SOFT;
		if(PhantomReference.class.isAssignableFrom(refClass)) return PHANTOM;
		return null;
	}
	
	public static void main(String... args) throws Throwable
	{
		System.out.println(forClassType(WeakReference.class).toString());
		System.out.println(forClassType(SoftReference.class).toString());
		try
		{
			System.out.println(forClassType(Reference.class).toString());
		}
		catch(IllegalArgumentException err)
		{
			//Expected, we are just testing the exception raised
			err.printStackTrace();
		}
		try
		{
			System.out.println(forClassType(Class.forName("java.lang.ref.FinalReference")).toString());
		}
		catch(IllegalArgumentException err)
		{
			//Expected, we are just testing the exception raised
			err.printStackTrace();
		}
		try
		{
			Reference<?> mocked = org.mockito.Mockito.mock(Reference.class); //Use "backdoor" methods to extends the Reference class directly
			System.out.println(forClassType(mocked.getClass()).toString());
		}
		catch(IllegalArgumentException err)
		{
			//Expected, we are just testing the exception raised
			err.printStackTrace();
		}
	}

}
