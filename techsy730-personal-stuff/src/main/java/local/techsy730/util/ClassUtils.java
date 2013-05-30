package local.techsy730.util;

import java.util.*;

//This class should be 1.4 source compatible
public final class ClassUtils
{
    private ClassUtils(){} //Utility class
    
    private static final String OBJECT_CLASS_FULL_NAME = "java.lang.Object";
    
    //It is vital that these following String constants are interned
    //Thanks to the Java spec though, all String literals are assured to already be interned for us 
    private static final String LANG_CLASS_CLASS_NAME = "Class";
    private static final String LANG_CLASS_LOADER_CLASS_NAME = "ClassLoader";
    private static final String LANG_CLONEABLE_CLASS_NAME = "Cloneable";
    private static final String LANG_STRING_CLASS_NAME = "String";
    private static final String LANG_THROWABLE_CLASS_NAME = "Throwable";
    private static final String LANG_EXCEPTION_CLASS_NAME = "Exception";
    private static final String LANG_ERROR_CLASS_NAME = "Error";
    private static final String LANG_RUNTIME_EXCEPTION_CLASS_NAME = "RuntimeException";
    private static final String LANG_CHAR_SEQUENCE_CLASS_NAME = "CharSequence";
    //private static final String LANG_STRING_CLASS_FULL_NAME = String.class.getName();
    
    //To my chagrin, Closeable is a class introduce in 1.5
    //private static final String IO_CLOSEABLE_CLASS_NAME = "Closeable";
    private static final String IO_SERIALIZABLE_CLASS_NAME = "Serializable";
    private static final String IO_INPUT_STREAM_CLASS_NAME = "InputStream";
    private static final String IO_OUTPUT_STREAM_CLASS_NAME = "OutputStream";
    private static final String IO_READER_CLASS_NAME = "Reader";
    private static final String IO_WRITER_CLASS_NAME = "Writer";
    //private static final String IO_CLOSEABLE_CLASS_FULL_NAME = java.io.Closeable.class.getName();
    
    private static final String UTIL_COMPARABLE_CLASS_NAME = "Comparable";
    private static final String UTIL_COMPARATOR_CLASS_NAME = "Comparator";
    private static final String UTIL_ITERATOR_CLASS_NAME = "Iterator";
    private static final String UTIL_LIST_ITERATOR_CLASS_NAME = "ListIterator";
    private static final String UTIL_COLLECTION_CLASS_NAME = "Collection";
    private static final String UTIL_LIST_CLASS_NAME = "List";
    private static final String UTIL_SET_CLASS_NAME = "Set";
    
    //Not doing queue and deque, as they were added post 1.4
    
    
    //Not using enum, as that was added post 1.4
    private static final class CheckedBooleanTriState
    {
        static final CheckedBooleanTriState
            CHECKED_TRUE = new CheckedBooleanTriState(true, true),
            CHECKED_FALSE = new CheckedBooleanTriState(true, false),
            NOT_CHECKED_FALSE = new CheckedBooleanTriState(false, false);
        
        
        private final boolean isChecked;
        private final boolean boolValue;
        
        private CheckedBooleanTriState(boolean isChecked, boolean boolValue)
        {
            this.isChecked = isChecked;
            this.boolValue = boolValue;
        }
        
        public final boolean isChecked()
        {
            return isChecked;
        }
        
        public final boolean boolValue()
        {
            return boolValue;
        }
        
    }
    
    private static final CheckedBooleanTriState wrapChecked(boolean val)
    {
        return val ? CheckedBooleanTriState.CHECKED_TRUE : CheckedBooleanTriState.CHECKED_FALSE;
    }
    
    private static final ClassLoader MY_CLASSLOADER = ClassUtils.class.getClassLoader();
    
    /**
     * Tests whether o is an instance of a class provided by the given <i>fully qualified</i>, raw (no generic types given) class name.
     * However, it treats failure to resolve the class (like if it doesn't exist or security restrictions prevent loading it)
     * as false, as presumably the class cannot be loaded and as such, there is no way the given object is an instance of it.
     * NOTE: It is possible that for certain core library classes (those in the java package or sub-packages) will not be loaded
     * using the current ClassLoader, but rather using the "bootstrap" ClassLoader.
     * This shouldn't give any different results unless the current ClassLoader was a non-delegation first ClassLoader,
     * in which case it might.<p>
     * NOTE: This method tries to use the ClassLoader of the caller of this method. However, if it cannot determine the caller's
     * ClassLoader due to Java security policies, it will instead fall back on its own ClassLoader.
     * Use {@link #isInstanceFalseOnFail(Object, String, ClassLoader)} if you want to ensure which ClassLoader is used.
     * @param o the object to test
     * @param className the <i>fully qualified</i> name of the class to check o is an instance of
     * @return true if o is an instance of the class provided, false if it isn't or if the class failed to be resolved 
     * @throws SecurityException if the current context does not allow access to the requested Class.
     *                              This is chosen instead of false because just because this method cannot access the class
     *                              doesn't mean the class is not loaded, and as such, o might be an instance,
     *                              so it would not be safe to say it isn't.
     */
    public static boolean isInstanceFalseOnFail(final Object o, final String className)
    {
        if(o == null)
            return false; //Nulls are not instances of anything
        //Fast path for Object itself
        if(className.equals(OBJECT_CLASS_FULL_NAME))
            return true; //We are clearly an object if we are not null
        CheckedBooleanTriState commonCaseCheck = isInstanceCommonCases(o, className); //Fast path for common classes
        if(commonCaseCheck.isChecked())
            return commonCaseCheck.boolValue();
        if(RuntimeTools.canGetStack())
        {
            try
            {
                return isInstanceFalseOnFail0(o, className, RuntimeTools.getCaller().getClassLoader());
            }
            catch(SecurityException e)
            {
                //TODO Log this somehow
                //Well, we can't use the caller's classloader, so "fallthrough" below to using our own classloader 
            }
        }
        return isInstanceFalseOnFail0(o, className, MY_CLASSLOADER);
    }

    
    /**
     * Tests whether o is an instance of a class provided by the given <i>fully qualified</i>, raw (no generic types given) class name,
     * using the given ClassLoader.
     * However, it treats failure to resolve the class (like if it doesn't exist or security restrictions prevent loading it)
     * as false, as presumably the class cannot be loaded and as such, there is no way the given object is an instance of it.<p>
     * NOTE: It is possible that for certain core library classes (those in the java package or sub-packages) will not be loaded
     * using the given ClassLoader, but rather using the "bootstrap" ClassLoader.
     * This shouldn't give any different results unless the ClassLoader given was a non-delegation first ClassLoader,
     * in which case it might.
     * @param o the object to test
     * @param className the <i>fully qualified</i> name of the class to check o is an instance of
     * @param classLoaderToUse the ClassLoader to use to lookup the class name
     * @return true if o is an instance of the class provided, false if it isn't or if the class failed to be resolved
     * @throws SecurityException if the current context does not allow access to the given ClassLoader or requested Class.
     *                              This is chosen instead of false because just because this method cannot access the classloader or class
     *                              doesn't mean the class is not loaded, and as such, o might be an instance,
     *                              so it would not be safe to say it isn't.  
     */
    public static boolean isInstanceFalseOnFail(final Object o, final String className, final ClassLoader classLoaderToUse)
    {
        if(o == null)
            return false; //Nulls are not instances of anything
        //Fast path for Object itself
        if(className.equals(OBJECT_CLASS_FULL_NAME))
            return true; //We are clearly an object if we are not null
        CheckedBooleanTriState commonCaseCheck = isInstanceCommonCases(o, className); //Fast path for common classes
        if(commonCaseCheck.isChecked())
            return commonCaseCheck.boolValue();
        return isInstanceFalseOnFail0(o, className, classLoaderToUse);
    }

    private static boolean isInstanceFalseOnFail0(final Object o, final String className,
        final ClassLoader classLoaderToUse)
    {
        try
        {
            if(o.getClass().getClassLoader() == classLoaderToUse &&
               o.getClass().getName().equals(className))
            {
                //If both the classloader and the fully qualified name match up
                //Then it must be the exact class, meaning it is trivially an instance of it
                return true;
            }
        }
        catch(SecurityException err)
        {
            //Well, we can't get access to the class loader directly, so just go on to the next check.
        }
        try
        {
            return Class.forName(className, false, classLoaderToUse).isInstance(o);
        }
        catch(ClassNotFoundException err)
        {
            //Can't find the class, so clearly not an instance
            return false;
        }
        catch(LinkageError err)
        {
            //Error while loading the class, so clearly not an instance
            return false;
        }
        catch(SecurityException err)
        {
            //Well, we can't use it directly, let's try "flexing" our "full" permission "muscles".
            //Can't use the generics, as this class is supposed to be 1.4 source compatible
            //Also, can't use @SuppressWarnings to stop this annoying warning for the same reason 
            return ((Boolean)java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction()
                {
                    public Object run()
                    {
                        try
                        {
                            return Boolean.valueOf(Class.forName(className, false, classLoaderToUse).isInstance(o));
                        }
                        catch(ClassNotFoundException err)
                        {
                            //Can't find the class, so clearly not an instance
                            return Boolean.FALSE;
                        }
                        catch(LinkageError err)
                        {
                            //Error while loading the class, so clearly not an instance
                            return Boolean.FALSE;
                        }
                    }  
                })).booleanValue();
        }
    }
    
    //Fast paths for some common classes. Does not fast path java.lang.Object!
    private static CheckedBooleanTriState isInstanceCommonCases(final Object o, final String className)
    {
        //Skip this test if we are not even in the right top level package
        if(className.startsWith("java."))
        {
            final String[] packageParts = className.split("\\.");
            if(packageParts.length != 3) //Only testing classes of a java.SubPackage.ClassName pattern 
            {
                return CheckedBooleanTriState.NOT_CHECKED_FALSE;
            }
            //XXX String switches would of been ideal for this, but that was added in java 1.7. This class targets java 1.4
            //Too many cases for equals method; should intern for == comparison
            final String intenedClassName = packageParts[2].intern();
            if(packageParts[1].equals("util"))
            {
                //Most common case first
                if(intenedClassName == UTIL_COMPARABLE_CLASS_NAME)
                    return wrapChecked(o instanceof Comparable);
                //Followed by smallest first, going up
                if(intenedClassName == UTIL_SET_CLASS_NAME)
                    return wrapChecked(o instanceof Set);
                if(intenedClassName == UTIL_LIST_CLASS_NAME)
                    return wrapChecked(o instanceof List);
                if(intenedClassName == UTIL_ITERATOR_CLASS_NAME)
                    return wrapChecked(o instanceof Iterator);
                if(intenedClassName == UTIL_COLLECTION_CLASS_NAME)
                    return wrapChecked(o instanceof Collection);
                if(intenedClassName == UTIL_COMPARATOR_CLASS_NAME)
                    return wrapChecked(o instanceof Comparator);
                if(intenedClassName == UTIL_LIST_ITERATOR_CLASS_NAME)
                    return wrapChecked(o instanceof ListIterator);
            }
            else if(packageParts[1].equals("lang"))
            {
                //Most common cases first
                if(intenedClassName == LANG_CLONEABLE_CLASS_NAME)
                    return wrapChecked(o instanceof Cloneable);
                if(intenedClassName == LANG_THROWABLE_CLASS_NAME)
                    return wrapChecked(o instanceof Throwable);
                if(intenedClassName == LANG_EXCEPTION_CLASS_NAME)
                    return wrapChecked(o instanceof Exception);
                if(intenedClassName == LANG_ERROR_CLASS_NAME)
                    return wrapChecked(o instanceof Error);
                if(intenedClassName == LANG_RUNTIME_EXCEPTION_CLASS_NAME)
                    return wrapChecked(o instanceof RuntimeException);
                //smallest first, going up
                if(intenedClassName == LANG_STRING_CLASS_NAME)
                    return wrapChecked(o instanceof String);
                if(intenedClassName == LANG_CLASS_CLASS_NAME)
                    return wrapChecked(o instanceof Class);
                if(intenedClassName == LANG_CLASS_LOADER_CLASS_NAME)
                    return wrapChecked(o instanceof ClassLoader);
                if(intenedClassName == LANG_CHAR_SEQUENCE_CLASS_NAME)
                    return wrapChecked(o instanceof CharSequence);
            }
            else if(packageParts[1].equals("io"))
            {
                //Most common cases first
                if(intenedClassName == IO_SERIALIZABLE_CLASS_NAME)
                    return wrapChecked(o instanceof java.io.Serializable);
                if(intenedClassName == IO_INPUT_STREAM_CLASS_NAME)
                    return wrapChecked(o instanceof java.io.InputStream);
                if(intenedClassName == IO_OUTPUT_STREAM_CLASS_NAME)
                    return wrapChecked(o instanceof java.io.OutputStream);
                if(intenedClassName == IO_READER_CLASS_NAME)
                    return wrapChecked(o instanceof java.io.Reader);
                if(intenedClassName == IO_WRITER_CLASS_NAME)
                    return wrapChecked(o instanceof java.io.Writer);
            }
        }
        return CheckedBooleanTriState.NOT_CHECKED_FALSE;
    }
    
    public static void main(String... args)
    {
        System.out.println(isInstanceFalseOnFail(new java.util.ArrayList(), "java.util.List"));
        System.out.println(isInstanceFalseOnFail(new java.util.ArrayList(), "java.lang.Cloneable"));
        System.out.println(isInstanceFalseOnFail(new Object(), "java.lang.reflect.Method"));
        System.out.println(isInstanceFalseOnFail(Object.class.getMethods()[0], "java.lang.reflect.Method"));
        System.out.println(isInstanceFalseOnFail(new Object(), "java.lang.String"));
        System.out.println(isInstanceFalseOnFail(new Object(), "java.lang.Object"));
        System.out.println(isInstanceFalseOnFail(ClassUtils.class, "java.lang.Object"));
        System.out.println(isInstanceFalseOnFail(ClassUtils.class, "java.lang.Class"));
    }
}
