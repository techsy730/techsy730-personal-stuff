package local.techsy730.util;

import java.util.List;
import java.util.Set;

/**
 * Much like the {@link ClassUtils} class, but can use 1.5 and up Java language features.
 * @author C. Sean Young
 *
 */
public final class ClassUtils2
{
    private ClassUtils2(){} //Utility class
    
    private static final Class<?>[] EMPTY_CLASS_ARRAY = ArrayUtils.EMPTY_CLASS_ARRAY;
    
    //To avoid proliferation of unchecked warnings
    @SuppressWarnings("unchecked")
    private static final <T> Class<? super T>[] getEmptyClassArray()
    {
        return (Class<? super T>[])EMPTY_CLASS_ARRAY;
    }
    
    /**
     * Returns an array containing all the superclasses (and possibly interfaces) of the given class,
     * gathered recursively. There is no assurance to the order of the returned classes.
     * The given class itself will not be in the returned array.<br>
     * It is important to not that if an interface type is given and super interfaces are excluded from the list,
     * then this method will return an empty array, as interfaces do not extend from classes.
     * Also, if a class representing a primitive type or void is given, then an empty array will be returned,
     * as these "dummy" types have no superclasses or superinterfaces.
     * @param <T> the type of object that clazz points to
     * @param clazz the class of object to gather the superclasses of 
     * @param includeInterfaces whether to include interfaces extended and/or implemented
     * @return an array containing all the superclasses (and possibly interfaces) of the given class,
     * gathered recursively
     * @throws NullPointerException if the given class is null
     */
    public static <T> Class<? super T>[] getAllSuperclasses(Class<T> clazz, boolean includeInterfaces)
    {
        if(clazz.isPrimitive() || //Primitives have no super classes or super interfaces. 
           clazz.equals(void.class) || //The void "type" has no super class either
           clazz.equals(Object.class)) //And finally, neither does Object
            return getEmptyClassArray();
        List<Class<? super T>> superClasses = new java.util.ArrayList<>(8);
        Set<Class<? super T>> dejaVu = new java.util.HashSet<>(8);
        getAllSuperclasses0(clazz, includeInterfaces, superClasses, dejaVu);
        return superClasses.toArray(getEmptyClassArray());
    }
    
    /**
     * Returns an array containing all the superclasses, superinterfaces, and implemented interfaces of the given class,
     * gathered recursively. There is no assurance to the order of the returned classes.
     * The given class itself will not be in the returned array.<br>
     * Also, if a class representing a primitive type or void is given, then an empty array will be returned,
     * as these "dummy" types have no superclasses or superinterfaces.
     * @param <T> the type of object that clazz points to
     * @param clazz the class of object to gather the superclasses of 
     * @return an array containing all the superclasses (and possibly interfaces) of the given class,
     * gathered recursively
     * @throws NullPointerException if the given class is null
     */
    public static final <T> Class<? super T>[] getAllSuperclasses(Class<T> clazz)
    {
        return getAllSuperclasses(clazz, true);
    }
    
    private static <T> void getAllSuperclasses0(Class<? super T> clazz, boolean includeInterfaces,
        List<Class<? super T>> resultList, Set<Class<? super T>> dejaVu)
    {
        {
            Class<? super T> superClass = clazz.getSuperclass();
            if(dejaVu.add(superClass))
            {
                resultList.add(superClass);
                //Don't recurse on Object
                if(!superClass.equals(Object.class))
                    getAllSuperclasses0(superClass, includeInterfaces, resultList, dejaVu);
            }
        }
        if(includeInterfaces)
        {
            @SuppressWarnings("unchecked")
            Class<? super T>[] interfaces = (Class<? super T>[])clazz.getInterfaces();
            for(Class<? super T> implemented : interfaces)
            {
                if(dejaVu.add(implemented))
                {
                    resultList.add(implemented);
                    getAllSuperclasses0(implemented, includeInterfaces, resultList, dejaVu);
                }
            }
        }
    }
}
