package local.techsy730.function;

public abstract class AbstractFunction2<R, T1, T2> extends AbstractFunctionBase<R, ParameterPair<T1, T2>>
    implements Function2<R, T1, T2>, FunctionTypeWithR<R>
{
    @Override
    public abstract R call(T1 arg1, T2 arg2);
    
    @Override
    public Function1<R, T2> bind1(final T1 argument)
    {
        return 
            new AbstractFunction1<R, T2>()
            {
                @Override
                public final Function0<R> bind1(final T2 arg2)
                {
                    return AbstractFunction2.this.new DoubleBoundFunction(argument, arg2);
                }
            
                @Override
                public final R call(T2 arg)
                {
                    return AbstractFunction2.this.call(argument, arg);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction2.this.returnType();
                }
            };
    }
    
    @Override
    public Function1<R, T1> bind2(final T2 argument)
    {
        return 
            new AbstractFunction1<R, T1>()
            {
                @Override
                public final Function0<R> bind1(final T1 arg1)
                {
                    return AbstractFunction2.this.new DoubleBoundFunction(arg1, argument);
                }
            
                @Override
                public final R call(T1 arg)
                {
                    return AbstractFunction2.this.call(arg, argument);
                }
                
                @Override
                public final Class<?> returnType()
                {
                    return AbstractFunction2.this.returnType();
                }
            };
    }
    
    private final class DoubleBoundFunction extends AbstractFunction0<R>
    {
        private final T1 arg1;
        private final T2 arg2;
        
        DoubleBoundFunction(T1 arg1, T2 arg2)
        {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public R call()
        {
            return AbstractFunction2.this.call(arg1, arg2);
        }
        
        @Override
        public final Class<?> returnType()
        {
            return AbstractFunction2.this.returnType();
        }
    }

    @Override
    public final int getArgumentCount()
    {
        return 2;
    }

    @Override
    public R callUnsafe(Object... arguments)
    {
        checkArgumentCount(arguments);
        return call((T1)arguments[0], (T2)arguments[1]);
    }
    
    @SuppressWarnings("cast") //Just in case someone does something stupid and subverts the type system with raw types, we want to check for this
    @Override
    //We can take a shortcut over the general purpose "unpacking" logic in this case
    public R callRoot(ParameterPair<T1, T2> argument)
    {
        if(argument == null)
            throw new IllegalArgumentException("Invalid number of arguments. Recieved 0, expected 2");
        //This would normally warn me that this is always false, but due to type erasure, I want double check anyways
        //Hence the suppression of cast warnings above
        if(!(argument instanceof ParameterPair)) 
            throw new IllegalArgumentException("Invalid number of arguments. Recieved 1, expected 2");
        T1 arg1 = argument.getFirst();
        T2 arg2 = argument.getSecond();
        if(arg2 instanceof ParameterPair)
            throw new IllegalArgumentException("Invalid number of arguments. Recieved more than 2, expected 2");
        return call(arg1, arg2);
    }

}
