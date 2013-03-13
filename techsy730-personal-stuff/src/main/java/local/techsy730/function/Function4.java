package local.techsy730.function;

//TODO Write a code generator for this, as it is getting rather absurd to do all this by hand
//Though that may be tough due to the complicated generics going on
public interface Function4<R, T1, T2, T3, T4>
    extends
        TwoOrMoreParameterFunction<R, T1, T2, 
            ParameterPair<T2, ParameterPair<T3, T4>>, //AggregateWithNoFirstType
            ParameterPair<T1, ParameterPair<T3, T4>>, //AggregateWithNoSecondType
            ParameterPair<T3, T4>, //AggregateWithNoFirstOrSecondType
            ParameterPair<T1, ParameterPair<T2, ParameterPair<T3, T4>>>, //AggregateType
            Function2<R, T3, T4>, //Bound1stAnd2ndType
            Function3<R, T2, T3, T4>, //Bound1stFunctionType
            Function3<R, T1, T3, T4>>, //Bound2ndFunctionType
        FunctionBase<R, ParameterPair<T1, ParameterPair<T2, ParameterPair<T3, T4>>>>, FunctionN, FunctionTypeWithR<R>
{
    public R call(T1 arg1, T2 arg2, T3 arg3, T4 arg4);
    
    @Override
    public Function3<R, T2, T3, T4> bind1(final T1 argument);
    
    @Override
    public Function3<R, T1, T3, T4> bind2(final T2 argument);
    
    public Function3<R, T1, T2, T4> bind3(final T3 argument);
    
    public Function3<R, T1, T2, T3> bind4(final T4 argument);
}
