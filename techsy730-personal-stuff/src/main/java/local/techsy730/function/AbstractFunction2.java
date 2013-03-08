package local.techsy730.function;

public abstract class AbstractFunction2<R, T1, T2> extends AbstractFunctionBase<R, ParameterPair<T1, T2>>
    implements Function2<R, T1, T2>
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
                public Function0<R> bind1(final T2 arg2)
                {
                    return AbstractFunction2.this.new DoubleBoundFunction(argument, arg2);
                }
            
                @Override
                public R call(T2 arg)
                {
                    return AbstractFunction2.this.call(argument, arg);
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
                public Function0<R> bind1(final T1 arg1)
                {
                    return AbstractFunction2.this.new DoubleBoundFunction(arg1, argument);
                }
            
                @Override
                public R call(T1 arg)
                {
                    return AbstractFunction2.this.call(arg, argument);
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

}
