package local.techsy730.function;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import local.techsy730.util.ArrayUtils;
import local.techsy730.util.ClassUtils2;

/**
 * A skeletal implementation for function types.
 * This class defines {@link #callRoot(Object) callRoot(T)} on top of its {@link #callUnsafe(Object...) callUnsafe(Object...)}
 * function, due to {@link #callUnsafe(Object...) callUnsafe(Object...)} being easier and more intuitive to implement, despite it's lack of compile time type safety.<br>
 * <p>
 * Users of the API will rarely need to deal with this class directly.
 * It is mostly to facilitate compile time type safety in the absence of variable argument generics.
 * Instead, most users will want to look at the AbstractFunctionN interfaces, where N is the number of arguments needed.
 * These provide much "cleaner" contracts to implement, and deal with complexities of the "generics chaining" mechanisms for the user.
 * @author C. Sean Young
 *
 * @param <R> {@inheritDoc}
 * @param <T> {@inheritDoc}
 */
public abstract class AbstractFunctionBase<R, T> implements FunctionBase<R, T>, FunctionTypeWithR<R>
{
    private transient volatile Class<?>[] argumentTypeCache = null;
    //Unlike ThreadLocals, ClassValues are assured to be unloaded as needed.
    private static transient final ClassValue<Class<?>> returnTypeCache = 
        new ClassValue<Class<?>>()
        {
            @Override
            protected Class<?> computeValue(Class<?> type)
            {
                return findReturnType(type);
            }        
        };
    
    @SuppressWarnings("unchecked")
    static final Class<?> findReturnType(Type selfClass)
    {
        assert selfClass != null;
        try
        {
            //Reflectively try to get the type of R that this class has passed in
            Type currentClass = selfClass;
            //Now, recursively step up the class tree until we get an immediate implementor of FunctionTypeWithR
            //(which MUST happen, as we are a AbstractFunctionBase is itself a FunctionTypeWithR)
            //XXX If there are outside the library generic types that "passthrough" the return type,
            //then this logic may erase it a bit too generally, instead of following the "passthrough"
            //This can be handled by better bookkeeping that is better suited for a method in ClassUtils or something.
            Type[] interfacesOfCurrent = ClassUtils2.extractClassFromType(currentClass).getGenericInterfaces();
            Type found = ClassUtils2.findClassInTypes(interfacesOfCurrent, FunctionTypeWithR.class);
            while(found == null && currentClass != null)
            {
                currentClass = ClassUtils2.extractClassFromType(currentClass).getGenericSuperclass();
                interfacesOfCurrent = ClassUtils2.extractClassFromType(currentClass).getGenericInterfaces();
                found = ClassUtils2.findClassInTypes(interfacesOfCurrent, FunctionTypeWithR.class);
            }
            //If we found the FunctionTypeWithR in the immediate implements list, our first type parameter (if still present) must be our return type
            if(currentClass instanceof ParameterizedType)
            {
                return ClassUtils2.extractClassFromType(((ParameterizedType)currentClass).getActualTypeArguments()[0]);
            }
            if(currentClass instanceof Class)
            {
                //Types got erased somewhere, so just extract the upper bound
                return ClassUtils2.extractClassFromType(((Class<? extends AbstractFunctionBase<?, ?>>)currentClass).getTypeParameters()[0].getBounds()[0]);
            }
            //Unable to find the marker interface, or something else went wrong, so just return our best guess, Object. :P
            return Object.class;
        }
        catch(NullPointerException | IndexOutOfBoundsException | ClassCastException |
            java.lang.reflect.GenericSignatureFormatError | java.lang.reflect.MalformedParameterizedTypeException |
            TypeNotPresentException err
          )
        {
            //Failed to parse the generic arguments, so just return our best guess, Object. :P
            return Object.class; 
        }
        
    }
    
    @Override
    public R callRoot(T argument)
    {
        return callUnsafe(unpackArguments(argument, getArgumentCount()));
    }
    
    final void checkArgumentCount(int givenNumArguments)
    {
        if(givenNumArguments != getArgumentCount())
        {
            throw new IllegalArgumentException("Invalid number of arguments. Recieved: " + givenNumArguments + " Expected: " + getArgumentCount());
        }
    }
    
    final void checkArgumentCount(Object[] arguments)
    {
        checkArgumentCount(arguments == null ? 0 : arguments.length);
    }
    
    @Override
    public Class<?>[] getArgumentTypes()
    {
          //TODO implement
//        if(argumentTypeCache == null)
//        {
//            @SuppressWarnings("unchecked")
//            Class<? extends AbstractFunctionBase<R, T>> myType = (Class<? extends AbstractFunctionBase<R, T>>)this.getClass();
//            myType.getGenericInterfaces()
//        }
        if(argumentTypeCache == null)
        {
            int argCount = getArgumentCount();
            if(argCount == 0)
            {
                argumentTypeCache = ArrayUtils.EMPTY_CLASS_ARRAY;
            }
            else
            {
                Class<?>[] newArgCache = new Class<?>[getArgumentCount()];
                java.util.Arrays.fill(newArgCache, Object.class);
                argumentTypeCache = newArgCache;
            }
        }
        return argumentTypeCache;
    }
    
    @Override
    public Class<?> getArgumentType(int arg)
    {
        return getArgumentTypes()[arg];
    }
    
    @SuppressWarnings("unchecked")
    @Override
    /**
     * {@inheritDoc}
     * <p>
     * This implementation attempts to find the return type based on the parameterized types
     * given to the super classes and/or interface at the declaration of this object's class.
     * If it fails to extract this information at runtime for any reflection related reason, it will fallback onto
     * returning Object.<br>
     * Due to the unreliability of parsing type parameter information at runtime,
     * this may sometimes give odd results in "chains" of return types.
     * Like if the function returns an Integer, but extends or implements
     * a function that returns an N extends Number (where N is a type parameter subclasses give, Integer in this case),
     * then this function might return Integer or Number, or possibly even Object.<p>
     * 
     * Also, this implementation caches the return type per concrete Class, the idea being that the
     * since the erased ("runtime") type of R is determined by class definition and not instantiation,
     * it will be consistent for the lifetime of any given Class, so thus this method does not waste
     * time with the somewhat expensive reflection for each call.<p>
     * 
     * If a subclass knows which type (or at least, top level type) it will always return,
     * but it is not made clear by the type parameters, or if there are many levels of "indirection" between
     * this object's class and the FunctionBase interface, then it is recommended to override this method
     * and have it return that class object.
     */
    public Class<?> returnType()
    {
        return returnTypeCache.get(this.getClass());
    }

    @Override
    public abstract R callUnsafe(Object... arguments);
    
    private static final Object[] EMPTY_OBJECT_ARRAY = local.techsy730.util.ArrayUtils.EMPTY_OBJECT_ARRAY;
    
    private static Object[] unpackArguments(Object arg, int argGuess)
    {
        if(arg == null)
            return EMPTY_OBJECT_ARRAY;
        if(!(arg instanceof ParameterPair))
            return new Object[]{arg};
        java.util.List<Object> buildUp = new java.util.ArrayList<>(argGuess);
        ParameterPair<?, ?> pair = (ParameterPair<?, ?>)arg;
        Object firstArg;
        Object secondArg;
        boolean isSecondArgPair;
        do
        {
            //Add the first argument, and then "recurse" if the second argument is itself another pair
            firstArg = pair.getFirst();
            secondArg = pair.getSecond();
            isSecondArgPair = secondArg instanceof ParameterPair;
            buildUp.add(firstArg);
            if(isSecondArgPair)
            {
                pair = (ParameterPair<?, ?>)secondArg;
            }
        } while(isSecondArgPair);
        buildUp.add(secondArg);
        return buildUp.toArray();
    }

}
