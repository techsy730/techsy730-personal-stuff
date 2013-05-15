package local.techsy730.function;

import java.util.Comparator;

public final class Functions
{
    private Functions(){} //Utility class
    
    private static final void checkFunctionArgCount(int expected, int gotten)
    {
        if(gotten != expected)
        {
            throw new IllegalArgumentException("Invalid number of arguments. Recieved: " + gotten + " Expected: " + expected);
        }
    }
    
    public static final <R> Function0<R> asFunction0(final FunctionBase<R, Void> function)
    {
        if(function instanceof Function0)
            return (Function0<R>)function;
        checkFunctionArgCount(0, function.getArgumentCount());
        return
            new AbstractFunction0<R>()
            {
                @Override
                public R call()
                {
                    return function.callRoot(null); 
                }
            };
    }
    
    public static final <R, T> Function1<R, T> asFunction1(final FunctionBase<R, T> function)
    {
        if(function instanceof Function1)
            return (Function1<R, T>)function;
        checkFunctionArgCount(1, function.getArgumentCount());
        return
            new AbstractFunction1<R, T>()
            {
                @Override
                public R call(T arg)
                {
                    return function.callRoot(arg); 
                }
                
                @Override
                public Class<?> returnType()
                {
                    return function.returnType();
                }
                
                @Override
                public java.lang.Class<?>[] getArgumentTypes()
                {
                    return function.getArgumentTypes();
                }
                
                @Override
                public java.lang.Class<?> getArgumentType(int arg)
                {
                    return function.getArgumentType(arg);
                }
            };
    }
    
    @SuppressWarnings("unchecked") //Um, compiler, that cast checks out generics safety wise. You saw that on asFunction1...I know you can figure this stuff out.
    public static final <R, T1, T2> Function2<R, T1, T2> asFunction2(final FunctionBase<R, ParameterPair<T1, T2>> function)
    {
        if(function instanceof Function2)
            return (Function2<R, T1, T2>)function;
        checkFunctionArgCount(2, function.getArgumentCount());
        return
            new AbstractFunction2<R, T1, T2>()
            {
                @Override
                public R call(T1 arg1, T2 arg2)
                {
                    return function.callRoot(new SimpleParameterPair<>(arg1, arg2));
                }
                
                @Override
                public Class<?> returnType()
                {
                    return function.returnType();
                }
                
                @Override
                public java.lang.Class<?>[] getArgumentTypes()
                {
                    return function.getArgumentTypes();
                }
                
                @Override
                public java.lang.Class<?> getArgumentType(int arg)
                {
                    return function.getArgumentType(arg);
                }
            };
    }
    
    @SuppressWarnings("unchecked") //Um, compiler, that cast checks out generics safety wise. You saw that on asFunction1...I know you can figure this stuff out.
    public static final <R, T1, T2, T3> Function3<R, T1, T2, T3>
        asFunction3(final FunctionBase<R, ParameterPair<T1, ParameterPair<T2, T3>>> function)
    {
        if(function instanceof Function3)
            return (Function3<R, T1, T2, T3>)function;
        checkFunctionArgCount(3, function.getArgumentCount());
        return
            new AbstractFunction3<R, T1, T2, T3>()
            {
                @Override
                public R call(T1 arg1, T2 arg2, T3 arg3)
                {
                    //At this point, it is cheaper to make an array than it is the chained parameter pairs
                    return function.callUnsafe(arg1, arg2, arg3);
                }
                
                @Override
                public Class<?> returnType()
                {
                    return function.returnType();
                }
                
                @Override
                public java.lang.Class<?>[] getArgumentTypes()
                {
                    return function.getArgumentTypes();
                }
                
                @Override
                public java.lang.Class<?> getArgumentType(int arg)
                {
                    return function.getArgumentType(arg);
                }
            };
            
            
    }
    
    @SuppressWarnings("unchecked") //Um, compiler, that cast checks out generics safety wise. You saw that on asFunction1...I know you can figure this stuff out.
    public static final <R, T1, T2, T3, T4> Function4<R, T1, T2, T3, T4>
        asFunction4(final FunctionBase<R, ParameterPair<T1, ParameterPair<T2, ParameterPair<T3, T4>>>> function)
    {
        if(function instanceof Function4)
            return (Function4<R, T1, T2, T3, T4>)function;
        checkFunctionArgCount(4, function.getArgumentCount());
        return
            new AbstractFunction4<R, T1, T2, T3, T4>()
            {
                @Override
                public R call(T1 arg1, T2 arg2, T3 arg3, T4 arg4)
                {
                    //At this point, it is cheaper to make an array than it is the chained parameter pairs
                    return function.callUnsafe(arg1, arg2, arg3, arg4);
                }
                
                @Override
                public Class<?> returnType()
                {
                    return function.returnType();
                }
                
                @Override
                public java.lang.Class<?>[] getArgumentTypes()
                {
                    return function.getArgumentTypes();
                }
                
                @Override
                public java.lang.Class<?> getArgumentType(int arg)
                {
                    return function.getArgumentType(arg);
                }
            };
    }
    
    private static final Function0<Void> NO_OP = 
        new AbstractFunction0<Void>()
        {
            @Override
            public final Void call()
            {
                return null;
            }
        };
    
    public static final Function0<Void> noOpFunction()
    {
        return NO_OP;
    }
    
    @SuppressWarnings("unchecked")
    public static final <T> Function0<T> alwaysNullFunction()
    {
        return (Function0<T>)NO_OP;
    }
    
    private static final Function1<Object, Object> IDENTITY = 
        new AbstractFunction1<Object, Object>()
        {
            @Override
            public final Object call(final Object argument)
            {
                return argument;
            }
        };
        
    @SuppressWarnings("unchecked")
    public static final <T> Function1<T, T> identityFunction()
    {
        return (Function1<T, T>)IDENTITY;
    }
    
    private static final Function2<Boolean, Object, Object> EQUALS_WITH_NO_BOUND = 
        new AbstractFunction2<Boolean, Object, Object>()
        {
            @Override
            public Boolean call(final Object o1, final Object o2)
            {
                return java.util.Objects.equals(o1, o2);
            }
        };
        
    public static final Function1<Boolean, Object> equalToFunction(final Object o)
    {
        return 
            new AbstractFunction1<Boolean, Object>()
            {
                @Override
                public Boolean call(final Object other)
                {
                    return java.util.Objects.equals(o, other);
                }
            };
    }
    
    @SuppressWarnings("unchecked")
    public static final <T> Function1<Boolean, T> equalToFunctionTyped(Object o)
    {
        return (Function1<Boolean, T>)equalToFunction(o);
    }
    
    public static final Function2<Boolean, Object, Object> equalsFunction()
    {
        return EQUALS_WITH_NO_BOUND;
    }
    
    @SuppressWarnings("unchecked")
    public static final <T1, T2> Function2<Boolean, T1, T2> equalsFunctionTyped()
    {
        return (Function2<Boolean, T1, T2>)EQUALS_WITH_NO_BOUND;
    }
    
    private static final Function1<Integer, Object> HASHCODE = 
        new AbstractFunction1<Integer, Object>()
        {
            @Override
            public Integer call(final Object arg)
            {
                return java.util.Objects.hashCode(arg);
            }
            
        };
    
    public static final Function1<Integer, Object> hashCodeFunction()
    {
        return HASHCODE;
    }
    
    @SuppressWarnings("unchecked")
    public static final <T> Function1<Integer, T> hashCodeFunctionTyped()
    {
        return (Function1<Integer, T>)HASHCODE;
    }
    
    public static final <T> Function2<Integer, T, T> comparatorAsFunction(final Comparator<T> comp)
    {
        return
            new AbstractFunction2<Integer, T, T>()
            {
                @Override
                public Integer call(T arg1, T arg2)
                {
                    return comp.compare(arg1, arg2);
                }
                
            };
    }
    
    public static final <T> Comparator<T> functionAsComparator(final Function2<Integer, T, T> func)
    {
        return
            new Comparator<T>()
            {
                @Override
                public int compare(T o1, T o2)
                {
                    return func.call(o1, o2);
                }
            };
    }
    
    public static final <T> Comparator<T> functionAsComparator(final FunctionBase<Integer, ParameterPair<T, T>> func)
    {
        return functionAsComparator(asFunction2(func));
    }
    
    public static final Function0<Void> runnableAsFunction(final Runnable runnable)
    {
        return
            new AbstractFunction0<Void>()
        {
            @Override
            public Void call()
            {
                runnable.run();
                return null;
            }
        };
    }

/*    public static final <R> FunctionBase<R, ?> bindUnsafe(final FunctionBase<R, ?> function, final int argumentIndex, final Object argument)
    {
    }*/

    private static final class BoundFunctionTo0<R> extends AbstractFunctionBase<R, Object>
    {
        private final FunctionBase<R, ?> function;
        private final Object argument;
        private transient final int myParamCount;
        private transient final Class<?>[] argTypes;
        
        BoundFunctionTo0(FunctionBase<R, ?> function, Object argument)
        {
            this.function = function;
            this.argument = argument;
            myParamCount = function.getArgumentCount() - 1;
            if(myParamCount == 0)
                argTypes = local.techsy730.util.ArrayUtils.EMPTY_CLASS_ARRAY;
            else
                argTypes = java.util.Arrays.copyOfRange(function.getArgumentTypes(), 1, function.getArgumentCount(), Class[].class);
        }
        
        @Override
        public int getArgumentCount()
        {
            return myParamCount;
        }
    
        @Override
        public R callUnsafe(Object... arguments)
        {
            checkArgumentCount(arguments.length);
            Object[] newParams = new Object[myParamCount + 1];
            newParams[0] = argument;
            System.arraycopy(arguments, 0, newParams, 1, myParamCount);
            return function.callUnsafe(newParams);
        }
        
        @Override
        public java.lang.Class<?>[] getArgumentTypes()
        {
            return argTypes.clone();
        }
        
        @Override
        public java.lang.Class<?> getArgumentType(int i)
        {
            return argTypes[i];
        }
        
    }

    private static final class BoundFunctionTo1<R> extends AbstractFunctionBase<R, Object>
    {
        private final FunctionBase<R, ?> function;
        private final Object argument;
        private transient final int myParamCount;
        private transient final Class<?>[] argTypes;
        
        BoundFunctionTo1(FunctionBase<R, ?> function, Object argument)
        {
            this.function = function;
            this.argument = argument;
            myParamCount = function.getArgumentCount() - 1;
            final Class<?>[] origArgTypes = function.getArgumentTypes();
            argTypes = new Class<?>[myParamCount];
            argTypes[0] = origArgTypes[0];
            System.arraycopy(origArgTypes, 2, argTypes, 1, myParamCount - 1);
        }
        
        @Override
        public int getArgumentCount()
        {
            return myParamCount;
        }
    
        @Override
        public R callUnsafe(Object... arguments)
        {
            checkArgumentCount(arguments.length);
            Object[] newParams = new Object[myParamCount + 1];
            newParams[0] = arguments[0];
            newParams[1] = argument;
            System.arraycopy(arguments, 1, newParams, 2, myParamCount - 1);
            return function.callUnsafe(newParams);
        }
        
        @Override
        public java.lang.Class<?>[] getArgumentTypes()
        {
            return argTypes.clone();
        }
        
        @Override
        public java.lang.Class<?> getArgumentType(int i)
        {
            return argTypes[i];
        }
        
    }
    
}
