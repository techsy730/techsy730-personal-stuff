package local.techsy730.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
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
    private static final <T> Class<T>[] getEmptyClassArray()
    {
        return (Class<T>[])EMPTY_CLASS_ARRAY;
    }
    
    /**
     * Returns a Set containing all the superclasses (and possibly interfaces) of the given class,
     * gathered recursively. There is no assurance to the order of the returned classes.
     * The given class itself will not be in the returned array.<br>
     * It is important to not that if an interface type is given and super interfaces are excluded from the list,
     * then this method will return an empty array, as interfaces do not extend from classes.
     * Also, if a class representing a primitive type or void is given, then an empty array will be returned,
     * as these "dummy" types have no superclasses or superinterfaces.
     * @param <T> the type of object that clazz points to
     * @param clazz the class of object to gather the superclasses of 
     * @param includeInterfaces whether to include interfaces extended and/or implemented
     * @return a Set containing all the superclasses (and possibly interfaces) of the given class,
     * gathered recursively
     * @throws NullPointerException if the given class is null
     */
    public static <T> Set<Class<? super T>> getAllSuperclasses(Class<T> clazz, boolean includeInterfaces)
    {
        if(clazz.isPrimitive() || //Primitives have no super classes or super interfaces. 
           clazz.equals(void.class) || //The void "type" has no super class either
           clazz.equals(Object.class)) //And finally, neither does Object
            return java.util.Collections.emptySet();
        Set<Class<? super T>> superClasses = new java.util.HashSet<>(8);
        getAllSuperclasses0(clazz, includeInterfaces, superClasses);
        return superClasses;
    }
    
    /**
     * Returns a Set containing all the superclasses, superinterfaces, and implemented interfaces of the given class,
     * gathered recursively. There is no assurance to the order of the returned classes.
     * The given class itself will not be in the returned array.<br>
     * Also, if a class representing a primitive type or void is given, then an empty array will be returned,
     * as these "dummy" types have no superclasses or superinterfaces.
     * @param <T> the type of object that clazz points to
     * @param clazz the class of object to gather the superclasses of 
     * @return a Set containing all the superclasses, superinterfaces, and implemented interfaces of the given class,
     * gathered recursively
     * @throws NullPointerException if the given class is null
     */
    public static final <T> Set<Class<? super T>> getAllSuperclasses(Class<T> clazz)
    {
        return getAllSuperclasses(clazz, true);
    }
    
    private static <T> void getAllSuperclasses0(Class<? super T> clazz, boolean includeInterfaces,
        Set<Class<? super T>> result)
    {
        {
            Class<? super T> superClass = clazz.getSuperclass();
            if(result.add(superClass))
            {
                //Don't recurse on Object
                if(!superClass.equals(Object.class))
                    getAllSuperclasses0(superClass, includeInterfaces, result);
            }
        }
        if(includeInterfaces)
        {
            @SuppressWarnings("unchecked")
            Class<? super T>[] interfaces = (Class<? super T>[])clazz.getInterfaces();
            for(Class<? super T> implemented : interfaces)
            {
                if(result.add(implemented))
                {
                    getAllSuperclasses0(implemented, includeInterfaces, result);
                }
            }
        }
    }
    
    
    public static <T> Set<Type> getAllGenericSuperclasses(Class<T> clazz, boolean includeInterfaces, boolean includeNonGenericSupers)
    {
        if(clazz.isPrimitive() || //Primitives have no super classes or super interfaces. 
            clazz.equals(void.class) || //The void "type" has no super class either
            clazz.equals(Object.class)) //And finally, neither does Object
             return java.util.Collections.emptySet();
        Set<Type> superTypes = new java.util.HashSet<>(8);
        getAllGenericSuperclasses0(clazz, includeInterfaces, includeNonGenericSupers, superTypes);
        return superTypes;
    }
    
    public static final Class<?> extractClassFromType(Type t)
    {
        if(t instanceof Class)
            return (Class<?>)t;
        if(t instanceof ParameterizedType)
            return (Class<?>)((ParameterizedType)t).getRawType();
        if(t instanceof java.lang.reflect.GenericArrayType)
            return ArrayUtils.getEmptyArray(extractClassFromType(((java.lang.reflect.GenericArrayType)t).getGenericComponentType())).getClass();
        return null;
    }
    
    public static Type findClassInTypes(Collection<? extends Type> coll, Class<?> clazz)
    {
        for(Type t : coll)
        {
            if(extractClassFromType(t).equals(clazz))
                return t;
        }
        return null;
    }
    
    public static Type findClassInTypes(Type[] coll, Class<?> clazz)
    {
        for(Type t : coll)
        {
            if(extractClassFromType(t).equals(clazz))
                return t;
        }
        return null;
    }
    
/*    public static java.lang.reflect.Type[] getTypeArgumentsForClassInTypes(Collection<? extends Type> coll, Class<?> clazz)
    {
        Type found = findClassInTypes(coll, clazz);
        if(found instanceof Class)
        {
            @SuppressWarnings("unchecked")
            Class<?> foundClazz = (Class<?>)found;
            return typeVariableToUpperBoundArray(foundClazz.getTypeParameters());
        }
        if(found instanceof ParameterizedType)
        {
            
        }
      
    }*/
    
    private static final Type[] typeVariableToUpperBoundArray(java.lang.reflect.TypeVariable<?>[] vars)
    {
        Type[] result = new Type[vars.length];
        for(int i = 0; i < vars.length; ++i)
        {
            Type[] upperBounds = vars[i].getBounds();
            if(upperBounds == null || upperBounds.length == 0)
                result[i] = Object.class;
            else
                result[i] = upperBounds[0];
        }
        return result;
    }
    

    
    private static void getAllGenericSuperclasses0(Type type, boolean includeInterfaces, boolean includeNonGenericSupers, Set<Type> result)
    {
        Type[] interfaces = null;
        if(type instanceof Class)
        {
            Class<?> clazz = (Class<?>)type;
            Type supertype = clazz.getGenericSuperclass();
            if((includeNonGenericSupers || supertype instanceof ParameterizedType)
                && result.add(supertype))
            {
                //Don't recurse on Object
                if(!supertype.equals(Object.class))
                    getAllGenericSuperclasses0(supertype, includeInterfaces, includeNonGenericSupers, result);
            }
            if(includeInterfaces)
            {
                interfaces = clazz.getGenericInterfaces();
            }
        }
        else if(type instanceof ParameterizedType)
        {
            ParameterizedType paramType = (ParameterizedType)type;
            Type rawType = paramType.getRawType();
            if(rawType instanceof Class)
            {
                Type supertype = ((Class<?>)rawType).getGenericSuperclass();
                if((includeNonGenericSupers || result instanceof ParameterizedType)
                    && result.add(supertype))
                {
                    //Don't recurse on Object
                    if(!supertype.equals(Object.class))
                        getAllGenericSuperclasses0(supertype, includeInterfaces, includeNonGenericSupers, result);
                }
                if(includeInterfaces)
                {
                    interfaces = ((Class<?>)rawType).getGenericInterfaces();
                }
            }
        }
        
        if(interfaces != null)
        {
            for(Type implemented : interfaces)
            {
                if((includeNonGenericSupers || implemented instanceof ParameterizedType)
                    && result.add(implemented))
                {
                    getAllGenericSuperclasses0(implemented, includeInterfaces, includeNonGenericSupers, result);
                }
            }
        }
    }
}
