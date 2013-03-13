package local.techsy730.function;

public interface Function2<R, T1, T2>
    extends
        TwoOrMoreParameterFunction<R, T1, T2,
            T2, //AggregateWithNoFirstType
            T1, //AggregateWithNoSecondType
            Void, //AggregateWithNoFirstOrSecondType
            ParameterPair<T1, T2>, //AggregateType
            Function0<R>, //Bound1stAnd2ndType
            Function1<R, T2>, //Bound1stFunctionType
            Function1<R, T1>>, //Bound2ndFunctionType
        FunctionBase<R, ParameterPair<T1, T2>>, FunctionN, FunctionTypeWithR<R>
{
    public R call(T1 arg1, T2 arg2);
    
    @Override
    public Function1<R, T2> bind1(final T1 argument);
    
    @Override
    public Function1<R, T1> bind2(final T2 argument);
}
