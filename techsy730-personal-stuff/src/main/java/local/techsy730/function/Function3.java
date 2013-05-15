package local.techsy730.function;

public interface Function3<R, T1, T2, T3>
    extends
        FunctionBase<R, ParameterPair<T1, ParameterPair<T2, T3>>>,
        
        TwoOrMoreParameterFunction<R, T1, T2,
            ParameterPair<T2, T3>, //AggregateWithNoFirstType
            ParameterPair<T1, T3>, //AggregateWithNoSecondType
            T3, //AggregateWithNoFirstOrSecondType
            ParameterPair<T1, ParameterPair<T2, T3>>, //AggregateType
            Function1<R, T3>, //Bound1stAnd2ndType
            Function2<R, T2, T3>, //Bound1stFunctionType
            Function2<R, T1, T3>>, //Bound2ndFunctionType
        FunctionN, FunctionTypeWithR<R>
{
    public R call(T1 arg1, T2 arg2, T3 arg3);
    
    @Override
    public Function2<R, T2, T3> bind1(final T1 argument);
    
    @Override
    public Function2<R, T1, T3> bind2(final T2 argument);
    
    public Function2<R, T1, T2> bind3(final T3 argument);
}
