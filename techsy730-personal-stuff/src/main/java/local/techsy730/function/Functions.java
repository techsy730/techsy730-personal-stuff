package local.techsy730.function;

public final class Functions
{
    private Functions(){} //Utility class
    
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
            public final Object call(Object argument)
            {
                return argument;
            }
        };
        
    @SuppressWarnings("unchecked")
    public static final <T> Function1<T, T> identityFunction()
    {
        return (Function1<T, T>)IDENTITY;
    }
    
    private static final class EqualsFunctionWithObject extends AbstractFunction1<Boolean, Object>
    {
        private final Object thisObj;
        
        EqualsFunctionWithObject(Object o)
        {
            thisObj = o;
        }
        
        @Override
        public Boolean call(Object other)
        {
            return java.util.Objects.equals(thisObj, other);
        }
    }
    
    private static final Function2<Boolean, Object, Object> EQUALS_WITH_NO_BOUND = 
        new AbstractFunction2<Boolean, Object, Object>()
        {
            @Override
            public Boolean call(Object o1, Object o2)
            {
                return java.util.Objects.equals(o1, o2);
            }
        };
        
    public static final Function1<Boolean, Object> equalToFunction(Object o)
    {
        return new EqualsFunctionWithObject(o);
    }
    
    @SuppressWarnings("unchecked")
    public static final <T> Function1<Boolean, T> equalToFunctionTyped(Object o)
    {
        return (Function1<Boolean, T>)new EqualsFunctionWithObject(o);
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
            public Integer call(Object arg)
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
}
